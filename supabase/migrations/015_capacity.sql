-- Capacity path for the free profile.
-- Clients must not insert track_points. Live positions are Broadcast, not postgres_changes.

CREATE TABLE activity_summaries (
    activity_id UUID PRIMARY KEY REFERENCES activities(id) ON DELETE CASCADE,
    point_count INTEGER NOT NULL DEFAULT 0,
    distance_meters INTEGER,
    duration_seconds INTEGER,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE track_batches (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    activity_id UUID NOT NULL REFERENCES activities(id) ON DELETE CASCADE,
    client_batch_id TEXT NOT NULL,
    point_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (activity_id, client_batch_id)
);

CREATE INDEX track_batches_activity_id_idx ON track_batches(activity_id);

ALTER TABLE activity_summaries ENABLE ROW LEVEL SECURITY;
ALTER TABLE track_batches ENABLE ROW LEVEL SECURITY;

CREATE POLICY "View summaries of accessible activities"
ON activity_summaries FOR SELECT
USING (
    activity_id IN (
        SELECT id FROM activities
        WHERE user_id = auth.uid()
        OR is_public = true
    )
);

CREATE POLICY "View own track batches"
ON track_batches FOR SELECT
USING (
    activity_id IN (
        SELECT id FROM activities
        WHERE user_id = auth.uid()
    )
);

DROP POLICY IF EXISTS "Insert track_points for own activities" ON track_points;

REVOKE INSERT, UPDATE, DELETE ON track_points FROM anon, authenticated;
REVOKE INSERT, UPDATE, DELETE ON activity_summaries FROM anon, authenticated;
REVOKE INSERT, UPDATE, DELETE ON track_batches FROM anon, authenticated;

GRANT SELECT ON activity_summaries TO anon, authenticated;
GRANT SELECT ON track_batches TO authenticated;

CREATE OR REPLACE FUNCTION append_track_batch(
    p_activity_id UUID,
    p_client_batch_id TEXT,
    p_points JSONB
) RETURNS INTEGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    caller UUID := auth.uid();
    owner UUID;
    point JSONB;
    inserted INTEGER := 0;
    last_ts TIMESTAMPTZ;
    point_ts TIMESTAMPTZ;
    lat DOUBLE PRECISION;
    lon DOUBLE PRECISION;
BEGIN
    IF caller IS NULL THEN
        RAISE EXCEPTION 'not authenticated' USING ERRCODE = '28000';
    END IF;

    IF p_client_batch_id IS NULL
        OR char_length(p_client_batch_id) < 1
        OR char_length(p_client_batch_id) > 128 THEN
        RAISE EXCEPTION 'invalid client_batch_id' USING ERRCODE = '22023';
    END IF;

    SELECT user_id INTO owner
    FROM activities
    WHERE id = p_activity_id;

    IF owner IS NULL OR owner <> caller THEN
        RAISE EXCEPTION 'forbidden' USING ERRCODE = '42501';
    END IF;

    INSERT INTO track_batches (activity_id, client_batch_id, point_count)
    VALUES (p_activity_id, p_client_batch_id, 0)
    ON CONFLICT (activity_id, client_batch_id) DO NOTHING;

    IF NOT FOUND THEN
        RETURN 0;
    END IF;

    IF p_points IS NULL
        OR jsonb_typeof(p_points) <> 'array'
        OR jsonb_array_length(p_points) < 1
        OR jsonb_array_length(p_points) > 50 THEN
        RAISE EXCEPTION 'batch must contain 1 to 50 points' USING ERRCODE = '22023';
    END IF;

    SELECT MAX(timestamp) INTO last_ts
    FROM track_points
    WHERE activity_id = p_activity_id;

    FOR point IN SELECT value FROM jsonb_array_elements(p_points)
    LOOP
        lat := (point->>'lat')::double precision;
        lon := (point->>'lon')::double precision;
        point_ts := (point->>'timestamp')::timestamptz;

        IF lat IS NULL OR lon IS NULL OR point_ts IS NULL
            OR lat < -90 OR lat > 90
            OR lon < -180 OR lon > 180 THEN
            RAISE EXCEPTION 'invalid track point' USING ERRCODE = '22023';
        END IF;

        IF last_ts IS NOT NULL AND point_ts < last_ts + INTERVAL '1 second' THEN
            RAISE EXCEPTION 'points exceed 1 Hz' USING ERRCODE = '22023';
        END IF;

        INSERT INTO track_points (
            activity_id,
            location,
            elevation_meters,
            timestamp,
            accuracy_meters,
            speed_mps,
            bearing_degrees
        ) VALUES (
            p_activity_id,
            ST_SetSRID(ST_MakePoint(lon, lat), 4326)::geography,
            (point->>'elevationMeters')::double precision,
            point_ts,
            (point->>'accuracyMeters')::double precision,
            (point->>'speedMps')::double precision,
            (point->>'bearingDegrees')::double precision
        );

        last_ts := point_ts;
        inserted := inserted + 1;
    END LOOP;

    UPDATE track_batches
    SET point_count = inserted
    WHERE activity_id = p_activity_id
      AND client_batch_id = p_client_batch_id;

    INSERT INTO activity_summaries (activity_id, point_count, updated_at)
    VALUES (p_activity_id, inserted, NOW())
    ON CONFLICT (activity_id) DO UPDATE
    SET point_count = activity_summaries.point_count + EXCLUDED.point_count,
        updated_at = NOW();

    RETURN inserted;
END;
$$;

REVOKE ALL ON FUNCTION append_track_batch(UUID, TEXT, JSONB) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION append_track_batch(UUID, TEXT, JSONB) TO authenticated;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_publication WHERE pubname = 'supabase_realtime') THEN
        IF EXISTS (
            SELECT 1
            FROM pg_publication_tables
            WHERE pubname = 'supabase_realtime'
              AND schemaname = 'public'
              AND tablename = 'track_points'
        ) THEN
            ALTER PUBLICATION supabase_realtime DROP TABLE public.track_points;
        END IF;
    END IF;
END $$;

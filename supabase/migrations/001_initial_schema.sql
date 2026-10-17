-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Enable PostGIS extension for geospatial queries
CREATE EXTENSION IF NOT EXISTS postgis;

-- Create enum types
CREATE TYPE privacy_level AS ENUM ('public', 'followers', 'private');
CREATE TYPE activity_type AS ENUM ('run', 'walk', 'hike', 'bike', 'other');

-- Profiles table (extends Supabase Auth users)
CREATE TABLE profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    username TEXT UNIQUE NOT NULL,
    full_name TEXT,
    avatar_url TEXT,
    bio TEXT,
    location TEXT,
    privacy_level privacy_level DEFAULT 'public',
    home_location GEOGRAPHY(Point, 4326),
    home_radius_meters INTEGER DEFAULT 300,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Activities table
CREATE TABLE activities (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT,
    activity_type activity_type DEFAULT 'run',
    
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ,
    
    distance_meters INTEGER,
    duration_seconds INTEGER,
    avg_pace_seconds_per_km INTEGER,
    elevation_gain_meters INTEGER,
    
    started_location GEOGRAPHY(Point, 4326),
    ended_location GEOGRAPHY(Point, 4326),
    
    is_public BOOLEAN DEFAULT false,
    is_live_shared BOOLEAN DEFAULT false,
    
    likes_count INTEGER DEFAULT 0,
    comments_count INTEGER DEFAULT 0,
    
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Track points table
CREATE TABLE track_points (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    activity_id UUID NOT NULL REFERENCES activities(id) ON DELETE CASCADE,
    
    location GEOGRAPHY(Point, 4326) NOT NULL,
    elevation_meters FLOAT,
    timestamp TIMESTAMPTZ NOT NULL,
    accuracy_meters FLOAT,
    speed_mps FLOAT,
    bearing_degrees FLOAT,
    
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Create indexes
CREATE INDEX profiles_username_idx ON profiles(username);
CREATE INDEX activities_user_id_idx ON activities(user_id);
CREATE INDEX activities_start_time_idx ON activities(start_time DESC);
CREATE INDEX track_points_activity_id_idx ON track_points(activity_id);
CREATE INDEX track_points_timestamp_idx ON track_points(timestamp);

-- Spatial indexes (PostGIS)
CREATE INDEX track_points_location_idx ON track_points USING GIST(location);
CREATE INDEX activities_started_location_idx ON activities USING GIST(started_location);

-- Enable Row Level Security
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE activities ENABLE ROW LEVEL SECURITY;
ALTER TABLE track_points ENABLE ROW LEVEL SECURITY;

-- RLS Policies for profiles
CREATE POLICY "Profiles are viewable by everyone"
ON profiles FOR SELECT
USING (true);

CREATE POLICY "Users can update own profile"
ON profiles FOR UPDATE
USING (auth.uid() = id);

-- RLS Policies for activities
CREATE POLICY "Users can view own activities"
ON activities FOR SELECT
USING (user_id = auth.uid());

CREATE POLICY "Anyone can view public activities"
ON activities FOR SELECT
USING (is_public = true);

CREATE POLICY "Users can insert own activities"
ON activities FOR INSERT
WITH CHECK (user_id = auth.uid());

CREATE POLICY "Users can update own activities"
ON activities FOR UPDATE
USING (user_id = auth.uid());

CREATE POLICY "Users can delete own activities"
ON activities FOR DELETE
USING (user_id = auth.uid());

-- RLS Policies for track_points
CREATE POLICY "View track_points of accessible activities"
ON track_points FOR SELECT
USING (
    activity_id IN (
        SELECT id FROM activities
        WHERE user_id = auth.uid()
        OR is_public = true
    )
);

CREATE POLICY "Insert track_points for own activities"
ON track_points FOR INSERT
WITH CHECK (
    activity_id IN (
        SELECT id FROM activities WHERE user_id = auth.uid()
    )
);

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for activities
CREATE TRIGGER update_activities_updated_at
BEFORE UPDATE ON activities
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- Trigger for profiles
CREATE TRIGGER update_profiles_updated_at
BEFORE UPDATE ON profiles
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

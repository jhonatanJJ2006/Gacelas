import { z } from 'zod'
import { FREE_TIER, type TierProfile } from './tier'

export const trackPointSchema = z.object({
  lat: z.number().gte(-90).lte(90),
  lon: z.number().gte(-180).lte(180),
  timestamp: z.string().datetime(),
  elevationMeters: z.number().optional(),
  accuracyMeters: z.number().nonnegative().optional(),
  speedMps: z.number().nonnegative().optional(),
  bearingDegrees: z.number().gte(0).lte(360).optional(),
})

export const trackBatchSchema = z.object({
  activityId: z.string().uuid(),
  clientBatchId: z.string().min(1).max(128),
  points: z.array(trackPointSchema).min(1).max(FREE_TIER.maxBatchPoints),
})

export const activitySummarySchema = z.object({
  activityId: z.string().uuid(),
  pointCount: z.number().int().nonnegative(),
  distanceMeters: z.number().int().nonnegative().nullable(),
  durationSeconds: z.number().int().nonnegative().nullable(),
  updatedAt: z.string().datetime(),
})

export type TrackPoint = z.infer<typeof trackPointSchema>
export type TrackBatch = z.infer<typeof trackBatchSchema>
export type ActivitySummary = z.infer<typeof activitySummarySchema>

export function assertPointSpacing(
  points: readonly { timestamp: string }[],
  tier: TierProfile = FREE_TIER,
  previousTimestamp?: string
): void {
  let last = previousTimestamp === undefined ? undefined : Date.parse(previousTimestamp)
  if (last !== undefined && Number.isNaN(last)) {
    throw new Error('invalid timestamp')
  }

  for (const point of points) {
    const current = Date.parse(point.timestamp)
    if (Number.isNaN(current)) {
      throw new Error('invalid timestamp')
    }
    if (last !== undefined && current - last < tier.minPointIntervalMs) {
      throw new Error('points exceed 1 Hz')
    }
    last = current
  }
}

export function parseTrackBatch(input: unknown, tier: TierProfile = FREE_TIER): TrackBatch {
  const batch = trackBatchSchema.parse(input)
  if (batch.points.length > tier.maxBatchPoints) {
    throw new Error('batch exceeds point limit')
  }
  assertPointSpacing(batch.points, tier)
  return batch
}

import contract from '../contract.json'

export const DESIGN_CAPACITY = contract.designCapacity

export const FREE_TIER = contract.free

export const PRO_TIER = contract.pro

export type TierId = 'free' | 'pro'

export type TierProfile = typeof FREE_TIER | typeof PRO_TIER

export function getTierProfile(id: TierId = 'free'): TierProfile {
  return id === 'pro' ? PRO_TIER : FREE_TIER
}

export type ChannelDecision = { ok: true; activityId: string } | { ok: false; reason: 'quota' }

export function tryOpenChannel(
  openActivityIds: readonly string[],
  activityId: string,
  tier: TierProfile = FREE_TIER
): ChannelDecision {
  if (openActivityIds.includes(activityId)) {
    return { ok: true, activityId }
  }
  if (openActivityIds.length >= tier.maxChannelsPerClient) {
    return { ok: false, reason: 'quota' }
  }
  return { ok: true, activityId }
}

export function canSendPosition(
  lastSentAtMs: number | null,
  nowMs: number,
  tier: TierProfile = FREE_TIER
): boolean {
  if (lastSentAtMs === null) return true
  return nowMs - lastSentAtMs >= tier.positionIntervalMs
}

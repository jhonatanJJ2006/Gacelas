import { describe, expect, it } from 'vitest'
import contract from '../contract.json'
import {
  DESIGN_CAPACITY,
  FREE_TIER,
  PRO_TIER,
  canSendPosition,
  parseTrackBatch,
  tryOpenChannel,
} from './index'

describe('tier contract', () => {
  it('matches the published free and pro budgets', () => {
    expect(DESIGN_CAPACITY).toEqual(contract.designCapacity)
    expect(FREE_TIER).toEqual(contract.free)
    expect(PRO_TIER).toEqual(contract.pro)
    expect(FREE_TIER.maxRealtimeConnections).toBeLessThan(FREE_TIER.platformRealtimeConnections)
    expect(FREE_TIER.allowPositionPolling).toBe(false)
    expect(FREE_TIER.maxChannelsPerClient).toBe(1)
    expect(FREE_TIER.maxBatchPoints).toBe(50)
    expect(FREE_TIER.positionIntervalMs).toBe(5000)
  })

  it('refuses a second live channel on the free profile', () => {
    const first = tryOpenChannel([], 'activity-1', FREE_TIER)
    expect(first.ok).toBe(true)
    const second = tryOpenChannel(['activity-1'], 'activity-2', FREE_TIER)
    expect(second).toEqual({ ok: false, reason: 'quota' })
  })

  it('allows reopening the same channel', () => {
    expect(tryOpenChannel(['activity-1'], 'activity-1', FREE_TIER).ok).toBe(true)
  })

  it('waits five seconds between live positions', () => {
    expect(canSendPosition(0, 4999, FREE_TIER)).toBe(false)
    expect(canSendPosition(0, 5000, FREE_TIER)).toBe(true)
  })

  it('rejects points faster than 1 Hz and batches over 50', () => {
    const point = {
      lat: -0.18,
      lon: -78.46,
      timestamp: '2026-10-14T12:00:00.000Z',
    }
    expect(() =>
      parseTrackBatch({
        activityId: '11111111-1111-4111-8111-111111111111',
        clientBatchId: 'batch-1',
        points: [point, { ...point, timestamp: '2026-10-14T12:00:00.500Z' }],
      })
    ).toThrow(/1 Hz/)

    expect(() =>
      parseTrackBatch({
        activityId: '11111111-1111-4111-8111-111111111111',
        clientBatchId: 'batch-2',
        points: Array.from({ length: 51 }, (_, index) => ({
          ...point,
          timestamp: new Date(Date.parse(point.timestamp) + index * 1000).toISOString(),
        })),
      })
    ).toThrow()
  })
})

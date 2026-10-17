import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'
import { FREE_TIER, PRO_TIER, DESIGN_CAPACITY } from './index'

const kotlinPath = fileURLToPath(
  new URL(
    '../../../apps/android/app/src/main/java/com/gacelas/core/contract/CapacityContract.kt',
    import.meta.url
  )
)

describe('android capacity contract', () => {
  const source = readFileSync(kotlinPath, 'utf8')

  it('keeps the same free and pro budgets as contract.json', () => {
    expect(source).toContain(`DESIGN_CONCURRENT_SESSIONS = ${DESIGN_CAPACITY.concurrentSessions}`)
    expect(source).toContain(`DESIGN_LIVE_PUBLISHERS = ${DESIGN_CAPACITY.livePublishers}`)
    expect(source).toContain(`DESIGN_EVENT_VIEWERS = ${DESIGN_CAPACITY.eventViewers}`)
    expect(source).toContain(`FREE_MAX_REALTIME_CONNECTIONS = ${FREE_TIER.maxRealtimeConnections}`)
    expect(source).toContain(
      `FREE_PLATFORM_REALTIME_CONNECTIONS = ${FREE_TIER.platformRealtimeConnections}`
    )
    expect(source).toContain(`FREE_MAX_MESSAGES_PER_SECOND = ${FREE_TIER.maxMessagesPerSecond}`)
    expect(source).toContain(`FREE_MAX_CHANNELS_PER_CLIENT = ${FREE_TIER.maxChannelsPerClient}`)
    expect(source).toContain(`POSITION_INTERVAL_MS = ${FREE_TIER.positionIntervalMs}`)
    expect(source).toContain(`MAX_BATCH_POINTS = ${FREE_TIER.maxBatchPoints}`)
    expect(source).toContain(`MIN_POINT_INTERVAL_MS = ${FREE_TIER.minPointIntervalMs}`)
    expect(source).toContain(`ALLOW_POSITION_POLLING = ${FREE_TIER.allowPositionPolling}`)
    expect(source).toContain(`PRO_MAX_REALTIME_CONNECTIONS = ${PRO_TIER.maxRealtimeConnections}`)
    expect(source).toContain(`PRO_MAX_MESSAGES_PER_SECOND = ${PRO_TIER.maxMessagesPerSecond}`)
    expect(source).toContain(`PRO_MAX_MESSAGES_PER_MONTH = ${PRO_TIER.maxMessagesPerMonth}`)
  })
})

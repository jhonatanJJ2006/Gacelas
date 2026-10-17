package com.gacelas.core.contract

object CapacityContract {
    const val DESIGN_CONCURRENT_SESSIONS = 3000
    const val DESIGN_LIVE_PUBLISHERS = 500
    const val DESIGN_EVENT_VIEWERS = 1000

    const val FREE_MAX_REALTIME_CONNECTIONS = 150
    const val FREE_PLATFORM_REALTIME_CONNECTIONS = 200
    const val FREE_MAX_MESSAGES_PER_SECOND = 80
    const val FREE_PLATFORM_MESSAGES_PER_SECOND = 100
    const val FREE_MAX_MESSAGES_PER_MONTH = 2000000
    const val FREE_MAX_CHANNELS_PER_CLIENT = 1
    const val POSITION_INTERVAL_MS = 5000
    const val MAX_BATCH_POINTS = 50
    const val MIN_POINT_INTERVAL_MS = 1000
    const val ALLOW_POSITION_POLLING = false

    const val PRO_MAX_REALTIME_CONNECTIONS = 500
    const val PRO_PLATFORM_REALTIME_CONNECTIONS = 500
    const val PRO_MAX_MESSAGES_PER_SECOND = 400
    const val PRO_PLATFORM_MESSAGES_PER_SECOND = 500
    const val PRO_MAX_MESSAGES_PER_MONTH = 5000000
    const val PRO_MAX_CHANNELS_PER_CLIENT = 1
}

data class TrackPointDto(
    val lat: Double,
    val lon: Double,
    val timestamp: String,
    val elevationMeters: Double? = null,
    val accuracyMeters: Double? = null,
    val speedMps: Double? = null,
    val bearingDegrees: Double? = null,
)

data class TrackBatchDto(
    val activityId: String,
    val clientBatchId: String,
    val points: List<TrackPointDto>,
)

data class ActivitySummaryDto(
    val activityId: String,
    val pointCount: Int,
    val distanceMeters: Int?,
    val durationSeconds: Int?,
    val updatedAt: String,
)

fun tryOpenChannel(
    openActivityIds: List<String>,
    activityId: String,
    maxChannels: Int = CapacityContract.FREE_MAX_CHANNELS_PER_CLIENT,
): Boolean {
    if (activityId in openActivityIds) return true
    return openActivityIds.size < maxChannels
}

fun canSendPosition(
    lastSentAtMs: Long?,
    nowMs: Long,
    intervalMs: Long = CapacityContract.POSITION_INTERVAL_MS.toLong(),
): Boolean {
    if (lastSentAtMs == null) return true
    return nowMs - lastSentAtMs >= intervalMs
}

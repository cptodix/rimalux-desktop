package com.cptodix.rimalux.desktop.scheduler

import kotlin.math.max

/**
 * Pure scheduling policy for desktop auto-change. Kept independent of Android/Compose so it can
 * be tested with a fake clock.
 */
object AutoChangePolicy {
    const val MIN_INTERVAL_MINUTES = 1
    const val MAX_INTERVAL_MINUTES = 10_080
    const val MIN_DEDUPE_MILLIS = 30_000L

    fun clampIntervalMinutes(minutes: Int): Int =
        minutes.coerceIn(MIN_INTERVAL_MINUTES, MAX_INTERVAL_MINUTES)

    fun intervalMillis(minutes: Int): Long =
        clampIntervalMinutes(minutes) * 60_000L

    fun dedupeWindowMillis(minutes: Int): Long =
        max(MIN_DEDUPE_MILLIS, intervalMillis(minutes) * 3L / 4L)

    fun isDue(enabled: Boolean, nextRunEpochMillis: Long, nowEpochMillis: Long): Boolean =
        enabled && nextRunEpochMillis > 0L && nowEpochMillis >= nextRunEpochMillis

    fun nextRunFrom(nowEpochMillis: Long, minutes: Int): Long =
        nowEpochMillis + intervalMillis(minutes)

    fun shouldSkip(lastRunEpochMillis: Long, nowEpochMillis: Long, minutes: Int): Boolean {
        if (lastRunEpochMillis <= 0L) return false
        val elapsed = nowEpochMillis - lastRunEpochMillis
        return elapsed in -60_000L until dedupeWindowMillis(minutes)
    }

    fun isValidFutureTarget(
        nextRunEpochMillis: Long,
        nowEpochMillis: Long,
        minutes: Int
    ): Boolean =
        nextRunEpochMillis > nowEpochMillis + 5_000L &&
            nextRunEpochMillis <= nowEpochMillis + intervalMillis(minutes) + 60_000L
}

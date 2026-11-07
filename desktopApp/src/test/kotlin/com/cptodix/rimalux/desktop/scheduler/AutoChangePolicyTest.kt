package com.cptodix.rimalux.desktop.scheduler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoChangePolicyTest {

    @Test
    fun intervalAndDedupeWindowsAreClamped() {
        assertEquals(1, AutoChangePolicy.clampIntervalMinutes(0))
        assertEquals(10_080, AutoChangePolicy.clampIntervalMinutes(20_000))
        assertEquals(60 * 60_000L, AutoChangePolicy.intervalMillis(60))
        assertEquals(45_000L, AutoChangePolicy.dedupeWindowMillis(1))
        assertEquals(45 * 60_000L, AutoChangePolicy.dedupeWindowMillis(60))
    }

    @Test
    fun dueAndDedupeRules() {
        val now = 1_000_000L
        assertFalse(AutoChangePolicy.isDue(false, now - 1, now))
        assertFalse(AutoChangePolicy.isDue(true, 0L, now))
        assertFalse(AutoChangePolicy.isDue(true, now + 1, now))
        assertTrue(AutoChangePolicy.isDue(true, now, now))

        assertFalse(AutoChangePolicy.shouldSkip(0L, now, 60))
        assertTrue(AutoChangePolicy.shouldSkip(now - 10_000L, now, 60))
        assertFalse(AutoChangePolicy.shouldSkip(now - 60 * 60_000L, now, 60))
    }

    @Test
    fun nextRunIsAlwaysFuture() {
        val now = 1_000_000L
        assertEquals(now + 60 * 60_000L, AutoChangePolicy.nextRunFrom(now, 60))
        assertTrue(AutoChangePolicy.isValidFutureTarget(now + 60_000L, now, 60))
        assertFalse(AutoChangePolicy.isValidFutureTarget(now - 1L, now, 60))
    }
}

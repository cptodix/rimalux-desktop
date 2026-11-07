package com.cptodix.rimalux.desktop.platform

import org.junit.Assert.assertTrue
import org.junit.Test

class OsSchedulerTest {

    @Test
    fun systemdUnitContentIsWellFormed() {
        val service = OsScheduler.buildService("/opt/rimalux/bin/Rimalux")
        assertTrue(service.contains("[Unit]"))
        assertTrue(service.contains("[Service]"))
        assertTrue(service.contains("Type=oneshot"))
        assertTrue(service.contains("ExecStart=\"/opt/rimalux/bin/Rimalux\" --rotate-now"))

        val timer = OsScheduler.buildTimer(90)
        assertTrue(timer.contains("[Timer]"))
        assertTrue(timer.contains("OnUnitActiveSec=90min"))
        assertTrue(timer.contains("Persistent=true"))
        assertTrue(timer.contains("Unit=rimalux-rotate.service"))
        assertTrue(timer.contains("WantedBy=timers.target"))
    }

    @Test
    fun timerIntervalIsClamped() {
        assertTrue(OsScheduler.buildTimer(0).contains("OnUnitActiveSec=1min"))
        assertTrue(OsScheduler.buildTimer(99_999).contains("OnUnitActiveSec=10080min"))
    }
}

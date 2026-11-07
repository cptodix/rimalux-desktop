package com.cptodix.rimalux.desktop.platform

import java.nio.file.Files
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SingleInstanceManagerTest {

    @Test
    fun secondInstanceNotifiesTheFirst() {
        runBlocking {
            val root = Files.createTempDirectory("rimalux-single-instance-test")
            val paths = PlatformPaths(
                osName = "Linux",
                environment = mapOf(
                    "XDG_CONFIG_HOME" to root.resolve("config").toString(),
                    "XDG_CACHE_HOME" to root.resolve("cache").toString(),
                    "XDG_STATE_HOME" to root.resolve("state").toString()
                ),
                userHome = root
            )
            val first = SingleInstanceManager(paths)
            val second = SingleInstanceManager(paths)
            try {
                assertTrue(first.acquire())
                assertFalse(second.acquire())
                assertTrue(second.notifyExisting())
                withTimeout(5_000L) {
                    first.showSignal.first { it > 0L }
                }
            } finally {
                second.close()
                first.close()
            }
        }
    }
}

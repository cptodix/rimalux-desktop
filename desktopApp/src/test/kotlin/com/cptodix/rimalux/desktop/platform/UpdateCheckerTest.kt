package com.cptodix.rimalux.desktop.platform

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun comparesDottedVersions() {
        assertTrue(UpdateChecker.isNewer("0.2.0", "0.1.0"))
        assertTrue(UpdateChecker.isNewer("1.0.0", "0.9.9"))
        assertTrue(UpdateChecker.isNewer("v1.2.3", "1.2.2"))
        assertFalse(UpdateChecker.isNewer("1.2.3", "1.2.3"))
        assertFalse(UpdateChecker.isNewer("1.2.2", "1.2.3"))
        assertFalse(UpdateChecker.isNewer("1.2.3-beta", "1.2.3"))
        assertTrue(UpdateChecker.isNewer("1.2.4-beta", "1.2.3"))
    }
}

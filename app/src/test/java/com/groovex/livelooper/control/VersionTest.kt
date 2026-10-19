package com.groovex.livelooper.control

import com.groovex.livelooper.control.update.Version
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionTest {
    private fun v(s: String) = Version.parse(s)!!

    @Test fun parsesTagsAndDefaults() {
        assertEquals(Version(1, 2, 3, null), v("v1.2.3"))
        assertEquals(Version(0, 2, 0, null), v("0.2"))
        assertEquals(Version(0, 1, 0, null), v("0.1.0-debug"))
        assertNull(Version.parse("latest"))
    }

    @Test fun ordersReleasesAndPrereleases() {
        assertTrue(v("0.2.0") > v("0.1.9"))
        assertTrue(v("1.0.0") > v("1.0.0-beta.2"))
        assertTrue(v("1.0.0-beta.10") > v("1.0.0-beta.2"))
        assertTrue(v("0.10.0") > v("0.9.0"))
        assertEquals(0, v("v0.3.1").compareTo(v("0.3.1")))
    }
}

package com.autohub.android.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserUserAgentModeTest {
    private val mobileUserAgent =
        "Mozilla/5.0 (Linux; Android 14; SM-A235F Build/UP1A; wv) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 " +
            "Chrome/152.0.0.0 Mobile Safari/537.36"

    @Test
    fun `mobile mode preserves original user agent`() {
        assertEquals(
            mobileUserAgent,
            BrowserUserAgentPolicy.userAgentFor(
                BrowserUserAgentMode.MOBILE,
                mobileUserAgent,
            ),
        )
    }

    @Test
    fun `desktop mode removes android webview markers and keeps browser version`() {
        val desktop = BrowserUserAgentPolicy.userAgentFor(
            BrowserUserAgentMode.DESKTOP,
            mobileUserAgent,
        )

        assertTrue(desktop.contains("(X11; Linux x86_64)"))
        assertTrue(desktop.contains("Chrome/152.0.0.0"))
        assertFalse(desktop.contains("Android"))
        assertFalse(desktop.contains("; wv"))
        assertFalse(desktop.contains("Version/4.0"))
        assertFalse(desktop.contains(" Mobile"))
    }

    @Test
    fun `stored mode falls back safely to mobile`() {
        assertEquals(BrowserUserAgentMode.MOBILE, BrowserUserAgentMode.fromStorage(null))
        assertEquals(BrowserUserAgentMode.MOBILE, BrowserUserAgentMode.fromStorage("UNKNOWN"))
        assertEquals(BrowserUserAgentMode.DESKTOP, BrowserUserAgentMode.fromStorage("DESKTOP"))
    }
}

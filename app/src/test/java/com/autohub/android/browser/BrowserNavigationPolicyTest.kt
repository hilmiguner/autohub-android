package com.autohub.android.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserNavigationPolicyTest {
    @Test
    fun `normalizes host-like input to https`() {
        assertEquals(
            "https://example.com/path",
            BrowserNavigationPolicy.normalizeUserInput("example.com/path"),
        )
    }

    @Test
    fun `preserves valid https URL`() {
        assertEquals(
            "https://example.com/path?q=1",
            BrowserNavigationPolicy.normalizeUserInput("https://example.com/path?q=1"),
        )
    }

    @Test
    fun `allows ordinary http and https navigation`() {
        assertTrue(BrowserNavigationPolicy.isAllowedAbsoluteUrl("https://example.com"))
        assertTrue(BrowserNavigationPolicy.isAllowedAbsoluteUrl("http://localhost:8080/test"))
    }

    @Test
    fun `blocks non web schemes`() {
        assertFalse(BrowserNavigationPolicy.isAllowedAbsoluteUrl("file:///tmp/test.html"))
        assertFalse(BrowserNavigationPolicy.isAllowedAbsoluteUrl("content://com.example/item/1"))
        assertFalse(BrowserNavigationPolicy.isAllowedAbsoluteUrl("javascript:alert(1)"))
    }

    @Test
    fun `rejects blank or malformed input`() {
        assertNull(BrowserNavigationPolicy.normalizeUserInput("   "))
        assertNull(BrowserNavigationPolicy.normalizeUserInput("https://"))
        assertNull(BrowserNavigationPolicy.normalizeUserInput("not a valid host"))
    }
}

package com.autohub.android.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrowserPersistencePolicyTest {
    @Test
    fun `keeps valid persisted https url`() {
        assertEquals(
            "https://example.com/path?q=1",
            BrowserPersistencePolicy.sanitizePersistedUrl("https://example.com/path?q=1"),
        )
    }

    @Test
    fun `trims valid persisted url`() {
        assertEquals(
            "https://example.com",
            BrowserPersistencePolicy.sanitizePersistedUrl("  https://example.com  "),
        )
    }

    @Test
    fun `rejects blocked persisted schemes`() {
        assertNull(BrowserPersistencePolicy.sanitizePersistedUrl("file:///sdcard/test.html"))
        assertNull(BrowserPersistencePolicy.sanitizePersistedUrl("javascript:alert(1)"))
        assertNull(BrowserPersistencePolicy.sanitizePersistedUrl("content://example/item"))
    }

    @Test
    fun `rejects null or blank persisted url`() {
        assertNull(BrowserPersistencePolicy.sanitizePersistedUrl(null))
        assertNull(BrowserPersistencePolicy.sanitizePersistedUrl("   "))
    }
}

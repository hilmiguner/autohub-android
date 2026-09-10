package com.autohub.android.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrowserDownloadPolicyTest {
    @Test
    fun `creates download spec for allowed https url`() {
        val spec = BrowserDownloadPolicy.create(
            url = " https://example.com/files/report.pdf ",
            suggestedFileName = "report.pdf",
            mimeType = " application/pdf ",
        )

        assertEquals("https://example.com/files/report.pdf", spec?.url)
        assertEquals("report.pdf", spec?.fileName)
        assertEquals("application/pdf", spec?.mimeType)
    }

    @Test
    fun `rejects non http download urls`() {
        assertNull(BrowserDownloadPolicy.create("file:///sdcard/report.pdf", "report.pdf", null))
        assertNull(BrowserDownloadPolicy.create("content://downloads/report.pdf", "report.pdf", null))
        assertNull(BrowserDownloadPolicy.create("javascript:alert(1)", "report.pdf", null))
        assertNull(BrowserDownloadPolicy.create("blob:https://example.com/id", "report.pdf", null))
        assertNull(BrowserDownloadPolicy.create("data:text/plain,hello", "report.txt", null))
    }

    @Test
    fun `sanitizes suggested file name`() {
        assertEquals(
            "unsafe_name___.pdf",
            BrowserDownloadPolicy.sanitizeFileName("../unsafe/name?:.pdf"),
        )
    }

    @Test
    fun `uses fallback for blank or path only names`() {
        assertEquals("download", BrowserDownloadPolicy.sanitizeFileName(null))
        assertEquals("download", BrowserDownloadPolicy.sanitizeFileName("   "))
        assertEquals("download", BrowserDownloadPolicy.sanitizeFileName("..."))
    }

    @Test
    fun `caps file name length`() {
        val longName = "a".repeat(200)

        assertEquals(128, BrowserDownloadPolicy.sanitizeFileName(longName).length)
    }
}

package com.autohub.android.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserProviderBridgePolicyTest {
    private val descriptor = BrowserProviderDescriptor(
        providerId = "demo.provider",
        allowedOrigins = setOf("https://media.example.com"),
        capabilities = setOf(
            BrowserProviderBridgeCapability.MEDIA_METADATA,
            BrowserProviderBridgeCapability.PLAYBACK_STATE,
        ),
    )

    @Test
    fun `accepts exact configured https origin`() {
        assertTrue(
            BrowserProviderBridgePolicy.isAllowedPage(
                "https://media.example.com/watch?id=42",
                descriptor,
            ),
        )
    }

    @Test
    fun `normalizes default https port and host case`() {
        assertEquals(
            "https://media.example.com",
            BrowserProviderBridgePolicy.normalizeHttpsOrigin(
                "https://MEDIA.EXAMPLE.COM:443/path",
            ),
        )
    }

    @Test
    fun `rejects http sibling and subdomain origins`() {
        assertFalse(
            BrowserProviderBridgePolicy.isAllowedPage(
                "http://media.example.com/watch",
                descriptor,
            ),
        )
        assertFalse(
            BrowserProviderBridgePolicy.isAllowedPage(
                "https://example.com/watch",
                descriptor,
            ),
        )
        assertFalse(
            BrowserProviderBridgePolicy.isAllowedPage(
                "https://cdn.media.example.com/watch",
                descriptor,
            ),
        )
    }

    @Test
    fun `rejects invalid provider descriptor`() {
        val invalidId = descriptor.copy(providerId = "Demo Provider")
        val invalidOrigin = descriptor.copy(allowedOrigins = setOf("http://media.example.com"))

        assertFalse(BrowserProviderBridgePolicy.isValidDescriptor(invalidId))
        assertFalse(BrowserProviderBridgePolicy.isValidDescriptor(invalidOrigin))
    }

    @Test
    fun `sanitizes provider metadata`() {
        val metadata = BrowserProviderBridgePolicy.sanitizeMetadata(
            title = "  Test title  ",
            subtitle = "  Subtitle  ",
            artworkUrl = "https://cdn.example.com/art.jpg",
        )

        assertEquals("Test title", metadata?.title)
        assertEquals("Subtitle", metadata?.subtitle)
        assertEquals("https://cdn.example.com/art.jpg", metadata?.artworkUrl)
    }

    @Test
    fun `metadata requires a title and rejects non web artwork`() {
        assertNull(
            BrowserProviderBridgePolicy.sanitizeMetadata(
                title = "   ",
                subtitle = null,
                artworkUrl = null,
            ),
        )

        val metadata = BrowserProviderBridgePolicy.sanitizeMetadata(
            title = "Track",
            subtitle = null,
            artworkUrl = "javascript:alert(1)",
        )
        assertEquals("Track", metadata?.title)
        assertNull(metadata?.artworkUrl)
    }

    @Test
    fun `validates playback state bounds`() {
        assertEquals(
            BrowserProviderPlaybackState(
                isPlaying = true,
                positionMs = 5_000L,
                durationMs = 10_000L,
            ),
            BrowserProviderBridgePolicy.sanitizePlaybackState(
                isPlaying = true,
                positionMs = 5_000L,
                durationMs = 10_000L,
            ),
        )

        assertNull(
            BrowserProviderBridgePolicy.sanitizePlaybackState(
                isPlaying = false,
                positionMs = -1L,
                durationMs = 10_000L,
            ),
        )
        assertNull(
            BrowserProviderBridgePolicy.sanitizePlaybackState(
                isPlaying = false,
                positionMs = 11_000L,
                durationMs = 10_000L,
            ),
        )
    }
}

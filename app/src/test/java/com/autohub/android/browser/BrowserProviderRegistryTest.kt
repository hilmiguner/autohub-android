package com.autohub.android.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrowserProviderRegistryTest {
    private val provider = BrowserProviderDescriptor(
        providerId = "demo.provider",
        allowedOrigins = setOf("https://media.example.com"),
        capabilities = setOf(BrowserProviderBridgeCapability.MEDIA_METADATA),
    )

    @Test
    fun `resolves registered provider from exact page origin`() {
        val registry = BrowserProviderRegistry(listOf(provider))

        assertEquals(
            provider,
            registry.resolveForPage("https://media.example.com/watch/42"),
        )
        assertNull(registry.resolveForPage("https://cdn.media.example.com/watch/42"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects duplicate provider ids`() {
        BrowserProviderRegistry(
            listOf(
                provider,
                provider.copy(allowedOrigins = setOf("https://other.example.com")),
            ),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects origin shared by multiple providers`() {
        BrowserProviderRegistry(
            listOf(
                provider,
                provider.copy(providerId = "other.provider"),
            ),
        )
    }
}

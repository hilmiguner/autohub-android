package com.autohub.android.browser

import java.net.URI
import java.util.Locale

enum class BrowserProviderBridgeCapability {
    MEDIA_METADATA,
    PLAYBACK_STATE,
    PLAYBACK_COMMANDS,
}

data class BrowserProviderDescriptor(
    val providerId: String,
    val allowedOrigins: Set<String>,
    val capabilities: Set<BrowserProviderBridgeCapability>,
)

data class BrowserProviderMediaMetadata(
    val title: String,
    val subtitle: String?,
    val artworkUrl: String?,
)

data class BrowserProviderPlaybackState(
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long?,
)

internal object BrowserProviderBridgePolicy {
    fun isValidDescriptor(descriptor: BrowserProviderDescriptor): Boolean =
        isValidProviderId(descriptor.providerId) &&
            descriptor.allowedOrigins.isNotEmpty() &&
            descriptor.allowedOrigins.all { normalizeHttpsOrigin(it) != null }

    fun isAllowedPage(
        pageUrl: String?,
        descriptor: BrowserProviderDescriptor,
    ): Boolean {
        if (!isValidDescriptor(descriptor)) return false
        val pageOrigin = normalizeHttpsOrigin(pageUrl) ?: return false
        return descriptor.allowedOrigins
            .asSequence()
            .mapNotNull(::normalizeHttpsOrigin)
            .any { it == pageOrigin }
    }

    fun sanitizeMetadata(
        title: String?,
        subtitle: String?,
        artworkUrl: String?,
    ): BrowserProviderMediaMetadata? {
        val safeTitle = sanitizeText(title, MAX_TITLE_LENGTH) ?: return null
        return BrowserProviderMediaMetadata(
            title = safeTitle,
            subtitle = sanitizeText(subtitle, MAX_SUBTITLE_LENGTH),
            artworkUrl = BrowserPersistencePolicy.sanitizePersistedUrl(artworkUrl),
        )
    }

    fun sanitizePlaybackState(
        isPlaying: Boolean,
        positionMs: Long,
        durationMs: Long?,
    ): BrowserProviderPlaybackState? {
        if (positionMs < 0L) return null
        if (durationMs != null && durationMs < 0L) return null
        if (durationMs != null && positionMs > durationMs) return null
        return BrowserProviderPlaybackState(
            isPlaying = isPlaying,
            positionMs = positionMs,
            durationMs = durationMs,
        )
    }

    fun normalizeHttpsOrigin(value: String?): String? {
        val trimmed = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val uri = runCatching { URI(trimmed) }.getOrNull() ?: return null
        if (!uri.scheme.equals("https", ignoreCase = true)) return null
        if (uri.host.isNullOrBlank()) return null
        if (uri.userInfo != null) return null

        val host = uri.host.lowercase(Locale.ROOT)
        val port = when (uri.port) {
            -1, DEFAULT_HTTPS_PORT -> ""
            else -> ":${uri.port}"
        }
        return "https://$host$port"
    }

    private fun isValidProviderId(value: String): Boolean =
        PROVIDER_ID_PATTERN.matches(value)

    private fun sanitizeText(
        value: String?,
        maxLength: Int,
    ): String? =
        value
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.take(maxLength)

    private const val DEFAULT_HTTPS_PORT = 443
    private const val MAX_TITLE_LENGTH = 256
    private const val MAX_SUBTITLE_LENGTH = 512
    private val PROVIDER_ID_PATTERN = Regex("[a-z0-9][a-z0-9._-]{0,63}")
}

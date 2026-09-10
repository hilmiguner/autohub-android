package com.autohub.android.tv

import java.net.URI
import java.security.MessageDigest

@JvmInline
value class TvPlaylistSourceId(val value: String) {
    init {
        require(value.matches(ID_PATTERN)) { "Invalid playlist source ID." }
    }

    private companion object {
        val ID_PATTERN = Regex("[A-Za-z0-9._-]{1,64}")
    }
}

data class TvPlaylistSource(
    val id: TvPlaylistSourceId,
    val name: String,
    val url: String,
)

object TvPlaylistSourcePolicy {
    fun createRemoteHttp(
        name: String?,
        url: String?,
    ): TvPlaylistSource? {
        val normalizedUrl = normalizeRemoteHttpUrl(url) ?: return null
        val sourceName = name
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: runCatching { URI(normalizedUrl).host }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_SOURCE_NAME

        return TvPlaylistSource(
            id = TvPlaylistSourceId(
                SOURCE_ID_PREFIX + sha256(normalizedUrl).take(SOURCE_HASH_LENGTH),
            ),
            name = sourceName.take(MAX_SOURCE_NAME_LENGTH),
            url = normalizedUrl,
        )
    }

    internal fun normalizeRemoteHttpUrl(rawValue: String?): String? {
        val value = rawValue?.trim().orEmpty()
        if (value.isEmpty()) return null

        return runCatching {
            val uri = URI(value)
            val scheme = uri.scheme?.lowercase()
            if (scheme !in ALLOWED_SCHEMES || uri.host.isNullOrBlank()) {
                return@runCatching null
            }

            val normalizedPort = when {
                scheme == "http" && uri.port == 80 -> -1
                scheme == "https" && uri.port == 443 -> -1
                else -> uri.port
            }

            URI(
                scheme,
                uri.userInfo,
                uri.host.lowercase(),
                normalizedPort,
                uri.path.takeUnless { it.isNullOrEmpty() } ?: "/",
                uri.query,
                null,
            ).normalize().toASCIIString()
        }.getOrNull()
    }

    internal fun sha256(value: String): String {
        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
        val chars = CharArray(bytes.size * 2)
        bytes.forEachIndexed { index, byte ->
            val unsigned = byte.toInt() and 0xff
            chars[index * 2] = HEX[unsigned ushr 4]
            chars[index * 2 + 1] = HEX[unsigned and 0x0f]
        }
        return String(chars)
    }

    private const val SOURCE_ID_PREFIX = "src_"
    private const val SOURCE_HASH_LENGTH = 32
    private const val MAX_SOURCE_NAME_LENGTH = 120
    private const val DEFAULT_SOURCE_NAME = "Playlist"
    private const val HEX = "0123456789abcdef"
    private val ALLOWED_SCHEMES = setOf("http", "https")
}

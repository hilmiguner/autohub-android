package com.autohub.android.tv

import java.net.URI

class M3uPlaylistParser {
    fun parse(content: String): M3uParseResult {
        val lines = content
            .lineSequence()
            .mapIndexed { index, rawLine ->
                ParsedLine(
                    number = index + 1,
                    value = if (index == 0) {
                        rawLine.removePrefix("\uFEFF").trim()
                    } else {
                        rawLine.trim()
                    },
                )
            }
            .toList()

        val hlsManifestMarker = lines.firstOrNull { line ->
            HLS_MANIFEST_MARKERS.any { marker ->
                line.value.startsWith(marker, ignoreCase = true)
            }
        }
        if (hlsManifestMarker != null) {
            return M3uParseResult(
                playlist = TvPlaylist(),
                issues = listOf(
                    M3uParseIssue(
                        lineNumber = hlsManifestMarker.number,
                        message = "HLS media/master manifests are playback resources, not TV channel playlists.",
                    ),
                ),
            )
        }

        val issues = mutableListOf<M3uParseIssue>()
        val channels = mutableListOf<TvChannel>()
        val epgUrls = linkedSetOf<String>()
        var playlistName: String? = null
        var pendingChannel: PendingChannel? = null

        val firstContentLine = lines.firstOrNull { it.value.isNotEmpty() }
        if (firstContentLine?.value?.startsWith(EXT_M3U, ignoreCase = true) == true) {
            val headerAttributes = parseAttributes(firstContentLine.value.removePrefixIgnoreCase(EXT_M3U))
            playlistName = cleanText(
                headerAttributes["playlist-name"] ?: headerAttributes["name"],
            )
            EPG_ATTRIBUTE_KEYS.forEach { key ->
                headerAttributes[key]
                    ?.split(',')
                    ?.mapNotNull(::normalizeHttpUrl)
                    ?.forEach(epgUrls::add)
            }
        } else {
            issues += M3uParseIssue(
                lineNumber = firstContentLine?.number ?: 1,
                message = "Missing #EXTM3U header; attempting tolerant playlist parsing.",
            )
        }

        lines.forEach { parsedLine ->
            val line = parsedLine.value
            if (line.isEmpty()) return@forEach
            if (line.startsWith(EXT_M3U, ignoreCase = true)) return@forEach

            when {
                line.startsWith(EXT_INF, ignoreCase = true) -> {
                    if (pendingChannel != null) {
                        issues += M3uParseIssue(
                            lineNumber = parsedLine.number,
                            message = "Previous #EXTINF entry has no stream URL.",
                        )
                    }
                    pendingChannel = parseExtInf(line)
                }

                line.startsWith(EXT_GRP, ignoreCase = true) -> {
                    val group = cleanText(line.substringAfter(':', ""))
                    if (pendingChannel != null && pendingChannel?.group == null && group != null) {
                        pendingChannel = pendingChannel?.copy(group = group)
                    }
                }

                line.startsWith('#') -> Unit

                else -> {
                    val streamUrl = normalizeHttpUrl(line)
                    if (streamUrl == null) {
                        issues += M3uParseIssue(
                            lineNumber = parsedLine.number,
                            message = "Unsupported or invalid stream URL; only absolute HTTP(S) URLs are accepted.",
                        )
                        pendingChannel = null
                        return@forEach
                    }

                    val metadata = pendingChannel
                    channels += TvChannel(
                        name = metadata?.displayName
                            ?: metadata?.tvgName
                            ?: fallbackChannelName(streamUrl),
                        streamUrl = streamUrl,
                        group = metadata?.group,
                        tvgId = metadata?.tvgId,
                        tvgName = metadata?.tvgName,
                        logoUrl = metadata?.logoUrl,
                    )
                    pendingChannel = null
                }
            }
        }

        if (pendingChannel != null) {
            issues += M3uParseIssue(
                lineNumber = lines.lastOrNull()?.number ?: 1,
                message = "Playlist ended before the final #EXTINF entry received a stream URL.",
            )
        }

        return M3uParseResult(
            playlist = TvPlaylist(
                name = playlistName,
                epgUrls = epgUrls.toList(),
                channels = channels,
            ),
            issues = issues,
        )
    }

    private fun parseExtInf(line: String): PendingChannel {
        val payload = line.substringAfter(':', "")
        val (metadataSegment, displayNameSegment) = splitMetadataAndTitle(payload)
        val attributes = parseAttributes(metadataSegment)

        return PendingChannel(
            displayName = cleanText(displayNameSegment),
            group = cleanText(attributes["group-title"]),
            tvgId = cleanText(attributes["tvg-id"]),
            tvgName = cleanText(attributes["tvg-name"]),
            logoUrl = attributes["tvg-logo"]?.let(::normalizeHttpUrl),
        )
    }

    private fun splitMetadataAndTitle(payload: String): Pair<String, String?> {
        var quote: Char? = null
        var escaped = false

        payload.forEachIndexed { index, char ->
            if (escaped) {
                escaped = false
                return@forEachIndexed
            }
            if (char == '\\') {
                escaped = true
                return@forEachIndexed
            }

            when {
                quote != null && char == quote -> quote = null
                quote == null && (char == '"' || char == '\'') -> quote = char
                quote == null && char == ',' -> {
                    return payload.substring(0, index) to payload.substring(index + 1)
                }
            }
        }

        return payload to null
    }

    private fun parseAttributes(segment: String): Map<String, String> =
        ATTRIBUTE_PATTERN
            .findAll(segment)
            .associate { match ->
                val key = match.groupValues[1].lowercase()
                val value = match.groupValues
                    .drop(2)
                    .firstOrNull { it.isNotEmpty() }
                    .orEmpty()
                key to value
            }

    private fun normalizeHttpUrl(rawValue: String): String? {
        val value = rawValue.trim()
        if (value.isEmpty()) return null

        return runCatching {
            val uri = URI(value)
            val scheme = uri.scheme?.lowercase()
            value.takeIf {
                scheme in ALLOWED_STREAM_SCHEMES && !uri.host.isNullOrBlank()
            }
        }.getOrNull()
    }

    private fun fallbackChannelName(streamUrl: String): String {
        val uri = runCatching { URI(streamUrl) }.getOrNull()
        val pathName = uri
            ?.path
            ?.substringAfterLast('/')
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        return pathName ?: uri?.host ?: DEFAULT_CHANNEL_NAME
    }

    private fun cleanText(value: String?): String? =
        value
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    private fun String.removePrefixIgnoreCase(prefix: String): String =
        if (startsWith(prefix, ignoreCase = true)) {
            substring(prefix.length)
        } else {
            this
        }

    private data class ParsedLine(
        val number: Int,
        val value: String,
    )

    private data class PendingChannel(
        val displayName: String?,
        val group: String?,
        val tvgId: String?,
        val tvgName: String?,
        val logoUrl: String?,
    )

    private companion object {
        const val EXT_M3U = "#EXTM3U"
        const val EXT_INF = "#EXTINF:"
        const val EXT_GRP = "#EXTGRP:"
        const val DEFAULT_CHANNEL_NAME = "Channel"

        val ALLOWED_STREAM_SCHEMES = setOf("http", "https")
        val EPG_ATTRIBUTE_KEYS = listOf("x-tvg-url", "url-tvg", "tvg-url")
        val HLS_MANIFEST_MARKERS = listOf(
            "#EXT-X-STREAM-INF",
            "#EXT-X-TARGETDURATION",
            "#EXT-X-MEDIA-SEQUENCE",
        )
        val ATTRIBUTE_PATTERN =
            Regex("""([A-Za-z0-9_-]+)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s]+))""")
    }
}

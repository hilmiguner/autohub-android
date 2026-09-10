package com.autohub.android.tv

@JvmInline
value class TvChannelId(val value: String)

data class IdentifiedTvChannel(
    val id: TvChannelId,
    val sourceId: TvPlaylistSourceId,
    val channel: TvChannel,
)

object TvChannelIdentityPolicy {
    fun identify(
        sourceId: TvPlaylistSourceId,
        channels: List<TvChannel>,
    ): List<IdentifiedTvChannel> {
        val tvgIdCounts = channels
            .mapNotNull { normalizeTvgId(it.tvgId) }
            .groupingBy { it }
            .eachCount()

        return channels.map { channel ->
            val normalizedTvgId = normalizeTvgId(channel.tvgId)
            val logicalKey = if (
                normalizedTvgId != null && tvgIdCounts[normalizedTvgId] == 1
            ) {
                "tvg-id:$normalizedTvgId"
            } else {
                "stream:${canonicalStreamIdentity(channel.streamUrl)}"
            }

            IdentifiedTvChannel(
                id = TvChannelId(
                    CHANNEL_ID_PREFIX + TvPlaylistSourcePolicy
                        .sha256("${sourceId.value}\u0000$logicalKey")
                        .take(CHANNEL_HASH_LENGTH),
                ),
                sourceId = sourceId,
                channel = channel,
            )
        }
    }

    private fun normalizeTvgId(value: String?): String? =
        value
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotEmpty() }

    private fun canonicalStreamIdentity(url: String): String =
        TvPlaylistSourcePolicy.normalizeRemoteHttpUrl(url) ?: url.trim()

    private const val CHANNEL_ID_PREFIX = "ch_"
    private const val CHANNEL_HASH_LENGTH = 32
}

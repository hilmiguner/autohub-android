package com.autohub.android.tv

data class TvChannel(
    val name: String,
    val streamUrl: String,
    val group: String? = null,
    val tvgId: String? = null,
    val tvgName: String? = null,
    val logoUrl: String? = null,
)

data class TvPlaylist(
    val name: String? = null,
    val epgUrls: List<String> = emptyList(),
    val channels: List<TvChannel> = emptyList(),
)

data class M3uParseIssue(
    val lineNumber: Int,
    val message: String,
)

data class M3uParseResult(
    val playlist: TvPlaylist,
    val issues: List<M3uParseIssue> = emptyList(),
) {
    val hasUsableChannels: Boolean
        get() = playlist.channels.isNotEmpty()
}

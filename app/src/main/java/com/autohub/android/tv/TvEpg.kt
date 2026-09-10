package com.autohub.android.tv

import java.time.Instant

data class TvEpgChannel(
    val id: String,
    val displayNames: List<String> = emptyList(),
    val iconUrl: String? = null,
)

data class TvEpgProgram(
    val channelId: String,
    val start: Instant,
    val stop: Instant? = null,
    val title: String,
    val subTitle: String? = null,
    val description: String? = null,
    val categories: List<String> = emptyList(),
)

data class TvEpgGuide(
    val channels: List<TvEpgChannel> = emptyList(),
    val programs: List<TvEpgProgram> = emptyList(),
)

data class XmlTvParseIssue(
    val lineNumber: Int?,
    val message: String,
)

data class XmlTvParseResult(
    val guide: TvEpgGuide,
    val issues: List<XmlTvParseIssue> = emptyList(),
) {
    val hasUsablePrograms: Boolean
        get() = guide.programs.isNotEmpty()
}

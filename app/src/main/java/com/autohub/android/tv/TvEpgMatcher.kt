package com.autohub.android.tv

import java.util.Locale

enum class TvEpgMatchReason {
    TVG_ID,
    DISPLAY_NAME,
}

data class TvChannelEpgMatch(
    val channelId: TvChannelId,
    val epgChannelId: String,
    val reason: TvEpgMatchReason,
)

data class TvEpgMatchResult(
    val matches: List<TvChannelEpgMatch>,
    val unmatchedChannelIds: List<TvChannelId>,
) {
    fun programsFor(
        channelId: TvChannelId,
        guide: TvEpgGuide,
    ): List<TvEpgProgram> {
        val epgChannelId = matches.firstOrNull { it.channelId == channelId }?.epgChannelId
            ?: return emptyList()
        return guide.programs.filter { it.channelId == epgChannelId }
    }
}

object TvEpgMatcher {
    fun match(
        channels: List<IdentifiedTvChannel>,
        guide: TvEpgGuide,
    ): TvEpgMatchResult {
        val epgByNormalizedId = guide.channels
            .groupBy { normalizeIdentity(it.id) }
            .filterKeys { it.isNotEmpty() }

        val epgByDisplayName = buildMap<String, MutableList<TvEpgChannel>> {
            guide.channels.forEach { epgChannel ->
                epgChannel.displayNames
                    .map(::normalizeName)
                    .filter(String::isNotEmpty)
                    .distinct()
                    .forEach { name -> getOrPut(name) { mutableListOf() }.add(epgChannel) }
            }
        }

        val matches = mutableListOf<TvChannelEpgMatch>()
        val unmatched = mutableListOf<TvChannelId>()

        channels.forEach { identified ->
            val byId = identified.channel.tvgId
                ?.let(::normalizeIdentity)
                ?.takeIf(String::isNotEmpty)
                ?.let(epgByNormalizedId::get)
                ?.singleOrNull()

            if (byId != null) {
                matches += TvChannelEpgMatch(
                    channelId = identified.id,
                    epgChannelId = byId.id,
                    reason = TvEpgMatchReason.TVG_ID,
                )
                return@forEach
            }

            val candidateNames = listOfNotNull(
                identified.channel.tvgName,
                identified.channel.name,
            )
                .map(::normalizeName)
                .filter(String::isNotEmpty)
                .distinct()

            val nameCandidates = candidateNames
                .flatMap { epgByDisplayName[it].orEmpty() }
                .distinctBy { it.id }

            val byName = nameCandidates.singleOrNull()
            if (byName != null) {
                matches += TvChannelEpgMatch(
                    channelId = identified.id,
                    epgChannelId = byName.id,
                    reason = TvEpgMatchReason.DISPLAY_NAME,
                )
            } else {
                unmatched += identified.id
            }
        }

        return TvEpgMatchResult(
            matches = matches,
            unmatchedChannelIds = unmatched,
        )
    }

    private fun normalizeIdentity(value: String): String =
        value.trim().lowercase(Locale.ROOT)

    private fun normalizeName(value: String): String =
        value
            .trim()
            .lowercase(Locale.ROOT)
            .replace(WHITESPACE_PATTERN, " ")

    private val WHITESPACE_PATTERN = Regex("\\s+")
}

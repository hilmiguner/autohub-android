package com.autohub.android.tv

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TvEpgMatcherTest {
    @Test
    fun `tvg id match wins even when names differ`() {
        val channel = identifiedChannel(
            id = "ch_news",
            name = "Completely Different Name",
            tvgId = " News.Example ",
            tvgName = "Other Name",
        )
        val guide = TvEpgGuide(
            channels = listOf(
                TvEpgChannel(
                    id = "news.example",
                    displayNames = listOf("Example News"),
                ),
            ),
        )

        val result = TvEpgMatcher.match(listOf(channel), guide)

        assertEquals(1, result.matches.size)
        assertEquals("news.example", result.matches.single().epgChannelId)
        assertEquals(TvEpgMatchReason.TVG_ID, result.matches.single().reason)
        assertTrue(result.unmatchedChannelIds.isEmpty())
    }

    @Test
    fun `unique display name is used when tvg id cannot match`() {
        val channel = identifiedChannel(
            id = "ch_sport",
            name = "  Example   Sports  ",
            tvgId = null,
        )
        val guide = TvEpgGuide(
            channels = listOf(
                TvEpgChannel(id = "sports.xmltv", displayNames = listOf("example sports")),
            ),
        )

        val result = TvEpgMatcher.match(listOf(channel), guide)

        assertEquals(TvEpgMatchReason.DISPLAY_NAME, result.matches.single().reason)
        assertEquals("sports.xmltv", result.matches.single().epgChannelId)
    }

    @Test
    fun `ambiguous display names remain unmatched`() {
        val channel = identifiedChannel(
            id = "ch_shared",
            name = "Shared Name",
            tvgId = null,
        )
        val guide = TvEpgGuide(
            channels = listOf(
                TvEpgChannel(id = "one", displayNames = listOf("Shared Name")),
                TvEpgChannel(id = "two", displayNames = listOf("shared name")),
            ),
        )

        val result = TvEpgMatcher.match(listOf(channel), guide)

        assertTrue(result.matches.isEmpty())
        assertEquals(listOf(channel.id), result.unmatchedChannelIds)
    }

    @Test
    fun `program lookup follows the resolved epg channel id`() {
        val channel = identifiedChannel(
            id = "ch_movies",
            name = "Movies",
            tvgId = "movie.epg",
        )
        val expectedProgram = TvEpgProgram(
            channelId = "movie.epg",
            start = Instant.parse("2026-09-10T20:00:00Z"),
            title = "Feature Film",
        )
        val guide = TvEpgGuide(
            channels = listOf(TvEpgChannel(id = "movie.epg", displayNames = listOf("Movies"))),
            programs = listOf(
                expectedProgram,
                TvEpgProgram(
                    channelId = "other.epg",
                    start = Instant.parse("2026-09-10T20:00:00Z"),
                    title = "Other",
                ),
            ),
        )

        val result = TvEpgMatcher.match(listOf(channel), guide)

        assertEquals(listOf(expectedProgram), result.programsFor(channel.id, guide))
    }

    private fun identifiedChannel(
        id: String,
        name: String,
        tvgId: String?,
        tvgName: String? = null,
    ): IdentifiedTvChannel =
        IdentifiedTvChannel(
            id = TvChannelId(id),
            sourceId = TvPlaylistSourceId("src_test"),
            channel = TvChannel(
                name = name,
                streamUrl = "https://stream.example.com/$id.m3u8",
                tvgId = tvgId,
                tvgName = tvgName,
            ),
        )
}

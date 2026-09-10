package com.autohub.android.tv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TvPlaylistRepositoryTest {
    @Test
    fun `normalizes remote source and derives deterministic id`() {
        val first = requireNotNull(
            TvPlaylistSourcePolicy.createRemoteHttp(
                name = " News ",
                url = " https://EXAMPLE.com:443/a/../playlist.m3u#section ",
            ),
        )
        val second = requireNotNull(
            TvPlaylistSourcePolicy.createRemoteHttp(
                name = "Other label",
                url = "https://example.com/playlist.m3u",
            ),
        )

        assertEquals("News", first.name)
        assertEquals("https://example.com/playlist.m3u", first.url)
        assertEquals(first.id, second.id)
    }

    @Test
    fun `uses host as source name fallback`() {
        val source = requireNotNull(
            TvPlaylistSourcePolicy.createRemoteHttp(
                name = "   ",
                url = "https://tv.example.com/list.m3u",
            ),
        )

        assertEquals("tv.example.com", source.name)
    }

    @Test
    fun `rejects unsupported playlist source schemes`() {
        assertNull(TvPlaylistSourcePolicy.createRemoteHttp("Local", "file:///tmp/list.m3u"))
        assertNull(TvPlaylistSourcePolicy.createRemoteHttp("Local", "content://playlist/list.m3u"))
        assertNull(TvPlaylistSourcePolicy.createRemoteHttp("Local", "ftp://example.com/list.m3u"))
        assertNull(TvPlaylistSourcePolicy.createRemoteHttp("Broken", "not a url"))
    }

    @Test
    fun `unique tvg id keeps channel identity stable across metadata changes`() {
        val sourceId = TvPlaylistSourceId("src_test")
        val before = TvChannel(
            name = "News HD",
            streamUrl = "https://stream.example.com/old.m3u8",
            group = "News",
            tvgId = " NEWS-1 ",
            logoUrl = "https://img.example.com/old.png",
        )
        val after = TvChannel(
            name = "News",
            streamUrl = "https://stream.example.com/new.m3u8",
            group = "National",
            tvgId = "news-1",
            logoUrl = "https://img.example.com/new.png",
        )

        val beforeId = TvChannelIdentityPolicy.identify(sourceId, listOf(before)).single().id
        val afterId = TvChannelIdentityPolicy.identify(sourceId, listOf(after)).single().id

        assertEquals(beforeId, afterId)
    }

    @Test
    fun `duplicate tvg ids are disambiguated by stream url`() {
        val sourceId = TvPlaylistSourceId("src_test")
        val identified = TvChannelIdentityPolicy.identify(
            sourceId,
            listOf(
                TvChannel(
                    name = "News A",
                    streamUrl = "https://stream.example.com/a.m3u8",
                    tvgId = "news",
                ),
                TvChannel(
                    name = "News B",
                    streamUrl = "https://stream.example.com/b.m3u8",
                    tvgId = "NEWS",
                ),
            ),
        )

        assertNotEquals(identified[0].id, identified[1].id)
    }

    @Test
    fun `stream fallback identity canonicalizes equivalent urls`() {
        val sourceId = TvPlaylistSourceId("src_test")
        val first = TvChannel(
            name = "Channel",
            streamUrl = "https://EXAMPLE.com:443/a/../live.m3u8#fragment",
        )
        val second = first.copy(
            streamUrl = "https://example.com/live.m3u8",
        )

        assertEquals(
            TvChannelIdentityPolicy.identify(sourceId, listOf(first)).single().id,
            TvChannelIdentityPolicy.identify(sourceId, listOf(second)).single().id,
        )
    }

    @Test
    fun `channel identity is scoped to playlist source`() {
        val channel = TvChannel(
            name = "News",
            streamUrl = "https://stream.example.com/news.m3u8",
            tvgId = "news",
        )

        val first = TvChannelIdentityPolicy
            .identify(TvPlaylistSourceId("src_one"), listOf(channel))
            .single()
        val second = TvChannelIdentityPolicy
            .identify(TvPlaylistSourceId("src_two"), listOf(channel))
            .single()

        assertNotEquals(first.id, second.id)
    }

    @Test
    fun `repository stores sources and replaces parsed snapshot`() {
        val repository = InMemoryTvPlaylistRepository()
        val source = requireNotNull(
            TvPlaylistSourcePolicy.createRemoteHttp(
                "Primary",
                "https://example.com/list.m3u",
            ),
        )
        repository.upsertSource(source)
        val channel = TvChannel(
            name = "News",
            streamUrl = "https://stream.example.com/news.m3u8",
            tvgId = "news",
        )
        val snapshot = repository.replaceParsedPlaylist(
            source.id,
            M3uParseResult(
                playlist = TvPlaylist(
                    name = "Example TV",
                    epgUrls = listOf(
                        "https://example.com/epg.xml",
                        "https://example.com/epg.xml",
                    ),
                    channels = listOf(channel, channel),
                ),
                issues = listOf(M3uParseIssue(4, "Ignored test issue")),
            ),
        )

        assertEquals(listOf(source), repository.listSources())
        assertEquals("Example TV", snapshot.name)
        assertEquals(listOf("https://example.com/epg.xml"), snapshot.epgUrls)
        assertEquals(1, snapshot.channels.size)
        assertEquals(1, snapshot.parseIssues.size)
        assertEquals(snapshot, repository.getSnapshot(source.id))
        assertNotNull(repository.findChannel(snapshot.channels.single().id))
    }

    @Test
    fun `updating source metadata preserves stored snapshot`() {
        val repository = InMemoryTvPlaylistRepository()
        val source = requireNotNull(
            TvPlaylistSourcePolicy.createRemoteHttp(
                "Old name",
                "https://example.com/list.m3u",
            ),
        )
        repository.upsertSource(source)
        repository.replaceParsedPlaylist(
            source.id,
            M3uParseResult(
                playlist = TvPlaylist(
                    channels = listOf(
                        TvChannel(
                            name = "News",
                            streamUrl = "https://stream.example.com/news.m3u8",
                        ),
                    ),
                ),
            ),
        )

        repository.upsertSource(source.copy(name = "New name"))

        assertEquals("New name", repository.getSource(source.id)?.name)
        assertEquals("New name", repository.getSnapshot(source.id)?.source?.name)
        assertEquals(1, repository.getSnapshot(source.id)?.channels?.size)
    }

    @Test
    fun `removing source also removes its snapshot`() {
        val repository = InMemoryTvPlaylistRepository()
        val source = requireNotNull(
            TvPlaylistSourcePolicy.createRemoteHttp(
                "Primary",
                "https://example.com/list.m3u",
            ),
        )
        repository.upsertSource(source)
        repository.replaceParsedPlaylist(source.id, M3uParseResult(TvPlaylist()))

        assertTrue(repository.removeSource(source.id))
        assertNull(repository.getSource(source.id))
        assertNull(repository.getSnapshot(source.id))
        assertFalse(repository.removeSource(source.id))
    }

    @Test
    fun `cannot store parsed playlist for unknown source`() {
        val repository = InMemoryTvPlaylistRepository()

        assertThrows(IllegalArgumentException::class.java) {
            repository.replaceParsedPlaylist(
                TvPlaylistSourceId("src_missing"),
                M3uParseResult(TvPlaylist()),
            )
        }
    }
}

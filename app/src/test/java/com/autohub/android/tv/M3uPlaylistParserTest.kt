package com.autohub.android.tv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uPlaylistParserTest {
    private val parser = M3uPlaylistParser()

    @Test
    fun `parses extended m3u channel metadata and epg urls`() {
        val content = "\uFEFF" +
            """
            #EXTM3U playlist-name="Demo TV" x-tvg-url="https://epg.example.com/guide.xml,https://backup.example.com/guide.xml"
            #EXTINF:-1 tvg-id="news.one" tvg-name="News, One" tvg-logo="https://img.example.com/news.png" group-title="News",News One HD
            https://stream.example.com/news/index.m3u8
            #EXTINF:-1 tvg-id="movie.one" group-title="Movies",Movie One
            http://stream.example.com/movie.ts
            """.trimIndent()

        val result = parser.parse(content)

        assertTrue(result.issues.isEmpty())
        assertTrue(result.hasUsableChannels)
        assertEquals("Demo TV", result.playlist.name)
        assertEquals(
            listOf(
                "https://epg.example.com/guide.xml",
                "https://backup.example.com/guide.xml",
            ),
            result.playlist.epgUrls,
        )
        assertEquals(2, result.playlist.channels.size)

        val news = result.playlist.channels[0]
        assertEquals("News One HD", news.name)
        assertEquals("https://stream.example.com/news/index.m3u8", news.streamUrl)
        assertEquals("News", news.group)
        assertEquals("news.one", news.tvgId)
        assertEquals("News, One", news.tvgName)
        assertEquals("https://img.example.com/news.png", news.logoUrl)

        val movie = result.playlist.channels[1]
        assertEquals("Movie One", movie.name)
        assertEquals("Movies", movie.group)
        assertEquals("movie.one", movie.tvgId)
        assertNull(movie.tvgName)
    }

    @Test
    fun `supports extgrp and bare http stream entries`() {
        val content = """
            #EXTM3U
            #EXTINF:-1 tvg-id="sports.one",Sports One
            #EXTGRP:Sports
            https://stream.example.com/sports/live.m3u8
            https://stream.example.com/misc/raw-channel.ts
        """.trimIndent()

        val result = parser.parse(content)

        assertTrue(result.issues.isEmpty())
        assertEquals(2, result.playlist.channels.size)
        assertEquals("Sports", result.playlist.channels[0].group)
        assertEquals("raw-channel.ts", result.playlist.channels[1].name)
    }

    @Test
    fun `rejects unsupported stream schemes and continues parsing`() {
        val content = """
            #EXTM3U
            #EXTINF:-1,Unsafe local file
            file:///sdcard/channel.ts
            #EXTINF:-1,Safe channel
            https://stream.example.com/safe.m3u8
        """.trimIndent()

        val result = parser.parse(content)

        assertEquals(1, result.playlist.channels.size)
        assertEquals("Safe channel", result.playlist.channels.single().name)
        assertEquals(1, result.issues.size)
        assertTrue(result.issues.single().message.contains("HTTP(S)"))
    }

    @Test
    fun `reports extinf entry that is replaced before receiving a stream url`() {
        val content = """
            #EXTM3U
            #EXTINF:-1,Missing stream
            #EXTINF:-1,Working channel
            https://stream.example.com/working.m3u8
        """.trimIndent()

        val result = parser.parse(content)

        assertEquals(1, result.playlist.channels.size)
        assertEquals("Working channel", result.playlist.channels.single().name)
        assertEquals(1, result.issues.size)
        assertTrue(result.issues.single().message.contains("no stream URL"))
    }

    @Test
    fun `does not treat an hls manifest as an iptv channel playlist`() {
        val content = """
            #EXTM3U
            #EXT-X-TARGETDURATION:6
            #EXT-X-MEDIA-SEQUENCE:1
            #EXTINF:6.0,
            segment001.ts
        """.trimIndent()

        val result = parser.parse(content)

        assertFalse(result.hasUsableChannels)
        assertEquals(1, result.issues.size)
        assertTrue(result.issues.single().message.contains("HLS"))
    }

    @Test
    fun `tolerates missing extended m3u header for a bare http playlist`() {
        val result = parser.parse("https://stream.example.com/live/channel.ts")

        assertEquals(1, result.playlist.channels.size)
        assertEquals("channel.ts", result.playlist.channels.single().name)
        assertEquals(1, result.issues.size)
        assertTrue(result.issues.single().message.contains("Missing #EXTM3U"))
    }
}

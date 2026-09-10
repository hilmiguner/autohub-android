package com.autohub.android.tv

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XmlTvParserTest {
    @Test
    fun `parses channel metadata programmes and timezone offsets`() {
        val result = XmlTvParser.parse(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv>
              <channel id="news.example">
                <display-name>Example News</display-name>
                <display-name>Example News HD</display-name>
                <icon src="https://cdn.example.com/news.png" />
              </channel>
              <programme start="20260910220000 +0300" stop="20260910223000 +0300" channel="news.example">
                <title>Evening News</title>
                <sub-title>Late Edition</sub-title>
                <desc>Daily headlines.</desc>
                <category>News</category>
              </programme>
            </tv>
            """.trimIndent(),
        )

        assertTrue(result.issues.isEmpty())
        assertEquals(1, result.guide.channels.size)
        assertEquals("news.example", result.guide.channels.single().id)
        assertEquals(listOf("Example News", "Example News HD"), result.guide.channels.single().displayNames)
        assertEquals("https://cdn.example.com/news.png", result.guide.channels.single().iconUrl)

        val program = result.guide.programs.single()
        assertEquals("news.example", program.channelId)
        assertEquals(Instant.parse("2026-09-10T19:00:00Z"), program.start)
        assertEquals(Instant.parse("2026-09-10T19:30:00Z"), program.stop)
        assertEquals("Evening News", program.title)
        assertEquals("Late Edition", program.subTitle)
        assertEquals("Daily headlines.", program.description)
        assertEquals(listOf("News"), program.categories)
    }

    @Test
    fun `invalid programme is skipped while later valid programme survives`() {
        val result = XmlTvParser.parse(
            """
            <tv>
              <channel id="one"><display-name>One</display-name></channel>
              <programme start="not-a-date" channel="one"><title>Broken</title></programme>
              <programme start="20260910190000 +0000" stop="broken" channel="one"><title>Valid Start</title></programme>
            </tv>
            """.trimIndent(),
        )

        assertEquals(1, result.guide.programs.size)
        assertEquals("Valid Start", result.guide.programs.single().title)
        assertNull(result.guide.programs.single().stop)
        assertTrue(result.issues.any { it.message.contains("invalid start time") })
        assertTrue(result.issues.any { it.message.contains("invalid stop time") })
    }

    @Test
    fun `programme channel without declaration becomes an implicit matchable channel`() {
        val result = XmlTvParser.parse(
            """
            <tv>
              <programme start="20260910190000 Z" channel="implicit.id">
                <title>Programme</title>
              </programme>
            </tv>
            """.trimIndent(),
        )

        assertEquals("implicit.id", result.guide.channels.single().id)
        assertTrue(result.issues.any { it.message.contains("undeclared XMLTV channel") })
    }

    @Test
    fun `doctype and entity declarations fail closed`() {
        val result = XmlTvParser.parse(
            """
            <!DOCTYPE tv [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
            <tv>
              <channel id="unsafe"><display-name>&xxe;</display-name></channel>
            </tv>
            """.trimIndent(),
        )

        assertFalse(result.hasUsablePrograms)
        assertTrue(result.guide.channels.isEmpty())
        assertTrue(result.issues.single().message.contains("DOCTYPE"))
    }

    @Test
    fun `date parser rejects impossible dates and accepts utc without explicit offset`() {
        assertNull(XmlTvDateTimePolicy.parse("20260230120000 +0000"))
        assertEquals(
            Instant.parse("2026-09-10T12:00:00Z"),
            XmlTvDateTimePolicy.parse("20260910120000"),
        )
    }
}

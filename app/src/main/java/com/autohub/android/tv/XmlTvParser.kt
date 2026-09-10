package com.autohub.android.tv

import java.io.StringReader
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.Locator
import org.xml.sax.SAXParseException
import org.xml.sax.helpers.DefaultHandler

object XmlTvParser {
    fun parse(xml: String): XmlTvParseResult {
        val normalizedXml = xml.removePrefix("\uFEFF")
        if (FORBIDDEN_DECLARATION.containsMatchIn(normalizedXml)) {
            return XmlTvParseResult(
                guide = TvEpgGuide(),
                issues = listOf(
                    XmlTvParseIssue(
                        lineNumber = null,
                        message = "DOCTYPE and ENTITY declarations are not supported in XMLTV input.",
                    ),
                ),
            )
        }

        val handler = XmlTvHandler()
        val result = runCatching {
            val factory = SAXParserFactory.newInstance().apply {
                isNamespaceAware = false
                isValidating = false
            }
            setFeatureIfSupported(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true)
            setFeatureIfSupported(factory, "http://xml.org/sax/features/external-general-entities", false)
            setFeatureIfSupported(factory, "http://xml.org/sax/features/external-parameter-entities", false)
            setFeatureIfSupported(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false)

            val reader = factory.newSAXParser().xmlReader.apply {
                contentHandler = handler
                errorHandler = handler
                entityResolver = org.xml.sax.EntityResolver { _, _ -> InputSource(StringReader("")) }
            }
            reader.parse(InputSource(StringReader(normalizedXml)))
        }

        result.exceptionOrNull()?.let { error ->
            val line = (error as? SAXParseException)?.lineNumber?.takeIf { it > 0 }
            handler.addIssue(line, "XMLTV document could not be parsed: ${error.message ?: error::class.java.simpleName}")
        }

        return XmlTvParseResult(
            guide = handler.buildGuide(),
            issues = handler.issues.toList(),
        )
    }

    private fun setFeatureIfSupported(
        factory: SAXParserFactory,
        name: String,
        enabled: Boolean,
    ) {
        runCatching { factory.setFeature(name, enabled) }
    }

    private val FORBIDDEN_DECLARATION = Regex(
        pattern = "<!\\s*(DOCTYPE|ENTITY)\\b",
        option = RegexOption.IGNORE_CASE,
    )
}

internal object XmlTvDateTimePolicy {
    fun parse(value: String?): Instant? {
        val normalized = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val match = DATE_TIME_PATTERN.matchEntire(normalized) ?: return null
        val localDateTime = runCatching {
            LocalDateTime.parse(match.groupValues[1], LOCAL_DATE_TIME_FORMATTER)
        }.getOrNull() ?: return null
        val zone = match.groupValues[2]
        val offset = when {
            zone.isEmpty() || zone.equals("Z", ignoreCase = true) -> ZoneOffset.UTC
            zone.equals("UTC", ignoreCase = true) || zone.equals("GMT", ignoreCase = true) -> ZoneOffset.UTC
            OFFSET_PATTERN.matches(zone) -> runCatching {
                ZoneOffset.of("${zone.substring(0, 3)}:${zone.substring(3, 5)}")
            }.getOrNull()
            else -> null
        } ?: return null

        return localDateTime.toInstant(offset)
    }

    private val DATE_TIME_PATTERN = Regex(
        "^(\\d{14})(?:\\s*([+-]\\d{4}|Z|UTC|GMT))?$",
        RegexOption.IGNORE_CASE,
    )
    private val OFFSET_PATTERN = Regex("^[+-]\\d{4}$")
    private val LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter
        .ofPattern("uuuuMMddHHmmss", Locale.ROOT)
        .withResolverStyle(ResolverStyle.STRICT)
}

private class XmlTvHandler : DefaultHandler() {
    val issues = mutableListOf<XmlTvParseIssue>()

    private val channels = linkedMapOf<String, TvEpgChannel>()
    private val programs = mutableListOf<TvEpgProgram>()
    private var locator: Locator? = null
    private var currentChannel: MutableChannel? = null
    private var currentProgram: MutableProgram? = null
    private var capture: Capture? = null
    private val capturedText = StringBuilder()

    override fun setDocumentLocator(locator: Locator?) {
        this.locator = locator
    }

    override fun startElement(
        uri: String?,
        localName: String?,
        qName: String?,
        attributes: Attributes,
    ) {
        when (elementName(localName, qName)) {
            "channel" -> startChannel(attributes)
            "programme" -> startProgram(attributes)
            "display-name" -> if (currentChannel != null) beginCapture(Capture.CHANNEL_DISPLAY_NAME)
            "title" -> if (currentProgram != null) beginCapture(Capture.PROGRAM_TITLE)
            "sub-title" -> if (currentProgram != null) beginCapture(Capture.PROGRAM_SUBTITLE)
            "desc" -> if (currentProgram != null) beginCapture(Capture.PROGRAM_DESCRIPTION)
            "category" -> if (currentProgram != null) beginCapture(Capture.PROGRAM_CATEGORY)
            "icon" -> if (currentChannel != null && currentProgram == null) {
                currentChannel?.iconUrl = attributes.getValue("src")
                    ?.trim()
                    ?.takeIf { TvPlaylistSourcePolicy.normalizeRemoteHttpUrl(it) != null }
            }
        }
    }

    override fun characters(ch: CharArray, start: Int, length: Int) {
        if (capture != null) capturedText.append(ch, start, length)
    }

    override fun endElement(uri: String?, localName: String?, qName: String?) {
        when (val name = elementName(localName, qName)) {
            "display-name", "title", "sub-title", "desc", "category" -> finishCapture(name)
            "channel" -> finishChannel()
            "programme" -> finishProgram()
        }
    }

    override fun warning(exception: SAXParseException) {
        addIssue(exception.lineNumber.takeIf { it > 0 }, exception.message ?: "XML warning")
    }

    override fun error(exception: SAXParseException) {
        addIssue(exception.lineNumber.takeIf { it > 0 }, exception.message ?: "XML parse error")
    }

    override fun fatalError(exception: SAXParseException) {
        addIssue(exception.lineNumber.takeIf { it > 0 }, exception.message ?: "XML fatal parse error")
        throw exception
    }

    fun addIssue(lineNumber: Int?, message: String) {
        issues += XmlTvParseIssue(lineNumber = lineNumber, message = message)
    }

    fun buildGuide(): TvEpgGuide {
        val resolvedChannels = linkedMapOf<String, TvEpgChannel>()
        resolvedChannels.putAll(channels)
        programs.forEach { program ->
            if (program.channelId !in resolvedChannels) {
                resolvedChannels[program.channelId] = TvEpgChannel(id = program.channelId)
                addIssue(
                    lineNumber = null,
                    message = "Programme references undeclared XMLTV channel '${program.channelId}'.",
                )
            }
        }
        return TvEpgGuide(
            channels = resolvedChannels.values.toList(),
            programs = programs.sortedWith(compareBy<TvEpgProgram> { it.start }.thenBy { it.channelId }),
        )
    }

    private fun startChannel(attributes: Attributes) {
        val id = attributes.getValue("id")?.trim().orEmpty()
        if (id.isEmpty()) {
            addIssue(currentLine(), "XMLTV channel is missing a non-empty id.")
            currentChannel = null
            return
        }
        currentChannel = MutableChannel(id = id, lineNumber = currentLine())
    }

    private fun finishChannel() {
        val channel = currentChannel ?: return
        val parsed = TvEpgChannel(
            id = channel.id,
            displayNames = channel.displayNames.distinct(),
            iconUrl = channel.iconUrl,
        )
        if (channels.putIfAbsent(channel.id, parsed) != null) {
            addIssue(channel.lineNumber, "Duplicate XMLTV channel id '${channel.id}' was ignored.")
        }
        currentChannel = null
    }

    private fun startProgram(attributes: Attributes) {
        currentProgram = MutableProgram(
            channelId = attributes.getValue("channel")?.trim().orEmpty(),
            rawStart = attributes.getValue("start")?.trim(),
            rawStop = attributes.getValue("stop")?.trim(),
            lineNumber = currentLine(),
        )
    }

    private fun finishProgram() {
        val program = currentProgram ?: return
        currentProgram = null

        if (program.channelId.isEmpty()) {
            addIssue(program.lineNumber, "XMLTV programme is missing a channel reference.")
            return
        }
        val start = XmlTvDateTimePolicy.parse(program.rawStart)
        if (start == null) {
            addIssue(program.lineNumber, "XMLTV programme has an invalid start time '${program.rawStart.orEmpty()}'.")
            return
        }
        val title = program.title?.trim()?.takeIf { it.isNotEmpty() }
        if (title == null) {
            addIssue(program.lineNumber, "XMLTV programme for '${program.channelId}' is missing a title.")
            return
        }

        var stop = program.rawStop?.takeIf { it.isNotBlank() }?.let(XmlTvDateTimePolicy::parse)
        if (!program.rawStop.isNullOrBlank() && stop == null) {
            addIssue(program.lineNumber, "XMLTV programme has an invalid stop time '${program.rawStop}'.")
        }
        if (stop != null && !stop.isAfter(start)) {
            addIssue(program.lineNumber, "XMLTV programme stop time must be after its start time.")
            stop = null
        }

        programs += TvEpgProgram(
            channelId = program.channelId,
            start = start,
            stop = stop,
            title = title,
            subTitle = program.subTitle?.trim()?.takeIf { it.isNotEmpty() },
            description = program.description?.trim()?.takeIf { it.isNotEmpty() },
            categories = program.categories.map(String::trim).filter(String::isNotEmpty).distinct(),
        )
    }

    private fun beginCapture(value: Capture) {
        capture = value
        capturedText.setLength(0)
    }

    private fun finishCapture(elementName: String) {
        val active = capture ?: return
        if (active.elementName != elementName) return
        val value = capturedText.toString().trim()
        if (value.isNotEmpty()) {
            when (active) {
                Capture.CHANNEL_DISPLAY_NAME -> currentChannel?.displayNames?.add(value)
                Capture.PROGRAM_TITLE -> currentProgram?.title = value
                Capture.PROGRAM_SUBTITLE -> currentProgram?.subTitle = value
                Capture.PROGRAM_DESCRIPTION -> currentProgram?.description = value
                Capture.PROGRAM_CATEGORY -> currentProgram?.categories?.add(value)
            }
        }
        capture = null
        capturedText.setLength(0)
    }

    private fun currentLine(): Int? = locator?.lineNumber?.takeIf { it > 0 }

    private fun elementName(localName: String?, qName: String?): String =
        localName?.takeIf { it.isNotBlank() } ?: qName.orEmpty()

    private data class MutableChannel(
        val id: String,
        val lineNumber: Int?,
        val displayNames: MutableList<String> = mutableListOf(),
        var iconUrl: String? = null,
    )

    private data class MutableProgram(
        val channelId: String,
        val rawStart: String?,
        val rawStop: String?,
        val lineNumber: Int?,
        var title: String? = null,
        var subTitle: String? = null,
        var description: String? = null,
        val categories: MutableList<String> = mutableListOf(),
    )

    private enum class Capture(val elementName: String) {
        CHANNEL_DISPLAY_NAME("display-name"),
        PROGRAM_TITLE("title"),
        PROGRAM_SUBTITLE("sub-title"),
        PROGRAM_DESCRIPTION("desc"),
        PROGRAM_CATEGORY("category"),
    }
}

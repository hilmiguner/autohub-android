package com.autohub.android.browser

import java.net.URI
import java.net.URISyntaxException

/**
 * Normalizes user-entered browser destinations and constrains the first browser
 * slice to ordinary HTTP(S) navigation.
 *
 * Provider-specific schemes, file/content URIs and JavaScript URLs stay outside
 * the generic browser core and must be handled explicitly in later integrations.
 */
object BrowserNavigationPolicy {
    private val allowedSchemes = setOf("http", "https")

    fun normalizeUserInput(rawInput: String): String? {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return null

        val candidate = if (SCHEME_PATTERN.containsMatchIn(trimmed)) {
            trimmed
        } else {
            "https://$trimmed"
        }

        return if (isAllowedAbsoluteUrl(candidate)) candidate else null
    }

    fun isAllowedAbsoluteUrl(url: String): Boolean {
        val uri = try {
            URI(url)
        } catch (_: URISyntaxException) {
            return false
        }

        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme !in allowedSchemes) return false

        return !uri.host.isNullOrBlank()
    }

    private val SCHEME_PATTERN = Regex("^[A-Za-z][A-Za-z0-9+.-]*://")
}

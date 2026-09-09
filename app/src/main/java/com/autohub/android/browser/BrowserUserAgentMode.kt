package com.autohub.android.browser

enum class BrowserUserAgentMode {
    MOBILE,
    DESKTOP,
    ;

    companion object {
        fun fromStorage(value: String?): BrowserUserAgentMode =
            entries.firstOrNull { it.name == value } ?: MOBILE
    }
}

internal object BrowserUserAgentPolicy {
    fun userAgentFor(
        mode: BrowserUserAgentMode,
        mobileUserAgent: String,
    ): String =
        when (mode) {
            BrowserUserAgentMode.MOBILE -> mobileUserAgent
            BrowserUserAgentMode.DESKTOP -> toDesktopUserAgent(mobileUserAgent)
        }

    fun toDesktopUserAgent(mobileUserAgent: String): String {
        val normalized = mobileUserAgent.trim()
        if (normalized.isEmpty()) return normalized

        return normalized
            .replace(ANDROID_PLATFORM_PATTERN, "(X11; Linux x86_64)")
            .replace("; wv", "")
            .replace(" Version/4.0", "")
            .replace(" Mobile", "")
            .replace(MULTIPLE_SPACES_PATTERN, " ")
            .trim()
    }

    private val ANDROID_PLATFORM_PATTERN = Regex("\\([^)]*Android[^)]*\\)")
    private val MULTIPLE_SPACES_PATTERN = Regex("\\s{2,}")
}

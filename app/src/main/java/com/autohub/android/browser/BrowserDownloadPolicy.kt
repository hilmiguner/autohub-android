package com.autohub.android.browser

data class BrowserDownloadSpec(
    val url: String,
    val fileName: String,
    val mimeType: String?,
)

internal object BrowserDownloadPolicy {
    fun create(
        url: String?,
        suggestedFileName: String?,
        mimeType: String?,
    ): BrowserDownloadSpec? {
        val allowedUrl = url
            ?.trim()
            ?.takeIf { BrowserNavigationPolicy.isAllowedAbsoluteUrl(it) }
            ?: return null

        return BrowserDownloadSpec(
            url = allowedUrl,
            fileName = sanitizeFileName(suggestedFileName),
            mimeType = mimeType?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    fun sanitizeFileName(value: String?): String {
        val sanitized = value
            ?.trim()
            ?.replace(INVALID_FILE_NAME_CHARACTERS, "_")
            ?.trim { it == '.' || it.isWhitespace() }
            ?.take(MAX_FILE_NAME_LENGTH)
            .orEmpty()

        return sanitized.ifBlank { DEFAULT_FILE_NAME }
    }

    private const val DEFAULT_FILE_NAME = "download"
    private const val MAX_FILE_NAME_LENGTH = 128
    private val INVALID_FILE_NAME_CHARACTERS = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")
}

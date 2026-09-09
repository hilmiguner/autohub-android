package com.autohub.android.browser

import android.content.Context

data class BrowserPersistentState(
    val currentUrl: String,
)

interface BrowserStateStore {
    fun load(): BrowserPersistentState?

    fun saveCurrentUrl(url: String)

    fun clear()
}

internal object BrowserPersistencePolicy {
    fun sanitizePersistedUrl(url: String?): String? =
        url
            ?.trim()
            ?.takeIf { BrowserNavigationPolicy.isAllowedAbsoluteUrl(it) }
}

class SharedPreferencesBrowserStateStore(
    context: Context,
) : BrowserStateStore {
    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    override fun load(): BrowserPersistentState? =
        BrowserPersistencePolicy
            .sanitizePersistedUrl(preferences.getString(KEY_CURRENT_URL, null))
            ?.let(::BrowserPersistentState)

    override fun saveCurrentUrl(url: String) {
        val sanitizedUrl = BrowserPersistencePolicy.sanitizePersistedUrl(url) ?: return
        preferences
            .edit()
            .putString(KEY_CURRENT_URL, sanitizedUrl)
            .commit()
    }

    override fun clear() {
        preferences.edit().clear().commit()
    }

    private companion object {
        const val PREFERENCES_NAME = "autohub.browser.state"
        const val KEY_CURRENT_URL = "current_url"
    }
}

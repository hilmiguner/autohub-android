package com.autohub.android.browser

import android.content.Context

data class BrowserPersistentState(
    val currentUrl: String,
)

interface BrowserStateStore {
    fun load(): BrowserPersistentState?

    fun saveCurrentUrl(url: String)

    fun loadUserAgentMode(): BrowserUserAgentMode

    fun saveUserAgentMode(mode: BrowserUserAgentMode)

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

    override fun loadUserAgentMode(): BrowserUserAgentMode =
        BrowserUserAgentMode.fromStorage(
            preferences.getString(KEY_USER_AGENT_MODE, null),
        )

    override fun saveUserAgentMode(mode: BrowserUserAgentMode) {
        preferences
            .edit()
            .putString(KEY_USER_AGENT_MODE, mode.name)
            .commit()
    }

    override fun clear() {
        preferences
            .edit()
            .remove(KEY_CURRENT_URL)
            .commit()
    }

    private companion object {
        const val PREFERENCES_NAME = "autohub.browser.state"
        const val KEY_CURRENT_URL = "current_url"
        const val KEY_USER_AGENT_MODE = "user_agent_mode"
    }
}

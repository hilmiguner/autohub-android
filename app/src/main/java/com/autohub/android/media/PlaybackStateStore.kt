package com.autohub.android.media

import android.content.Context

/**
 * Lightweight Phase 1 persistence boundary for playback-resume state.
 *
 * The payload is tiny and synchronously readable during service startup. A future
 * dedicated data module can move this implementation to DataStore without changing
 * the snapshot contract consumed by PlaybackService.
 */
class PlaybackStateStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun load(): PlaybackSnapshot? {
        val encodedMediaIds = preferences.getString(KEY_MEDIA_IDS, null) ?: return null
        val mediaIds = PlaybackSnapshotCodec.decodeMediaIds(encodedMediaIds)
        if (mediaIds.isEmpty()) {
            clear()
            return null
        }

        val currentIndex = preferences.getInt(KEY_CURRENT_INDEX, INVALID_INDEX)
        val positionMs = preferences.getLong(KEY_POSITION_MS, 0L)
        val wasPlaying = preferences.getBoolean(KEY_WAS_PLAYING, false)

        return runCatching {
            PlaybackSnapshot(
                mediaIds = mediaIds,
                currentIndex = currentIndex,
                positionMs = positionMs,
                wasPlaying = wasPlaying,
            )
        }.getOrElse {
            clear()
            null
        }
    }

    fun save(snapshot: PlaybackSnapshot) {
        preferences.edit()
            .putString(KEY_MEDIA_IDS, PlaybackSnapshotCodec.encodeMediaIds(snapshot.mediaIds))
            .putInt(KEY_CURRENT_INDEX, snapshot.currentIndex)
            .putLong(KEY_POSITION_MS, snapshot.positionMs)
            .putBoolean(KEY_WAS_PLAYING, snapshot.wasPlaying)
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "autohub_playback_state"
        private const val KEY_MEDIA_IDS = "media_ids"
        private const val KEY_CURRENT_INDEX = "current_index"
        private const val KEY_POSITION_MS = "position_ms"
        private const val KEY_WAS_PLAYING = "was_playing"
        private const val INVALID_INDEX = -1
    }
}

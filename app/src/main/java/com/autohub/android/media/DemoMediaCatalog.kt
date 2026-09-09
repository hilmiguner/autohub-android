package com.autohub.android.media

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.autohub.android.R

/**
 * Small deterministic catalog used to validate the Phase 1 playback pipeline.
 *
 * Product content providers will replace this catalog in later phases. Keeping the
 * test item behind a catalog boundary lets the Media3 service stay provider-agnostic.
 */
class DemoMediaCatalog(context: Context) {
    val root: MediaItem = MediaItem.Builder()
        .setMediaId(ROOT_ID)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle("AutoHub Test Media")
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .build(),
        )
        .build()

    private val testTone: MediaItem = MediaItem.Builder()
        .setMediaId(TEST_TONE_ID)
        .setUri(
            Uri.parse(
                "android.resource://${context.packageName}/${R.raw.autohub_test_tone}",
            ),
        )
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle("AutoHub Test Tone")
                .setArtist("AutoHub")
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .build(),
        )
        .build()

    fun getItem(mediaId: String): MediaItem? = when (mediaId) {
        ROOT_ID -> root
        TEST_TONE_ID -> testTone
        else -> null
    }

    fun getChildren(parentId: String): List<MediaItem> = when (parentId) {
        ROOT_ID -> listOf(testTone)
        else -> emptyList()
    }

    /** Resolve ID-only controller requests into a playable MediaItem with a URI. */
    fun resolveForPlayback(request: MediaItem): MediaItem? {
        if (request.localConfiguration != null) {
            return request
        }
        return getItem(request.mediaId)?.takeIf { it.mediaMetadata.isPlayable == true }
    }

    companion object {
        const val ROOT_ID = "autohub-root"
        const val TEST_TONE_ID = "autohub-test-tone"
    }
}

package com.autohub.android.media

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

/**
 * Small deterministic catalog used to validate the Phase 1 playback pipeline.
 *
 * Product content providers will replace this catalog in later phases. Keeping the
 * test items behind a catalog boundary lets the Media3 service stay provider-agnostic.
 */
class DemoMediaCatalog(context: Context) {
    val root: MediaItem = MediaItem.Builder()
        .setMediaId(ROOT_ID)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle("AutoHub Test Queue")
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .build(),
        )
        .build()

    private val testToneUri = TestToneFile.getOrCreate(context)

    val playableItems: List<MediaItem> = listOf(
        buildTestItem(TEST_TONE_1_ID, "AutoHub Test Tone 1"),
        buildTestItem(TEST_TONE_2_ID, "AutoHub Test Tone 2"),
        buildTestItem(TEST_TONE_3_ID, "AutoHub Test Tone 3"),
    )

    fun getItem(mediaId: String): MediaItem? = when (mediaId) {
        ROOT_ID -> root
        else -> playableItems.firstOrNull { it.mediaId == mediaId }
    }

    fun getChildren(parentId: String): List<MediaItem> = when (parentId) {
        ROOT_ID -> playableItems
        else -> emptyList()
    }

    fun indexOfPlayable(mediaId: String): Int =
        playableItems.indexOfFirst { it.mediaId == mediaId }

    /** Resolve ID-only controller requests into a playable MediaItem with a URI. */
    fun resolveForPlayback(request: MediaItem): MediaItem? {
        if (request.localConfiguration != null) {
            return request
        }
        return getItem(request.mediaId)?.takeIf { it.mediaMetadata.isPlayable == true }
    }

    private fun buildTestItem(mediaId: String, title: String): MediaItem =
        MediaItem.Builder()
            .setMediaId(mediaId)
            .setUri(testToneUri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist("AutoHub")
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build(),
            )
            .build()

    companion object {
        const val ROOT_ID = "autohub-root"
        const val TEST_TONE_1_ID = "autohub-test-tone-1"
        const val TEST_TONE_2_ID = "autohub-test-tone-2"
        const val TEST_TONE_3_ID = "autohub-test-tone-3"

        /** Compatibility alias for the original Phase 1 single-item test API. */
        const val TEST_TONE_ID = TEST_TONE_1_ID

        val TEST_QUEUE_IDS = listOf(
            TEST_TONE_1_ID,
            TEST_TONE_2_ID,
            TEST_TONE_3_ID,
        )
    }
}

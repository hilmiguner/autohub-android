package com.autohub.android.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PlaybackSnapshotTest {
    @Test
    fun `media id codec preserves queue order`() {
        val mediaIds = listOf("tone-1", "tone-2", "tone-3")

        val encoded = PlaybackSnapshotCodec.encodeMediaIds(mediaIds)
        val decoded = PlaybackSnapshotCodec.decodeMediaIds(encoded)

        assertEquals(mediaIds, decoded)
    }

    @Test
    fun `snapshot accepts valid queue state`() {
        val snapshot = PlaybackSnapshot(
            mediaIds = listOf("tone-1", "tone-2", "tone-3"),
            currentIndex = 1,
            positionMs = 750L,
            wasPlaying = true,
        )

        assertEquals("tone-2", snapshot.mediaIds[snapshot.currentIndex])
        assertEquals(750L, snapshot.positionMs)
    }

    @Test
    fun `snapshot rejects index outside queue`() {
        assertThrows(IllegalArgumentException::class.java) {
            PlaybackSnapshot(
                mediaIds = listOf("tone-1"),
                currentIndex = 2,
                positionMs = 0L,
                wasPlaying = false,
            )
        }
    }

    @Test
    fun `snapshot rejects negative position`() {
        assertThrows(IllegalArgumentException::class.java) {
            PlaybackSnapshot(
                mediaIds = listOf("tone-1"),
                currentIndex = 0,
                positionMs = -1L,
                wasPlaying = false,
            )
        }
    }
}

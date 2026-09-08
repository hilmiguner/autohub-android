package com.autohub.android.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackQueueTest {
    private val queue = PlaybackQueue(
        items = listOf(
            PlaybackQueueItem("one", "One"),
            PlaybackQueueItem("two", "Two"),
            PlaybackQueueItem("three", "Three"),
        ),
    )

    @Test
    fun `queue starts without a selected item`() {
        assertEquals(PlaybackQueue.NO_SELECTION, queue.currentIndex)
        assertNull(queue.currentItem)
    }

    @Test
    fun `move next selects first item then advances without wrapping`() {
        val first = queue.moveNext()
        val second = first.moveNext()
        val third = second.moveNext()
        val stillThird = third.moveNext()

        assertEquals("one", first.currentItem?.mediaId)
        assertEquals("two", second.currentItem?.mediaId)
        assertEquals("three", third.currentItem?.mediaId)
        assertEquals(third, stillThird)
    }

    @Test
    fun `move previous stops at first item`() {
        val first = queue.select(0)

        assertEquals(first, first.movePrevious())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `select rejects an invalid index`() {
        queue.select(99)
    }
}

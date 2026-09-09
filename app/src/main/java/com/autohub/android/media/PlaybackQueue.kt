package com.autohub.android.media

/** Provider-independent queue entry used by the shared playback domain. */
data class PlaybackQueueItem(
    val mediaId: String,
    val title: String,
)

/**
 * Immutable queue state kept independent from Media3 and content-provider details.
 *
 * Media3 adapters can translate this state to Player media items without leaking
 * framework types into future browser/TV/content modules.
 */
data class PlaybackQueue(
    val items: List<PlaybackQueueItem> = emptyList(),
    val currentIndex: Int = NO_SELECTION,
) {
    init {
        require(currentIndex == NO_SELECTION || currentIndex in items.indices) {
            "currentIndex must point to an existing item or be NO_SELECTION"
        }
    }

    val currentItem: PlaybackQueueItem?
        get() = items.getOrNull(currentIndex)

    fun select(index: Int): PlaybackQueue {
        require(index in items.indices) { "Queue index out of range: $index" }
        return copy(currentIndex = index)
    }

    fun moveNext(): PlaybackQueue = when {
        items.isEmpty() -> this
        currentIndex == NO_SELECTION -> copy(currentIndex = 0)
        currentIndex < items.lastIndex -> copy(currentIndex = currentIndex + 1)
        else -> this
    }

    fun movePrevious(): PlaybackQueue = when {
        currentIndex > 0 -> copy(currentIndex = currentIndex - 1)
        else -> this
    }

    companion object {
        const val NO_SELECTION = -1
    }
}

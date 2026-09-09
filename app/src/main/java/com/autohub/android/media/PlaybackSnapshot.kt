package com.autohub.android.media

/**
 * Provider-independent playback snapshot persisted across process recreation.
 *
 * The snapshot intentionally stores media IDs rather than framework-specific
 * MediaItem payloads so content providers can resolve fresh playable items later.
 */
data class PlaybackSnapshot(
    val mediaIds: List<String>,
    val currentIndex: Int,
    val positionMs: Long,
    val wasPlaying: Boolean,
) {
    init {
        require(mediaIds.isNotEmpty()) { "A persisted playback queue cannot be empty" }
        require(currentIndex in mediaIds.indices) {
            "currentIndex must point to an item in the persisted queue"
        }
        require(positionMs >= 0L) { "positionMs cannot be negative" }
    }
}

/** Small stable codec for preserving queue order in a single preference value. */
object PlaybackSnapshotCodec {
    private const val SEPARATOR = '\u001F'

    fun encodeMediaIds(mediaIds: List<String>): String {
        require(mediaIds.none { SEPARATOR in it }) {
            "Media IDs cannot contain the persistence separator"
        }
        return mediaIds.joinToString(SEPARATOR.toString())
    }

    fun decodeMediaIds(encoded: String): List<String> =
        if (encoded.isEmpty()) {
            emptyList()
        } else {
            encoded.split(SEPARATOR)
        }
}

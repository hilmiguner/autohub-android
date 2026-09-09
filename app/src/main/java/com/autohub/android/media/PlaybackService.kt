package com.autohub.android.media

import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Owns the Phase 1 player and media session outside the Activity lifecycle.
 *
 * A MediaLibraryService is used rather than an Activity-owned player so Android
 * system media controls and Android Auto can connect to the same playback session.
 */
class PlaybackService : MediaLibraryService() {
    private lateinit var catalog: DemoMediaCatalog
    private lateinit var player: ExoPlayer
    private lateinit var playbackStateStore: PlaybackStateStore
    private var mediaLibrarySession: MediaLibrarySession? = null

    private val persistenceHandler by lazy { Handler(Looper.getMainLooper()) }

    private val persistenceTicker = object : Runnable {
        override fun run() {
            if (!::player.isInitialized) return
            persistPlaybackSnapshot()
            if (player.isPlaying) {
                persistenceHandler.postDelayed(this, PERSISTENCE_INTERVAL_MS)
            }
        }
    }

    private val persistenceListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            persistPlaybackSnapshot()
            scheduleProgressPersistence()
        }
    }

    private val libraryCallback = object : MediaLibrarySession.Callback {
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> =
            Futures.immediateFuture(LibraryResult.ofItem(catalog.root, params))

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val children = catalog.getChildren(parentId)
            val fromIndex = (page.toLong() * pageSize)
                .coerceAtMost(children.size.toLong())
                .toInt()
            val toIndex = (fromIndex + pageSize).coerceAtMost(children.size)

            return Futures.immediateFuture(
                LibraryResult.ofItemList(children.subList(fromIndex, toIndex), params),
            )
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val item = catalog.getItem(mediaId)
                ?: return Futures.immediateFuture(
                    LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE, null),
                )

            return Futures.immediateFuture(LibraryResult.ofItem(item, null))
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> {
            val resolvedItems = mediaItems.map { requestedItem ->
                catalog.resolveForPlayback(requestedItem)
                    ?: return Futures.immediateFailedFuture(
                        IllegalArgumentException(
                            "Unknown or non-playable media item: ${requestedItem.mediaId}",
                        ),
                    )
            }

            return Futures.immediateFuture(resolvedItems)
        }

        @UnstableApi
        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            if (mediaItems.size == 1) {
                val requestedIndex = catalog.indexOfPlayable(mediaItems.single().mediaId)
                if (requestedIndex >= 0) {
                    return Futures.immediateFuture(
                        MediaSession.MediaItemsWithStartPosition(
                            catalog.playableItems,
                            requestedIndex,
                            startPositionMs,
                        ),
                    )
                }
            }

            val resolvedItems = mediaItems.map { requestedItem ->
                catalog.resolveForPlayback(requestedItem)
                    ?: return Futures.immediateFailedFuture(
                        IllegalArgumentException(
                            "Unknown or non-playable media item: ${requestedItem.mediaId}",
                        ),
                    )
            }

            val resolvedStartIndex = when {
                startIndex == C.INDEX_UNSET -> C.INDEX_UNSET
                resolvedItems.isEmpty() -> C.INDEX_UNSET
                else -> startIndex.coerceIn(resolvedItems.indices)
            }

            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(
                    resolvedItems,
                    resolvedStartIndex,
                    startPositionMs,
                ),
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        catalog = DemoMediaCatalog(this)
        playbackStateStore = PlaybackStateStore(this)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        restorePersistedState()
        player.addListener(persistenceListener)
        mediaLibrarySession = MediaLibrarySession.Builder(this, player, libraryCallback).build()
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo,
    ): MediaLibrarySession? = mediaLibrarySession

    override fun onDestroy() {
        persistenceHandler.removeCallbacks(persistenceTicker)
        if (::player.isInitialized) {
            persistPlaybackSnapshot()
            player.removeListener(persistenceListener)
        }
        mediaLibrarySession?.release()
        mediaLibrarySession = null
        if (::player.isInitialized) {
            player.release()
        }
        super.onDestroy()
    }

    private fun restorePersistedState() {
        val snapshot = playbackStateStore.load() ?: return
        val resolvedItems = snapshot.mediaIds.mapNotNull { mediaId ->
            catalog.getItem(mediaId)?.takeIf { it.mediaMetadata.isPlayable == true }
        }

        if (resolvedItems.isEmpty() || resolvedItems.size != snapshot.mediaIds.size) {
            playbackStateStore.clear()
            return
        }

        val restoredIndex = snapshot.currentIndex.coerceIn(resolvedItems.indices)
        player.setMediaItems(resolvedItems, restoredIndex, snapshot.positionMs)
        player.prepare()

        // Restore context without surprising the user with automatic audio on startup.
        player.pause()
    }

    private fun persistPlaybackSnapshot() {
        if (!::player.isInitialized || !::playbackStateStore.isInitialized) return

        val mediaItemCount = player.mediaItemCount
        if (mediaItemCount == 0) {
            playbackStateStore.clear()
            return
        }

        val currentIndex = player.currentMediaItemIndex
        if (currentIndex !in 0 until mediaItemCount) return

        val mediaIds = (0 until mediaItemCount).map { index ->
            player.getMediaItemAt(index).mediaId
        }

        playbackStateStore.save(
            PlaybackSnapshot(
                mediaIds = mediaIds,
                currentIndex = currentIndex,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                wasPlaying = player.isPlaying,
            ),
        )
    }

    private fun scheduleProgressPersistence() {
        persistenceHandler.removeCallbacks(persistenceTicker)
        if (player.isPlaying) {
            persistenceHandler.postDelayed(persistenceTicker, PERSISTENCE_INTERVAL_MS)
        }
    }

    companion object {
        private const val PERSISTENCE_INTERVAL_MS = 1_000L
    }
}

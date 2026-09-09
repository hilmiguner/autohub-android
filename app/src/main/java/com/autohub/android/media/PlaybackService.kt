package com.autohub.android.media

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
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
    private var mediaLibrarySession: MediaLibrarySession? = null

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
    }

    override fun onCreate() {
        super.onCreate()
        catalog = DemoMediaCatalog(this)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, libraryCallback).build()
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo,
    ): MediaLibrarySession? = mediaLibrarySession

    override fun onDestroy() {
        mediaLibrarySession?.run {
            player.release()
            release()
        }
        mediaLibrarySession = null
        super.onDestroy()
    }
}

package com.autohub.android

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.autohub.android.browser.BrowserActivity
import com.autohub.android.media.DemoMediaCatalog
import com.autohub.android.media.PlaybackService
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val context = LocalContext.current
                    var controller by remember { mutableStateOf<MediaController?>(null) }
                    var playbackStatus by remember { mutableStateOf("Connecting to playback service…") }
                    var isPlaying by remember { mutableStateOf(false) }
                    var currentTitle by remember { mutableStateOf("None") }
                    var currentIndex by remember { mutableStateOf(0) }
                    var queueSize by remember { mutableStateOf(0) }
                    var hasPrevious by remember { mutableStateOf(false) }
                    var hasNext by remember { mutableStateOf(false) }
                    var positionMs by remember { mutableStateOf(0L) }
                    var durationMs by remember { mutableStateOf(0L) }

                    fun updateUi(player: Player) {
                        isPlaying = player.isPlaying
                        currentTitle = player.currentMediaItem
                            ?.mediaMetadata
                            ?.title
                            ?.toString()
                            ?: "None"
                        queueSize = player.mediaItemCount
                        currentIndex = if (player.mediaItemCount > 0) {
                            player.currentMediaItemIndex + 1
                        } else {
                            0
                        }
                        hasPrevious = player.hasPreviousMediaItem()
                        hasNext = player.hasNextMediaItem()
                        positionMs = player.currentPosition.coerceAtLeast(0L)
                        durationMs = player.duration.validDurationOrZero()
                        playbackStatus = playbackStateLabel(player.playbackState, player.isPlaying)
                    }

                    DisposableEffect(context) {
                        val token = SessionToken(
                            context,
                            ComponentName(context, PlaybackService::class.java),
                        )
                        val controllerFuture = MediaController.Builder(context, token).buildAsync()

                        val playerListener = object : Player.Listener {
                            override fun onEvents(player: Player, events: Player.Events) {
                                updateUi(player)
                            }
                        }

                        controllerFuture.addListener(
                            {
                                try {
                                    controller = controllerFuture.get().also { mediaController ->
                                        mediaController.addListener(playerListener)
                                        updateUi(mediaController)
                                    }
                                } catch (error: Exception) {
                                    playbackStatus = "Playback service connection failed"
                                }
                            },
                            ContextCompat.getMainExecutor(context),
                        )

                        onDispose {
                            controller?.removeListener(playerListener)
                            controller = null
                            MediaController.releaseFuture(controllerFuture)
                        }
                    }

                    LaunchedEffect(controller) {
                        while (true) {
                            controller?.let { mediaController ->
                                positionMs = mediaController.currentPosition.coerceAtLeast(0L)
                                durationMs = mediaController.duration.validDurationOrZero()
                            }
                            delay(250)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "AutoHub",
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Phase 2 · Browser Foundation",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "The first browser slice runs on the phone only. Phase 1 media controls remain available below for regression testing.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                context.startActivity(Intent(context, BrowserActivity::class.java))
                            },
                        ) {
                            Text("Open phone browser")
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Media session: $playbackStatus",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Current item: $currentTitle",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Queue: $currentIndex / $queueSize",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Position: ${formatPosition(positionMs)} / ${formatPosition(durationMs)}",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isPlaying) "Playback: Playing" else "Playback: Not playing",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = controller != null,
                            onClick = {
                                controller?.let { mediaController ->
                                    if (mediaController.mediaItemCount == 0) {
                                        val queue = DemoMediaCatalog.TEST_QUEUE_IDS.map { mediaId ->
                                            MediaItem.Builder().setMediaId(mediaId).build()
                                        }
                                        mediaController.setMediaItems(queue, 0, 0L)
                                        mediaController.prepare()
                                    } else if (mediaController.playbackState == Player.STATE_IDLE) {
                                        mediaController.prepare()
                                    } else if (mediaController.playbackState == Player.STATE_ENDED) {
                                        mediaController.seekToDefaultPosition(0)
                                    }
                                    mediaController.play()
                                }
                            },
                        ) {
                            Text(if (queueSize > 0) "Resume saved queue" else "Play test queue")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                enabled = controller != null && hasPrevious,
                                onClick = { controller?.seekToPreviousMediaItem() },
                            ) {
                                Text("Previous")
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                enabled = controller != null && hasNext,
                                onClick = { controller?.seekToNextMediaItem() },
                            ) {
                                Text("Next")
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                enabled = controller != null && queueSize > 0,
                                onClick = {
                                    controller?.let { mediaController ->
                                        mediaController.seekTo(
                                            (mediaController.currentPosition - SEEK_STEP_MS)
                                                .coerceAtLeast(0L),
                                        )
                                    }
                                },
                            ) {
                                Text("Seek -0.5s")
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                enabled = controller != null && queueSize > 0,
                                onClick = {
                                    controller?.let { mediaController ->
                                        val duration = mediaController.duration.validDurationOrZero()
                                        val requested = mediaController.currentPosition + SEEK_STEP_MS
                                        val target = if (duration > 0L) {
                                            requested.coerceAtMost(duration)
                                        } else {
                                            requested
                                        }
                                        mediaController.seekTo(target)
                                    }
                                },
                            ) {
                                Text("Seek +0.5s")
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = controller != null && queueSize > 0,
                            onClick = {
                                controller?.let { mediaController ->
                                    if (mediaController.isPlaying) {
                                        mediaController.pause()
                                    } else {
                                        mediaController.play()
                                    }
                                }
                            },
                        ) {
                            Text(if (isPlaying) "Pause" else "Resume")
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val SEEK_STEP_MS = 500L
    }
}

private fun playbackStateLabel(playbackState: Int, isPlaying: Boolean): String = when {
    isPlaying -> "Playing"
    playbackState == Player.STATE_BUFFERING -> "Buffering"
    playbackState == Player.STATE_READY -> "Ready"
    playbackState == Player.STATE_ENDED -> "Ended"
    else -> "Idle"
}

private fun Long.validDurationOrZero(): Long =
    if (this == C.TIME_UNSET || this <= 0L) 0L else this

private fun formatPosition(positionMs: Long): String {
    val safeMs = positionMs.coerceAtLeast(0L)
    val seconds = safeMs / 1_000L
    val tenths = (safeMs % 1_000L) / 100L
    return "$seconds.${tenths}s"
}

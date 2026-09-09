package com.autohub.android

import android.content.ComponentName
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.autohub.android.media.DemoMediaCatalog
import com.autohub.android.media.PlaybackService

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

                    DisposableEffect(context) {
                        val token = SessionToken(
                            context,
                            ComponentName(context, PlaybackService::class.java),
                        )
                        val controllerFuture = MediaController.Builder(context, token).buildAsync()

                        fun updateUi(player: Player) {
                            isPlaying = player.isPlaying
                            currentTitle = player.currentMediaItem
                                ?.mediaMetadata
                                ?.title
                                ?.toString()
                                ?: "None"
                            playbackStatus = playbackStateLabel(player.playbackState, player.isPlaying)
                        }

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
                            text = "Phase 1 · Media Foundation",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Media3 ExoPlayer and MediaLibraryService now own playback outside the Activity lifecycle.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Session: $playbackStatus",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Current item: $currentTitle",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isPlaying) "Playback: Playing" else "Playback: Not playing",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = controller != null,
                            onClick = {
                                controller?.let { mediaController ->
                                    if (mediaController.mediaItemCount == 0) {
                                        mediaController.setMediaItem(
                                            MediaItem.Builder()
                                                .setMediaId(DemoMediaCatalog.TEST_TONE_ID)
                                                .build(),
                                        )
                                        mediaController.prepare()
                                    } else if (mediaController.playbackState == Player.STATE_IDLE) {
                                        mediaController.prepare()
                                    }
                                    mediaController.play()
                                }
                            },
                        ) {
                            Text("Play test tone")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                enabled = controller != null,
                                onClick = { controller?.pause() },
                            ) {
                                Text("Pause")
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                enabled = controller != null,
                                onClick = {
                                    controller?.let { mediaController ->
                                        if (mediaController.mediaItemCount == 0) {
                                            mediaController.setMediaItem(
                                                MediaItem.Builder()
                                                    .setMediaId(DemoMediaCatalog.TEST_TONE_ID)
                                                    .build(),
                                            )
                                        }
                                        mediaController.seekTo(0)
                                        mediaController.prepare()
                                        mediaController.play()
                                    }
                                },
                            ) {
                                Text("Restart")
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "The bundled test tone is intentionally short and offline so playback tests do not depend on a network source.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

private fun playbackStateLabel(playbackState: Int, isPlaying: Boolean): String = when {
    isPlaying -> "Playing"
    playbackState == Player.STATE_BUFFERING -> "Buffering"
    playbackState == Player.STATE_READY -> "Ready"
    playbackState == Player.STATE_ENDED -> "Ended"
    else -> "Idle"
}

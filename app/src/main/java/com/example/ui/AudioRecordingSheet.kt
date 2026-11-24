package com.example.ui

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.max

@Composable
fun AudioRecordingStudioContent(
    hasAudioPermission: Boolean,
    isRecording: Boolean,
    isPaused: Boolean,
    recordingSeconds: Int,
    amplitudes: List<Float>,
    attachedAudioUri: Uri?,
    attachedAudioName: String?,
    onRequestPermission: () -> Unit,
    onStartRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onDiscardAttachedAudio: () -> Unit,
    onUploadAudioFile: () -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    var isPlayingPreview by remember { mutableStateOf(false) }
    var playbackProgress by remember { mutableFloatStateOf(0f) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Clean up MediaPlayer whenever URI changes or sheet disposes
    DisposableEffect(attachedAudioUri) {
        onDispose {
            try {
                mediaPlayer?.stop()
            } catch (_: Exception) {
            }
            try {
                mediaPlayer?.release()
            } catch (_: Exception) {
            }
            mediaPlayer = null
            isPlayingPreview = false
            playbackProgress = 0f
        }
    }

    // Update playback progress bar smoothly while previewing
    LaunchedEffect(isPlayingPreview, mediaPlayer) {
        while (isPlayingPreview) {
            val player = mediaPlayer
            if (player != null && player.isPlaying) {
                val dur = max(1, player.duration)
                playbackProgress = (player.currentPosition.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
            }
            delay(80L)
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_pulse_alpha"
    )

    val statusColor by animateColorAsState(
        targetValue = when {
            isRecording && !isPaused -> MaterialTheme.colorScheme.error
            isRecording && isPaused -> MaterialTheme.colorScheme.secondary
            attachedAudioUri != null -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.primary
        },
        label = "status_color"
    )

    val statusLabel = when {
        isRecording && !isPaused -> "LIVE MICROPHONE"
        isRecording && isPaused -> "RECORDING PAUSED"
        attachedAudioUri != null -> "VOICE CLIP READY"
        else -> "VOICE STUDIO READY"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("audio_recording_studio_sheet"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header row with live status pill & elapsed timer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    color = statusColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .alpha(if (isRecording && !isPaused) pulseAlpha else 1f)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Real-Time Voice Capture",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Text(
                    text = "${formatDurationMmSs(recordingSeconds)} / 02:00",
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("audio_timer_text")
                )
            }
        }

        if (!hasAudioPermission && !isRecording && attachedAudioUri == null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Microphone Access Needed",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Text(
                        text = "Grant microphone access to record real-time speech for acoustic tone, pitch, pace, and communication credibility analysis.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onRequestPermission,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("grant_audio_permission_button"),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Filled.Mic, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Allow Microphone Access")
                    }
                }
            }
        } else {
            // Real-time Acoustic Waveform Visualizer
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = when {
                                    isRecording && !isPaused -> "Capturing live microphone input..."
                                    isRecording && isPaused -> "Microphone paused — tap Resume to continue"
                                    attachedAudioUri != null -> attachedAudioName ?: "Recorded Voice Clip"
                                    else -> "Speak naturally 6–12 inches from your microphone"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    val activeBarColor = statusColor
                    val idleTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .testTag("audio_waveform_canvas")
                    ) {
                        val bars = amplitudes.ifEmpty { List(42) { 0.08f } }
                        val barCount = bars.size
                        val slotWidth = size.width / barCount
                        val barWidth = (slotWidth * 0.56f).coerceAtLeast(3.dp.toPx())
                        val centerY = size.height / 2f

                        // Center baseline
                        drawLine(
                            color = idleTrackColor,
                            start = Offset(0f, centerY),
                            end = Offset(size.width, centerY),
                            strokeWidth = 1.dp.toPx()
                        )

                        bars.forEachIndexed { index, amp ->
                            val normalized = amp.coerceIn(0.06f, 1f)
                            val barHeight = max(6.dp.toPx(), size.height * 0.88f * normalized)
                            val x = index * slotWidth + (slotWidth - barWidth) / 2f
                            val y = centerY - barHeight / 2f
                            drawRoundRect(
                                color = if (isRecording || attachedAudioUri != null) {
                                    activeBarColor.copy(alpha = 0.55f + 0.45f * normalized)
                                } else {
                                    idleTrackColor
                                },
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                            )
                        }
                    }

                    if (!isRecording && attachedAudioUri != null) {
                        LinearProgressIndicator(
                            progress = { playbackProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Studio Controls based on current state
            when {
                isRecording -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                if (isPaused) onResumeRecording() else onPauseRecording()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                                .testTag("audio_studio_pause_resume_button"),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Filled.Mic else Icons.Filled.Pause,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (isPaused) "Resume" else "Pause")
                        }

                        Button(
                            onClick = onStopRecording,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                                .testTag("audio_studio_stop_button"),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Stop & Review")
                        }
                    }

                    TextButton(
                        onClick = onCancelRecording,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .testTag("audio_studio_cancel_button")
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Discard Recording")
                    }
                }

                attachedAudioUri != null -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                if (isPlayingPreview) {
                                    runCatching { mediaPlayer?.pause() }
                                    isPlayingPreview = false
                                } else {
                                    val existing = mediaPlayer
                                    if (existing != null) {
                                        runCatching { existing.start() }
                                        isPlayingPreview = true
                                    } else {
                                        runCatching {
                                            MediaPlayer().apply {
                                                setDataSource(context, attachedAudioUri)
                                                setOnCompletionListener {
                                                    isPlayingPreview = false
                                                    playbackProgress = 1f
                                                }
                                                prepare()
                                                start()
                                            }
                                        }.onSuccess { player ->
                                            mediaPlayer = player
                                            isPlayingPreview = true
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                                .testTag("audio_studio_play_button"),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                imageVector = if (isPlayingPreview) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (isPlayingPreview) "Pause Clip" else "Play Preview")
                        }

                        OutlinedButton(
                            onClick = {
                                runCatching { mediaPlayer?.stop() }
                                runCatching { mediaPlayer?.release() }
                                mediaPlayer = null
                                isPlayingPreview = false
                                playbackProgress = 0f
                                onDiscardAttachedAudio()
                                onStartRecording()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                                .testTag("audio_studio_rerecord_button"),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Re-record")
                        }
                    }

                    Button(
                        onClick = onDone,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("audio_studio_attach_button"),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Use Recording for Voice Analysis")
                    }
                }

                else -> {
                    Button(
                        onClick = onStartRecording,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 54.dp)
                            .testTag("audio_studio_start_button"),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Filled.Mic, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Start Live Recording")
                    }
                }
            }
        }

        if (!isRecording) {
            TextButton(
                onClick = onUploadAudioFile,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTag("audio_studio_upload_file_button")
            ) {
                Icon(Icons.Filled.AudioFile, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Or choose an existing audio file")
            }
        }
    }
}

private fun formatDurationMmSs(totalSeconds: Int): String {
    val safe = max(0, totalSeconds)
    val mins = safe / 60
    val secs = safe % 60
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}

package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.min

private const val TAG = "MediaProcessor"
private const val MAX_IMAGE_DIMENSION = 1024
private const val MAX_AUDIO_BYTES = 12 * 1024 * 1024 // 12 MB safe limit under 20 MB ceiling

data class AudioPayload(
    val bytes: ByteArray,
    val mimeType: String,
    val displayName: String
)

object MediaProcessor {

    fun createTempCameraUri(context: Context, prefix: String = "photo"): Uri {
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun getFileDisplayName(context: Context, uri: Uri): String {
        var name = "Selected media"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)?.let { name = it }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to resolve file display name", e)
        }
        return name
    }

    suspend fun decodeScaledBitmap(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }
            val origWidth = boundsOptions.outWidth
            val origHeight = boundsOptions.outHeight
            if (origWidth <= 0 || origHeight <= 0) return@withContext null

            var sampleSize = 1
            while (max(origWidth / sampleSize, origHeight / sampleSize) > MAX_IMAGE_DIMENSION * 2) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@withContext null

            scaleBitmapIfNeeded(decoded)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode bitmap from URI: $uri", e)
            null
        }
    }

    private fun scaleBitmapIfNeeded(bitmap: Bitmap): Bitmap {
        val maxEdge = max(bitmap.width, bitmap.height)
        if (maxEdge <= MAX_IMAGE_DIMENSION) return bitmap
        val ratio = MAX_IMAGE_DIMENSION.toFloat() / maxEdge.toFloat()
        val targetWidth = max(1, (bitmap.width * ratio).toInt())
        val targetHeight = max(1, (bitmap.height * ratio).toInt())
        val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        if (scaled != bitmap) {
            bitmap.recycle()
        }
        return scaled
    }

    suspend fun extractVideoKeyframes(
        context: Context,
        videoUri: Uri,
        maxFrames: Int = 5
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val frames = mutableListOf<Bitmap>()
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val durationMs = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L

            if (durationMs <= 0L) {
                retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let {
                    frames.add(scaleBitmapIfNeeded(it))
                }
            } else {
                val frameCount = min(maxFrames, max(1, (durationMs / 1000L).toInt().coerceIn(2, maxFrames)))
                for (i in 0 until frameCount) {
                    val timeUs = ((durationMs * (i + 0.5)) / frameCount).toLong() * 1000L
                    val rawFrame = retriever.getFrameAtTime(
                        timeUs,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )
                    if (rawFrame != null) {
                        frames.add(scaleBitmapIfNeeded(rawFrame))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract video keyframes from $videoUri", e)
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
        frames
    }

    suspend fun readAudioPayload(context: Context, audioUri: Uri): AudioPayload? =
        withContext(Dispatchers.IO) {
            try {
                val rawMime = context.contentResolver.getType(audioUri) ?: "audio/mp4"
                val normalizedMime = when {
                    rawMime.contains("mpeg") || rawMime.contains("mp3") -> "audio/mp3"
                    rawMime.contains("wav") -> "audio/wav"
                    rawMime.contains("ogg") -> "audio/ogg"
                    rawMime.contains("aac") -> "audio/aac"
                    rawMime.contains("flac") -> "audio/flac"
                    else -> "audio/mp4"
                }
                val displayName = getFileDisplayName(context, audioUri)
                val bytes = context.contentResolver.openInputStream(audioUri)?.use { input ->
                    val buffer = ByteArrayOutputStream()
                    val chunk = ByteArray(8192)
                    var total = 0
                    while (true) {
                        val read = input.read(chunk)
                        if (read <= 0) break
                        if (total + read > MAX_AUDIO_BYTES) {
                            throw IllegalArgumentException(
                                "Audio file exceeds 12 MB limit. Please trim or record a shorter clip."
                            )
                        }
                        buffer.write(chunk, 0, read)
                        total += read
                    }
                    buffer.toByteArray()
                } ?: return@withContext null

                if (bytes.isEmpty()) return@withContext null
                AudioPayload(bytes = bytes, mimeType = normalizedMime, displayName = displayName)
            } catch (e: IllegalArgumentException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read audio URI: $audioUri", e)
                null
            }
        }
}

class VoiceRecorderManager(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var isPaused: Boolean = false

    fun startRecording(): File {
        stopSilently()
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        val file = File(dir, "voice_clip_${System.currentTimeMillis()}.m4a")
        outputFile = file
        isPaused = false

        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        mediaRecorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(96000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        recorder = mediaRecorder
        return file
    }

    fun pauseRecording(): Boolean {
        val active = recorder ?: return false
        return try {
            if (!isPaused) {
                active.pause()
                isPaused = true
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause MediaRecorder", e)
            false
        }
    }

    fun resumeRecording(): Boolean {
        val active = recorder ?: return false
        return try {
            if (isPaused) {
                active.resume()
                isPaused = false
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume MediaRecorder", e)
            false
        }
    }

    fun sampleAmplitude(): Float {
        if (isPaused) return 0.04f
        val raw = try {
            recorder?.maxAmplitude ?: 0
        } catch (_: Exception) {
            0
        }
        if (raw <= 0) return 0.06f
        val linear = (raw / 32767f).coerceIn(0f, 1f)
        return kotlin.math.sqrt(linear.toDouble()).toFloat().coerceIn(0.06f, 1f)
    }

    fun stopRecording(): Uri? {
        val file = outputFile
        isPaused = false
        return try {
            recorder?.apply {
                stop()
                reset()
                release()
            }
            recorder = null
            if (file != null && file.exists() && file.length() > 0L) {
                Uri.fromFile(file)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop MediaRecorder cleanly", e)
            stopSilently()
            null
        }
    }

    fun cancelRecording() {
        isPaused = false
        stopSilently()
        try {
            outputFile?.delete()
        } catch (_: Exception) {
        }
        outputFile = null
    }

    fun stopSilently() {
        isPaused = false
        try {
            recorder?.apply {
                reset()
                release()
            }
        } catch (_: Exception) {
        } finally {
            recorder = null
        }
    }
}

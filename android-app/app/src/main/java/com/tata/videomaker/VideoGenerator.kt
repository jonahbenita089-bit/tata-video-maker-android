package com.tata.videomaker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.CountDownLatch

object VideoGenerator {

    private fun createBitmapWithText(text: String, width: Int = 1280, height: Int = 720): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.parseColor("#050505"))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE
        paint.textSize = 48f
        paint.textAlign = Paint.Align.LEFT

        val padding = 80f
        val x = padding
        var y = height / 2f

        // Simple multi-line wrap
        val lines = wrapText(text, 36)
        val lineHeight = paint.fontSpacing
        y -= (lines.size - 1) * lineHeight / 2

        for (line in lines) {
            canvas.drawText(line, x, y, paint)
            y += lineHeight
        }

        return bmp
    }

    private fun wrapText(text: String, maxChars: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (w in words) {
            if (current.length + w.length + 1 > maxChars) {
                lines.add(current.toString())
                current = StringBuilder()
            }
            if (current.isNotEmpty()) current.append(' ')
            current.append(w)
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }

    private fun saveBitmapToFile(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
        }
    }

    suspend fun generateOfflineVideo(
        context: Context,
        prompt: String,
        durationSeconds: Int = 10,
        style: String = "Cinematic",
    ): Pair<Boolean, String?> {
        return withContext(Dispatchers.IO) {
            try {
                val outDir = File(context.filesDir, "generated_videos")
                if (!outDir.exists()) outDir.mkdirs()

                val safeName = sanitizeFilename(prompt)
                val imageFile = File(outDir, "${safeName}.png")
                val audioFile = File(outDir, "${safeName}.wav")
                val outFile = File(outDir, "${safeName}-${durationSeconds}s.mp4")

                // Create bitmap image with prompt text
                val bmp = createBitmapWithText("${style.uppercase()}\n$prompt")
                saveBitmapToFile(bmp, imageFile)

                // Generate speech audio using TextToSpeech (blocking until complete)
                val latch = CountDownLatch(1)
                val tts = TextToSpeech(context) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        tts.language = Locale.US
                        val params = Bundle()
                        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "utt1")

                        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String) {}
                            override fun onDone(utteranceId: String) { latch.countDown() }
                            override fun onError(utteranceId: String) { latch.countDown() }
                        })

                        // synthesizeToFile signature: (CharSequence, Bundle, File, String)
                        try {
                            tts.synthesizeToFile(prompt, params, audioFile, "utt1")
                        } catch (ex: Exception) {
                            // some Android versions require the older HashMap API, attempt fallback
                            try {
                                @Suppress("DEPRECATION")
                                val map = HashMap<String, String>()
                                map[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = "utt1"
                                @Suppress("DEPRECATION")
                                tts.synthesizeToFile(prompt, map as HashMap<String, String>, audioFile.absolutePath)
                            } catch (_: Exception) {
                                latch.countDown()
                            }
                        }

                    } else {
                        latch.countDown()
                    }
                }

                latch.await()
                try { tts.shutdown() } catch (_: Exception) {}

                // Use FFmpegKit to combine image (looped) and audio into mp4
                // Command: loop image, set duration, add audio, encode
                // -loop 1 -i image.png -i audio.wav -c:v libx264 -t <duration> -pix_fmt yuv420p -c:a aac -shortest out.mp4
                val cmd = listOf(
                    "-y",
                    "-loop", "1",
                    "-i", imageFile.absolutePath,
                    "-i", audioFile.absolutePath,
                    "-c:v", "libx264",
                    "-t", durationSeconds.toString(),
                    "-pix_fmt", "yuv420p",
                    "-c:a", "aac",
                    "-shortest",
                    outFile.absolutePath
                ).joinToString(" ")

                // Increase FFmpeg logging folder so library loads native libraries properly
                FFmpegKitConfig.setLogLevel(com.arthenica.ffmpegkit.LogLevel.AV_LOG_INFO)

                val session = FFmpegKit.execute(cmd)
                val returnCode = session.returnCode
                if (returnCode.isValueSuccess) {
                    Pair(true, outFile.absolutePath)
                } else {
                    val fail = session.failStackTrace ?: "Unknown ffmpeg error"
                    Pair(false, "FFmpeg failed: $fail")
                }

            } catch (ex: Exception) {
                Pair(false, ex.message)
            }
        }
    }

    private fun sanitizeFilename(text: String): String {
        return text.trim().lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifEmpty { "video" }
    }
}

package com.tata.videomaker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
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
    fun buildAssistantScript(prompt: String, style: String, durationSeconds: Int = 12): String {
        val safePrompt = prompt.trim().replace("\"", "'").ifEmpty { "Create a modern product reveal" }
        val safeStyle = style.trim().ifEmpty { "Cinematic" }
        val shot1 = durationSeconds / 4
        val shot2 = durationSeconds / 3
        val shot3 = durationSeconds / 4
        return """
            // Motion scene plan — edit the actions and timings before rendering.
            export default {
              prompt: "$safePrompt",
              style: "$safeStyle",
              durationSeconds: $durationSeconds,
              scenes: [
                { start: 0, duration: $shot1, action: "Fade in background; slowly reveal the graphic." },
                { start: $shot1, duration: $shot2, action: "Animate logo and title with $safeStyle motion." },
                { start: ${shot1 + shot2}, duration: $shot3, action: "Emphasize the product or key message." },
                { start: ${shot1 + shot2 + shot3}, duration: ${durationSeconds - shot1 - shot2 - shot3}, action: "Fade to a branded closing frame." }
              ]
            };
        """.trimIndent()
    }

    private fun createBitmapWithText(text: String, graphic: Bitmap?, width: Int = 1280, height: Int = 720): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.parseColor("#080C18"))
        val graphicBottom = if (graphic != null) height * 0.68f else height * 0.42f

        if (graphic != null) {
            val maxW = width * 0.72f
            val maxH = height * 0.55f
            val scale = minOf(maxW / graphic.width, maxH / graphic.height)
            val drawW = graphic.width * scale
            val drawH = graphic.height * scale
            val dst = android.graphics.RectF(
                (width - drawW) / 2f,
                (height * 0.08f) + (maxH - drawH) / 2f,
                (width + drawW) / 2f,
                (height * 0.08f) + (maxH + drawH) / 2f
            )
            val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; alpha = 245 }
            canvas.drawBitmap(graphic, null, dst, glow)
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 46f
            textAlign = Paint.Align.CENTER
            setShadowLayer(10f, 0f, 2f, Color.rgb(80, 120, 255))
        }
        val lines = wrapText(text, 34)
        val lineHeight = paint.fontSpacing
        var y = graphicBottom + ((height - graphicBottom) - lines.size * lineHeight) / 2f + lineHeight
        lines.forEach { line ->
            canvas.drawText(line, width / 2f, y, paint)
            y += lineHeight
        }
        return bmp
    }

    private fun wrapText(text: String, maxChars: Int): List<String> {
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in text.split(Regex("\\s+"))) {
            if (current.isNotEmpty() && current.length + word.length + 1 > maxChars) {
                lines.add(current.toString())
                current = StringBuilder()
            }
            if (current.isNotEmpty()) current.append(' ')
            current.append(word)
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
        durationSeconds: Int = 12,
        style: String = "Cinematic",
        graphicUri: String? = null,
    ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val outDir = File(context.filesDir, "generated_videos").apply { mkdirs() }
            val safeName = sanitizeFilename("$style-$prompt")
            val imageFile = File(outDir, "$safeName.png")
            val audioFile = File(outDir, "$safeName.wav")
            val outFile = File(outDir, "$safeName-${durationSeconds}s.mp4")

            val sourceGraphic = graphicUri?.let { uriString ->
                try {
                    context.contentResolver.openInputStream(Uri.parse(uriString))?.use(BitmapFactory::decodeStream)
                } catch (_: Exception) { null }
            }
            val poster = createBitmapWithText("${style.uppercase()}\n$prompt", sourceGraphic)
            saveBitmapToFile(poster, imageFile)
            poster.recycle()
            sourceGraphic?.recycle()

            val latch = CountDownLatch(1)
            lateinit var tts: TextToSpeech
            tts = TextToSpeech(context) { result ->
                if (result == TextToSpeech.SUCCESS) {
                    tts.language = Locale.US
                    val params = Bundle().apply { putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "video-narration") }
                    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String) {}
                        override fun onDone(utteranceId: String) { latch.countDown() }
                        override fun onError(utteranceId: String) { latch.countDown() }
                    })
                    try {
                        tts.synthesizeToFile(prompt, params, audioFile, "video-narration")
                    } catch (_: Exception) {
                        latch.countDown()
                    }
                } else latch.countDown()
            }
            latch.await()
            try { tts.shutdown() } catch (_: Exception) { }

            val frames = durationSeconds * 30
            val cmd = listOf(
                "-y", "-loop", "1", "-i", imageFile.absolutePath,
                "-i", audioFile.absolutePath,
                "-vf", "zoompan=z='min(zoom+0.001,1.08)':d=$frames:s=1280x720:fps=30",
                "-c:v", "libx264", "-t", durationSeconds.toString(),
                "-pix_fmt", "yuv420p", "-c:a", "aac", "-shortest", outFile.absolutePath
            ).joinToString(" ")

            FFmpegKitConfig.setLogLevel(com.arthenica.ffmpegkit.LogLevel.AV_LOG_INFO)
            val session = FFmpegKit.execute(cmd)
            if (session.returnCode.isValueSuccess) Pair(true, outFile.absolutePath)
            else Pair(false, session.failStackTrace ?: "FFmpeg could not render the video")
        } catch (ex: Exception) {
            Pair(false, ex.message ?: "Video rendering failed")
        }
    }

    private fun sanitizeFilename(text: String): String = text.trim().lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .take(64)
        .ifEmpty { "video" }
}

package com.tata.videomaker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tata.videomaker.ui.theme.TataVideoMakerTheme
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TataVideoMakerTheme { MotionStudioScreen() } }
    }
}

private data class AssistantMessage(val role: String, val text: String)

@Composable
private fun MotionStudioScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf("Create a cinematic product teaser with glowing logo, modern typography, and a premium reveal") }
    var style by remember { mutableStateOf("Cinematic") }
    var durationText by remember { mutableStateOf("12") }
    var sceneScript by remember { mutableStateOf(VideoGenerator.buildAssistantScript(prompt, style, 12)) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var videoPath by remember { mutableStateOf<String?>(null) }
    var chatInput by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Ready. Connect the backend and Gemini key to chat with the AI assistant.") }
    var isSending by remember { mutableStateOf(false) }
    var isRendering by remember { mutableStateOf(false) }
    val messages = remember { mutableStateListOf(AssistantMessage("Assistant", "Hi! Describe your video or ask for an edit. Messages are sent to Gemini through your configured backend.")) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            imageUri = uri
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) { }
            status = "Graphic attached: ${uri.lastPathSegment ?: "image"}"
        }
    }
    val duration = (durationText.toIntOrNull() ?: 12).coerceIn(3, 30)

    fun sendToAssistant(text: String, requestScenePlan: Boolean = false) {
        val cleanText = text.trim()
        if (cleanText.isEmpty() || isSending) return
        messages.add(AssistantMessage("user", cleanText))
        chatInput = ""
        isSending = true
        status = "Waiting for Gemini…"
        val history = messages.map { AssistantTurn(it.role, it.text) }
        scope.launch {
            try {
                val answer = AssistantApi.reply(history)
                messages.add(AssistantMessage("Assistant", answer))
                if (requestScenePlan) sceneScript = answer
                status = if (requestScenePlan) "AI scene plan generated. Review and edit it before rendering." else "Gemini replied."
            } catch (error: Exception) {
                val detail = error.message ?: "Could not reach the assistant backend."
                messages.add(AssistantMessage("Assistant", "I couldn't respond: $detail"))
                status = detail
            } finally {
                isSending = false
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF090B14), Color(0xFF0F172A), Color(0xFF111827))))) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Tata Motion Studio", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                    Text("AI scene writing + MP4 export", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f))
                }
                Box(Modifier.size(42.dp).clip(CircleShape).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))), contentAlignment = Alignment.Center) {
                    Text("AI", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Video brief", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(value = prompt, onValueChange = { prompt = it }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 5, label = { Text("Describe your animation") })
                    OutlinedTextField(value = durationText, onValueChange = { durationText = it.filter(Char::isDigit).take(2) }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Duration (3–30 seconds)") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Cinematic", "Neon", "Minimal", "Luxury").forEach { option ->
                            Button(onClick = { style = option }, colors = ButtonDefaults.buttonColors(containerColor = if (style == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)) { Text(option) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { imagePicker.launch(arrayOf("image/*")) }, modifier = Modifier.weight(1f)) { Text(if (imageUri == null) "Attach graphic" else "Change graphic") }
                        Button(
                            onClick = { sendToAssistant("Create a concise, editable motion-graphics scene plan for a ${duration}-second $style video. Brief: $prompt. Include timed scenes and visual actions; return plain text, not executable code.", requestScenePlan = true) },
                            modifier = Modifier.weight(1f), enabled = !isSending,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) { Text(if (isSending) "Planning…" else "Ask Gemini for plan") }
                    }
                    if (imageUri != null) Text("Attached: ${imageUri?.lastPathSegment ?: "graphic"}", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Editable scene plan", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(value = sceneScript, onValueChange = { sceneScript = it }, modifier = Modifier.fillMaxWidth(), minLines = 6, maxLines = 12)
                }
            }

            Button(
                onClick = {
                    if (prompt.isBlank()) { status = "Describe the video before rendering." }
                    else {
                        isRendering = true
                        status = "Rendering MP4…"
                        scope.launch {
                            val (success, result) = VideoGenerator.generateOfflineVideo(context, prompt, duration, style)
                            if (success && result != null) { videoPath = result; status = "Render complete. Preview or save your video." }
                            else status = "Render failed: ${result ?: "unknown error"}"
                            isRendering = false
                        }
                    }
                }, modifier = Modifier.fillMaxWidth(), enabled = !isRendering,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text(if (isRendering) "Rendering…" else "Render MP4") }

            if (videoPath != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(Uri.fromFile(File(videoPath!!)), "video/mp4"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) })
                        } catch (_: Exception) { status = "No video player is installed." }
                    }, modifier = Modifier.weight(1f)) { Text("Preview") }
                    Button(onClick = {
                        val result = VideoGalleryHelper.saveVideoToGallery(context, File(videoPath!!))
                        status = if (result.isSuccess) "Saved to gallery." else "Could not save: ${result.exceptionOrNull()?.message ?: "unknown error"}"
                    }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("Save video") }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Gemini assistant", style = MaterialTheme.typography.titleLarge)
                    messages.takeLast(8).forEach { message ->
                        Surface(color = if (message.role == "Assistant") MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(if (message.role == "user") "You" else "Assistant", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text(message.text, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    OutlinedTextField(value = chatInput, onValueChange = { chatInput = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Ask the AI to revise your video") }, enabled = !isSending)
                    Button(onClick = { sendToAssistant(chatInput) }, modifier = Modifier.fillMaxWidth(), enabled = !isSending && chatInput.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                        Text(if (isSending) "Thinking…" else "Send to Gemini")
                    }
                }
            }

            Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Text(status, modifier = Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

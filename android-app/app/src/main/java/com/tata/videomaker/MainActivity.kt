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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.Modifier
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
        setContent {
            TataVideoMakerTheme {
                MotionStudioScreen()
            }
        }
    }
}

private data class AssistantMessage(val role: String, val text: String)

@Composable
private fun MotionStudioScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf("") }
    var style by remember { mutableStateOf("Cinematic") }
    var durationText by remember { mutableStateOf("12") }
    var sceneScript by remember { mutableStateOf(VideoGenerator.buildAssistantScript("", "Cinematic", 12)) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var videoPath by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("Describe an animation, attach a graphic, then ask the assistant for a scene plan.") }
    var chatInput by remember { mutableStateOf("") }
    val messages = remember {
        mutableStateListOf(AssistantMessage("Assistant", "Hi! Describe your video and I’ll turn it into an editable motion-graphics scene script."))
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            imageUri = uri
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) { }
            status = "Graphic attached: ${uri.lastPathSegment ?: "image"}"
        }
    }
    val duration = (durationText.toIntOrNull() ?: 12).coerceIn(3, 30)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Tata Motion Studio", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text("Create motion graphics with an AI scene-writing assistant", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f))

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Your video", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Describe the video and animation") },
                    placeholder = { Text("Make my logo slide in, glow, then reveal a product title…") },
                    minLines = 3,
                    maxLines = 5
                )
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it.filter(Char::isDigit).take(2) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Duration (3–30 seconds)") },
                    singleLine = true
                )
                Text("Visual style", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Cinematic", "Neon", "Minimal").forEach { option ->
                        Button(
                            onClick = { style = option },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (style == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                        ) { Text(option) }
                    }
                }
                Button(onClick = { imagePicker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (imageUri == null) "Add a graphic or logo" else "Change attached graphic")
                }
                if (imageUri != null) Text("Attached: ${imageUri?.lastPathSegment ?: "graphic"}", fontSize = 13.sp)
                Button(
                    onClick = {
                        if (prompt.isBlank()) {
                            status = "Add a video description first."
                        } else {
                            sceneScript = VideoGenerator.buildAssistantScript(prompt, style, duration)
                            messages.add(AssistantMessage("You", prompt))
                            messages.add(AssistantMessage("Assistant", "Scene plan written: opening reveal, graphic motion, feature emphasis, and a closing frame. Review or edit the script below, then render."))
                            status = "Scene script ready to review."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Ask assistant to write the scene") }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Editable scene script", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                OutlinedTextField(
                    value = sceneScript,
                    onValueChange = { sceneScript = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 8,
                    maxLines = 14,
                    textStyle = MaterialTheme.typography.bodySmall
                )
                Text("The script is a constrained scene description; the app does not execute arbitrary generated code.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }

        Button(
            onClick = {
                if (prompt.isBlank()) {
                    status = "Describe your video before rendering."
                } else {
                    status = "Rendering your video on this device…"
                    scope.launch {
                        val (success, result) = VideoGenerator.generateOfflineVideo(
                            context = context,
                            prompt = prompt,
                            durationSeconds = duration,
                            style = style,
                            graphicUri = imageUri?.toString()
                        )
                        if (success && result != null) {
                            videoPath = result
                            status = "Video rendered. Preview it or save it to your gallery."
                            messages.add(AssistantMessage("Assistant", "Your MP4 is ready. You can preview it or save a copy to your gallery."))
                        } else {
                            videoPath = null
                            status = "Render failed: ${result ?: "unknown error"}"
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = status != "Rendering your video on this device…"
        ) { Text("Render MP4") }

        if (videoPath != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = {
                    val playIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.fromFile(File(videoPath!!)), "video/mp4")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(playIntent)
                }, modifier = Modifier.weight(1f)) { Text("Preview") }
                Button(onClick = {
                    val result = VideoGalleryHelper.saveVideoToGallery(context, File(videoPath!!))
                    status = if (result.isSuccess) "Saved to gallery." else "Could not save: ${result.exceptionOrNull()?.message ?: "unknown error"}"
                }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("Save video") }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Assistant", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                messages.takeLast(6).forEach { message ->
                    Surface(color = if (message.role == "Assistant") MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.fillMaxWidth().padding(10.dp)) {
                            Text(message.role, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(message.text, fontSize = 13.sp)
                        }
                    }
                }
                OutlinedTextField(
                    value = chatInput,
                    onValueChange = { chatInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Ask for an edit") },
                    placeholder = { Text("Make the title bigger and delay it by 2 seconds") }
                )
                Button(onClick = {
                    val edit = chatInput.trim()
                    if (edit.isNotEmpty()) {
                        messages.add(AssistantMessage("You", edit))
                        sceneScript = "$sceneScript\n// Requested revision: $edit"
                        messages.add(AssistantMessage("Assistant", "Added that revision to the editable scene script. Review it above before rendering."))
                        chatInput = ""
                        status = "Revision added to the scene script."
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Apply edit to script") }
            }
        }

        Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Text(status, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(8.dp))
    }
}

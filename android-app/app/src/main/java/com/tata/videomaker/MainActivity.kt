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
        setContent {
            TataVideoMakerTheme {
                PremiumMotionStudioScreen()
            }
        }
    }
}

private data class AssistantMessage(val role: String, val text: String)

@Composable
private fun PremiumMotionStudioScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf("Create a cinematic product teaser with glowing logo, modern typography, and a premium reveal") }
    var style by remember { mutableStateOf("Cinematic") }
    var durationText by remember { mutableStateOf("12") }
    var sceneScript by remember { mutableStateOf(VideoGenerator.buildAssistantScript(prompt, style, 12)) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var videoPath by remember { mutableStateOf<String?>(null) }
    var chatInput by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Ready to design your next short video.") }

    val messages = remember {
        mutableStateListOf(
            AssistantMessage("Assistant", "I’ll turn your concept into a motion scene plan, then render it to MP4 on-device."),
            AssistantMessage("User", "Create a modern product reveal for a smart speaker with neon accents."),
            AssistantMessage("Assistant", "Scene plan generated: opening reveal, logo intro, product close-up, title animation, final brand lockup.")
        )
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            imageUri = uri
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) { }
            status = "Graphic selected: ${uri.lastPathSegment ?: "image"}"
        }
    }

    val duration = (durationText.toIntOrNull() ?: 12).coerceIn(3, 30)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF090B14),
                        Color(0xFF0F172A),
                        Color(0xFF111827)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Tata Motion Studio", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                    Text("AI scene writing + MP4 export", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f), style = MaterialTheme.typography.bodyMedium)
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("AI", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Video brief", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 6,
                        label = { Text("Describe your animation") }
                    )

                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it.filter(Char::isDigit).take(2) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Duration (seconds)") }
                    )

                    Text("Style", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Cinematic", "Neon", "Minimal", "Luxury").forEach { option ->
                            Button(
                                onClick = { style = option },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (style == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(option)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { imagePicker.launch(arrayOf("image/*")) }, modifier = Modifier.weight(1f)) {
                            Text(if (imageUri == null) "Attach graphic" else "Replace graphic")
                        }
                        Button(onClick = {
                            if (prompt.isBlank()) {
                                status = "Write a brief before generating the scene."
                            } else {
                                sceneScript = VideoGenerator.buildAssistantScript(prompt, style, duration)
                                messages.add(AssistantMessage("You", prompt))
                                messages.add(AssistantMessage("Assistant", "Scene plan written: opening reveal, graphic motion, emphasis shot, and closing frame."))
                                status = "Scene plan generated."
                            }
                        }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                            Text("Generate plan")
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Scene script", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    OutlinedTextField(
                        value = sceneScript,
                        onValueChange = { sceneScript = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 8,
                        maxLines = 15,
                        label = { Text("Editable motion plan") }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        if (prompt.isBlank()) {
                            status = "Describe the video before rendering."
                            return@Button
                        }
                        status = "Rendering MP4…"
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
                                messages.add(AssistantMessage("Assistant", "Video exported successfully. Preview or save it to your gallery."))
                                status = "Render complete."
                            } else {
                                videoPath = null
                                status = "Render failed: ${result ?: "unknown error"}"
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Render MP4")
                }
                Button(
                    onClick = { if (videoPath != null) { val playIntent = Intent(Intent.ACTION_VIEW).apply { setDataAndType(Uri.fromFile(File(videoPath!!)), "video/mp4"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }; context.startActivity(playIntent) } },
                    modifier = Modifier.weight(1f),
                    enabled = videoPath != null,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Preview")
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Assistant chat", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    messages.takeLast(5).forEach { msg ->
                        Surface(
                            color = if (msg.role == "Assistant") MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(msg.role, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(msg.text, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = chatInput,
                        onValueChange = { chatInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Ask for a revision") }
                    )
                    Button(
                        onClick = {
                            val input = chatInput.trim()
                            if (input.isNotEmpty()) {
                                messages.add(AssistantMessage("You", input))
                                sceneScript = "$sceneScript\n// Revision: $input"
                                messages.add(AssistantMessage("Assistant", "Revision added. Review the scene script before export."))
                                chatInput = ""
                                status = "Revision added to the scene script."
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("Apply edit")
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = status,
                    modifier = Modifier.padding(14.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

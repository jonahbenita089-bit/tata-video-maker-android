package com.tata.videomaker

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tata.videomaker.ui.theme.TataVideoMakerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TataVideoMakerTheme {
                TataVideoMakerApp()
            }
        }
    }
}

@Composable
fun TataVideoMakerApp() {
    val navController = rememberNavController()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AppBottomNavigation(navController)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "projects",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("projects") { ProjectsScreen(navController) }
            composable("create") { CreateScreen(navController) }
            composable("chat") { ChatScreen(navController) }
            composable("settings") { SettingsScreen(navController) }
            composable("menu") { MenuScreen(navController) }
        }
    }
}

@Composable
fun AppBottomNavigation(navController: NavController) {
    val items = listOf(
        BottomNavItem("Projects", "projects", Icons.Default.Home),
        BottomNavItem("Create", "create", Icons.Default.Add),
        BottomNavItem("Chat", "chat", Icons.Default.Message),
        BottomNavItem("Settings", "settings", Icons.Default.Settings),
        BottomNavItem("Menu", "menu", Icons.Default.Menu),
    )

    Surface(
        tonalElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                Column(
                    modifier = Modifier
                        .clickable { navController.navigate(item.route) }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = item.label,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

data class BottomNavItem(
    val label: String,
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

@Composable
fun ProjectsScreen(navController: NavController) {
    val projects = listOf(
        ProjectCardData("Intro Video", "20 min ago", "Ready"),
        ProjectCardData("Product Demo", "1 hour ago", "Rendering"),
        ProjectCardData("Brand Reel", "Yesterday", "Draft"),
        ProjectCardData("Marketing Story", "2 days ago", "Ready")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Projects",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = { navController.navigate("create") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("New")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(projects) { project ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = project.title,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = project.updated,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = project.status,
                                color = if (project.status == "Ready") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                            Button(
                                onClick = { navController.navigate("create") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text("Open")
                            }
                        }
                    }
                }
            }
        }
    }
}

data class ProjectCardData(
    val title: String,
    val updated: String,
    val status: String,
)

@Composable
fun CreateScreen(navController: NavController) {
    var prompt by remember { mutableStateOf("Create a cinematic ad for a smart home device") }
    var style by remember { mutableStateOf("Cinematic") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "Create",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Prompt") },
            singleLine = false,
            minLines = 4
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("Style: $style", color = MaterialTheme.colorScheme.onBackground)

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("Cinematic", "Photoreal", "Cartoon", "Anime", "Explainer").forEach { item ->
                Button(
                    onClick = { style = item },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (style == item) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text(item)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { navController.navigate("chat") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Generate video")
        }
    }
}

@Composable
fun ChatScreen(navController: NavController) {
    val chatState = listOf(
        ChatMessage("Assistant", "I can turn this into a short cinematic storyboard."),
        ChatMessage("User", "Create a 20 second product launch video for a clean smartwatch."),
        ChatMessage("Assistant", "Scene 1: close-up product reveal. Scene 2: UI showcase. Scene 3: lifestyle shot. Scene 4: CTA."),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "New Chat",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(chatState) { message ->
                Surface(
                    color = if (message.sender == "Assistant") MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = message.sender,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = message.text,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = "",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Type a prompt") }
        )
    }
}

data class ChatMessage(
    val sender: String,
    val text: String,
)

@Composable
fun SettingsScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        val settings = listOf(
            "General",
            "Dark mode",
            "AI model",
            "Storage",
            "Privacy",
            "Export",
            "Help"
        )

        settings.forEach { item ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                Text(text = item, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun MenuScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "Menu",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        val menuItems = listOf("New project", "Import", "Export", "Templates", "Docs", "About")
        menuItems.forEach { item ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                Text(text = item, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

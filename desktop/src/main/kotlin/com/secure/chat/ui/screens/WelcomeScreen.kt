package com.secure.chat.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.secure.chat.ui.theme.ElectricCyan
import com.secure.chat.ui.theme.NeonIndigo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    onNavigateToCreate: (serverUrl: String, nickname: String) -> Unit,
    onNavigateToJoin: (serverUrl: String, nickname: String) -> Unit
) {
    var serverUrl by remember { mutableStateOf("wss://chat-2jk8.onrender.com") }
    var nickname by remember { mutableStateOf("") }
    var showServerSettings by remember { mutableStateOf(false) }
    var showNicknameWarning by remember { mutableStateOf(false) }

    if (showNicknameWarning) {
        AlertDialog(
            onDismissRequest = { showNicknameWarning = false },
            title = { Text("Username Required") },
            text = { Text("Please enter a Cyber Identity (Nickname) before creating or joining a room.") },
            confirmButton = {
                Button(onClick = { showNicknameWarning = false }) {
                    Text("OK")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo Cover Image
            Image(
                painter = painterResource("app_logo.png"),
                contentDescription = "ChaT Logo",
                modifier = Modifier
                    .size(120.dp)
                    .background(Color.Transparent)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "ChaT",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Zero-Knowledge End-to-End Encrypted Chat",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Nickname Input Field
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("Cyber Identity (Nickname)") },
                placeholder = { Text("Enter your alias...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.8f),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Button 1: Create Secure Room
            Button(
                onClick = {
                    if (nickname.trim().isEmpty()) {
                        showNicknameWarning = true
                    } else {
                        onNavigateToCreate(serverUrl, nickname.trim())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(NeonIndigo, ElectricCyan)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MeetingRoom,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Create Secure Room",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Button 2: Join Existing Room
            OutlinedButton(
                onClick = {
                    if (nickname.trim().isEmpty()) {
                        showNicknameWarning = true
                    } else {
                        onNavigateToJoin(serverUrl, nickname.trim())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onBackground
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = ElectricCyan
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Join Existing Room",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Server Settings Toggle Button
            TextButton(onClick = { showServerSettings = !showServerSettings }) {
                Text(
                    text = if (showServerSettings) "Hide Server Options" else "⚙️ Server Settings",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            if (showServerSettings) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text("Backend WebSocket URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(0.8f),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

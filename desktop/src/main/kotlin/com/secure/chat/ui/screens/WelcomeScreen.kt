package com.secure.chat.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    
    var acceptedTerms by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

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

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("Terms & Conditions") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "1. Zero-Knowledge E2EE:\nAll chats and attachments are encrypted locally on your device. Decryption keys never leave your device, and the relay server has no access to your messages or files.\n\n" +
                               "2. Zero Retention:\nNo conversation history, files, metadata, or usernames are saved on any database. Rooms are completely deleted from server memory as soon as all users disconnect.\n\n" +
                               "3. User Responsibility:\nYou are solely responsible for keeping your 6-digit Room Codes safe. Lost codes cannot be recovered by the developers under any circumstances.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showTermsDialog = false }) {
                    Text("Close")
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

            Spacer(modifier = Modifier.height(12.dp))

            // Terms Checkbox Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(horizontal = 4.dp)
            ) {
                Checkbox(
                    checked = acceptedTerms,
                    onCheckedChange = { acceptedTerms = it }
                )
                Text(
                    text = "I agree to the ",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
                Text(
                    text = "Terms and Conditions",
                    color = ElectricCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { showTermsDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Button 1: Create Secure Room
            Button(
                enabled = acceptedTerms,
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                ),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(NeonIndigo, ElectricCyan)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            alpha = if (acceptedTerms) 1f else 0.4f
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
                enabled = acceptedTerms,
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

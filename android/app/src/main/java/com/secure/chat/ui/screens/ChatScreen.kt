package com.secure.chat.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.secure.chat.crypto.CryptoUtils
import com.secure.chat.network.ChatMessage
import com.secure.chat.network.ConnectionStatus
import com.secure.chat.network.WebSocketManager
import com.secure.chat.ui.theme.BubbleOther
import com.secure.chat.ui.theme.BubbleSelf
import com.secure.chat.ui.theme.ElectricCyan
import com.secure.chat.ui.theme.EmeraldGreen
import com.secure.chat.ui.theme.NeonIndigo
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    serverUrl: String,
    code: String,
    isAdmin: Boolean,
    onLeave: () -> Unit
) {
    val context = LocalContext.current
    val webSocketManager = remember { WebSocketManager() }
    val connectionStatus by webSocketManager.connectionStatus.collectAsState()
    val messages by webSocketManager.messages.collectAsState()

    // Derive roomId and secretKey locally
    val derived = remember(code) { CryptoUtils.deriveFromCode(code) }
    val roomId = derived.first
    val secretKey = derived.second

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showKickedDialog by remember { mutableStateOf(false) }
    var showTerminateConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(roomId, secretKey) {
        webSocketManager.onRoomTerminated = {
            showKickedDialog = true
        }
        webSocketManager.connect(serverUrl, roomId, secretKey)
    }

    DisposableEffect(Unit) {
        onDispose {
            webSocketManager.disconnect()
        }
    }

    // Auto-scroll to bottom when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Attachment launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                val size = fileDescriptor?.statSize ?: 0
                fileDescriptor?.close()

                if (size > 5 * 1024 * 1024) {
                    Toast.makeText(context, "File too large (Max 5MB)", Toast.LENGTH_SHORT).show()
                    return@let
                }

                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                if (bytes != null) {
                    val mimeType = context.contentResolver.getType(uri) ?: "image/png"
                    val base64Str = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val dataUrl = "data:$mimeType;base64,$base64Str"

                    val msgType = if (mimeType.startsWith("image/")) "image" else "file"
                    webSocketManager.sendMessage(dataUrl, msgType)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read attachment", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Admin Terminated Popup for member
    if (showKickedDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Room Terminated") },
            text = { Text("The Admin has terminated this chat room.") },
            confirmButton = {
                Button(
                    onClick = {
                        showKickedDialog = false
                        webSocketManager.disconnect()
                        onLeave()
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }

    // Confirm Termination dialog for admin
    if (showTerminateConfirm) {
        AlertDialog(
            onDismissRequest = { showTerminateConfirm = false },
            title = { Text("Terminate Room?") },
            text = { Text("Are you sure you want to terminate this room? All members will be disconnected instantly.") },
            confirmButton = {
                Button(
                    onClick = {
                        showTerminateConfirm = false
                        // Send control message to kick everyone
                        val controlJson = JSONObject()
                        controlJson.put("action", "terminate")
                        webSocketManager.sendMessage(controlJson.toString(), "control")
                        
                        Toast.makeText(context, "Room Terminated", Toast.LENGTH_SHORT).show()
                        webSocketManager.disconnect()
                        onLeave()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Terminate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTerminateConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Code: $code",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "E2EE",
                                modifier = Modifier.size(14.dp),
                                tint = ElectricCyan
                            )
                        }

                        // Connection Status Indicator
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val (statusText, statusColor) = when (val status = connectionStatus) {
                                is ConnectionStatus.Connected -> Pair("Encrypted Tunnel Active", EmeraldGreen)
                                is ConnectionStatus.Connecting -> Pair("Connecting...", Color.Yellow)
                                is ConnectionStatus.Disconnected -> Pair("Disconnected", Color.Gray)
                                is ConnectionStatus.Error -> Pair("Error: ${status.message}", MaterialTheme.colorScheme.error)
                            }

                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(statusColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusText,
                                fontSize = 12.sp,
                                color = statusColor
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        webSocketManager.disconnect()
                        onLeave()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Leave")
                    }
                },
                actions = {
                    if (isAdmin) {
                        IconButton(onClick = { showTerminateConfirm = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Terminate Room",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Chat Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message = message)
                }
            }

            // Input Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Attachment Trigger Button
                    IconButton(
                        onClick = { filePickerLauncher.launch("image/*") },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Attach File",
                            tint = ElectricCyan
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Type an encrypted message...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = NeonIndigo
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                val msgToSend = textInput
                                textInput = ""
                                webSocketManager.sendMessage(msgToSend)
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(NeonIndigo, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val alignment = if (message.isFromMe) Alignment.End else Alignment.Start
    val bubbleColor = if (message.isFromMe) BubbleSelf else BubbleOther
    val textColor = Color.White

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (!message.isFromMe) {
            Text(
                text = message.senderNickname,
                fontSize = 11.sp,
                color = ElectricCyan,
                modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)
            )
        }

        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isFromMe) 16.dp else 4.dp,
                bottomEnd = if (message.isFromMe) 4.dp else 16.dp
            ),
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.Start
            ) {
                if (message.msgType == "image") {
                    val imageBitmap = remember(message.text) {
                        base64ToImageBitmap(message.text)
                    }

                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = "Shared Image",
                            modifier = Modifier
                                .widthIn(max = 240.dp)
                                .heightIn(max = 240.dp)
                                .background(Color.DarkGray, RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    } else {
                        Text(
                            text = "[Corrupted Image]",
                            color = Color.Red,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Text(
                        text = message.text,
                        color = textColor,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "E2EE Secured",
                        modifier = Modifier.size(10.dp),
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

private fun base64ToImageBitmap(base64Str: String): ImageBitmap? {
    return try {
        val cleanBase64 = if (base64Str.startsWith("data:")) {
            base64Str.substringAfter("base64,")
        } else {
            base64Str
        }
        val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        bitmap?.asImageBitmap()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

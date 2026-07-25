package com.secure.chat.ui.screens

import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FileOutputStream
import android.graphics.Bitmap
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
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
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
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    serverUrl: String,
    code: String,
    nickname: String,
    isAdmin: Boolean,
    onLeave: () -> Unit
) {
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
    var showQrDialog by remember { mutableStateOf(false) }
    var activeToastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(roomId, secretKey, nickname) {
        webSocketManager.myNickname = if (nickname.isNotBlank()) nickname else "Anon-" + (1000..9999).random()
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
                        
                        activeToastMessage = "Room Terminated"
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

    // QR Code Dialog for all members
    if (showQrDialog) {
        val qrBitmap = remember(code) {
            generateQrBitmap(code)
        }
        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            title = { Text("Scan to Join Room") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier
                                .size(200.dp)
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Room Code: $code",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showQrDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Room Code: $code",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
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
                    IconButton(onClick = { showQrDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "Show Room QR",
                            tint = ElectricCyan
                        )
                    }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
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
                        MessageBubble(message = message, onToast = { activeToastMessage = it })
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
                        // Attachment Trigger Button (AWT FileDialog)
                        IconButton(
                            onClick = {
                                try {
                                    val fileDialog = FileDialog(null as Frame?, "Select File to Send", FileDialog.LOAD)
                                    fileDialog.isVisible = true
                                    val dir = fileDialog.directory
                                    val fileName = fileDialog.file
                                    if (dir != null && fileName != null) {
                                        val file = File(dir, fileName)
                                        if (file.length() > 15 * 1024 * 1024) {
                                            activeToastMessage = "File too large (Max 15MB)"
                                            return@IconButton
                                        }

                                        val bytes = file.readBytes()
                                        val mimeType = java.nio.file.Files.probeContentType(file.toPath()) ?: "application/octet-stream"
                                        val base64Str = java.util.Base64.getEncoder().encodeToString(bytes)
                                        val dataUrl = "data:$mimeType;base64,$base64Str"

                                        var msgType = "file"
                                        if (mimeType.startsWith("image/")) {
                                            msgType = "image"
                                        } else if (mimeType.startsWith("video/")) {
                                            msgType = "video"
                                        } else if (mimeType.startsWith("audio/")) {
                                            msgType = "audio"
                                        }

                                        val rawPayload = "$fileName|$dataUrl"
                                        webSocketManager.sendMessage(rawPayload, msgType)
                                    }
                                } catch (e: Exception) {
                                    activeToastMessage = "Failed to send attachment"
                                }
                            },
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

            // Desktop custom notification snackbar overlay
            if (activeToastMessage != null) {
                LaunchedEffect(activeToastMessage) {
                    kotlinx.coroutines.delay(3000)
                    activeToastMessage = null
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E1E38),
                    tonalElevation = 8.dp
                ) {
                    Text(
                        text = activeToastMessage!!,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage, onToast: (String) -> Unit) {
    val alignment = if (message.isFromMe) Alignment.End else Alignment.Start
    val bubbleColor = if (message.isFromMe) BubbleSelf else BubbleOther
    val textColor = Color.White

    // Parse payload (format: FILENAME|DATAURL)
    val parsed = remember(message.text) {
        if (message.text.contains("|")) {
            val idx = message.text.indexOf("|")
            val name = message.text.substring(0, idx)
            val url = message.text.substring(idx + 1)
            Pair(name, url)
        } else {
            Pair("file", message.text)
        }
    }
    val fileName = parsed.first
    val dataUrl = parsed.second

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
                    val imageBitmap = remember(dataUrl) {
                        base64ToImageBitmap(dataUrl)
                    }

                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = fileName,
                            modifier = Modifier
                                .widthIn(max = 240.dp)
                                .heightIn(max = 240.dp)
                                .background(Color.DarkGray, RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text(
                            text = "[Corrupted Image]",
                            color = Color.Red,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { saveFileToDevice(fileName, dataUrl, onToast) },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Image", color = ElectricCyan, fontSize = 12.sp)
                    }
                } else if (message.msgType == "video" || message.msgType == "audio" || message.msgType == "file") {
                    val iconStr = when(message.msgType) {
                        "video" -> "🎥 Video"
                        "audio" -> "🎵 Audio"
                        else -> "📄 Document"
                    }
                    Column(
                        modifier = Modifier
                            .widthIn(max = 240.dp)
                            .padding(8.dp)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(text = iconStr, fontWeight = FontWeight.Bold, color = ElectricCyan, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = fileName, color = Color.White, fontSize = 12.sp, maxLines = 1)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { saveFileToDevice(fileName, dataUrl, onToast) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save to Device", color = ElectricCyan, fontSize = 12.sp)
                        }
                    }
                } else {
                    Text(
                        text = dataUrl,
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
        val decodedBytes = java.util.Base64.getDecoder().decode(cleanBase64)
        val image = ImageIO.read(ByteArrayInputStream(decodedBytes))
        
        // Convert java.awt.image.BufferedImage to Compose ImageBitmap
        val width = image.width
        val height = image.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val buffer = IntArray(width * height)
        image.getRGB(0, 0, width, height, buffer, 0, width)
        bitmap.setPixels(buffer, 0, width, 0, 0, width, height)
        bitmap.asImageBitmap()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun saveFileToDevice(fileName: String, base64DataUrl: String, onToast: (String) -> Unit) {
    try {
        val cleanBase64 = if (base64DataUrl.startsWith("data:")) {
            base64DataUrl.substringAfter("base64,")
        } else {
            base64DataUrl
        }
        val decodedBytes = java.util.Base64.getDecoder().decode(cleanBase64)

        val userHome = System.getProperty("user.home")
        val downloadsDir = File(userHome, "Downloads")
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        val file = File(downloadsDir, fileName)
        FileOutputStream(file).use { it.write(decodedBytes) }
        
        onToast("Saved to Downloads: $fileName")
    } catch (e: Exception) {
        e.printStackTrace()
        onToast("Failed to save file")
    }
}

private fun generateQrBitmap(content: String): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bmp
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

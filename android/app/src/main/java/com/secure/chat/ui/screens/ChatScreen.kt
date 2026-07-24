package com.secure.chat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.secure.chat.network.ChatMessage
import com.secure.chat.network.ConnectionStatus
import com.secure.chat.network.WebSocketManager
import com.secure.chat.ui.theme.BubbleOther
import com.secure.chat.ui.theme.BubbleSelf
import com.secure.chat.ui.theme.ElectricCyan
import com.secure.chat.ui.theme.EmeraldGreen
import com.secure.chat.ui.theme.NeonIndigo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    serverUrl: String,
    roomId: String,
    secretKey: String,
    onLeave: () -> Unit
) {
    val webSocketManager = remember { WebSocketManager() }
    val connectionStatus by webSocketManager.connectionStatus.collectAsState()
    val messages by webSocketManager.messages.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(roomId, secretKey) {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Room: $roomId",
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
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message.text,
                    color = textColor,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
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

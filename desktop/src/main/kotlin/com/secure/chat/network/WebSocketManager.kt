package com.secure.chat.network

import com.secure.chat.crypto.CryptoUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderNickname: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFromMe: Boolean = false,
    val isEncrypted: Boolean = true,
    val msgType: String = "text"
)

sealed class ConnectionStatus {
    object Disconnected : ConnectionStatus()
    object Connecting : ConnectionStatus()
    object Connected : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
}

class WebSocketManager {

    private var client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private var currentRoomId: String? = null
    private var currentSecretKey: String? = null
    var myNickname: String = "Anon-" + (1000..9999).random()
    var onRoomTerminated: (() -> Unit)? = null

    fun connect(serverUrl: String, roomId: String, secretKey: String) {
        currentRoomId = roomId
        currentSecretKey = secretKey
        _connectionStatus.value = ConnectionStatus.Connecting

        val fullUrl = if (serverUrl.endsWith("/")) "$serverUrl$roomId" else "$serverUrl/$roomId"
        val request = Request.Builder()
            .url(fullUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionStatus.value = ConnectionStatus.Connected
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val sender = json.optString("sender", "Peer")
                    val msgType = json.optString("msgType", "text")
                    val encryptedPayload = json.optString("payload", "")

                    val key = currentSecretKey
                    val decryptedText = if (key != null && encryptedPayload.isNotEmpty()) {
                        CryptoUtils.decrypt(encryptedPayload, key)
                    } else {
                        "[Unencrypted message]"
                    }

                    if (msgType == "control") {
                        try {
                            val controlJson = JSONObject(decryptedText)
                            if (controlJson.optString("action") == "terminate") {
                                onRoomTerminated?.invoke()
                            }
                        } catch (e: Exception) {}
                        return
                    }

                    val incomingMsg = ChatMessage(
                        senderNickname = sender,
                        text = decryptedText,
                        isFromMe = false,
                        msgType = msgType
                    )

                    _messages.value = _messages.value + incomingMsg
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionStatus.value = ConnectionStatus.Disconnected
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionStatus.value = ConnectionStatus.Error(t.localizedMessage ?: "Connection Failed")
            }
        })
    }

    fun sendMessage(text: String, msgType: String = "text"): Boolean {
        val ws = webSocket ?: return false
        val key = currentSecretKey ?: return false
        if (text.isBlank()) return false

        val encryptedPayload = CryptoUtils.encrypt(text, key)

        val json = JSONObject()
        json.put("sender", myNickname)
        json.put("msgType", msgType)
        json.put("payload", encryptedPayload)

        val sent = ws.send(json.toString())
        if (sent && msgType != "control") {
            val myMsg = ChatMessage(
                senderNickname = myNickname,
                text = text,
                isFromMe = true,
                msgType = msgType
            )
            _messages.value = _messages.value + myMsg
        }
        return sent
    }

    fun disconnect() {
        webSocket?.close(1000, "User Left")
        webSocket = null
        _connectionStatus.value = ConnectionStatus.Disconnected
        _messages.value = emptyList()
    }
}

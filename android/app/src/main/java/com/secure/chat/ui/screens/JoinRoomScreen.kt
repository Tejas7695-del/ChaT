package com.secure.chat.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.secure.chat.crypto.CryptoUtils
import com.secure.chat.ui.theme.ElectricCyan
import com.secure.chat.ui.theme.NeonIndigo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinRoomScreen(
    serverUrl: String,
    onBack: () -> Unit,
    onConnect: (code: String) -> Unit
) {
    val context = LocalContext.current
    var roomInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Join Private Room") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Enter Room Code",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enter the 6-digit Room Code shared with you (e.g., A7X9B2)",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = roomInput,
                    onValueChange = {
                        roomInput = it
                        errorMessage = null
                    },
                    label = { Text("Room Code") },
                    leadingIcon = {
                        Icon(Icons.Default.Key, contentDescription = null, tint = ElectricCyan)
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            val scanner = GmsBarcodeScanning.getClient(context)
                            scanner.startScan()
                                .addOnSuccessListener { barcode ->
                                    val scannedCode = barcode.rawValue?.trim()?.uppercase()
                                    if (scannedCode != null && scannedCode.length == 6) {
                                        roomInput = scannedCode
                                        onConnect(scannedCode)
                                    } else if (scannedCode != null) {
                                        errorMessage = "Invalid QR Code format"
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Scanning cancelled or failed", Toast.LENGTH_SHORT).show()
                                }
                        }) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR Code", tint = ElectricCyan)
                        }
                    },
                    isError = errorMessage != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }

            Button(
                onClick = {
                    val code = roomInput.trim().uppercase()
                    if (code.length == 6) {
                        onConnect(code)
                    } else {
                        errorMessage = "Invalid Room Code. It must be exactly 6 characters."
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo)
            ) {
                Text(
                    text = "Connect & Enter",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

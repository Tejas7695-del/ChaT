package com.secure.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.secure.chat.ui.screens.ChatScreen
import com.secure.chat.ui.screens.CreateRoomScreen
import com.secure.chat.ui.screens.JoinRoomScreen
import com.secure.chat.ui.screens.WelcomeScreen
import com.secure.chat.ui.theme.SecureChatTheme
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SecureChatTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "welcome") {
                    composable("welcome") {
                        WelcomeScreen(
                            onNavigateToCreate = { serverUrl ->
                                val encodedUrl = URLEncoder.encode(serverUrl, StandardCharsets.UTF_8.toString())
                                navController.navigate("create/$encodedUrl")
                            },
                            onNavigateToJoin = { serverUrl ->
                                val encodedUrl = URLEncoder.encode(serverUrl, StandardCharsets.UTF_8.toString())
                                navController.navigate("join/$encodedUrl")
                            }
                        )
                    }

                    composable(
                        route = "create/{serverUrl}",
                        arguments = listOf(navArgument("serverUrl") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val encodedUrl = backStackEntry.arguments?.getString("serverUrl") ?: ""
                        val serverUrl = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())

                        CreateRoomScreen(
                            serverUrl = serverUrl,
                            onBack = { navController.popBackStack() },
                            onEnterChat = { roomId, secretKey ->
                                val encUrl = URLEncoder.encode(serverUrl, StandardCharsets.UTF_8.toString())
                                val encKey = URLEncoder.encode(secretKey, StandardCharsets.UTF_8.toString())
                                navController.navigate("chat/$encUrl/$roomId/$encKey") {
                                    popUpTo("welcome")
                                }
                            }
                        )
                    }

                    composable(
                        route = "join/{serverUrl}",
                        arguments = listOf(navArgument("serverUrl") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val encodedUrl = backStackEntry.arguments?.getString("serverUrl") ?: ""
                        val serverUrl = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())

                        JoinRoomScreen(
                            serverUrl = serverUrl,
                            onBack = { navController.popBackStack() },
                            onConnect = { roomId, secretKey ->
                                val encUrl = URLEncoder.encode(serverUrl, StandardCharsets.UTF_8.toString())
                                val encKey = URLEncoder.encode(secretKey, StandardCharsets.UTF_8.toString())
                                navController.navigate("chat/$encUrl/$roomId/$encKey") {
                                    popUpTo("welcome")
                                }
                            }
                        )
                    }

                    composable(
                        route = "chat/{serverUrl}/{roomId}/{secretKey}",
                        arguments = listOf(
                            navArgument("serverUrl") { type = NavType.StringType },
                            navArgument("roomId") { type = NavType.StringType },
                            navArgument("secretKey") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val encUrl = backStackEntry.arguments?.getString("serverUrl") ?: ""
                        val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
                        val encKey = backStackEntry.arguments?.getString("secretKey") ?: ""

                        val serverUrl = URLDecoder.decode(encUrl, StandardCharsets.UTF_8.toString())
                        val secretKey = URLDecoder.decode(encKey, StandardCharsets.UTF_8.toString())

                        ChatScreen(
                            serverUrl = serverUrl,
                            roomId = roomId,
                            secretKey = secretKey,
                            onLeave = {
                                navController.navigate("welcome") {
                                    popUpTo("welcome") { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

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
                            onNavigateToCreate = { serverUrl, nickname ->
                                val encodedUrl = URLEncoder.encode(serverUrl, StandardCharsets.UTF_8.toString())
                                val encodedNick = URLEncoder.encode(nickname, StandardCharsets.UTF_8.toString())
                                navController.navigate("create/$encodedUrl/$encodedNick")
                            },
                            onNavigateToJoin = { serverUrl, nickname ->
                                val encodedUrl = URLEncoder.encode(serverUrl, StandardCharsets.UTF_8.toString())
                                val encodedNick = URLEncoder.encode(nickname, StandardCharsets.UTF_8.toString())
                                navController.navigate("join/$encodedUrl/$encodedNick")
                            }
                        )
                    }

                    composable(
                        route = "create/{serverUrl}/{nickname}",
                        arguments = listOf(
                            navArgument("serverUrl") { type = NavType.StringType },
                            navArgument("nickname") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val encodedUrl = backStackEntry.arguments?.getString("serverUrl") ?: ""
                        val encodedNick = backStackEntry.arguments?.getString("nickname") ?: ""
                        val serverUrl = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())
                        val nickname = URLDecoder.decode(encodedNick, StandardCharsets.UTF_8.toString())

                        CreateRoomScreen(
                            serverUrl = serverUrl,
                            onBack = { navController.popBackStack() },
                            onEnterChat = { code ->
                                val encUrl = URLEncoder.encode(serverUrl, StandardCharsets.UTF_8.toString())
                                val encNick = URLEncoder.encode(nickname, StandardCharsets.UTF_8.toString())
                                navController.navigate("chat/$encUrl/$code/$encNick/true") {
                                    popUpTo("welcome")
                                }
                            }
                        )
                    }

                    composable(
                        route = "join/{serverUrl}/{nickname}",
                        arguments = listOf(
                            navArgument("serverUrl") { type = NavType.StringType },
                            navArgument("nickname") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val encodedUrl = backStackEntry.arguments?.getString("serverUrl") ?: ""
                        val encodedNick = backStackEntry.arguments?.getString("nickname") ?: ""
                        val serverUrl = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())
                        val nickname = URLDecoder.decode(encodedNick, StandardCharsets.UTF_8.toString())

                        JoinRoomScreen(
                            serverUrl = serverUrl,
                            onBack = { navController.popBackStack() },
                            onConnect = { code ->
                                val encUrl = URLEncoder.encode(serverUrl, StandardCharsets.UTF_8.toString())
                                val encNick = URLEncoder.encode(nickname, StandardCharsets.UTF_8.toString())
                                navController.navigate("chat/$encUrl/$code/$encNick/false") {
                                    popUpTo("welcome")
                                }
                            }
                        )
                    }

                    composable(
                        route = "chat/{serverUrl}/{code}/{nickname}/{isAdmin}",
                        arguments = listOf(
                            navArgument("serverUrl") { type = NavType.StringType },
                            navArgument("code") { type = NavType.StringType },
                            navArgument("nickname") { type = NavType.StringType },
                            navArgument("isAdmin") { type = NavType.BoolType }
                        )
                    ) { backStackEntry ->
                        val encUrl = backStackEntry.arguments?.getString("serverUrl") ?: ""
                        val code = backStackEntry.arguments?.getString("code") ?: ""
                        val encNick = backStackEntry.arguments?.getString("nickname") ?: ""
                        val isAdmin = backStackEntry.arguments?.getBoolean("isAdmin") ?: false

                        val serverUrl = URLDecoder.decode(encUrl, StandardCharsets.UTF_8.toString())
                        val nickname = URLDecoder.decode(encNick, StandardCharsets.UTF_8.toString())

                        ChatScreen(
                            serverUrl = serverUrl,
                            code = code,
                            nickname = nickname,
                            isAdmin = isAdmin,
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

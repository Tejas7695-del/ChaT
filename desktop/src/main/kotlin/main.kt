import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import com.secure.chat.ui.screens.WelcomeScreen
import com.secure.chat.ui.screens.CreateRoomScreen
import com.secure.chat.ui.screens.JoinRoomScreen
import com.secure.chat.ui.screens.ChatScreen
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val NeonIndigo = Color(0xFF6F00FF)
val ElectricCyan = Color(0xFF00F0FF)
val EmeraldGreen = Color(0xFF00FF88)

val BubbleSelf = Color(0xFF2E1A47)
val BubbleOther = Color(0xFF1E1E38)

val DarkColorScheme = darkColorScheme(
    primary = NeonIndigo,
    secondary = ElectricCyan,
    background = Color(0xFF0F0F1B),
    surface = Color(0xFF16162A),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFFE2E2EC),
    onSurface = Color(0xFFE2E2EC)
)

@Composable
fun DesktopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

sealed class Screen {
    object Welcome : Screen()
    data class CreateRoom(val serverUrl: String, val nickname: String) : Screen()
    data class JoinRoom(val serverUrl: String, val nickname: String) : Screen()
    data class Chat(val serverUrl: String, val code: String, val nickname: String, val isAdmin: Boolean) : Screen()
}

@Composable
fun DesktopApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Welcome) }

    DesktopTheme {
        when (val screen = currentScreen) {
            is Screen.Welcome -> {
                WelcomeScreen(
                    onNavigateToCreate = { serverUrl, nickname ->
                        currentScreen = Screen.CreateRoom(serverUrl, nickname)
                    },
                    onNavigateToJoin = { serverUrl, nickname ->
                        currentScreen = Screen.JoinRoom(serverUrl, nickname)
                    }
                )
            }
            is Screen.CreateRoom -> {
                CreateRoomScreen(
                    serverUrl = screen.serverUrl,
                    onBack = { currentScreen = Screen.Welcome },
                    onEnterChat = { code ->
                        currentScreen = Screen.Chat(screen.serverUrl, code, screen.nickname, true)
                    }
                )
            }
            is Screen.JoinRoom -> {
                JoinRoomScreen(
                    serverUrl = screen.serverUrl,
                    onBack = { currentScreen = Screen.Welcome },
                    onConnect = { code ->
                        currentScreen = Screen.Chat(screen.serverUrl, code, screen.nickname, false)
                    }
                )
            }
            is Screen.Chat -> {
                ChatScreen(
                    serverUrl = screen.serverUrl,
                    code = screen.code,
                    nickname = screen.nickname,
                    isAdmin = screen.isAdmin,
                    onLeave = {
                        currentScreen = Screen.Welcome
                    }
                )
            }
        }
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "ChaT - Secure E2EE Chat Space",
        state = rememberWindowState(width = 800.dp, height = 700.dp),
        icon = painterResource("app_logo.png")
    ) {
        DesktopApp()
    }
}

package com.secure.chat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DeepSlate = Color(0xFF0A0E17)
val CardBackground = Color(0x99141821)
val NeonIndigo = Color(0xFF6366F1)
val ElectricCyan = Color(0xFF06B6D4)
val EmeraldGreen = Color(0xFF10B981)
val TextPrimary = Color(0xFFF9FAFB)
val TextSecondary = Color(0xFF9CA3AF)
val BubbleSelf = Color(0xFF4F46E5)
val BubbleOther = Color(0xFF1E293B)

private val DarkColorScheme = darkColorScheme(
    primary = NeonIndigo,
    secondary = ElectricCyan,
    background = DeepSlate,
    surface = CardBackground,
    onPrimary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun SecureChatTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

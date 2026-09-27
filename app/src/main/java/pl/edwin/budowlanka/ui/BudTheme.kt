package pl.edwin.budowlanka.ui

import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalTextSelectionColors

val BudOrange = Color(0xFFFF7A00)
val BudOrangeStrong = Color(0xFFFF9400)
val BudOrangeLight = Color(0xFFFFB14A)
val BudSelectedSoft = Color(0xFF4A290B)
val BudSelectedStrong = Color(0xFF6A3908)
val BudBg = Color(0xFF0B0F12)
val BudPanel = Color(0xFF151A1E)
val BudPanel2 = Color(0xFF1B2024)
val BudLine = Color(0xFF343A40)
val BudText = Color(0xFFF4F5F6)
val BudMuted = Color(0xFFA9AFB5)
val BudGreen = Color(0xFF42C765)

private val BudColors = darkColorScheme(
    primary = BudOrangeStrong,
    onPrimary = Color.Black,
    primaryContainer = BudSelectedStrong,
    onPrimaryContainer = Color.White,
    secondary = BudOrange,
    onSecondary = Color.Black,
    secondaryContainer = BudSelectedSoft,
    onSecondaryContainer = Color.White,
    background = BudBg,
    onBackground = BudText,
    surface = BudPanel,
    onSurface = BudText,
    surfaceVariant = BudPanel2,
    onSurfaceVariant = BudMuted,
    outline = BudLine,
    outlineVariant = Color(0xFF4B5157),
    error = Color(0xFFFF5C5C)
)

private val BudTextSelection = TextSelectionColors(
    handleColor = BudOrangeLight,
    backgroundColor = BudSelectedStrong
)

@Composable
fun BudTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTextSelectionColors provides BudTextSelection) {
        MaterialTheme(
            colorScheme = BudColors,
            content = content
        )
    }
}

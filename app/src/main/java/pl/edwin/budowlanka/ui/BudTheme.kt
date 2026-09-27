package pl.edwin.budowlanka.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BudOrange = Color(0xFFFF7A00)
val BudOrangeDark = Color(0xFFE86500)
val BudSelected = Color(0xFFFFB000)
val BudSelectedStrong = Color(0xFFFFC247)
val BudSelectedSoft = Color(0x33FFB000)
val BudOrangeStrong = BudSelectedStrong
val BudOrangeLight = Color(0xFFFFD166)
val BudBg = Color(0xFF0B0F12)
val BudPanel = Color(0xFF151A1E)
val BudPanel2 = Color(0xFF1B2024)
val BudLine = Color(0xFF2A3035)
val BudText = Color(0xFFF4F5F6)
val BudMuted = Color(0xFF9AA0A6)
val BudGreen = Color(0xFF42C765)

private val BudColors = darkColorScheme(
    primary = BudSelected,
    onPrimary = Color.Black,
    primaryContainer = BudSelectedSoft,
    onPrimaryContainer = BudSelectedStrong,
    secondary = BudOrange,
    onSecondary = Color.Black,
    background = BudBg,
    onBackground = BudText,
    surface = BudPanel,
    onSurface = BudText,
    surfaceVariant = BudPanel2,
    onSurfaceVariant = BudMuted,
    outline = BudLine,
    outlineVariant = Color(0xFF3A4045),
    error = Color(0xFFFF5C5C)
)

@Composable
fun BudTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BudColors,
        content = content
    )
}

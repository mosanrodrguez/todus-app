package cu.todus.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Red,
    onPrimary = Color.White,
    primaryContainer = RedDark,
    background = Background,
    onBackground = TextWhite,
    surface = Surface,
    onSurface = TextWhite,
    surfaceVariant = Input,
    onSurfaceVariant = TextMuted,
    outline = BorderColor,
    error = Red
)

@Composable
fun TodusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

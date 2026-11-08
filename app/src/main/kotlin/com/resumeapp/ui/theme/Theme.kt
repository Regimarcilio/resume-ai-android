package com.resumeapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6366F1),
    primaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF8B5CF6),
    secondaryContainer = Color(0xFFEDE9FE),
    tertiary = Color(0xFF06B6D4),
    tertiaryContainer = Color(0xFFCFFAFE),
    surface = Color(0xFF1E1E1E),
    surfaceContainerLow = Color(0xFF2A2A2A),
    surfaceContainerHigh = Color(0xFF3A3A3A),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFFC4C4C4),
    outline = Color(0xFF5A5A5A),
    error = Color(0xFFFCA5A5),
    errorContainer = Color(0xFF7F1D1D),
    warning = Color(0xFFFBBF24),
    warningContainer = Color(0xFF713F12)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4F46E5),
    primaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF7C3AED),
    secondaryContainer = Color(0xFFEDE9FE),
    tertiary = Color(0xFF0891B2),
    tertiaryContainer = Color(0xFFCFFAFE),
    surface = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainerHigh = Color(0xFFF1F5F9),
    onSurface = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626),
    errorContainer = Color(0xFFFEF2F2),
    warning = Color(0xFFD97706),
    warningContainer = Color(0xFFFFFBE6)
)

@Composable
fun ResumeTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!darkTheme) {
        SideEffect {
            val windowInsetsController = WindowInsetsControllerCompat(window, view)
            windowInsetsController.isAppearanceLightStatusBars = true
            windowInsetsController.isAppearanceLightNavigationBars = true
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
        }
    } else {
        SideEffect {
            val windowInsetsController = WindowInsetsControllerCompat(window, view)
            windowInsetsController.isAppearanceLightStatusBars = false
            windowInsetsController.isAppearanceLightNavigationBars = false
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
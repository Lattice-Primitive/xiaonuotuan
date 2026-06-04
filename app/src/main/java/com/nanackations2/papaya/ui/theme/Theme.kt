package com.nanackations2.papaya.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = PrimaryDark,
    secondary = Secondary,
    onSecondary = Color.White,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = SecondaryDark,
    tertiary = Tertiary,
    onTertiary = Color.White,
    tertiaryContainer = TertiaryLight,
    onTertiaryContainer = TertiaryDark,
    error = Error,
    onError = Color.White,
    errorContainer = ErrorLight,
    onErrorContainer = Error,
    background = BackgroundLight,
    onBackground = TextOnLight,
    surface = SurfaceLight,
    onSurface = TextOnLight,
    surfaceVariant = SurfaceContainer,
    onSurfaceVariant = TextOnLightSecondary,
    outline = DividerColor,
    outlineVariant = DividerColor,
    inverseSurface = SurfaceDark,
    inverseOnSurface = TextOnDark,
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9B8AFB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2D2A5E),
    onPrimaryContainer = Color(0xFFD4CFFF),
    secondary = Color(0xFF4AD6B5),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1A3D33),
    onSecondaryContainer = Color(0xFFB8F0DF),
    tertiary = Color(0xFFFF8A8A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF4A2020),
    onTertiaryContainer = Color(0xFFFFDADA),
    error = Color(0xFFFF8A8A),
    onError = Color.White,
    errorContainer = Color(0xFF4A2020),
    onErrorContainer = Color(0xFFFFDADA),
    background = BackgroundDark,
    onBackground = TextOnDark,
    surface = SurfaceDark,
    onSurface = TextOnDark,
    surfaceVariant = SurfaceContainerDark,
    onSurfaceVariant = TextOnDarkSecondary,
    outline = DividerColorDark,
    outlineVariant = DividerColorDark,
    inverseSurface = SurfaceLight,
    inverseOnSurface = TextOnLight,
)

@Composable
fun PapayaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

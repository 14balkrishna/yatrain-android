package com.yatrain.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary          = AppNavy,
    onPrimary        = AppCard,
    primaryContainer = Color(0xFFDCE8FF),
    secondary        = AppOrange,
    onSecondary      = AppCard,
    background       = AppBackground,
    surface          = AppCard,
    onBackground     = AppTitle,
    onSurface        = AppTitle,
    outline          = AppBorder,
    error            = AppError
)

private val DarkColorScheme = darkColorScheme(
    primary          = Color(0xFF4D80D0),
    onPrimary        = AppNavyDark,
    primaryContainer = AppNavy,
    secondary        = AppOrange,
    onSecondary      = AppNavyDark,
    background       = AppNavyDark,
    surface          = AppCardDark,
    onBackground     = AppCard,
    onSurface        = AppCard,
    outline          = AppBorderDark,
    error            = AppError
)

// Never use dynamic colors — always enforce the Yatrain custom palette.
@Composable
fun YatrainTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = YatrainTypography,
        content     = content
    )
}

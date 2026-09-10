package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = MujtamaPrimaryLight,
    onPrimary = MujtamaDarkBackground,
    primaryContainer = MujtamaPrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = MujtamaTealLight,
    onSecondary = Color.Black,
    tertiary = MujtamaGold,
    onTertiary = Color.Black,
    background = MujtamaDarkBackground,
    onBackground = MujtamaDarkText,
    surface = MujtamaDarkSurface,
    onSurface = MujtamaDarkText,
    surfaceVariant = MujtamaDarkSurfaceVariant,
    onSurfaceVariant = MujtamaDarkTextMuted,
    outline = Color(0xFF4A4268)
)

private val LightColorScheme = lightColorScheme(
    primary = MujtamaPrimary,
    onPrimary = MujtamaOnPrimary,
    primaryContainer = MujtamaPrimaryLight,
    onPrimaryContainer = MujtamaPrimaryDark,
    secondary = MujtamaTeal,
    onSecondary = Color.White,
    tertiary = MujtamaGold,
    onTertiary = Color.Black,
    background = MujtamaLightBackground,
    onBackground = MujtamaLightText,
    surface = MujtamaLightSurface,
    onSurface = MujtamaLightText,
    surfaceVariant = MujtamaLightSurfaceVariant,
    onSurfaceVariant = MujtamaLightTextMuted,
    outline = Color(0xFFD6CEE5)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep cohesive brand palette
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

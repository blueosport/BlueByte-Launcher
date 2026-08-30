package com.bluebyte.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimary,
    secondary = BlueSecondary,
    tertiary = WhiteText,
    background = BlackBg,
    surface = BlackBg,
    onPrimary = WhiteText,
    onSecondary = WhiteText,
    onTertiary = BlackBg,
    onBackground = WhiteText,
    onSurface = WhiteText,
)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    secondary = BlueSecondary,
    tertiary = WhiteText,
    background = BlackBg,
    surface = BlackBg,
    onPrimary = WhiteText,
    onSecondary = WhiteText,
    onTertiary = BlackBg,
    onBackground = WhiteText,
    onSurface = WhiteText,
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Set to false to force our Blue/Black theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

package com.example.myapplication.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RedMagentaColorScheme = lightColorScheme(
    primary = RedPrimary,
    secondary = MagentaSecondary,
    tertiary = PinkTertiary,
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Ignore dark theme
    dynamicColor: Boolean = false, // Ignore dynamic color
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RedMagentaColorScheme,
        typography = Typography,
        content = content
    )
}

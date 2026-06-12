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

private val DarkColorScheme =
  darkColorScheme(
    primary = DeepBlue,
    secondary = CyanAccent,
    tertiary = HintGray,
    background = DarkNavy,
    surface = PremiumNavyCard,
    onPrimary = BrightWhite,
    onSecondary = DarkNavy,
    onBackground = BrightWhite,
    onSurface = BrightWhite
  )

private val LightColorScheme =
  lightColorScheme(
    primary = DeepBlue,
    secondary = CyanAccent,
    tertiary = HintGray,
    background = BrightWhite,
    surface = Color(0xFFF1F5F9),
    onPrimary = BrightWhite,
    onSecondary = DarkNavy,
    onBackground = DarkNavy,
    onSurface = DarkNavy
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark premium style by default
  dynamicColor: Boolean = false, // Disable dynamic color to stick to our beautiful cyber slate theme
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

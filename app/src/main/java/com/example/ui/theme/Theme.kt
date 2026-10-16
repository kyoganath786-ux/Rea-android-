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
  primary = CyanPrimary,
  onPrimary = Color(0xFF00363D),
  primaryContainer = Color(0xFF004F58),
  onPrimaryContainer = CyanPrimaryLight,

  secondary = TealAccent,
  onSecondary = Color(0xFF003831),
  secondaryContainer = Color(0xFF005147),
  onSecondaryContainer = Color(0xFF73F8D4),

  tertiary = VioletPurple,
  onTertiary = Color(0xFFFFFFFF),
  tertiaryContainer = Color(0xFF4C1D95),
  onTertiaryContainer = Color(0xFFDDD6FE),

  background = SlateBackgroundDark,
  onBackground = TextPrimaryDark,

  surface = SlateSurfaceDark,
  onSurface = TextPrimaryDark,
  surfaceVariant = SlateSurfaceVariantDark,
  onSurfaceVariant = TextSecondaryDark,

  outline = SlateBorderDark,
  error = RoseError,
  onError = Color.White
)

private val LightColorScheme = lightColorScheme(
  primary = Color(0xFF006877),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFABEDFF),
  onPrimaryContainer = Color(0xFF001F25),

  secondary = Color(0xFF006B5E),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFF86F8DE),
  onSecondaryContainer = Color(0xFF00201B),

  tertiary = Color(0xFF6B4FA0),
  onTertiary = Color.White,
  tertiaryContainer = Color(0xFFEADBFF),
  onTertiaryContainer = Color(0xFF26005A),

  background = SlateBackgroundLight,
  onBackground = TextPrimaryLight,

  surface = SlateSurfaceLight,
  onSurface = TextPrimaryLight,
  surfaceVariant = SlateSurfaceVariantLight,
  onSurfaceVariant = TextSecondaryLight,

  outline = SlateBorderLight,
  error = RoseError,
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep intentional brand colors
  content: @Composable () -> Unit,
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

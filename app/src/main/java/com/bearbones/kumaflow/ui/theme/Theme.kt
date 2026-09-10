package com.bearbones.kumaflow.ui.theme

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

// 1. Dark Mode Color Scheme
private val DarkColorScheme = darkColorScheme(
    primary = BearRust,
    onPrimary = PitchBlack,
    primaryContainer = CardDark,
    onPrimaryContainer = CreamyText,
    secondary = BearTan,
    onSecondary = PitchBlack,
    secondaryContainer = Color(0xFF382319),
    onSecondaryContainer = CreamyText,
    tertiary = BearGreen,
    onTertiary = PitchBlack,
    background = PitchBlack,
    onBackground = CreamyText,
    surface = CardDark,
    onSurface = CreamyText,
    surfaceVariant = Color(0xFF382319),
    onSurfaceVariant = AshBear,
    surfaceContainerLowest = Color(0xFF120A07),
    surfaceContainerLow = PitchBlack,
    surfaceContainer = CardDark,
    surfaceContainerHigh = Color(0xFF382319),
    surfaceContainerHighest = Color(0xFF452B1F),
    outline = AshBear,
    outlineVariant = Color(0xFF3D251A),
    error = BearRed,
    onError = PitchBlack,
    errorContainer = Color(0xFF5A1605),
    onErrorContainer = CreamyText
)

// 2. Light Mode Color Scheme
private val LightColorScheme = lightColorScheme(
    primary = DeepGrizzly,
    onPrimary = Color.White,
    primaryContainer = CreamyBelly,
    onPrimaryContainer = DeepGrizzly,
    secondary = BearRust,
    onSecondary = Color.White,
    secondaryContainer = BearTan,
    onSecondaryContainer = BearCharcoal,
    tertiary = BearGreen,
    onTertiary = Color.White,
    background = Color(0xFFF6F7F9),
    onBackground = DeepGrizzly,
    surface = Color.White,
    onSurface = DeepGrizzly,
    surfaceVariant = CreamyBelly,
    onSurfaceVariant = BearCharcoal,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBF8F5),
    surfaceContainer = Color(0xFFF5EDE0),
    surfaceContainerHigh = BearTan,
    surfaceContainerHighest = Color(0xFFDEC3A3),
    outline = AshBear,
    outlineVariant = Color(0xFFE0D0BE),
    error = BearRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

// Ensure a data type exists to represent the application settings state, for example:
enum class ThemePreference { LIGHT, DARK, SYSTEM }

@Composable
fun KumaFlowTheme(
    // 1. Modify the parameter to directly accept state from DataStore or ViewModel
    themePref: ThemePreference = ThemePreference.SYSTEM,
    // MUST BE FALSE: Prevents device-specific dynamic colors (e.g., heavily customized OS themes) from overriding the intended design language.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // 2. Strict evaluation for theme mode:
    val isDark = when (themePref) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }

    // 3. Apply the resolved isDark boolean to the color scheme assignment
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    // Synchronize the status bar color (clock and battery icons) with the active background theme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            // 4. Toggle the status bar icon contrast based on the isDark state
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

val LocalIsBrutal = androidx.compose.runtime.compositionLocalOf { false }
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
    background = PitchBlack,
    surface = CardDark, // Background color for card and menu elements
    onPrimary = Color.White, // Text color overlaid on primary color
    onBackground = CreamyText, // Primary text color for Dark Mode
    onSurface = CreamyText
)

// 2. Light Mode Color Scheme
private val LightColorScheme = lightColorScheme(
    primary = DeepGrizzly,
    background = CreamyBelly,
    surface = CreamyBelly,
    onPrimary = Color.White,
    onBackground = DeepGrizzly,
    onSurface = DeepGrizzly
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
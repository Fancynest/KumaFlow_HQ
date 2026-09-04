package com.bearbones.kumaflow.ui.components

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Adaptive screen dimension helpers for KumaFlow.
 * Standard Android Material 3 breakpoints:
 * - Compact (Phone): < 600dp
 * - Medium (Small tablet, foldable unfolded, large phone landscape): 600dp - 839dp
 * - Expanded (Standard & large tablet, desktop): >= 840dp
 */
data class KumaWindowSize(
    val screenWidthDp: Int,
    val screenHeightDp: Int,
    val isTablet: Boolean,
    val isExpanded: Boolean,
    val isLandscape: Boolean
)

@Composable
fun rememberKumaWindowSize(): KumaWindowSize {
    val config = LocalConfiguration.current
    val width = config.screenWidthDp
    val height = config.screenHeightDp
    val isTablet = width >= 600 || config.smallestScreenWidthDp >= 600
    val isExpanded = width >= 840
    val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE

    return KumaWindowSize(
        screenWidthDp = width,
        screenHeightDp = height,
        isTablet = isTablet,
        isExpanded = isExpanded,
        isLandscape = isLandscape
    )
}

val isTabletDevice: Boolean
    @Composable
    get() {
        val config = LocalConfiguration.current
        return config.screenWidthDp >= 600 || config.smallestScreenWidthDp >= 600
    }

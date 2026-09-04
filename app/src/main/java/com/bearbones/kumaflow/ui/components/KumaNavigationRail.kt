package com.bearbones.kumaflow.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.circle
import com.bearbones.kumaflow.*
import com.bearbones.kumaflow.ui.theme.*
import com.bearbones.kumaflow.utils.MorphPolygonShape
import com.bearbones.kumaflow.utils.bouncyScale
import com.bearbones.kumaflow.utils.kumaClickable

@Composable
fun KumaNavigationRail(
    pagerState: PagerState,
    haptic: HapticFeedback,
    tiltState: State<TiltState>,
    isNavMotionEnabled: Boolean = true,
    isSpeedDialOpen: Boolean,
    onToggleSpeedDial: () -> Unit,
    onOpenNormalEntry: () -> Unit,
    onOpenOcrEntry: () -> Unit,
    targetFabShapeIndex: Int,
    m3Shapes: List<androidx.graphics.shapes.RoundedPolygon>,
    onOpenSettings: () -> Unit,
    onItemSelected: (Int) -> Unit
) {
    val isLiquidGlass = LocalIsLiquidGlass.current
    val isDark = LocalIsDark.current

    val railShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)

    val fabInteractionSource = remember { MutableInteractionSource() }
    val isFabPressed by fabInteractionSource.collectIsPressedAsState()

    val fabProgress by animateFloatAsState(
        targetValue = if (isFabPressed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "railFabMorph"
    )

    val morph = remember(targetFabShapeIndex) {
        Morph(RoundedPolygon.circle(), m3Shapes[targetFabShapeIndex])
    }
    val fabShape = remember(fabProgress) { MorphPolygonShape(morph, fabProgress) }

    val bgColor = if (isLiquidGlass) {
        if (isDark) Color(0xFF2C2C2E).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.7f)
    } else {
        AppPrimary()
    }
    val fgColor = if (isLiquidGlass) AppPrimary() else Color.White

    val dialSpringSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    val dialProgress by animateFloatAsState(
        targetValue = if (isSpeedDialOpen) 1f else 0f,
        animationSpec = dialSpringSpec,
        label = "railDialProgress"
    )
    val fabRotation by animateFloatAsState(
        targetValue = if (isSpeedDialOpen) 45f else 0f,
        animationSpec = dialSpringSpec,
        label = "railFabRotation"
    )

    val dial1Interaction = remember { MutableInteractionSource() }
    val dial2Interaction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .width(88.dp)
            .fillMaxHeight()
            .zIndex(10f)
    ) {
        // 1. Sidebar Container Surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    shape = railShape
                    clip = true
                    shadowElevation = if (isLiquidGlass) 0f else 10.dp.toPx()
                }
                .background(AppSurface())
                .border(
                    width = if (isLiquidGlass) 1.dp else 0.5.dp,
                    color = if (isLiquidGlass) Color.White.copy(0.25f) else AppText().copy(alpha = 0.08f),
                    shape = railShape
                )
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Brand Header Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppPrimary().copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.AccountBalanceWallet,
                        contentDescription = "KumaFlow",
                        tint = AppPrimary(),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Action FAB (+)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(54.dp)
                            .bouncyScale(fabInteractionSource)
                            .graphicsLayer {
                                shadowElevation = if (isLiquidGlass) 0f else 8.dp.toPx()
                                shape = fabShape
                                clip = true
                            }
                            .background(bgColor)
                            .border(
                                width = 2.dp,
                                color = AppSurface(),
                                shape = fabShape
                            )
                            .border(
                                width = if (isLiquidGlass) 1.dp else 0.dp,
                                color = if (isLiquidGlass) Color.White.copy(0.3f) else Color.Transparent,
                                shape = fabShape
                            )
                            .kumaClickable(
                                interactionSource = fabInteractionSource,
                                indication = null,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onToggleSpeedDial()
                                }
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Menu Tambah",
                            modifier = Modifier
                                .size(26.dp)
                                .graphicsLayer { rotationZ = fabRotation },
                            tint = fgColor
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Navigation Items with Vertical Organic Squircle Slider
                val itemHeight = 64.dp
                val organicTabShape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 14.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 20.dp
                )

                val currentSlotFloat = remember(pagerState.currentPage, pagerState.currentPageOffsetFraction) {
                    (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, 3f)
                }

                Box(
                    modifier = Modifier
                        .width(76.dp)
                        .height(itemHeight * 4),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Vertical Sliding Organic Indicator
                    Box(
                        modifier = Modifier
                            .offset(y = itemHeight * currentSlotFloat)
                            .fillMaxWidth()
                            .height(itemHeight)
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                            .clip(organicTabShape)
                            .background(
                                if (isDark) Color.White.copy(alpha = 0.12f)
                                else AppPrimary().copy(alpha = 0.15f)
                            )
                            .border(
                                1.dp,
                                if (isDark) Color.White.copy(alpha = 0.06f)
                                else Color.Black.copy(alpha = 0.04f),
                                organicTabShape
                            )
                    )

                    Column(modifier = Modifier.fillMaxSize()) {
                        RailSlotItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(itemHeight),
                            icon = Icons.Rounded.Home,
                            label = AppStr.home,
                            isSelected = pagerState.currentPage == 0,
                            isNavMotionEnabled = isNavMotionEnabled,
                            shape = organicTabShape,
                            tiltState = tiltState,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onItemSelected(0)
                            }
                        )

                        RailSlotItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(itemHeight),
                            icon = Icons.Rounded.History,
                            label = AppStr.hist,
                            isSelected = pagerState.currentPage == 1,
                            isNavMotionEnabled = isNavMotionEnabled,
                            shape = organicTabShape,
                            tiltState = tiltState,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onItemSelected(1)
                            }
                        )

                        RailSlotItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(itemHeight),
                            icon = Icons.Rounded.AccountBalanceWallet,
                            label = if (AppStr.isId) "Tabungan" else "Savings",
                            isSelected = pagerState.currentPage == 2,
                            isNavMotionEnabled = isNavMotionEnabled,
                            shape = organicTabShape,
                            tiltState = tiltState,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onItemSelected(2)
                            }
                        )

                        RailSlotItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(itemHeight),
                            icon = Icons.Rounded.Equalizer,
                            label = AppStr.rep,
                            isSelected = pagerState.currentPage == 3,
                            isNavMotionEnabled = isNavMotionEnabled,
                            shape = organicTabShape,
                            tiltState = tiltState,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onItemSelected(3)
                            }
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                // Bottom Settings Shortcut
                val settingsInteraction = remember { MutableInteractionSource() }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .clip(organicTabShape)
                        .bouncyScale(settingsInteraction)
                        .clickable(
                            interactionSource = settingsInteraction,
                            indication = null,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onOpenSettings()
                            }
                        )
                        .padding(vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Rounded.Settings,
                        contentDescription = AppStr.set,
                        tint = AppText().copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = AppStr.set,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppText().copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 2. Speed Dial Items (Fanning out to the right into the content area)
        if (dialProgress > 0.01f) {
            Box(
                modifier = Modifier.wrapContentSize(align = Alignment.TopStart, unbounded = true)
            ) {
                // Speed Dial Option 1: Catat Normal (Aligned with FAB)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .wrapContentSize(align = Alignment.TopStart, unbounded = true)
                        .statusBarsPadding()
                        .padding(top = 84.dp)
                        .offset(x = 96.dp * dialProgress)
                        .graphicsLayer {
                            scaleX = dialProgress
                            scaleY = dialProgress
                            alpha = dialProgress.coerceIn(0f, 1f)
                        }
                        .bouncyScale(dial1Interaction, scaleDown = 0.88f)
                        .clickable(
                            interactionSource = dial1Interaction,
                            indication = null,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onOpenNormalEntry()
                            }
                        )
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .graphicsLayer {
                                shadowElevation = if (isLiquidGlass) 0f else 6.dp.toPx()
                                shape = RoundedCornerShape(16.dp)
                                clip = true
                            }
                            .background(bgColor)
                            .border(
                                width = if (isLiquidGlass) 1.dp else 0.dp,
                                color = if (isLiquidGlass) Color.White.copy(0.3f) else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = AppStr.manualEntryTitle,
                            modifier = Modifier.size(20.dp),
                            tint = fgColor
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AppSurface(),
                        border = BorderStroke(1.dp, if (isLiquidGlass) Color.White.copy(0.2f) else AppText().copy(alpha = 0.12f)),
                        shadowElevation = 8.dp
                    ) {
                        Text(
                            AppStr.manualEntryTitle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppText(),
                            softWrap = false,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }

                // Speed Dial Option 2: Scan Struk AI OCR (Below Option 1)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .wrapContentSize(align = Alignment.TopStart, unbounded = true)
                        .statusBarsPadding()
                        .padding(top = 146.dp)
                        .offset(x = 96.dp * dialProgress)
                        .graphicsLayer {
                            scaleX = dialProgress
                            scaleY = dialProgress
                            alpha = dialProgress.coerceIn(0f, 1f)
                        }
                        .bouncyScale(dial2Interaction, scaleDown = 0.88f)
                        .clickable(
                            interactionSource = dial2Interaction,
                            indication = null,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onOpenOcrEntry()
                            }
                        )
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .graphicsLayer {
                                shadowElevation = if (isLiquidGlass) 0f else 6.dp.toPx()
                                shape = RoundedCornerShape(16.dp)
                                clip = true
                            }
                            .background(bgColor)
                            .border(
                                width = if (isLiquidGlass) 1.dp else 0.dp,
                                color = if (isLiquidGlass) Color.White.copy(0.3f) else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Icon(
                            Icons.Default.DocumentScanner,
                            contentDescription = AppStr.scanReceiptOptionTitle,
                            modifier = Modifier.size(20.dp),
                            tint = fgColor
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AppSurface(),
                        border = BorderStroke(1.dp, if (isLiquidGlass) Color.White.copy(0.2f) else AppText().copy(alpha = 0.12f)),
                        shadowElevation = 8.dp
                    ) {
                        Text(
                            AppStr.scanReceiptOptionTitle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppText(),
                            softWrap = false,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RailSlotItem(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    isNavMotionEnabled: Boolean,
    shape: androidx.compose.ui.graphics.Shape,
    tiltState: State<TiltState>,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDark = LocalIsDark.current

    val pressedAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = tween(durationMillis = 100),
        label = "railSlotPressed"
    )

    Box(
        modifier = modifier
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .clip(shape)
            .background(
                if (pressedAlpha > 0.01f && !isSelected) {
                    if (isDark) Color.White.copy(alpha = 0.14f * pressedAlpha)
                    else AppPrimary().copy(alpha = 0.20f * pressedAlpha)
                } else {
                    Color.Transparent
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        val contentScale by animateFloatAsState(
            targetValue = if (isPressed) 0.94f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "railSlotScale"
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = contentScale
                    scaleY = contentScale
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isSelected && isNavMotionEnabled) {
                    val tilt = tiltState.value
                    val tx = if (tilt.x.isNaN() || tilt.x.isInfinite()) 0f else tilt.x.coerceIn(-1f, 1f)
                    val ty = if (tilt.y.isNaN() || tilt.y.isInfinite()) 0f else tilt.y.coerceIn(-1f, 1f)

                    // Echo Layer 2 (Outer)
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AppPrimary().copy(alpha = 0.15f),
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                translationX = tx * 3.5f
                                translationY = ty * 3.5f
                            }
                    )

                    // Echo Layer 1 (Inner)
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AppPrimary().copy(alpha = 0.30f),
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                translationX = tx * 1.8f
                                translationY = ty * 1.8f
                            }
                    )
                }

                KumaExpressiveIcon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) AppText() else AppText().copy(alpha = 0.5f),
                    containerColor = Color.Transparent,
                    size = 24.dp,
                    iconPadding = 0.dp
                )
            }

            Spacer(Modifier.height(3.dp))

            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = if (isSelected) AppText() else AppText().copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

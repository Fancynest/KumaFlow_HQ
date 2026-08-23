package com.bearbones.kumaflow

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Custom Shape that looks like a physical wallet / Apple Wallet icon.
 * Rounded rectangle body with a curved notch (scoop) cut out of the top center.
 */
class WalletPouchShape(
    private val cornerRadius: Dp = 24.dp,
    private val notchWidth: Dp = 120.dp,
    private val notchDepth: Dp = 24.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cr = with(density) { cornerRadius.toPx() }
        val nw = with(density) { notchWidth.toPx() }
        val nd = with(density) { notchDepth.toPx() }

        val path = Path().apply {
            // Start from top-left corner (after corner radius)
            moveTo(0f, cr)
            // Top-left corner arc
            arcTo(
                rect = Rect(0f, 0f, cr * 2, cr * 2),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Line to the left edge of the notch
            val notchLeft = (size.width - nw) / 2f
            val notchRight = (size.width + nw) / 2f
            lineTo(notchLeft, 0f)

            // Curved notch (scoop) — cubic bezier going down and back up
            cubicTo(
                notchLeft + nw * 0.15f, 0f,
                notchLeft + nw * 0.15f, nd,
                size.width / 2f, nd
            )
            cubicTo(
                notchRight - nw * 0.15f, nd,
                notchRight - nw * 0.15f, 0f,
                notchRight, 0f
            )

            // Line to top-right corner
            lineTo(size.width - cr, 0f)
            // Top-right corner arc
            arcTo(
                rect = Rect(size.width - cr * 2, 0f, size.width, cr * 2),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Right side down
            lineTo(size.width, size.height - cr)
            // Bottom-right corner arc
            arcTo(
                rect = Rect(size.width - cr * 2, size.height - cr * 2, size.width, size.height),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Bottom side
            lineTo(cr, size.height)
            // Bottom-left corner arc
            arcTo(
                rect = Rect(0f, size.height - cr * 2, cr * 2, size.height),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun WalletCardStack(
    wallets: List<VirtualWallet>,
    balances: Map<String, Long>,
    currencySymbol: String,
    formatHide: (Long) -> String,
    onWalletClick: (String) -> Unit,
    onOrderChange: (List<VirtualWallet>) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val cardHeight = 120.dp
    val peekHeight = 36.dp // How much of each card peeks out above
    val walletPadding = 16.dp // Internal padding of the wallet pouch
    val walletShape = remember { WalletPouchShape(cornerRadius = 28.dp, notchWidth = 140.dp, notchDepth = 20.dp) }

    // Drag state
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var orderedWallets by remember(wallets) { mutableStateOf(wallets) }

    // Calculate wallet pouch height: top padding for notch + cards area + bottom padding
    val notchSpace = 28.dp
    val cardsAreaHeight = if (orderedWallets.isEmpty()) 80.dp
        else cardHeight + (peekHeight * (orderedWallets.size - 1).coerceAtLeast(0))
    val totalPouchHeight = notchSpace + cardsAreaHeight + walletPadding

    if (orderedWallets.isEmpty()) return

    // Wallet Pouch Container
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(totalPouchHeight)
            .padding(horizontal = 16.dp)
            .shadow(8.dp, walletShape, clip = false)
            .clip(walletShape)
            .background(AppSurface())
            .border(1.dp, AppText().copy(alpha = 0.08f), walletShape)
    ) {
        // Cards inside the pouch
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = walletPadding, end = walletPadding, top = notchSpace, bottom = walletPadding)
        ) {
            orderedWallets.forEachIndexed { index, wallet ->
                val isDragged = draggedIndex == index

                val peekPx = with(density) { peekHeight.toPx() }
                val baseOffset = index * peekPx
                val currentOffset = if (isDragged) baseOffset + dragOffsetY else baseOffset

                val animatedOffset = remember { Animatable(baseOffset) }
                LaunchedEffect(index, isDragged, baseOffset) {
                    if (!isDragged) {
                        animatedOffset.animateTo(
                            baseOffset,
                            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                        )
                    }
                }

                val finalOffset = if (isDragged) currentOffset else animatedOffset.value

                val zIdx = if (isDragged) 100f else index.toFloat()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cardHeight)
                        .zIndex(zIdx)
                        .offset(y = with(density) { finalOffset.toDp() })
                        .graphicsLayer {
                            if (isDragged) {
                                scaleX = 1.04f
                                scaleY = 1.04f
                                shadowElevation = 16f
                            }
                        }
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (wallet.backgroundType == "SOLID") {
                                try {
                                    Color(android.graphics.Color.parseColor(wallet.backgroundValue))
                                } catch (e: Exception) {
                                    Color(0xFF2A2A2A)
                                }
                            } else Color(0xFF2A2A2A)
                        )
                        .pointerInput(wallet.name) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggedIndex = index
                                    dragOffsetY = 0f
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetY += dragAmount.y

                                    // Calculate which slot the dragged card is over
                                    val rawCurrent = (baseOffset + dragOffsetY) / peekPx
                                    val targetIndex = rawCurrent.roundToInt().coerceIn(0, orderedWallets.size - 1)

                                    if (targetIndex != draggedIndex) {
                                        val newList = orderedWallets.toMutableList()
                                        val dragItem = newList.removeAt(draggedIndex)
                                        newList.add(targetIndex, dragItem)
                                        orderedWallets = newList

                                        // Adjust offset so card stays under finger
                                        dragOffsetY -= (targetIndex - draggedIndex) * peekPx
                                        draggedIndex = targetIndex
                                    }
                                },
                                onDragEnd = {
                                    draggedIndex = -1
                                    dragOffsetY = 0f
                                    onOrderChange(orderedWallets)
                                },
                                onDragCancel = {
                                    draggedIndex = -1
                                    dragOffsetY = 0f
                                }
                            )
                        }
                        .clickable { onWalletClick(wallet.name) }
                ) {
                    // Background image for TEMPLATE type
                    if (wallet.backgroundType == "TEMPLATE") {
                        val resId = context.resources.getIdentifier(
                            wallet.backgroundValue, "drawable", context.packageName
                        )
                        if (resId != 0) {
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Gradient scrim for text readability
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Black.copy(alpha = 0.3f),
                                                Color.Black.copy(alpha = 0.7f)
                                            )
                                        )
                                    )
                            )
                        }
                    }

                    // Card content
                    val amt = balances[wallet.name] ?: 0L
                    val prefix = if (amt < 0) "- " else ""

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                wallet.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                "KumaFlow",
                                color = Color.White.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            "$prefix$currencySymbol ${formatHide(abs(amt))}",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

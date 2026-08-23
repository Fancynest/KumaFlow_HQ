package com.bearbones.kumaflow

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
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
 * The wallet "front flap" shape — a panel with a curved scoop/notch at the TOP center.
 * This sits at the bottom of the wallet, covering the lower portion of cards.
 * The scoop allows cards to peek out visually.
 */
class WalletFlapShape(
    private val scoopWidth: Dp = 120.dp,
    private val scoopDepth: Dp = 32.dp,
    private val cornerRadius: Dp = 24.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val sw = with(density) { scoopWidth.toPx() }
        val sd = with(density) { scoopDepth.toPx() }
        val cr = with(density) { cornerRadius.toPx() }

        val scoopLeft = (size.width - sw) / 2f
        val scoopRight = (size.width + sw) / 2f

        val path = Path().apply {
            // Start at top-left
            moveTo(0f, 0f)

            // Left side — line up to where scoop starts
            lineTo(scoopLeft, 0f)

            // Scoop curve (concave going DOWN into the flap)
            cubicTo(
                scoopLeft + sw * 0.2f, 0f,      // control point 1
                scoopLeft + sw * 0.2f, sd,       // control point 2 (down)
                size.width / 2f, sd               // apex of scoop (down)
            )
            cubicTo(
                scoopRight - sw * 0.2f, sd,      // control point 3 (down)
                scoopRight - sw * 0.2f, 0f,       // control point 4
                scoopRight, 0f                     // end of scoop (back at 0)
            )

            // Continue to top-right
            lineTo(size.width, 0f)

            // Right side down
            lineTo(size.width, size.height - cr)

            // Bottom-right corner
            arcTo(
                rect = Rect(size.width - cr * 2, size.height - cr * 2, size.width, size.height),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Bottom side
            lineTo(cr, size.height)

            // Bottom-left corner
            arcTo(
                rect = Rect(0f, size.height - cr * 2, cr * 2, size.height),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Left side back up
            lineTo(0f, 0f)

            close()
        }
        return Outline.Generic(path)
    }
}

class TopUnboundedShape(private val cornerRadius: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cr = with(density) { cornerRadius.toPx() }
        val path = Path().apply {
            moveTo(-10000f, -10000f) 
            lineTo(size.width + 10000f, -10000f) 
            lineTo(size.width + 10000f, size.height - cr)
            arcTo(
                rect = Rect(size.width - cr * 2, size.height - cr * 2, size.width, size.height),
                startAngleDegrees = 0f, sweepAngleDegrees = 90f, forceMoveTo = false
            )
            lineTo(cr, size.height)
            arcTo(
                rect = Rect(0f, size.height - cr * 2, cr * 2, size.height),
                startAngleDegrees = 90f, sweepAngleDegrees = 90f, forceMoveTo = false
            )
            lineTo(-10000f, size.height - cr)
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

    // Dimensions
    val cardHeight = 190.dp
    val cardPeek = 72.dp          // Increased from 48.dp to give more touch area for easier selection/reordering
    val walletCorner = 28.dp
    val flapHeight = 100.dp       // The wallet front flap height (covers bottom of cards)
    val walletSidePadding = 12.dp // Padding inside wallet for cards
    val scoopDepth = 32.dp

    // Drag and Pop state
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var orderedWallets by remember(wallets) { mutableStateOf(wallets) }

    var poppedCard by remember { mutableStateOf<String?>(null) }
    var popState by remember { mutableIntStateOf(0) }
    var isReconcileHold by remember { mutableStateOf(false) }


    // Cards area: first card full + subsequent cards peek
    val cardsVisibleHeight = cardHeight + (cardPeek * (orderedWallets.size - 1).coerceAtLeast(0))
    // The wallet body extends: cards area visible at top + flap covering bottom
    // The flap overlaps the bottom part of the last card
    val flapOverlap = 40.dp
    val totalWalletHeight = cardsVisibleHeight + flapHeight - flapOverlap

    val isDark = com.bearbones.kumaflow.LocalIsDark.current
    val walletColor = if (isDark) Color(0xFF3D3D3D) else Color(0xFFDCDCDC)
    val walletBorderColor = AppText().copy(alpha = 0.08f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            // Removed top padding to eliminate the gap above the wallet
    ) {
        // === WALLET BODY (the beige/gray outer container) ===
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalWalletHeight)
                .shadow(6.dp, RoundedCornerShape(walletCorner), clip = false)
                .background(walletColor, RoundedCornerShape(walletCorner))
        ) {
            // === CARDS CONTAINER (clipped at bottom, open at top) ===
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(TopUnboundedShape(walletCorner))
            ) {
                // Cards stacked inside
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = walletSidePadding)
                        .padding(top = 10.dp) // small gap from wallet top edge
                ) {
                    if (orderedWallets.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(cardHeight)
                                .drawBehind {
                                    drawRoundRect(
                                        color = walletBorderColor.copy(alpha = 0.5f),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                                            width = 6f,
                                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
                                        ),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(40f, 40f)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Empty Wallet",
                                color = AppText().copy(alpha = 0.4f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        orderedWallets.forEachIndexed { index, wallet ->
                            key(wallet.name) {
                            val isDragged = draggedIndex == index
                            val isPopped = poppedCard == wallet.name
                            
                            val peekPx = with(density) { cardPeek.toPx() }
                            val baseOffset = index * peekPx

                        val targetOffset = when {
                            isPopped && popState == 1 -> baseOffset - with(density) { 60.dp.toPx() }
                            isPopped && popState == 2 -> baseOffset - with(density) { 150.dp.toPx() }
                            else -> baseOffset
                        }

                        val currentOffset = if (isDragged) baseOffset + dragOffsetY else targetOffset

                        val animatedOffset = remember { Animatable(baseOffset) }
                        
                        LaunchedEffect(isDragged, currentOffset) {
                            if (isDragged) {
                                animatedOffset.snapTo(currentOffset)
                            }
                        }

                        LaunchedEffect(index, isDragged, targetOffset) {
                            if (!isDragged) {
                                animatedOffset.animateTo(
                                    targetOffset,
                                    spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        }

                        val finalOffset = if (isDragged) currentOffset else animatedOffset.value
                        val zIdx = if (isDragged || isPopped) 100f + index else index.toFloat()

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(cardHeight)
                                .zIndex(zIdx)
                                .offset(y = with(density) { finalOffset.toDp() })
                                .graphicsLayer {
                                    if (isDragged) {
                                        scaleX = 1.03f
                                        scaleY = 1.03f
                                        shadowElevation = 20f
                                    } else if (isPopped) {
                                        shadowElevation = 30f
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
                                .pointerInput(wallet.name, popState, poppedCard) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            if (poppedCard == wallet.name && popState == 2) {
                                                isReconcileHold = true
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onWalletClick(wallet.name)
                                            } else {
                                                isReconcileHold = false
                                                val startIndex = orderedWallets.indexOf(wallet)
                                                if (startIndex != -1) {
                                                    draggedIndex = startIndex
                                                }
                                                dragOffsetY = 0f
                                                poppedCard = null
                                                popState = 0
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            if (isReconcileHold) return@detectDragGesturesAfterLongPress
                                            
                                            change.consume()
                                            dragOffsetY += dragAmount.y

                                            val currentIndex = orderedWallets.indexOf(wallet)
                                            if (currentIndex == -1) return@detectDragGesturesAfterLongPress
                                            
                                            val freshBaseOffset = currentIndex * peekPx

                                            val rawCurrent = (freshBaseOffset + dragOffsetY) / peekPx
                                            val targetIndex = rawCurrent
                                                .roundToInt()
                                                .coerceIn(0, orderedWallets.size - 1)

                                            if (targetIndex != currentIndex) {
                                                val newList = orderedWallets.toMutableList()
                                                val dragItem = newList.removeAt(currentIndex)
                                                newList.add(targetIndex, dragItem)
                                                orderedWallets = newList
                                                
                                                dragOffsetY -= (targetIndex - currentIndex) * peekPx
                                                draggedIndex = targetIndex
                                            }
                                        },
                                        onDragEnd = {
                                            draggedIndex = -1
                                            dragOffsetY = 0f
                                            if (!isReconcileHold) onOrderChange(orderedWallets)
                                            isReconcileHold = false
                                        },
                                        onDragCancel = {
                                            draggedIndex = -1
                                            dragOffsetY = 0f
                                            isReconcileHold = false
                                        }
                                    )
                                }
                                .clickable {
                                    if (poppedCard == wallet.name) {
                                        if (popState == 1) {
                                            popState = 2
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                        else if (popState == 2) {
                                            poppedCard = null
                                            popState = 0
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    } else {
                                        poppedCard = wallet.name
                                        popState = 1
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
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
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color.Black.copy(alpha = 0.2f),
                                                    Color.Black.copy(alpha = 0.6f)
                                                )
                                            )
                                        )
                                )
                            }
                        }

                        // Card text content
                        val amt = balances[wallet.name] ?: 0L
                        val prefix = if (amt < 0) "- " else ""

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 18.dp, vertical = 12.dp),
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
                                    fontSize = 15.sp
                                )
                                Text(
                                    "KumaFlow",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.sp
                                )
                            }
                            Text(
                                "$prefix$currencySymbol ${formatHide(abs(amt))}",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1
                            )
                        }
                        } // end Box(card)
                    } // end key
                        } // end forEachIndexed
                    } // end if-else
                } // end Box(cards padding box)
            } // end Box(cards container)

            // === WALLET FRONT FLAP (with scoop) — sits at the bottom, on top of cards ===
            val flapShape = remember { WalletFlapShape(scoopWidth = 100.dp, scoopDepth = scoopDepth, cornerRadius = walletCorner) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(flapHeight)
                    .align(Alignment.BottomCenter)
                    .zIndex(200f)  // Always on top of cards
                    .clip(flapShape)
                    .background(walletColor)
            )
        }
    }
}

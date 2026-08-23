package com.bearbones.kumaflow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import com.bearbones.kumaflow.ui.components.rememberTiltState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
    userName: String,
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
    val tiltState = rememberTiltState()
    var lastLongPressTime by remember { mutableStateOf(0L) }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    // Dimensions
    val walletSidePadding = 12.dp // Padding inside wallet for cards
    // The parent Column in HomeScreen has 24.dp horizontal padding.
    // The Wallet Body will have 0 extra horizontal padding to match Total Balance.
    val cardWidth = screenWidth - 48.dp - (walletSidePadding * 2)
    val cardHeight = cardWidth / 1.6f // Maintain nice aspect ratio
    val cardPeek = 48.dp
    val walletCorner = 28.dp
    val flapHeight = 100.dp       // The wallet front flap height (covers bottom of cards)
    val scoopDepth = 32.dp

    // Drag and Pop state
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var orderedWallets by remember(wallets) { mutableStateOf(wallets) }

    var poppedCard by remember { mutableStateOf<String?>(null) }
    var popState by remember { mutableIntStateOf(0) }
    var isReconcileHold by remember { mutableStateOf(false) }


    // Cards area: first card full + subsequent cards peek
    // When empty, fake 3 cards but with a smaller peek distance so the slots aren't too far apart
    val emptySlotCount = 3
    val emptyCardPeek = 48.dp
    val effectiveCardCount = if (orderedWallets.isEmpty()) emptySlotCount else orderedWallets.size
    val effectiveCardPeek = if (orderedWallets.isEmpty()) emptyCardPeek else cardPeek
    val cardsVisibleHeight = cardHeight + (effectiveCardPeek * (effectiveCardCount - 1).coerceAtLeast(0))
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
            // Removed 16.dp padding so it exactly matches the width of the Total Balance card
    ) {
        val stitchColor = AppText().copy(alpha = 0.25f)
        // === WALLET BODY (the beige/gray outer container) ===
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalWalletHeight)
                .shadow(6.dp, RoundedCornerShape(walletCorner), clip = false)
                .background(walletColor, RoundedCornerShape(walletCorner))
                .drawWithContent {
                    drawContent()
                    if (orderedWallets.isEmpty()) {
                        // Stitching around the entire wallet body
                        val inset = 8.dp.toPx()
                        val cr = walletCorner.toPx()
                        drawRoundRect(
                            color = stitchColor,
                            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                            size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cr - inset, cr - inset),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 3f,
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                    floatArrayOf(12f, 8f), 0f
                                )
                            )
                        )
                    }
                }
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
                        // Draw horizontal slot lines at the same positions where card peeks would be
                        val slotLineColor = AppText().copy(alpha = 0.35f) // Made darker
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(cardHeight + emptyCardPeek * (emptySlotCount - 1))
                                .drawBehind {
                                    val peekPx = emptyCardPeek.toPx()
                                    // Gap from edges so it doesn't touch the stitching
                                    val linePadding = 16.dp.toPx()
                                    // Draw lines at each "card peek" boundary
                                    for (i in 1 until emptySlotCount) {
                                        val y = i * peekPx
                                        drawLine(
                                            color = slotLineColor,
                                            start = androidx.compose.ui.geometry.Offset(linePadding, y),
                                            end = androidx.compose.ui.geometry.Offset(size.width - linePadding, y),
                                            strokeWidth = 6f, // Made thicker
                                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                                        )
                                    }
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

                            val cardHeightPx = with(density) { cardHeight.toPx() }
                            val targetOffset = when {
                                isPopped && popState == 1 -> baseOffset - (cardHeightPx * 0.15f)
                                isPopped && popState == 2 -> baseOffset - (cardHeightPx * 0.85f)
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
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
                            }

                            val finalOffset = if (isDragged) currentOffset else animatedOffset.value
                            val zIdx = if (isDragged || (isPopped && popState == 2)) 100f + index else index.toFloat()

                            val animatedScale by animateFloatAsState(
                                targetValue = if (isDragged) 1.05f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ), label = "cardScale"
                            )
                            val animatedElevation by animateFloatAsState(
                                targetValue = if (isDragged) 24f else if (isPopped && popState == 2) 30f else if (isPopped && popState == 1) 8f else 4f,
                                animationSpec = tween(300), label = "cardElevation"
                            )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(cardHeight)
                                .zIndex(zIdx)
                                .offset(y = with(density) { finalOffset.toDp() })
                                .graphicsLayer {
                                    scaleX = animatedScale
                                    scaleY = animatedScale
                                    shadowElevation = animatedElevation
                                    shape = RoundedCornerShape(24.dp)
                                    clip = true
                                    // Parallax 3D tilt effect from gyroscope
                                    rotationY = tiltState.value.x * 8f  // subtle left/right tilt
                                    rotationX = -tiltState.value.y * 4f // subtle forward/back tilt
                                    cameraDistance = 12f * density.density
                                }
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
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
                                            } else if (poppedCard == wallet.name && popState == 1) {
                                                isReconcileHold = true
                                                popState = 2
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                                            val diff = rawCurrent - currentIndex
                                            val targetIndex = if (diff > 0.6f) {
                                                currentIndex + 1
                                            } else if (diff < -0.6f) {
                                                currentIndex - 1
                                            } else {
                                                currentIndex
                                            }.coerceIn(0, orderedWallets.size - 1)

                                            if (targetIndex != currentIndex) {
                                                val newList = orderedWallets.toMutableList()
                                                val dragItem = newList.removeAt(currentIndex)
                                                newList.add(targetIndex, dragItem)
                                                orderedWallets = newList
                                                
                                                dragOffsetY -= (targetIndex - currentIndex) * peekPx
                                                draggedIndex = targetIndex
                                                
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        },
                                        onDragEnd = {
                                            lastLongPressTime = System.currentTimeMillis()
                                            draggedIndex = -1
                                            dragOffsetY = 0f
                                            if (!isReconcileHold) onOrderChange(orderedWallets)
                                            isReconcileHold = false
                                        },
                                        onDragCancel = {
                                            lastLongPressTime = System.currentTimeMillis()
                                            draggedIndex = -1
                                            dragOffsetY = 0f
                                            isReconcileHold = false
                                        }
                                    )
                                }
                                .clickable {
                                    // Prevent tap if a long press drag/hold just finished (within 300ms)
                                    if (System.currentTimeMillis() - lastLongPressTime < 300) return@clickable
                                    
                                    if (poppedCard == wallet.name) {
                                        // If already popped (quarter or full), tapping unpops it
                                        poppedCard = null
                                        popState = 0
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    } else {
                                        // Tapping unpopped card pops it to quarter (1)
                                        poppedCard = wallet.name
                                        popState = 1
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                    ) {
                        if (wallet.backgroundType == "GRADIENT") {
                            val parts = wallet.backgroundValue.split(",")
                            if (parts.size >= 2) {
                                var startColor = Color.Gray
                                var endColor = Color.DarkGray
                                var valid = false
                                try {
                                    startColor = Color(android.graphics.Color.parseColor(parts[0].trim()))
                                    endColor = Color(android.graphics.Color.parseColor(parts[1].trim()))
                                    valid = true
                                } catch (e: Exception) {}
                                
                                if (valid) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(startColor, endColor)
                                                )
                                            )
                                    )
                                }
                            }
                        } else if (wallet.backgroundType == "TEMPLATE") {
                            val isPrideReq = wallet.backgroundValue == "pride"
                            val isBearReq = wallet.backgroundValue == "bear" || wallet.backgroundValue == "bear2"
                            val isPrideAllowed = userName.contains("#pride", ignoreCase = true)
                            val isBearAllowed = userName.contains("#bear", ignoreCase = true)
                            
                            val shouldRender = when {
                                isPrideReq -> isPrideAllowed
                                isBearReq -> isBearAllowed
                                else -> true
                            }
                            
                            if (shouldRender) {
                                val resId = context.resources.getIdentifier(wallet.backgroundValue, "drawable", context.packageName)
                                if (resId != 0) {
                                    Image(
                                        painter = painterResource(id = resId),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        } else if (wallet.backgroundType == "CUSTOM") {
                            val file = java.io.File(java.io.File(context.filesDir, "custom_cards"), wallet.backgroundValue)
                            val bitmap = remember(wallet.backgroundValue) { android.graphics.BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() }
                            if (bitmap != null) {
                                Image(bitmap = bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            }
                        }

                        // === Holographic/iridescent overlay — shifts with gyroscope tilt ===
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = 0.12f } // very subtle
                                .drawBehind {
                                    val tilt = tiltState.value
                                    // Shift the gradient start/end based on tilt
                                    val holoOffsetX = tilt.x * 0.5f + 0.5f // 0..1
                                    val holoOffsetY = tilt.y * 0.5f + 0.5f // 0..1
                                    
                                    val brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFFF6EC7), // pink
                                            Color(0xFF7DF9FF), // cyan
                                            Color(0xFFB19CD9), // purple
                                            Color(0xFF77DD77), // green
                                            Color(0xFFFFD700), // gold
                                            Color(0xFFFF6EC7)  // pink again for loop
                                        ),
                                        start = androidx.compose.ui.geometry.Offset(
                                            holoOffsetX * size.width, holoOffsetY * size.height
                                        ),
                                        end = androidx.compose.ui.geometry.Offset(
                                            (1f - holoOffsetX) * size.width, (1f - holoOffsetY) * size.height
                                        )
                                    )
                                    drawRect(brush = brush, size = size)
                                }
                        )

                        // Scrim overlays for text legibility
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .align(Alignment.TopCenter)
                                .background(Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent)
                                ))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .align(Alignment.BottomCenter)
                                .background(Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))
                                ))
                        )

                        // Card text content
                        val amt = balances[wallet.name] ?: 0L
                        val prefix = if (amt < 0) "- " else ""

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .background(Color.White, CircleShape)
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        androidx.compose.material3.Icon(
                                            imageVector = Icons.Filled.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = Color(0xFF2A2A2A) // Dark gray
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        wallet.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                                Text(
                                    "KumaFlow",
                                    color = Color.White.copy(alpha = 0.5f),
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
            val stitchColor = AppText().copy(alpha = 0.25f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(flapHeight)
                    .align(Alignment.BottomCenter)
                    .zIndex(200f)  // Always on top of cards
                    .clip(flapShape)
                    .background(walletColor)
                    .drawWithContent {
                        drawContent()
                        // Dashed stitching line along the flap edge (inset a few px)
                        val inset = 8.dp.toPx()
                        val sw = 100.dp.toPx()
                        val sd = scoopDepth.toPx()
                        val cr = walletCorner.toPx()
                        val scoopLeft = (size.width - sw) / 2f
                        val scoopRight = (size.width + sw) / 2f

                        val stitchPath = androidx.compose.ui.graphics.Path().apply {
                            // Start top-left (inset)
                            moveTo(inset, inset)
                            // Top edge to scoop start
                            lineTo(scoopLeft, inset)
                            // Scoop curve (shifted down by inset)
                            cubicTo(
                                scoopLeft + sw * 0.2f, inset,
                                scoopLeft + sw * 0.2f, sd + inset,
                                size.width / 2f, sd + inset
                            )
                            cubicTo(
                                scoopRight - sw * 0.2f, sd + inset,
                                scoopRight - sw * 0.2f, inset,
                                scoopRight, inset
                            )
                            // Top edge to top-right
                            lineTo(size.width - inset, inset)
                            // Right side down
                            lineTo(size.width - inset, size.height - cr)
                            // Bottom-right corner
                            arcTo(
                                rect = androidx.compose.ui.geometry.Rect(
                                    size.width - cr * 2 + inset, size.height - cr * 2 + inset,
                                    size.width - inset, size.height - inset
                                ),
                                startAngleDegrees = 0f,
                                sweepAngleDegrees = 90f,
                                forceMoveTo = false
                            )
                            // Bottom edge
                            lineTo(cr, size.height - inset)
                            // Bottom-left corner
                            arcTo(
                                rect = androidx.compose.ui.geometry.Rect(
                                    inset, size.height - cr * 2 + inset,
                                    cr * 2 - inset, size.height - inset
                                ),
                                startAngleDegrees = 90f,
                                sweepAngleDegrees = 90f,
                                forceMoveTo = false
                            )
                            // Left side up
                            lineTo(inset, inset)
                        }
                        drawPath(
                            path = stitchPath,
                            color = stitchColor,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 3f,
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                    floatArrayOf(12f, 8f), 0f
                                )
                            )
                        )
                    }
            )
        }
    }
}

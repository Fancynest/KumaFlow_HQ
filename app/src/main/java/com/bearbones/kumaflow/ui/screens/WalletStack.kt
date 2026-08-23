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
            moveTo(0f, sd)

            // Left side — line up to where scoop starts, then curve into scoop
            lineTo(scoopLeft, sd)

            // Scoop curve (concave going UP from the flap top edge)
            cubicTo(
                scoopLeft + sw * 0.2f, sd,      // control point 1
                scoopLeft + sw * 0.2f, 0f,       // control point 2
                size.width / 2f, 0f               // apex of scoop
            )
            cubicTo(
                scoopRight - sw * 0.2f, 0f,      // control point 3
                scoopRight - sw * 0.2f, sd,       // control point 4
                scoopRight, sd                     // end of scoop
            )

            // Continue to top-right
            lineTo(size.width, sd)

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
            lineTo(0f, sd)

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
    val cardHeight = 110.dp
    val cardPeek = 28.dp          // How much each card peeks above the one below
    val walletCorner = 28.dp
    val flapHeight = 80.dp        // The wallet front flap height (covers bottom of cards)
    val walletSidePadding = 12.dp // Padding inside wallet for cards
    val scoopDepth = 32.dp

    // Drag state
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var orderedWallets by remember(wallets) { mutableStateOf(wallets) }

    if (orderedWallets.isEmpty()) return

    // Cards area: first card full + subsequent cards peek
    val cardsVisibleHeight = cardHeight + (cardPeek * (orderedWallets.size - 1).coerceAtLeast(0))
    // The wallet body extends: cards area visible at top + flap covering bottom
    // The flap overlaps the bottom part of the last card
    val flapOverlap = 20.dp
    val totalWalletHeight = cardsVisibleHeight + flapHeight - flapOverlap

    // Wallet body colors
    val walletColor = AppSurface()
    val walletBorderColor = AppText().copy(alpha = 0.08f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // === WALLET BODY (the beige/gray outer container) ===
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalWalletHeight)
                .shadow(6.dp, RoundedCornerShape(walletCorner))
                .clip(RoundedCornerShape(walletCorner))
                .background(walletColor)
        ) {
            // === CARDS stacked inside, aligned to top ===
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = walletSidePadding)
                    .padding(top = 10.dp) // small gap from wallet top edge
            ) {
                orderedWallets.forEachIndexed { index, wallet ->
                    val isDragged = draggedIndex == index
                    val peekPx = with(density) { cardPeek.toPx() }
                    val baseOffset = index * peekPx

                    val currentOffset = if (isDragged) baseOffset + dragOffsetY else baseOffset

                    val animatedOffset = remember { Animatable(baseOffset) }
                    LaunchedEffect(index, isDragged, baseOffset) {
                        if (!isDragged) {
                            animatedOffset.animateTo(
                                baseOffset,
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
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
                                    scaleX = 1.03f
                                    scaleY = 1.03f
                                    shadowElevation = 20f
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

                                        val rawCurrent = (baseOffset + dragOffsetY) / peekPx
                                        val targetIndex = rawCurrent
                                            .roundToInt()
                                            .coerceIn(0, orderedWallets.size - 1)

                                        if (targetIndex != draggedIndex) {
                                            val newList = orderedWallets.toMutableList()
                                            val dragItem = newList.removeAt(draggedIndex)
                                            newList.add(targetIndex, dragItem)
                                            orderedWallets = newList
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
                    }
                }
            }

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

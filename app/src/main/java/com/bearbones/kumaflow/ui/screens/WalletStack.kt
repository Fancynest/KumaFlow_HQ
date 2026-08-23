package com.bearbones.kumaflow

import androidx.compose.animation.core.animateIntOffsetAsState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.bearbones.kumaflow.VirtualWallet
import kotlin.math.abs

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
    val itemHeight = 200.dp
    val overlapHeight = 60.dp
    
    var mutableWallets by remember(wallets) { mutableStateOf(wallets) }
    
    var expandedWallet by remember { mutableStateOf<String?>(null) }
    var draggedWallet by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (mutableWallets.isEmpty()) 0.dp else itemHeight + (overlapHeight * (mutableWallets.size - 1)))
            .padding(horizontal = 24.dp)
    ) {
        mutableWallets.forEachIndexed { index, wallet ->
            val isExpanded = expandedWallet == wallet.name
            val isDragged = draggedWallet == wallet.name
            
            val targetY = if (isExpanded) 0 else if (expandedWallet != null) (itemHeight.value + 20).toInt() else (index * overlapHeight.value).toInt()
            val animatedY by animateIntOffsetAsState(
                targetValue = IntOffset(0, if (isDragged) (index * overlapHeight.value).toInt() + dragOffsetY.toInt() else targetY),
                label = "offsetY"
            )
            
            val zIndex = if (isDragged) 100f else if (isExpanded) 50f else index.toFloat()
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .zIndex(zIndex)
                    .offset { animatedY.copy(x = 0) }
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (wallet.backgroundType == "SOLID") {
                            try {
                                Color(android.graphics.Color.parseColor(wallet.backgroundValue))
                            } catch (e: Exception) { Color.DarkGray }
                        } else Color.Gray
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .pointerInput(wallet.name) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { draggedWallet = wallet.name },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetY += dragAmount.y
                                
                                val currentY = (index * overlapHeight.toPx()) + dragOffsetY
                                var newIndex = (currentY / overlapHeight.toPx()).toInt()
                                newIndex = newIndex.coerceIn(0, mutableWallets.size - 1)
                                
                                if (newIndex != index) {
                                    val newList = mutableWallets.toMutableList()
                                    val item = newList.removeAt(index)
                                    newList.add(newIndex, item)
                                    mutableWallets = newList
                                    dragOffsetY -= (newIndex - index) * overlapHeight.toPx()
                                }
                            },
                            onDragEnd = {
                                draggedWallet = null
                                dragOffsetY = 0f
                                onOrderChange(mutableWallets)
                            },
                            onDragCancel = {
                                draggedWallet = null
                                dragOffsetY = 0f
                            }
                        )
                    }
                    .clickable {
                        if (expandedWallet == wallet.name) {
                            onWalletClick(wallet.name)
                        } else {
                            expandedWallet = if (expandedWallet == null) wallet.name else null
                        }
                    }
            ) {
                if (wallet.backgroundType == "TEMPLATE") {
                    val resId = context.resources.getIdentifier(wallet.backgroundValue, "drawable", context.packageName)
                    if (resId != 0) {
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))))
                    }
                }
                
                val amt = balances[wallet.name] ?: 0L
                val wBalPref = if (amt < 0) "- " else ""
                
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("KumaFlow", color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                        Text(wallet.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Text("$wBalPref$currencySymbol ${formatHide(abs(amt))}", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

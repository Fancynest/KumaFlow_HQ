package com.bearbones.kumaflow.ui.screens
import com.bearbones.kumaflow.utils.kumaClickable

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import android.graphics.Paint
import android.graphics.Typeface
import com.bearbones.kumaflow.AppPrimary
import com.bearbones.kumaflow.AppStr
import com.bearbones.kumaflow.AppSurface
import com.bearbones.kumaflow.AppText
import com.bearbones.kumaflow.KumaTransaction
import com.bearbones.kumaflow.TransactionSplit
import com.bearbones.kumaflow.glassCard
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import com.bearbones.kumaflow.ui.components.KumaOutlinedButton
import com.bearbones.kumaflow.ui.components.KumaTextButton
import com.bearbones.kumaflow.utils.bouncySheetContent

data class RouletteOption(
    val name: String,
    val amount: Long,
    val category: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouletteBottomSheet(
    walletBalances: Map<String, Long>,
    physicalWallets: List<String>,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSaveTransaction: (List<Pair<KumaTransaction, List<TransactionSplit>>>) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var selectedWallet by remember { mutableStateOf<String?>(physicalWallets.firstOrNull()) }
    val maxAvailableBalance = selectedWallet?.let { walletBalances[it] ?: 0L } ?: 0L
    var maxBudget by remember { mutableFloatStateOf(maxAvailableBalance.toFloat().coerceAtLeast(0f)) }
    
    var manualOptionName by remember { mutableStateOf("") }
    var manualOptionAmount by remember { mutableStateOf("") }
    var manualOptionCategory by remember { mutableStateOf(categories.firstOrNull() ?: "Others") }

    var options by remember { mutableStateOf<List<RouletteOption>>(emptyList()) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var isSpinning by remember { mutableStateOf(false) }
    var winningOption by remember { mutableStateOf<RouletteOption?>(null) }
    var showWinnerDialog by remember { mutableStateOf(false) }

    // Dynamic budget slider max value when wallet changes
    LaunchedEffect(selectedWallet) {
        maxBudget = maxAvailableBalance.toFloat().coerceAtLeast(0f)
    }

    val colors = listOf(
        Color(0xFFE57373), Color(0xFF81C784), Color(0xFF64B5F6), Color(0xFFFFD54F),
        Color(0xFFBA68C8), Color(0xFFFF8A65), Color(0xFF4DB6AC), Color(0xFFAED581)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = AppSurface(),
        dragHandle = { BottomSheetDefaults.DragHandle(color = AppPrimary()) }
    ) {
        Column(
            modifier = Modifier
                .bouncySheetContent()
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = AppStr.rouletteHeader,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AppText(),
                modifier = Modifier.padding(bottom = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (physicalWallets.isEmpty()) {
                Text(AppStr.rouletteEmptyWallet, color = AppText())
                Spacer(modifier = Modifier.height(24.dp))
                return@Column
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(physicalWallets) { wName ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selectedWallet == wName) AppPrimary() else Color.Transparent)
                            .kumaClickable { selectedWallet = wName }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = wName,
                            color = if (selectedWallet == wName) Color.White else AppText(),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            selectedWallet?.let { _ ->
                if (maxAvailableBalance <= 0L) {
                    Text(AppStr.rouletteEmptyWallet, color = AppText(), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    Text("${AppStr.rouletteBudgetLabel}: Rp ${maxBudget.toLong()}", color = AppText(), fontWeight = FontWeight.Bold)
                    Slider(
                        value = maxBudget,
                        onValueChange = { maxBudget = it },
                        valueRange = 0f..maxAvailableBalance.toFloat(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Manual option adder
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    com.bearbones.kumaflow.ui.components.KumaOutlinedTextField(
                        value = manualOptionName,
                        onValueChange = { manualOptionName = it },
                        placeholder = { Text(AppStr.rouletteOptionNameHint) },
                        modifier = Modifier.weight(1.5f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    com.bearbones.kumaflow.ui.components.KumaOutlinedTextField(
                        value = manualOptionAmount,
                        onValueChange = { input -> 
                            val digits = input.filter { it.isDigit() }
                            manualOptionAmount = digits
                        },
                        placeholder = { Text(AppStr.rouletteNominalHint) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = ThousandSeparatorVisualTransformation()
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    var expCat by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expCat,
                        onExpandedChange = { expCat = !expCat },
                        modifier = Modifier.weight(1f)
                    ) {
                        com.bearbones.kumaflow.ui.components.KumaOutlinedTextField(
                            value = manualOptionCategory,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expCat) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = expCat,
                            onDismissRequest = { expCat = false }
                        ) {
                            categories.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c) },
                                    onClick = { manualOptionCategory = c; expCat = false }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    com.bearbones.kumaflow.ui.components.KumaButton(
                        onClick = {
                            if (manualOptionName.isNotBlank() && manualOptionAmount.isNotBlank() && (manualOptionAmount.toLongOrNull() ?: 0L) > 0) {
                                options = (options + RouletteOption(manualOptionName, manualOptionAmount.toLong(), manualOptionCategory)).takeLast(8)
                                manualOptionName = ""
                                manualOptionAmount = ""
                            }
                        },
                        modifier = Modifier.height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppPrimary())
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(AppStr.rouletteAddBtn, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            var spinDurationSeconds by remember { mutableFloatStateOf(10f) }
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if(AppStr.isId) "Durasi: ${spinDurationSeconds.toInt()} dtk" else "Duration: ${spinDurationSeconds.toInt()} sec",
                    color = AppText(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Slider(
                    value = spinDurationSeconds,
                    onValueChange = { spinDurationSeconds = it },
                    valueRange = 10f..30f,
                    steps = 19,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val textPaint = remember {
                Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 38f
                    textAlign = Paint.Align.RIGHT
                    isAntiAlias = true
                    typeface = Typeface.DEFAULT_BOLD
                }
            }

            // Wheel Canvas
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .clip(CircleShape)
                    .kumaClickable(enabled = !isSpinning && options.isNotEmpty()) {
                        isSpinning = true
                        scope.launch {
                            val duration = (spinDurationSeconds * 1000).toInt()
                            val secureRandom = SecureRandom()
                            val baseSpins = (duration / 1000f) * 2.5f // 2.5 revs per sec avg
                            val randomSpins = secureRandom.nextInt(5) + baseSpins.toInt()
                            val randomAngle = secureRandom.nextInt(360).toFloat()
                            val targetRotation = rotation + (randomSpins * 360f) + randomAngle

                            val anim = Animatable(rotation)
                            var lastTick = rotation
                            
                            anim.animateTo(
                                targetValue = targetRotation,
                                animationSpec = tween(
                                    durationMillis = duration, 
                                    easing = RouletteEasing(duration.toFloat())
                                )
                            ) {
                                rotation = this.value
                                // Haptic tick when a slice passes (slice angle = 360/options.size)
                                val sliceAngle = 360f / options.size
                                if ((this.value - lastTick) >= sliceAngle / 2) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    lastTick = this.value
                                }
                            }
                            
                            rotation = targetRotation % 360f
                            val normalizedRotation = (360f - rotation) % 360f
                            val sliceAngle = 360f / options.size
                            val winningIndex = (normalizedRotation / sliceAngle).toInt() % options.size
                            
                            winningOption = options[winningIndex]
                            showWinnerDialog = true
                            isSpinning = false
                        }
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (options.isEmpty()) {
                        drawCircle(color = Color.LightGray)
                        return@Canvas
                    }
                    val sliceAngle = 360f / options.size
                    val radius = size.minDimension / 2
                    
                    rotate(rotation) {
                        for (i in options.indices) {
                            drawArc(
                                color = colors[i % colors.size],
                                startAngle = i * sliceAngle - 90f, // Start from top
                                sweepAngle = sliceAngle,
                                useCenter = true,
                                size = Size(size.width, size.height)
                            )
                            
                            drawContext.canvas.nativeCanvas.apply {
                                save()
                                translate(center.x, center.y)
                                rotate(i * sliceAngle + sliceAngle / 2f - 90f)
                                val text = options[i].name
                                val maxChars = 14
                                val shortText = if (text.length > maxChars) text.take(maxChars) + ".." else text
                                drawText(shortText, radius - 20f, 12f, textPaint)
                                restore()
                            }
                        }
                    }
                }
                
                // Center Kuma Avatar
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(AppSurface())
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Pets, contentDescription = null, tint = AppPrimary(), modifier = Modifier.size(30.dp))
                }
                
                // The winning tick mark
                Icon(
                    Icons.Default.Pets, 
                    contentDescription = null, 
                    tint = Color.Red, 
                    modifier = Modifier.align(Alignment.TopCenter).offset(y = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (options.isEmpty()) {
                Text(AppStr.rouletteNoOptions, color = AppText())
            } else {
                Text(AppStr.rouletteSpinHint, color = AppPrimary(), fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showWinnerDialog && winningOption != null) {
        val won = winningOption!!
        AlertDialog(
            onDismissRequest = { showWinnerDialog = false },
            title = { Text(AppStr.rouletteWinnerTitle, fontWeight = FontWeight.Black) },
            text = { 
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(AppStr.rouletteWinnerMsg)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(won.name, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = AppPrimary())
                    Text("Rp ${won.amount}", fontSize = 18.sp, color = AppText())
                }
            },
            confirmButton = {
                com.bearbones.kumaflow.ui.components.KumaButton(
                    onClick = {
                        val dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale.forLanguageTag("id-ID")))
                        val timeStr = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        val newTx = KumaTransaction(
                            name = won.name,
                            date = dateStr,
                            amount = won.amount.toString(),
                            isIncome = false,
                            category = won.category,
                            wallet = selectedWallet ?: physicalWallets.firstOrNull() ?: "Cash",
                            timestamp = timeStr,
                            message = "Kuma Roulette Winner"
                        )
                        onSaveTransaction(listOf(Pair(newTx, emptyList())))
                        showWinnerDialog = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppPrimary())
                ) { Text(AppStr.rouletteConfirmBtn, color = Color.White) }
            },
            dismissButton = {
                KumaTextButton(onClick = { showWinnerDialog = false }) {
                    Text(AppStr.rouletteCancelBtn, color = AppText())
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = AppSurface(),
            titleContentColor = AppText(),
            textContentColor = AppText()
        )
    }
}

class ThousandSeparatorVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        if (original.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        
        val formatted = original.reversed().chunked(3).joinToString(".").reversed()
        
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val commasBefore = (offset - 1) / 3
                return offset + commasBefore
            }
            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                val originalLength = original.length
                val formattedLength = formatted.length
                val reverseOffset = formattedLength - offset
                val commasBeforeReverse = reverseOffset / 4
                val totalCommas = (originalLength - 1) / 3
                return offset - (totalCommas - commasBeforeReverse)
            }
        }
        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

class RouletteEasing(private val totalDurationMs: Float) : androidx.compose.animation.core.Easing {
    override fun transform(fraction: Float): Float {
        var A = 1000f
        var S = 5000f
        var F = totalDurationMs - A - S
        
        if (F < 0f) {
            val ratio = totalDurationMs / 6000f
            A = 1000f * ratio
            S = 5000f * ratio
            F = 0f
        }
        
        val D = totalDurationMs
        val Tot = A / 2f + F + S / 2f
        
        val tA = A / D
        val tF = (A + F) / D
        
        return when {
            fraction <= tA -> {
                val x = fraction / tA
                (x * x) * (A / 2f) / Tot
            }
            fraction <= tF -> {
                val x = (fraction - tA) / (tF - tA)
                ((A / 2f) + x * F) / Tot
            }
            else -> {
                val x = (fraction - tF) / (1f - tF)
                val easeOut = 1f - (1f - x) * (1f - x)
                ((A / 2f) + F + easeOut * (S / 2f)) / Tot
            }
        }
    }
}


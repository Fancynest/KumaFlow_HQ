@file:Suppress("SpellCheckingInspection", "UNUSED_PARAMETER", "unused", "CanBeVal", "DEPRECATION", "ScheduleExactAlarm")

package com.bearbones.kumaflow
import com.bearbones.kumaflow.utils.kumaClickable

import android.Manifest
import android.annotation.SuppressLint
import com.bearbones.kumaflow.ui.tutorial.tutorialTarget
import com.bearbones.kumaflow.ui.tutorial.TutorialStep
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.compose.ui.draw.alpha
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.interaction.collectIsPressedAsState
import com.bearbones.kumaflow.ui.components.KumaExpressiveIcon
import com.bearbones.kumaflow.utils.m3Shapes
import com.bearbones.kumaflow.utils.PolygonShape
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.drawBehind

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import com.bearbones.kumaflow.ui.components.BokehBackground
import dev.chrisbanes.haze.*
import androidx.compose.ui.draw.blur
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.bearbones.kumaflow.utils.bouncyScale
import com.bearbones.kumaflow.utils.MorphPolygonShape
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.star
import androidx.graphics.shapes.circle
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.graphics.shapes.CornerRounding
import com.bearbones.kumaflow.ui.components.KumaButton
import com.bearbones.kumaflow.ui.components.KumaIconButton
import com.bearbones.kumaflow.ui.components.KumaTextButton

// --- DATA CLASSES & OBJECTS ---


val LocalIsDark = compositionLocalOf { true }
val LocalIsAmoled = compositionLocalOf { false }
val LocalIsLiquidGlass = compositionLocalOf { false }
val LocalIsPremiumGlassBlur = compositionLocalOf { false }
val LocalHazeState = compositionLocalOf { HazeState() }

@Composable
fun AppBg(): Color {
    val isBrutal = com.bearbones.kumaflow.ui.theme.LocalIsBrutal.current
    return if (isBrutal && LocalIsDark.current) Color(0xFF1C1B20) else MaterialTheme.colorScheme.background
}

@Composable
fun AppSurface(): Color {
    val isBrutal = com.bearbones.kumaflow.ui.theme.LocalIsBrutal.current
    return if (isBrutal && LocalIsDark.current) Color(0xFF1C1B20) else MaterialTheme.colorScheme.surface
}

@Composable
fun AppText(): Color {
    val isBrutal = com.bearbones.kumaflow.ui.theme.LocalIsBrutal.current
    return if (isBrutal && LocalIsDark.current) Color(0xFFE8DEF8) else MaterialTheme.colorScheme.onSurface
}

@Composable
fun AppPrimary(): Color {
    val isBrutal = com.bearbones.kumaflow.ui.theme.LocalIsBrutal.current
    return if (isBrutal && LocalIsDark.current) Color(0xFFD0BCFF) else MaterialTheme.colorScheme.primary
}

@Composable
fun AppSurfaceVariant(): Color {
    val isBrutal = com.bearbones.kumaflow.ui.theme.LocalIsBrutal.current
    if (isBrutal) {
        return if (LocalIsDark.current) Color(0xFF2B2930) else com.bearbones.kumaflow.ui.theme.BrutalWhite
    }
    return if (LocalIsDark.current) {
        if (LocalIsAmoled.current) Color(0xFF1A1A1A) else Color(0xFF1E222B)
    } else {
        Color.White
    }
}

@Composable
fun AppGreen() = if (LocalIsDark.current) Color(0xFF2ECC71) else Color(0xFF16A34A)

@Composable
fun AppRed() = if (LocalIsDark.current) Color(0xFFFF5A5F) else Color(0xFFDC2626)

@Composable
fun Modifier.glassmorphic(
    radius: androidx.compose.ui.unit.Dp = 16.dp,
    borderAlpha: Float = 0.2f
): Modifier {
    return if (LocalIsLiquidGlass.current) {
        val glassColor = if (LocalIsDark.current) {
            Color.White.copy(alpha = 0.05f)
        } else {
            Color.White.copy(alpha = 0.15f)
        }
        val borderColor = if (LocalIsDark.current) {
            Color.White.copy(alpha = borderAlpha)
        } else {
            Color.White.copy(alpha = 0.5f)
        }
        
        this
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(radius))
            .background(glassColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(radius)
            )
    } else {
        val isDark = LocalIsDark.current
        val frostedBg = if (isDark) {
            AppSurfaceVariant().copy(alpha = 0.55f)
        } else {
            AppSurfaceVariant().copy(alpha = 0.65f)
        }
        val nonGlassBorder = if (isDark) {
            Color.White.copy(alpha = 0.12f)
        } else {
            Color.Black.copy(alpha = 0.08f)
        }
        this
            .shadow(
                elevation = if (isDark) 4.dp else 6.dp,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(radius),
                ambientColor = if (isDark) Color.Black.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.12f),
                spotColor = if (isDark) Color.Black.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.12f)
            )
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(radius))
            .background(frostedBg)
            .border(0.5.dp, nonGlassBorder, androidx.compose.foundation.shape.RoundedCornerShape(radius))
    }
}

@Composable
fun Modifier.neobrutalism(
    isBrutal: Boolean = true,
    cornerRadius: androidx.compose.ui.unit.Dp = 16.dp,
    borderWidth: androidx.compose.ui.unit.Dp = 3.dp,
    offset: androidx.compose.ui.unit.Dp = 4.dp,
    shadowColor: Color = if (LocalIsDark.current) com.bearbones.kumaflow.ui.theme.BrutalGreen else Color.Black,
    borderColor: Color = if (LocalIsDark.current) Color.White else Color.Black,
    backgroundColor: Color = Color.Unspecified
): Modifier {
    if (!isBrutal) return this

    return this
        .drawBehind {
            val cardPath = androidx.compose.ui.graphics.Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = 0f,
                        top = 0f,
                        right = size.width,
                        bottom = size.height,
                        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                    )
                )
            }
            
            // Draw shadow only OUTSIDE the card
            clipPath(
                path = cardPath,
                clipOp = androidx.compose.ui.graphics.ClipOp.Difference
            ) {
                drawRoundRect(
                    color = shadowColor,
                    topLeft = Offset(offset.toPx(), offset.toPx()),
                    size = size,
                    cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                )
            }
            
            // Draw background if specified
            if (backgroundColor != Color.Unspecified) {
                drawRoundRect(
                    color = backgroundColor,
                    size = size,
                    cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                )
            }
        }
        .border(width = borderWidth, color = borderColor, shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius))
        .clip(androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius))
}

@Composable
fun Modifier.glassCard(
    radius: androidx.compose.ui.unit.Dp = 12.dp,
    fallbackColor: Color,
    useHaze: Boolean = false,
    forceColor: Boolean = false
): Modifier {
    val isBrutal = com.bearbones.kumaflow.ui.theme.LocalIsBrutal.current
    if (isBrutal) {
        return this.neobrutalism(
            isBrutal = true,
            cornerRadius = radius,
            backgroundColor = fallbackColor,
            shadowColor = if (LocalIsDark.current) com.bearbones.kumaflow.ui.theme.BrutalGreen else Color.Black
        )
    }

    val glassColor = if (forceColor) {
        fallbackColor
    } else if (LocalIsDark.current) {
        Color(0xFF2C2C2E).copy(alpha = 0.60f)
    } else {
        Color.White.copy(alpha = 0.60f)
    }

    val shineGradient = remember {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.1f),
                Color.Transparent,
                Color.Transparent,
                Color.White.copy(alpha = 0.05f)
            ),
            start = Offset.Zero,
            end = Offset.Infinite
        )
    }

    val isDark = LocalIsDark.current
    val borderGradient = remember(isDark) {
        if (isDark) {
            Brush.linearGradient(
                colors = listOf(Color(0x50808080), Color(0x50404040), Color(0x50808080))
            )
        } else {
            Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.4f), Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.4f))
            )
        }
    }

    return if (LocalIsLiquidGlass.current) {
        val tintAlpha = if (LocalIsPremiumGlassBlur.current) 0.45f else 0.35f
        val adjustedColor = glassColor.copy(alpha = (glassColor.alpha + tintAlpha).coerceAtMost(1f))
        
        val hazeModifier = if (useHaze) {
            if (LocalIsPremiumGlassBlur.current) {
                Modifier.hazeChild(
                    state = LocalHazeState.current,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(radius),
                    style = HazeStyle(blurRadius = 30.dp, backgroundColor = Color.Transparent, tint = dev.chrisbanes.haze.HazeTint(adjustedColor), noiseFactor = 0.15f)
                )
            } else {
                Modifier.hazeChild(
                    state = LocalHazeState.current,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(radius),
                    style = HazeStyle(blurRadius = 20.dp, backgroundColor = Color.Transparent, tint = dev.chrisbanes.haze.HazeTint(adjustedColor), noiseFactor = 0.1f)
                )
            }
        } else {
            // Frosted glass: background text is fully obscured, gradient subtly peeks through
            val glassFallback = if (isDark) {
                Color(0xFF2C2C2E).copy(alpha = 0.82f)
            } else {
                Color.White.copy(alpha = 0.88f)
            }
            Modifier.background(glassFallback)
        }

        this.clip(androidx.compose.foundation.shape.RoundedCornerShape(radius))
            .then(hazeModifier)
            .background(shineGradient)
            .border(0.5.dp, borderGradient, androidx.compose.foundation.shape.RoundedCornerShape(radius))
    } else {
        val frostedBg = if (isDark) {
            fallbackColor.copy(alpha = 0.55f)
        } else {
            fallbackColor.copy(alpha = 0.65f)
        }
        val nonGlassBorder = remember(isDark) {
            if (isDark) {
                Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.04f), Color.White.copy(alpha = 0.12f))
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.08f), Color.Black.copy(alpha = 0.03f), Color.Black.copy(alpha = 0.08f))
                )
            }
        }
        this
            .shadow(
                elevation = if (isDark) 4.dp else 6.dp,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(radius),
                ambientColor = if (isDark) Color.Black.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.12f),
                spotColor = if (isDark) Color.Black.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.12f)
            )
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(radius))
            .background(frostedBg)
            .background(shineGradient)
            .border(0.5.dp, nonGlassBorder, androidx.compose.foundation.shape.RoundedCornerShape(radius))
    }
}

@Composable
fun getGlassTextFieldColors(): androidx.compose.material3.TextFieldColors {
    return if (LocalIsLiquidGlass.current) {
        val glassContainer = Color.Transparent
        val glassBorder = Color.Transparent
        OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = glassContainer,
            focusedContainerColor = glassContainer,
            unfocusedBorderColor = glassBorder,
            focusedBorderColor = AppPrimary()
        )
    } else {
        OutlinedTextFieldDefaults.colors()
    }
}

fun showBiometricPrompt(activity: FragmentActivity, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val executor = ContextCompat.getMainExecutor(activity)
    val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            onSuccess()
        }
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            onError(errString.toString())
        }
    })

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(AppStr.secKuma)
        .setSubtitle(AppStr.scanBio)
        .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK)
        .setNegativeButtonText(AppStr.usePin)
        .build()

    biometricPrompt.authenticate(promptInfo)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    userProfileState: UserProfile?,
    dao: TransactionDao,
    onOpenWrapped: (Int, Int) -> Unit = { _, _ -> },
    homeListState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    onOverlayStateChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val pagerState = rememberPagerState(pageCount = { 5 })
    val transactionListWithSplits by dao.getAllTransactionsWithSplits().collectAsState(initial = emptyList())
    val userProfile = userProfileState ?: UserProfile(userName = "User")
    var selectedItemIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedItemIndex) { if (pagerState.currentPage != selectedItemIndex) pagerState.animateScrollToPage(selectedItemIndex) }
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) { if (!pagerState.isScrollInProgress) selectedItemIndex = pagerState.currentPage }

    var selectedMonth by remember { mutableIntStateOf(java.time.LocalDateTime.now().monthValue) }
    var selectedYear by remember { mutableIntStateOf(java.time.LocalDateTime.now().year) }
    var forceUpdateTrigger by remember { mutableIntStateOf(0) }


    // ðŸ”¥ STATE SELECTION HOISTING ðŸ”¥
    var selectedTxs by remember { mutableStateOf(setOf<Int>()) }
    val isSelectionMode = selectedTxs.isNotEmpty()

    val isFabVisible by remember {
        derivedStateOf {
            homeListState.firstVisibleItemIndex == 0 || !homeListState.isScrollInProgress
        }
    }

    val monthlyTransactionsWithSplits by remember(transactionListWithSplits, selectedMonth, selectedYear, forceUpdateTrigger) {
        derivedStateOf {
            transactionListWithSplits.filter { t ->
                try {
                    val dt = java.time.LocalDateTime.parse(t.transaction.timestamp, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    dt.monthValue == selectedMonth && dt.year == selectedYear
                } catch (e: Exception) { true }
            }
        }
    }

    val walletBalances by remember(transactionListWithSplits, userProfile.wallets, userProfile.useCarryOver, selectedMonth, selectedYear, forceUpdateTrigger) {
        derivedStateOf {
            val balances = userProfile.wallets.split(",").filter { it.isNotBlank() }.associateWith { 0L }.toMutableMap()
            val relevantTxs = if (userProfile.useCarryOver) {
                transactionListWithSplits.filter { t ->
                    try {
                        val dt = java.time.LocalDateTime.parse(t.transaction.timestamp, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        dt.year < selectedYear || (dt.year == selectedYear && dt.monthValue <= selectedMonth)
                    } catch (e: Exception) { false }
                }
            } else monthlyTransactionsWithSplits

            relevantTxs.forEach { txObj ->
                if (txObj.splits.isNotEmpty()) {
                    txObj.splits.forEach { split ->
                        val current = balances[split.splitWallet] ?: 0L
                        balances[split.splitWallet] = current + (if (txObj.transaction.isIncome) split.splitAmount else -split.splitAmount)
                    }
                } else {
                    val amt = txObj.transaction.amount.toLongOrNull() ?: 0L
                    val current = balances[txObj.transaction.wallet] ?: 0L
                    balances[txObj.transaction.wallet] = current + (if (txObj.transaction.isIncome) amt else -amt)
                }
            }
            balances
        }
    }

    val totalBalance by remember(walletBalances, userProfile.savingsWallets, forceUpdateTrigger) { 
        derivedStateOf { 
            val saveWallets = userProfile.savingsWallets.split(",").filter { it.isNotBlank() }.toSet()
            walletBalances.filterKeys { it !in saveWallets }.values.sum() 
        } 
    }
    val totalIncome by remember(monthlyTransactionsWithSplits, forceUpdateTrigger) { derivedStateOf { monthlyTransactionsWithSplits.filter { it.transaction.isIncome && it.transaction.category != "Transfer" }.sumOf { it.transaction.amount.toLongOrNull() ?: 0L } } }
    val totalExpenses by remember(monthlyTransactionsWithSplits, forceUpdateTrigger) { derivedStateOf { monthlyTransactionsWithSplits.filter { !it.transaction.isIncome && it.transaction.category != "Transfer" }.sumOf { it.transaction.amount.toLongOrNull() ?: 0L } } }

    var showMilestone by remember { mutableStateOf(false /* userProfile.currentStreak > 0 && userProfile.currentStreak % 30 == 0 && userProfile.currentStreak > userProfile.lastMilestoneNotified */) }
    var showRouletteSheet by remember { mutableStateOf(false) }
    var showSplitBill by remember { mutableStateOf(false) }
    
    if (showMilestone) {
        com.bearbones.kumaflow.ui.screens.MilestonePopUp(
            profile = userProfile,
            onDismiss = {
                showMilestone = false
                scope.launch {
                    val updatedProfile = userProfile.copy(lastMilestoneNotified = userProfile.currentStreak)
                    dao.saveProfile(updatedProfile)
                }
            },
            onShare = {
                com.bearbones.kumaflow.utils.ShareStreakUtils.shareStreak(context, userProfile)
            }
        )
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showBottomSheet by remember { mutableStateOf(false) }
    var showBackupReminder by remember { mutableStateOf(false) }
    var showQrTransfer by remember { mutableStateOf(false) }
    var showDuoSync by remember { mutableStateOf(false) }
    var showDuoPairing by remember { mutableStateOf(false) }
    LaunchedEffect(showBottomSheet) { onOverlayStateChange(showBottomSheet) }
    var transactionToEdit by remember { mutableStateOf<TransactionWithSplits?>(null) }
    val totalTxCount = transactionListWithSplits.size

    // FAB shape state - changes when sheet closes, but only visible when pressed
    var targetFabShapeIndex by remember { mutableIntStateOf(if (m3Shapes.size > 1) (1..m3Shapes.lastIndex).random() else 0) }
    LaunchedEffect(showBottomSheet) {
        if (!showBottomSheet && m3Shapes.size > 1) {
            targetFabShapeIndex = (1..m3Shapes.lastIndex).random()
        }
    }

    androidx.activity.compose.BackHandler(enabled = showBottomSheet || pagerState.currentPage != 0 || isSelectionMode) {
        if (showBottomSheet) {
            showBottomSheet = false
        } else if (isSelectionMode) {
            selectedTxs = emptySet()
        } else if (pagerState.currentPage != 0) {
            scope.launch { pagerState.animateScrollToPage(0) }
            selectedItemIndex = 0
        }
    }

    if (showRouletteSheet) {
        com.bearbones.kumaflow.ui.screens.RouletteBottomSheet(
            walletBalances = walletBalances,
            physicalWallets = userProfile.wallets.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            categories = (userProfile.expenseCats.split(",") + userProfile.incomeCats.split(",")).map { it.trim() }.filter { it.isNotEmpty() }.distinct(),
            onDismiss = { showRouletteSheet = false },
            onSaveTransaction = { txList ->
                scope.launch {
                    txList.forEach { (tx, splits) ->
                        dao.insertFullTransaction(tx, splits)
                    }
                    forceUpdateTrigger++
                    updateKumaWidget(context)
                }
                showRouletteSheet = false
            }
        )
    }

    if (showSplitBill) {
        val splitViewModel: com.bearbones.kumaflow.ui.screens.SplitBillViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
        
        LaunchedEffect(Unit) {
            splitViewModel.resetState()
            splitViewModel.setTotalBill(0L)
        }

        com.bearbones.kumaflow.ui.screens.SplitBillSheet(
            viewModel = splitViewModel,
            qrisFilePath = userProfile.qrisFilePath,
            holderName = userProfile.qrisHolderName,
            bankName = userProfile.bankName,
            bankAccount = userProfile.bankAccount,
            physicalWallets = userProfile.wallets.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            expenseCategories = (userProfile.expenseCats.split(",") + userProfile.incomeCats.split(",")).map { it.trim() }.filter { it.isNotEmpty() }.distinct(),
            onSaveExpense = { tx ->
                scope.launch {
                    dao.insertFullTransaction(tx, emptyList())
                    forceUpdateTrigger++
                    updateKumaWidget(context)
                    Toast.makeText(context, AppStr.splitExpenseRecorded, Toast.LENGTH_SHORT).show()
                }
            },
            onDismissRequest = { showSplitBill = false }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            val isSelectionModeActive = isSelectionMode || showBottomSheet
            val showFab = selectedItemIndex == 0 && isFabVisible && !isSelectionModeActive
            
            androidx.compose.animation.AnimatedVisibility(
                visible = showFab,
                enter = androidx.compose.animation.scaleIn(),
                exit = androidx.compose.animation.scaleOut()
            ) {
                val fabInteractionSource = remember { MutableInteractionSource() }
                val isPressed by fabInteractionSource.collectIsPressedAsState()
                
                val progress by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (isPressed) 1f else 0f,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                    ),
                    label = "fabMorph"
                )
                
                val morph = remember(targetFabShapeIndex) { 
                    androidx.graphics.shapes.Morph(androidx.graphics.shapes.RoundedPolygon.circle(), m3Shapes[targetFabShapeIndex]) 
                }
                val fabShape = remember(progress) { MorphPolygonShape(morph, progress) }
                
                val isDark = LocalIsDark.current
                val bgColor = if (LocalIsLiquidGlass.current) {
                    if (isDark) Color(0xFF2C2C2E).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.7f)
                } else {
                    AppPrimary()
                }
                val fgColor = if (LocalIsLiquidGlass.current) AppPrimary() else Color.White
                val isLiquidGlass = LocalIsLiquidGlass.current
                
                val tutorialState = com.bearbones.kumaflow.ui.tutorial.LocalTutorialState.current
                
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(70.dp)
                        .tutorialTarget(TutorialStep.HOME_ADD_BTN)
                        .bouncyScale(fabInteractionSource)
                        .graphicsLayer {
                            shadowElevation = if (isLiquidGlass) 0f else 6.dp.toPx()
                            shape = fabShape
                            clip = true
                        }
                        .background(bgColor)
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
                                transactionToEdit = null
                                showBottomSheet = true
                                if (tutorialState.currentStep == TutorialStep.HOME_ADD_BTN) {
                                    tutorialState.next()
                                }
                            }
                        )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(32.dp), tint = fgColor)
                }
            }
        },
        bottomBar = {
            androidx.compose.animation.AnimatedVisibility(
                visible = !showBottomSheet && !showQrTransfer && !showDuoSync && !showDuoPairing,
                enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
                exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it })
            ) {
                CustomBottomNav(pagerState, haptic) { scope.launch { pagerState.animateScrollToPage(it) }; selectedItemIndex = it }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(
            bottom = paddingValues.calculateBottomPadding(),
            start = paddingValues.calculateStartPadding(androidx.compose.ui.platform.LocalLayoutDirection.current),
            end = paddingValues.calculateEndPadding(androidx.compose.ui.platform.LocalLayoutDirection.current)
        )) {
            val isOREasterEgg = userProfile.userName.contains("#OR", ignoreCase = true)
            if (isOREasterEgg) {
                val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
                val lightProgressState = infiniteTransition.animateFloat(
                    initialValue = -0.5f,
                    targetValue = 1.5f,
                    animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                        animation = androidx.compose.animation.core.tween(4000, easing = androidx.compose.animation.core.LinearEasing),
                        repeatMode = androidx.compose.animation.core.RepeatMode.Restart
                    )
                )

                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val pageOffset = pagerState.currentPage + pagerState.currentPageOffsetFraction
                    val screenWidth = size.width

                    translate(left = -pageOffset * screenWidth) {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            val h = size.height
                            val w = screenWidth

                            // Screen 1: Home (Start at middle-left and wave down to bottom of heart)
                            moveTo(0f, h * 0.2f)
                            cubicTo(
                                w * 0.5f, h * 0.2f,
                                w * 0.8f, h * 0.8f,
                                w * 2.0f, h * 0.8f // Bottom tip of the heart (Boundary of History/Report)
                            )
                            
                            // Screen 2: Right lobe of heart (in Report)
                            cubicTo(
                                w * 2.8f, h * 0.8f,
                                w * 2.5f, h * 0.1f,
                                w * 2.0f, h * 0.4f // Center dip of heart
                            )
                            
                            // Screen 2 to 3: Left lobe of heart (in History)
                            cubicTo(
                                w * 1.5f, h * 0.1f,
                                w * 1.2f, h * 0.8f,
                                w * 2.0f, h * 0.8f // Crosses back at the bottom tip
                            )
                            
                            // Screen 3 to 4: Exit to Settings
                            cubicTo(
                                w * 2.5f, h * 0.8f,
                                w * 3.5f, h * 0.2f,
                                w * 4.0f, h * 0.6f
                            )
                        }

                        if (screenWidth > 0f && size.height > 0f) {
                            // Base shadow for depth
                            drawPath(path, color = Color.Black.copy(alpha=0.15f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 16.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                            
                            val yarnBaseColor = Color(0xFFC2185B)
                            val startX = screenWidth * 4f * lightProgressState.value
                            val lightBrush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(yarnBaseColor, Color(0xFFFF80AB), Color.White, Color(0xFFFF80AB), yarnBaseColor),
                                startX = startX - screenWidth * 0.5f,
                                endX = startX + screenWidth * 0.5f
                            )
                            
                            drawPath(path, brush = lightBrush, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 14.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                        }
                    }
                }
                

                // TRACKLIST BACKGROUND
                val tracklist = listOf(
                    "Drop dead", "Stupid song", "Honeybee", "Maggots for brains", "U + Me = <3",
                    "My Way", "Purple", "The Cure", "Begged", "What's Wrong with Me (feat. Robert Smith)",
                    "Less", "Expectations", "Cigarette Smoke"
                )
                val density = androidx.compose.ui.platform.LocalDensity.current
                val conf = androidx.compose.ui.platform.LocalConfiguration.current
                val screenWidthPx = remember(density, conf) { with(density) { conf.screenWidthDp.dp.toPx() } }
                val screenHeightPx = remember(density, conf) { with(density) { conf.screenHeightDp.dp.toPx() } }
                
                Box(modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.6f)
                ) {
                    val pageOffset = pagerState.currentPage + pagerState.currentPageOffsetFraction
                    val random = remember { kotlin.random.Random(42) }
                    
                    val customFontFamily = remember { 
                        androidx.compose.ui.text.font.FontFamily(
                            androidx.compose.ui.text.font.Font(com.bearbones.kumaflow.R.font.olivia_regular)
                        )
                    }

                    Box(modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationX = -pageOffset * screenWidthPx }
                    ) {
                        val positions = listOf(
                            // Page 0
                            Pair(0.05f, 0.15f), Pair(0.55f, 0.25f), Pair(0.15f, 0.45f),
                            Pair(0.65f, 0.60f), Pair(0.10f, 0.75f), Pair(0.50f, 0.90f),
                            // Page 1
                            Pair(1.05f, 0.15f), Pair(1.55f, 0.25f), Pair(1.15f, 0.45f),
                            Pair(1.65f, 0.60f), Pair(1.10f, 0.75f), Pair(1.50f, 0.90f),
                            // Page 2
                            Pair(2.05f, 0.15f), Pair(2.55f, 0.25f), Pair(2.15f, 0.45f),
                            Pair(2.65f, 0.60f), Pair(2.10f, 0.75f), Pair(2.50f, 0.90f),
                            // Page 3
                            Pair(3.05f, 0.15f), Pair(3.55f, 0.25f), Pair(3.15f, 0.45f),
                            Pair(3.65f, 0.60f), Pair(3.10f, 0.75f), Pair(3.50f, 0.90f)
                        )
                        val displaySongs = remember {
                            val list = mutableListOf<String>()
                            var i = 0
                            while (list.size < positions.size) {
                                list.add(tracklist[i % tracklist.size])
                                i++
                            }
                            list
                        }
                        
                        displaySongs.forEachIndexed { index, song ->
                            val xPos = positions[index].first * screenWidthPx
                            val yPos = positions[index].second * screenHeightPx
                            
                            val rot = remember {
                                random.nextFloat() * 30f - 15f
                            }
                            
                            Text(
                                text = song,
                                fontFamily = customFontFamily,
                                fontSize = 28.sp,
                                color = Color(0xFFC2185B),
                                modifier = Modifier
                                    .graphicsLayer {
                                        translationX = xPos
                                        translationY = yPos + (kotlin.math.sin(lightProgressState.value.toDouble() * kotlin.math.PI + index.toDouble()).toFloat() * 15f)
                                        rotationZ = rot
                                    }
                            )
                        }
                    }
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    0 -> HomeScreen(
                        profile = userProfile,
                        transactionsWithSplits = monthlyTransactionsWithSplits,
                        balance = totalBalance,
                        walletBalances = walletBalances,
                        income = totalIncome,
                        expenses = totalExpenses,
                        selectedMonth = selectedMonth,
                        selectedYear = selectedYear,
                        paddingValues = paddingValues,
                        onMonthChange = { m: Int, y: Int -> haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedMonth = m; selectedYear = y },
                        onEdit = { t: TransactionWithSplits -> transactionToEdit = t; showBottomSheet = true },
                        onDelete = { t: TransactionWithSplits -> scope.launch { try { dao.updateFullTransaction(t.transaction.copy(isDeleted = true, lastModified = System.currentTimeMillis(), syncVersion = t.transaction.syncVersion + 1), t.splits); updateKumaWidget(context) } catch (e: Exception) { Toast.makeText(context, "Delete Error: ${e.message}", Toast.LENGTH_LONG).show() } } },
                        onOpenWrapped = onOpenWrapped,
                        onOpenSplitBill = { showSplitBill = true },
                        listState = homeListState,
                        selectedTxs = selectedTxs,
                        onToggleSelect = { id: Int ->
                            val newSet = selectedTxs.toMutableSet()
                            if (newSet.contains(id)) newSet.remove(id) else newSet.add(id)
                            selectedTxs = newSet
                        },
                        clearSelection = { selectedTxs = emptySet() },
                        onBulkDelete = { listToDelete: List<TransactionWithSplits> ->
                            scope.launch {
                                try {
                                    listToDelete.forEach { dao.updateFullTransaction(it.transaction.copy(isDeleted = true, lastModified = System.currentTimeMillis(), syncVersion = it.transaction.syncVersion + 1), it.splits) }
                                    updateKumaWidget(context)
                                    Toast.makeText(context, AppStr.txDeleted(listToDelete.size), Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Bulk Delete Error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onUpdateProfile = { updatedProfile -> scope.launch { dao.saveProfile(updatedProfile); forceUpdateTrigger++; updateKumaWidget(context) } },
                        onBulkUpdateCategory = { listToUpdate: List<TransactionWithSplits>, newCat: String ->
                            scope.launch {
                                listToUpdate.forEach { txObj ->
                                    val updatedTx = txObj.transaction.copy(
                                        category = newCat,
                                        lastModified = System.currentTimeMillis(),
                                        syncVersion = txObj.transaction.syncVersion + 1
                                    )
                                    dao.updateFullTransaction(updatedTx, txObj.splits)
                                }
                                forceUpdateTrigger++
                                updateKumaWidget(context)
                                Toast.makeText(context, AppStr.txChangedTo(listToUpdate.size, newCat), Toast.LENGTH_SHORT).show()
                            }
                        },
                        onAddTransaction = { isIncome ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            transactionToEdit = null
                            // Pass the intention to the bottom sheet if we ever support pre-selecting
                            showBottomSheet = true
                        },
                        onOpenRoulette = { showRouletteSheet = true },
                        onReconcile = { walletName, delta ->
                            scope.launch {
                                try {
                                    val isIncome = delta > 0
                                    val amountStr = kotlin.math.abs(delta).toString()
                                    val timeStr = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                    val dateStr = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern(userProfile.dateFormat, java.util.Locale.forLanguageTag("id-ID")))
                                    
                                    val adjustmentTx = KumaTransaction(
                                        name = if (isIncome) "Koreksi Plus - Kelebihan Saldo" else "Koreksi Minus - Lupa Catat",
                                        date = dateStr,
                                        amount = amountStr,
                                        isIncome = isIncome,
                                        category = "Balancing",
                                        wallet = walletName,
                                        timestamp = timeStr,
                                        message = "Auto-Reconciliation"
                                    )
                                    
                                    dao.insertFullTransaction(adjustmentTx, emptyList())
                                    forceUpdateTrigger++
                                    updateKumaWidget(context)
                                    Toast.makeText(context, if(AppStr.isId) "Saldo $walletName berhasil disesuaikan!" else "$walletName balance adjusted successfully!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Reconciliation Error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                    1 -> com.bearbones.kumaflow.ui.screens.HistoryScreen(
                        profile = userProfile,
                        allTransactions = transactionListWithSplits,
                        dao = dao,
                        paddingValues = paddingValues,
                        onEdit = { t: TransactionWithSplits -> transactionToEdit = t; showBottomSheet = true },
                        onDelete = { t: TransactionWithSplits -> scope.launch { try { dao.deleteTransaction(t.transaction); updateKumaWidget(context) } catch (e: Exception) { Toast.makeText(context, "Delete Error: ${e.message}", Toast.LENGTH_LONG).show() } } },
                        onToggleSelect = { id: Int ->
                            val newSet = selectedTxs.toMutableSet()
                            if (newSet.contains(id)) newSet.remove(id) else newSet.add(id)
                            selectedTxs = newSet
                        },
                        selectedTxs = selectedTxs,
                        isSelectionMode = isSelectionMode,
                        onBulkDelete = { listToDelete: List<TransactionWithSplits> ->
                            scope.launch {
                                try {
                                    listToDelete.forEach { dao.deleteTransaction(it.transaction) }
                                    updateKumaWidget(context)
                                    Toast.makeText(context, AppStr.txDeleted(listToDelete.size), Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Delete Error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        clearSelection = { selectedTxs = emptySet() }
                    )
                    2 -> com.bearbones.kumaflow.ui.screens.SavingsScreen(
                        profile = userProfile,
                        dao = dao,
                        paddingValues = paddingValues,
                        walletBalances = walletBalances,
                        onAddTransaction = { showBottomSheet = true; transactionToEdit = null },
                        forceUpdateTrigger = forceUpdateTrigger
                    )
                    3 -> ReportScreen(
                        profile = userProfile, monthlyTransactions = monthlyTransactionsWithSplits.map { it.transaction }, allTransactions = transactionListWithSplits.map { it.transaction }, income = totalIncome, expenses = totalExpenses, balance = totalBalance, selectedMonth = selectedMonth, selectedYear = selectedYear,
                        paddingValues = paddingValues,
                        onMonthChange = { m, y -> haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedMonth = m; selectedYear = y },
                        onOpenWrapped = onOpenWrapped
                    )
                    4 -> SettingsScreen(
                        currentProfile = userProfile, monthlyTransactionsWithSplits = monthlyTransactionsWithSplits, allTransactionsWithSplits = transactionListWithSplits, dao = dao, selectedMonth = selectedMonth, selectedYear = selectedYear,
                        paddingValues = paddingValues,
                        onForceUpdate = { forceUpdateTrigger++; updateKumaWidget(context) },
                        onOpenQrTransfer = { showQrTransfer = true },
                        onOpenDuoSync = { showDuoSync = true }
                    )
                }
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = showBottomSheet,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut()
        ) {
            var offsetY by remember { mutableFloatStateOf(0f) }
            val animatedOffsetY by androidx.compose.animation.core.animateFloatAsState(targetValue = offsetY)
            LaunchedEffect(showBottomSheet) { if (showBottomSheet) offsetY = 0f }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .kumaClickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { showBottomSheet = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .animateEnterExit(
                            enter = androidx.compose.animation.slideInVertically(
                                animationSpec = androidx.compose.animation.core.spring(
                                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                    stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                                ),
                                initialOffsetY = { it }
                            ) + androidx.compose.animation.scaleIn(
                                animationSpec = androidx.compose.animation.core.spring(
                                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                    stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                                ),
                                initialScale = 0.8f,
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                            ),
                            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.scaleOut(targetScale = 0.8f)
                        )
                        .offset { androidx.compose.ui.unit.IntOffset(0, animatedOffsetY.toInt()) }
                        .kumaClickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { /* Prevent click through */ }
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (offsetY > 300f) showBottomSheet = false else offsetY = 0f
                                },
                                onDragCancel = { offsetY = 0f }
                            ) { change: androidx.compose.ui.input.pointer.PointerInputChange, dragAmount: Float ->
                                change.consume()
                                if (offsetY + dragAmount > 0f) offsetY += dragAmount
                            }
                        }
                        .glassCard(32.dp, AppBg(), useHaze = true)
                ) {
                    TransactionBottomSheet(
                        profile = userProfile, transactionToEdit = transactionToEdit, onDismiss = { showBottomSheet = false },
                        onSave = { txList ->
                            scope.launch {
                                try {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    txList.forEach { (newTrans, splits) -> if (newTrans.id == 0) dao.insertFullTransaction(newTrans, splits) else dao.updateFullTransaction(newTrans, splits) }
                                    com.bearbones.kumaflow.utils.StreakManager.checkAndUpdateStreak(dao)
                                    if ((totalTxCount + txList.size) % 10 == 0) showBackupReminder = true
                                    forceUpdateTrigger++; updateKumaWidget(context)
                                    Toast.makeText(context, AppStr.txSaved, Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Save Error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onUpdateProfile = { updatedProfile -> scope.launch { dao.saveProfile(updatedProfile); forceUpdateTrigger++; updateKumaWidget(context) } }
                    )
                }
            }
        }

        if (showBackupReminder) {
            AlertDialog(
                onDismissRequest = { showBackupReminder = false },
                title = { Text(AppStr.backupReminderTitle, fontWeight = FontWeight.Black) },
                text = { Text(AppStr.backupReminderMsg) },
                confirmButton = {
                    KumaButton(
                        onClick = { showBackupReminder = false; backupAppToJSON(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = AppPrimary())
                    ) { Text(AppStr.backupNow, color = Color.White) }
                },
                dismissButton = { KumaTextButton(onClick = { showBackupReminder = false }) { Text(AppStr.later, color = AppText()) } },
                shape = RoundedCornerShape(28.dp), containerColor = AppSurface(), titleContentColor = AppText(), textContentColor = AppText()
            )
        }

        if (showQrTransfer && userProfileState != null) {
            com.bearbones.kumaflow.ui.screens.QrTransferScreen(
                onBack = { showQrTransfer = false },
                profile = userProfileState,
                allTransactionsWithSplits = transactionListWithSplits
            )
        }

        if (showDuoSync && userProfileState != null) {
            com.bearbones.kumaflow.ui.screens.DuoSyncScreen(
                onBack = { showDuoSync = false },
                onNavigateToPairing = { showDuoSync = false; showDuoPairing = true },
                database = com.bearbones.kumaflow.KumaDatabase.getDatabase(context)
            )
        }

        if (showDuoPairing && userProfileState != null) {
            com.bearbones.kumaflow.ui.screens.DuoPairingScreen(
                onBack = { showDuoPairing = false; showDuoSync = true },
                profile = userProfileState,
                database = com.bearbones.kumaflow.KumaDatabase.getDatabase(context)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
data class SplitItemUi(var id: String, var wallet: String, var amount: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionBottomSheet(
    profile: UserProfile,
    transactionToEdit: TransactionWithSplits?,
    onDismiss: () -> Unit,
    onSave: (List<Pair<KumaTransaction, List<TransactionSplit>>>) -> Unit,
    onUpdateProfile: (UserProfile) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val baseTx = transactionToEdit?.transaction

    var txMode by remember(baseTx) { mutableIntStateOf(if (baseTx != null && baseTx.isIncome) 1 else 0) }

    val calendar = remember { java.util.Calendar.getInstance() }
    var txDateStr by remember(baseTx) {
        mutableStateOf(baseTx?.date ?: java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern(profile.dateFormat, java.util.Locale.forLanguageTag("id-ID"))))
    }
    var txTimestamp by remember(baseTx) {
        mutableStateOf(baseTx?.timestamp ?: java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME))
    }
    
    var showSplitBill by remember { mutableStateOf(false) }

    var showM3DatePicker by remember { mutableStateOf(false) }

    val expenseCategories = remember(profile.expenseCats) { profile.expenseCats.split(",").filter { it.isNotBlank() } }
    val incomeCategories = remember(profile.incomeCats) { profile.incomeCats.split(",").filter { it.isNotBlank() } }
    var walletList by remember(profile.wallets) { mutableStateOf(profile.wallets.split(",").filter { it.isNotBlank() }) }

    val currentCategories = if (txMode == 1) incomeCategories else expenseCategories
    var selectedCategory by remember(baseTx, txMode) {
        mutableStateOf(if (baseTx != null && (baseTx.isIncome == (txMode == 1))) baseTx.category else "")
    }

    var name by remember(baseTx) { mutableStateOf(baseTx?.name ?: "") }
    var message by remember(baseTx) { mutableStateOf(baseTx?.message ?: "") }

    var transferFromWallet by remember { mutableStateOf(walletList.firstOrNull() ?: "Cash") }
    var transferToWallet by remember { mutableStateOf(if (walletList.size > 1) walletList[1] else "Cash") }

    val initialSplits = remember(transactionToEdit) {
        if (transactionToEdit != null && transactionToEdit.splits.isNotEmpty()) {
            transactionToEdit.splits.map {
                SplitItemUi(java.util.UUID.randomUUID().toString(), it.splitWallet, it.splitAmount.toString())
            }.toMutableStateList()
        } else {
            mutableStateListOf(
                SplitItemUi(java.util.UUID.randomUUID().toString(), baseTx?.wallet ?: walletList.firstOrNull() ?: "Cash", baseTx?.amount ?: "")
            )
        }
    }

    var expandedCat by remember { mutableStateOf(false) }
    var showNewWalletDialog by remember { mutableStateOf(false) }
    var newWalletName by remember { mutableStateOf("") }

    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }

    val allowedMathChars = setOf('0','1','2','3','4','5','6','7','8','9','+','-','*','/','(',')',' ','.')

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(AppText().copy(alpha = 0.2f)))
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (baseTx == null) AppStr.addTx else AppStr.editTx,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AppText()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .tutorialTarget(TutorialStep.ADD_TX_TABS)
                .padding(end = if (com.bearbones.kumaflow.ui.theme.LocalIsBrutal.current) 4.dp else 0.dp, bottom = if (com.bearbones.kumaflow.ui.theme.LocalIsBrutal.current) 4.dp else 0.dp)
                .height(50.dp)
                .glassCard(16.dp, AppSurfaceVariant(), useHaze = false)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (txMode == 1) AppGreen() else Color.Transparent)
                    .kumaClickable { txMode = 1; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                contentAlignment = Alignment.Center
            ) {
                Text(AppStr.inc, color = if (txMode == 1) Color.White else AppText(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (txMode == 0) AppRed() else Color.Transparent)
                    .kumaClickable { txMode = 0; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                contentAlignment = Alignment.Center
            ) {
                Text(AppStr.exp, color = if (txMode == 0) Color.White else AppText(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            if (baseTx == null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (txMode == 2) Color(0xFF1976D2) else Color.Transparent)
                        .kumaClickable { txMode = 2; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(AppStr.mutasi, color = if (txMode == 2) Color.White else AppText(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .tutorialTarget(TutorialStep.ADD_TX_DATE)
        ) {
            com.bearbones.kumaflow.ui.components.KumaOutlinedTextField(
                value = txDateStr,
                onValueChange = {},
                readOnly = true,
                placeholder = { Text(AppStr.date) },
                trailingIcon = {
                    KumaExpressiveIcon(Icons.Default.CalendarToday, contentDescription = null, tint = AppPrimary(), size = 24.dp)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            // Transparent overlay to intercept clicks properly without disabling the TextField
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .kumaClickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showM3DatePicker = true
                    }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (txMode == 2) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                var expFrom by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expFrom,
                    onExpandedChange = { expFrom = !expFrom },
                    modifier = Modifier.weight(1f)
                ) {
                    com.bearbones.kumaflow.ui.components.KumaOutlinedTextField(
                        value = transferFromWallet,
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text(AppStr.tarikDari) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expFrom) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expFrom,
                        onDismissRequest = { expFrom = false }
                    ) {
                        walletList.forEach { w ->
                            DropdownMenuItem(
                                text = { Text(w) },
                                onClick = { transferFromWallet = w; expFrom = false }
                            )
                        }
                    }
                }

                var expTo by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expTo,
                    onExpandedChange = { expTo = !expTo },
                    modifier = Modifier.weight(1f)
                ) {
                    com.bearbones.kumaflow.ui.components.KumaOutlinedTextField(
                        value = transferToWallet,
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text(AppStr.simpanKe) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expTo) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expTo,
                        onDismissRequest = { expTo = false }
                    ) {
                        walletList.forEach { w ->
                            DropdownMenuItem(
                                text = { Text(w) },
                                onClick = { transferToWallet = w; expTo = false }
                            )
                        }
                    }
                }
            }
        } else {
            ExposedDropdownMenuBox(
                expanded = expandedCat,
                onExpandedChange = { expandedCat = !expandedCat },
                modifier = Modifier.fillMaxWidth().tutorialTarget(TutorialStep.ADD_TX_CATEGORY)
            ) {
                com.bearbones.kumaflow.ui.components.KumaOutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text(AppStr.cat) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCat) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryEditable, true)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                ExposedDropdownMenu(
                    expanded = expandedCat,
                    onDismissRequest = { expandedCat = false }
                ) {
                    currentCategories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                selectedCategory = cat
                                expandedCat = false
                            }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(AppStr.addCat, color = AppPrimary(), fontWeight = FontWeight.Bold) },
                        onClick = {
                            expandedCat = false
                            showNewCategoryDialog = true
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        com.bearbones.kumaflow.ui.components.KumaOutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text(if (txMode == 2) "${AppStr.nme} (Opsional)" else AppStr.nme) },
            modifier = Modifier
                .fillMaxWidth()
                .tutorialTarget(TutorialStep.ADD_TX_TITLE_NOTES)
                .focusRequester(focusRequester),
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )

        Spacer(modifier = Modifier.height(12.dp))

        com.bearbones.kumaflow.ui.components.KumaOutlinedTextField(
            value = message,
            onValueChange = { message = it },
            placeholder = { Text(AppStr.msgInp) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )

        Spacer(modifier = Modifier.height(24.dp))

        val curSym = when(profile.currency) {
            "USD", "AUD", "CAD", "SGD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "JPY", "CNY" -> "¥"
            "CHF" -> "CHF"
            "MYR" -> "RM"
            "THB" -> "฿"
            "PHP" -> "₱"
            "VND" -> "₫"
            else -> "Rp"
        }

        if (txMode == 2) {
            com.bearbones.kumaflow.ui.components.KumaOutlinedTextField(
                value = initialSplits[0].amount,
                onValueChange = {
                    if (it.all { c -> c in allowedMathChars }) {
                        initialSplits[0] = initialSplits[0].copy(amount = it)
                    }
                },
                placeholder = { Text("Cth: 15000+2000") },
                visualTransformation = if (initialSplits[0].amount.any { c -> c in "+-*/()" }) androidx.compose.ui.text.input.VisualTransformation.None else ThousandSeparatorTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            val calcTransfer = evaluateMathExpression(initialSplits[0].amount) ?: 0L
            Text(
                "${AppStr.totalTransfer}: $curSym ${NumberFormat.getInstance(java.util.Locale.getDefault()).format(calcTransfer)}",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = AppText()
            )
        } else {
            Text(
                AppStr.splitSource,
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = AppText()
            )
            Spacer(modifier = Modifier.height(8.dp))

            initialSplits.forEachIndexed { index, splitItem ->
                var expandedWallet by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .tutorialTarget(if (index == 0) TutorialStep.ADD_TX_FUNDING else TutorialStep.NONE)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = expandedWallet,
                        onExpandedChange = { expandedWallet = !expandedWallet },
                        modifier = Modifier.weight(1.2f)
                    ) {
                        com.bearbones.kumaflow.ui.components.KumaOutlinedTextField(
                            value = splitItem.wallet,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWallet) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryEditable, true)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedWallet,
                            onDismissRequest = { expandedWallet = false }
                        ) {
                            walletList.forEach { w ->
                                DropdownMenuItem(
                                    text = { Text(w) },
                                    onClick = {
                                        initialSplits[index] = splitItem.copy(wallet = w)
                                        expandedWallet = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(AppStr.addWallet, color = AppPrimary(), fontWeight = FontWeight.Bold) },
                                onClick = {
                                    expandedWallet = false
                                    showNewWalletDialog = true
                                }
                            )
                        }
                    }

                    com.bearbones.kumaflow.ui.components.KumaOutlinedTextField(
                        value = splitItem.amount,
                        onValueChange = {
                            if (it.all { c -> c in allowedMathChars }) {
                                initialSplits[index] = splitItem.copy(amount = it)
                            }
                        },
                        placeholder = { Text("15000+2000") },
                        visualTransformation = if (splitItem.amount.any { c -> c in "+-*/()" }) androidx.compose.ui.text.input.VisualTransformation.None else ThousandSeparatorTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (initialSplits.size > 1) {
                        KumaIconButton(
                            onClick = { initialSplits.removeAt(index) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            KumaExpressiveIcon(Icons.Default.RemoveCircleOutline, null, tint = AppRed(), containerColor = androidx.compose.ui.graphics.Color.Transparent, size = 24.dp, iconPadding = 2.dp)
                        }
                    }
                }
            }

            KumaTextButton(
                onClick = {
                    initialSplits.add(
                        SplitItemUi(
                            java.util.UUID.randomUUID().toString(),
                            walletList.firstOrNull() ?: "Cash",
                            ""
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                KumaExpressiveIcon(Icons.Default.Add, null, tint = AppPrimary(), containerColor = AppPrimary().copy(alpha = 0.1f))
                Spacer(modifier = Modifier.width(8.dp))
                Text(AppStr.addOtherWallet, color = AppPrimary(), fontWeight = FontWeight.Bold)
            }

            val totalAmount = initialSplits.sumOf { evaluateMathExpression(it.amount) ?: 0L }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "${AppStr.total}: $curSym ${NumberFormat.getInstance(java.util.Locale.getDefault()).format(totalAmount)}",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = AppText()
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        val evaluatedSplits = initialSplits.map { it.copy(amount = (evaluateMathExpression(it.amount) ?: 0L).toString()) }
        val totalAmtFinal = if (txMode == 2) (evaluatedSplits[0].amount.toLongOrNull() ?: 0L) else evaluatedSplits.sumOf { it.amount.toLongOrNull() ?: 0L }
        val isAmountValid = totalAmtFinal > 0L

        if (txMode == 0 && isAmountValid) {
            KumaButton(
                onClick = { showSplitBill = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppPrimary().copy(alpha = 0.2f), contentColor = AppPrimary()),
                shape = RoundedCornerShape(16.dp)
            ) {
                KumaExpressiveIcon(Icons.Default.ReceiptLong, contentDescription = null, containerColor = AppText().copy(alpha = 0.05f))
                Spacer(modifier = Modifier.width(8.dp))
                Text(AppStr.splitBill, fontWeight = FontWeight.Bold)
            }
        }

        KumaButton(
            onClick = {
                val timeStr = txTimestamp
                val dateStr = txDateStr

                if (txMode == 2) {
                    val title = name.ifEmpty { AppStr.defMutasiTitle }
                    val txOut = KumaTransaction(id = 0, name = title, date = dateStr, amount = totalAmtFinal.toString(), isIncome = false, category = "Transfer", wallet = transferFromWallet, timestamp = timeStr, message = message)
                    val txIn = KumaTransaction(id = 0, name = title, date = dateStr, amount = totalAmtFinal.toString(), isIncome = true, category = "Transfer", wallet = transferToWallet, timestamp = timeStr, message = message)

                    onSave(listOf(Pair(txOut, emptyList()), Pair(txIn, emptyList())))
                } else {
                    val parentWalletStr = if(evaluatedSplits.size > 1) "${AppStr.multiWallet} (${evaluatedSplits.size})" else evaluatedSplits[0].wallet
                    val newTx = if (baseTx != null) {
                        baseTx.copy(
                            name = name,
                            date = dateStr,
                            amount = totalAmtFinal.toString(),
                            isIncome = (txMode == 1),
                            category = selectedCategory,
                            wallet = parentWalletStr,
                            timestamp = timeStr,
                            message = message,
                            isEdited = true,
                            lastModified = System.currentTimeMillis(),
                            syncVersion = baseTx.syncVersion + 1
                        )
                    } else {
                        KumaTransaction(
                            id = 0,
                            name = name,
                            date = dateStr,
                            amount = totalAmtFinal.toString(),
                            isIncome = (txMode == 1),
                            category = selectedCategory,
                            wallet = parentWalletStr,
                            timestamp = timeStr,
                            message = message,
                            isEdited = false
                        )
                    }
                    val dbSplits = evaluatedSplits.filter { it.amount.isNotBlank() && (it.amount.toLongOrNull() ?: 0L) > 0 }.map { TransactionSplit(transactionId = 0, splitWallet = it.wallet, splitAmount = it.amount.toLongOrNull() ?: 0L) }

                    onSave(listOf(Pair(newTx, dbSplits)))
                }
                onDismiss()
            },
            modifier = Modifier
                .fillMaxWidth()
                .tutorialTarget(TutorialStep.ADD_TX_SAVE)
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppPrimary(),
                contentColor = if (com.bearbones.kumaflow.ui.theme.LocalIsBrutal.current) AppBg() else Color.White,
                disabledContainerColor = AppPrimary().copy(alpha = 0.5f),
                disabledContentColor = if (com.bearbones.kumaflow.ui.theme.LocalIsBrutal.current) AppBg().copy(alpha = 0.5f) else Color.White.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp),
            brutalCornerRadius = 16.dp,
            enabled = isAmountValid && (txMode == 2 || (name.isNotEmpty() && selectedCategory.isNotEmpty()))
        ) {
            Text(AppStr.saveTx, fontWeight = FontWeight.ExtraBold)
        }
    }

    if (showM3DatePicker) {
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = calendar.timeInMillis
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showM3DatePicker = false },
            confirmButton = {
                com.bearbones.kumaflow.ui.components.KumaTextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val cal = java.util.Calendar.getInstance().apply { timeInMillis = millis }
                        val locale = java.util.Locale.forLanguageTag("id-ID")
                        txDateStr = java.time.LocalDateTime.of(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH), 0, 0).format(java.time.format.DateTimeFormatter.ofPattern(profile.dateFormat, locale))

                        val now = java.time.LocalDateTime.now()
                        txTimestamp = java.time.LocalDateTime.of(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH), now.hour, now.minute).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    }
                    showM3DatePicker = false
                }) {
                    Text("OK", color = AppPrimary(), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                com.bearbones.kumaflow.ui.components.KumaTextButton(onClick = { showM3DatePicker = false }) {
                    Text(AppStr.no, color = AppText())
                }
            },
            colors = androidx.compose.material3.DatePickerDefaults.colors(containerColor = AppBg())
        ) {
            androidx.compose.material3.DatePicker(
                state = datePickerState,
                colors = androidx.compose.material3.DatePickerDefaults.colors(
                    containerColor = Color.Transparent,
                    titleContentColor = AppPrimary(),
                    headlineContentColor = AppText(),
                    weekdayContentColor = AppText().copy(alpha=0.7f),
                    subheadContentColor = AppText(),
                    yearContentColor = AppText(),
                    currentYearContentColor = AppPrimary(),
                    selectedYearContentColor = Color.White,
                    selectedYearContainerColor = AppPrimary(),
                    dayContentColor = AppText(),
                    disabledDayContentColor = AppText().copy(alpha=0.3f),
                    selectedDayContentColor = Color.White,
                    selectedDayContainerColor = AppPrimary(),
                    todayContentColor = AppPrimary(),
                    todayDateBorderColor = AppPrimary()
                )
            )
        }
    }

    if (showNewWalletDialog) {
        AlertDialog(
            onDismissRequest = { showNewWalletDialog = false; newWalletName = "" },
            title = { Text(AppStr.newWallet, fontWeight = FontWeight.Bold) },
            text = {
                com.bearbones.kumaflow.ui.components.KumaOutlinedTextField(
                    value = newWalletName,
                    onValueChange = { newWalletName = it },
                    placeholder = { Text(AppStr.walletName) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                KumaButton(
                    onClick = {
                        if (newWalletName.isNotBlank() && !walletList.contains(newWalletName.trim())) {
                            val newWallet = newWalletName.trim()
                            val updatedList = walletList + newWallet
                            walletList = updatedList
                            onUpdateProfile(profile.copy(wallets = updatedList.joinToString(",")))
                        }
                        showNewWalletDialog = false
                        newWalletName = ""
                    }
                ) { Text(AppStr.save) }
            },
            dismissButton = {
                KumaTextButton(onClick = { showNewWalletDialog = false }) {
                    Text(AppStr.no, color = AppText())
                }
            }
        )
    }

    if (showNewCategoryDialog) {
        var selectedIconKey by remember { mutableStateOf("Kategori") }

        AlertDialog(
            onDismissRequest = { showNewCategoryDialog = false; newCategoryName = "" },
            title = { Text(AppStr.newCat, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    com.bearbones.kumaflow.ui.components.KumaOutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        placeholder = { Text(AppStr.catName) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(AppStr.chooseCatIcon, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AppText())
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        modifier = Modifier.height(150.dp).glassCard(8.dp, AppSurfaceVariant(), useHaze = false)
                    ) {
                        items(kumaIconLibrary.keys.toList()) { key ->
                            val icon = kumaIconLibrary[key]!!
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedIconKey == key) AppPrimary() else Color.Transparent)
                                    .kumaClickable {
                                        selectedIconKey = key
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                KumaExpressiveIcon(icon, contentDescription = key, tint = if (selectedIconKey == key) Color.White else AppText(), containerColor = androidx.compose.ui.graphics.Color.Transparent, size = 30.dp, iconPadding = 5.dp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                KumaButton(
                    onClick = {
                        if (newCategoryName.isNotBlank() && !currentCategories.contains(newCategoryName.trim())) {
                            val newCat = newCategoryName.trim()

                            val iconJson = try { JSONObject(profile.categoryIcons) } catch (e: Exception) { JSONObject() }
                            iconJson.put(newCat, selectedIconKey)

                            if (txMode == 1) {
                                val updated = incomeCategories + newCat
                                onUpdateProfile(profile.copy(incomeCats = updated.joinToString(","), categoryIcons = iconJson.toString()))
                            } else {
                                val updated = expenseCategories + newCat
                                onUpdateProfile(profile.copy(expenseCats = updated.joinToString(","), categoryIcons = iconJson.toString()))
                            }
                            selectedCategory = newCat
                        }
                        showNewCategoryDialog = false
                        newCategoryName = ""
                    }
                ) { Text(AppStr.save) }
            },
            dismissButton = {
                KumaTextButton(onClick = { showNewCategoryDialog = false }) {
                    Text(AppStr.no, color = AppText())
                }
            }
        )
    }

    if (showSplitBill) {
        val splitViewModel: com.bearbones.kumaflow.ui.screens.SplitBillViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
        
        // Auto-fill total bill based on current transaction total
        val evaluatedSplits = initialSplits.map { it.copy(amount = (evaluateMathExpression(it.amount) ?: 0L).toString()) }
        val totalAmtFinal = evaluatedSplits.sumOf { it.amount.toLongOrNull() ?: 0L }
        
        LaunchedEffect(Unit) {
            splitViewModel.resetState()
            splitViewModel.setTotalBill(totalAmtFinal)
        }

        LaunchedEffect(totalAmtFinal) {
            splitViewModel.setTotalBill(totalAmtFinal)
        }

        com.bearbones.kumaflow.ui.screens.SplitBillSheet(
            viewModel = splitViewModel,
            qrisFilePath = profile.qrisFilePath,
            holderName = profile.qrisHolderName,
            bankName = profile.bankName,
            bankAccount = profile.bankAccount,
            physicalWallets = profile.wallets.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            expenseCategories = (profile.expenseCats.split(",") + profile.incomeCats.split(",")).map { it.trim() }.filter { it.isNotEmpty() }.distinct(),
            onSaveExpense = { tx ->
                onSave(listOf(Pair(tx, emptyList())))
                Toast.makeText(context, AppStr.splitExpenseRecorded, Toast.LENGTH_SHORT).show()
                showSplitBill = false
                onDismiss()
            },
            onDismissRequest = { showSplitBill = false }
        )
    }
}

@Composable
fun MonthYearSelector(currentMonth: Int, currentYear: Int, onMonthChange: (Int, Int) -> Unit) {
    val monthNames = if (AppStr.isId) {
        listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
    } else {
        listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(20.dp, AppSurfaceVariant(), useHaze = false)
            .padding(vertical = 6.dp)
    ) {
        KumaIconButton(
            onClick = {
                var m = currentMonth - 1
                var y = currentYear
                if (m < 1) {
                    m = 12
                    y -= 1
                }
                onMonthChange(m, y)
            }
        ) {
            KumaExpressiveIcon(Icons.Default.ChevronLeft, null, tint = AppText(), containerColor = androidx.compose.ui.graphics.Color.Transparent)
        }

        Text(
            text = "${monthNames[currentMonth - 1]} $currentYear",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            color = AppText(),
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        KumaIconButton(
            onClick = {
                var m = currentMonth + 1
                var y = currentYear
                if (m > 12) {
                    m = 1
                    y += 1
                }
                onMonthChange(m, y)
            }
        ) {
            KumaExpressiveIcon(Icons.Default.ChevronRight, null, tint = AppText(), containerColor = androidx.compose.ui.graphics.Color.Transparent)
        }
    }
}


// --- 6. SHARED COMPONENTS (AutoSizeText, PDF, CSV, Item) ---

@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    maxLines: Int = 1,
    minimumFallbackSize: TextUnit = 12.sp,
    textAlign: TextAlign? = null
) {
    var scaledTextStyle by remember { mutableStateOf(TextStyle(fontSize = fontSize)) }
    var readyToDraw by remember { mutableStateOf(false) }

    Text(
        text = text,
        modifier = modifier.drawWithContent { if (readyToDraw) drawContent() },
        color = color,
        fontSize = scaledTextStyle.fontSize,
        fontWeight = fontWeight,
        maxLines = maxLines,
        softWrap = false,
        textAlign = textAlign,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.hasVisualOverflow && scaledTextStyle.fontSize > minimumFallbackSize) {
                scaledTextStyle = scaledTextStyle.copy(fontSize = scaledTextStyle.fontSize * 0.9f)
            } else {
                readyToDraw = true
            }
        }
    )
}

class ThousandSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val formattedText = try {
            NumberFormat.getInstance(Locale.getDefault()).format(originalText.toLong())
        } catch (_: Exception) {
            originalText
        }
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                var transformedOffset = 0
                var digitsSeen = 0
                while (digitsSeen < offset && transformedOffset < formattedText.length) {
                    if (formattedText[transformedOffset].isDigit()) {
                        digitsSeen++
                    }
                    transformedOffset++
                }
                return transformedOffset
            }
            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                val textBeforeCursor = formattedText.substring(0, offset.coerceAtMost(formattedText.length))
                return textBeforeCursor.filter { it.isDigit() }.length.coerceAtMost(originalText.length)
            }
        }
        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}

fun drawHeaders(
    canvas: android.graphics.Canvas,
    paint: Paint,
    pageNum: Int,
    profile: UserProfile,
    titlePaint: Paint,
    headerPaint: Paint,
    periodStr: String,
    logoBitmap: android.graphics.Bitmap?
) {
    if (logoBitmap != null) {
        val destRect = android.graphics.RectF(40f, 40f, 90f, 90f)
        canvas.drawBitmap(logoBitmap, null, destRect, paint)
        canvas.drawText("${AppStr.repPdf} ($pageNum)", 100f, 50f, titlePaint)
        canvas.drawText("${AppStr.cur}: ${profile.currency} | Periode: $periodStr", 100f, 75f, Paint().apply { textSize = 12f })
    } else {
        canvas.drawText("${AppStr.repPdf} ($pageNum)", 40f, 50f, titlePaint)
        canvas.drawText("${AppStr.cur}: ${profile.currency} | Periode: $periodStr", 40f, 75f, Paint().apply { textSize = 12f })
    }
    
    canvas.drawLine(40f, 95f, 550f, 95f, paint)
    canvas.drawText(AppStr.date, 40f, 115f, headerPaint)
    canvas.drawText(AppStr.cat, 120f, 115f, headerPaint)
    canvas.drawText(AppStr.walletShort, 200f, 115f, headerPaint)
    canvas.drawText(AppStr.nme, 280f, 115f, headerPaint)
    canvas.drawText(AppStr.amt, 480f, 115f, headerPaint)
    canvas.drawLine(40f, 125f, 550f, 125f, paint)
}

fun generatePDF(context: Context, data: List<KumaTransaction>, profile: UserProfile, month: Int, year: Int) {
    val sortedData = data.sortedBy { it.timestamp } // Sort oldest to newest chronologically
    val monthNames = if (AppStr.isId) listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember") else listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    val periodStr = if (month in 1..12) "${monthNames[month - 1]} $year" else if (AppStr.isId) "Kustom/Filter" else "Custom/Filtered"
    val pdfDocument = PdfDocument()
    var pageNum = 1
    var page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNum).create())

    val paint = Paint()
    val titlePaint = Paint().apply { isFakeBoldText = true; textSize = 18f; color = android.graphics.Color.BLACK }
    val headerPaint = Paint().apply { isFakeBoldText = true; textSize = 12f; color = android.graphics.Color.DKGRAY }
    val curSym = when(profile.currency) { "USD", "AUD", "CAD", "SGD" -> "$"; "EUR" -> "€"; "GBP" -> "£"; "JPY", "CNY" -> "¥"; "CHF" -> "CHF"; "MYR" -> "RM"; "THB" -> "฿"; "PHP" -> "₱"; "VND" -> "₫"; else -> "Rp" }

    var logoBitmap: android.graphics.Bitmap? = null
    try {
        val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
        if (drawable != null) {
            logoBitmap = android.graphics.Bitmap.createBitmap(drawable.intrinsicWidth.coerceAtLeast(1), drawable.intrinsicHeight.coerceAtLeast(1), android.graphics.Bitmap.Config.ARGB_8888)
            val canvasBitmap = android.graphics.Canvas(logoBitmap)
            drawable.setBounds(0, 0, canvasBitmap.width, canvasBitmap.height)
            drawable.draw(canvasBitmap)
        }
    } catch (e: Exception) {}

    drawHeaders(page.canvas, paint, pageNum, profile, titlePaint, headerPaint, periodStr, logoBitmap)
    var yPos = 150f
    
    val formatter = java.text.NumberFormat.getInstance(java.util.Locale("id", "ID"))

    sortedData.forEach { item ->
        if (yPos > 800f) {
            pdfDocument.finishPage(page)
            pageNum++
            page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNum).create())
            drawHeaders(page.canvas, paint, pageNum, profile, titlePaint, headerPaint, periodStr, logoBitmap)
            yPos = 150f
        }
        val amountPrefix = if (item.isIncome) "+" else "-"
        val amountColor = if (item.isIncome) android.graphics.Color.parseColor("#1B5E20") else android.graphics.Color.parseColor("#B71C1C")
        
        val amtVal = item.amount.toLongOrNull() ?: 0L
        val formattedAmt = formatter.format(amtVal)

        paint.color = android.graphics.Color.BLACK
        page.canvas.drawText(item.date.take(12), 40f, yPos, paint)
        page.canvas.drawText(item.category.take(10), 120f, yPos, paint)
        page.canvas.drawText(item.wallet.take(10), 200f, yPos, paint)
        page.canvas.drawText(item.name.take(25), 280f, yPos, paint)

        paint.color = amountColor
        page.canvas.drawText("$amountPrefix $curSym $formattedAmt", 480f, yPos, paint)
        yPos += 25f
    }

    if (yPos > 720f) {
        pdfDocument.finishPage(page)
        pageNum++
        page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNum).create())
        yPos = 50f
    }

    paint.color = android.graphics.Color.BLACK
    page.canvas.drawLine(40f, yPos, 550f, yPos, paint)
    yPos += 20f
    val inc = sortedData.filter { it.isIncome }.sumOf { it.amount.toLongOrNull() ?: 0L }
    val exp = sortedData.filter { !it.isIncome }.sumOf { it.amount.toLongOrNull() ?: 0L }
    page.canvas.drawText("${AppStr.inc}: $curSym ${formatter.format(inc)}", 40f, yPos, titlePaint.apply { textSize = 12f; color = android.graphics.Color.parseColor("#1B5E20") })
    yPos += 20f
    page.canvas.drawText("${AppStr.exp}: $curSym ${formatter.format(exp)}", 40f, yPos, titlePaint.apply { textSize = 12f; color = android.graphics.Color.parseColor("#B71C1C") })
    
    yPos += 30f
    val sdf = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale("id", "ID"))
    val printDateStr = if (AppStr.isId) "Dicetak pada: ${sdf.format(java.util.Date())}" else "Printed on: ${sdf.format(java.util.Date())}"
    val footerTextPaint = Paint().apply { textSize = 10f; color = android.graphics.Color.WHITE; isFakeBoldText = true }
    val textWidth = footerTextPaint.measureText(printDateStr)
    val bgRect = android.graphics.RectF(40f, yPos - 12f, 40f + textWidth + 20f, yPos + 6f)
    val bgPaint = Paint().apply { color = android.graphics.Color.parseColor("#1B5E20") }
    page.canvas.drawRoundRect(bgRect, 10f, 10f, bgPaint)
    page.canvas.drawText(printDateStr, 50f, yPos, footerTextPaint)

    pdfDocument.finishPage(page)
    val fileSuffix = if (month in 1..12) "${month}_${year}" else "Filtered"
    val filename = "KumaFlow_Report_$fileSuffix.pdf"
    
    try {
        val bos = java.io.ByteArrayOutputStream()
        pdfDocument.writeTo(bos)
        saveToMediaStore(context, filename, "application/pdf", "KumaPDF", bos.toByteArray())
    } catch (_: Exception) {
        android.widget.Toast.makeText(context, AppStr.failPdf, android.widget.Toast.LENGTH_SHORT).show()
    } finally {
        pdfDocument.close()
    }
}

fun generateCSV(context: Context, data: List<KumaTransaction>, profile: UserProfile, month: Int, year: Int) {
    val fileSuffix = if (month in 1..12) "${month}_${year}" else "Filtered"
    val filename = "KumaFlow_Report_$fileSuffix.csv"
    try {
        val sb = java.lang.StringBuilder()
        sb.append("${AppStr.date},${AppStr.cat},${AppStr.walletShort},${AppStr.type},${AppStr.nme},${AppStr.msgInp},${AppStr.cur},${AppStr.amt}\n")
        data.sortedBy { it.timestamp }.forEach { t ->
            val type = if (t.isIncome) AppStr.inc else AppStr.exp
            sb.append("${t.date},${t.category},${t.wallet},$type,\"${t.name}\",\"${t.message}\",${profile.currency},${t.amount}\n")
        }
        saveToMediaStore(context, filename, "text/csv", "KumaCSV", sb.toString().toByteArray())
    } catch (_: Exception) {
        Toast.makeText(context, AppStr.failCsv, Toast.LENGTH_SHORT).show()
    }
}

fun exportToDrive(context: Context, data: List<KumaTransaction>, profile: UserProfile, month: Int, year: Int) {
    val filename = "KumaFlow_Drive_${month}_${year}.csv"
    try {
        val sb = java.lang.StringBuilder()
        sb.append("${AppStr.date},${AppStr.cat},${AppStr.walletShort},${AppStr.type},${AppStr.nme},${AppStr.msgInp},${AppStr.cur},${AppStr.amt}\n")
        data.sortedBy { it.timestamp }.forEach { t ->
            val type = if (t.isIncome) AppStr.inc else AppStr.exp
            sb.append("${t.date},${t.category},${t.wallet},$type,\"${t.name}\",\"${t.message}\",${profile.currency},${t.amount}\n")
        }
        saveToMediaStore(context, filename, "text/csv", "KumaCSV", sb.toString().toByteArray())
    } catch (_: Exception) {
        Toast.makeText(context, AppStr.failCsv, Toast.LENGTH_LONG).show()
    }
}

fun backupAppToJSON(context: Context) {
    // Launch Coroutine to perform DB read and file IO off the main thread
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
        try {
            val dao = KumaDatabase.getDatabase(context).transactionDao()
            val profile = dao.getProfileSync() ?: return@launch
            val txsWithSplits = dao.getAllTransactionsWithSplitsSync()

            if (txsWithSplits.isEmpty()) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toast.makeText(context, AppStr.noTx, Toast.LENGTH_SHORT).show()
                }
                // Do NOT return here, continue backing up profile and wallets
            }
            val root = JSONObject()
            root.put("backupVersion", 7) // Incremented backup version

            val pJson = JSONObject().apply {
                put("userName", profile.userName)
                put("isAppLocked", profile.isAppLocked)
                put("appPin", profile.appPin)
                put("currency", profile.currency)
                put("dateFormat", profile.dateFormat)
                put("monthlyTarget", profile.monthlyTarget)
                put("themeMode", profile.themeMode)
                put("isReminderOn", profile.isReminderOn)
                put("reminderTimes", profile.reminderTimes)
                put("useCarryOver", profile.useCarryOver)
                put("expenseCats", profile.expenseCats)
                put("incomeCats", profile.incomeCats)
                put("wallets", profile.wallets)
                put("categoryTargets", profile.categoryTargets)
                put("isAmoledMode", profile.isAmoledMode)
                put("categoryIcons", profile.categoryIcons)
                put("isLiquidGlass", profile.isLiquidGlass)
                put("isPremiumGlassBlur", profile.isPremiumGlassBlur)
                put("currentStreak", profile.currentStreak)
                put("lastActiveDate", profile.lastActiveDate)
                put("freezeCount", profile.freezeCount)
                put("lastMilestoneNotified", profile.lastMilestoneNotified)
                put("savingsWallets", profile.savingsWallets)
                put("savingsGoals", profile.savingsGoals)
                put("qrisFilePath", profile.qrisFilePath)
                put("qrisHolderName", profile.qrisHolderName)
                put("bankName", profile.bankName)
                put("bankAccount", profile.bankAccount)
                if (profile.qrisFilePath.isNotEmpty()) {
                    try {
                        val file = java.io.File(profile.qrisFilePath)
                        if (file.exists()) {
                            val bytes = file.readBytes()
                            val base64Str = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                            put("qrisBase64", base64Str)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            root.put("profile", pJson)

            // Backup custom card images
            val customCardsDir = java.io.File(context.filesDir, "custom_cards")
            if (customCardsDir.exists()) {
                val cardsArr = JSONArray()
                customCardsDir.listFiles()?.forEach { file ->
                    try {
                        val bytes = file.readBytes()
                        val base64Str = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                        cardsArr.put(JSONObject().apply {
                            put("name", file.name)
                            put("data", base64Str)
                        })
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (cardsArr.length() > 0) {
                    root.put("customCards", cardsArr)
                }
            }
            
            val virtualWallets = dao.getAllVirtualWallets()
            if (virtualWallets.isNotEmpty()) {
                val vwArr = JSONArray()
                virtualWallets.forEach { vw ->
                    vwArr.put(JSONObject().apply {
                        put("name", vw.name)
                        put("orderIndex", vw.orderIndex)
                        put("backgroundType", vw.backgroundType)
                        put("backgroundValue", vw.backgroundValue)
                        put("cardNumber", vw.cardNumber)
                        put("notes", vw.notes)
                        put("cardLabel", vw.cardLabel)
                    })
                }
                root.put("virtualWallets", vwArr)
            }
            
            val walletMetadatas = dao.getAllWalletMetadata()
            if (walletMetadatas.isNotEmpty()) {
                val wmArr = JSONArray()
                walletMetadatas.forEach { wm ->
                    wmArr.put(JSONObject().apply {
                        put("walletStableId", wm.walletStableId)
                        put("currentName", wm.currentName)
                        put("createdAt", wm.createdAt)
                        put("nameLastModified", wm.nameLastModified)
                    })
                }
                root.put("walletMetadata", wmArr)
            }

            val tArr = JSONArray()
            txsWithSplits.forEach { obj ->
                val tJson = JSONObject().apply {
                    put("name", obj.transaction.name)
                    put("date", obj.transaction.date)
                    put("amount", obj.transaction.amount)
                    put("isIncome", obj.transaction.isIncome)
                    put("category", obj.transaction.category)
                    put("wallet", obj.transaction.wallet)
                    put("timestamp", obj.transaction.timestamp)
                    put("message", obj.transaction.message)
                    put("isEdited", obj.transaction.isEdited)
                }
                if (obj.splits.isNotEmpty()) {
                    val splitArr = JSONArray()
                    obj.splits.forEach { s ->
                        splitArr.put(JSONObject().apply {
                            put("w", s.splitWallet)
                            put("a", s.splitAmount)
                        })
                    }
                    tJson.put("splits", splitArr)
                }
                tArr.put(tJson)
            }

            root.put("transactions", tArr)
            val filename = "KumaFlow_Backup_${System.currentTimeMillis()}.kuma"
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                saveToMediaStore(context, filename, "application/json", "KumaBackup", root.toString().toByteArray())
            }
        } catch (_: Exception) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                Toast.makeText(context, AppStr.failBak, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private fun saveToMediaStore(
    context: Context,
    filename: String,
    mimeType: String,
    subFolder: String,
    content: ByteArray
) {
    try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOCUMENTS + "/KumaFlow/" + subFolder)
            }
            val uri = context.contentResolver.insert(android.provider.MediaStore.Files.getContentUri("external"), values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { it.write(content) }
                android.widget.Toast.makeText(context, "Saved to Documents/KumaFlow/$subFolder", android.widget.Toast.LENGTH_LONG).show()
            } else {
                android.widget.Toast.makeText(context, "Failed to create file", android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            val dir = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS), "KumaFlow/$subFolder")
            if (!dir.exists()) dir.mkdirs()
            val file = java.io.File(dir, filename)
            java.io.FileOutputStream(file).use { it.write(content) }
            android.widget.Toast.makeText(context, "Saved to Documents/KumaFlow/$subFolder", android.widget.Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun SettingsGroupCard(
    title: String,
    modifier: Modifier = Modifier,
    items: List<Pair<String, ImageVector>>,
    hasSwitch: Boolean = false,
    isSwitchOn: Boolean = false,
    onSwitchChange: (Boolean) -> Unit = {},
    onClick: (String) -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .glassCard(24.dp, AppSurface()),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                title,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = AppText(),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(18.dp))
            items.forEachIndexed { index, (label, icon) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .kumaClickable { onClick(label) }
                        .padding(vertical = 14.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KumaExpressiveIcon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AppPrimary(),
                        containerColor = AppPrimary().copy(alpha = 0.1f),
                        size = 32.dp,
                        iconPadding = 7.dp
                    )
                    Text(
                        label,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 14.dp),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppText(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (hasSwitch && label == AppStr.appLck) {
                        Switch(
                            checked = isSwitchOn,
                            onCheckedChange = onSwitchChange,
                            modifier = Modifier.scale(0.8f)
                        )
                    } else {
                        KumaExpressiveIcon(Icons.Default.ChevronRight, contentDescription = null, tint = AppText().copy(alpha = 0.3f), containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    }
                }
                if (index < items.size - 1) {
                    HorizontalDivider(modifier = Modifier.padding(start = 50.dp), color = AppText().copy(alpha = 0.05f))
                }
            }
        }
    }
}

@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun CustomBottomNav(
    pagerState: androidx.compose.foundation.pager.PagerState,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onItemSelected: (Int) -> Unit
) {
    val items = listOf(
        Pair(Icons.Rounded.Home, AppStr.home),
        Pair(Icons.Rounded.History, AppStr.hist),
        Pair(Icons.Rounded.AccountBalanceWallet, if (AppStr.isId) "Tabungan" else "Savings"),
        Pair(Icons.Rounded.Equalizer, AppStr.rep),
        Pair(Icons.Rounded.Settings, AppStr.set)
    )
    val itemCount = items.size

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
            .height(75.dp)
            .glassCard(32.dp, AppSurface(), useHaze = true)
            .border(1.dp, AppText().copy(alpha = 0.1f), RoundedCornerShape(32.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val segmentWidth = size.width / itemCount.toFloat()
                    val targetIndex = (change.position.x / segmentWidth).toInt().coerceIn(0, itemCount - 1)
                    if (targetIndex != pagerState.currentPage) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onItemSelected(targetIndex)
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { offset ->
                    val segmentWidth = size.width / itemCount.toFloat()
                    val targetIndex = (offset.x / segmentWidth).toInt().coerceIn(0, itemCount - 1)
                    if (targetIndex != pagerState.currentPage) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onItemSelected(targetIndex)
                    }
                })
            }
    ) {
        val segmentWidth = maxWidth / itemCount
        val exactPosition = (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, (itemCount - 1).toFloat())
        val indicatorOffset = segmentWidth * exactPosition

        // Sliding Pill Frame
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .padding(6.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(if (LocalIsDark.current) Color.White.copy(alpha = 0.12f) else AppPrimary().copy(alpha = 0.15f))
                .border(1.dp, if (LocalIsDark.current) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f), RoundedCornerShape(32.dp))
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, pair ->
                val isSelected = pagerState.currentPage == index
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f).padding(vertical = 4.dp)
                ) {
                    KumaExpressiveIcon(
                        imageVector = pair.first,
                        contentDescription = null,
                        tint = if (isSelected) AppText() else AppText().copy(alpha = 0.5f),
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        size = 28.dp,
                        iconPadding = 2.dp
                    )
                    Text(
                        pair.second,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSelected) AppText() else AppText().copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun IncomeExpensePill(label: String, amount: String, color: Color, isUp: Boolean) {
    Column(horizontalAlignment = Alignment.End) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KumaExpressiveIcon(
                        imageVector = if (isUp) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = color,
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        size = 12.dp,
                        iconPadding = 0.dp
                    )
            Text(
                " $label",
                color = color,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp
            )
        }
        AutoSizeText(
            text = amount,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            minimumFallbackSize = 8.sp
        )
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = AppText()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionItem(
    profile: UserProfile,
    obj: TransactionWithSplits,
    isPrivacyMode: Boolean,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onEdit: (TransactionWithSplits) -> Unit,
    onDelete: (TransactionWithSplits) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val trans = obj.transaction
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    val curSym = remember(profile.currency) {
        when(profile.currency) {
            "USD", "AUD", "CAD", "SGD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "JPY", "CNY" -> "¥"
            "CHF" -> "CHF"
            "MYR" -> "RM"
            "THB" -> "฿"
            "PHP" -> "₱"
            "VND" -> "₫"
            else -> "Rp"
        }
    }

    val savedIcons = remember(profile.categoryIcons) {
        try { JSONObject(profile.categoryIcons) } catch (e: Exception) { JSONObject() }
    }

    val iconKey = savedIcons.optString(trans.category, "")
    val icon = kumaIconLibrary[iconKey] ?: when(trans.category) {
        "Financial" -> Icons.Default.AccountBalance
        "Food" -> Icons.Default.Restaurant
        "Shopping" -> Icons.Default.LocalMall
        "Health" -> Icons.Default.Favorite
        "Transport" -> Icons.Default.DirectionsCar
        "Education" -> Icons.Default.School
        "Entertainment" -> Icons.Default.Gamepad
        "Transfer" -> Icons.Default.SyncAlt
        else -> Icons.Default.DashboardCustomize
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(AppStr.delConf) },
            confirmButton = {
                KumaButton(
                    onClick = {
                        onDelete(obj)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppRed())
                ) { Text(AppStr.yes) }
            },
            dismissButton = {
                KumaTextButton(onClick = { showDeleteDialog = false }) {
                    Text(AppStr.no, color = AppText())
                }
            }
        )
    }

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.8f },
        confirmValueChange = { value ->
            if (isSelectionMode) return@rememberSwipeToDismissBoxState false
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showDeleteDialog = true
                    false
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEdit(obj)
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = !isSelectionMode,
        enableDismissFromEndToStart = !isSelectionMode,
        modifier = Modifier.clip(RoundedCornerShape(20.dp)),
        backgroundContent = {
            val color = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Color(0xFF1976D2).copy(alpha = 0.85f)
                SwipeToDismissBoxValue.EndToStart -> AppRed().copy(alpha = 0.85f)
                else -> Color.Transparent
            }
            val alignment = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            val iconSwipe = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Edit
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                else -> null
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 24.dp),
                contentAlignment = alignment
            ) {
                if (iconSwipe != null) {
                    Icon(iconSwipe, contentDescription = null, tint = Color.White)
                }
            }
        }
    ) {
        val baseCardColor = if (trans.category == "Transfer") Color(0xFF1976D2) else Color(0xFFD5641C)
        val finalCardColor = if (isSelected) AppPrimary().copy(alpha = 0.5f) else baseCardColor

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .glassCard(20.dp, if (isSelected) AppPrimary().copy(alpha = 0.2f) else AppSurface())
                .bouncyScale(interactionSource = interactionSource)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = androidx.compose.foundation.LocalIndication.current,
                    onClick = {
                        if (isSelectionMode) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleSelect()
                        } else {
                            onEdit(obj)
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (!isSelectionMode) {
                            onToggleSelect()
                        } else {
                            val duplicateTx = obj.copy(transaction = trans.copy(id = 0, name = "${trans.name} (Copy)"))
                            onEdit(duplicateTx)
                        }
                    }
                ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(if (isSelected) AppPrimary() else AppPrimary().copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .size(45.dp)
                            .background(AppPrimary().copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = AppPrimary(),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(trans.name, color = AppText(), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (trans.isEdited) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (AppStr.isId) "(Diedit)" else "(Edited)", color = AppText().copy(alpha = 0.4f), fontSize = 10.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }
                    }

                    if (trans.message.isNotEmpty()) {
                        Text(trans.message, color = AppText().copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp, bottom = 2.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    Text("${trans.wallet} • ${trans.category}", color = AppText().copy(alpha = 0.5f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                val formatted = remember(trans.amount) {
                    try {
                        NumberFormat.getInstance(Locale.forLanguageTag("id-ID")).format(trans.amount.toLong())
                    } catch (_: Exception) {
                        trans.amount
                    }
                }

                AutoSizeText(
                    text = "${if (trans.isIncome) "+ " else "- "} $curSym $formatted",
                    color = AppText(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.widthIn(max = 120.dp).padding(start = 8.dp).alpha(if (isPrivacyMode) 0f else 1f),
                    minimumFallbackSize = 10.sp
                )
            }
        }
    }
}

fun updateKumaWidget(context: Context) {
    val updateIntent = Intent(context, com.bearbones.kumaflow.KumaWidgetProvider::class.java).apply {
        action = "com.bearbones.kumaflow.UPDATE_WIDGET"
    }
    context.sendBroadcast(updateIntent)

    val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
    val streakWidgetIds = appWidgetManager.getAppWidgetIds(android.content.ComponentName(context, com.bearbones.kumaflow.KumaStreakWidgetProvider::class.java))
    val streakUpdateIntent = Intent(context, com.bearbones.kumaflow.KumaStreakWidgetProvider::class.java).apply {
        action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
        putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, streakWidgetIds)
    }
    context.sendBroadcast(streakUpdateIntent)
}

fun checkAndApplyPrideEasterEgg(
    context: android.content.Context, 
    userProfile: com.bearbones.kumaflow.UserProfile?,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner? = null
) {
    val pm = context.packageManager
    val pkg = context.packageName

    val normalIcon = android.content.ComponentName(context, "$pkg.MainActivityAliasNormal")
    val prideIcon = android.content.ComponentName(context, "$pkg.MainActivityAliasPride")
    val bearIcon = android.content.ComponentName(context, "$pkg.MainActivityAliasBear")
    val prideGlassIcon = android.content.ComponentName(context, "$pkg.MainActivityAliasPrideGlass")
    val bearGlassIcon = android.content.ComponentName(context, "$pkg.MainActivityAliasBearGlass")
    val kumaGlassIcon = android.content.ComponentName(context, "$pkg.MainActivityAliasKumaGlass")
    val orIcon = android.content.ComponentName(context, "$pkg.MainActivityAliasOR")
    val orGlassIcon = android.content.ComponentName(context, "$pkg.MainActivityAliasORGlass")
    val brutalIcon = android.content.ComponentName(context, "$pkg.MainActivityAliasBrutal")

    val allIcons = listOf(normalIcon, prideIcon, bearIcon, prideGlassIcon, bearGlassIcon, kumaGlassIcon, orIcon, orGlassIcon, brutalIcon)

    fun applyIconChanges(targetIcon: android.content.ComponentName) {
        // Enable the target icon FIRST to prevent the app from being killed before the new icon is registered
        if (pm.getComponentEnabledSetting(targetIcon) != android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            pm.setComponentEnabledSetting(
                targetIcon,
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                android.content.pm.PackageManager.DONT_KILL_APP
            )
        }

        // Then disable all other icons
        for (icon in allIcons) {
            if (icon != targetIcon && pm.getComponentEnabledSetting(icon) != android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                pm.setComponentEnabledSetting(
                    icon,
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP
                )
            }
        }
    }

    if (userProfile == null) {
        // Data is still loading from the database, do nothing yet!
        return
    }

    val isGlass = userProfile.isLiquidGlass
    val userName = userProfile.userName
    val isJune = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) == java.util.Calendar.JUNE
    val isOR = userName.contains("#OR", ignoreCase = true)
    val isBrutalEasterEgg = userName.contains("kumabrutal", ignoreCase = true) || userName.contains("#brutal", ignoreCase = true)

    val targetIcon = when {
        userName.isEmpty() -> normalIcon
        isBrutalEasterEgg -> brutalIcon
        isOR -> if (isGlass) orGlassIcon else orIcon
        (userName.contains("🌈") || userName.contains("#pride", ignoreCase = true)) -> if (isGlass) prideGlassIcon else prideIcon
        (userName.contains("🐻") || userName.contains("#bear", ignoreCase = true)) -> if (isGlass) bearGlassIcon else bearIcon
        isGlass -> kumaGlassIcon
        else -> normalIcon
    }

    if (pm.getComponentEnabledSetting(targetIcon) == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
        return
    }

    // Delay the execution to onStop using the provided lifecycleOwner (the Activity) to prevent immediate crash loops.
    if (lifecycleOwner != null) {
        val observer = object : androidx.lifecycle.LifecycleEventObserver {
            override fun onStateChanged(owner: androidx.lifecycle.LifecycleOwner, event: androidx.lifecycle.Lifecycle.Event) {
                if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                    try {
                        applyIconChanges(targetIcon)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    owner.lifecycle.removeObserver(this)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }
}

fun evaluateMathExpression(input: String): Long? {
    return try {
        val expression = input.replace("\\s".toRegex(), "")
        if (expression.isEmpty()) return null

        object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < expression.length) expression[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < expression.length) throw RuntimeException("Unexpected: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm()
                    else if (eat('-'.code)) x -= parseTerm()
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor()
                    else if (eat('/'.code)) x /= parseFactor()
                    else return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()
                var x: Double
                val startPos = pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) {
                    while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                    x = expression.substring(startPos, pos).toDouble()
                } else {
                    throw RuntimeException("Unexpected: " + ch.toChar())
                }
                return x
            }
        }.parse().toLong()
    } catch (e: Exception) {
        null
    }
}





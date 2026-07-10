@file:Suppress("SpellCheckingInspection", "UNUSED_PARAMETER", "unused", "CanBeVal", "DEPRECATION", "ScheduleExactAlarm")
@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bearbones.kumaflow

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import com.airbnb.lottie.LottieProperty
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.json.JSONObject
import java.text.NumberFormat
import kotlin.math.abs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import com.airbnb.lottie.compose.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import com.bearbones.kumaflow.ui.screens.StreakDetailsSheet
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.draw.shadow
import org.burnoutcrew.reorderable.*

// --- DATA CLASSES & OBJECTS ---
@Composable
fun HomeScreen(
    profile: UserProfile,
    transactionsWithSplits: List<TransactionWithSplits>,
    balance: Long,
    walletBalances: Map<String, Long>,
    income: Long,
    expenses: Long,
    selectedMonth: Int,
    selectedYear: Int,
    paddingValues: PaddingValues,
    onMonthChange: (Int, Int) -> Unit,
    onEdit: (TransactionWithSplits) -> Unit,
    onDelete: (TransactionWithSplits) -> Unit,
    onOpenWrapped: (Int, Int) -> Unit = { _, _ -> },
    listState: androidx.compose.foundation.lazy.LazyListState,
    selectedTxs: Set<Int>,
    onToggleSelect: (Int) -> Unit,
    clearSelection: () -> Unit,
    onBulkDelete: (List<TransactionWithSplits>) -> Unit,
    onBulkUpdateCategory: (List<TransactionWithSplits>, String) -> Unit,
    onUpdateProfile: (UserProfile) -> Unit,
    onReconcile: (String, Long) -> Unit = { _, _ -> },
    onAddTransaction: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val locale = java.util.Locale.forLanguageTag("id-ID")
    val curSym = when(profile.currency) { "USD", "AUD", "CAD", "SGD" -> "$"; "EUR" -> "€"; "GBP" -> "£"; "JPY", "CNY" -> "¥"; "CHF" -> "CHF"; else -> "Rp" }

    val haptic = LocalHapticFeedback.current

    var isPrivacyMode by rememberSaveable { mutableStateOf(false) }
    val blurRadius by androidx.compose.animation.core.animateDpAsState(targetValue = if (isPrivacyMode) 12.dp else 0.dp, label = "blur_anim")
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            focusManager.clearFocus()
        }
    }

    val todayFormatted = remember(profile.dateFormat, locale) {
        java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern(profile.dateFormat, locale))
    }

    var searchQuery by remember { mutableStateOf("") }
    val filteredTx = transactionsWithSplits.filter {
        it.transaction.date == todayFormatted && 
        (it.transaction.name.contains(searchQuery, ignoreCase = true) || 
         it.transaction.category.contains(searchQuery, ignoreCase = true) || 
         it.transaction.message.contains(searchQuery, ignoreCase = true))
    }

    val groupedTx = remember(filteredTx) { filteredTx.groupBy { it.transaction.date } }

    val sharedPrefs = remember { context.getSharedPreferences("kumaflow_prefs", android.content.Context.MODE_PRIVATE) }
    val cal = java.util.Calendar.getInstance()
    cal.add(java.util.Calendar.MONTH, -1)
    val prevMonth = cal.get(java.util.Calendar.MONTH) + 1
    val prevYear = cal.get(java.util.Calendar.YEAR)
    val wrappedKey = "$prevMonth-$prevYear"

    var showWrappedBanner by remember { mutableStateOf(sharedPrefs.getString("last_viewed_wrapped", "") != wrappedKey) }

    val isSelectionMode = selectedTxs.isNotEmpty()
    var showBulkCatDialog by remember { mutableStateOf(false) }
    var expandedDates by rememberSaveable { mutableStateOf(setOf<String>()) }
    var showReconcileDialog by remember { mutableStateOf(false) }
    var reconcileWalletName by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(top = 24.dp),
            contentPadding = PaddingValues(bottom = paddingValues.calculateBottomPadding() + 24.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    if (showWrappedBanner) {
                        val bannerGradient = androidx.compose.ui.graphics.Brush.linearGradient(colors = listOf(Color(0xFFE40303), Color(0xFF732982)))
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp).clickable { onOpenWrapped(prevMonth, prevYear) },
                            shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().background(bannerGradient).padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("KumaFlow Wrapped ✨", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val monthName = cal.getDisplayName(java.util.Calendar.MONTH, java.util.Calendar.LONG, locale) ?: "Bulan Lalu"
                                        Text(if (AppStr.isId) "Rapor keuanganmu di bulan $monthName udah siap! Yuk intip pengeluaranmu." else "Your financial report for $monthName is ready! Let's take a look at your expenses.", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, lineHeight = 16.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Icon(Icons.Default.ArrowForwardIos, contentDescription = "Buka Wrapped", tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    val displayName = profile.userName.replace("#pride", "", ignoreCase = true).replace("#bear", "", ignoreCase = true).replace("#OR", "", ignoreCase = true).trim()
                    val greeting = rememberSaveable {
                        val calNow = java.util.Calendar.getInstance()
                        val hour = calNow.get(java.util.Calendar.HOUR_OF_DAY)
                        val dayOfYear = calNow.get(java.util.Calendar.DAY_OF_YEAR)
                        
                        val periodName = when (hour) {
                            in 5..11 -> "Morning"
                            in 12..17 -> "Afternoon"
                            else -> "Night"
                        }
                        val periodKey = "$dayOfYear-$periodName"
                        val lastGreetingPeriod = sharedPrefs.getString("last_greeting_period", "")
                        
                        val isFirstTimeInPeriod = (lastGreetingPeriod != periodKey)
                        if (isFirstTimeInPeriod) {
                            sharedPrefs.edit().putString("last_greeting_period", periodKey).apply()
                        }
                        
                        val idList = listOf("Halo", "Hai", "Semangat terus", "Apa kabar", "Selamat datang kembali", "Yuk cek keuanganmu")
                        val enList = listOf("Hello", "Hi", "Keep it up", "How are you", "Welcome back", "Let's track your money")
                        
                        if (isFirstTimeInPeriod) {
                            when (periodName) {
                                "Morning" -> if (AppStr.isId) "Selamat pagi" else "Good morning"
                                "Afternoon" -> if (AppStr.isId) "Selamat siang" else "Good afternoon"
                                else -> if (AppStr.isId) "Selamat malam" else "Good evening"
                            }
                        } else {
                            val idx = (0 until idList.size).random()
                            if (AppStr.isId) idList[idx] else enList[idx]
                        }
                    }
                    var showStreakSheet by remember { mutableStateOf(false) }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("$greeting, $displayName!", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = AppText(), modifier = Modifier.weight(1f))
                        
                        // Lottie Fire Streak
                        val isStreakActiveToday = remember(profile.lastActiveDate) {
                            profile.lastActiveDate == LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                        }
                        
                        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(com.bearbones.kumaflow.R.raw.fire))
                        val progress by animateLottieCompositionAsState(composition = composition, iterations = LottieConstants.IterateForever, isPlaying = isStreakActiveToday, speed = 0.5f)
                        val dynamicProperties = rememberLottieDynamicProperties(
                            rememberLottieDynamicProperty(
                                property = LottieProperty.COLOR_FILTER,
                                value = if (isStreakActiveToday) null else ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) }),
                                keyPath = arrayOf("**")
                            )
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(AppSurfaceVariant())
                                .clickable { showStreakSheet = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("${profile.currentStreak}", fontWeight = FontWeight.Bold, color = AppText())
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(modifier = Modifier.size(24.dp)) {
                                LottieAnimation(composition = composition, progress = { if (isStreakActiveToday) progress else 0.5f }, modifier = Modifier.fillMaxSize(), dynamicProperties = dynamicProperties)
                            }
                        }
                    }
                    
                    if (showStreakSheet) {
                        val activeDates = remember(profile.lastActiveDate, profile.currentStreak) {
                            val dates = mutableListOf<LocalDate>()
                            if (profile.lastActiveDate.isNotEmpty()) {
                                try {
                                    val lastDate = LocalDate.parse(profile.lastActiveDate, DateTimeFormatter.ISO_LOCAL_DATE)
                                    for (i in 0 until profile.currentStreak) {
                                        dates.add(lastDate.minusDays(i.toLong()))
                                    }
                                } catch (e: Exception) {
                                    // ignore
                                }
                            }
                            dates
                        }
                        
                        ModalBottomSheet(
                            onDismissRequest = { showStreakSheet = false },
                            sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
                            containerColor = AppSurface(),
                            scrimColor = Color.Black.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                        ) {
                            StreakDetailsSheet(profile = profile, activeDates = activeDates, onDismiss = { showStreakSheet = false })
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    MonthYearSelector(selectedMonth, selectedYear, onMonthChange)
                    Spacer(modifier = Modifier.height(24.dp))

                    val isJune = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) == java.util.Calendar.JUNE
                    val isPrideThemeActive = isJune && (profile.themeMode == 3 || profile.themeMode == 4)
                    val prideGradient = androidx.compose.ui.graphics.Brush.linearGradient(colors = listOf(Color(0xFFE40303), Color(0xFFFF8C00), Color(0xFFFFED00), Color(0xFF008026), Color(0xFF24408E), Color(0xFF732982)))
                    val defaultSurfaceColor = AppSurfaceVariant()

                    // --- NEW DYNAMIC HEADER & FLOATING BALANCE ---
                    val animatedBal by androidx.compose.animation.core.animateFloatAsState(targetValue = abs(balance).toFloat(), animationSpec = androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.FastOutSlowInEasing), label = "bal")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(AppStr.curBal, color = AppText().copy(alpha = 0.8f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = if (isPrivacyMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Privacy",
                                    tint = AppText().copy(alpha = 0.8f),
                                    modifier = Modifier.clip(CircleShape).clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        isPrivacyMode = !isPrivacyMode
                                    }.padding(4.dp).size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val balPref = if (balance < 0) "- " else ""
                            AutoSizeText(
                                text = "$balPref$curSym ${NumberFormat.getInstance(locale).format(animatedBal.toLong())}", 
                                modifier = Modifier.alpha(if (isPrivacyMode) 0f else 1f), 
                                fontSize = 42.sp, 
                                fontWeight = FontWeight.Black, 
                                color = AppText(), 
                                minimumFallbackSize = 24.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- NEW FINANCIAL HUB CARD ---
                    Box(
                        modifier = if (isPrideThemeActive) {
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(prideGradient)
                        } else {
                            Modifier.fillMaxWidth().glassCard(32.dp, defaultSurfaceColor, useHaze = true)
                        }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
                            // Income & Expense Summary
                            val animatedInc by androidx.compose.animation.core.animateFloatAsState(targetValue = income.toFloat(), animationSpec = androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.FastOutSlowInEasing), label = "inc")
                            val animatedExp by androidx.compose.animation.core.animateFloatAsState(targetValue = expenses.toFloat(), animationSpec = androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.FastOutSlowInEasing), label = "exp")
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Total Income", color = if (isPrideThemeActive) Color.White.copy(alpha=0.8f) else AppText().copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ArrowUpward, null, tint = if(isPrideThemeActive) Color.White else AppGreen(), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        AutoSizeText(text = "$curSym ${NumberFormat.getInstance(locale).format(animatedInc.toLong())}", modifier = Modifier.alpha(if (isPrivacyMode) 0f else 1f), color = if(isPrideThemeActive) Color.White else AppText(), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, minimumFallbackSize = 10.sp)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Total Expenses", color = if (isPrideThemeActive) Color.White.copy(alpha=0.8f) else AppText().copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AutoSizeText(text = "$curSym ${NumberFormat.getInstance(locale).format(animatedExp.toLong())}", modifier = Modifier.alpha(if (isPrivacyMode) 0f else 1f), color = if(isPrideThemeActive) Color.White else AppText(), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, minimumFallbackSize = 10.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.ArrowDownward, null, tint = if(isPrideThemeActive) Color.White else AppRed(), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = if (isPrideThemeActive) Color.White.copy(alpha=0.2f) else AppText().copy(alpha = 0.1f))
                            Spacer(modifier = Modifier.height(20.dp))

                            // Wallet Row
                            val walletOrder = remember {
                                mutableStateListOf(*profile.wallets.split(",").filter { it.isNotBlank() }.toTypedArray())
                            }
                            val currentWalletSet = remember(profile.wallets) {
                                profile.wallets.split(",").filter { it.isNotBlank() }.toSet()
                            }
                            LaunchedEffect(currentWalletSet) {
                                val displayedSet = walletOrder.toSet()
                                if (displayedSet != currentWalletSet) {
                                    walletOrder.clear()
                                    walletOrder.addAll(profile.wallets.split(",").filter { it.isNotBlank() })
                                }
                            }

                            val reorderState = rememberReorderableLazyListState(
                                onMove = { from, to -> walletOrder.apply { add(to.index, removeAt(from.index)) } },
                                onDragEnd = { _, _ -> onUpdateProfile(profile.copy(wallets = walletOrder.joinToString(","))) }
                            )

                            LazyRow(
                                state = reorderState.listState,
                                modifier = Modifier.fillMaxWidth().reorderable(reorderState),
                                contentPadding = PaddingValues(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(walletOrder, { it }) { walletName ->
                                    ReorderableItem(reorderState, key = walletName) { isDragging ->
                                        val cardScale by androidx.compose.animation.core.animateFloatAsState(if (isDragging) 1.05f else 1f, label = "drag_scale")
                                        val amt = walletBalances[walletName] ?: 0L
                                        val wBalPref = if (amt < 0) "- " else ""
                                        Column(
                                            modifier = Modifier
                                                .zIndex(if (isDragging) 1f else 0f)
                                                .scale(cardScale)
                                                .detectReorderAfterLongPress(reorderState)
                                                .clickable { 
                                                    reconcileWalletName = walletName
                                                    showReconcileDialog = true
                                                }
                                                .background(if (isPrideThemeActive) Color.White.copy(alpha=0.15f) else AppBg().copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            val logoBitmap = com.bearbones.kumaflow.rememberWalletLogo(context = context, walletName = walletName)
                                            if (logoBitmap != null) {
                                                androidx.compose.foundation.Image(
                                                    bitmap = logoBitmap,
                                                    contentDescription = walletName,
                                                    modifier = Modifier.size(42.dp).clip(CircleShape).background(Color.White, CircleShape),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier.size(42.dp).background(if (isPrideThemeActive) Color.White.copy(alpha=0.3f) else AppPrimary().copy(alpha = 0.15f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(walletName.take(1).uppercase(), color = if (isPrideThemeActive) Color.White else AppPrimary(), fontWeight = FontWeight.Black, fontSize = 20.sp)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(walletName, color = if (isPrideThemeActive) Color.White.copy(alpha = 0.9f) else AppText().copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("$wBalPref$curSym ${NumberFormat.getInstance(locale).format(abs(amt))}", color = if (isPrideThemeActive) Color.White else AppText(), fontSize = 13.sp, fontWeight = FontWeight.Black, modifier = Modifier.alpha(if (isPrivacyMode) 0f else 1f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // --- CONTRIBUTION HEATMAP ---
                    com.bearbones.kumaflow.ui.components.ContributionHeatmap(
                        allTransactions = transactionsWithSplits,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    OutlinedTextField(
                        value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text(AppStr.searchTx) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = AppText().copy(alpha = 0.5f)) },
                        trailingIcon = { if (searchQuery.isNotEmpty()) { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null, tint = AppText()) } } },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).glassCard(16.dp, AppSurfaceVariant(), useHaze = true), shape = RoundedCornerShape(16.dp), singleLine = true,
                        colors = getGlassTextFieldColors()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if(AppStr.isId) "Transaksi Hari Ini" else "Today's Transactions", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = AppText())
                        if (isSelectionMode) {
                            TextButton(onClick = { clearSelection() }) {
                                Text(AppStr.cancelBulk, color = AppRed(), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (filteredTx.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().height(250.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val composition by com.airbnb.lottie.compose.rememberLottieComposition(com.airbnb.lottie.compose.LottieCompositionSpec.RawRes(R.raw.beruang_kosong))
                        val progress by com.airbnb.lottie.compose.animateLottieCompositionAsState(composition = composition, iterations = com.airbnb.lottie.compose.LottieConstants.IterateForever)
                        com.airbnb.lottie.compose.LottieAnimation(composition = composition, progress = { progress }, modifier = Modifier.size(150.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(AppStr.noTx, textAlign = TextAlign.Center, color = AppText().copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                groupedTx.forEach { (date, txs) ->
                    stickyHeader(key = "home_header_$date") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(androidx.compose.ui.graphics.Color.Transparent)
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AppPrimary()))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = date,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AppText().copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    item(key = "home_txgroup_$date") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .glassCard(24.dp, AppSurfaceVariant(), useHaze = true)
                                .padding(vertical = 8.dp)
                        ) {
                            val isExpanded = expandedDates.contains(date)
                            val displayTxs = if (isExpanded || txs.size <= 1) txs else txs.take(1)

                            displayTxs.forEachIndexed { index, item ->
                                val isSelected = selectedTxs.contains(item.transaction.id)
                                TransactionItem(
                                    profile = profile,
                                    obj = item,
                                    isPrivacyMode = isPrivacyMode,
                                    isSelected = isSelected,
                                    isSelectionMode = isSelectionMode,
                                    onToggleSelect = { onToggleSelect(item.transaction.id) },
                                    onEdit = onEdit,
                                    onDelete = onDelete
                                )
                                if (index < displayTxs.size - 1) {
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = AppText().copy(alpha = 0.1f))
                                }
                            }
                            
                            if (!isExpanded && txs.size > 1) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = AppText().copy(alpha = 0.1f))
                                TextButton(
                                    onClick = { expandedDates = expandedDates + date },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                ) {
                                    Text("${txs.size - 1} More", color = AppPrimary(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            } else if (isExpanded && txs.size > 1) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = AppText().copy(alpha = 0.1f))
                                TextButton(
                                    onClick = { expandedDates = expandedDates - date },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                ) {
                                    Text("Show Less", color = AppPrimary(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(if (isSelectionMode) 180.dp else 100.dp)) }
        }

        // ðŸ”¥ BULK ACTION OVERLAY BAR ðŸ”¥
        androidx.compose.animation.AnimatedVisibility(
            visible = isSelectionMode,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = paddingValues.calculateBottomPadding() + 8.dp),
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .border(1.dp, AppText().copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                    .glassCard(24.dp, AppSurfaceVariant())
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            showBulkCatDialog = true
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    ) {
                        Icon(Icons.Default.DashboardCustomize, contentDescription = null, tint = AppPrimary())
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(AppStr.changeCat, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AppText())
                    }

                    Text("${selectedTxs.size} ${AppStr.selected}", fontWeight = FontWeight.Black, fontSize = 14.sp, color = AppText())

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val txsToDelete = transactionsWithSplits.filter { selectedTxs.contains(it.transaction.id) }
                            onBulkDelete(txsToDelete)
                            clearSelection()
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = AppRed())
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(AppStr.bulkDel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AppRed())
                    }
                }
            }
        }

        if (showBulkCatDialog) {
            val allCats = (profile.expenseCats.split(",") + profile.incomeCats.split(",")).filter { it.isNotBlank() }.distinct()
            AlertDialog(
                onDismissRequest = { showBulkCatDialog = false },
                title = { Text(AppStr.chooseNewCat, fontWeight = FontWeight.Bold) },
                text = {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(allCats) { catName ->
                            Text(
                                text = catName,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val txsToUpdate = transactionsWithSplits.filter { selectedTxs.contains(it.transaction.id) }
                                        onBulkUpdateCategory(txsToUpdate, catName)
                                        clearSelection()
                                        showBulkCatDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                color = AppText(),
                                fontWeight = FontWeight.Bold
                            )
                            HorizontalDivider(color = AppText().copy(alpha = 0.1f))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showBulkCatDialog = false }) { Text(AppStr.close, color = AppRed()) }
                },
                containerColor = AppSurface()
            )
        }
        
        if (showReconcileDialog) {
            val systemBalance = walletBalances[reconcileWalletName] ?: 0L
            var realBalanceInput by remember { mutableStateOf("") }
            
            ModalBottomSheet(
                onDismissRequest = { showReconcileDialog = false },
                containerColor = AppSurface(),
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if(AppStr.isId) "Sesuaikan Saldo $reconcileWalletName" else "Adjust $reconcileWalletName Balance",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = AppText()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if(AppStr.isId) "Saldo saat ini di KumaFlow: $curSym ${NumberFormat.getInstance(locale).format(systemBalance)}" else "Current system balance: $curSym ${NumberFormat.getInstance(locale).format(systemBalance)}",
                        fontSize = 14.sp,
                        color = AppText().copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    OutlinedTextField(
                        value = realBalanceInput,
                        onValueChange = { 
                            val digitsOnly = it.filter { char -> char.isDigit() }
                            realBalanceInput = digitsOnly
                        },
                        label = { Text(if(AppStr.isId) "Saldo Sebenarnya" else "Actual Balance") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(16.dp, AppSurfaceVariant(), useHaze = true),
                        shape = RoundedCornerShape(16.dp),
                        colors = getGlassTextFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        visualTransformation = ThousandSeparatorTransformation()
                    )
                    
                    val actualBal = realBalanceInput.toLongOrNull() ?: systemBalance
                    val delta = actualBal - systemBalance
                    
                    if (realBalanceInput.isNotEmpty() && delta != 0L) {
                        Spacer(modifier = Modifier.height(16.dp))
                        val isIncome = delta > 0
                        val deltaColor = if (isIncome) AppGreen() else AppRed()
                        val sign = if (isIncome) "+" else "-"
                        
                        Text(
                            text = if(AppStr.isId) "Selisih: $sign $curSym ${NumberFormat.getInstance(locale).format(abs(delta))} (${if(isIncome) "Pemasukan" else "Pengeluaran"})" else "Delta: $sign $curSym ${NumberFormat.getInstance(locale).format(abs(delta))} (${if(isIncome) "Income" else "Expense"})",
                            color = deltaColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = {
                            if (delta != 0L) {
                                onReconcile(reconcileWalletName, delta)
                            }
                            showReconcileDialog = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = AppPrimary(), spotColor = AppPrimary()),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppPrimary()),
                        enabled = realBalanceInput.isNotEmpty() && delta != 0L
                    ) {
                        Text(
                            text = if(AppStr.isId) "Sesuaikan Otomatis" else "Auto-Adjust Balance",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}




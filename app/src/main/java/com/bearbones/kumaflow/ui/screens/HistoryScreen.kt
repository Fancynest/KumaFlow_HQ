package com.bearbones.kumaflow.ui.screens
import com.bearbones.kumaflow.utils.kumaClickable

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.bearbones.kumaflow.neobrutalism
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumaflow.*
import com.bearbones.kumaflow.TransactionWithSplits
import com.bearbones.kumaflow.UserProfile
import com.bearbones.kumaflow.R
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import org.json.JSONObject
import com.bearbones.kumaflow.ui.components.KumaOutlinedButton
import com.bearbones.kumaflow.ui.components.KumaTextButton
import com.bearbones.kumaflow.utils.bouncySheetContent

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    profile: UserProfile,
    allTransactions: List<TransactionWithSplits>,
    dao: com.bearbones.kumaflow.TransactionDao,
    paddingValues: PaddingValues,
    onEdit: (TransactionWithSplits) -> Unit,
    onDelete: (TransactionWithSplits) -> Unit,
    onToggleSelect: (Int) -> Unit,
    selectedTxs: Set<Int>,
    isSelectionMode: Boolean,
    onBulkDelete: (List<TransactionWithSplits>) -> Unit,
    clearSelection: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val locale = Locale.forLanguageTag("id-ID")
    val isId = AppStr.isId

    fun getCatColor(catName: String): Color {
        val predefined = mapOf(
            "Financial" to Color(0xFF4CAF50),
            "Food" to Color(0xFFFF9800),
            "Shopping" to Color(0xFFE91E63),
            "Health" to Color(0xFFF44336),
            "Transport" to Color(0xFF2196F3),
            "Education" to Color(0xFF9C27B0),
            "Entertainment" to Color(0xFF673AB7),
            "Transfer" to Color(0xFF00BCD4),
            "Others" to Color(0xFF607D8B)
        )
        return predefined[catName] ?: Color(android.graphics.Color.HSVToColor(floatArrayOf(kotlin.math.abs(catName.hashCode()) % 360f, 0.7f, 0.8f)))
    }

    // FILTER STATES
    var selectedDateFilter by rememberSaveable { mutableStateOf("All time") }
    var customStartDate by rememberSaveable { mutableStateOf<Long?>(null) }
    var customEndDate by rememberSaveable { mutableStateOf<Long?>(null) }

    var selectedCategories by rememberSaveable { mutableStateOf(setOf<String>()) }
    var selectedWallets by rememberSaveable { mutableStateOf(setOf<String>()) }

    var expandedDates by rememberSaveable { mutableStateOf(setOf<String>()) }

    // BOTTOM SHEET STATES
    val dateSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showDateSheet by remember { mutableStateOf(false) }

    val catSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showCatSheet by remember { mutableStateOf(false) }

    val walletSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showWalletSheet by remember { mutableStateOf(false) }

    var showM3DatePicker by remember { mutableStateOf(false) }

    // SEARCH STATE
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<TransactionWithSplits>>(emptyList()) }
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            dao.searchTransactions("$searchQuery*").collect { searchResults = it }
        } else {
            searchResults = emptyList()
        }
    }
    
    val baseTxList = if (searchQuery.isNotBlank()) searchResults else allTransactions

    // DERIVED FILTERED TRANSACTIONS
    val filteredTx by remember(baseTxList, selectedDateFilter, customStartDate, customEndDate, selectedCategories, selectedWallets) {
        derivedStateOf {
            baseTxList.filter { txObj ->
                val tx = txObj.transaction
                var match = true

                // Filter Category
                if (selectedCategories.isNotEmpty() && !selectedCategories.contains(tx.category)) match = false

                // Filter Wallet
                if (match && selectedWallets.isNotEmpty() && !selectedWallets.contains(tx.wallet)) match = false

                // Filter Date
                if (match && selectedDateFilter != "All time") {
                    try {
                        val txDate = LocalDateTime.parse(tx.timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        val now = LocalDateTime.now()
                        when (selectedDateFilter) {
                            "Last 7 days" -> {
                                if (ChronoUnit.DAYS.between(txDate, now) > 7) match = false
                            }
                            "Last 30 days" -> {
                                if (ChronoUnit.DAYS.between(txDate, now) > 30) match = false
                            }
                            "Last 90 days" -> {
                                if (ChronoUnit.DAYS.between(txDate, now) > 90) match = false
                            }
                            "Custom date" -> {
                                val txMillis = txDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                if (customStartDate != null && txMillis < customStartDate!!) match = false
                                // Add 1 day to end date to make it inclusive
                                if (customEndDate != null && txMillis > customEndDate!! + 86400000L) match = false
                            }
                        }
                    } catch (e: Exception) {
                        // ignore parse err
                    }
                }
                match
            }.sortedByDescending { 
                try {
                    LocalDateTime.parse(it.transaction.timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                } catch (e: Exception) { LocalDateTime.MIN }
            }
        }
    }

    val groupedTx by remember(filteredTx) {
        derivedStateOf { 
            filteredTx.groupBy { txObj -> 
                try {
                    val dt = LocalDateTime.parse(txObj.transaction.timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    val formatter = DateTimeFormatter.ofPattern(if(isId) "EEEE, dd MMM yyyy" else "EEEE, MMM dd, yyyy", locale)
                    dt.format(formatter)
                } catch (e: Exception) {
                    txObj.transaction.date
                }
            } 
        }
    }

    // MAIN UI
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 800.dp)
                .fillMaxWidth()
        ) {
            // Header
            Spacer(modifier = Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 24.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = com.bearbones.kumaflow.AppStr.hist,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppText()
                )
                
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // SEARCH BAR
            com.bearbones.kumaflow.ui.components.KumaOutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(AppStr.searchTx) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                singleLine = true,
                colors = getGlassTextFieldColors(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AppText().copy(alpha = 0.5f)) },
                trailingIcon = { if (searchQuery.isNotEmpty()) { com.bearbones.kumaflow.ui.components.KumaIconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null, tint = AppText()) } } }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // DATE CHIP
                val dateActive = selectedDateFilter != "All time"
                FilterChipCustom(
                    modifier = Modifier.weight(1f),
                    text = if(dateActive) selectedDateFilter else if (isId) "Tanggal" else "Date",
                    isActive = dateActive,
                    onClick = { showDateSheet = true }
                )
                // CATEGORY CHIP
                val catActive = selectedCategories.isNotEmpty()
                FilterChipCustom(
                    modifier = Modifier.weight(1f),
                    text = if(catActive) "${selectedCategories.size} selected" else if (isId) "Kategori" else "Category",
                    isActive = catActive,
                    onClick = { showCatSheet = true }
                )
                // WALLET CHIP
                val walletActive = selectedWallets.isNotEmpty()
                FilterChipCustom(
                    modifier = Modifier.weight(1f),
                    text = if(walletActive) "${selectedWallets.size} selected" else if (isId) "Dompet" else "Wallet",
                    isActive = walletActive,
                    onClick = { showWalletSheet = true }
                )
                
                // PDF EXPORT
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (com.bearbones.kumaflow.ui.theme.LocalIsBrutal.current) Modifier.neobrutalism(isBrutal = true, backgroundColor = AppSurface(), cornerRadius = 16.dp, borderWidth = 2.dp, offset = 2.dp)
                            else Modifier.glassCard(16.dp, AppSurface())
                        )
                        .kumaClickable { com.bearbones.kumaflow.generatePDF(context, filteredTx.map { it.transaction }, profile, 0, 0) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppText(), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.PictureAsPdf, contentDescription = AppStr.expPdf, tint = AppText(), modifier = Modifier.size(14.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // LIST
            if (filteredTx.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val composition by com.airbnb.lottie.compose.rememberLottieComposition(com.airbnb.lottie.compose.LottieCompositionSpec.RawRes(R.raw.beruang_kosong))
                    val progress by com.airbnb.lottie.compose.animateLottieCompositionAsState(composition = composition, iterations = com.airbnb.lottie.compose.LottieConstants.IterateForever)
                    com.airbnb.lottie.compose.LottieAnimation(composition = composition, progress = { progress }, modifier = Modifier.size(150.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(AppStr.noTx, textAlign = TextAlign.Center, color = AppText().copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = paddingValues.calculateBottomPadding() + 100.dp)
                ) {
                    groupedTx.forEach { (date, txs) ->
                        stickyHeader(key = "header_$date") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Transparent)
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

                        item(key = "txgroup_$date") {
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
                                        isPrivacyMode = false,
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
                                    KumaTextButton(
                                        onClick = { expandedDates = expandedDates + date },
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                    ) {
                                        Text("${txs.size - 1} More", color = AppPrimary(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                } else if (isExpanded && txs.size > 1) {
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = AppText().copy(alpha = 0.1f))
                                    KumaTextButton(
                                        onClick = { expandedDates = expandedDates - date },
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                    ) {
                                        Text("Show Less", color = AppPrimary(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(if (isSelectionMode) 120.dp else 100.dp)) }
                }
            }
        }
        
        // 🔥 BULK ACTION OVERLAY BAR 🔥
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
                    Text("${selectedTxs.size} ${AppStr.selected}", fontWeight = FontWeight.Black, fontSize = 14.sp, color = AppText())

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.kumaClickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val txsToDelete = allTransactions.filter { selectedTxs.contains(it.transaction.id) }
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

        // DATE SHEET
        if (showDateSheet) {
            ModalBottomSheet(
                onDismissRequest = { showDateSheet = false },
                sheetState = dateSheetState,
                containerColor = AppSurface()
            ) {
                Column(modifier = Modifier.bouncySheetContent().padding(24.dp).fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if(isId) "Pilih tanggal transaksi" else "Choose transaction date", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = AppText())
                        KumaTextButton(onClick = { selectedDateFilter = "All time"; customStartDate = null; customEndDate = null; showDateSheet = false }) {
                            Text(if(isId) "Hapus" else "Clear", color = AppPrimary())
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    val dateOptions = listOf("Last 7 days", "Last 30 days", "Last 90 days", "Custom date", "All time")
                    dateOptions.forEach { opt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .kumaClickable { 
                                    if (opt == "Custom date") {
                                        showM3DatePicker = true
                                        showDateSheet = false
                                    } else {
                                        selectedDateFilter = opt 
                                        showDateSheet = false
                                    }
                                }
                                .padding(vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(opt, fontWeight = FontWeight.Bold, color = AppText())
                            if (selectedDateFilter == opt) {
                                Icon(Icons.Default.CheckCircle, null, tint = AppPrimary())
                            } else {
                                Icon(Icons.Default.RadioButtonUnchecked, null, tint = AppText().copy(alpha = 0.5f))
                            }
                        }
                        HorizontalDivider(color = AppText().copy(alpha = 0.1f))
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // CATEGORY SHEET
        if (showCatSheet) {
            ModalBottomSheet(
                onDismissRequest = { showCatSheet = false },
                sheetState = catSheetState,
                containerColor = AppSurface()
            ) {
                val allCats = (profile.expenseCats.split(",") + profile.incomeCats.split(",")).filter { it.isNotBlank() }.distinct()
                val savedIcons = remember(profile.categoryIcons) { try { JSONObject(profile.categoryIcons) } catch (e: Exception) { JSONObject() } }
                
                Column(modifier = Modifier.bouncySheetContent().padding(24.dp).fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if(isId) "Filter kategori" else "Filter category", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = AppText())
                        KumaTextButton(onClick = { selectedCategories = emptySet() }) {
                            Text(if(isId) "Hapus" else "Clear", color = AppPrimary())
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.heightIn(max = 400.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(allCats) { cat ->
                            val isSelected = selectedCategories.contains(cat)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .glassCard(16.dp, if (isSelected) AppPrimary().copy(alpha = 0.2f) else AppSurface())
                                    .kumaClickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        val newSet = selectedCategories.toMutableSet()
                                        if (isSelected) newSet.remove(cat) else newSet.add(cat)
                                        selectedCategories = newSet
                                    }
                                    .padding(8.dp)
                            ) {
                                val catCol = getCatColor(cat)
                                val iconKey = savedIcons.optString(cat, "")
                                val icon = kumaIconLibrary[iconKey] ?: when(cat) {
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
                                
                                Box(modifier = Modifier.size(40.dp).background(catCol.copy(alpha=0.2f), CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(icon, null, tint = catCol)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(cat, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AppText(), textAlign = TextAlign.Center)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    com.bearbones.kumaflow.ui.components.KumaButton(
                        onClick = { showCatSheet = false },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppPrimary(), contentColor = MaterialTheme.colorScheme.onPrimary),
                        shape = RoundedCornerShape(16.dp),
                        brutalCornerRadius = 16.dp
                    ) {
                        Text(if(AppStr.isId) "Atur Filter" else "Set filter", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // WALLET SHEET
        if (showWalletSheet) {
            ModalBottomSheet(
                onDismissRequest = { showWalletSheet = false },
                sheetState = walletSheetState,
                containerColor = AppSurface()
            ) {
                val allWallets = profile.wallets.split(",").filter { it.isNotBlank() }
                Column(modifier = Modifier.bouncySheetContent().padding(24.dp).fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if(isId) "Filter dompet" else "Filter by methods", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = AppText())
                        KumaTextButton(onClick = { selectedWallets = emptySet() }) {
                            Text(if(isId) "Hapus" else "Clear", color = AppPrimary())
                        }
                    }
                    Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    allWallets.forEach { w ->
                        val isSelected = selectedWallets.contains(w)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .kumaClickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val newSet = selectedWallets.toMutableSet()
                                    if (isSelected) newSet.remove(w) else newSet.add(w)
                                    selectedWallets = newSet
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val logoBitmap = com.bearbones.kumaflow.rememberWalletLogo(context = context, walletName = w)
                                if (logoBitmap != null) {
                                    androidx.compose.foundation.Image(
                                        bitmap = logoBitmap,
                                        contentDescription = w,
                                        modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White, CircleShape),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                    )
                                } else {
                                    Box(modifier = Modifier.size(32.dp).background(AppPrimary().copy(alpha=0.2f), CircleShape), contentAlignment = Alignment.Center) {
                                        Text(w.take(1).uppercase(), color = AppPrimary(), fontWeight = FontWeight.Black)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(w, fontWeight = FontWeight.Bold, color = AppText())
                            }
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = AppPrimary(), uncheckedColor = AppText().copy(alpha=0.5f))
                            )
                        }
                        HorizontalDivider(color = AppText().copy(alpha = 0.1f))
                    }
                    } // end scrollable wallet list Column
                    Spacer(modifier = Modifier.height(24.dp))
                    com.bearbones.kumaflow.ui.components.KumaButton(
                        onClick = { showWalletSheet = false },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppPrimary(), contentColor = MaterialTheme.colorScheme.onPrimary),
                        shape = RoundedCornerShape(16.dp),
                        brutalCornerRadius = 16.dp
                    ) {
                        Text(if(AppStr.isId) "Atur Filter" else "Set filter", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // M3 DATE RANGE PICKER
        if (showM3DatePicker) {
            val dateRangePickerState = rememberDateRangePickerState()
            
            DatePickerDialog(
                onDismissRequest = { showM3DatePicker = false },
                confirmButton = {
                    KumaTextButton(onClick = {
                        customStartDate = dateRangePickerState.selectedStartDateMillis
                        customEndDate = dateRangePickerState.selectedEndDateMillis
                        selectedDateFilter = "Custom date"
                        showM3DatePicker = false
                    }) {
                        Text("OK", color = AppPrimary(), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    KumaTextButton(onClick = { showM3DatePicker = false }) {
                        Text(AppStr.no, color = AppText())
                    }
                },
                colors = DatePickerDefaults.colors(
                    containerColor = AppBg()
                )
            ) {
                DateRangePicker(
                    state = dateRangePickerState,
                    modifier = Modifier.fillMaxHeight(0.8f).padding(16.dp),
                    title = { Text(if(AppStr.isId) "Pilih Rentang Tanggal" else "Select Date Range", modifier = Modifier.padding(16.dp)) },
                    headline = { 
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(dateRangePickerState.selectedStartDateMillis?.let { 
                                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().toString()
                            } ?: "Start Date")
                            Text(" - ")
                            Text(dateRangePickerState.selectedEndDateMillis?.let { 
                                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().toString()
                            } ?: "End Date")
                        }
                    },
                    showModeToggle = false,
                    colors = DatePickerDefaults.colors(
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
                        disabledSelectedDayContentColor = Color.White.copy(alpha=0.5f),
                        selectedDayContainerColor = AppPrimary(),
                        disabledSelectedDayContainerColor = AppPrimary().copy(alpha=0.5f),
                        todayContentColor = AppPrimary(),
                        todayDateBorderColor = AppPrimary(),
                        dayInSelectionRangeContentColor = AppText(),
                        dayInSelectionRangeContainerColor = AppPrimary().copy(alpha=0.2f)
                    )
                )
            }
        }
    }
}

@Composable
fun FilterChipCustom(text: String, isActive: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .then(
                if (com.bearbones.kumaflow.ui.theme.LocalIsBrutal.current) Modifier.neobrutalism(isBrutal = true, backgroundColor = if (isActive) AppPrimary().copy(alpha = 0.2f) else AppSurface(), cornerRadius = 16.dp, borderWidth = 2.dp, offset = 2.dp)
                else Modifier.glassCard(16.dp, if (isActive) AppPrimary().copy(alpha = 0.2f) else AppSurface())
            )
            .clip(RoundedCornerShape(16.dp))
            .kumaClickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isActive) AppPrimary() else AppText(), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        Spacer(modifier = Modifier.width(4.dp))
        Icon(Icons.Default.KeyboardArrowDown, null, tint = if (isActive) AppPrimary() else AppText(), modifier = Modifier.size(14.dp))
    }
}


package com.bearbones.kumaflow.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumaflow.TransactionDao
import com.bearbones.kumaflow.UserProfile
import com.bearbones.kumaflow.AppPrimary
import com.bearbones.kumaflow.AppSurface
import com.bearbones.kumaflow.AppText
import com.bearbones.kumaflow.AppStr
import com.bearbones.kumaflow.TransactionWithSplits
import com.bearbones.kumaflow.utils.CsvExportUtil
import kotlinx.coroutines.launch
import org.json.JSONObject
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import com.bearbones.kumaflow.KumaTransaction
import com.bearbones.kumaflow.glassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsMoreSheet(
    goalName: String,
    profile: UserProfile,
    dao: TransactionDao,
    onDismiss: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCloseConfirm: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppSurface()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Lainnya",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AppText()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // History
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDismiss(); onOpenHistory() }
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Receipt, contentDescription = null, tint = AppPrimary())
                Spacer(modifier = Modifier.width(16.dp))
                Text("Lihat Riwayat Transaksi", color = AppText(), fontSize = 16.sp, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = AppText().copy(alpha = 0.5f))
            }
            Divider(color = AppText().copy(alpha = 0.1f))

            // Settings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDismiss(); onOpenSettings() }
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Settings, contentDescription = null, tint = AppPrimary())
                Spacer(modifier = Modifier.width(16.dp))
                Text("Pengaturan Tabungan", color = AppText(), fontSize = 16.sp, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = AppText().copy(alpha = 0.5f))
            }
            Divider(color = AppText().copy(alpha = 0.1f))

            // Close Savings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDismiss(); onOpenCloseConfirm() }
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.HeartBroken, contentDescription = null, tint = Color.Red)
                Spacer(modifier = Modifier.width(16.dp))
                Text("Tutup Tabungan", color = Color.Red, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = AppText().copy(alpha = 0.5f))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsHistorySheet(
    goalName: String,
    profile: UserProfile,
    dao: TransactionDao,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var allTxs by remember { mutableStateOf<List<TransactionWithSplits>>(emptyList()) }
    var displayedTxs by remember { mutableStateOf<List<TransactionWithSplits>>(emptyList()) }
    
    var searchQuery by remember { mutableStateOf("") }
    
    val currentDate = LocalDate.now()
    var selectedMonth by remember { mutableStateOf(currentDate.monthValue) }
    var selectedYear by remember { mutableStateOf(currentDate.year) }
    
    val months = listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
    var showMonthPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        dao.getAllTransactionsWithSplits().collect { txs ->
            // Filter by this wallet
            allTxs = txs.filter { it.transaction.wallet == goalName }
        }
    }
    
    LaunchedEffect(allTxs, searchQuery, selectedMonth, selectedYear) {
        val filtered = allTxs.filter { txw ->
            val txDate = LocalDate.parse(txw.transaction.date)
            val matchesMonth = txDate.monthValue == selectedMonth && txDate.year == selectedYear
            val matchesSearch = txw.transaction.name.contains(searchQuery, ignoreCase = true)
            matchesMonth && matchesSearch
        }
        displayedTxs = filtered.sortedByDescending { it.transaction.date }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppSurface()
    ) {
        Column(modifier = Modifier.fillMaxHeight(0.9f).fillMaxWidth().padding(horizontal = 24.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Riwayat Transaksi", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = AppText())
                var showDownloadMenu by remember { mutableStateOf(false) }
                
                Box {
                    IconButton(onClick = { showDownloadMenu = true }) {
                        Icon(Icons.Filled.Download, contentDescription = "Unduh Riwayat", tint = AppPrimary())
                    }
                    DropdownMenu(
                        expanded = showDownloadMenu,
                        onDismissRequest = { showDownloadMenu = false },
                        modifier = Modifier.background(AppSurface())
                    ) {
                        DropdownMenuItem(
                            text = { Text("Unduh sebagai CSV", color = AppText()) },
                            onClick = {
                                showDownloadMenu = false
                                scope.launch {
                                    CsvExportUtil.exportTransactionsToCsv(context, goalName, displayedTxs)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Unduh sebagai PDF", color = AppText()) },
                            onClick = {
                                showDownloadMenu = false
                                scope.launch {
                                    CsvExportUtil.exportTransactionsToPdf(context, goalName, displayedTxs)
                                }
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cari...", color = AppText().copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = AppText().copy(alpha = 0.5f)) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppPrimary(),
                    unfocusedBorderColor = AppText().copy(alpha = 0.2f),
                    focusedTextColor = AppText(),
                    unfocusedTextColor = AppText(),
                    cursorColor = AppPrimary()
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Month Picker Chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppPrimary().copy(alpha = 0.1f))
                    .clickable { showMonthPicker = true }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("${months[selectedMonth - 1]} $selectedYear", color = AppPrimary(), fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // List
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(displayedTxs) { txw ->
                    val tx = txw.transaction
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp, end = 6.dp) // prevent shadow clip
                            .glassCard(12.dp, AppSurface())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(if (tx.isIncome) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color(0xFFF44336).copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(if (tx.isIncome) Icons.Filled.SouthWest else Icons.Filled.NorthEast, contentDescription = null, tint = if (tx.isIncome) Color(0xFF4CAF50) else Color(0xFFF44336))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tx.name, fontWeight = FontWeight.Bold, color = AppText(), fontSize = 16.sp)
                                Text(tx.date, color = AppText().copy(alpha = 0.5f), fontSize = 12.sp)
                            }
                            val prefix = if (tx.isIncome) "+" else "-"
                            val color = if (tx.isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
                            Text("$prefix Rp ${NumberFormat.getInstance(Locale.getDefault()).format(tx.amount.toLongOrNull() ?: 0L)}", color = color, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                if (displayedTxs.isEmpty()) {
                    item {
                        Text(
                            "Tidak ada transaksi bulan ini",
                            color = AppText().copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
    
    if (showMonthPicker) {
        AlertDialog(
            onDismissRequest = { showMonthPicker = false },
            title = { Text("Pilih Bulan", color = AppText()) },
            containerColor = AppSurface(),
            text = {
                // Simple list of last 6 months for simplicity
                LazyColumn {
                    items((0..11).toList()) { i ->
                        val d = currentDate.minusMonths(i.toLong())
                        val text = "${months[d.monthValue - 1]} ${d.year}"
                        Text(
                            text = text,
                            color = AppText(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedMonth = d.monthValue
                                    selectedYear = d.year
                                    showMonthPicker = false
                                }
                                .padding(16.dp)
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsSettingsSheet(
    goalName: String,
    profile: UserProfile,
    dao: TransactionDao,
    onDismiss: () -> Unit,
    onNameChanged: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var editName by remember { mutableStateOf(goalName) }
    
    val json = remember { 
        try { JSONObject(profile.savingsGoals) } catch (e: Exception) { JSONObject() }
    }
    
    // Parse old format (Long) or new format (JSONObject)
    val (targetAmountRaw, initialFreq, initialAmount, initialAdEnabled) = remember {
        val obj = json.opt(goalName)
        if (obj is JSONObject) {
            val target = obj.optLong("target", 0L)
            val freq = obj.optString("autoDebitFreq", "Harian")
            val amount = obj.optLong("autoDebitAmount", 0L)
            val enabled = obj.has("autoDebitFreq")
            listOf(target, freq, amount, enabled)
        } else if (obj is Number) {
            listOf(obj.toLong(), "Harian", 0L, false)
        } else {
            listOf(0L, "Harian", 0L, false)
        }
    }
    
    var goalEnabled by remember { mutableStateOf((targetAmountRaw as Long) > 0L) }
    var editTargetStr by remember { mutableStateOf(if ((targetAmountRaw as Long) > 0L) targetAmountRaw.toString() else "") }
    
    var autoDebitEnabled by remember { mutableStateOf(initialAdEnabled as Boolean) }
    var autoDebitFreq by remember { mutableStateOf(initialFreq as String) }
    var autoDebitAmountStr by remember { mutableStateOf(if ((initialAmount as Long) > 0L) initialAmount.toString() else "") }
    
    var showFreqSheet by remember { mutableStateOf(false) }

    val amountFormatted = remember(editTargetStr) {
        val digits = editTargetStr.filter { it.isDigit() }
        val long = digits.toLongOrNull() ?: 0L
        if (long > 0) NumberFormat.getInstance(Locale.getDefault()).format(long) else ""
    }
    
    val adAmountFormatted = remember(autoDebitAmountStr) {
        val digits = autoDebitAmountStr.filter { it.isDigit() }
        val long = digits.toLongOrNull() ?: 0L
        if (long > 0) NumberFormat.getInstance(Locale.getDefault()).format(long) else ""
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppSurface()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text("Pengaturan Tabungan", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = AppText())
            Spacer(modifier = Modifier.height(24.dp))
            
            // Edit Name
            OutlinedTextField(
                value = editName,
                onValueChange = { editName = it },
                label = { Text("Nama Tabungan") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppPrimary(),
                    unfocusedBorderColor = AppText().copy(alpha = 0.3f),
                    focusedTextColor = AppText(),
                    unfocusedTextColor = AppText()
                )
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Goal
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Atur Goal", fontWeight = FontWeight.Bold, color = AppText())
                    Text("Mimpi kamu akan terwujud ketika mencapai goal", fontSize = 12.sp, color = AppText().copy(alpha = 0.6f))
                }
                Switch(
                    checked = goalEnabled,
                    onCheckedChange = { goalEnabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AppPrimary())
                )
            }
            if (goalEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amountFormatted,
                    onValueChange = { editTargetStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Target Nominal") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    prefix = { Text("Rp ", color = AppText().copy(alpha = 0.6f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppPrimary(),
                        unfocusedBorderColor = AppText().copy(alpha = 0.3f),
                        focusedTextColor = AppText(),
                        unfocusedTextColor = AppText()
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Autodebit
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Autodebit", fontWeight = FontWeight.Bold, color = AppText())
                    Text("Tentukan waktu penarikan otomatis dari Rekening Utama", fontSize = 12.sp, color = AppText().copy(alpha = 0.6f))
                }
                Switch(
                    checked = autoDebitEnabled,
                    onCheckedChange = { autoDebitEnabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AppPrimary())
                )
            }
            
            if (autoDebitEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Frekuensi
                OutlinedTextField(
                    value = autoDebitFreq,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Frekuensi") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showFreqSheet = true },
                    trailingIcon = {
                        Icon(androidx.compose.material.icons.Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = AppText())
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = AppText().copy(alpha = 0.3f),
                        disabledTextColor = AppText(),
                        disabledLabelColor = AppText().copy(alpha = 0.7f),
                        disabledTrailingIconColor = AppText()
                    ),
                    enabled = false // to make it clickable cleanly
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Amount to debit
                OutlinedTextField(
                    value = adAmountFormatted,
                    onValueChange = { autoDebitAmountStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Jumlah yang didebit") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    prefix = { Text("Rp ", color = AppText().copy(alpha = 0.6f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppPrimary(),
                        unfocusedBorderColor = AppText().copy(alpha = 0.3f),
                        focusedTextColor = AppText(),
                        unfocusedTextColor = AppText()
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    scope.launch {
                        // 1. Rename in wallets list if changed
                        var newWalletsList = profile.savingsWallets
                        var finalName = goalName
                        if (editName.isNotBlank() && editName != goalName) {
                            val list = profile.savingsWallets.split(",").filter { it.isNotBlank() }.toMutableList()
                            val idx = list.indexOf(goalName)
                            if (idx != -1) {
                                list[idx] = editName
                                newWalletsList = list.joinToString(",")
                                finalName = editName
                                
                                // Also update all transactions
                                dao.updateTransactionsWalletName(goalName, finalName)
                                dao.updateSplitsWalletName(goalName, finalName)
                            }
                        }
                        
                        // 2. Update Goals JSON
                        val newJson = JSONObject(profile.savingsGoals)
                        if (editName.isNotBlank() && editName != goalName) {
                            newJson.remove(goalName)
                        }
                        val newTarget = if (goalEnabled) editTargetStr.filter { it.isDigit() }.toLongOrNull() ?: 0L else 0L
                        
                        // Create a nested JSON for goal details including autodebit
                        val goalDetails = JSONObject()
                        goalDetails.put("target", newTarget)
                        if (autoDebitEnabled) {
                            goalDetails.put("autoDebitFreq", autoDebitFreq)
                            goalDetails.put("autoDebitAmount", autoDebitAmountStr.toLongOrNull() ?: 0L)
                        }
                        newJson.put(finalName, goalDetails)
                        
                        // Save profile
                        dao.saveProfile(profile.copy(
                            savingsWallets = newWalletsList,
                            savingsGoals = newJson.toString()
                        ))
                        
                        onNameChanged(finalName)
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppPrimary())
            ) {
                Text("Simpan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
    
    if (showFreqSheet) {
        SavingsAutodebitFreqSheet(
            initialFreq = autoDebitFreq,
            onDismiss = { showFreqSheet = false },
            onSelect = { freq ->
                autoDebitFreq = freq
                showFreqSheet = false
            },
            onDelete = {
                autoDebitEnabled = false
                showFreqSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsAutodebitFreqSheet(
    initialFreq: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableStateOf(if (initialFreq.startsWith("Mingguan")) "Mingguan" else if (initialFreq.startsWith("Bulanan")) "Bulanan" else "Harian") }
    
    var selectedDay by remember { mutableStateOf(if (initialFreq.startsWith("Mingguan")) initialFreq.removePrefix("Mingguan - ") else "Senin") }
    var selectedDate by remember { mutableStateOf(if (initialFreq.startsWith("Bulanan")) initialFreq.removePrefix("Bulanan - ") else "1") }
    
    val days = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppSurface()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text("Kapan waktu autodebit tabungan kamu?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppText())
            Spacer(modifier = Modifier.height(24.dp))
            
            // Segmented Control
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("Harian", "Mingguan", "Bulanan").forEach { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) AppPrimary().copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { selectedTab = tab }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            color = if (isSelected) AppPrimary() else AppText().copy(alpha = 0.5f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (selectedTab == "Harian") {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Lightbulb, contentDescription = null, tint = AppPrimary())
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Kamu akan diautodebit setiap hari", color = AppText().copy(alpha = 0.7f), fontSize = 14.sp)
                }
            } else if (selectedTab == "Mingguan") {
                Text("Setiap hari apa?", fontWeight = FontWeight.Bold, color = AppText())
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(days) { day ->
                        val isSelected = selectedDay == day
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) AppPrimary().copy(alpha = 0.1f) else Color.Transparent)
                                .clickable { selectedDay = day }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(day, color = if (isSelected) AppPrimary() else AppText(), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            } else if (selectedTab == "Bulanan") {
                Text("Setiap tanggal berapa?", fontWeight = FontWeight.Bold, color = AppText())
                Spacer(modifier = Modifier.height(16.dp))
                
                // Grid of dates 1-31
                val dates = (1..31).toList()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(dates) { d ->
                        val dateStr = d.toString()
                        val isSelected = selectedDate == dateStr
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) AppPrimary() else Color.Transparent)
                                .clickable { selectedDate = dateStr },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dateStr,
                                color = if (isSelected) Color.White else AppText(),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    val finalFreq = when (selectedTab) {
                        "Harian" -> "Harian"
                        "Mingguan" -> "Mingguan - $selectedDay"
                        "Bulanan" -> "Bulanan - $selectedDate"
                        else -> "Harian"
                    }
                    onSelect(finalFreq)
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppPrimary())
            ) {
                Text("Pilih", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Hapus", color = AppText())
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SavingsCloseDialog(
    goalName: String,
    profile: UserProfile,
    walletBalances: Map<String, Long>,
    dao: TransactionDao,
    onDismiss: () -> Unit,
    onClosed: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val balance = walletBalances[goalName] ?: 0L
    val curSym = if (profile.currency == "IDR") "Rp" else profile.currency
    val balanceStr = NumberFormat.getInstance(Locale.getDefault()).format(balance)
    
    val mainWallets = profile.wallets.split(",").filter { it.isNotBlank() }
    val defaultMainWallet = mainWallets.firstOrNull() ?: "Wallet"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface(),
        title = { Text("Tutup", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 24.sp) },
        text = {
            Column {
                Text("Yakin mau tutup tabungan kamu?", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppText())
                Spacer(modifier = Modifier.height(8.dp))
                if (balance > 0) {
                    Text("Dana sebesar $curSym $balanceStr akan masuk ke $defaultMainWallet kamu.", color = AppText().copy(alpha = 0.7f))
                } else {
                    Text("Tabungan ini akan dihapus secara permanen.", color = AppText().copy(alpha = 0.7f))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        if (balance > 0) {
                            val dt = LocalDateTime.now()
                            val dateStr = dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                            val timeStr = dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            
                            val txOut = KumaTransaction(id = 0, name = "Penutupan Tabungan $goalName", date = dateStr, amount = balance.toString(), isIncome = false, category = "Transfer", wallet = goalName, timestamp = timeStr)
                            val txIn = KumaTransaction(id = 0, name = "Penutupan Tabungan $goalName", date = dateStr, amount = balance.toString(), isIncome = true, category = "Transfer", wallet = defaultMainWallet, timestamp = timeStr)
                            
                            dao.insertFullTransaction(txOut, emptyList())
                            dao.insertFullTransaction(txIn, emptyList())
                        }
                        
                        // Remove from profile
                        val newWallets = profile.savingsWallets.split(",").filter { it.isNotBlank() && it != goalName }.joinToString(",")
                        val json = try { JSONObject(profile.savingsGoals) } catch (e: Exception) { JSONObject() }
                        json.remove(goalName)
                        
                        dao.saveProfile(profile.copy(
                            savingsWallets = newWallets,
                            savingsGoals = json.toString()
                        ))
                        com.bearbones.kumaflow.updateKumaWidget(context)
                        onClosed()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppPrimary())
            ) {
                Text("Yakin", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Batal", color = AppText())
            }
        }
    )
}

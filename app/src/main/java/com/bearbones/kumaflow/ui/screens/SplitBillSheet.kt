@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.bearbones.kumaflow.ui.screens

import com.bearbones.kumaflow.utils.kumaClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import com.bearbones.kumaflow.ui.components.KumaExpressiveIcon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.bearbones.kumaflow.AppPrimary
import com.bearbones.kumaflow.AppStr
import com.bearbones.kumaflow.AppSurface
import com.bearbones.kumaflow.AppSurfaceVariant
import com.bearbones.kumaflow.AppText
import com.bearbones.kumaflow.AppGreen
import com.bearbones.kumaflow.getGlassTextFieldColors
import com.bearbones.kumaflow.glassCard
import com.bearbones.kumaflow.utils.bouncySheetContent
import com.bearbones.kumaflow.utils.ShareSplitBillUtils
import com.bearbones.kumaflow.KumaTransaction
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.bearbones.kumaflow.ui.components.KumaIconButton
import com.bearbones.kumaflow.ui.components.KumaButton
import com.bearbones.kumaflow.ui.components.KumaTextButton
import com.bearbones.kumaflow.ui.components.KumaOutlinedTextField

@Composable
fun SplitBillSheet(
    viewModel: SplitBillViewModel,
    qrisFilePath: String,
    holderName: String,
    bankName: String,
    bankAccount: String,
    physicalWallets: List<String> = emptyList(),
    expenseCategories: List<String> = emptyList(),
    onSaveExpense: ((KumaTransaction) -> Unit)? = null,
    onDismissRequest: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val format = NumberFormat.getNumberInstance(Locale.getDefault())

    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var previewAmountStr by remember { mutableStateOf("") }

    // Record expense dialog state
    var showRecordExpenseDialog by remember { mutableStateOf(false) }
    var selectedRecordAmount by remember { mutableLongStateOf(0L) }
    var recordTxName by remember { mutableStateOf("Split Bill") }
    var selectedWallet by remember { mutableStateOf(physicalWallets.firstOrNull() ?: "Cash") }
    var selectedCategory by remember { 
        mutableStateOf(
            expenseCategories.firstOrNull { it.contains("Makan", ignoreCase = true) || it.contains("Food", ignoreCase = true) }
                ?: expenseCategories.firstOrNull()
                ?: "Makanan & Minuman"
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = AppSurface(),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ) {
        Column(
            modifier = Modifier
                .bouncySheetContent()
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .verticalScroll(rememberScrollState())
        ) {
            // Header with Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = AppPrimary(),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = AppStr.splitBillCalc,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppText()
                )
            }

            // Mode Toggle (Bagi Rata vs Sesuai Pesanan)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppSurfaceVariant())
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .kumaClickable { viewModel.setMode(SplitMode.SAMA_RATA) }
                        .background(if (state.mode == SplitMode.SAMA_RATA) AppPrimary() else Color.Transparent)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        AppStr.modeEqual,
                        color = if (state.mode == SplitMode.SAMA_RATA) Color.White else AppText(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .kumaClickable { viewModel.setMode(SplitMode.TAHU_DIRI) }
                        .background(if (state.mode == SplitMode.TAHU_DIRI) AppPrimary() else Color.Transparent)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        AppStr.modeCustom,
                        color = if (state.mode == SplitMode.TAHU_DIRI) Color.White else AppText(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TAX & SERVICE PRESET CHIPS
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${AppStr.taxPct} / Service",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppText().copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val taxPresets = listOf("0", "10", "11", "15")
                    taxPresets.forEach { preset ->
                        val isSelected = state.taxPercentage == preset
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) AppPrimary() else AppSurfaceVariant())
                                .clickable { viewModel.setTaxPercentage(preset) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$preset%",
                                color = if (isSelected) Color.White else AppText(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Custom Tax Input
                    KumaOutlinedTextField(
                        value = state.taxPercentage,
                        onValueChange = { viewModel.setTaxPercentage(it) },
                        placeholder = { Text("Custom %", fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = getGlassTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // MODE 1: SAMA RATA (EQUAL SPLIT)
            if (state.mode == SplitMode.SAMA_RATA) {
                // Total Bill Input
                KumaOutlinedTextField(
                    value = state.totalBillStr,
                    onValueChange = { viewModel.setTotalBillStr(it) },
                    label = { Text(AppStr.totalBill) },
                    placeholder = { Text("Contoh: 150000") },
                    prefix = { Text("Rp ", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = getGlassTextFieldColors(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Number of People with Counter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        AppStr.totalPeople,
                        fontWeight = FontWeight.SemiBold,
                        color = AppText(),
                        fontSize = 14.sp
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KumaIconButton(
                            onClick = {
                                val cur = state.numberOfPeople.toIntOrNull() ?: 2
                                if (cur > 1) viewModel.setNumberOfPeople((cur - 1).toString())
                            }
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = AppPrimary())
                        }

                        KumaOutlinedTextField(
                            value = state.numberOfPeople,
                            onValueChange = { viewModel.setNumberOfPeople(it) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = getGlassTextFieldColors(),
                            singleLine = true,
                            modifier = Modifier.width(72.dp)
                        )

                        KumaIconButton(
                            onClick = {
                                val cur = state.numberOfPeople.toIntOrNull() ?: 2
                                viewModel.setNumberOfPeople((cur + 1).toString())
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase", tint = AppPrimary())
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Result Card for Equal Split
                val finalAmount = viewModel.calculateEqualSplit()
                val peopleCount = state.numberOfPeople.toIntOrNull() ?: 1
                val cleanBill = state.totalBillStr.replace("[^0-9]".toRegex(), "").toDoubleOrNull() ?: 0.0
                val taxPct = state.taxPercentage.toDoubleOrNull() ?: 0.0
                val grandTotal = (cleanBill * (1.0 + (taxPct / 100.0))).toLong()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard(16.dp, AppPrimary().copy(alpha = 0.12f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(AppStr.eachPays, fontSize = 13.sp, color = AppText().copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Rp ${format.format(finalAmount)}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AppPrimary()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Total Rp ${format.format(grandTotal)} (${peopleCount} ${AppStr.splitBillOrg})",
                            fontSize = 12.sp,
                            color = AppText().copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Share QRIS Button
                    KumaButton(
                        onClick = {
                            val uri = ShareSplitBillUtils.generateQRWithText(
                                context, qrisFilePath, holderName, "Rp ${format.format(finalAmount)}", finalAmount
                            )
                            previewUri = uri
                            previewAmountStr = format.format(finalAmount)
                            showPreviewDialog = true
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppPrimary())
                    ) {
                        KumaExpressiveIcon(Icons.Default.Share, contentDescription = null, containerColor = Color.Transparent, size = 18.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(AppStr.previewQris, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Record to Expenses Button (Centang)
                    if (onSaveExpense != null) {
                        KumaButton(
                            onClick = {
                                selectedRecordAmount = finalAmount
                                showRecordExpenseDialog = true
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppGreen())
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(AppStr.recordExpenseBtn, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // MODE 2: TAHU DIRI (ITEMIZED PER PERSON)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        AppStr.customItems,
                        fontWeight = FontWeight.Bold,
                        color = AppText(),
                        fontSize = 16.sp
                    )

                    // Add Person Button
                    KumaButton(
                        onClick = { viewModel.addPerson() },
                        colors = ButtonDefaults.buttonColors(containerColor = AppPrimary().copy(alpha = 0.2f), contentColor = AppPrimary()),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(AppStr.addPersonBtn, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // List of Person Cards with (+) Multi-Nominal input
                state.customItems.forEachIndexed { pIdx, person ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .glassCard(12.dp, AppSurfaceVariant())
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Badge for User / Anda
                            if (pIdx == 0) {
                                Row(
                                    modifier = Modifier.padding(bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(AppGreen().copy(alpha = 0.18f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = if (AppStr.isId) "👤 Anda (Pengguna)" else "👤 You (User)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AppGreen()
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (AppStr.isId) "• Dicatat ke Expenses" else "• Recorded to Expenses",
                                        fontSize = 11.sp,
                                        color = AppText().copy(alpha = 0.6f)
                                    )
                                }
                            }

                            // Person Header (Name + Subtotal + Delete)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                KumaOutlinedTextField(
                                    value = person.name,
                                    onValueChange = { viewModel.updatePersonName(person.id, it) },
                                    label = { Text(if (pIdx == 0) (if (AppStr.isId) "Nama (Anda)" else "Name (You)") else "Nama") },
                                    placeholder = { Text(if (pIdx == 0) "Saya" else "Teman $pIdx", fontSize = 13.sp) },
                                    colors = getGlassTextFieldColors(),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // Subtotal Badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AppPrimary().copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        "Rp ${format.format(person.totalPrice)}",
                                        color = AppPrimary(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                if (state.customItems.size > 1) {
                                    KumaIconButton(
                                        onClick = { viewModel.removePerson(person.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Hapus Orang",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Nominal Rows for this Person
                            person.amounts.forEachIndexed { aIdx, subItem ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    KumaOutlinedTextField(
                                        value = subItem.amountStr,
                                        onValueChange = { viewModel.updatePersonAmount(person.id, subItem.id, it) },
                                        label = { Text("Nominal ${aIdx + 1}") },
                                        placeholder = { Text("0") },
                                        prefix = { Text("Rp ", fontWeight = FontWeight.Bold) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = getGlassTextFieldColors(),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (person.amounts.size > 1) {
                                        KumaIconButton(
                                            onClick = { viewModel.removePersonAmount(person.id, subItem.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Hapus Nominal",
                                                tint = AppText().copy(alpha = 0.5f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // (+) Button to add another nominal to this person
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .kumaClickable { viewModel.addAmountToPerson(person.id) }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Tambah Nominal",
                                        tint = AppPrimary(),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        AppStr.addNominalBtn,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AppPrimary()
                                    )
                                }

                                val taxPctVal = state.taxPercentage.toDoubleOrNull() ?: 0.0
                                if (taxPctVal > 0.0) {
                                    val withTax = person.totalPrice * (1.0 + (taxPctVal / 100.0))
                                    Text(
                                        "+ Pajak: Rp ${format.format(withTax.toLong())}",
                                        fontSize = 11.sp,
                                        color = AppText().copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Breakdown & Summary Card
                val result = viewModel.calculateTahuDiriSplit()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard(14.dp, AppSurfaceVariant())
                        .padding(14.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            AppStr.resultAfterTax,
                            fontWeight = FontWeight.Bold,
                            color = AppText(),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        result.itemizedShares.forEach { share ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(share.name, fontWeight = FontWeight.SemiBold, color = AppText(), fontSize = 13.sp)
                                    if (share.taxAmount > 0L) {
                                        Text("Subtotal: Rp ${format.format(share.subtotal)}", fontSize = 11.sp, color = AppText().copy(alpha = 0.6f))
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Rp ${format.format(share.finalAmount)}",
                                        fontWeight = FontWeight.Bold,
                                        color = AppPrimary(),
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    KumaIconButton(
                                        onClick = {
                                            val uri = ShareSplitBillUtils.generateQRWithText(
                                                context, qrisFilePath, holderName, "Rp ${format.format(share.finalAmount)}", share.finalAmount
                                            )
                                            previewUri = uri
                                            previewAmountStr = format.format(share.finalAmount)
                                            showPreviewDialog = true
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        KumaExpressiveIcon(Icons.Default.Share, contentDescription = "Share", size = 18.dp, tint = AppPrimary(), containerColor = Color.Transparent)
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Keseluruhan", fontWeight = FontWeight.Bold, color = AppText(), fontSize = 14.sp)
                            Text(
                                "Rp ${format.format(result.grandTotal)}",
                                fontWeight = FontWeight.ExtraBold,
                                color = AppPrimary(),
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Record to Expenses Button (Centang)
                if (onSaveExpense != null) {
                    KumaButton(
                        onClick = {
                            val myShare = result.itemizedShares.firstOrNull()?.finalAmount ?: result.grandTotal
                            selectedRecordAmount = myShare
                            showRecordExpenseDialog = true
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppGreen())
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(AppStr.recordExpenseBtn, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // DIALOG 1: CATAT KE PENGELUARAN (RECORD EXPENSE)
    if (showRecordExpenseDialog) {
        val equalAmount = viewModel.calculateEqualSplit()
        val tahuDiriResult = viewModel.calculateTahuDiriSplit()
        val myAmount = if (state.mode == SplitMode.SAMA_RATA) equalAmount else (tahuDiriResult.itemizedShares.firstOrNull()?.finalAmount ?: 0L)
        val fullAmount = if (state.mode == SplitMode.SAMA_RATA) {
            val clean = state.totalBillStr.replace("[^0-9]".toRegex(), "").toDoubleOrNull() ?: 0.0
            val taxPct = state.taxPercentage.toDoubleOrNull() ?: 0.0
            (clean * (1.0 + (taxPct / 100.0))).toLong()
        } else {
            tahuDiriResult.grandTotal
        }

        AlertDialog(
            onDismissRequest = { showRecordExpenseDialog = false },
            containerColor = AppSurface(),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = AppGreen(), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(AppStr.recordSplitExpenseTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppText())
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "Pilih nominal yang ingin dicatat:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppText().copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val personalShareLabel = if (state.mode == SplitMode.SAMA_RATA) {
                        if (AppStr.isId) "Bagian per Orang" else "Per Person Share"
                    } else {
                        val pName = tahuDiriResult.itemizedShares.firstOrNull()?.name?.ifBlank { "Saya" } ?: "Saya"
                        if (AppStr.isId) "Bagian $pName (Anda)" else "$pName's Share (You)"
                    }

                    // Option: Bagian Saya / Person 1
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedRecordAmount == myAmount) AppPrimary().copy(alpha = 0.15f) else AppSurfaceVariant())
                            .clickable { selectedRecordAmount = myAmount }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(personalShareLabel, color = AppText(), fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        Text("Rp ${format.format(myAmount)}", fontWeight = FontWeight.Bold, color = AppPrimary())
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Option: Total Tagihan Keseluruhan
                    if (fullAmount != myAmount && fullAmount > 0L) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedRecordAmount == fullAmount) AppPrimary().copy(alpha = 0.15f) else AppSurfaceVariant())
                            .clickable { selectedRecordAmount = fullAmount }
                            .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(AppStr.totalBillRecorded, color = AppText(), fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text("Rp ${format.format(fullAmount)}", fontWeight = FontWeight.Bold, color = AppPrimary())
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Transaction Name
                    KumaOutlinedTextField(
                        value = recordTxName,
                        onValueChange = { recordTxName = it },
                        label = { Text(AppStr.txNameLabel) },
                        singleLine = true,
                        colors = getGlassTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Wallet Selection
                    if (physicalWallets.isNotEmpty()) {
                        Text(AppStr.selectWallet, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppText().copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(physicalWallets) { w ->
                                val isSel = selectedWallet == w
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) AppPrimary() else AppSurfaceVariant())
                                        .clickable { selectedWallet = w }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(w, color = if (isSel) Color.White else AppText(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Category Selection
                    if (expenseCategories.isNotEmpty()) {
                        Text(AppStr.selectCategory, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppText().copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(expenseCategories) { c ->
                                val isSel = selectedCategory == c
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) AppPrimary() else AppSurfaceVariant())
                                        .clickable { selectedCategory = c }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(c, color = if (isSel) Color.White else AppText(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                KumaButton(
                    onClick = {
                        if (selectedRecordAmount > 0L) {
                            val dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID")))
                            val timeStr = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            val newTx = KumaTransaction(
                                name = recordTxName.ifBlank { "Split Bill" },
                                date = dateStr,
                                amount = selectedRecordAmount.toString(),
                                isIncome = false,
                                category = selectedCategory,
                                wallet = selectedWallet,
                                timestamp = timeStr,
                                message = "Split Bill"
                            )
                            onSaveExpense?.invoke(newTx)
                            showRecordExpenseDialog = false
                            onDismissRequest()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppGreen())
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppStr.recordExpenseBtn, color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                KumaTextButton(onClick = { showRecordExpenseDialog = false }) {
                    Text(AppStr.cancelBtn, color = AppText())
                }
            }
        )
    }

    // DIALOG 2: QRIS PREVIEW & WHATSAPP SHARE
    if (showPreviewDialog) {
        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            modifier = Modifier.glassCard(24.dp, AppSurface()),
            containerColor = if (com.bearbones.kumaflow.LocalIsLiquidGlass.current) Color.Transparent else AppSurface(),
            title = { Text(AppStr.previewQris, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val bitmap = remember(previewUri) {
                        previewUri?.let { uri ->
                            try {
                                context.contentResolver.openInputStream(uri)?.use { stream ->
                                    android.graphics.BitmapFactory.decodeStream(stream)?.asImageBitmap()
                                }
                            } catch (e: Exception) { null }
                        }
                    }
                    if (bitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = bitmap,
                            contentDescription = "QRIS Preview",
                            modifier = Modifier.fillMaxWidth().aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
                        )
                    } else {
                        Text(AppStr.loadQrisFailed, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                KumaButton(
                    onClick = {
                        ShareSplitBillUtils.shareBillingDetails(context, previewUri, previewAmountStr, holderName, bankName, bankAccount)
                        showPreviewDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppPrimary())
                ) {
                    KumaExpressiveIcon(Icons.Default.Share, contentDescription = null, containerColor = Color.Transparent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(AppStr.shareWa)
                }
            },
            dismissButton = {
                KumaTextButton(onClick = { showPreviewDialog = false }) { Text(AppStr.close) }
            }
        )
    }
}

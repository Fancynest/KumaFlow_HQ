package com.bearbones.kumaflow.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumaflow.TransactionDao
import com.bearbones.kumaflow.UserProfile
import com.bearbones.kumaflow.AppBg
import com.bearbones.kumaflow.AppPrimary
import com.bearbones.kumaflow.AppSurface
import com.bearbones.kumaflow.AppText
import com.bearbones.kumaflow.AppStr
import com.bearbones.kumaflow.utils.kumaClickable
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*
import org.json.JSONObject
import com.bearbones.kumaflow.glassCard
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border

@Composable
fun SavingsScreen(
    profile: UserProfile,
    dao: TransactionDao,
    paddingValues: PaddingValues,
    walletBalances: Map<String, Long>,
    onAddTransaction: () -> Unit,
    forceUpdateTrigger: Int
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val locale = Locale.getDefault()
    val isId = AppStr.isId

    val sharedPref = remember { context.getSharedPreferences("kumaflow_prefs", Context.MODE_PRIVATE) }
    var isPrivacyMode by remember { mutableStateOf(sharedPref.getBoolean("privacy_mode", false)) }

    fun formatHide(value: Long): String {
        val formatted = NumberFormat.getInstance(locale).format(value)
        return if (isPrivacyMode) formatted.replace(Regex("\\d"), "*") else formatted
    }

    val savingsWallets = remember(profile.savingsWallets, forceUpdateTrigger) {
        profile.savingsWallets.split(",").filter { it.isNotBlank() }
    }

    val savingsGoalsMap = remember(profile.savingsGoals, forceUpdateTrigger) {
        try {
            val json = JSONObject(profile.savingsGoals)
            val map = mutableMapOf<String, Long>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = json.opt(key)
                if (value is JSONObject) {
                    map[key] = value.optLong("target", 0L)
                } else if (value is Number) {
                    map[key] = value.toLong()
                } else {
                    map[key] = 0L
                }
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    val totalSavings = remember(savingsWallets, walletBalances, forceUpdateTrigger) {
        walletBalances.filterKeys { it in savingsWallets }.values.sum()
    }

    var showAddGoalSheet by remember { mutableStateOf(false) }
    var selectedGoalForAction by remember { mutableStateOf<String?>(null) }

    val curSym = if (profile.currency == "IDR") "Rp" else profile.currency

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = paddingValues.calculateTopPadding() + 24.dp,
                start = 24.dp,
                end = 24.dp,
                bottom = paddingValues.calculateBottomPadding() + 100.dp
            )
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(16.dp, AppSurface())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = AppPrimary(), modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kuma Savings",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = AppText()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isId) "Total Dana Terkumpul" else "Total Savings",
                            fontSize = 14.sp,
                            color = AppText().copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (isPrivacyMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle Privacy",
                            tint = AppText().copy(alpha = 0.8f),
                            modifier = Modifier
                                .clip(CircleShape)
                                .kumaClickable {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    isPrivacyMode = !isPrivacyMode
                                    sharedPref.edit().putBoolean("privacy_mode", isPrivacyMode).apply()
                                }
                                .padding(4.dp)
                                .size(20.dp)
                        )
                    }
                    Text(
                        text = "$curSym ${formatHide(totalSavings)}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppText()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Title and Add Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isId) "Tabungan Kamu" else "Your Savings",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppText()
            )
            com.bearbones.kumaflow.ui.components.KumaIconButton(
                onClick = { showAddGoalSheet = true },
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(AppPrimary().copy(alpha = 0.1f))
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Goal", tint = AppPrimary())
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 64.dp)
        ) {
            items(savingsWallets) { walletName ->
                val currentBalance = walletBalances[walletName] ?: 0L
                val targetAmount = savingsGoalsMap[walletName] ?: 0L
                val progress = if (targetAmount > 0) (currentBalance.toFloat() / targetAmount.toFloat()).coerceIn(0f, 1f) else 0f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp, end = 6.dp) // Prevent shadow clipping
                        .glassCard(16.dp, AppSurface())
                        .clickable { selectedGoalForAction = walletName }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = walletName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppText())
                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "$curSym ${formatHide(currentBalance)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = AppPrimary()
                        )

                        if (targetAmount > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${(progress * 100).toInt()}%", fontSize = 12.sp, color = AppText().copy(alpha = 0.6f))
                                Text("Goal: $curSym ${NumberFormat.getInstance(locale).format(targetAmount)}", fontSize = 12.sp, color = AppText().copy(alpha = 0.6f))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            SavingsProgressBar(progress = progress, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            if (savingsWallets.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val composition by com.airbnb.lottie.compose.rememberLottieComposition(com.airbnb.lottie.compose.LottieCompositionSpec.RawRes(com.bearbones.kumaflow.R.raw.beruang_kosong))
                        val progress by com.airbnb.lottie.compose.animateLottieCompositionAsState(composition = composition, iterations = com.airbnb.lottie.compose.LottieConstants.IterateForever)
                        com.airbnb.lottie.compose.LottieAnimation(composition = composition, progress = { progress }, modifier = Modifier.size(150.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isId) "Belum ada tabungan. Yuk mulai nabung!" else "No savings yet. Start saving now!",
                            color = AppText().copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    if (showAddGoalSheet) {
        AddSavingsGoalSheet(
            profile = profile,
            dao = dao,
            onDismiss = { showAddGoalSheet = false }
        )
    }

    selectedGoalForAction?.let { goalName ->
        val targetAmount = savingsGoalsMap[goalName] ?: 0L
        SavingsDetailSheet(
            goalName = goalName,
            currentBalance = walletBalances[goalName] ?: 0L,
            targetAmount = targetAmount,
            profile = profile,
            walletBalances = walletBalances,
            dao = dao,
            onDismiss = { selectedGoalForAction = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsDetailSheet(
    goalName: String,
    currentBalance: Long,
    targetAmount: Long,
    profile: UserProfile,
    walletBalances: Map<String, Long>,
    dao: TransactionDao,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isId = AppStr.isId
    val locale = Locale.getDefault()
    val curSym = if (profile.currency == "IDR") "Rp" else profile.currency
    val progress = if (targetAmount > 0) (currentBalance.toFloat() / targetAmount.toFloat()).coerceIn(0f, 1f) else 0f

    var showActionSheet by remember { mutableStateOf(false) }
    var actionIsAdding by remember { mutableStateOf(true) }

    val autoDebitInfo = remember(profile.savingsGoals, goalName) {
        try {
            val json = JSONObject(profile.savingsGoals)
            val obj = json.opt(goalName)
            if (obj is JSONObject && obj.has("autoDebitFreq")) {
                val freq = obj.optString("autoDebitFreq", "")
                val amount = obj.optLong("autoDebitAmount", 0L)
                if (amount > 0) Pair(freq, amount) else null
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    var showMoreSheet by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showCloseDialog by remember { mutableStateOf(false) }
    
    var currentGoalName by remember { mutableStateOf(goalName) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppSurface()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with 3 dots
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = currentGoalName,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = AppText(),
                    modifier = Modifier.align(Alignment.Center)
                )
                IconButton(
                    onClick = { showMoreSheet = true },
                    modifier = Modifier.align(Alignment.CenterEnd)
                        .clip(CircleShape)
                        .background(AppPrimary().copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = AppPrimary())
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons Row (Tambah Dana / Tarik Dana)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { actionIsAdding = true; showActionSheet = true },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(AppPrimary().copy(alpha = 0.15f))
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = AppPrimary(), modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isId) "Tambah Dana" else "Add Funds",
                        fontSize = 13.sp,
                        color = AppText().copy(alpha = 0.8f)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { actionIsAdding = false; showActionSheet = true },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(AppPrimary().copy(alpha = 0.15f))
                    ) {
                        Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = AppPrimary(), modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isId) "Tarik Dana" else "Withdraw",
                        fontSize = 13.sp,
                        color = AppText().copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Balance Info Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(16.dp, AppBg())
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = if (isId) "Kamu telah menabung" else "You have saved",
                        fontSize = 14.sp,
                        color = AppText().copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$curSym ${NumberFormat.getInstance(locale).format(currentBalance)}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = AppText()
                    )

                    if (targetAmount > 0) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isId) "Target" else "Goal",
                                fontSize = 13.sp,
                                color = AppText().copy(alpha = 0.6f)
                            )
                            Text(
                                text = "$curSym ${NumberFormat.getInstance(locale).format(targetAmount)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppText()
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        SavingsProgressBar(progress = progress, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            fontSize = 13.sp,
                            color = AppText().copy(alpha = 0.6f),
                            modifier = Modifier.align(Alignment.End)
                        )
                    }

                    if (autoDebitInfo != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(AppPrimary().copy(alpha = 0.1f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(androidx.compose.material.icons.Icons.Filled.Refresh, contentDescription = null, tint = AppPrimary(), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isId) "Autodebit Aktif" else "Autodebit Active",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppPrimary()
                                )
                                Text(
                                    text = "${autoDebitInfo.first} • $curSym ${NumberFormat.getInstance(locale).format(autoDebitInfo.second)}",
                                    fontSize = 11.sp,
                                    color = AppText().copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    if (showActionSheet) {
        SavingsActionSheet(
            goalName = currentGoalName,
            profile = profile,
            walletBalances = walletBalances,
            dao = dao,
            initialIsAdding = actionIsAdding,
            onDismiss = { showActionSheet = false }
        )
    }

    if (showMoreSheet) {
        SavingsMoreSheet(
            goalName = currentGoalName,
            profile = profile,
            dao = dao,
            onDismiss = { showMoreSheet = false },
            onOpenHistory = { showHistorySheet = true },
            onOpenSettings = { showSettingsSheet = true },
            onOpenCloseConfirm = { showCloseDialog = true }
        )
    }
    if (showHistorySheet) {
        SavingsHistorySheet(
            goalName = currentGoalName,
            profile = profile,
            dao = dao,
            onDismiss = { showHistorySheet = false }
        )
    }
    if (showSettingsSheet) {
        SavingsSettingsSheet(
            goalName = currentGoalName,
            profile = profile,
            dao = dao,
            onDismiss = { showSettingsSheet = false },
            onNameChanged = { currentGoalName = it }
        )
    }
    if (showCloseDialog) {
        SavingsCloseDialog(
            goalName = currentGoalName,
            profile = profile,
            walletBalances = walletBalances,
            dao = dao,
            onDismiss = { showCloseDialog = false },
            onClosed = { showCloseDialog = false; onDismiss() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSavingsGoalSheet(
    profile: UserProfile,
    dao: TransactionDao,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isId = AppStr.isId

    var goalName by remember { mutableStateOf("") }
    var goalTargetRaw by remember { mutableStateOf("") }

    val goalTargetFormatted = remember(goalTargetRaw) {
        val digits = goalTargetRaw.filter { it.isDigit() }
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
            Text(
                text = if (isId) "Tabungan Baru" else "New Savings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AppText(),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Savings Name
            Text(
                text = if (isId) "Nama Tabungan" else "Savings Name",
                fontSize = 14.sp,
                color = AppText().copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = goalName,
                onValueChange = { goalName = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppPrimary(),
                    unfocusedBorderColor = AppText().copy(alpha = 0.3f),
                    focusedTextColor = AppText(),
                    unfocusedTextColor = AppText(),
                    cursorColor = AppPrimary()
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Target Amount
            Text(
                text = if (isId) "Target Nominal (opsional)" else "Target Amount (optional)",
                fontSize = 14.sp,
                color = AppText().copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            val curSym = if (profile.currency == "IDR") "Rp" else profile.currency
            OutlinedTextField(
                value = goalTargetFormatted,
                onValueChange = { newVal ->
                    goalTargetRaw = newVal.filter { it.isDigit() }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                prefix = { Text("$curSym ", color = AppText().copy(alpha = 0.6f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppPrimary(),
                    unfocusedBorderColor = AppText().copy(alpha = 0.3f),
                    focusedTextColor = AppText(),
                    unfocusedTextColor = AppText(),
                    cursorColor = AppPrimary()
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            com.bearbones.kumaflow.ui.components.KumaButton(
                onClick = {
                    if (goalName.isNotBlank()) {
                        scope.launch {
                            val newWallets = if (profile.savingsWallets.isBlank()) goalName else "${profile.savingsWallets},$goalName"
                            val json = try { JSONObject(profile.savingsGoals) } catch (e: Exception) { JSONObject() }
                            val targetLong = goalTargetRaw.filter { it.isDigit() }.toLongOrNull() ?: 0L
                            val goalDetails = JSONObject()
                            goalDetails.put("target", targetLong)
                            json.put(goalName, goalDetails)
                            dao.saveProfile(profile.copy(
                                savingsWallets = newWallets,
                                savingsGoals = json.toString()
                            ))
                            sheetState.hide()
                            onDismiss()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppPrimary()),
                enabled = goalName.isNotBlank()
            ) {
                Text(
                    if (isId) "Simpan" else "Save",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun SavingsProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val isBrutal = com.bearbones.kumaflow.ui.theme.LocalIsBrutal.current
    
    if (isBrutal) {
        Box(
            modifier = modifier
                .height(18.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AppSurface())
                .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
        ) {
            if (progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = progress)
                        .clip(RoundedCornerShape(8.dp))
                        .background(com.bearbones.kumaflow.ui.theme.BrutalGreen)
                        .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
                )
            }
        }
    } else {
        LinearProgressIndicator(
            progress = { progress },
            modifier = modifier
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            color = AppPrimary(),
            trackColor = AppPrimary().copy(alpha = 0.2f)
        )
    }
}

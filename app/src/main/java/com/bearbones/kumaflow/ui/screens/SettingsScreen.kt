@file:Suppress("SpellCheckingInspection", "UNUSED_PARAMETER", "unused", "CanBeVal", "DEPRECATION", "ScheduleExactAlarm")
@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bearbones.kumaflow

import android.Manifest
import android.annotation.SuppressLint
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
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.bearbones.kumaflow.ui.components.KumaExpressiveIcon
import androidx.compose.material.icons.Icons
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
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
import com.bearbones.kumaflow.glassCard
import com.bearbones.kumaflow.getGlassTextFieldColors
import com.bearbones.kumaflow.ManageWalletFullScreen
import com.bearbones.kumaflow.AppBg
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.draw.blur
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import com.bearbones.kumaflow.TransactionDao
import com.bearbones.kumaflow.ui.components.KumaIconButton
import com.bearbones.kumaflow.ui.components.KumaTextButton

// --- DATA CLASSES & OBJECTS ---
fun isNotificationServiceEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat?.contains(pkgName) == true
}

@Composable
fun SettingsScreen(
    currentProfile: UserProfile,
    monthlyTransactionsWithSplits: List<TransactionWithSplits>,
    allTransactionsWithSplits: List<TransactionWithSplits>,
    dao: TransactionDao,
    selectedMonth: Int,
    selectedYear: Int,
    paddingValues: PaddingValues,
    onForceUpdate: () -> Unit,
    onOpenQrTransfer: () -> Unit,
    onOpenDuoSync: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val mainActivity = context as? MainActivity
    val scope = rememberCoroutineScope()
    val sharedPrefs = remember { context.getSharedPreferences("kumaflow_prefs", Context.MODE_PRIVATE) }
    var autoTrackerEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("enable_auto_tracker", false)) }
    var showOemWarningDialog by remember { mutableStateOf(false) }

    var showVersionDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showTargetDialog by remember { mutableStateOf(false) }
    var showCatBudgetDialog by remember { mutableStateOf(false) }
    var showQrisDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showWalletDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showOcrDialog by remember { mutableStateOf(false) }

    var pinInput by remember { mutableStateOf("") }
    var targetInput by remember { mutableStateOf(currentProfile.monthlyTarget.toString()) }
    var isTurningOn by remember { mutableStateOf(true) }
    var newName by remember(currentProfile.userName) { 
        mutableStateOf(
            currentProfile.userName
                .replace("#pride", "", ignoreCase = true)
                .replace("#bear", "", ignoreCase = true)
                .replace("#brutal", "", ignoreCase = true)
                .replace("#OR", "", ignoreCase = true)
                .trim()
        )
    }
    
    val sharedPref = remember { context.getSharedPreferences("kumaflow_prefs", android.content.Context.MODE_PRIVATE) }
    var newDob by remember { mutableStateOf(sharedPref.getString("user_dob", "") ?: "") }
    var newEasterEgg by remember { mutableStateOf(sharedPref.getString("easter_egg_code", "") ?: "") }
    var isRestoring by remember { mutableStateOf(false) }

    var showRestoreCompleteNote by remember { mutableStateOf(false) }

    LaunchedEffect(mainActivity?.pendingRestoreJson) {
        val jsonToRestore = mainActivity?.pendingRestoreJson
        if (jsonToRestore != null) {
            isRestoring = true
            scope.launch(Dispatchers.IO) {
                try {
                    // Atomic block to prevent corruption if user leaves or crashes
                    kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                        com.bearbones.kumaflow.utils.RestoreUtils.parseAndRestoreJson(jsonToRestore, context)
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, AppStr.resOk, Toast.LENGTH_SHORT).show()
                        updateKumaWidget(context)
                        showRestoreCompleteNote = true
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "${AppStr.errRestore} ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        isRestoring = false
                        mainActivity.pendingRestoreJson = null
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 24.dp))
            Text(AppStr.set, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = AppText())
            Spacer(modifier = Modifier.height(24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                SettingsGroupCard(
                    title = AppStr.accSec,
                    modifier = Modifier.fillMaxWidth(),
                    items = listOf(
                        AppStr.editProf to Icons.Default.Edit,
                        AppStr.theme to Icons.Default.Palette,
                        AppStr.appLck to Icons.Default.Fingerprint
                    ),
                    hasSwitch = true,
                    isSwitchOn = currentProfile.isAppLocked,
                    onSwitchChange = {
                        isTurningOn = it
                        showPinDialog = true
                    }
                ) { label ->
                    when(label) {
                        AppStr.editProf -> showEditProfileDialog = true
                        AppStr.theme -> showThemeDialog = true
                    }
                }

                SettingsGroupCard(
                    title = AppStr.finPref,
                    modifier = Modifier.fillMaxWidth(),
                    items = listOf(
                        AppStr.cur to Icons.Default.Sync,
                        AppStr.manageWallet to Icons.Default.AccountBalanceWallet,
                        AppStr.manageCat to Icons.Default.DashboardCustomize,
                        AppStr.tar to Icons.Default.Adjust,
                        AppStr.catBudget to Icons.Default.PieChart,
                        AppStr.splitBillCfg to Icons.Default.QrCodeScanner,
                        AppStr.ocrSettingsTitle to Icons.Default.DocumentScanner
                    ),
                    onClick = { label ->
                        when(label) {
                            AppStr.cur -> showCurrencyDialog = true
                            AppStr.manageWallet -> showWalletDialog = true
                            AppStr.tar -> showTargetDialog = true
                            AppStr.manageCat -> showCategoryDialog = true
                            AppStr.catBudget -> showCatBudgetDialog = true
                            AppStr.splitBillCfg -> showQrisDialog = true
                            AppStr.ocrSettingsTitle -> showOcrDialog = true
                        }
                    }
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(24.dp, AppSurface()),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                var expandReminders by remember { mutableStateOf(false) }

                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        AppStr.notif,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = AppText(),
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(AppPrimary().copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            KumaExpressiveIcon(Icons.Default.AccountBalanceWallet, null, tint = AppPrimary(), containerColor = androidx.compose.ui.graphics.Color.Transparent, size = 24.dp, iconPadding = 3.dp)
                        }
                        Text(
                            AppStr.carryOver,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 14.dp),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppText(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Switch(
                            checked = currentProfile.useCarryOver,
                            onCheckedChange = { isChecked ->
                                scope.launch {
                                    dao.saveProfile(currentProfile.copy(useCarryOver = isChecked))
                                    onForceUpdate()
                                }
                            },
                            modifier = Modifier.scale(0.8f)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(AppPrimary().copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            KumaExpressiveIcon(Icons.Default.NotificationsActive, null, tint = AppPrimary(), containerColor = androidx.compose.ui.graphics.Color.Transparent, size = 24.dp, iconPadding = 3.dp)
                        }
                        Text(
                            AppStr.dailyRem,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 14.dp),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppText(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (currentProfile.isReminderOn) {
                            KumaIconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    expandReminders = !expandReminders
                                },
                                modifier = Modifier.size(28.dp).padding(end = 4.dp)
                            ) {
                                KumaExpressiveIcon(
                                    imageVector = if (expandReminders) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = AppPrimary(),
                                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                                    size = 24.dp,
                                    iconPadding = 0.dp
                                )
                            }
                        }

                        Switch(
                            checked = currentProfile.isReminderOn,
                            onCheckedChange = { isChecked ->
                                scope.launch {
                                    val newProf = currentProfile.copy(isReminderOn = isChecked)
                                    dao.saveProfile(newProf)
                                    onForceUpdate()
                                }
                                if (!isChecked) expandReminders = false
                            },
                            modifier = Modifier.scale(0.8f)
                        )
                    }

                    if (currentProfile.isReminderOn && expandReminders) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .glassCard(12.dp, AppSurfaceVariant())
                                .padding(12.dp)
                        ) {
                            val timesList = currentProfile.reminderTimes.split(",")
                            timesList.forEachIndexed { index, time ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val parts = time.split(":")
                                            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
                                            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

                                            TimePickerDialog(context, { _, h, m ->
                                                val newTimes = timesList.toMutableList()
                                                newTimes[index] = String.format(Locale.getDefault(), "%02d:%02d", h, m)
                                                val newProf = currentProfile.copy(reminderTimes = newTimes.joinToString(","))
                                                scope.launch {
                                                    dao.saveProfile(newProf)
                                                    onForceUpdate()
                                                }
                                            }, hour, minute, true).show()
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    KumaExpressiveIcon(Icons.Default.AccessTime, null, tint = AppText().copy(alpha=0.7f), containerColor = androidx.compose.ui.graphics.Color.Transparent, size = 20.dp, iconPadding = 2.dp)
                                    Text(
                                        "${AppStr.rem} ${index + 1}",
                                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                                        fontSize = 12.sp,
                                        color = AppText().copy(alpha=0.8f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        time,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AppPrimary()
                                    )
                                }
                                if (index < timesList.size - 1) {
                                    HorizontalDivider(color = AppText().copy(alpha = 0.1f))
                                }
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(24.dp, AppSurface()),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 26.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(AppPrimary().copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        KumaExpressiveIcon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, tint = AppPrimary(), containerColor = androidx.compose.ui.graphics.Color.Transparent, size = 24.dp, iconPadding = 3.dp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Auto Notif Tracker",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppText(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Mencatat transaksi E-Wallet dari notifikasi (GoPay, DANA, BCA, dll).",
                            fontSize = 10.sp,
                            color = AppText().copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = autoTrackerEnabled,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                if (!isNotificationServiceEnabled(context)) {
                                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                    Toast.makeText(context, AppStr.reqNotifAcc, Toast.LENGTH_LONG).show()
                                } else {
                                    autoTrackerEnabled = true
                                    sharedPrefs.edit().putBoolean("enable_auto_tracker", true).apply()
                                    showOemWarningDialog = true
                                }
                            } else {
                                autoTrackerEnabled = false
                                sharedPrefs.edit().putBoolean("enable_auto_tracker", false).apply()
                            }
                        },
                        modifier = Modifier.scale(0.8f)
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SettingsGroupCard(
                    title = AppStr.dat,
                    modifier = Modifier.fillMaxWidth(),
                    items = listOf(
                        AppStr.expPdf to Icons.Default.PictureAsPdf,
                        AppStr.expCsv to Icons.Default.Description,
                        AppStr.expDrive to Icons.Default.AddToDrive,
                        AppStr.backApp to Icons.Default.CloudUpload,
                        "Transfer via Local WiFi" to Icons.Default.Wifi,
                        "Kuma Duo (Shared Wallet)" to Icons.Default.SyncAlt,
                        AppStr.rest to Icons.Default.History,
                        AppStr.optDb to Icons.Default.CleaningServices,
                        AppStr.resetBal to Icons.Default.Delete
                    )
                ) { label ->
                    val plainMonthlyTxs = monthlyTransactionsWithSplits.map { it.transaction }
                    when (label) {
                        AppStr.expPdf -> generatePDF(context, plainMonthlyTxs, currentProfile, selectedMonth, selectedYear)
                        AppStr.expCsv -> generateCSV(context, plainMonthlyTxs, currentProfile, selectedMonth, selectedYear)
                        AppStr.expDrive -> exportToDrive(context, plainMonthlyTxs, currentProfile, selectedMonth, selectedYear)
                        AppStr.backApp -> backupAppToJSON(context)
                        "Transfer via Local WiFi" -> onOpenQrTransfer()
                        "Kuma Duo (Shared Wallet)" -> onOpenDuoSync()
                        AppStr.rest -> { mainActivity?.openSafeFilePicker() }
                        AppStr.resetBal -> { showResetDialog = true }
                        AppStr.optDb -> {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val db = KumaDatabase.getDatabase(context)
                                    db.openHelper.writableDatabase.execSQL("VACUUM")
                                    withContext(Dispatchers.Main) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        Toast.makeText(context, AppStr.optSuccess, Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "${AppStr.optFail} ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
                }

                SettingsGroupCard(
                    title = AppStr.abt,
                    modifier = Modifier.fillMaxWidth(),
                    items = listOf(
                        AppStr.appVer to Icons.Default.Info,
                        AppStr.priv to Icons.Default.PrivacyTip,
                        AppStr.trms to Icons.AutoMirrored.Filled.MenuBook,
                        AppStr.contDev to Icons.Default.SupportAgent
                    )
                ) { label ->
                    when (label) {
                        AppStr.appVer -> showVersionDialog = true
                        AppStr.priv -> showPrivacyDialog = true
                        AppStr.trms -> showTermsDialog = true
                        AppStr.contDev -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/kumaflow")))
                    }
                }
            }
        }

        if (showCatBudgetDialog) {
            val expenseCatsList = currentProfile.expenseCats.split(",").filter { it.isNotBlank() }
            var targetMap by remember {
                mutableStateOf(
                    try {
                        val json = JSONObject(currentProfile.categoryTargets)
                        val map = mutableMapOf<String, Long>()
                        val keys = json.keys()
                        while(keys.hasNext()) {
                            val k = keys.next()
                            map[k] = json.optLong(k, 0L)
                        }
                        map
                    } catch (e: Exception) {
                        mutableMapOf<String, Long>()
                    }
                )
            }
            AlertDialog(
                onDismissRequest = { showCatBudgetDialog = false },
                modifier = Modifier.glassCard(24.dp, AppSurface()),
                containerColor = if (LocalIsLiquidGlass.current) androidx.compose.ui.graphics.Color.Transparent else AppSurface(),
                title = { Text(AppStr.catBudget, fontWeight = FontWeight.Bold) },
                text = {
                    Box(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                            expenseCatsList.forEach { catName ->
                                val currentVal = targetMap[catName] ?: 0L
                                var inputValue by remember { mutableStateOf(if (currentVal == 0L) "" else currentVal.toString()) }
                                com.bearbones.kumaflow.ui.components.KumaOutlinedTextField(
                                    value = inputValue,
                                    onValueChange = { input ->
                                        if (input.isEmpty() || input.all { char -> char.isDigit() }) {
                                            inputValue = input
                                            targetMap = targetMap.toMutableMap().apply { put(catName, input.toLongOrNull() ?: 0L) }
                                        }
                                    },
                                    label = { Text(catName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .glassCard(12.dp, AppSurfaceVariant()),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = getGlassTextFieldColors(),
                                    singleLine = true,
                                    visualTransformation = com.bearbones.kumaflow.ThousandSeparatorTransformation()
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    com.bearbones.kumaflow.ui.components.KumaButton(
                        onClick = {
                            scope.launch {
                                val json = JSONObject()
                                targetMap.forEach { (k, v) -> json.put(k, v) }
                                dao.saveProfile(currentProfile.copy(categoryTargets = json.toString()))
                                onForceUpdate()
                                showCatBudgetDialog = false
                            }
                        }
                    ) { Text(AppStr.save) }
                },
                dismissButton = {
                    KumaTextButton(onClick = { showCatBudgetDialog = false }) { Text(AppStr.close, color = AppText()) }
                },
                
            )
        }

        if (showOemWarningDialog) {
            AlertDialog(
                onDismissRequest = { showOemWarningDialog = false },
                modifier = Modifier.glassCard(24.dp, AppSurface()),
                containerColor = if (LocalIsLiquidGlass.current) androidx.compose.ui.graphics.Color.Transparent else AppSurface(),
                title = { Text(AppStr.importantBattery, fontWeight = FontWeight.Bold) },
                text = {
                    Text(AppStr.oemWarningText)
                },
                confirmButton = {
                    com.bearbones.kumaflow.ui.components.KumaButton(onClick = {
                        showOemWarningDialog = false
                        try {
                            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        } catch (e: Exception) {
                            Toast.makeText(context, AppStr.errOpenBat, Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text(AppStr.openBatterySet)
                    }
                },
                dismissButton = {
                    KumaTextButton(onClick = { showOemWarningDialog = false }) {
                        Text(AppStr.laterBtn)
                    }
                }
            )
        }

        var virtualWallets by remember { mutableStateOf<List<com.bearbones.kumaflow.VirtualWallet>>(emptyList()) }
        LaunchedEffect(Unit) {
            dao.observeAllVirtualWallets().collect {
                virtualWallets = it
            }
        }

        if (showWalletDialog) {
            ManageWalletFullScreen(
                virtualWallets = virtualWallets,
                onSave = { oldName, wallet ->
                    scope.launch {
                        if (oldName != null && oldName != wallet.name) {
                            dao.renameVirtualWalletFully(oldName, wallet)
                        } else {
                            if (oldName == null) {
                                val newOrderIndex = if (virtualWallets.isEmpty()) 0 else virtualWallets.maxOf { it.orderIndex } + 1
                                dao.upsertVirtualWallet(wallet.copy(orderIndex = newOrderIndex))
                                dao.addWalletToProfile(wallet.name) // Add to old system
                            } else {
                                dao.upsertVirtualWallet(wallet)
                            }
                        }
                        
                        onForceUpdate()
                    }
                },
                onDelete = { w ->
                    scope.launch {
                        dao.deleteVirtualWallet(w.name)
                        dao.removeWalletFromProfile(w.name) // Remove from old system
                        onForceUpdate()
                    }
                },
                onDismiss = { showWalletDialog = false },
                userProfileName = currentProfile.userName
            )
        }

        if (showCategoryDialog) {
            var isIncomeTab by remember { mutableStateOf(false) }
            var newCatName by remember { mutableStateOf("") }
            var activeIncomeCats by remember { mutableStateOf(currentProfile.incomeCats.split(",").filter { it.isNotBlank() }) }
            var activeExpenseCats by remember { mutableStateOf(currentProfile.expenseCats.split(",").filter { it.isNotBlank() }) }

            var selectedIconKey by remember { mutableStateOf("Kategori") }
            var editingCatOldName by remember { mutableStateOf<String?>(null) }

            AlertDialog(
                onDismissRequest = {
                    showCategoryDialog = false
                    onForceUpdate()
                },
                modifier = Modifier.glassCard(24.dp, AppSurface()),
                containerColor = if (LocalIsLiquidGlass.current) androidx.compose.ui.graphics.Color.Transparent else AppSurface(),
                title = { Text(AppStr.manageCat, fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .glassCard(12.dp, AppSurfaceVariant(), useHaze = false)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (!isIncomeTab) AppRed() else Color.Transparent)
                                    .clickable {
                                        isIncomeTab = false
                                        editingCatOldName = null
                                        newCatName = ""
                                        selectedIconKey = "Kategori"
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    },
                                contentAlignment = Alignment.Center
                            ) { Text(AppStr.exp, color = if (!isIncomeTab) Color.White else AppText(), fontSize = 12.sp, fontWeight = FontWeight.Bold) }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isIncomeTab) AppGreen() else Color.Transparent)
                                    .clickable {
                                        isIncomeTab = true
                                        editingCatOldName = null
                                        newCatName = ""
                                        selectedIconKey = "Kategori"
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    },
                                contentAlignment = Alignment.Center
                            ) { Text(AppStr.inc, color = if (isIncomeTab) Color.White else AppText(), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        val currentList = if (isIncomeTab) activeIncomeCats else activeExpenseCats

                        LazyColumn(modifier = Modifier.heightIn(max = 120.dp)) {
                            items(currentList) { cat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("• $cat", color = AppText(), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))

                                    Row {
                                        KumaIconButton(
                                            onClick = {
                                                editingCatOldName = cat
                                                newCatName = cat
                                                val iconJson = try { JSONObject(currentProfile.categoryIcons) } catch (e: Exception) { JSONObject() }
                                                selectedIconKey = iconJson.optString(cat, "Kategori")
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) { KumaExpressiveIcon(Icons.Default.Edit, null, tint = AppPrimary(), containerColor = androidx.compose.ui.graphics.Color.Transparent) }

                                        if (currentList.size > 1) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            KumaIconButton(
                                                onClick = {
                                                    if (isIncomeTab) {
                                                        val newList = activeIncomeCats.filter { it != cat }
                                                        activeIncomeCats = newList
                                                        scope.launch { dao.saveProfile(currentProfile.copy(incomeCats = newList.joinToString(","))) }
                                                    } else {
                                                        val newList = activeExpenseCats.filter { it != cat }
                                                        activeExpenseCats = newList
                                                        scope.launch { dao.saveProfile(currentProfile.copy(expenseCats = newList.joinToString(","))) }
                                                    }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) { KumaExpressiveIcon(Icons.Default.Delete, null, tint = AppRed(), containerColor = androidx.compose.ui.graphics.Color.Transparent) }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (editingCatOldName != null) {
                                Text("${AppStr.edit}: $editingCatOldName", fontSize = 10.sp, color = AppPrimary(), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(bottom = 4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                ) {
                                    com.bearbones.kumaflow.ui.components.KumaOutlinedTextField(
                                        value = newCatName,
                                        onValueChange = { newCatName = it },
                                        label = { Text(AppStr.catName) },
                                        modifier = Modifier.weight(1f).glassCard(12.dp, AppSurfaceVariant()),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = getGlassTextFieldColors()
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(modifier = Modifier.size(48.dp).clip(androidx.compose.foundation.shape.CircleShape).background(AppRed()).clickable {
                                        editingCatOldName = null
                                        newCatName = ""
                                        selectedIconKey = "Kategori"
                                    }, contentAlignment = Alignment.Center) {
                                        KumaExpressiveIcon(Icons.Default.Close, null, tint = Color.White, containerColor = androidx.compose.ui.graphics.Color.Transparent)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(modifier = Modifier.size(48.dp).clip(androidx.compose.foundation.shape.CircleShape).background(AppPrimary()).clickable {
                                        if (newCatName.isNotBlank()) {
                                            val newCat = newCatName.trim()
                                            val iconJson = try { org.json.JSONObject(currentProfile.categoryIcons) } catch (e: Exception) { org.json.JSONObject() }
    
                                            if (editingCatOldName != null && editingCatOldName != newCat) {
                                                iconJson.remove(editingCatOldName)
                                            }
                                            iconJson.put(newCat, selectedIconKey)
    
                                            if (isIncomeTab) {
                                                val newList = activeIncomeCats.toMutableList()
                                                if (editingCatOldName != null) {
                                                    val idx = newList.indexOf(editingCatOldName)
                                                    if (idx != -1) newList[idx] = newCat
                                                }
                                                activeIncomeCats = newList
                                                scope.launch { dao.saveProfile(currentProfile.copy(incomeCats = newList.joinToString(","), categoryIcons = iconJson.toString())) }
                                            } else {
                                                val newList = activeExpenseCats.toMutableList()
                                                if (editingCatOldName != null) {
                                                    val idx = newList.indexOf(editingCatOldName)
                                                    if (idx != -1) newList[idx] = newCat
                                                }
                                                activeExpenseCats = newList
                                                scope.launch { dao.saveProfile(currentProfile.copy(expenseCats = newList.joinToString(","), categoryIcons = iconJson.toString())) }
                                            }
                                            newCatName = ""
                                            selectedIconKey = "Kategori"
                                            editingCatOldName = null
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        }
                                    }, contentAlignment = Alignment.Center) {
                                        KumaExpressiveIcon(Icons.Default.Check, null, tint = Color.White, containerColor = androidx.compose.ui.graphics.Color.Transparent)
                                    }
                                }
                            } else {
                                com.bearbones.kumaflow.ui.components.KumaOutlinedTextField(
                                    value = newCatName,
                                    onValueChange = { newCatName = it },
                                    label = { Text(AppStr.catName) },
                                    modifier = Modifier.fillMaxWidth().glassCard(12.dp, AppSurfaceVariant()),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = getGlassTextFieldColors()
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(AppStr.chooseCatIcon, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppText())
                            Spacer(modifier = Modifier.height(8.dp))

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(5),
                                modifier = Modifier.height(120.dp).glassCard(8.dp, AppSurfaceVariant(), useHaze = false)
                            ) {
                                items(kumaIconLibrary.keys.toList()) { key ->
                                    val icon = kumaIconLibrary[key]!!
                                    Box(
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selectedIconKey == key) AppPrimary() else Color.Transparent)
                                            .clickable {
                                                selectedIconKey = key
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                            }
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        KumaExpressiveIcon(icon, contentDescription = key, tint = if (selectedIconKey == key) Color.White else AppText(), containerColor = androidx.compose.ui.graphics.Color.Transparent, size = 30.dp, iconPadding = 5.dp)
                                    }
                                }
                            }

                            if (editingCatOldName == null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                com.bearbones.kumaflow.ui.components.KumaButton(
                                    onClick = {
                                        if (newCatName.isNotBlank()) {
                                            val newCat = newCatName.trim()
                                            val iconJson = try { org.json.JSONObject(currentProfile.categoryIcons) } catch (e: Exception) { org.json.JSONObject() }
    
                                            iconJson.put(newCat, selectedIconKey)
    
                                            if (isIncomeTab) {
                                                val newList = activeIncomeCats.toMutableList()
                                                if (!newList.contains(newCat)) newList.add(newCat)
                                                activeIncomeCats = newList
                                                scope.launch { dao.saveProfile(currentProfile.copy(incomeCats = newList.joinToString(","), categoryIcons = iconJson.toString())) }
                                            } else {
                                                val newList = activeExpenseCats.toMutableList()
                                                if (!newList.contains(newCat)) newList.add(newCat)
                                                activeExpenseCats = newList
                                                scope.launch { dao.saveProfile(currentProfile.copy(expenseCats = newList.joinToString(","), categoryIcons = iconJson.toString())) }
                                            }
                                            newCatName = ""
                                            selectedIconKey = "Kategori"
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(45.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text(AppStr.addCat) }
                            }
                        }
                    }
                },
                confirmButton = {
                    KumaTextButton(
                        onClick = {
                            showCategoryDialog = false
                            onForceUpdate()
                        }
                    ) { Text(AppStr.close) }
                }
            )
        }

        if (showEditProfileDialog) {
            AlertDialog(
                onDismissRequest = { showEditProfileDialog = false },
                modifier = Modifier.glassCard(24.dp, AppSurface()),
                containerColor = if (LocalIsLiquidGlass.current) androidx.compose.ui.graphics.Color.Transparent else AppSurface(),
                title = { Text(AppStr.editProf, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        com.bearbones.kumaflow.ui.components.KumaOutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text(AppStr.usr) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = getGlassTextFieldColors()
                        )
                        com.bearbones.kumaflow.ui.components.KumaOutlinedTextField(
                            value = newDob.filter { it.isDigit() },
                            onValueChange = { 
                                newDob = it.filter { char -> char.isDigit() }.take(8)
                            },
                            label = { Text(AppStr.dobLbl) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = { text ->
                                val trimmed = if (text.text.length >= 8) text.text.substring(0..7) else text.text
                                var out = ""
                                for (i in trimmed.indices) {
                                    out += trimmed[i]
                                    if (i == 1 || i == 3) out += "-"
                                }
                                val offsetMapping = object : androidx.compose.ui.text.input.OffsetMapping {
                                    override fun originalToTransformed(offset: Int): Int {
                                        if (offset <= 1) return offset
                                        if (offset <= 3) return offset + 1
                                        if (offset <= 8) return offset + 2
                                        return 10
                                    }
                                    override fun transformedToOriginal(offset: Int): Int {
                                        if (offset <= 2) return offset
                                        if (offset <= 5) return offset - 1
                                        if (offset <= 10) return offset - 2
                                        return 8
                                    }
                                }
                                androidx.compose.ui.text.input.TransformedText(androidx.compose.ui.text.AnnotatedString(out), offsetMapping)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = getGlassTextFieldColors()
                        )
                        com.bearbones.kumaflow.ui.components.KumaOutlinedTextField(
                            value = newEasterEgg.removePrefix("#"),
                            onValueChange = { 
                                val raw = it.replace("#", "")
                                newEasterEgg = if (raw.isNotEmpty()) "#$raw" else ""
                            },
                            label = { Text(AppStr.easterEggLbl) },
                            prefix = { Text("#") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = getGlassTextFieldColors()
                        )
                    }
                },
                confirmButton = {
                    com.bearbones.kumaflow.ui.components.KumaButton(
                        onClick = {
                            val formattedDob = buildString {
                                val d = newDob.filter { it.isDigit() }
                                for (i in d.indices) {
                                    append(d[i])
                                    if (i == 1 || i == 3) append("-")
                                }
                            }
                            sharedPref.edit()
                                .putString("user_dob", formattedDob)
                                .putString("easter_egg_code", newEasterEgg)
                                .apply()
                                
                            val finalName = "$newName $newEasterEgg".trim()
                            
                            scope.launch {
                                dao.saveProfile(currentProfile.copy(userName = finalName))
                                onForceUpdate()
                                showEditProfileDialog = false
                            }
                        }
                    ) { Text(AppStr.save) }
                }
            )
        }

        if (showCurrencyDialog) {
            AlertDialog(
                onDismissRequest = { showCurrencyDialog = false },
                modifier = Modifier.glassCard(24.dp, AppSurface()),
                containerColor = if (LocalIsLiquidGlass.current) androidx.compose.ui.graphics.Color.Transparent else AppSurface(),
                title = { Text(AppStr.selCur) },
                text = {
                    val currencies = listOf(
                        "IDR" to "🇮🇩", "USD" to "🇺🇸", "EUR" to "🇪🇺", "JPY" to "🇯🇵",
                        "GBP" to "🇬🇧", "AUD" to "🇦🇺", "CAD" to "🇨🇦", "CHF" to "🇨🇭",
                        "CNY" to "🇨🇳", "SGD" to "🇸🇬", "MYR" to "🇲🇾", "THB" to "🇹🇭",
                        "PHP" to "🇵🇭", "VND" to "🇻🇳"
                    )
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        currencies.chunked(2).forEach { rowItems ->
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowItems.forEach { (c, flag) ->
                                    com.bearbones.kumaflow.ui.components.KumaButton(
                                        onClick = {
                                            scope.launch {
                                                dao.saveProfile(currentProfile.copy(currency = c))
                                                onForceUpdate()
                                                showCurrencyDialog = false
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = AppPrimary().copy(alpha = 0.2f), contentColor = AppPrimary()),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                    ) {
                                        Text("$flag  $c", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }

        if (showOcrDialog) {
            val ocrStorage = remember { com.bearbones.kumaflow.utils.OcrSecureStorage(context) }
            var selectedProvider by remember { mutableStateOf(ocrStorage.getSelectedProvider()) }
            var anthropicKeyInput by remember { mutableStateOf(ocrStorage.getApiKey() ?: "") }
            var geminiKeyInput by remember { mutableStateOf(ocrStorage.getGeminiApiKey() ?: "") }
            var isKeyVisible by remember { mutableStateOf(false) }

            val isGemini = selectedProvider == "gemini"
            val hasCurrentKey = if (isGemini) ocrStorage.hasGeminiApiKey() else ocrStorage.hasApiKey()
            val currentKeyInput = if (isGemini) geminiKeyInput else anthropicKeyInput

            AlertDialog(
                onDismissRequest = { showOcrDialog = false },
                modifier = Modifier.glassCard(24.dp, AppSurface()),
                containerColor = if (LocalIsLiquidGlass.current) androidx.compose.ui.graphics.Color.Transparent else AppSurface(),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KumaExpressiveIcon(Icons.Default.DocumentScanner, null, tint = AppPrimary())
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(AppStr.ocrSettingsTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            AppStr.ocrProviderLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppText().copy(alpha = 0.8f)
                        )

                        // 2 Pilihan Provider (Anthropic vs Gemini)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = { selectedProvider = "anthropic" },
                                shape = RoundedCornerShape(12.dp),
                                color = if (!isGemini) AppPrimary() else AppSurface().copy(alpha = 0.6f),
                                contentColor = if (!isGemini) Color.White else AppText(),
                                border = if (!isGemini) null else BorderStroke(1.dp, AppText().copy(alpha = 0.15f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        AppStr.ocrProviderAnthropic,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }

                            Surface(
                                onClick = { selectedProvider = "gemini" },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isGemini) AppPrimary() else AppSurface().copy(alpha = 0.6f),
                                contentColor = if (isGemini) Color.White else AppText(),
                                border = if (isGemini) null else BorderStroke(1.dp, AppText().copy(alpha = 0.15f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        AppStr.ocrProviderGemini,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Text(
                            if (isGemini) AppStr.ocrGeminiKeyHint else AppStr.ocrApiKeyHint,
                            fontSize = 12.sp,
                            color = AppText().copy(alpha = 0.7f),
                            lineHeight = 16.sp
                        )

                        com.bearbones.kumaflow.ui.components.KumaOutlinedTextField(
                            value = currentKeyInput,
                            onValueChange = {
                                if (isGemini) {
                                    geminiKeyInput = it
                                } else {
                                    anthropicKeyInput = it
                                }
                            },
                            placeholder = { Text(if (isGemini) AppStr.ocrGeminiKeyLabel else AppStr.ocrApiKeyLabel) },
                            visualTransformation = if (isKeyVisible) VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                    Icon(
                                        if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = AppPrimary()
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (hasCurrentKey) AppStr.ocrApiKeyConfigured else AppStr.ocrApiKeyNotConfigured,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasCurrentKey) AppGreen() else AppRed()
                            )

                            if (hasCurrentKey) {
                                TextButton(
                                    onClick = {
                                        if (isGemini) {
                                            ocrStorage.deleteGeminiApiKey()
                                            geminiKeyInput = ""
                                        } else {
                                            ocrStorage.deleteApiKey()
                                            anthropicKeyInput = ""
                                        }
                                        Toast.makeText(context, AppStr.ocrApiKeyDeleted, Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text(AppStr.deleteApiKey, color = AppRed(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    com.bearbones.kumaflow.ui.components.KumaButton(
                        onClick = {
                            ocrStorage.saveSelectedProvider(selectedProvider)
                            if (isGemini) {
                                if (geminiKeyInput.isNotBlank()) {
                                    ocrStorage.saveGeminiApiKey(geminiKeyInput)
                                    Toast.makeText(context, AppStr.ocrApiKeySaved, Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                if (anthropicKeyInput.isNotBlank()) {
                                    ocrStorage.saveApiKey(anthropicKeyInput)
                                    Toast.makeText(context, AppStr.ocrApiKeySaved, Toast.LENGTH_SHORT).show()
                                }
                            }
                            showOcrDialog = false
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(AppStr.save, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showOcrDialog = false }) {
                        Text(AppStr.close, color = AppText().copy(alpha = 0.6f))
                    }
                }
            )
        }

        if (showTargetDialog) {
            AlertDialog(
                onDismissRequest = { showTargetDialog = false },
                modifier = Modifier.glassCard(24.dp, AppSurface()),
                containerColor = if (LocalIsLiquidGlass.current) androidx.compose.ui.graphics.Color.Transparent else AppSurface(),
                title = { Text(AppStr.setTar) },
                text = {
                    com.bearbones.kumaflow.ui.components.KumaOutlinedTextField(
                        value = targetInput,
                        onValueChange = { if (it.all { c -> c.isDigit() }) targetInput = it },
                        label = { Text(AppStr.limExp) },
                        modifier = Modifier.fillMaxWidth().glassCard(12.dp, AppSurfaceVariant()),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = getGlassTextFieldColors(),
                        visualTransformation = com.bearbones.kumaflow.ThousandSeparatorTransformation()
                    )
                },
                confirmButton = {
                    com.bearbones.kumaflow.ui.components.KumaButton(
                        onClick = {
                            scope.launch {
                                dao.saveProfile(currentProfile.copy(monthlyTarget = targetInput.toLongOrNull() ?: 0L))
                                onForceUpdate()
                                showTargetDialog = false
                            }
                        }
                    ) { Text(AppStr.btnSet) }
                }
            )
        }

        if (showThemeDialog) {
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                modifier = Modifier.glassCard(24.dp, AppSurface()),
                containerColor = if (LocalIsLiquidGlass.current) androidx.compose.ui.graphics.Color.Transparent else AppSurface(),
                title = { Text(AppStr.theme) },
                text = {
                    Column {
                        val hasPride = currentProfile.userName.contains("#pride", ignoreCase = true)
                        val hasBear = currentProfile.userName.contains("#bear", ignoreCase = true)
                        val hasBrutal = currentProfile.userName.contains("#brutal", ignoreCase = true)

                        val themeOptions = mutableListOf(
                            0 to AppStr.themeSys,
                            1 to AppStr.themeLight,
                            2 to AppStr.themeDark
                        )

                        if (hasPride) {
                            themeOptions.add(3 to "Pride Light 🏳️‍🌈")
                            themeOptions.add(4 to "Pride Dark 🏳️‍🌈")
                        }
                        if (hasBear) {
                            themeOptions.add(5 to "Bear Light 🐻")
                            themeOptions.add(6 to "Bear Dark 🐻")
                        }
                        
                        if (hasBrutal) {
                            themeOptions.add(7 to "Brutal Light")
                            themeOptions.add(8 to "Brutal Dark")
                        }

                        themeOptions.forEach { (value, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            dao.saveProfile(currentProfile.copy(themeMode = value))
                                            onForceUpdate()
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = currentProfile.themeMode == value, onClick = null)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(label, color = AppText(), fontWeight = if (value > 2) FontWeight.ExtraBold else FontWeight.Normal)
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = AppSurfaceVariant()
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp)) {
                                Text(AppStr.amoledDark, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppText())
                                Text(AppStr.amoledDesc, fontSize = 10.sp, color = AppText().copy(alpha=0.6f))
                            }
                            Switch(
                                checked = currentProfile.isAmoledMode,
                                onCheckedChange = { isChecked ->
                                    scope.launch {
                                        dao.saveProfile(currentProfile.copy(isAmoledMode = isChecked))
                                        onForceUpdate()
                                    }
                                },
                                modifier = Modifier.scale(0.8f)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp)) {
                                Text(AppStr.liquidGlass, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppText())
                                Text(AppStr.liquidGlassDesc, fontSize = 10.sp, color = AppText().copy(alpha=0.6f))
                            }
                            Switch(
                                checked = currentProfile.isLiquidGlass,
                                onCheckedChange = { isChecked ->
                                    scope.launch {
                                        dao.saveProfile(currentProfile.copy(isLiquidGlass = isChecked))
                                        onForceUpdate()
                                    }
                                },
                                modifier = Modifier.scale(0.8f)
                            )
                        }

                    }
                },
                confirmButton = {
                    KumaTextButton(onClick = { showThemeDialog = false }) { Text(AppStr.close) }
                }
            )
        }

        if (showPinDialog) {
            AlertDialog(
                onDismissRequest = { showPinDialog = false; pinInput = "" },
                modifier = Modifier.glassCard(24.dp, AppSurface()),
                containerColor = if (LocalIsLiquidGlass.current) androidx.compose.ui.graphics.Color.Transparent else AppSurface(),
                title = { Text(if(isTurningOn) AppStr.setPin else AppStr.confPin) },
                text = {
                    com.bearbones.kumaflow.ui.components.KumaOutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pinInput = it },
                        label = { Text(AppStr.pinLabel) },
                        modifier = Modifier.fillMaxWidth().glassCard(12.dp, AppSurfaceVariant()),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        shape = RoundedCornerShape(12.dp),
                        colors = getGlassTextFieldColors()
                    )
                },
                confirmButton = {
                    com.bearbones.kumaflow.ui.components.KumaButton(
                        enabled = pinInput.length == 6,
                        onClick = {
                            when {
                                isTurningOn -> {
                                    scope.launch {
                                        dao.saveProfile(currentProfile.copy(isAppLocked = true, appPin = pinInput))
                                        showPinDialog = false
                                        pinInput = ""
                                        Toast.makeText(context, AppStr.pinAct, Toast.LENGTH_SHORT).show()
                                    }
                                }
                                pinInput == currentProfile.appPin -> {
                                    scope.launch {
                                        dao.saveProfile(currentProfile.copy(isAppLocked = false))
                                        showPinDialog = false
                                        pinInput = ""
                                        Toast.makeText(context, AppStr.pinDeact, Toast.LENGTH_SHORT).show()
                                    }
                                }
                                else -> {
                                    Toast.makeText(context, AppStr.wrongPin, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) { Text(AppStr.okBtn) }
                }
            )
        }

        if (showVersionDialog) {
            AlertDialog(
                onDismissRequest = { showVersionDialog = false },
                modifier = Modifier.glassCard(24.dp, AppSurface()),
                containerColor = if (LocalIsLiquidGlass.current) androidx.compose.ui.graphics.Color.Transparent else AppSurface(),
                title = { Text(AppStr.info, fontWeight = FontWeight.Bold) },
                text = { Text(AppStr.versionInfo) },
                confirmButton = { KumaTextButton(onClick = { showVersionDialog = false }) { Text(AppStr.close) } },
                
                
            )
        }

        if (showPrivacyDialog) {
            AlertDialog(
                onDismissRequest = { showPrivacyDialog = false },
                modifier = Modifier.glassCard(24.dp, AppSurface()),
                containerColor = if (LocalIsLiquidGlass.current) androidx.compose.ui.graphics.Color.Transparent else AppSurface(),
                title = { Text(AppStr.priv, fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(AppStr.privDesc)
                    }
                },
                confirmButton = { KumaTextButton(onClick = { showPrivacyDialog = false }) { Text(AppStr.gotIt) } },
                
                
            )
        }

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                modifier = Modifier.glassCard(24.dp, AppSurface()),
                containerColor = if (LocalIsLiquidGlass.current) androidx.compose.ui.graphics.Color.Transparent else AppSurface(),
                title = { Text(AppStr.resetBalConfTitle, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFFE53935)) },
                text = { Text(AppStr.resetBalConfDesc, color = AppText()) },
                confirmButton = {
                    com.bearbones.kumaflow.ui.components.KumaButton(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                val balances = mutableMapOf<String, Long>()
                                allTransactionsWithSplits.forEach { txObj ->
                                    val amt = txObj.transaction.amount.toLongOrNull() ?: 0L
                                    val isInc = txObj.transaction.isIncome
                                    if (txObj.splits.isNotEmpty()) {
                                        txObj.splits.forEach { split ->
                                            val cur = balances[split.splitWallet] ?: 0L
                                            val spAmt = split.splitAmount
                                            balances[split.splitWallet] = if (isInc) cur + spAmt else cur - spAmt
                                        }
                                    } else {
                                        val w = txObj.transaction.wallet
                                        val cur = balances[w] ?: 0L
                                        balances[w] = if (isInc) cur + amt else cur - amt
                                    }
                                }

                                val now = java.time.LocalDateTime.now()
                                val dateStr = java.time.LocalDate.now().toString()
                                val timeStr = now.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)

                                balances.forEach { (wName, bal) ->
                                    if (bal != 0L) {
                                        val adjustTx = com.bearbones.kumaflow.KumaTransaction(
                                            name = if(AppStr.isId) "Reset Saldo" else "Balance Reset",
                                            date = dateStr,
                                            amount = kotlin.math.abs(bal).toString(),
                                            isIncome = bal < 0,
                                            category = "Balancing",
                                            wallet = wName,
                                            timestamp = timeStr,
                                            message = if(AppStr.isId) "Penyesuaian sistem untuk reset saldo" else "System adjustment to reset balance"
                                        )
                                        dao.insertTransaction(adjustTx)
                                    }
                                }

                                withContext(Dispatchers.Main) {
                                    showResetDialog = false
                                    Toast.makeText(context, AppStr.resetOk, Toast.LENGTH_SHORT).show()
                                    onForceUpdate()
                                }
                            }
                        }
                    ) { Text(AppStr.resetBal, color = androidx.compose.ui.graphics.Color.White) }
                },
                dismissButton = {
                    com.bearbones.kumaflow.ui.components.KumaTextButton(onClick = { showResetDialog = false }) { Text(AppStr.cancelBtn) }
                }
            )
        }

        if (showQrisDialog) {
            var qrisUri by remember { mutableStateOf<Uri?>(null) }
            var holderName by remember { mutableStateOf(currentProfile.qrisHolderName) }
            var bankName by remember { mutableStateOf(currentProfile.bankName) }
            var bankAcc by remember { mutableStateOf(currentProfile.bankAccount) }

            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                if (uri != null) {
                    scope.launch(Dispatchers.IO) {
                        try {
                            val inputStream = context.contentResolver.openInputStream(uri)
                            val file = File(context.filesDir, "qris_image.jpg")
                            val outputStream = FileOutputStream(file)
                            inputStream?.copyTo(outputStream)
                            inputStream?.close()
                            outputStream.close()
                            val newProf = currentProfile.copy(qrisFilePath = file.absolutePath)
                            dao.saveProfile(newProf)
                            withContext(Dispatchers.Main) {
                                onForceUpdate()
                                qrisUri = Uri.fromFile(file)
                                Toast.makeText(context, "QRIS Image Saved", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            AlertDialog(
                onDismissRequest = { showQrisDialog = false },
                modifier = Modifier.glassCard(24.dp, AppSurface()),
                containerColor = if (LocalIsLiquidGlass.current) androidx.compose.ui.graphics.Color.Transparent else AppSurface(),
                title = { Text(AppStr.splitBillCfg, fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(AppStr.qrisImg, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                        com.bearbones.kumaflow.ui.components.KumaButton(
                            onClick = { launcher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = AppPrimary().copy(alpha = 0.2f), contentColor = AppPrimary()),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            KumaExpressiveIcon(Icons.Default.Image, contentDescription = null, containerColor = androidx.compose.ui.graphics.Color.Transparent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (currentProfile.qrisFilePath.isNotEmpty()) "Change QRIS Image" else "Upload QRIS Image")
                        }
                        if (currentProfile.qrisFilePath.isNotEmpty()) {
                            Text("QRIS File: ...${currentProfile.qrisFilePath.takeLast(15)}", fontSize = 10.sp, color = AppText().copy(alpha = 0.5f))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(AppStr.holderName, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                        com.bearbones.kumaflow.ui.components.KumaOutlinedTextField(
                            value = holderName,
                            onValueChange = { holderName = it },
                            placeholder = { Text("e.g. Gabriel Bernard") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = getGlassTextFieldColors()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(AppStr.bankName, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                        com.bearbones.kumaflow.ui.components.KumaOutlinedTextField(
                            value = bankName,
                            onValueChange = { bankName = it },
                            placeholder = { Text("e.g. BCA") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = getGlassTextFieldColors()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(AppStr.bankAcc, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                        com.bearbones.kumaflow.ui.components.KumaOutlinedTextField(
                            value = bankAcc,
                            onValueChange = { bankAcc = it },
                            placeholder = { Text("e.g. 1234567890") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = getGlassTextFieldColors()
                        )
                    }
                },
                confirmButton = {
                    KumaTextButton(onClick = {
                        scope.launch {
                            val newProf = currentProfile.copy(qrisHolderName = holderName, bankName = bankName, bankAccount = bankAcc)
                            dao.saveProfile(newProf)
                            onForceUpdate()
                            showQrisDialog = false
                        }
                    }) { Text(AppStr.save) }
                },
                dismissButton = {
                    KumaTextButton(onClick = { showQrisDialog = false }) { Text(AppStr.cancelBtn) }
                }
            )
        }

        if (showTermsDialog) {
            AlertDialog(
                onDismissRequest = { showTermsDialog = false },
                modifier = Modifier.glassCard(24.dp, AppSurface()),
                containerColor = if (LocalIsLiquidGlass.current) androidx.compose.ui.graphics.Color.Transparent else AppSurface(),
                title = { Text(AppStr.trms, fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(AppStr.termDesc)
                    }
                },
                confirmButton = { KumaTextButton(onClick = { showTermsDialog = false }) { Text(AppStr.agree) } },
                
                
            )
        }

        Spacer(modifier = Modifier.height(50.dp))

        val easterEggEmoji = when (currentProfile.themeMode) {
            3, 4 -> " \uD83C\uDFF3\uFE0F\u200D\uD83C\uDF08"
            5, 6 -> " \uD83D\uDC3B"
            else -> ""
        }

        Text(
            text = "KumaFlow ${AppStr.VERSION}$easterEggEmoji\nLocal Data Only & Privacy First",
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            textAlign = TextAlign.Center,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = AppText().copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding() + 24.dp))
    }

    if (isRestoring) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                color = AppPrimary(),
                modifier = Modifier.size(64.dp)
            )
        }
    }

    if (showRestoreCompleteNote) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRestoreCompleteNote = false },
            modifier = Modifier.glassCard(24.dp, AppSurface()),
            containerColor = if (LocalIsLiquidGlass.current) androidx.compose.ui.graphics.Color.Transparent else AppSurface(),
            title = { Text(if(AppStr.isId) "Restore Selesai" else "Restore Complete", fontWeight = FontWeight.Bold, color = AppText()) },
            text = {
                Text(
                    text = if(AppStr.isId) "Data berhasil dipulihkan.\n\nCatatan: Koneksi Duo Sync tidak di-backup karena alasan keamanan. Jika sebelumnya Anda terhubung dengan partner, silakan lakukan Pairing ulang di menu Duo Sync."
                           else "Data successfully restored.\n\nNote: Duo Sync pairings are not backed up for security reasons. If you were connected to a partner, please re-pair your devices in the Duo Sync menu.",
                    color = AppText().copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                com.bearbones.kumaflow.ui.components.KumaButton(
                    onClick = { showRestoreCompleteNote = false }
                ) { Text(if(AppStr.isId) "Mengerti" else "Got it", color = androidx.compose.ui.graphics.Color.White) }
            }
        )
    }
}
}





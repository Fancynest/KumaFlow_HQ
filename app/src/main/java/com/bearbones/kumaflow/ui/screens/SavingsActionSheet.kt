package com.bearbones.kumaflow.ui.screens

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import com.bearbones.kumaflow.R
import com.bearbones.kumaflow.KumaTransaction
import com.bearbones.kumaflow.TransactionDao
import com.bearbones.kumaflow.UserProfile
import com.bearbones.kumaflow.AppPrimary
import com.bearbones.kumaflow.AppSurface
import com.bearbones.kumaflow.AppText
import com.bearbones.kumaflow.AppStr
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsActionSheet(
    goalName: String,
    profile: UserProfile,
    walletBalances: Map<String, Long>,
    dao: TransactionDao,
    initialIsAdding: Boolean = true,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isId = AppStr.isId
    val locale = Locale.getDefault()

    val isAdding = initialIsAdding
    var amountRaw by remember { mutableStateOf("") }

    val amountFormatted = remember(amountRaw) {
        val digits = amountRaw.filter { it.isDigit() }
        val long = digits.toLongOrNull() ?: 0L
        if (long > 0) NumberFormat.getInstance(locale).format(long) else ""
    }

    val mainWallets = remember(profile.wallets) { profile.wallets.split(",").filter { it.isNotBlank() } }
    var selectedWallet by remember(mainWallets) { mutableStateOf(mainWallets.firstOrNull() ?: "") }
    var showWalletDropdown by remember { mutableStateOf(false) }

    val curSym = if (profile.currency == "IDR") "Rp" else profile.currency
    val currentSavingsBal = walletBalances[goalName] ?: 0L
    val currentMainBal = walletBalances[selectedWallet] ?: 0L

    val sourceLabel = if (isAdding) selectedWallet else goalName
    val sourceBal = if (isAdding) currentMainBal else currentSavingsBal
    val destLabel = if (isAdding) goalName else selectedWallet
    val destBal = if (isAdding) currentSavingsBal else currentMainBal

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
            // Title
            Text(
                text = if (isAdding) {
                    if (isId) "Tambah Dana" else "Add Funds"
                } else {
                    if (isId) "Tarik Dana" else "Withdraw Funds"
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = AppText()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Transfer flow card (Source → Destination)
            val borderColor = AppText().copy(alpha = 0.15f)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppSurface()),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Source row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (isAdding) Modifier.clip(RoundedCornerShape(8.dp)).background(Color.Transparent).clickable { showWalletDropdown = true }.padding(0.dp) else Modifier)
                            .let { mod -> if (isAdding) mod.padding(0.dp) else mod },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AppPrimary().copy(alpha = 0.2f))
                                .then(if (isAdding) Modifier.padding(0.dp) else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(sourceLabel.take(1).uppercase(), fontWeight = FontWeight.Bold, color = AppPrimary(), fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(
                            modifier = Modifier.weight(1f)
                                .then(if (isAdding) Modifier.clip(RoundedCornerShape(4.dp)).background(Color.Transparent).padding(0.dp) else Modifier)
                        ) {
                            Text(sourceLabel, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AppText())
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("$curSym ${NumberFormat.getInstance(locale).format(sourceBal)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppText())
                            Text(if (isId) "Saldo" else "Balance", fontSize = 12.sp, color = AppText().copy(alpha = 0.5f))
                        }
                    }

                    // Wallet dropdown for source (Add mode) or destination (Withdraw mode)
                    if (isAdding) {
                        DropdownMenu(
                            expanded = showWalletDropdown,
                            onDismissRequest = { showWalletDropdown = false }
                        ) {
                            mainWallets.forEach { w ->
                                DropdownMenuItem(
                                    text = { Text(w) },
                                    onClick = { selectedWallet = w; showWalletDropdown = false }
                                )
                            }
                        }
                    }

                    // Dashed line connector
                    val lineColor = AppText().copy(alpha = 0.2f)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .padding(start = 19.dp)
                            .width(2.dp)
                            .height(24.dp)
                            .drawBehind {
                                val dashHeight = 4.dp.toPx()
                                val gapHeight = 4.dp.toPx()
                                var y = 0f
                                while (y < size.height) {
                                    drawLine(
                                        color = lineColor,
                                        start = Offset(size.width / 2, y),
                                        end = Offset(size.width / 2, (y + dashHeight).coerceAtMost(size.height)),
                                        strokeWidth = 2.dp.toPx()
                                    )
                                    y += dashHeight + gapHeight
                                }
                            }
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Destination row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (!isAdding) Modifier.clip(RoundedCornerShape(8.dp)).background(Color.Transparent).clickable { showWalletDropdown = true }.padding(0.dp) else Modifier)
                            .let { mod -> if (!isAdding) mod.padding(0.dp) else mod },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AppPrimary().copy(alpha = 0.2f))
                                .then(if (!isAdding) Modifier.padding(0.dp) else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(destLabel.take(1).uppercase(), fontWeight = FontWeight.Bold, color = AppPrimary(), fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(
                            modifier = Modifier.weight(1f)
                                .then(if (!isAdding) Modifier.clip(RoundedCornerShape(4.dp)).background(Color.Transparent).padding(0.dp) else Modifier)
                        ) {
                            Text(destLabel, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AppText())
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("$curSym ${NumberFormat.getInstance(locale).format(destBal)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppText())
                            Text(if (isId) "Saldo" else "Balance", fontSize = 12.sp, color = AppText().copy(alpha = 0.5f))
                        }
                    }

                    if (!isAdding) {
                        DropdownMenu(
                            expanded = showWalletDropdown,
                            onDismissRequest = { showWalletDropdown = false }
                        ) {
                            mainWallets.forEach { w ->
                                DropdownMenuItem(
                                    text = { Text(w) },
                                    onClick = { selectedWallet = w; showWalletDropdown = false }
                                )
                            }
                        }
                    }
                }
            }

            // Make the source/dest rows tappable to change wallet
            // We handle this by making the whole card area respond
            androidx.compose.runtime.LaunchedEffect(Unit) {
                // no-op, dropdown is triggered by the text
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Amount Input
            OutlinedTextField(
                value = amountRaw,
                onValueChange = { newVal ->
                    amountRaw = newVal.filter { it.isDigit() }
                },
                prefix = { Text("$curSym ", color = AppText().copy(alpha = 0.6f)) },
                label = { Text(if (isId) "Jumlah" else "Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                visualTransformation = com.bearbones.kumaflow.ThousandSeparatorTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppPrimary(),
                    unfocusedBorderColor = AppText().copy(alpha = 0.3f),
                    focusedTextColor = AppText(),
                    unfocusedTextColor = AppText(),
                    cursorColor = AppPrimary()
                )
            )

            val relevantBal = if (isAdding) currentMainBal else currentSavingsBal
            val relevantLabel = if (isAdding) selectedWallet else goalName
            Text(
                text = "${if (isId) "Saldo" else "Balance"} $relevantLabel: $curSym ${NumberFormat.getInstance(locale).format(relevantBal)}",
                color = AppText().copy(alpha = 0.5f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Confirm Button
            Button(
                onClick = {
                    val amtLong = amountRaw.filter { it.isDigit() }.toLongOrNull() ?: 0L
                    if (amtLong <= 0) {
                        Toast.makeText(context, if (isId) "Nominal tidak valid" else "Invalid amount", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (isAdding && amtLong > currentMainBal) {
                        Toast.makeText(context, if (isId) "Saldo $selectedWallet tidak cukup" else "$selectedWallet balance insufficient", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (!isAdding && amtLong > currentSavingsBal) {
                        Toast.makeText(context, if (isId) "Saldo tabungan tidak cukup" else "Savings balance insufficient", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    scope.launch {
                        val dt = LocalDateTime.now()
                        val dateStr = dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        val timeStr = dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        val title = if (isAdding) {
                            if (isId) "Isi Tabungan $goalName" else "Deposit to $goalName"
                        } else {
                            if (isId) "Tarik Tabungan $goalName" else "Withdraw from $goalName"
                        }

                        val sourceWallet = if (isAdding) selectedWallet else goalName
                        val destWallet = if (isAdding) goalName else selectedWallet

                        val txOut = KumaTransaction(id = 0, name = title, date = dateStr, amount = amtLong.toString(), isIncome = false, category = "Transfer", wallet = sourceWallet, timestamp = timeStr)
                        val txIn = KumaTransaction(id = 0, name = title, date = dateStr, amount = amtLong.toString(), isIncome = true, category = "Transfer", wallet = destWallet, timestamp = timeStr)

                        dao.insertFullTransaction(txOut, emptyList<com.bearbones.kumaflow.TransactionSplit>())
                        dao.insertFullTransaction(txIn, emptyList<com.bearbones.kumaflow.TransactionSplit>())

                        sendSavingsNotification(context, isAdding, goalName, amtLong, curSym)
                        com.bearbones.kumaflow.updateKumaWidget(context)

                        sheetState.hide()
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppPrimary())
            ) {
                Text(
                    if (isId) "Lanjut" else "Continue",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

fun sendSavingsNotification(context: Context, isAdding: Boolean, goalName: String, amount: Long, curSym: String) {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "kuma_savings"
    val isId = AppStr.isId

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channelName = if (isId) "Update Tabungan" else "Savings Updates"
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
        nm.createNotificationChannel(channel)
    }

    val amtStr = NumberFormat.getInstance(Locale.getDefault()).format(amount)
    val title = if (isAdding) {
        if (isId) "Kamu Berhasil Menambahkan Dana" else "Funds Added Successfully"
    } else {
        if (isId) "Dana Berhasil Ditarik" else "Funds Withdrawn Successfully"
    }
    val text = if (isAdding) {
        if (isId) "Penambahan dana ke $goalName $curSym $amtStr berhasil!" else "Successfully added $curSym $amtStr to $goalName!"
    } else {
        if (isId) "Penarikan dana dari $goalName $curSym $amtStr berhasil!" else "Successfully withdrawn $curSym $amtStr from $goalName!"
    }

    val largeIconBitmap = android.graphics.BitmapFactory.decodeResource(
        context.resources,
        R.drawable.ic_kumaflow_logo
    )

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_notification_small)
        .setLargeIcon(largeIconBitmap)
        .setColor(androidx.core.content.ContextCompat.getColor(context, R.color.kumaflow_notification_accent))
        .setContentTitle(title)
        .setContentText(text)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)

    nm.notify(System.currentTimeMillis().toInt(), builder.build())
    Toast.makeText(context, text, Toast.LENGTH_LONG).show()
}

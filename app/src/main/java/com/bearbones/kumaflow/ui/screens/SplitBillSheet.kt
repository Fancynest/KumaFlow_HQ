@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.bearbones.kumaflow.ui.screens
import com.bearbones.kumaflow.utils.kumaClickable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import com.bearbones.kumaflow.ui.components.KumaExpressiveIcon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.bearbones.kumaflow.getGlassTextFieldColors
import com.bearbones.kumaflow.glassCard
import com.bearbones.kumaflow.utils.bouncySheetContent
import com.bearbones.kumaflow.utils.ShareSplitBillUtils
import java.text.NumberFormat
import java.util.Locale
import com.bearbones.kumaflow.ui.components.KumaIconButton
import com.bearbones.kumaflow.ui.components.KumaTextButton

@Composable
fun SplitBillSheet(
    viewModel: SplitBillViewModel,
    qrisFilePath: String,
    holderName: String,
    bankName: String,
    bankAccount: String,
    onDismissRequest: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val format = NumberFormat.getNumberInstance(Locale.getDefault())

    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var previewAmountStr by remember { mutableStateOf("") }

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
        ) {
            Text(
                text = AppStr.splitBillCalc,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AppText(),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Total Bill Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(12.dp, AppPrimary().copy(alpha = 0.1f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(AppStr.totalBill, fontSize = 12.sp, color = AppText().copy(alpha = 0.7f))
                    Text(
                        text = "Rp ${format.format(state.totalBill)}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppPrimary()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppSurfaceVariant()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .kumaClickable { viewModel.setMode(SplitMode.SAMA_RATA) }
                        .background(if (state.mode == SplitMode.SAMA_RATA) AppPrimary() else AppSurfaceVariant())
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        AppStr.modeEqual,
                        color = if (state.mode == SplitMode.SAMA_RATA) AppSurface() else AppText(),
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .kumaClickable { viewModel.setMode(SplitMode.TAHU_DIRI) }
                        .background(if (state.mode == SplitMode.TAHU_DIRI) AppPrimary() else AppSurfaceVariant())
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        AppStr.modeCustom,
                        color = if (state.mode == SplitMode.TAHU_DIRI) AppSurface() else AppText(),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Common Inputs
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.bearbones.kumaflow.ui.components.KumaOutlinedTextField(
                    value = state.numberOfPeople,
                    onValueChange = { viewModel.setNumberOfPeople(it) },
                    label = { Text(AppStr.totalPeople) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = getGlassTextFieldColors(),
                    modifier = Modifier.weight(1f)
                )

                if (state.mode == SplitMode.TAHU_DIRI) {
                    com.bearbones.kumaflow.ui.components.KumaOutlinedTextField(
                        value = state.taxPercentage,
                        onValueChange = { viewModel.setTaxPercentage(it) },
                        label = { Text(AppStr.taxPct) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = getGlassTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (state.mode == SplitMode.SAMA_RATA) {
                val finalAmount = viewModel.calculateEqualSplit()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard(12.dp, AppSurfaceVariant())
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(AppStr.eachPays, fontSize = 14.sp, color = AppText().copy(alpha = 0.7f))
                        Text(
                            text = "Rp ${format.format(finalAmount)}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppText()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                com.bearbones.kumaflow.ui.components.KumaButton(
                    onClick = {
                        val uri = ShareSplitBillUtils.generateQRWithText(context, qrisFilePath, holderName, "Rp ${format.format(finalAmount)}", finalAmount)
                        previewUri = uri
                        previewAmountStr = format.format(finalAmount)
                        showPreviewDialog = true
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppPrimary())
                ) {
                    KumaExpressiveIcon(Icons.Default.Share, contentDescription = null, containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(AppStr.previewQris)
                }
            } else {
                // Tahu Diri Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(AppStr.customItems, fontWeight = FontWeight.Bold, color = AppText())
                    KumaIconButton(onClick = { viewModel.addCustomItem() }) {
                        KumaExpressiveIcon(Icons.Default.Add, contentDescription = "Add", tint = AppPrimary(), containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    }
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(state.customItems) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            com.bearbones.kumaflow.ui.components.KumaOutlinedTextField(
                                value = item.name,
                                onValueChange = { viewModel.updateCustomItemName(item.id, it) },
                                placeholder = { Text(AppStr.name) },
                                colors = getGlassTextFieldColors(),
                                modifier = Modifier.weight(1f)
                            )
                            com.bearbones.kumaflow.ui.components.KumaOutlinedTextField(
                                value = if (item.price == 0L) "" else item.price.toString(),
                                onValueChange = { viewModel.updateCustomItemPrice(item.id, it) },
                                placeholder = { Text(AppStr.pricePreTax) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = getGlassTextFieldColors(),
                                modifier = Modifier.weight(1f)
                            )
                            KumaIconButton(onClick = { viewModel.removeCustomItem(item.id) }) {
                                KumaExpressiveIcon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, containerColor = androidx.compose.ui.graphics.Color.Transparent)
                            }
                        }
                    }

                    item {
                        val result = viewModel.calculateTahuDiriSplit()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 16.dp)
                                .glassCard(12.dp, AppSurfaceVariant())
                                .padding(16.dp)
                        ) {
                            Text(AppStr.resultAfterTax, fontWeight = FontWeight.Bold, color = AppText(), modifier = Modifier.padding(bottom = 8.dp))
                            
                            result.itemizedShares.forEach { (name, amount) ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(name, color = AppText())
                                    Text("Rp ${format.format(amount)}", fontWeight = FontWeight.Bold, color = AppPrimary())
                                    KumaIconButton(
                                        onClick = {
                                            val uri = ShareSplitBillUtils.generateQRWithText(context, qrisFilePath, holderName, "Rp ${format.format(amount)}", amount)
                                            previewUri = uri
                                            previewAmountStr = format.format(amount)
                                            showPreviewDialog = true
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        KumaExpressiveIcon(Icons.Default.Share, contentDescription = "Share", size = 20.dp, iconPadding = 2.dp, tint = AppPrimary(), containerColor = androidx.compose.ui.graphics.Color.Transparent)
                                    }
                                }
                            }

                            if (result.remainingPeopleCount > 0) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${AppStr.remaining} (${result.remainingPeopleCount} ${AppStr.splitBillOrg})", color = AppText())
                                    Text("Rp ${format.format(result.remainingPerPerson)} /${AppStr.splitBillOrg}", fontWeight = FontWeight.Bold, color = AppPrimary())
                                    KumaIconButton(
                                        onClick = {
                                            val uri = ShareSplitBillUtils.generateQRWithText(context, qrisFilePath, holderName, "Rp ${format.format(result.remainingPerPerson)}", result.remainingPerPerson)
                                            previewUri = uri
                                            previewAmountStr = format.format(result.remainingPerPerson)
                                            showPreviewDialog = true
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        KumaExpressiveIcon(Icons.Default.Share, contentDescription = "Share", size = 20.dp, iconPadding = 2.dp, tint = AppPrimary(), containerColor = androidx.compose.ui.graphics.Color.Transparent)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPreviewDialog) {
        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            modifier = Modifier.glassCard(24.dp, AppSurface()),
            containerColor = if (com.bearbones.kumaflow.LocalIsLiquidGlass.current) androidx.compose.ui.graphics.Color.Transparent else AppSurface(),
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
                com.bearbones.kumaflow.ui.components.KumaButton(
                    onClick = {
                        ShareSplitBillUtils.shareBillingDetails(context, previewUri, previewAmountStr, holderName, bankName, bankAccount)
                        showPreviewDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppPrimary())
                ) {
                    KumaExpressiveIcon(Icons.Default.Share, contentDescription = null, containerColor = androidx.compose.ui.graphics.Color.Transparent)
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


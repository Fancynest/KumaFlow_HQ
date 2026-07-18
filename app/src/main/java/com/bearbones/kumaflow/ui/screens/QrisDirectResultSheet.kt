package com.bearbones.kumaflow.ui.screens

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumaflow.AppBg
import com.bearbones.kumaflow.AppPrimary
import com.bearbones.kumaflow.AppSurface
import com.bearbones.kumaflow.AppSurfaceVariant
import com.bearbones.kumaflow.AppText
import com.bearbones.kumaflow.ui.components.KumaButton
import com.bearbones.kumaflow.ui.components.KumaIconButton
import com.bearbones.kumaflow.glassCard
import com.bearbones.kumaflow.utils.DynamicQrisUtils
import com.bearbones.kumaflow.utils.ShareSplitBillUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.util.Locale
import com.bearbones.kumaflow.AppStr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrisDirectResultSheet(
    qrisFilePath: String,
    holderName: String,
    bankName: String,
    bankAccount: String,
    amount: Long,
    message: String,
    onDismissRequest: () -> Unit,
    onEditNominal: () -> Unit
) {
    val context = LocalContext.current
    var qrBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isError by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }

    val format = NumberFormat.getInstance(Locale.GERMANY)

    LaunchedEffect(qrisFilePath, amount, message) {
        withContext(Dispatchers.IO) {
            loading = true
            isError = false
            try {
                if (amount > 0) {
                    val file = File(qrisFilePath)
                    if (file.exists()) {
                        val decoded = DynamicQrisUtils.decodeQRImage(context, Uri.fromFile(file))
                        if (decoded != null && decoded.payload.isNotEmpty()) {
                            val dynPayload = DynamicQrisUtils.generateDynamicQrisString(decoded.payload, amount, message)
                            if (dynPayload != null) {
                                qrBitmap = DynamicQrisUtils.encodeDynamicQris(dynPayload, 600)
                            } else {
                                isError = true
                            }
                        } else {
                            isError = true
                        }
                    } else {
                        isError = true
                    }
                } else {
                    // Static QRIS
                    val fileUri = Uri.fromFile(java.io.File(qrisFilePath))
                    val decodedQris = DynamicQrisUtils.decodeQRImage(context, fileUri)
                    if (decodedQris != null) {
                        qrBitmap = DynamicQrisUtils.encodeDynamicQris(decodedQris.payload, 512)
                    }
                    
                    if (qrBitmap == null) {
                        val file = java.io.File(qrisFilePath)
                        if (file.exists()) {
                            qrBitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                        } else {
                            isError = true
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isError = true
            }
            loading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppBg(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "QRIS",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppText()
                )
                KumaIconButton(onClick = onDismissRequest) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = AppText())
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // User Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(16.dp, AppSurface())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(holderName, fontWeight = FontWeight.Black, fontSize = 16.sp, color = AppText(), textAlign = TextAlign.Center)
                Text(bankName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppText().copy(alpha = 0.8f), textAlign = TextAlign.Center)
                Text(bankAccount, fontSize = 12.sp, color = AppText().copy(alpha = 0.6f), textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // QR Code
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(24.dp, AppSurface())
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (loading) {
                        CircularProgressIndicator(color = AppPrimary())
                    } else if (isError || qrBitmap == null) {
                        Text(if (AppStr.isId) "Gagal memuat QRIS." else "Failed to load QRIS.", color = Color.Red)
                    } else {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "QRIS",
                            modifier = Modifier.size(250.dp).clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Surface(
                            color = AppSurfaceVariant(),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.clickable { onEditNominal() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = AppText(), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (AppStr.isId) "Buat tagihan QRIS pakai nominal" else "Create QRIS billing with amount", color = AppText(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            KumaButton(
                onClick = { 
                    if (qrBitmap != null) {
                        try {
                            val cacheFile = java.io.File(context.cacheDir, "kuma_qris_share.jpg")
                            val out = java.io.FileOutputStream(cacheFile)
                            qrBitmap!!.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                            out.flush()
                            out.close()
                            
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                cacheFile
                            )
                            val amtStr = if (amount > 0) "Total: Rp ${format.format(amount)}" else ""
                            ShareSplitBillUtils.shareToWhatsApp(context, uri, amtStr, holderName, bankName, bankAccount)
                        } catch(e: Exception) {
                            e.printStackTrace()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(0.6f).height(55.dp),
                enabled = qrBitmap != null
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (AppStr.isId) "Bagikan" else "Share", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            }
        }
    }
}

package com.bearbones.kumaflow.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.bearbones.kumaflow.UserProfile
import com.bearbones.kumaflow.KumaDatabase
import com.bearbones.kumaflow.duo.DuoPairingManager
import com.bearbones.kumaflow.duo.model.DuoPairing
import com.bearbones.kumaflow.duo.model.WalletMetadata
import com.bearbones.kumaflow.utils.DuoServerListener
import com.bearbones.kumaflow.utils.QrTransferServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuoPairingScreen(
    onBack: () -> Unit,
    profile: UserProfile,
    database: KumaDatabase
) {
    val context = LocalContext.current
    var isSender by remember { mutableStateOf(true) }
    var successfulPairing by remember { mutableStateOf<DuoPairing?>(null) }
    
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(context.getString(com.bearbones.kumaflow.R.string.duo_pairing_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (successfulPairing == null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(
                        onClick = { isSender = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSender) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSender) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(context.getString(com.bearbones.kumaflow.R.string.duo_btn_show_qr))
                    }
                    Button(
                        onClick = { isSender = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isSender) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (!isSender) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(context.getString(com.bearbones.kumaflow.R.string.duo_btn_scan_qr))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isSender) {
                    DuoSenderView(profile, database) { pairing ->
                        successfulPairing = pairing
                    }
                } else {
                    DuoReceiverView(profile, database) { pairing ->
                        successfulPairing = pairing
                    }
                }
            } else {
                DuoWalletSelectionView(
                    pairing = successfulPairing!!,
                    database = database,
                    onComplete = onBack
                )
            }
        }
    }
}

@Composable
fun DuoSenderView(profile: UserProfile, database: KumaDatabase, onPaired: (DuoPairing) -> Unit) {
    val context = LocalContext.current
    val pairingManager = remember { DuoPairingManager(context, database) }
    var server by remember { mutableStateOf<QrTransferServer?>(null) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var errorMsg by remember { mutableStateOf("") }
    
    DisposableEffect(Unit) {
        val newServer = QrTransferServer(8081)
         newServer.duoListener = object : DuoServerListener {
            override fun onHandshakeRequest(payloadJson: String): Pair<Int, String> {
                val res = pairingManager.handleHandshakeRequest(payloadJson)
                if (res.first == 200) {
                    // Create pairing instance based on handshake
                    try {
                        val reqObj = org.json.JSONObject(payloadJson)
                        val partnerDeviceId = reqObj.getString("deviceId")
                        val partnerDisplayName = reqObj.getString("displayName")
                        val secret = com.bearbones.kumaflow.duo.DuoSecureStorage(context).getPairingSecret("temp_pairing") ?: ""
                        // Use the SAME pairingId that was embedded in the QR code
                        val pairingId = context.getSharedPreferences("duo_prefs", android.content.Context.MODE_PRIVATE)
                            .getString("temp_pairing_id", null) ?: java.util.UUID.randomUUID().toString()
                        
                        val pairing = DuoPairing(
                            pairingId = pairingId,
                            partnerDeviceId = partnerDeviceId,
                            partnerDisplayName = partnerDisplayName,
                            pairingSecret = secret,
                            sharedWalletStableId = "",
                            pairedAt = System.currentTimeMillis()
                        )
                        com.bearbones.kumaflow.duo.DuoSecureStorage(context).savePairingSecret(pairing.pairingId, secret)
                        
                        // Wait for UI to navigate to Wallet Selection
                        kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
                            onPaired(pairing)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                return res
            }

            override fun onSyncRequest(payloadJson: String, signature: String): Pair<Int, String> {
                return Pair(400, "{\"error\": \"Not expected in pairing mode\"}")
            }
        }
        
        try {
            newServer.start()
            server = newServer
        } catch (e: Exception) {
            errorMsg = context.getString(com.bearbones.kumaflow.R.string.duo_err_server_start)
        }
        onDispose {
            server?.stop()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            withContext(Dispatchers.IO) {
                try {
                    val ip = pairingManager.getLocalIpAddress()
                    if (ip == null || ip == "127.0.0.1") {
                        errorMsg = context.getString(com.bearbones.kumaflow.R.string.duo_err_connect_wifi)
                    } else {
                        errorMsg = ""
                        val payload = pairingManager.generatePairingPayload(profile.userName)
                        val bmp = generateQrBitmap(payload)
                        withContext(Dispatchers.Main) {
                            qrBitmap = bmp
                        }
                    }
                } catch (e: Exception) {
                    errorMsg = context.getString(com.bearbones.kumaflow.R.string.duo_err_gen_qr)
                }
            }
            delay(15000)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (errorMsg.isNotEmpty()) {
            Text(errorMsg, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        } else {
            Text(context.getString(com.bearbones.kumaflow.R.string.duo_msg_ask_partner_scan), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            qrBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier
                        .size(250.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(context.getString(com.bearbones.kumaflow.R.string.duo_msg_waiting_conn), color = Color.Gray)
        }
    }
}

@Composable
fun DuoReceiverView(profile: UserProfile, database: KumaDatabase, onPaired: (DuoPairing) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val pairingManager = remember { DuoPairingManager(context, database) }
    
    var hasCamPermission by remember { 
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED)
    }
    var isPairing by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCamPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCamPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    if (isPairing) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(context.getString(com.bearbones.kumaflow.R.string.duo_msg_pairing_handshake))
    } else if (hasCamPermission) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(24.dp))
        ) {
            val boxWidth = maxWidth
            val boxHeight = maxHeight
            val holeSizeDp = minOf(boxWidth, boxHeight) * 0.7f

            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    startCamera(ctx, lifecycleOwner, previewView) { payload ->
                        if (!isPairing && payload.contains("secret")) {
                            isPairing = true
                            kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
                                val result = pairingManager.processScannedQrAndHandshake(payload, profile.userName)
                                if (result.isSuccess) {
                                    Toast.makeText(ctx, context.getString(com.bearbones.kumaflow.R.string.duo_msg_pairing_success), Toast.LENGTH_SHORT).show()
                                    onPaired(result.getOrNull()!!)
                                } else {
                                    isPairing = false
                                    Toast.makeText(ctx, result.exceptionOrNull()?.message ?: context.getString(com.bearbones.kumaflow.R.string.duo_msg_pairing_failed), Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val holeSizePx = holeSizeDp.toPx()
                val cornerRadius = 32.dp.toPx()
                val rectTopLeft = androidx.compose.ui.geometry.Offset((canvasWidth - holeSizePx) / 2f, (canvasHeight - holeSizePx) / 2f)

                // Draw darkened background
                val bgPath = androidx.compose.ui.graphics.Path().apply {
                    addRect(androidx.compose.ui.geometry.Rect(0f, 0f, canvasWidth, canvasHeight))
                    addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            rect = androidx.compose.ui.geometry.Rect(rectTopLeft, androidx.compose.ui.geometry.Size(holeSizePx, holeSizePx)),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
                        )
                    )
                    fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
                }
                drawPath(bgPath, Color.Black.copy(alpha = 0.65f))

                // Draw brackets
                val bracketLength = 48.dp.toPx()
                val strokeWidth = 4.dp.toPx()
                val bracketColor = Color.White

                // Top Left Bracket
                val tlPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(rectTopLeft.x, rectTopLeft.y + bracketLength)
                    lineTo(rectTopLeft.x, rectTopLeft.y + cornerRadius)
                    arcTo(androidx.compose.ui.geometry.Rect(rectTopLeft.x, rectTopLeft.y, rectTopLeft.x + cornerRadius * 2, rectTopLeft.y + cornerRadius * 2), 180f, 90f, false)
                    lineTo(rectTopLeft.x + bracketLength, rectTopLeft.y)
                }

                // Top Right Bracket
                val trPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(rectTopLeft.x + holeSizePx - bracketLength, rectTopLeft.y)
                    lineTo(rectTopLeft.x + holeSizePx - cornerRadius, rectTopLeft.y)
                    arcTo(androidx.compose.ui.geometry.Rect(rectTopLeft.x + holeSizePx - cornerRadius * 2, rectTopLeft.y, rectTopLeft.x + holeSizePx, rectTopLeft.y + cornerRadius * 2), 270f, 90f, false)
                    lineTo(rectTopLeft.x + holeSizePx, rectTopLeft.y + bracketLength)
                }

                // Bottom Right Bracket
                val brPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(rectTopLeft.x + holeSizePx, rectTopLeft.y + holeSizePx - bracketLength)
                    lineTo(rectTopLeft.x + holeSizePx, rectTopLeft.y + holeSizePx - cornerRadius)
                    arcTo(androidx.compose.ui.geometry.Rect(rectTopLeft.x + holeSizePx - cornerRadius * 2, rectTopLeft.y + holeSizePx - cornerRadius * 2, rectTopLeft.x + holeSizePx, rectTopLeft.y + holeSizePx), 0f, 90f, false)
                    lineTo(rectTopLeft.x + holeSizePx - bracketLength, rectTopLeft.y + holeSizePx)
                }

                // Bottom Left Bracket
                val blPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(rectTopLeft.x + bracketLength, rectTopLeft.y + holeSizePx)
                    lineTo(rectTopLeft.x + cornerRadius, rectTopLeft.y + holeSizePx)
                    arcTo(androidx.compose.ui.geometry.Rect(rectTopLeft.x, rectTopLeft.y + holeSizePx - cornerRadius * 2, rectTopLeft.x + cornerRadius * 2, rectTopLeft.y + holeSizePx), 90f, 90f, false)
                    lineTo(rectTopLeft.x, rectTopLeft.y + holeSizePx - bracketLength)
                }

                drawPath(tlPath, bracketColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                drawPath(trPath, bracketColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                drawPath(brPath, bracketColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                drawPath(blPath, bracketColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }

            Text(
                context.getString(com.bearbones.kumaflow.R.string.duo_msg_point_cam),
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = -(holeSizeDp / 2 + 30.dp))
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    } else {
        Text(context.getString(com.bearbones.kumaflow.R.string.duo_err_cam_perm))
    }
}

@Composable
fun DuoWalletSelectionView(
    pairing: DuoPairing,
    database: KumaDatabase,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var wallets by remember { mutableStateOf<List<WalletMetadata>>(emptyList()) }
    var selectedWalletIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var syncOldHistory by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val allWallets = database.transactionDao().getOrGenerateAllWalletMetadata()
            withContext(Dispatchers.Main) {
                wallets = allWallets
                // Auto-select the first one if any
                allWallets.firstOrNull()?.let {
                    selectedWalletIds = setOf(it.walletStableId)
                }
            }
        }
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(context.getString(com.bearbones.kumaflow.R.string.duo_title_select_wallet), style = MaterialTheme.typography.titleLarge)
        Text(context.getString(com.bearbones.kumaflow.R.string.duo_lbl_connected_with, pairing.partnerDisplayName.substringBefore("#")), color = Color.Gray)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (wallets.isEmpty()) {
            CircularProgressIndicator()
        } else {
            wallets.forEach { wallet ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = selectedWalletIds.contains(wallet.walletStableId),
                        onCheckedChange = { checked ->
                            if (checked) {
                                selectedWalletIds = selectedWalletIds + wallet.walletStableId
                            } else {
                                selectedWalletIds = selectedWalletIds - wallet.walletStableId
                            }
                        }
                    )
                    Text(wallet.currentName)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = syncOldHistory,
                    onCheckedChange = { syncOldHistory = it }
                )
                Column {
                    Text(context.getString(com.bearbones.kumaflow.R.string.duo_lbl_sync_old_history))
                    Text(context.getString(com.bearbones.kumaflow.R.string.duo_desc_sync_old_history), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    if (selectedWalletIds.isNotEmpty()) {
                        isSaving = true
                        coroutineScope.launch(Dispatchers.IO) {
                            val finalPairing = pairing.copy(
                                sharedWalletStableId = selectedWalletIds.joinToString(","),
                                // If syncOldHistory is false, we set lastSyncedTimestamp to NOW. 
                                // So it won't pick up old transactions.
                                lastSyncedTimestamp = if (syncOldHistory) 0L else System.currentTimeMillis()
                            )
                            database.transactionDao().upsertPairing(finalPairing)
                            withContext(Dispatchers.Main) {
                                onComplete()
                            }
                        }
                    }
                },
                enabled = !isSaving && selectedWalletIds.isNotEmpty()
            ) {
                Text(if (isSaving) context.getString(com.bearbones.kumaflow.R.string.duo_btn_saving) else context.getString(com.bearbones.kumaflow.R.string.duo_btn_done))
            }
        }
    }
}

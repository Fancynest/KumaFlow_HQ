package com.bearbones.kumaflow.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.wifi.WifiManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.bearbones.kumaflow.UserProfile
import com.bearbones.kumaflow.TransactionWithSplits
import com.bearbones.kumaflow.utils.QrTransferServer
import com.bearbones.kumaflow.utils.RestoreUtils
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrTransferScreen(
    onBack: () -> Unit,
    profile: UserProfile,
    allTransactionsWithSplits: List<TransactionWithSplits>
) {
    val context = LocalContext.current
    var isSender by remember { mutableStateOf(true) }

    androidx.activity.compose.BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transfer Data (Local WiFi)") },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(
                    onClick = { isSender = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSender) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isSender) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("Send (Generate QR)")
                }
                Button(
                    onClick = { isSender = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isSender) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (!isSender) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("Receive (Scan QR)")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = "Warning", tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Please use a Personal Hotspot if transferring fails on Public WiFi (due to AP Isolation).",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isSender) {
                SenderView(profile, allTransactionsWithSplits)
            } else {
                ReceiverView(onBack)
            }
        }
    }
}

@Composable
fun SenderView(profile: UserProfile, txs: List<TransactionWithSplits>) {
    val context = LocalContext.current
    var server by remember { mutableStateOf<QrTransferServer?>(null) }
    var qrContent by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var errorMsg by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        val newServer = QrTransferServer(8080)
        try {
            newServer.start()
            server = newServer
        } catch (e: Exception) {
            errorMsg = "Failed to start server: ${e.message}"
        }
        onDispose {
            server?.stop()
        }
    }

    LaunchedEffect(profile, txs, server) {
        if (profile != null && server != null) {
            while (true) {
                withContext(Dispatchers.IO) {
                    try {
                        val ip = getLocalIpAddress(context)
                        if (ip == "127.0.0.1") {
                            errorMsg = "Not connected to WiFi or Hotspot"
                        } else {
                            errorMsg = ""
                            val token = java.util.UUID.randomUUID().toString()
                            server?.updateTokenAndData(token, profile, txs)
                            val url = "http://$ip:8080/download-kuma?token=$token"
                            val bmp = generateQrBitmap(url)
                            withContext(Dispatchers.Main) {
                                qrContent = url
                                qrBitmap = bmp
                            }
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        errorMsg = "Error generating QR: ${e.message}"
                    }
                }
                delay(15000) // Refresh token every 15s
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (errorMsg.isNotEmpty()) {
            Text(errorMsg, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        } else {
            Text("Scan this QR code from your new device.", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            qrBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier
                        .size(250.dp)
                        .background(androidx.compose.ui.graphics.Color.White, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("QR refreshes automatically every 15s for security.", style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color.Gray)
        }
    }
}

@Composable
fun ReceiverView(onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCamPermission by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
    ) }
    var isDownloading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCamPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCamPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    if (isDownloading) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text("Downloading and restoring data...", style = MaterialTheme.typography.bodyLarge)
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
                    startCamera(ctx, lifecycleOwner, previewView) { url ->
                        if (!isDownloading && url.startsWith("http")) {
                            isDownloading = true
                            downloadAndRestore(url, ctx) { success, msg ->
                                isDownloading = false
                                Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
                                if (success) {
                                    onBack()
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
                drawPath(bgPath, androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.65f))

                // Draw brackets
                val bracketLength = 48.dp.toPx()
                val strokeWidth = 4.dp.toPx()
                val bracketColor = androidx.compose.ui.graphics.Color.White

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
                "Point your camera at the Sender's QR Code",
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = -(holeSizeDp / 2 + 30.dp))
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = androidx.compose.ui.graphics.Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    } else {
        Text("Camera permission is required to scan QR.")
    }
}

fun getLocalIpAddress(context: Context): String {
    try {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val ip = wifiManager?.connectionInfo?.ipAddress ?: 0
        if (ip != 0) {
            return String.format(
                "%d.%d.%d.%d",
                ip and 0xff,
                ip shr 8 and 0xff,
                ip shr 16 and 0xff,
                ip shr 24 and 0xff
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return "127.0.0.1"
}

fun generateQrBitmap(content: String): Bitmap {
    val size = 512
    val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bmp.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
        }
    }
    return bmp
}

@OptIn(ExperimentalGetImage::class)
fun startCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    onQrScanned: (String) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val barcodeScanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                barcodeScanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            barcode.rawValue?.let { onQrScanned(it) }
                        }
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
        
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }, ContextCompat.getMainExecutor(context))
}

fun downloadAndRestore(url: String, context: Context, onComplete: (Boolean, String) -> Unit) {
    val request = Request.Builder().url(url).build()
    OkHttpClient().newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            CoroutineScope(Dispatchers.Main).launch {
                onComplete(false, "Network error: ${e.message}")
            }
        }
        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                val json = response.body?.string()
                if (json != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            RestoreUtils.parseAndRestoreJson(json, context)
                            withContext(Dispatchers.Main) {
                                onComplete(true, "Data successfully restored!")
                            }
                        } catch(e: Exception) {
                            withContext(Dispatchers.Main) {
                                onComplete(false, "Restore failed: ${e.message}")
                            }
                        }
                    }
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        onComplete(false, "Empty file received")
                    }
                }
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    onComplete(false, "Server error ${response.code}")
                }
            }
        }
    })
}

package com.bearbones.kumaflow.ui.screens

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.bearbones.kumaflow.AppStr
import com.bearbones.kumaflow.nfc.CardInfo
import com.bearbones.kumaflow.nfc.CardParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcReaderSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    
    val sharedPrefs = remember { context.getSharedPreferences("kumaflow_prefs", Context.MODE_PRIVATE) }
    var hideBalance by remember { mutableStateOf(sharedPrefs.getBoolean("nfc_hide_balance", false)) }
    
    var nfcStatus by remember { mutableStateOf(if (AppStr.isId) "Menunggu kartu..." else "Waiting for card...") }
    var cardInfo by remember { mutableStateOf<CardInfo?>(null) }
    var isReading by remember { mutableStateOf(false) }

    val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }

    DisposableEffect(lifecycleOwner) {
        val activity = context as? Activity
        
        val readerCallback = NfcAdapter.ReaderCallback { tag ->
            coroutineScope.launch(Dispatchers.Main) {
                isReading = true
                nfcStatus = if (AppStr.isId) "Membaca kartu..." else "Reading card..."
                
                withContext(Dispatchers.IO) {
                    val info = CardParser.parseCard(tag)
                    
                    withContext(Dispatchers.Main) {
                        cardInfo = info
                        nfcStatus = if (AppStr.isId) "Berhasil dibaca!" else "Card read successfully!"
                        isReading = false
                        // Haptic feedback
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                }
            }
        }

        val enableReaderMode = {
            if (activity != null && nfcAdapter != null && nfcAdapter.isEnabled) {
                val flags = NfcAdapter.FLAG_READER_NFC_A or 
                            NfcAdapter.FLAG_READER_NFC_B or 
                            NfcAdapter.FLAG_READER_NFC_F or 
                            NfcAdapter.FLAG_READER_NFC_V or
                            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
                
                nfcAdapter.enableReaderMode(activity, readerCallback, flags, Bundle())
            }
        }

        val disableReaderMode = {
            if (activity != null && nfcAdapter != null) {
                nfcAdapter.disableReaderMode(activity)
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                enableReaderMode()
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                disableReaderMode()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            enableReaderMode()
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            disableReaderMode()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "NFC Reader",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (nfcAdapter == null) {
                Text(if (AppStr.isId) "Perangkat ini tidak memiliki fitur NFC." else "This device does not have an NFC feature.", color = MaterialTheme.colorScheme.error)
            } else if (!nfcAdapter.isEnabled) {
                Text(if (AppStr.isId) "Mohon nyalakan NFC di pengaturan HP Anda." else "Please turn on NFC in your phone settings.", color = MaterialTheme.colorScheme.error)
            } else {
                Text(
                    text = nfcStatus,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isReading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (cardInfo != null) {
                    // Physical Card UI Google Wallet Style
                    val cardGradient = when (cardInfo!!.cardType) {
                        "Mandiri e-Money" -> Brush.linearGradient(listOf(Color(0xFFB89B5E), Color(0xFF1E3A8A)))
                        "BCA Flazz" -> Brush.linearGradient(listOf(Color(0xFF005AA9), Color(0xFF003060)))
                        "BNI TapCash" -> Brush.linearGradient(listOf(Color(0xFFF26522), Color(0xFF005E6A)))
                        "BRI Brizzi" -> Brush.linearGradient(listOf(Color(0xFF00529C), Color(0xFF003366)))
                        else -> Brush.linearGradient(listOf(Color.DarkGray, Color.Black))
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.586f) // Standard credit card ratio
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardGradient)
                            .padding(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Top row: Bank Name and Contactless Icon
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = cardInfo!!.cardType,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.Contactless,
                                    contentDescription = "Contactless",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            
                            // Middle: Chip Icon (Placeholder)
                            Box(
                                modifier = Modifier
                                    .size(40.dp, 30.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFD4AF37).copy(alpha = 0.8f))
                            )
                            
                            // Bottom row: Balance only
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (cardInfo!!.balance != null) {
                                        Text(
                                            text = if (hideBalance) "Rp * * * *" else "Rp ${String.format("%,d", cardInfo!!.balance).replace(',', '.')}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 20.sp
                                        )
                                        
                                        Spacer(modifier = Modifier.width(8.dp))
                                        
                                        // Eye icon for hiding balance
                                        IconButton(
                                            onClick = {
                                                hideBalance = !hideBalance
                                                sharedPrefs.edit().putBoolean("nfc_hide_balance", hideBalance).apply()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (hideBalance) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = "Toggle Balance",
                                                tint = Color.White.copy(alpha = 0.7f)
                                            )
                                        }
                                    } else {
                                        // Card detected but balance not readable (encrypted)
                                        Text(
                                            text = if (AppStr.isId) "Terdeteksi ✓" else "Detected ✓",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Scanning Animation Lottie
                    val composition by com.airbnb.lottie.compose.rememberLottieComposition(
                        com.airbnb.lottie.compose.LottieCompositionSpec.RawRes(com.bearbones.kumaflow.R.raw.nfcreader)
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(250.dp, 250.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        com.airbnb.lottie.compose.LottieAnimation(
                            composition = composition,
                            iterations = com.airbnb.lottie.compose.LottieConstants.IterateForever,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

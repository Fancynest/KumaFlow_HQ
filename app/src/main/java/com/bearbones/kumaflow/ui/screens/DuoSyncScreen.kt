package com.bearbones.kumaflow.ui.screens

import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bearbones.kumaflow.KumaDatabase
import com.bearbones.kumaflow.duo.DuoSecureStorage
import com.bearbones.kumaflow.duo.DuoSyncEngine
import com.bearbones.kumaflow.duo.model.DuoPairing
import com.bearbones.kumaflow.duo.DuoPairingManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuoSyncScreen(
    onBack: () -> Unit,
    onNavigateToPairing: () -> Unit,
    database: KumaDatabase
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var activePairing by remember { mutableStateOf<DuoPairing?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSyncing by remember { mutableStateOf(false) }
    var showUnpairDialog by remember { mutableStateOf(false) }
    
    androidx.activity.compose.BackHandler(onBack = onBack)

    val localDeviceId = remember {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: UUID.randomUUID().toString()
    }
    
    var localIp by remember { mutableStateOf<String?>(null) }
    var server by remember { mutableStateOf<com.bearbones.kumaflow.utils.QrTransferServer?>(null) }
    var showIpDialog by remember { mutableStateOf(false) }
    var ipInput by remember { mutableStateOf("") }
    val syncEngine = remember { DuoSyncEngine(database, DuoSecureStorage(context), localDeviceId) }

    fun loadPairing() {
        coroutineScope.launch(Dispatchers.IO) {
            val pairings = database.transactionDao().getActivePairings()
            withContext(Dispatchers.Main) {
                activePairing = pairings.firstOrNull()
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadPairing()
        
        withContext(Dispatchers.IO) {
            localIp = DuoPairingManager(context, database).getLocalIpAddress()
        }
    }
    
    // QrTransferServer is now managed globally by DuoAutoSyncManager in MainActivity.
    // Auto-discovery handles sync seamlessly in the background.
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kuma Duo (Sync)") },
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
            if (isLoading) {
                CircularProgressIndicator()
            } else if (activePairing == null) {
                Spacer(modifier = Modifier.height(32.dp))
                Text("Belum terhubung dengan Kuma Duo", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onNavigateToPairing) {
                    Text("Mulai Pairing Baru")
                }
            } else {
                val pairing = activePairing!!
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Terhubung dengan:", color = Color.Gray)
                        Text(pairing.partnerDisplayName.substringBefore("#"), style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text("Terakhir Sync:", color = Color.Gray)
                        val lastSyncStr = if (pairing.lastSyncedTimestamp == 0L) {
                            "Belum pernah"
                        } else {
                            val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
                            sdf.format(Date(pairing.lastSyncedTimestamp))
                        }
                        Text(lastSyncStr, fontWeight = FontWeight.Bold)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("IP Address Anda:", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        Text(localIp ?: "Tidak terhubung ke WiFi", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = { showIpDialog = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isSyncing
                ) {
                    Text(if (isSyncing) "Sedang Sinkronisasi..." else "Sync Sekarang")
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                OutlinedButton(
                    onClick = { showUnpairDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Putuskan Hubungan (Unpair)")
                }
            }
        }
        
        if (showUnpairDialog && activePairing != null) {
            AlertDialog(
                onDismissRequest = { showUnpairDialog = false },
                title = { Text("Unpair Kuma Duo") },
                text = { Text("Dompet ini akan kembali menjadi dompet personal. Semua riwayat transaksi yang ada tidak akan terhapus. Yakin ingin unpair?") },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                val updated = activePairing!!.copy(isActive = false)
                                database.transactionDao().upsertPairing(updated)
                                DuoSecureStorage(context).deletePairingSecret(activePairing!!.pairingId)
                                withContext(Dispatchers.Main) {
                                    showUnpairDialog = false
                                    loadPairing()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Unpair")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUnpairDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }
        
        if (showIpDialog && activePairing != null) {
            AlertDialog(
                onDismissRequest = { if (!isSyncing) showIpDialog = false },
                title = { Text("Masukkan IP Partner") },
                text = {
                    Column {
                        Text("Masukkan IP Address yang tampil di layar Kuma Duo partner kamu.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = ipInput,
                            onValueChange = { newValue ->
                                val cleaned = newValue.filter { it.isDigit() || it == '.' }.replace(Regex("\\.+"), ".")
                                ipInput = cleaned
                            },
                            label = { Text("Contoh: 192.168.1.5") },
                            singleLine = true,
                            enabled = !isSyncing,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (ipInput.isNotBlank()) {
                                isSyncing = true
                                coroutineScope.launch(Dispatchers.IO) {
                                    val result = syncEngine.startSync(activePairing!!, ipInput.trim())
                                    withContext(Dispatchers.Main) {
                                        isSyncing = false
                                        showIpDialog = false
                                        if (result.isSuccess) {
                                            Toast.makeText(context, "Sync berhasil!", Toast.LENGTH_SHORT).show()
                                            loadPairing()
                                        } else {
                                            Toast.makeText(context, "Sync gagal: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isSyncing
                    ) {
                        Text(if (isSyncing) "Menyinkronkan..." else "Mulai Sync")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showIpDialog = false }, enabled = !isSyncing) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

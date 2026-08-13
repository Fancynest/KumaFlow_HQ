package com.bearbones.kumaflow.duo

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.bearbones.kumaflow.KumaDatabase
import com.bearbones.kumaflow.duo.model.DuoPairing
import com.bearbones.kumaflow.utils.DuoServerListener
import com.bearbones.kumaflow.utils.QrTransferServer
import kotlinx.coroutines.*

class DuoAutoSyncManager(private val context: Context, private val database: KumaDatabase) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var isRunning = false
    private var currentServer: QrTransferServer? = null
    
    private val syncEngine by lazy { 
        val deviceId = android.provider.Settings.Secure.getString(
            context.contentResolver, android.provider.Settings.Secure.ANDROID_ID
        ) ?: java.util.UUID.randomUUID().toString()
        DuoSyncEngine(database, DuoSecureStorage(context), deviceId) 
    }
    
    private val localDeviceId: String by lazy {
        android.provider.Settings.Secure.getString(
            context.contentResolver, android.provider.Settings.Secure.ANDROID_ID
        ) ?: java.util.UUID.randomUUID().toString()
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val lastSyncAttempt = mutableMapOf<String, Long>()
    
    private val SERVICE_TYPE = "_kumaduo._tcp."
    private var myRegisteredServiceName: String? = null
    
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    // We can have a listener to show UI updates if needed
    var onSyncEvent: ((String) -> Unit)? = null
    
    // Resolve lock to prevent IllegalArgumentException when resolving multiple services
    private var isResolving = false

    fun start() {
        if (isRunning) return
        isRunning = true
        scope.launch {
            val dao = database.transactionDao()
            dao.observeActivePairings().collect { pairings ->
                val activePairing = pairings.firstOrNull { it.isActive }
                if (activePairing != null) {
                    if (currentServer == null) {
                        withContext(Dispatchers.Main) {
                            onSyncEvent?.invoke("Duo: Starting sync server for ${activePairing.partnerDisplayName.substringBefore("#")}...")
                        }
                        startServerAndAdvertise(activePairing)
                    }
                    if (discoveryListener == null) {
                        startDiscovery(activePairing)
                    }
                } else {
                    stopServices()
                }
            }
        }
    }

    private fun stopServices() {
        try {
            registrationListener?.let { nsdManager.unregisterService(it) }
        } catch (e: Exception) {}
        try {
            discoveryListener?.let { nsdManager.stopServiceDiscovery(it) }
        } catch (e: Exception) {}
        
        registrationListener = null
        discoveryListener = null
        currentServer?.stop()
        currentServer = null
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false
        stopServices()
        scope.coroutineContext.cancelChildren()
    }

    private fun startServerAndAdvertise(pairing: DuoPairing) {
        try {
            currentServer = QrTransferServer(8080).apply {
                duoListener = object : DuoServerListener {
                    override fun onHandshakeRequest(payloadJson: String): Pair<Int, String> {
                        return Pair(400, "{\"error\": \"Use pairing screen\"}")
                    }

                    override fun onSyncRequest(payloadJson: String, signature: String): Pair<Int, String> {
                        var responseBody = "{\"error\": \"Internal server error\"}"
                        var statusCode = 500
                        runBlocking {
                            val result = syncEngine.processIncomingPayload(payloadJson, signature, pairing)
                            if (result.isSuccess) {
                                val responsePayload = syncEngine.generateSyncPayload(pairing)
                                if (responsePayload != null) {
                                    val responseSignature = com.bearbones.kumaflow.duo.DuoCrypto.generateHmacSignature(responsePayload, pairing.pairingSecret)
                                    val resJson = org.json.JSONObject().apply {
                                        put("payload", responsePayload)
                                        put("signature", responseSignature)
                                    }.toString()
                                    statusCode = 200
                                    responseBody = resJson
                                    
                                    // Update last synced
                                    val updatedP = pairing.copy(lastSyncedTimestamp = System.currentTimeMillis())
                                    database.transactionDao().upsertPairing(updatedP)
                                    
                                    // Debounce subsequent outbound attempts
                                    lastSyncAttempt[pairing.pairingId] = System.currentTimeMillis()
                                    withContext(Dispatchers.Main) {
                                        onSyncEvent?.invoke("Auto-synced with ${pairing.partnerDisplayName.substringBefore("#")}")
                                    }
                                }
                            } else {
                                statusCode = 400
                                responseBody = "{\"error\": \"${result.exceptionOrNull()?.message}\"}"
                            }
                        }
                        return Pair(statusCode, responseBody)
                    }
                }
                start()
            }
            
            val port = currentServer?.listeningPort ?: return
            
            // Include local device ID hash so we can filter out our own broadcast
            val serviceName = "KumaDuo_${pairing.pairingId.take(8)}_${localDeviceId.take(4)}"
            
            val serviceInfo = NsdServiceInfo().apply {
                this.serviceName = serviceName
                this.serviceType = SERVICE_TYPE
                this.port = port
            }
            
            registrationListener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(info: NsdServiceInfo) {
                    // NsdManager may change the service name to resolve conflicts
                    myRegisteredServiceName = info.serviceName
                }
                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                override fun onServiceUnregistered(arg0: NsdServiceInfo) {}
                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
            }
            
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startDiscovery(pairing: DuoPairing) {
        val pairingPrefix = "KumaDuo_${pairing.pairingId.take(8)}"
        
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {}
            override fun onServiceFound(service: NsdServiceInfo) {
                // Must match the pairing prefix
                if (!service.serviceName.startsWith(pairingPrefix)) return
                // Filter out our OWN service to avoid self-sync
                if (service.serviceName == myRegisteredServiceName) return
                
                if (isResolving) return // Prevent overlapping resolves
                isResolving = true
                try {
                    nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            isResolving = false
                        }
                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            isResolving = false
                            val host = serviceInfo.host.hostAddress ?: return
                            val port = serviceInfo.port
                            triggerSync(pairing, host, port)
                        }
                    })
                } catch (e: Exception) {
                    isResolving = false
                }
            }
            override fun onServiceLost(service: NsdServiceInfo) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                try { nsdManager.stopServiceDiscovery(this) } catch(e: Exception){}
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }
        
        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {}
    }
    
    private fun triggerSync(pairing: DuoPairing, host: String, port: Int) {
        val lastAttempt = lastSyncAttempt[pairing.pairingId] ?: 0L
        val now = System.currentTimeMillis()
        // Debounce: wait at least 15 seconds before auto-syncing with the same partner again
        if (now - lastAttempt < 15_000) {
            return
        }
        
        lastSyncAttempt[pairing.pairingId] = now
        scope.launch {
            val res = syncEngine.startSync(pairing, host, port)
            if (res.isSuccess) {
                withContext(Dispatchers.Main) {
                    onSyncEvent?.invoke("Auto-synced with ${pairing.partnerDisplayName.substringBefore("#")}")
                }
            }
        }
    }
}

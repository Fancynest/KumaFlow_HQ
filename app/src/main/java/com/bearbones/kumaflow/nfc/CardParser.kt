package com.bearbones.kumaflow.nfc

import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.util.Log

interface NfcCardStrategy {
    val cardName: String
    fun readCard(isoDep: IsoDep, uidStr: String): CardInfo?
}

    // Mandiri Strategy — balance readable via 00 B5
    class MandiriStrategy : NfcCardStrategy {
        override val cardName = "Mandiri e-Money"
        
        override fun readCard(isoDep: IsoDep, uidStr: String): CardInfo? {
            val aid = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01)
            val selectRes = isoDep.transceive(CardParser.createSelectApdu(aid))
            
            if (!CardParser.isSuccess(selectRes)) return null
            Log.d("MandiriStrategy", "Mandiri e-Money selected")
            
            // Read balance: 00 B5 00 00 0A
            val getBalanceCmd = byteArrayOf(0x00, 0xB5.toByte(), 0x00, 0x00, 0x0A)
            val balRes = isoDep.transceive(getBalanceCmd)
            
            var balance: Long? = null
            if (CardParser.isSuccess(balRes) && balRes.size >= 6) {
                try {
                    var bal = 0L
                    bal = bal or ((balRes[0].toLong() and 0xFF) shl 0)
                    bal = bal or ((balRes[1].toLong() and 0xFF) shl 8)
                    bal = bal or ((balRes[2].toLong() and 0xFF) shl 16)
                    bal = bal or ((balRes[3].toLong() and 0xFF) shl 24)
                    balance = bal
                } catch (e: Exception) { }
            }
            
            return CardInfo(cardName, "", balance)
        }
    }

    // Flazz BCA — try DESFire commands to read balance
    class FlazzStrategy : NfcCardStrategy {
        override val cardName = "Flazz BCA"
        override fun readCard(isoDep: IsoDep, uidStr: String): CardInfo? {
            val aids = listOf(
                CardParser.hexToBytes("A00000000386980701"),
                CardParser.hexToBytes("A0000000031010")
            )
            
            var selected = false
            for (aid in aids) {
                try {
                    val selectRes = isoDep.transceive(CardParser.createSelectApduWithLe(aid))
                    if (CardParser.isSuccess(selectRes)) {
                        Log.d("FlazzStrategy", "Flazz selected via AID ${CardParser.bytesToHex(aid)}")
                        selected = true
                        break
                    }
                } catch (e: Exception) { continue }
            }
            if (!selected) return null
            
            // Try to read balance using various methods
            val balance = CardParser.tryReadBalance(isoDep, "FlazzStrategy")
            return CardInfo(cardName, "", balance)
        }
    }

    // BRI Brizzi
    class BrizziStrategy : NfcCardStrategy {
        override val cardName = "BRI Brizzi"
        override fun readCard(isoDep: IsoDep, uidStr: String): CardInfo? {
            val aids = listOf(
                CardParser.hexToBytes("A0000000041010"),
                CardParser.hexToBytes("A000000004101001"),
                "BRIZZI".toByteArray()
            )
            
            var selected = false
            for (aid in aids) {
                try {
                    val selectRes = isoDep.transceive(CardParser.createSelectApduWithLe(aid))
                    if (CardParser.isSuccess(selectRes)) {
                        Log.d("BrizziStrategy", "Brizzi selected via AID ${CardParser.bytesToHex(aid)}")
                        selected = true
                        break
                    }
                } catch (e: Exception) { continue }
            }
            if (!selected) return null
            
            val balance = CardParser.tryReadBalance(isoDep, "BrizziStrategy")
            return CardInfo(cardName, "", balance)
        }
    }

    // JakCard Bank DKI
    class JakCardStrategy : NfcCardStrategy {
        override val cardName = "JakCard (Bank DKI)"
        override fun readCard(isoDep: IsoDep, uidStr: String): CardInfo? {
            val aids = listOf(
                CardParser.hexToBytes("A000000004306A6B636172640101"),
                CardParser.hexToBytes("A000000004306A6B63617264")
            )
            
            var selected = false
            for (aid in aids) {
                try {
                    val selectRes = isoDep.transceive(CardParser.createSelectApduWithLe(aid))
                    if (CardParser.isSuccess(selectRes)) {
                        Log.d("JakCardStrategy", "JakCard selected")
                        selected = true
                        break
                    }
                } catch (e: Exception) { continue }
            }
            if (!selected) return null
            
            val balance = CardParser.tryReadBalance(isoDep, "JakCardStrategy")
            return CardInfo(cardName, "", balance)
        }
    }

    // Generic EMV contactless card detector (Visa/Mastercard/etc)
    class EmvCardStrategy : NfcCardStrategy {
        override val cardName = "Kartu Contactless"
        
        private val knownAids = mapOf(
            "A0000000031010" to "Visa",
            "A0000000032010" to "Visa Electron",
            "A0000000041010" to "Mastercard",
            "A0000000042010" to "Mastercard Maestro",
            "A000000025010104" to "AMEX",
            "A0000003241010" to "Discover",
            "A0000000651010" to "JCB",
            "325041592E5359532E4444463031" to "PPSE"
        )
        
        override fun readCard(isoDep: IsoDep, uidStr: String): CardInfo? {
            for ((aidHex, brand) in knownAids) {
                try {
                    val aid = CardParser.hexToBytes(aidHex)
                    val selectRes = isoDep.transceive(CardParser.createSelectApduWithLe(aid))
                    if (CardParser.isSuccess(selectRes)) {
                        Log.d("EmvCardStrategy", "EMV card detected: $brand")
                        return CardInfo("$brand Card", "", null)
                    }
                } catch (e: Exception) { continue }
            }
            return null
        }
    }

    class CatchAllStrategy : NfcCardStrategy {
        override val cardName = "Kartu Uang Elektronik"
        
        override fun readCard(isoDep: IsoDep, uidStr: String): CardInfo? {
            Log.d("CatchAllStrategy", "Running fallback strategy")
            // Try direct balance read
            val balance = CardParser.tryReadBalance(isoDep, "CatchAllStrategy")
            if (balance != null) return CardInfo(cardName, "", balance)
            
            // Try DESFire get AppIDs (90 6A 00 00 00)
            try {
                val getAppsCmd = byteArrayOf(0x90.toByte(), 0x6A.toByte(), 0x00, 0x00, 0x00)
                val appsRes = isoDep.transceive(getAppsCmd)
                if (appsRes.size >= 5 && appsRes[appsRes.size - 2] == 0x91.toByte() && appsRes[appsRes.size - 1] == 0x00.toByte()) {
                    val numApps = (appsRes.size - 2) / 3
                    for (i in 0 until numApps) {
                        val offset = i * 3
                        val appId = byteArrayOf(appsRes[offset], appsRes[offset + 1], appsRes[offset + 2])
                        Log.d("CatchAllStrategy", "Found DESFire AppID: ${CardParser.bytesToHex(appId)}")
                        
                        // Select this AppID: 90 5A 00 00 03 [AppID] 00
                        val selectCmd = byteArrayOf(
                            0x90.toByte(), 0x5A.toByte(), 0x00, 0x00, 
                            0x03, appId[0], appId[1], appId[2], 0x00
                        )
                        val selRes = isoDep.transceive(selectCmd)
                        if (selRes.size >= 2 && selRes[selRes.size - 2] == 0x91.toByte() && selRes[selRes.size - 1] == 0x00.toByte()) {
                            Log.d("CatchAllStrategy", "Selected AppID ${CardParser.bytesToHex(appId)}")
                            val appBal = CardParser.tryReadBalance(isoDep, "CatchAllStrategy-App")
                            if (appBal != null) return CardInfo(cardName, "", appBal)
                        }
                    }
                }
            } catch (e: Exception) {}
            
            // Return at least something better than Unknown Card
            return CardInfo("Kartu NFC", "", null)
        }
    }

    object CardParser {
        private const val TAG = "CardParser"
        
        private val strategies = listOf(
            BniTapcashStrategy(),
            MandiriStrategy(),
            FlazzStrategy(),
            BrizziStrategy(),
            JakCardStrategy(),
            EmvCardStrategy(),
            CatchAllStrategy() // Fallback that runs tryReadBalance on EVERYTHING
        )

        /**
         * Aggressive balance reading: tries multiple common APDU commands
         * that work on DESFire-based Indonesian e-money cards.
         * Many cards allow reading balance without authentication.
         */
        fun tryReadBalance(isoDep: IsoDep, tag: String): Long? {
            // === Method 1: DESFire GetValue (90 6C) on file 0-4 ===
            for (fileNo in 0..4) {
                try {
                    val cmd = byteArrayOf(
                        0x90.toByte(), 0x6C.toByte(), 0x00, 0x00, 
                        0x01, fileNo.toByte(), 0x00
                    )
                    val res = isoDep.transceive(cmd)
                    Log.d(tag, "GetValue file=$fileNo -> ${bytesToHex(res)}")
                    // DESFire success: last 2 bytes = 91 00
                    if (res.size >= 6 && res[res.size - 2] == 0x91.toByte() && res[res.size - 1] == 0x00.toByte()) {
                        // Value is 4 bytes, little-endian, before status
                        var bal = 0L
                        bal = bal or ((res[0].toLong() and 0xFF) shl 0)
                        bal = bal or ((res[1].toLong() and 0xFF) shl 8)
                        bal = bal or ((res[2].toLong() and 0xFF) shl 16)
                        bal = bal or ((res[3].toLong() and 0xFF) shl 24)
                        if (bal in 0..50_000_000L) {
                            Log.d(tag, "Balance found via GetValue: $bal")
                            return bal
                        }
                    }
                } catch (e: Exception) {
                    Log.d(tag, "GetValue file=$fileNo error: ${e.message}")
                }
            }
            
            // === Method 2: ISO ReadBinary (00 B0) ===
            for (sfi in 0..5) {
                try {
                    val p1 = if (sfi == 0) 0x00.toByte() else (0x80 or sfi).toByte()
                    val cmd = byteArrayOf(0x00, 0xB0.toByte(), p1, 0x00, 0x00)
                    val res = isoDep.transceive(cmd)
                    if (isSuccess(res) && res.size >= 6) {
                        Log.d(tag, "ReadBinary SFI=$sfi -> ${bytesToHex(res)}")
                        // Try LE 4-byte balance
                        var bal = 0L
                        bal = bal or ((res[0].toLong() and 0xFF) shl 0)
                        bal = bal or ((res[1].toLong() and 0xFF) shl 8)
                        bal = bal or ((res[2].toLong() and 0xFF) shl 16)
                        bal = bal or ((res[3].toLong() and 0xFF) shl 24)
                        if (bal in 1..50_000_000L) {
                            Log.d(tag, "Balance found via ReadBinary: $bal")
                            return bal
                        }
                    }
                } catch (e: Exception) { }
            }
            
            // === Method 3: ISO GetBalance (00 B5) ===
            try {
                val cmd = byteArrayOf(0x00, 0xB5.toByte(), 0x00, 0x00, 0x0A)
                val res = isoDep.transceive(cmd)
                if (isSuccess(res) && res.size >= 6) {
                    Log.d(tag, "GetBalance -> ${bytesToHex(res)}")
                    var bal = 0L
                    bal = bal or ((res[0].toLong() and 0xFF) shl 0)
                    bal = bal or ((res[1].toLong() and 0xFF) shl 8)
                    bal = bal or ((res[2].toLong() and 0xFF) shl 16)
                    bal = bal or ((res[3].toLong() and 0xFF) shl 24)
                    if (bal in 0..50_000_000L) {
                        Log.d(tag, "Balance found via GetBalance: $bal")
                        return bal
                    }
                }
            } catch (e: Exception) { }
            
            // === Method 4: DESFire ReadData (90 BD) on file 0-8 ===
            for (fileNo in 0..8) {
                try {
                    val cmd = byteArrayOf(
                        0x90.toByte(), 0xBD.toByte(), 0x00, 0x00, 
                        0x07, fileNo.toByte(), 
                        0x00, 0x00, 0x00, // offset
                        0x00, 0x00, 0x00, // length (0 = all)
                        0x00
                    )
                    val res = isoDep.transceive(cmd)
                    if (res.size >= 6 && res[res.size - 2] == 0x91.toByte() && 
                        (res[res.size - 1] == 0x00.toByte() || res[res.size - 1] == 0xAF.toByte())) {
                        Log.d(tag, "ReadData file=$fileNo -> ${bytesToHex(res)}")
                        // Try to find balance in data (LE 4-byte)
                        for (offset in 0 until minOf(res.size - 5, 32) step 4) {
                            var bal = 0L
                            bal = bal or ((res[offset].toLong() and 0xFF) shl 0)
                            bal = bal or ((res[offset + 1].toLong() and 0xFF) shl 8)
                            bal = bal or ((res[offset + 2].toLong() and 0xFF) shl 16)
                            bal = bal or ((res[offset + 3].toLong() and 0xFF) shl 24)
                            if (bal in 1000..50_000_000L) { // Min Rp 1000 to filter noise
                                Log.d(tag, "Balance candidate via ReadData file=$fileNo offset=$offset: $bal")
                                return bal
                            }
                        }
                        // Also try BE 3-byte (like TapCash)
                        for (offset in 0 until minOf(res.size - 4, 32)) {
                            val bal = ((res[offset].toInt() and 0xFF).toLong() shl 16) or
                                      ((res[offset + 1].toInt() and 0xFF).toLong() shl 8) or
                                      (res[offset + 2].toInt() and 0xFF).toLong()
                            if (bal in 1000..50_000_000L) {
                                Log.d(tag, "Balance candidate via ReadData BE file=$fileNo offset=$offset: $bal")
                                return bal
                            }
                        }
                    }
                } catch (e: Exception) { }
            }
            
            // === Method 5: Proprietary read commands ===
            val proprietaryCmds = listOf(
                byteArrayOf(0x00, 0xD0.toByte(), 0x00, 0x00, 0x00), // Like TapCash
                byteArrayOf(0x00, 0x32, 0x00, 0x00, 0x00),         // Like TapCash
                byteArrayOf(0x80.toByte(), 0xD0.toByte(), 0x00, 0x00, 0x00),
                byteArrayOf(0x80.toByte(), 0x32, 0x00, 0x00, 0x00),
                byteArrayOf(0x00, 0xCA.toByte(), 0x00, 0x00, 0x00), // GET DATA
                byteArrayOf(0x80.toByte(), 0xCA.toByte(), 0x00, 0x00, 0x00)
            )
            
            for (cmd in proprietaryCmds) {
                try {
                    val res = isoDep.transceive(cmd)
                    if (isSuccess(res) && res.size >= 6) {
                        Log.d(tag, "Proprietary ${bytesToHex(cmd)} -> ${bytesToHex(res)}")
                        // Try BE 3-byte search
                        for (offset in 0 until minOf(res.size - 4, 48)) {
                            val bal = ((res[offset].toInt() and 0xFF).toLong() shl 16) or
                                      ((res[offset + 1].toInt() and 0xFF).toLong() shl 8) or
                                      (res[offset + 2].toInt() and 0xFF).toLong()
                            if (bal in 1000..50_000_000L) {
                                Log.d(tag, "Balance found via proprietary offset=$offset: $bal")
                                return bal
                            }
                        }
                        // Try LE 4-byte
                        for (offset in 0 until minOf(res.size - 5, 48) step 4) {
                            var bal = 0L
                            bal = bal or ((res[offset].toLong() and 0xFF) shl 0)
                            bal = bal or ((res[offset + 1].toLong() and 0xFF) shl 8)
                            bal = bal or ((res[offset + 2].toLong() and 0xFF) shl 16)
                            bal = bal or ((res[offset + 3].toLong() and 0xFF) shl 24)
                            if (bal in 1000..50_000_000L) {
                                Log.d(tag, "Balance found via proprietary LE offset=$offset: $bal")
                                return bal
                            }
                        }
                    }
                } catch (e: Exception) { }
            }
            
            // === Method 6: EMV Read Records ===
            for (sfi in 1..5) {
                val p2 = (sfi shl 3) or 4
                for (rec in 1..3) {
                    try {
                        val cmd = byteArrayOf(0x00, 0xB2.toByte(), rec.toByte(), p2.toByte(), 0x00)
                        val res = isoDep.transceive(cmd)
                        if (isSuccess(res) && res.size >= 6) {
                            Log.d(tag, "ReadRecord SFI=$sfi rec=$rec -> ${bytesToHex(res)}")
                        }
                    } catch (e: Exception) { }
                }
            }
            
            Log.d(tag, "No balance found with any method")
            return null
        }

        fun parseCard(tag: android.nfc.Tag): CardInfo {
            val uidStr = bytesToHex(tag.id)
            val formattedUidStr = uidStr.chunked(4).joinToString(" ")
            
            val techList = tag.techList
            Log.d(TAG, "techList=${techList.joinToString()}")
            
            val nfcA = android.nfc.tech.NfcA.get(tag)
            if (nfcA != null) {
                Log.d(TAG, "ATQA=${bytesToHex(nfcA.atqa)} SAK=${nfcA.sak}")
            }

            try {
                val isoDep = IsoDep.get(tag)
                if (isoDep != null) {
                    isoDep.timeout = 5000
                    isoDep.connect()
                    Log.d(TAG, "historicalBytes=${isoDep.historicalBytes?.let { bytesToHex(it) }} hiLayerResponse=${isoDep.hiLayerResponse?.let { bytesToHex(it) }}")

                    for (strategy in strategies) {
                        try {
                            val info = strategy.readCard(isoDep, formattedUidStr)
                            if (info != null) {
                                Log.d(TAG, "Card: ${info.cardType}, balance=${info.balance}")
                                isoDep.close()
                                return info
                            }
                        } catch (e: Exception) {
                            Log.d(TAG, "${strategy.cardName} failed: ${e.message}")
                        }
                    }
                    isoDep.close()
                } else {
                    val mifareClassic = MifareClassic.get(tag)
                    if (mifareClassic != null) {
                        mifareClassic.connect()
                        Log.d(TAG, "Mifare Classic, sectors=${mifareClassic.sectorCount}")
                        val info = CardInfo("Mifare Classic", "", null)
                        mifareClassic.close()
                        return info
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing card", e)
            }

            return CardInfo("Unknown Card", "", null)
        }

        fun hexToBytes(s: String): ByteArray {
            val len = s.length
            val data = ByteArray(len / 2)
            var i = 0
            while (i < len) {
                data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
                i += 2
            }
            return data
        }

        fun createSelectApdu(aid: ByteArray): ByteArray {
            val command = ByteArray(5 + aid.size)
            command[0] = 0x00; command[1] = 0xA4.toByte(); command[2] = 0x04; command[3] = 0x00
            command[4] = aid.size.toByte()
            System.arraycopy(aid, 0, command, 5, aid.size)
            return command
        }

        fun createSelectApduWithLe(aid: ByteArray): ByteArray {
            val command = ByteArray(6 + aid.size)
            command[0] = 0x00; command[1] = 0xA4.toByte(); command[2] = 0x04; command[3] = 0x00
            command[4] = aid.size.toByte()
            System.arraycopy(aid, 0, command, 5, aid.size)
            command[5 + aid.size] = 0x00
            return command
        }

        fun isSuccess(response: ByteArray): Boolean {
            if (response.size >= 2) {
                return response[response.size - 2] == 0x90.toByte() && response[response.size - 1] == 0x00.toByte()
            }
            return false
        }

        fun bytesToHex(bytes: ByteArray): String {
            val hexChars = CharArray(bytes.size * 2)
            for (j in bytes.indices) {
                val v = bytes[j].toInt() and 0xFF
                hexChars[j * 2] = "0123456789ABCDEF"[v ushr 4]
                hexChars[j * 2 + 1] = "0123456789ABCDEF"[v and 0x0F]
            }
            return String(hexChars)
        }
    }

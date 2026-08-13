package com.bearbones.kumaflow.nfc

import android.nfc.tech.IsoDep
import android.util.Log

class BniTapcashStrategy : NfcCardStrategy {
    companion object {
        private const val TAG = "BniTapcashStrategy"
        private val AID_BNI_TAPCASH = CardParser.hexToBytes("A000424E49100001")
        // INS D0 returns transaction/purse data including balance
        private val CMD_READ_PURSE_D0 = byteArrayOf(0x00, 0xD0.toByte(), 0x00, 0x00, 0x00)
    }

    override val cardName = "BNI TapCash"
    
    override fun readCard(isoDep: IsoDep, defaultUid: String): CardInfo? {
        try {
            // Select BNI TapCash Applet
            val selectCmd = CardParser.createSelectApduWithLe(AID_BNI_TAPCASH)
            val selRes = isoDep.transceive(selectCmd)

            if (!CardParser.isSuccess(selRes)) return null

            Log.d(TAG, "BNI TapCash Applet Selected")

            // Read purse data using INS D0
            val readRes = isoDep.transceive(CMD_READ_PURSE_D0)
            Log.d(TAG, "D0 response: ${CardParser.bytesToHex(readRes)}")
            
            if (CardParser.isSuccess(readRes) && readRes.size >= 14) {
                // D0 response structure (from sweep):
                // 0000000000031403060013883B5964B3...9000
                // Offset 9-11 (3 bytes BE) = balance in Rupiah
                // 00 13 88 = 5000 = Rp 5.000
                val balance = ((readRes[9].toInt() and 0xFF).toLong() shl 16) or
                              ((readRes[10].toInt() and 0xFF).toLong() shl 8) or
                              (readRes[11].toInt() and 0xFF).toLong()

                Log.d(TAG, "Balance = Rp $balance")
                return CardInfo(cardName, "", balance)
            }

            // Fallback: try INS 32 if D0 didn't work
            val cmd32 = byteArrayOf(0x00, 0x32, 0x00, 0x00, 0x00)
            val res32 = isoDep.transceive(cmd32)
            Log.d(TAG, "INS 32 response: ${CardParser.bytesToHex(res32)}")
            
            if (CardParser.isSuccess(res32) && res32.size >= 4) {
                // Try to scan for balance (BE 3-byte or LE 4-byte)
                // We'll return null instead of 0L so it shows "Terdeteksi" rather than Rp 0
                val bal = CardParser.tryReadBalance(isoDep, "BniTapcashFallback")
                return CardInfo(cardName, "", bal)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
        }
        return null
    }
}

package com.bearbones.kumaflow

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NewUserAnnouncementDialog() {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("KumaFlowPrefs", Context.MODE_PRIVATE)

    // Verify if this is the user's first time launching the application (defaults to true)
    var showDialog by remember {
        mutableStateOf(sharedPref.getBoolean("is_first_time_user", true))
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                // Intentionally left blank to prevent the user from accidentally dismissing the dialog
                // by tapping outside the pop-up area. Explicit interaction with the "Got it" button is required.
            },
            title = {
                Text(
                    text = "📢 Info Penting Pengingat",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column {
                    Text(text = if(AppStr.isId) "Biar KumaFlow bisa selalu ngingetin kamu nyatat pengeluaran, kamu akan melihat notifikasi \"KumaFlow Aktif\" di atas layar." else "To remind you to track expenses, you will see a \"KumaFlow Active\" notification at the top of the screen.")
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "🛡️ Pelindung Sistem", fontWeight = FontWeight.Bold)
                    Text(text = "Tanda KumaFlow sedang standby agar alarm tidak dimatikan paksa oleh HP.")
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "🛑 Jangan Dihapus (Di-swipe)", fontWeight = FontWeight.Bold)
                    Text(text = "Tolong biarkan notifikasi ini. Jika dihapus, pengingat berisiko mati.")
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "🔋 Hemat Baterai", fontWeight = FontWeight.Bold)
                    Text(text = "Sistem ini sangat ringan dan aman untuk baterai HP kamu.")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Upon button interaction, update the "is_first_time_user" status flag to false
                        // Persist this state locally to ensure the dialog is not displayed on subsequent app launches
                        sharedPref.edit().putBoolean("is_first_time_user", false).apply()
                        showDialog = false
                    }
                ) {
                    Text(if(AppStr.isId) "Paham & Lanjutkan" else "Understand & Continue")
                }
            }
        )
    }
}
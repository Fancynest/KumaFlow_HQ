package com.bearbones.kumaflow

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumaflow.ui.components.KumaTextButton
import com.bearbones.kumaflow.ui.components.rememberKumaWindowSize

@Composable
fun NewUserAnnouncementDialog(onDismissed: () -> Unit) {
    val context = LocalContext.current
    val windowSize = rememberKumaWindowSize()
    val sharedPref = context.getSharedPreferences("KumaFlowPrefs", Context.MODE_PRIVATE)

    var showDialog by remember {
        mutableStateOf(sharedPref.getBoolean("is_first_time_user", true))
    }

    if (showDialog) {
        LaunchedEffect(Unit) {
            sharedPref.edit().putBoolean("is_first_time_user", false).apply()
        }
        AlertDialog(
            modifier = Modifier.widthIn(max = 480.dp),
            onDismissRequest = {},
            title = {
                Text(
                    text = AppStr.infoReminder,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(modifier = Modifier.widthIn(max = 480.dp)) {
                    Text(text = AppStr.infoReminderDesc)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = AppStr.sysProtector, fontWeight = FontWeight.Bold)
                    Text(text = AppStr.sysProtectorDesc)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = AppStr.doNotDismiss, fontWeight = FontWeight.Bold)
                    Text(text = AppStr.doNotDismissDesc)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = AppStr.battEfficiency, fontWeight = FontWeight.Bold)
                    Text(text = AppStr.battEfficiencyDesc)
                }
            },
            confirmButton = {
                KumaTextButton(
                    onClick = {
                        showDialog = false
                        onDismissed()
                    }
                ) {
                    Text(AppStr.understandCont)
                }
            }
        )
    } else {
        LaunchedEffect(Unit) {
            onDismissed()
        }
    }
}
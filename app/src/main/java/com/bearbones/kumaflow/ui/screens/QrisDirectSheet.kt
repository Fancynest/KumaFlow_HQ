package com.bearbones.kumaflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumaflow.AppBg
import com.bearbones.kumaflow.AppPrimary
import com.bearbones.kumaflow.AppSurface
import com.bearbones.kumaflow.AppSurfaceVariant
import com.bearbones.kumaflow.AppText
import com.bearbones.kumaflow.MorphingKeypadButton
import com.bearbones.kumaflow.ui.components.KumaButton
import com.bearbones.kumaflow.ui.components.KumaIconButton
import com.bearbones.kumaflow.ui.components.KumaOutlinedTextField
import com.bearbones.kumaflow.glassCard
import com.bearbones.kumaflow.AppStr
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.ui.focus.onFocusChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrisDirectSheet(
    onDismissRequest: () -> Unit,
    onSubmitNominal: (Long, String) -> Unit,
    onWithoutNominal: () -> Unit
) {
    var nominalStr by remember { mutableStateOf("") }
    var messageStr by remember { mutableStateOf("") }

    val format = NumberFormat.getInstance(Locale.GERMANY)

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

            // Nominal Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(16.dp, AppSurface())
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val formattedAmount = if (nominalStr.isEmpty()) "0" else format.format(nominalStr.toLong())
                Text(
                    text = "Rp $formattedAmount",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = AppText()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            var isMessageFocused by remember { mutableStateOf(false) }

            // Message Input
            KumaOutlinedTextField(
                value = messageStr,
                onValueChange = { messageStr = it },
                placeholder = { Text(if (AppStr.isId) "Cth: Pembayaran Makan Siang" else "Ex: Lunch Payment") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, tint = AppText().copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth().onFocusChanged { isMessageFocused = it.isFocused },
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Switch to Static
            Surface(
                color = AppSurfaceVariant(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.clickable { onWithoutNominal() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = AppText(), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (AppStr.isId) "Ganti ke QRIS tanpa nominal" else "Switch to QRIS without amount", color = AppText(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Custom Numpad (Hide when typing message to prevent overlapping)
            if (!isMessageFocused) {
                val numpadKeys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("0", "000", "DEL")
                )

                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    numpadKeys.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                            row.forEach { key ->
                                val label = if (key == "DEL") "Del" else key
                                MorphingKeypadButton(
                                    label = label,
                                    onClick = {
                                        when (key) {
                                            "DEL" -> if (nominalStr.isNotEmpty()) nominalStr = nominalStr.dropLast(1)
                                            "000" -> if (nominalStr.isNotEmpty() && nominalStr.length <= 9) nominalStr += "000"
                                            else -> if (nominalStr.length <= 11) {
                                                if (!(nominalStr == "0" && key == "0")) {
                                                    nominalStr += key
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            KumaButton(
                onClick = { 
                    val amt = nominalStr.toLongOrNull() ?: 0L
                    onSubmitNominal(amt, messageStr)
                },
                modifier = Modifier.fillMaxWidth().height(55.dp),
                enabled = nominalStr.isNotEmpty() && (nominalStr.toLongOrNull() ?: 0L) > 0
            ) {
                Text(if (AppStr.isId) "Buat tagihan QRIS" else "Create QRIS billing", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        }
    }
}

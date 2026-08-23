package com.bearbones.kumaflow

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumaflow.VirtualWallet
import com.bearbones.kumaflow.ui.components.KumaButton
import com.bearbones.kumaflow.ui.components.KumaOutlinedTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageWalletSheet(
    walletToEdit: VirtualWallet?,
    onSave: (oldName: String?, wallet: VirtualWallet) -> Unit,
    onDelete: (VirtualWallet) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(walletToEdit?.name ?: "") }
    var bgType by remember { mutableStateOf(walletToEdit?.backgroundType ?: "SOLID") }
    var bgValue by remember { mutableStateOf(walletToEdit?.backgroundValue ?: "#2A2A2A") }
    
    val solidColors = listOf("#2A2A2A", "#D32F2F", "#1976D2", "#388E3C", "#FBC02D", "#7B1FA2", "#111111", "#0288D1")
    val templateImages = listOf(
        "minangkabau_card", "java_card", "papua_card", "bali_card", "bugis_card", "westkalimantan_card"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = com.bearbones.kumaflow.AppBg(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (walletToEdit == null) "Add Wallet" else "Edit Wallet",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = com.bearbones.kumaflow.AppText(),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Preview Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (bgType == "SOLID") {
                            try {
                                Color(android.graphics.Color.parseColor(bgValue))
                            } catch (e: Exception) { Color.DarkGray }
                        } else Color.Gray
                    )
            ) {
                if (bgType == "TEMPLATE") {
                    val resId = context.resources.getIdentifier(bgValue, "drawable", context.packageName)
                    if (resId != 0) {
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Scrim
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("KumaFlow", color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                        Text(name.ifBlank { "Wallet Name" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Text("Rp 0", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            KumaOutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Wallet Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Background Theme", color = com.bearbones.kumaflow.AppText(), fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))
            
            // Solid Colors
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(solidColors) { colorHex ->
                    val isSelected = bgType == "SOLID" && bgValue == colorHex
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(colorHex)))
                            .border(if (isSelected) 3.dp else 0.dp, if (isSelected) com.bearbones.kumaflow.AppPrimary() else Color.Transparent, CircleShape)
                            .clickable {
                                bgType = "SOLID"
                                bgValue = colorHex
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) Icon(Icons.Default.Check, null, tint = Color.White)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Nusantara Themes", color = com.bearbones.kumaflow.AppText(), fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))
            
            // Templates
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(templateImages) { template ->
                    val isSelected = bgType == "TEMPLATE" && bgValue == template
                    val resId = context.resources.getIdentifier(template, "drawable", context.packageName)
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Gray)
                            .border(if (isSelected) 3.dp else 0.dp, if (isSelected) com.bearbones.kumaflow.AppPrimary() else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable {
                                bgType = "TEMPLATE"
                                bgValue = template
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (resId != 0) {
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("Missing", fontSize = 10.sp)
                        }
                        if (isSelected) {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
                            Icon(Icons.Default.Check, null, tint = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (walletToEdit != null) {
                    KumaButton(
                        onClick = { onDelete(walletToEdit) }, 
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Delete, null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete", color = Color.White)
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                KumaButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSave(
                                walletToEdit?.name,
                                VirtualWallet(
                                    name = name.trim(),
                                    orderIndex = walletToEdit?.orderIndex ?: 0,
                                    backgroundType = bgType,
                                    backgroundValue = bgValue
                                )
                            )
                        }
                    },
                    modifier = if (walletToEdit == null) Modifier.fillMaxWidth() else Modifier.weight(1f)
                ) {
                    Text("Save", color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

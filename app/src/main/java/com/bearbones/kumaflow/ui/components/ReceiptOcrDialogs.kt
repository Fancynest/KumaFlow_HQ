package com.bearbones.kumaflow.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bearbones.kumaflow.AppPrimary
import com.bearbones.kumaflow.AppStr
import com.bearbones.kumaflow.AppSurface
import com.bearbones.kumaflow.AppText
import com.bearbones.kumaflow.utils.OcrSecureStorage

@Composable
fun OcrSourceChooserDialog(
    onDismiss: () -> Unit,
    onChooseCamera: () -> Unit,
    onChooseGallery: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStr.chooseSource, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Option 1: Camera
                Surface(
                    onClick = onChooseCamera,
                    shape = RoundedCornerShape(16.dp),
                    color = AppPrimary().copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        KumaExpressiveIcon(Icons.Default.CameraAlt, null, tint = AppPrimary(), size = 28.dp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(AppStr.camera, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AppText())
                            Text(AppStr.scanReceiptDesc, fontSize = 11.sp, color = AppText().copy(alpha = 0.6f))
                        }
                    }
                }

                // Option 2: Gallery
                Surface(
                    onClick = onChooseGallery,
                    shape = RoundedCornerShape(16.dp),
                    color = AppPrimary().copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        KumaExpressiveIcon(Icons.Default.PhotoLibrary, null, tint = AppPrimary(), size = 28.dp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(AppStr.gallery, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AppText())
                            Text(AppStr.galleryPickDesc, fontSize = 11.sp, color = AppText().copy(alpha = 0.6f))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            KumaTextButton(onClick = onDismiss) {
                Text(AppStr.close, color = AppText())
            }
        }
    )
}

@Composable
fun OcrKeyPromptDialog(
    ocrStorage: OcrSecureStorage,
    onDismiss: () -> Unit,
    onKeySaved: () -> Unit
) {
    val context = LocalContext.current
    var selectedPromptProvider by remember { mutableStateOf(ocrStorage.getSelectedProvider()) }
    var promptAnthropicKey by remember { mutableStateOf(ocrStorage.getApiKey() ?: "") }
    var promptGeminiKey by remember { mutableStateOf(ocrStorage.getGeminiApiKey() ?: "") }
    var isDirectKeyVisible by remember { mutableStateOf(false) }

    val isGemini = selectedPromptProvider == "gemini"
    val currentKeyInput = if (isGemini) promptGeminiKey else promptAnthropicKey

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KumaExpressiveIcon(Icons.Default.Key, null, tint = AppPrimary())
                Spacer(modifier = Modifier.width(8.dp))
                Text(AppStr.ocrApiKeyRequiredTitle, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    AppStr.ocrApiKeyRequiredDesc,
                    fontSize = 13.sp,
                    color = AppText().copy(alpha = 0.8f),
                    lineHeight = 18.sp
                )

                // 2 Provider Choices (Anthropic vs Gemini)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = { selectedPromptProvider = "anthropic" },
                        shape = RoundedCornerShape(12.dp),
                        color = if (!isGemini) AppPrimary() else AppSurface().copy(alpha = 0.6f),
                        contentColor = if (!isGemini) Color.White else AppText(),
                        border = if (!isGemini) null else BorderStroke(1.dp, AppText().copy(alpha = 0.15f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                AppStr.ocrProviderAnthropic,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    Surface(
                        onClick = { selectedPromptProvider = "gemini" },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isGemini) AppPrimary() else AppSurface().copy(alpha = 0.6f),
                        contentColor = if (isGemini) Color.White else AppText(),
                        border = if (isGemini) null else BorderStroke(1.dp, AppText().copy(alpha = 0.15f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                AppStr.ocrProviderGemini,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Clickable URL Button
                Surface(
                    onClick = {
                        try {
                            val targetUrl = if (isGemini) "https://aistudio.google.com/api-keys" else "https://console.anthropic.com/settings/keys"
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)))
                        } catch (_: Exception) {}
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = AppPrimary().copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        KumaExpressiveIcon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = AppPrimary(), size = 18.dp, containerColor = Color.Transparent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isGemini) AppStr.ocrOpenAiStudio else AppStr.ocrOpenAnthropicConsole,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppPrimary(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Text(
                    if (isGemini) AppStr.ocrGeminiKeyHint else AppStr.ocrApiKeyHint,
                    fontSize = 11.sp,
                    color = AppText().copy(alpha = 0.6f),
                    lineHeight = 15.sp
                )

                KumaOutlinedTextField(
                    value = currentKeyInput,
                    onValueChange = {
                        if (isGemini) promptGeminiKey = it else promptAnthropicKey = it
                    },
                    placeholder = { Text(if (isGemini) AppStr.ocrGeminiKeyLabel else AppStr.ocrApiKeyLabel) },
                    visualTransformation = if (isDirectKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isDirectKeyVisible = !isDirectKeyVisible }) {
                            Icon(
                                if (isDirectKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = AppPrimary()
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        },
        confirmButton = {
            KumaButton(
                onClick = {
                    ocrStorage.saveSelectedProvider(selectedPromptProvider)
                    if (isGemini) {
                        ocrStorage.saveGeminiApiKey(promptGeminiKey)
                    } else {
                        ocrStorage.saveApiKey(promptAnthropicKey)
                    }
                    onKeySaved()
                }
            ) {
                Text(AppStr.save, color = Color.White)
            }
        },
        dismissButton = {
            KumaTextButton(onClick = onDismiss) {
                Text(AppStr.close, color = AppText())
            }
        }
    )
}

@Composable
fun OcrScanningDialog(isScanning: Boolean) {
    if (isScanning) {
        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = AppSurface(),
                border = BorderStroke(1.dp, AppText().copy(alpha = 0.1f)),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = AppPrimary(),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = AppStr.scanningReceipt,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = AppText()
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (AppStr.isId) "AI sedang menganalisis foto struk..." else "AI is analyzing receipt...",
                        fontSize = 12.sp,
                        color = AppText().copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

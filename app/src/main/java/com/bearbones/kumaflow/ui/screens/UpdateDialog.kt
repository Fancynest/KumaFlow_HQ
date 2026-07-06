package com.bearbones.kumaflow.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bearbones.kumaflow.utils.DownloadState
import com.bearbones.kumaflow.utils.UpdateInfo
import java.util.Locale

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    currentVersionName: String,
    downloadState: DownloadState,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
    onUpdate: () -> Unit,
    onCancelDownload: () -> Unit
) {
    val isDownloading = downloadState is DownloadState.Downloading
    val isSuccess = downloadState is DownloadState.Success

    Dialog(
        onDismissRequest = { if (!isDownloading && !isSuccess) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isDownloading && !isSuccess,
            dismissOnClickOutside = !isDownloading && !isSuccess,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E2328) // Deep dark premium blue/grey
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header with Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF00ACC1)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Update Icon",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = com.bearbones.kumaflow.AppStr.updAvail,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = com.bearbones.kumaflow.AppStr.newVerRdy,
                            fontSize = 14.sp,
                            color = Color(0xFFA0AAB2)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                // Version Comparison Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF161A1E))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(com.bearbones.kumaflow.AppStr.currVer, fontSize = 12.sp, color = Color(0xFFA0AAB2))
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF2A3138)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Text(currentVersionName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA0AAB2))
                            }
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = "To", tint = Color(0xFF00ACC1))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(com.bearbones.kumaflow.AppStr.newVer, fontSize = 12.sp, color = Color(0xFFA0AAB2))
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF00545E)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Text(updateInfo.versionName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00ACC1))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Animated Interactive Content Block
                AnimatedContent(
                    targetState = downloadState,
                    contentKey = { it::class },
                    transitionSpec = {
                        (fadeIn() + expandVertically()).togetherWith(fadeOut() + shrinkVertically())
                    },
                    label = "DownloadStateAnimation"
                ) { state ->
                    when (state) {
                        is DownloadState.Idle, is DownloadState.Error -> {
                            Column {
                                // Release Notes
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 140.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFF161A1E))
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = if (state is DownloadState.Error) "Error: ${state.message}\n\n${updateInfo.releaseNotes}" else updateInfo.releaseNotes,
                                        fontSize = 13.sp,
                                        color = if (state is DownloadState.Error) Color(0xFFFF5252) else Color(0xFFDDE3E8),
                                        modifier = Modifier.verticalScroll(rememberScrollState()),
                                        lineHeight = 20.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedButton(
                                        onClick = onSnooze,
                                        modifier = Modifier.weight(1f).height(56.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA0AAB2)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A3138))
                                    ) {
                                        Text(com.bearbones.kumaflow.AppStr.laterBtn, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Button(
                                        onClick = onUpdate,
                                        modifier = Modifier.weight(1f).height(56.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00ACC1))
                                    ) {
                                        Text(if (state is DownloadState.Error) com.bearbones.kumaflow.AppStr.retryBtn else com.bearbones.kumaflow.AppStr.downloadBtn, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        is DownloadState.Downloading -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF161A1E))
                                    .padding(20.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color(0xFF00ACC1),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(com.bearbones.kumaflow.AppStr.downloading, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                val animatedProgress by animateFloatAsState(
                                    targetValue = state.progress,
                                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 300, easing = androidx.compose.animation.core.LinearEasing),
                                    label = "progressAnim"
                                )
                                LinearProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                    color = Color(0xFF00ACC1),
                                    trackColor = Color(0xFF2A3138),
                                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                                    gapSize = 0.dp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val formattedDl = String.format(Locale.US, "%.1f", state.downloadedMB)
                                    val formattedTotal = String.format(Locale.US, "%.1f", state.totalMB)
                                    val percent = (state.progress * 100).toInt()
                                    
                                    Text("$formattedDl / $formattedTotal MB", color = Color(0xFFA0AAB2), fontSize = 12.sp)
                                    Text("$percent%", color = Color(0xFF00ACC1), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        is DownloadState.Success -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF161A1E))
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(com.bearbones.kumaflow.AppStr.dlDone, color = Color(0xFF4CAF50), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(com.bearbones.kumaflow.AppStr.openingInst, color = Color(0xFFA0AAB2), fontSize = 13.sp)
                            }
                        }
                    }
                }
                
                // Cancel button (only when downloading)
                AnimatedVisibility(
                    visible = isDownloading,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = onCancelDownload,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00ACC1)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A3138))
                        ) {
                            Text(com.bearbones.kumaflow.AppStr.cancelBtn, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

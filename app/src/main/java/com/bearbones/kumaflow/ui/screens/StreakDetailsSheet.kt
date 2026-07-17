package com.bearbones.kumaflow.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import com.bearbones.kumaflow.ui.components.KumaExpressiveIcon
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.bearbones.kumaflow.R
import com.bearbones.kumaflow.AppStr
import com.bearbones.kumaflow.UserProfile
import com.bearbones.kumaflow.AppPrimary
import com.bearbones.kumaflow.AppSurface
import com.bearbones.kumaflow.AppText
import com.bearbones.kumaflow.utils.ShareStreakUtils
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun StreakDetailsSheet(
    profile: UserProfile,
    activeDates: List<LocalDate>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val todayStrForFire = LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
    val isActiveTodayFire = profile.lastActiveDate == todayStrForFire
    
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.fire))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = isActiveTodayFire,
        speed = 0.5f
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        
        Box(modifier = Modifier.size(150.dp)) {
            LottieAnimation(
                composition = composition,
                progress = { if (isActiveTodayFire) progress else 0f },
                modifier = Modifier.fillMaxSize().let {
                    if (!isActiveTodayFire) {
                        it.graphicsLayer(alpha = 0.5f).drawWithContent {
                            val paint = Paint().apply {
                                colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                            }
                            drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), paint)
                            drawContent()
                            drawContext.canvas.restore()
                        }
                    } else it
                }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "${profile.currentStreak} ${AppStr.daysStreak}",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 28.sp,
            color = AppText()
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            KumaExpressiveIcon(Icons.Default.AcUnit, contentDescription = "Freeze", tint = Color(0xFF4FC3F7), containerColor = androidx.compose.ui.graphics.Color.Transparent, size = 20.dp, iconPadding = 2.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text("${AppStr.activeFreeze}: ${profile.freezeCount} / 2", color = AppText().copy(alpha = 0.7f), fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        // Progress to next milestone (next multiple of 30)
        val nextMilestone = ((profile.currentStreak / 30) + 1) * 30
        val currentProgress = profile.currentStreak.toFloat() / nextMilestone.toFloat()
        
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(AppStr.progToNextFreeze, fontSize = 12.sp, color = AppText().copy(alpha = 0.6f))
                Text("${profile.currentStreak} / $nextMilestone", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppText())
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { currentProgress },
                modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                color = AppPrimary(),
                trackColor = AppText().copy(alpha = 0.1f)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        val todayStr = LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
        val isActiveToday = profile.lastActiveDate == todayStr
        
        if (!isActiveToday) {
            val scope = rememberCoroutineScope()
            com.bearbones.kumaflow.ui.components.KumaButton(
                onClick = {
                    scope.launch {
                        val dao = com.bearbones.kumaflow.KumaDatabase.getDatabase(context).transactionDao()
                        com.bearbones.kumaflow.utils.StreakManager.checkAndUpdateStreak(dao)
                        
                        // Notify widgets
                        com.bearbones.kumaflow.updateKumaWidget(context)
                        
                        // Update MainActivity's state to reflect the new streak
                        // State flows will trigger recomposition and update the sheet live!
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppPrimary())
            ) {
                Text(if (com.bearbones.kumaflow.AppStr.isId) "Klaim Hari Bebas Jajan!" else "Claim No Spend Day!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Calendar Mini
        SimpleCalendar(activeDates)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            com.bearbones.kumaflow.ui.components.KumaButton(
                onClick = { ShareStreakUtils.shareStreak(context, profile, saveOnly = true) },
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppSurface().copy(alpha=0.5f))
            ) {
                KumaExpressiveIcon(Icons.Default.Download, contentDescription = null, tint = AppText(), containerColor = androidx.compose.ui.graphics.Color.Transparent)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (com.bearbones.kumaflow.AppStr.isId) "Simpan" else "Save", color = AppText(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            com.bearbones.kumaflow.ui.components.KumaButton(
                onClick = { ShareStreakUtils.shareStreak(context, profile, saveOnly = false) },
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppPrimary())
            ) {
                KumaExpressiveIcon(Icons.Default.Share, contentDescription = null, tint = Color.White, containerColor = androidx.compose.ui.graphics.Color.Transparent)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (com.bearbones.kumaflow.AppStr.isId) "Bagikan" else "Share", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun SimpleCalendar(activeDates: List<LocalDate>) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value // 1 = Mon, 7 = Sun
    
    val daysList = mutableListOf<Int?>()
    // Add empty slots for days before 1st of month
    val emptySlots = if (firstDayOfWeek == 7) 0 else firstDayOfWeek
    for (i in 0 until emptySlots) {
        daysList.add(null)
    }
    // Add days
    for (i in 1..daysInMonth) {
        daysList.add(i)
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(AppSurface().copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                KumaExpressiveIcon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = AppText(), containerColor = androidx.compose.ui.graphics.Color.Transparent)
            }
            Text(
                text = "${currentMonth.month.name.take(3)} ${currentMonth.year}",
                fontWeight = FontWeight.Bold,
                color = AppText(),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                KumaExpressiveIcon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = AppText(), containerColor = androidx.compose.ui.graphics.Color.Transparent)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
            userScrollEnabled = false
        ) {
            items(daysList) { day ->
                if (day != null) {
                    val date = currentMonth.atDay(day)
                    val isActive = activeDates.contains(date)
                    val isToday = date == LocalDate.now()
                    
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isActive -> AppPrimary()
                                    isToday -> AppText().copy(alpha = 0.1f)
                                    else -> Color.Transparent
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.toString(),
                            fontSize = 12.sp,
                            fontWeight = if (isActive || isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive) Color.White else AppText()
                        )
                    }
                } else {
                    Box(modifier = Modifier.aspectRatio(1f).padding(2.dp))
                }
            }
        }
    }
}

@file:Suppress("SpellCheckingInspection", "UNUSED_PARAMETER", "unused", "CanBeVal", "DEPRECATION")
@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bearbones.kumaflow

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.bearbones.kumaflow.ui.components.KumaExpressiveIcon
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.border
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.translate


@Composable
fun ReportScreen(
    profile: UserProfile,
    monthlyTransactions: List<KumaTransaction>,
    allTransactions: List<KumaTransaction>,
    income: Long,
    expenses: Long,
    balance: Long,
    selectedMonth: Int,
    selectedYear: Int,
    paddingValues: PaddingValues,
    onMonthChange: (Int, Int) -> Unit,
    onOpenWrapped: (Int, Int) -> Unit = { _, _ -> }
) {
    val locale = Locale.forLanguageTag("id-ID")
    val curSym = when(profile.currency) { "USD", "AUD", "CAD", "SGD" -> "$"; "EUR" -> "€"; "GBP" -> "£"; "JPY", "CNY" -> "¥"; "CHF" -> "CHF"; else -> "Rp" }

    fun getCatColor(catName: String): Color {
        val predefined = mapOf(
            "Financial" to Color(0xFF4CAF50),
            "Food" to Color(0xFFFF9800),
            "Shopping" to Color(0xFFE91E63),
            "Health" to Color(0xFFF44336),
            "Transport" to Color(0xFF2196F3),
            "Education" to Color(0xFF9C27B0),
            "Entertainment" to Color(0xFF673AB7),
            "Transfer" to Color(0xFF00BCD4),
            "Others" to Color(0xFF607D8B)
        )
        return predefined[catName] ?: Color(android.graphics.Color.HSVToColor(floatArrayOf(abs(catName.hashCode()) % 360f, 0.7f, 0.8f)))
    }

    val expensePerCat = monthlyTransactions.filter { !it.isIncome }.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount.toLongOrNull() ?: 0L } }.toList().sortedByDescending { it.second }
    val catTargets = remember(profile.categoryTargets) { try { JSONObject(profile.categoryTargets) } catch (e: Exception) { JSONObject() } }
    val savedIcons = remember(profile.categoryIcons) { try { JSONObject(profile.categoryIcons) } catch (e: Exception) { JSONObject() } }
    var showAllCategories by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 24.dp).verticalScroll(rememberScrollState())) {
        Text(AppStr.rep, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = AppText())
        Spacer(modifier = Modifier.height(16.dp))

        MonthYearSelector(selectedMonth, selectedYear, onMonthChange)
        Spacer(modifier = Modifier.height(16.dp))

        val monthNamesList = if (AppStr.isId) listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember") else listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        val currentSelectedMonthName = monthNamesList.getOrElse(selectedMonth - 1) { "" }

        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH) + 1
        val currentYear = cal.get(Calendar.YEAR)
        val isCurrentOrFutureMonth = selectedYear > currentYear || (selectedYear == currentYear && selectedMonth >= currentMonth)

        val context = LocalContext.current

        OutlinedButton(
            onClick = {
                if (isCurrentOrFutureMonth) {
                    Toast.makeText(context, AppStr.wrappedComingSoon(currentSelectedMonthName, selectedYear.toString()), Toast.LENGTH_SHORT).show()
                } else {
                    onOpenWrapped(selectedMonth, selectedYear)
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isCurrentOrFutureMonth) AppText().copy(alpha = 0.2f) else AppPrimary().copy(alpha = 0.5f))
        ) {
            KumaExpressiveIcon(if (isCurrentOrFutureMonth) Icons.Default.Lock else Icons.Default.AutoAwesome, contentDescription = "Rewatch", tint = if (isCurrentOrFutureMonth) AppText().copy(alpha = 0.5f) else AppPrimary(), containerColor = androidx.compose.ui.graphics.Color.Transparent)
            Spacer(modifier = Modifier.width(8.dp))
            val btnText = if (isCurrentOrFutureMonth) "Wrapped $currentSelectedMonthName $selectedYear (Coming Soon) ✨" else "Putar Ulang Wrapped $currentSelectedMonthName $selectedYear ✨"
            Text(btnText, color = if (isCurrentOrFutureMonth) AppText().copy(alpha = 0.5f) else AppPrimary(), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SMART INSIGHTS GENERATION
        val insightMessage = remember(income, expenses, expensePerCat) {
            if (income == 0L && expenses == 0L) {
                AppStr.repNoData
            } else if (income > expenses * 1.5) {
                AppStr.repGreat
            } else if (expenses > income) {
                AppStr.repWarn
            } else if (expensePerCat.isNotEmpty()) {
                val topCat = expensePerCat.first()
                val pct = ((topCat.second.toFloat() / expenses.toFloat()) * 100).toInt()
                String.format(AppStr.repTop, topCat.first, pct)
            } else {
                AppStr.repStable
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(24.dp, AppPrimary().copy(alpha = 0.15f), useHaze = true)
                .border(1.dp, AppPrimary().copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KumaExpressiveIcon(Icons.Default.AutoAwesome, null, tint = AppPrimary(), containerColor = androidx.compose.ui.graphics.Color.Transparent, size = 32.dp, iconPadding = 4.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(AppStr.smartInsights, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = AppPrimary())
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(insightMessage, fontSize = 12.sp, color = AppText(), lineHeight = 16.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val animatedBal by androidx.compose.animation.core.animateFloatAsState(targetValue = abs(balance).toFloat(), animationSpec = androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.FastOutSlowInEasing), label = "bal")
        val animatedInc by androidx.compose.animation.core.animateFloatAsState(targetValue = income.toFloat(), animationSpec = androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.FastOutSlowInEasing), label = "inc")
        val animatedExp by androidx.compose.animation.core.animateFloatAsState(targetValue = expenses.toFloat(), animationSpec = androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.FastOutSlowInEasing), label = "exp")
        val balPref = if (balance < 0) "- " else "+"

        // BALANCE CARD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(32.dp, AppSurfaceVariant())
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(AppStr.net, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppText().copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(8.dp))
                AutoSizeText(text = "$curSym $balPref${NumberFormat.getInstance(locale).format(animatedBal.toLong())}", modifier = Modifier.fillMaxWidth(), fontSize = 42.sp, fontWeight = FontWeight.Black, color = AppText(), minimumFallbackSize = 20.sp, textAlign = TextAlign.Center)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        // INCOME EXPENSE ROW
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // INCOME
            Box(modifier = Modifier.weight(1f).glassCard(24.dp, AppGreen().copy(alpha = 0.1f)).border(1.dp, AppGreen().copy(alpha = 0.2f), RoundedCornerShape(24.dp)).padding(16.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KumaExpressiveIcon(Icons.Default.ArrowUpward, null, tint = AppGreen(), containerColor = AppGreen().copy(alpha=0.2f), size = 28.dp, iconPadding = 6.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(AppStr.inc, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppText().copy(alpha = 0.7f))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    AutoSizeText(text = "$curSym ${NumberFormat.getInstance(locale).format(animatedInc.toLong())}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = AppText(), minimumFallbackSize = 12.sp)
                }
            }
            // EXPENSE
            Box(modifier = Modifier.weight(1f).glassCard(24.dp, AppRed().copy(alpha = 0.1f)).border(1.dp, AppRed().copy(alpha = 0.2f), RoundedCornerShape(24.dp)).padding(16.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KumaExpressiveIcon(Icons.Default.ArrowDownward, null, tint = AppRed(), containerColor = AppRed().copy(alpha=0.2f), size = 28.dp, iconPadding = 6.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(AppStr.exp, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppText().copy(alpha = 0.7f))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    AutoSizeText(text = "$curSym ${NumberFormat.getInstance(locale).format(animatedExp.toLong())}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = AppText(), minimumFallbackSize = 12.sp)
                }
            }
        }

        if (profile.monthlyTarget > 0) {
            Spacer(modifier = Modifier.height(20.dp))
            val progress = (expenses.toFloat() / profile.monthlyTarget.toFloat()).coerceIn(0f, 1f)
            val isOver = expenses > profile.monthlyTarget

            Text(AppStr.targetProg, fontWeight = FontWeight.Bold, color = AppText())
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape), color = if(isOver) AppRed() else AppGreen(), trackColor = AppSurfaceVariant())
            Text("${(progress * 100).toInt()}% " + (if(AppStr.isId) "dari" else "of") + " $curSym ${NumberFormat.getInstance(locale).format(profile.monthlyTarget)}", fontSize = 12.sp, color = if(isOver) AppRed() else Color.Gray)
        }

        Spacer(modifier = Modifier.height(30.dp))
        Text(AppStr.spendBreak, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = AppText())
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(32.dp, AppSurface()),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(220.dp).padding(16.dp), contentAlignment = Alignment.Center) {
                    val bgArcCol = AppSurfaceVariant()
                    var selectedCategory by remember { mutableStateOf<String?>(null) }
                    val animatedSweep by androidx.compose.animation.core.animateFloatAsState(targetValue = 1f, animationSpec = androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.FastOutSlowInEasing), label = "sweep")
                    
                    Canvas(modifier = Modifier.fillMaxSize().pointerInput(expensePerCat, expenses) {
                        detectTapGestures { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = offset.x - center.x
                            val dy = offset.y - center.y
                            val dist = Math.hypot(dx.toDouble(), dy.toDouble())
                            // Inner radius approx size.width/2 - 35.dp, outer size.width/2
                            if (dist > (size.width/2f - 80f) && dist < (size.width/2f + 20f)) {
                                var angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                if (angle < 0) angle += 360f
                                val tapAngle = (angle + 90f) % 360f
                                
                                var start = 0f
                                var found = false
                                for ((cat, amt) in expensePerCat) {
                                    val sweep = (amt.toFloat() / expenses.toFloat()) * 360f
                                    if (tapAngle >= start && tapAngle <= start + sweep) {
                                        selectedCategory = if (selectedCategory == cat) null else cat
                                        found = true
                                        break
                                    }
                                    start += sweep
                                }
                                if (!found) selectedCategory = null
                            } else {
                                selectedCategory = null
                            }
                        }
                    }) {
                        if (expenses == 0L) {
                            drawArc(color = bgArcCol, startAngle = -90f, sweepAngle = 360f * animatedSweep, useCenter = false, style = Stroke(25.dp.toPx(), cap = StrokeCap.Butt))
                        } else {
                            var start = -90f
                            expensePerCat.forEach { (cat, amt) ->
                                val sweep = (amt.toFloat() / expenses.toFloat()) * 360f * animatedSweep
                                val isSelected = (selectedCategory == cat)
                                
                                val middleAngle = start + (sweep / 2)
                                val popOutOffset = if (isSelected) 10.dp.toPx() else 0f
                                val popX = kotlin.math.cos(Math.toRadians(middleAngle.toDouble())).toFloat() * popOutOffset
                                val popY = kotlin.math.sin(Math.toRadians(middleAngle.toDouble())).toFloat() * popOutOffset
                                
                                val strokeWidth = if (isSelected) 35.dp.toPx() else 25.dp.toPx()
                                val sweepGap = if (expensePerCat.size > 1) 2f else 0f
                                
                                withTransform({
                                    translate(popX, popY)
                                }) {
                                    drawArc(
                                        color = getCatColor(cat),
                                        startAngle = start,
                                        sweepAngle = maxOf(0.1f, sweep - sweepGap),
                                        useCenter = false,
                                        style = Stroke(strokeWidth, cap = StrokeCap.Butt)
                                    )
                                }
                                start += sweep
                            }
                        }
                    }
                    val animatedExp by androidx.compose.animation.core.animateFloatAsState(targetValue = expenses.toFloat(), animationSpec = androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.FastOutSlowInEasing), label = "expCenter")
                    AutoSizeText(text = "$curSym ${NumberFormat.getInstance(locale).format(animatedExp.toLong())}", modifier = Modifier.padding(24.dp).fillMaxWidth(), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = AppText(), minimumFallbackSize = 12.sp, textAlign = TextAlign.Center)
                }

                if (expenses == 0L) {
                    Text(AppStr.noData, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppText().copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(24.dp, AppSurfaceVariant(), useHaze = true)
                            .padding(vertical = 8.dp)
                    ) {
                        var index = 0
                        val itemsToShow = if (showAllCategories) expensePerCat else expensePerCat.take(5)
                        
                        itemsToShow.forEach { (label, amt) ->
                            val target = catTargets.optLong(label, 0L)
                            val catCol = getCatColor(label)
                            val progress = if (target > 0) (amt.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f
                            val isOverLimit = target > 0 && amt > target

                            val iconKey = savedIcons.optString(label, "")
                            val icon = kumaIconLibrary[iconKey] ?: when(label) {
                                "Financial" -> Icons.Default.AccountBalance
                                "Food" -> Icons.Default.Restaurant
                                "Shopping" -> Icons.Default.LocalMall
                                "Health" -> Icons.Default.Favorite
                                "Transport" -> Icons.Default.DirectionsCar
                                "Education" -> Icons.Default.School
                                "Entertainment" -> Icons.Default.Gamepad
                                "Transfer" -> Icons.Default.SyncAlt
                                else -> Icons.Default.DashboardCustomize
                            }

                            Box(modifier = Modifier.fillMaxWidth().height(65.dp)) {
                                if (target > 0) Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progress).background(catCol.copy(alpha = 0.15f)).align(Alignment.CenterStart))
                                Row(modifier = Modifier.padding(horizontal = 16.dp).fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                                    KumaExpressiveIcon(icon, contentDescription = null, tint = catCol, containerColor = catCol.copy(alpha = 0.2f), size = 36.dp, iconPadding = 9.dp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppText(), modifier = Modifier.weight(1f))
                                    Text("$curSym ${NumberFormat.getInstance(locale).format(amt)}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = AppText())
                                }
                                if (target > 0) {
                                    val budgetInfo = if(isOverLimit) "$curSym ${NumberFormat.getInstance(locale).format(amt-target)} OVER!" else "$curSym ${NumberFormat.getInstance(locale).format(target-amt)} left"
                                    Text(budgetInfo, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if(isOverLimit) AppRed() else AppText().copy(alpha=0.6f), modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 6.dp))
                                }
                            }

                            if (index < itemsToShow.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 60.dp),
                                    color = AppText().copy(alpha = 0.05f)
                                )
                            }
                            index++
                        }
                        
                        if (expensePerCat.size > 5) {
                            TextButton(onClick = { showAllCategories = !showAllCategories }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (showAllCategories) AppStr.showLess else "${AppStr.showMore} (${expensePerCat.size - 5})", color = AppPrimary(), fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    KumaExpressiveIcon(if (showAllCategories) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = AppPrimary(), containerColor = androidx.compose.ui.graphics.Color.Transparent)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
        Text(AppStr.trends, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = AppText())
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .glassCard(32.dp, AppSurface()),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                val greenCol = AppGreen()
                val redCol = AppRed()
                val variantCol = AppSurfaceVariant()
                val textCol = AppText()

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    LegendItem(AppStr.inc, greenCol)
                    Spacer(modifier = Modifier.width(16.dp))
                    LegendItem(AppStr.exp, redCol)
                }

                Spacer(modifier = Modifier.height(16.dp))

                val incomeData = FloatArray(5) { 0f }
                val expenseData = FloatArray(5) { 0f }
                val monthLabels = mutableListOf<String>()
                val targetMonths = mutableListOf<Pair<Int, Int>>()

                val cal = Calendar.getInstance()
                cal.set(Calendar.YEAR, selectedYear)
                cal.set(Calendar.MONTH, selectedMonth - 1)

                for (i in 4 downTo 0) {
                    val tempCal = cal.clone() as Calendar
                    tempCal.add(Calendar.MONTH, -i)
                    val m = tempCal.get(Calendar.MONTH) + 1
                    val y = tempCal.get(Calendar.YEAR)
                    targetMonths.add(Pair(m, y))
                    val monthName = tempCal.getDisplayName(Calendar.MONTH, Calendar.SHORT, locale) ?: ""
                    monthLabels.add(monthName)
                }

                allTransactions.forEach { t ->
                    try {
                        val dt = LocalDateTime.parse(t.timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        val txMonth = dt.monthValue
                        val txYear = dt.year
                        val idx = targetMonths.indexOf(Pair(txMonth, txYear))
                        if (idx != -1) {
                            val amt = t.amount.toFloatOrNull() ?: 0f
                            if (t.isIncome) incomeData[idx] += amt else expenseData[idx] += amt
                        }
                    } catch (_: Exception) {}
                }

                val maxVal = maxOf(incomeData.maxOrNull() ?: 0f, expenseData.maxOrNull() ?: 0f).coerceAtLeast(1f)
                val incPoints = incomeData.map { it / maxVal }
                val expPoints = expenseData.map { it / maxVal }
                val hasData = incomeData.sum() > 0f || expenseData.sum() > 0f

                val isDark = LocalIsDark.current
                val isBrutal = com.bearbones.kumaflow.ui.theme.LocalIsBrutal.current
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val gridColVertical = if (isDark) Color.White.copy(alpha = 0.15f) else variantCol.copy(alpha = 0.5f)
                        val gridColHorizontal = if (isDark) Color.White.copy(alpha = 0.1f) else variantCol.copy(alpha = 0.3f)
                        for (i in 0..4) {
                            val x = i * size.width / 4
                            drawLine(color = gridColVertical, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1.dp.toPx())
                        }
                        for (i in 0..5) {
                            val y = size.height - (i * size.height / 5)
                            drawLine(color = gridColHorizontal, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1.dp.toPx())
                        }
                        if (hasData) {
                            val isBrut = isBrutal // Wait, let's pass `com.bearbones.kumaflow.ui.theme.LocalIsBrutal.current` from Compose scope
                            drawTrendsArea(incPoints, greenCol, isBrut)
                            drawTrendsArea(expPoints, redCol, isBrut)
                        }
                    }
                    if (!hasData) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(AppStr.noTrendData, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textCol.copy(alpha = 0.5f))
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    monthLabels.forEachIndexed { index, monthStr ->
                        val isCurrentSelected = index == 4
                        Text(monthStr, fontSize = if(isCurrentSelected) 12.sp else 10.sp, fontWeight = if(isCurrentSelected) FontWeight.Black else FontWeight.Bold, color = if(isCurrentSelected) greenCol else textCol)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding() + 24.dp))
    }
}
fun DrawScope.drawTrendsArea(points: List<Float>, color: Color, isBrutal: Boolean = false) {
    if (points.size < 2) return
    val w = size.width
    val h = size.height
    val path = Path()
    val fillPath = Path()

    path.moveTo(0f, h - (points[0] * h))
    fillPath.moveTo(0f, h)
    fillPath.lineTo(0f, h - (points[0] * h))

    val step = w / (points.size - 1)

    for (i in 0 until points.size - 1) {
        val x1 = i * step
        val y1 = h - (points[i] * h)
        val x2 = (i + 1) * step
        val y2 = h - (points[i+1] * h)
        val cX = (x1 + x2) / 2f
        path.cubicTo(cX, y1, cX, y2, x2, y2)
        fillPath.cubicTo(cX, y1, cX, y2, x2, y2)
    }

    fillPath.lineTo(w, h)
    fillPath.lineTo(0f, h)
    fillPath.close()

    drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
            colors = listOf(color.copy(alpha = if (isBrutal) 0.6f else 0.4f), color.copy(alpha = 0.0f)),
            startY = 0f,
            endY = h
        )
    )
    
    val strokeWidth = if (isBrutal) 5.dp.toPx() else 3.dp.toPx()

    if (isBrutal) {
        translate(left = 4.dp.toPx(), top = 4.dp.toPx()) {
            drawPath(
                path = path,
                color = Color.Black.copy(alpha = 1f),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
        drawPath(
            path = path,
            color = Color.Black,
            style = Stroke(width = strokeWidth + 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    val lastX = w
    val lastY = h - (points.last() * h)
    
    if (isBrutal) {
        drawCircle(Color.Black, radius = 7.dp.toPx(), center = Offset(lastX + 2.dp.toPx(), lastY + 2.dp.toPx()))
        drawCircle(Color.Black, radius = 8.dp.toPx(), center = Offset(lastX, lastY))
    }
    
    drawCircle(color, radius = 6.dp.toPx(), center = Offset(lastX, lastY))
    drawCircle(Color.White, radius = 3.dp.toPx(), center = Offset(lastX, lastY))
}




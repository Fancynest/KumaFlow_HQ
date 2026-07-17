@file:Suppress("SpellCheckingInspection")

package com.bearbones.kumaflow.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumaflow.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun ContributionHeatmap(
    allTransactions: List<TransactionWithSplits>,
    modifier: Modifier = Modifier
) {
    val isId = AppStr.isId

    // Aggregate: expense per day for last ~6 months (182 days)
    val today = remember { LocalDate.now() }
    val startDate = remember { today.minusDays(181) }

    val dailyExpense = remember(allTransactions) {
        val map = mutableMapOf<LocalDate, Long>()
        allTransactions.forEach { txObj ->
            val tx = txObj.transaction
            if (!tx.isIncome) {
                try {
                    val dt = java.time.LocalDateTime.parse(
                        tx.timestamp,
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME
                    )
                    val date = dt.toLocalDate()
                    if (!date.isBefore(startDate) && !date.isAfter(today)) {
                        map[date] = (map[date] ?: 0L) + (tx.amount.toLongOrNull() ?: 0L)
                    }
                } catch (_: Exception) {}
            }
        }
        map
    }

    // Calculate monthly average for color thresholds
    val avgDaily = remember(dailyExpense) {
        if (dailyExpense.isEmpty()) 50000.0
        else dailyExpense.values.average()
    }

    val emptyColor = AppSurfaceVariant()
    val borderColor = AppText().copy(alpha = 0.2f)

    // Color mapping: green = below avg (hemat), yellow = around avg, red = above avg (boros)
    fun getHeatColor(amount: Long): Color {
        if (amount == 0L) return emptyColor // empty/no tx
        val ratio = amount.toDouble() / avgDaily
        return when {
            ratio <= 0.5 -> Color(0xFF0E4429) // very hemat
            ratio <= 0.8 -> Color(0xFF006D32) // hemat  
            ratio <= 1.2 -> Color(0xFF26A641) // normal
            ratio <= 1.8 -> Color(0xFFFFB020) // agak boros
            else -> Color(0xFFE53935)          // boros
        }
    }

    val cellSize = 14f
    val cellGap = 3f
    val totalCellSize = cellSize + cellGap

    // Align start to Monday of that week
    val adjustedStart = remember {
        var d = startDate
        while (d.dayOfWeek != DayOfWeek.MONDAY) d = d.minusDays(1)
        d
    }

    val totalDays = remember { ChronoUnit.DAYS.between(adjustedStart, today).toInt() + 1 }
    val weeks = remember { (totalDays + 6) / 7 }

    val scrollState = rememberScrollState(Int.MAX_VALUE) // scroll to end (today)

    Column(modifier = modifier) {
        Text(
            text = if (isId) "Peta Pengeluaran" else "Expense Heatmap",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            color = AppText()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Day labels
        val dayLabels = if (isId) listOf("S", "S", "R", "K", "J", "S", "M") else listOf("M", "T", "W", "T", "F", "S", "S")

        Row(modifier = Modifier.horizontalScroll(scrollState)) {
            // Day labels column
            Column(modifier = Modifier.padding(end = 4.dp)) {
                dayLabels.forEach { label ->
                    Box(
                        modifier = Modifier.size(totalCellSize.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontSize = 8.sp, color = AppText().copy(alpha = 0.5f))
                    }
                }
            }

            // Heatmap grid via Canvas
            Canvas(
                modifier = Modifier
                    .width((weeks * totalCellSize).dp)
                    .height((7 * totalCellSize).dp)
            ) {
                for (week in 0 until weeks) {
                    for (day in 0..6) {
                        val dayIndex = week * 7 + day
                        val date = adjustedStart.plusDays(dayIndex.toLong())
                        if (date.isAfter(today)) continue

                        val amount = dailyExpense[date] ?: 0L
                        val color = getHeatColor(amount)

                        val x = week * totalCellSize.dp.toPx()
                        val y = day * totalCellSize.dp.toPx()

                        drawRoundRect(
                            color = color,
                            topLeft = Offset(x, y),
                            size = Size(cellSize.dp.toPx(), cellSize.dp.toPx()),
                            cornerRadius = CornerRadius(3.dp.toPx())
                        )
                        drawRoundRect(
                            color = borderColor,
                            topLeft = Offset(x, y),
                            size = Size(cellSize.dp.toPx(), cellSize.dp.toPx()),
                            cornerRadius = CornerRadius(3.dp.toPx()),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Legend
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(if (isId) "Hemat" else "Less", fontSize = 10.sp, color = AppText().copy(alpha = 0.5f))
            listOf(
                emptyColor,
                Color(0xFF0E4429),
                Color(0xFF006D32),
                Color(0xFF26A641),
                Color(0xFFFFB020),
                Color(0xFFE53935)
            ).forEach { c ->
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawRoundRect(
                        color = c,
                        cornerRadius = CornerRadius(2.dp.toPx())
                    )
                    drawRoundRect(
                        color = borderColor,
                        cornerRadius = CornerRadius(2.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                    )
                }
            }
            Text(if (isId) "Boros" else "More", fontSize = 10.sp, color = AppText().copy(alpha = 0.5f))
        }
    }
}

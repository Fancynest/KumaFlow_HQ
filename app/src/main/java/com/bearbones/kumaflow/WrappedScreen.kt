package com.bearbones.kumaflow

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.absoluteValue
import com.bearbones.kumaflow.ui.components.KumaButton
import com.bearbones.kumaflow.ui.components.KumaIconButton

@Composable
fun WrappedScreen(
    profile: UserProfile,
    prevMonthTransactions: List<KumaTransaction>,
    monthName: String,
    onClose: () -> Unit
) {
    val locale = Locale.forLanguageTag("id-ID")
    val isId = AppStr.isId // Retrieve the locale settings from the main application context

    val curSym = when(profile.currency) {
        "USD", "AUD", "CAD", "SGD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        "JPY", "CNY" -> "¥"
        "CHF" -> "CHF"
        "MYR" -> "RM"
        "THB" -> "฿"
        "PHP" -> "₱"
        "VND" -> "₫"
        else -> "Rp"
    }

    val googleFont = FontFamily.SansSerif

    // --- WRAPPED DATA CALCULATION ---
    val expenses = prevMonthTransactions.filter { !it.isIncome }
    val incomes = prevMonthTransactions.filter { it.isIncome }

    val totalExp = expenses.sumOf { it.amount.toLongOrNull() ?: 0L }
    val totalInc = incomes.sumOf { it.amount.toLongOrNull() ?: 0L }

    val categoryGroup = expenses.groupBy { it.category }.mapValues { it.value.sumOf { t -> t.amount.toLongOrNull() ?: 0L } }
    val topCategory = categoryGroup.maxByOrNull { it.value }?.key ?: if (isId) "Belum ada" else "Nothing yet"
    val topCatAmount = categoryGroup[topCategory] ?: 0L

    val biggestTx = expenses.maxByOrNull { it.amount.toLongOrNull() ?: 0L }
    val biggestInc = incomes.maxByOrNull { it.amount.toLongOrNull() ?: 0L }

    val persona = when {
        totalExp == 0L -> if (isId) "Sepuh Frugal Living \uD83E\uDDDD\u200D♂\uFE0F" else "Frugal Living Master \uD83E\uDDDD\u200D♂\uFE0F"
        categoryGroup["Food"] ?: 0L > totalExp / 3 -> if (isId) "Foodie Sejati \uD83C\uDF54" else "Certified Foodie \uD83C\uDF54"
        categoryGroup["Shopping"] ?: 0L > totalExp / 3 -> if (isId) "Trendsetter FOMO \uD83D\uDECD\uFE0F" else "FOMO Trendsetter \uD83D\uDECD\uFE0F"
        totalExp > totalInc && totalInc > 0L -> if (isId) "Donatur Tetap Kafe ☕" else "Cafe's Sugar Daddy ☕"
        else -> if (isId) "Si Paling Bijak \uD83E\uDD13" else "The Wise Spender \uD83E\uDD13"
    }

    // --- INSTAGRAM STORY-STYLE LOGIC (DURATION EXTENDED TO 8 SECONDS) ---
    val pages = 6
    val pagerState = rememberPagerState(pageCount = { pages })
    val coroutineScope = rememberCoroutineScope()

    var isPaused by remember { mutableStateOf(false) }
    val progressAnim = remember { Animatable(0f) }

    // 🔥 BUG FIX: Reset the active timer upon page transition
    LaunchedEffect(pagerState.currentPage) {
        progressAnim.snapTo(0f)
    }

    // 🔥 BUG FIX: Decouple the screen transition animation from the pause effect
    LaunchedEffect(pagerState.currentPage, isPaused) {
        if (!isPaused) {
            val remainingTime = ((1f - progressAnim.value) * 8000).toInt() // Current animation duration is set to 8 seconds
            if (remainingTime > 0) {
                progressAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = remainingTime, easing = LinearEasing)
                )
            }

            // Upon reaching 100% completion, trigger the page transition via an external coroutine to prevent cancellation upon touch events
            if (progressAnim.value >= 1f && pagerState.currentPage < pages - 1) {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                }
            }
        }
    }

    val neonColors = listOf(
        Color(0xFFFF0055), // Pink
        Color(0xFF00FFCC), // Cyan
        Color(0xFFFFD500), // Yellow
        Color(0xFF00FF33), // Green
        Color(0xFFB000FF), // Purple
        Color(0xFFFF5500)  // Orange
    )

    val bgImages = listOf(
        R.drawable.bg_slide_1,
        R.drawable.bg_slide_2,
        R.drawable.bg_slide_3,
        R.drawable.bg_slide_4,
        R.drawable.bg_slide_5,
        R.drawable.bg_slide_6
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                        onTap = { offset ->
                            coroutineScope.launch {
                                if (offset.x < size.width / 3) {
                                    if (pagerState.currentPage > 0) pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                } else {
                                    if (pagerState.currentPage < pages - 1) pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        }
                    )
                }
        ) { page ->

            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue

            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = bgImages[page % bgImages.size]),
                    contentDescription = "Background Slide $page",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                        .graphicsLayer {
                            val scale = 1f - (pageOffset * 0.15f).coerceIn(0f, 0.2f)
                            val alphaFade = 1f - (pageOffset * 1.5f).coerceIn(0f, 1f)
                            scaleX = scale
                            scaleY = scale
                            alpha = alphaFade
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val pageColor = neonColors[page % neonColors.size]

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .border(2.dp, pageColor, RoundedCornerShape(32.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                        shape = RoundedCornerShape(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            when (page) {
                                0 -> {
                                    Text(if(isId) "BULAN INI" else "THIS MONTH", fontFamily = googleFont, fontSize = 14.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(if(isId) "Kamu udah ngeluarin duit sebanyak..." else "You've successfully burned through...", fontFamily = googleFont, color = Color.White, fontSize = 20.sp, textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text("$curSym ${NumberFormat.getInstance(locale).format(totalExp)}", fontFamily = googleFont, fontSize = 42.sp, fontWeight = FontWeight.Black, color = pageColor, textAlign = TextAlign.Center, lineHeight = 48.sp)
                                    Spacer(modifier = Modifier.height(24.dp))
                                    val textEmpty = if(isId) "Lagi puasa jajan ya? Hebat bener \uD83D\uDE31" else "Fasting from spending? Absolute legend \uD83D\uDE31"
                                    val textBusy = if(isId) "Lumayan sibuk ya dompetmu bulan ini! \uD83D\uDE80" else "Your wallet has been working overtime! \uD83D\uDE80"
                                    Text(if (totalExp == 0L) textEmpty else textBusy, fontFamily = googleFont, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, textAlign = TextAlign.Center)
                                }
                                1 -> {
                                    Text(if(isId) "TOP KATEGORI" else "TOP CATEGORY", fontFamily = googleFont, fontSize = 14.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(if(isId) "Ternyata, dana kamu paling deres ngalir ke..." else "Looks like most of your cash flowed into...", fontFamily = googleFont, color = Color.White, fontSize = 20.sp, textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(topCategory.uppercase(), fontFamily = googleFont, fontSize = 42.sp, fontWeight = FontWeight.Black, color = pageColor, textAlign = TextAlign.Center, lineHeight = 48.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("$curSym ${NumberFormat.getInstance(locale).format(topCatAmount)}", fontFamily = googleFont, fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(if(isId) "Asal bikin happy, sesekali gapapa dong! ✨" else "Hey, as long as it brings joy, right? ✨", fontFamily = googleFont, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, textAlign = TextAlign.Center)
                                }
                                2 -> {
                                    Text(if(isId) "TRANSAKSI TERGILA" else "CRAZIEST SPEND", fontFamily = googleFont, fontSize = 14.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(if(isId) "Momen pengeluaran paling brutal jatuh kepada..." else "The most brutal damage to your balance goes to...", fontFamily = googleFont, color = Color.White, fontSize = 20.sp, textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(biggestTx?.name ?: if(isId) "Kosong" else "Nothing", fontFamily = googleFont, fontSize = 32.sp, fontWeight = FontWeight.Black, color = pageColor, textAlign = TextAlign.Center, lineHeight = 36.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("$curSym ${NumberFormat.getInstance(locale).format(biggestTx?.amount?.toLongOrNull() ?: 0L)}", fontFamily = googleFont, fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(if(isId) "Semoga beneran kepake dan worth it ya! \uD83D\uDE4F" else "Let's hope this was actually worth it! \uD83D\uDE4F", fontFamily = googleFont, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, textAlign = TextAlign.Center)
                                }
                                3 -> {
                                    Text(if(isId) "PAHLAWAN PEMASUKAN" else "INCOME SAVIOR", fontFamily = googleFont, fontSize = 14.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(if(isId) "Kabar baiknya, ada rezeki nomplok dari..." else "The good news is, you got a solid injection from...", fontFamily = googleFont, color = Color.White, fontSize = 20.sp, textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(biggestInc?.name ?: if(isId) "Belum ada rejeki" else "No income yet", fontFamily = googleFont, fontSize = 32.sp, fontWeight = FontWeight.Black, color = pageColor, textAlign = TextAlign.Center, lineHeight = 36.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("$curSym ${NumberFormat.getInstance(locale).format(biggestInc?.amount?.toLongOrNull() ?: 0L)}", fontFamily = googleFont, fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(24.dp))
                                    val textZeroInc = if(isId) "Bulan depan pasti ada, semangat! \uD83D\uDCAA" else "Next month for sure, keep grinding! \uD83D\uDCAA"
                                    val textHasInc = if(isId) "Kerja keras terbayar lunas! Lanjutkan! \uD83D\uDD25" else "Hard work paid off! Keep it burning! \uD83D\uDD25"
                                    Text(if (biggestInc == null) textZeroInc else textHasInc, fontFamily = googleFont, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, textAlign = TextAlign.Center)
                                }
                                4 -> {
                                    Text("FINANCIAL PERSONA", fontFamily = googleFont, fontSize = 14.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(if(isId) "Berdasarkan gaya jajanmu, gelar yang paling cocok buat kamu adalah..." else "Based on your habits, your ultimate financial title is...", fontFamily = googleFont, color = Color.White, fontSize = 20.sp, textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(persona, fontFamily = googleFont, fontSize = 36.sp, fontWeight = FontWeight.Black, color = pageColor, textAlign = TextAlign.Center, lineHeight = 42.sp)
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(if(isId) "Kira-kira bulan depan gelarnya bakal berubah kaga nih? \uD83E\uDD14" else "Will this title survive until next month? \uD83E\uDD14", fontFamily = googleFont, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, textAlign = TextAlign.Center)
                                }
                                5 -> {
                                    Text("THAT'S A WRAP!", fontFamily = googleFont, fontSize = 14.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(if(isId) "Perjalanan keuangan $monthName kamu resmi ditutup." else "Your financial journey for $monthName is officially closed.", fontFamily = googleFont, color = Color.White, fontSize = 24.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(32.dp))

                                    KumaButton(
                                        onClick = onClose,
                                        colors = ButtonDefaults.buttonColors(containerColor = pageColor),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth().height(55.dp)
                                    ) {
                                        Text(if(isId) "SIAP BUAT BULAN INI \uD83D\uDCAA" else "READY FOR THIS MONTH \uD83D\uDCAA", fontFamily = googleFont, color = Color.Black, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 0 until pages) {
                LinearProgressIndicator(
                    progress = {
                        when {
                            i < pagerState.currentPage -> 1f
                            i == pagerState.currentPage -> progressAnim.value
                            else -> 0f
                        }
                    },
                    modifier = Modifier.weight(1f).height(3.dp).clip(CircleShape),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }

        KumaIconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 64.dp, end = 16.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
        }
    }
}
package com.bearbones.kumaflow.ui.tutorial

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.*
import com.bearbones.kumaflow.neobrutalism

enum class TutorialStep {
    NONE,
    HOME_SUMMARY,        // Highlight top summary area
    HOME_WALLETS,        // Highlight the wallet list
    HOME_ADD_BTN,        // Highlight the bottom right + button
    ADD_TX_TABS,         // Highlight Income/Expenses/Transfer tabs
    ADD_TX_DATE,         // Highlight Date field
    ADD_TX_CATEGORY,     // Highlight Category field
    ADD_TX_TITLE_NOTES,  // Highlight Title & Notes
    ADD_TX_FUNDING,      // Highlight Funding source
    ADD_TX_SAVE          // Highlight Save button
}

class TutorialState(initialStep: TutorialStep = TutorialStep.NONE) {
    var currentStep by mutableStateOf(initialStep)
    
    private val stepBounds = mutableStateMapOf<TutorialStep, Rect>()

    fun setBounds(step: TutorialStep, coordinates: LayoutCoordinates) {
        stepBounds[step] = coordinates.boundsInRoot()
    }

    fun getBounds(step: TutorialStep): Rect? {
        return stepBounds[step]
    }
    
    fun next() {
        val values = TutorialStep.values()
        val nextIndex = currentStep.ordinal + 1
        if (nextIndex < values.size) {
            currentStep = values[nextIndex]
        } else {
            currentStep = TutorialStep.NONE
        }
    }
    
    fun advanceTo(step: TutorialStep) {
        currentStep = step
    }
}

val LocalTutorialState = compositionLocalOf { TutorialState() }

@Composable
fun Modifier.tutorialTarget(step: TutorialStep): Modifier {
    val tutorialState = LocalTutorialState.current
    return this.onGloballyPositioned { coordinates ->
        tutorialState.setBounds(step, coordinates)
    }
}

@Composable
fun TutorialOverlay(
    onComplete: () -> Unit
) {
    val tutorialState = LocalTutorialState.current
    val currentStep = tutorialState.currentStep
    
    if (currentStep == TutorialStep.NONE) return

    val targetBounds = tutorialState.getBounds(currentStep)
    val text = getTutorialText(currentStep)
    val density = LocalDensity.current
    
    val isInteractiveStep = currentStep == TutorialStep.HOME_ADD_BTN

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (!isInteractiveStep) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { tutorialState.next() }
            )
        } else if (targetBounds != null) {
            val padding = with(density) { 8.dp.toPx() }
            val topH = with(density) { (targetBounds.top - padding).coerceAtLeast(0f).toDp() }
            val botY = with(density) { (targetBounds.bottom + padding).toDp() }
            val leftW = with(density) { (targetBounds.left - padding).coerceAtLeast(0f).toDp() }
            val rightX = with(density) { (targetBounds.right + padding).toDp() }
            val midH = with(density) { ((targetBounds.bottom + padding) - (targetBounds.top - padding)).coerceAtLeast(0f).toDp() }

            val blocker = Modifier.pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                            .changes.forEach { it.consume() }
                    }
                }
            }

            Box(Modifier.fillMaxWidth().height(topH).then(blocker))
            Box(Modifier.fillMaxWidth().padding(top = botY).fillMaxHeight().then(blocker))
            Box(Modifier.padding(top = topH).width(leftW).height(midH).then(blocker))
            Box(Modifier.padding(top = topH, start = rightX).fillMaxWidth().height(midH).then(blocker))
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val path = Path().apply {
                addRect(Rect(0f, 0f, canvasWidth, canvasHeight))
            }
            
            if (targetBounds != null) {
                val highlightPath = Path().apply {
                    val padding = 8.dp.toPx()
                    addRoundRect(
                        RoundRect(
                            left = targetBounds.left - padding,
                            top = targetBounds.top - padding,
                            right = targetBounds.right + padding,
                            bottom = targetBounds.bottom + padding,
                            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                        )
                    )
                }
                
                clipPath(path = highlightPath, clipOp = androidx.compose.ui.graphics.ClipOp.Difference) {
                    drawRect(color = Color.Black.copy(alpha = 0.75f))
                }
            } else {
                drawRect(color = Color.Black.copy(alpha = 0.75f))
            }
        }
        
        if (targetBounds != null) {
            val screenHeight = with(density) { targetBounds.bottom } // just use coordinates
            val tooltipTop = if (targetBounds.bottom > 1000f) {
                with(density) { (targetBounds.top - 200.dp.toPx()).toDp() }
            } else {
                with(density) { (targetBounds.bottom + 20.dp.toPx()).toDp() }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .offset(y = tooltipTop)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neobrutalism(isBrutal = true, backgroundColor = Color.White, cornerRadius = 16.dp)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (!isInteractiveStep) {
                        Text(
                            text = if (currentStep == TutorialStep.ADD_TX_SAVE) com.bearbones.kumaflow.AppStr.tutFinishBtn else com.bearbones.kumaflow.AppStr.tutTapNext,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = com.bearbones.kumaflow.AppStr.tutClickBtn,
                            fontSize = 12.sp,
                            color = com.bearbones.kumaflow.ui.theme.BrutalGreen,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
        
        LaunchedEffect(currentStep) {
            if (currentStep == TutorialStep.NONE) {
                onComplete()
            }
        }
    }
}

fun getTutorialText(step: TutorialStep): String {
    return when (step) {
        TutorialStep.HOME_SUMMARY -> com.bearbones.kumaflow.AppStr.tutSummary
        TutorialStep.HOME_WALLETS -> com.bearbones.kumaflow.AppStr.tutWallets
        TutorialStep.HOME_ADD_BTN -> com.bearbones.kumaflow.AppStr.tutAddBtn
        TutorialStep.ADD_TX_TABS -> com.bearbones.kumaflow.AppStr.tutTabs
        TutorialStep.ADD_TX_DATE -> com.bearbones.kumaflow.AppStr.tutDate
        TutorialStep.ADD_TX_CATEGORY -> com.bearbones.kumaflow.AppStr.tutCategory
        TutorialStep.ADD_TX_TITLE_NOTES -> com.bearbones.kumaflow.AppStr.tutTitleNotes
        TutorialStep.ADD_TX_FUNDING -> com.bearbones.kumaflow.AppStr.tutFunding
        TutorialStep.ADD_TX_SAVE -> com.bearbones.kumaflow.AppStr.tutSave
        TutorialStep.NONE -> ""
    }
}

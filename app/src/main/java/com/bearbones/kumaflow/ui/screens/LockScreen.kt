@file:Suppress("SpellCheckingInspection", "UNUSED_PARAMETER", "unused", "CanBeVal", "DEPRECATION", "ScheduleExactAlarm")
@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bearbones.kumaflow

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline

import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import androidx.compose.ui.draw.blur
import androidx.compose.animation.core.animateDpAsState
import dev.chrisbanes.haze.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.CornerRounding
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import com.bearbones.kumaflow.utils.bouncyScale
import com.bearbones.kumaflow.utils.PolygonShape
import androidx.graphics.shapes.circle
import androidx.compose.ui.graphics.graphicsLayer
import androidx.graphics.shapes.Morph
import com.bearbones.kumaflow.utils.MorphPolygonShape

@Composable
fun MorphingPinIndicator(
    isFilled: Boolean,
    targetShape: RoundedPolygon,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = if (isFilled) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pinMorph"
    )
    
    val morph = remember(targetShape) {
        Morph(RoundedPolygon.circle(), targetShape)
    }
    val currentShape = remember(progress) { MorphPolygonShape(morph, progress) }
    
    val color = if (isFilled) AppText() else AppSurfaceVariant()
    
    val scale by animateFloatAsState(
        targetValue = if (isFilled) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pinScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shape = currentShape
                clip = true
            }
            .background(color)
    )
}


// --- DATA CLASSES & OBJECTS ---
@Composable
fun LockScreen(correctPin: String, activity: FragmentActivity, onSuccess: () -> Unit) {
    var inputPin by remember { mutableStateOf("") }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        showBiometricPrompt(activity, onSuccess, {})
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBg())
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = AppText()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            AppStr.appLocked,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = AppText()
        )
        Spacer(modifier = Modifier.height(32.dp))
        val pinShapes = remember {
            listOf(
                RoundedPolygon(12, rounding = CornerRounding(radius = 0.5f)),                  // Circle-ish
                RoundedPolygon(4, rounding = CornerRounding(radius = 0.4f)),                   // Squircle
                RoundedPolygon(3, rounding = CornerRounding(radius = 0.3f)),                   // Rounded Triangle
                RoundedPolygon.star(4, innerRadius = 0.7f, rounding = CornerRounding(radius = 0.2f)),  // Soft Star
                RoundedPolygon(5, rounding = CornerRounding(radius = 0.3f)),                   // Pentagon
                RoundedPolygon(6, rounding = CornerRounding(radius = 0.2f))                    // Hexagon
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(6) { index ->
                MorphingPinIndicator(
                    isFilled = index < inputPin.length,
                    targetShape = pinShapes[index],
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(48.dp))

        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "Finger", "0", "Del")
        keys.chunked(3).forEachIndexed { rowIndex, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                row.forEachIndexed { colIndex, label ->
                    val index = rowIndex * 3 + colIndex
                    MorphingKeypadButton(
                        label = label,
                        onClick = {
                            when (label) {
                                "Del" -> if (inputPin.isNotEmpty()) inputPin = inputPin.dropLast(1)
                                "Finger" -> showBiometricPrompt(activity, onSuccess, {})
                                else -> {
                                    if (inputPin.length < 6) {
                                        inputPin += label
                                    }
                                    if (inputPin.length == 6) {
                                        if (inputPin == correctPin) {
                                            onSuccess()
                                        } else {
                                            inputPin = ""
                                            Toast.makeText(context, AppStr.wrongPin, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun MorphingKeypadButton(
    label: String,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    
    val width by animateDpAsState(
        targetValue = if (isPressed) 80.dp else 70.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "width"
    )
    val height by animateDpAsState(
        targetValue = if (isPressed) 60.dp else 70.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "height"
    )

    val isBrutal = com.bearbones.kumaflow.ui.theme.LocalIsBrutal.current
    val buttonShape = RoundedCornerShape(percent = 50)
    
    val fallbackColor = if (isBrutal) Color.White else AppSurface()
    val contentColor = if (isBrutal) Color.Black else AppText()
    
    val isLiquidGlass = com.bearbones.kumaflow.LocalIsLiquidGlass.current
    val isPremiumGlassBlur = com.bearbones.kumaflow.LocalIsPremiumGlassBlur.current
    val isDark = com.bearbones.kumaflow.LocalIsDark.current
    val hazeState = com.bearbones.kumaflow.LocalHazeState.current

    val glassColor = if (isDark) {
        Color(0xFF2C2C2E).copy(alpha = 0.60f)
    } else {
        Color(0xFFE8E8EC).copy(alpha = 0.85f)
    }

    val shineGradient = remember {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.15f),
                Color.Transparent,
                Color.Transparent,
                Color.White.copy(alpha = 0.05f)
            ),
            start = Offset.Zero,
            end = Offset.Infinite
        )
    }
    
    val borderGradient = remember {
        Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.05f))
        )
    }

    val baseModifier = if (isBrutal) {
        Modifier
            .drawBehind {
                drawRoundRect(
                    color = Color.Black,
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width / 2, size.height / 2),
                    topLeft = Offset(3.dp.toPx(), 4.dp.toPx())
                )
            }
            .clip(buttonShape)
            .background(Color.White)
            .border(2.dp, Color.Black, buttonShape)
    } else if (isLiquidGlass) {
        val tintAlpha = if (isPremiumGlassBlur) 0.45f else 0.35f
        val adjustedColor = glassColor.copy(alpha = (glassColor.alpha + tintAlpha).coerceAtMost(1f))
        Modifier
            .clip(buttonShape)
            .hazeChild(
                state = hazeState,
                shape = buttonShape,
                style = dev.chrisbanes.haze.HazeStyle(
                    blurRadius = if (isPremiumGlassBlur) 30.dp else 20.dp,
                    backgroundColor = Color.Transparent,
                    tint = dev.chrisbanes.haze.HazeTint(adjustedColor),
                    noiseFactor = if (isPremiumGlassBlur) 0.15f else 0.1f
                )
            )
            .background(shineGradient)
            .border(1.dp, borderGradient, buttonShape)
    } else {
        Modifier
            .clip(buttonShape)
            .background(fallbackColor)
            .border(1.dp, if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f), buttonShape)
    }

    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .then(baseModifier)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        val startTime = System.currentTimeMillis()
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                        
                        tryAwaitRelease()
                        
                        val duration = System.currentTimeMillis() - startTime
                        if (duration < 150) {
                            kotlinx.coroutines.delay(150 - duration)
                        }
                        isPressed = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        when (label) {
            "Finger" -> Icon(Icons.Default.Fingerprint, contentDescription = null, tint = contentColor)
            "Del" -> Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = null, tint = contentColor)
            else -> Text(label, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = contentColor)
        }
    }
}

class MainActivity : FragmentActivity() {
    var pendingRestoreJson: String? = null
    private lateinit var autoSyncManager: com.bearbones.kumaflow.duo.DuoAutoSyncManager

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkNfcIntent(intent)
    }

    private fun checkNfcIntent(intent: Intent?) {
        if (intent?.action == android.nfc.NfcAdapter.ACTION_TECH_DISCOVERED ||
            intent?.action == android.nfc.NfcAdapter.ACTION_TAG_DISCOVERED ||
            intent?.action == android.nfc.NfcAdapter.ACTION_NDEF_DISCOVERED) {
            com.bearbones.kumaflow.nfc.NfcTriggerManager.showNfcTrigger.tryEmit(Unit)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 12345 && resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                try {
                    contentResolver.openInputStream(uri)?.use { stream ->
                        pendingRestoreJson = stream.bufferedReader().readText()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Gagal baca file: ${e.message}", Toast.LENGTH_LONG).show()
                    pendingRestoreJson = null
                }
            } else {
                pendingRestoreJson = null
            }
        }
    }

    fun openSafeFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        try {
            startActivityForResult(intent, 12345)
        } catch (e: Exception) {
            Toast.makeText(this, AppStr.noFileMgr, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        val nfcAdapter = android.nfc.NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter != null) {
            val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                android.app.PendingIntent.FLAG_MUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                android.app.PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = android.app.PendingIntent.getActivity(this, 0, intent, flags)
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null)
        }
        autoSyncManager.start()
    }

    override fun onPause() {
        super.onPause()
        val nfcAdapter = android.nfc.NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter != null) {
            try {
                nfcAdapter.disableForegroundDispatch(this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        autoSyncManager.stop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkNfcIntent(intent)
        
        autoSyncManager = com.bearbones.kumaflow.duo.DuoAutoSyncManager(this, com.bearbones.kumaflow.KumaDatabase.getDatabase(this))
        autoSyncManager.onSyncEvent = { msg ->
            runOnUiThread {
                android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val display = windowManager.defaultDisplay
            val modes = display.supportedModes
            val bestMode = modes.maxByOrNull { it.refreshRate }
            bestMode?.let { mode ->
                val params = window.attributes
                params.preferredDisplayModeId = mode.modeId
                window.attributes = params
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    11
                )
            }
        }

        val serviceIntent = Intent(this, KumaService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        setContent {
            val context = LocalContext.current
            
            var updateInfo by remember { mutableStateOf<com.bearbones.kumaflow.utils.UpdateInfo?>(null) }
            var showUpdateDialog by remember { mutableStateOf(false) }
            var currentVersionName by remember { mutableStateOf("") }
            
            var downloadState by remember { mutableStateOf<com.bearbones.kumaflow.utils.DownloadState>(com.bearbones.kumaflow.utils.DownloadState.Idle) }
            val coroutineScope = rememberCoroutineScope()
            var downloadJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
            
            LaunchedEffect(Unit) {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                currentVersionName = packageInfo.versionName ?: ""
                
                val info = com.bearbones.kumaflow.utils.UpdateChecker.checkForUpdate()
                if (info != null) {
                    val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode
                    } else {
                        packageInfo.versionCode.toLong()
                    }
                    if (info.versionCode > currentVersionCode) {
                        val prefs = context.getSharedPreferences("kumaflow_prefs", android.content.Context.MODE_PRIVATE)
                        val lastSnooze = prefs.getLong("last_update_snooze", 0L)
                        val snoozedVersionCode = prefs.getInt("snoozed_version_code", 0)
                        val ignoredVersionCode = prefs.getInt("ignored_update_version_code", 0)
                        val currentTime = System.currentTimeMillis()
                        
                        if (info.versionCode > ignoredVersionCode) {
                            // Tampilkan jika versinya lebih baru dari yang di-snooze, ATAU sudah lewat 2 hari
                            if (info.versionCode > snoozedVersionCode || currentTime - lastSnooze >= 2 * 24 * 60 * 60 * 1000L) {
                                updateInfo = info
                                showUpdateDialog = true
                            }
                        }
                    }
                }
            }

            val db = remember { KumaDatabase.getDatabase(context) }
            val dao = db.transactionDao()
            val userProfile by dao.getUserProfile().collectAsState(initial = null)

            val sharedPrefs = remember { context.getSharedPreferences("kumaflow_prefs", android.content.Context.MODE_PRIVATE) }

            // ðŸ”¥ NEW STATE TO CAPTURE CURRENT MONTH & YEAR ðŸ”¥
            var wrappedTarget by remember { mutableStateOf<Pair<Int, Int>?>(null) }

            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            LaunchedEffect(userProfile?.userName, userProfile?.isLiquidGlass) {
                checkAndApplyPrideEasterEgg(context, userProfile, lifecycleOwner)
            }

            var isAuthenticated by rememberSaveable { mutableStateOf(false) }
            
            // Auto-lock after 3 minutes in background
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                        val prefs = context.getSharedPreferences("kumaflow_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putLong("last_background_time", System.currentTimeMillis()).apply()
                    } else if (event == androidx.lifecycle.Lifecycle.Event.ON_START) {
                        val prefs = context.getSharedPreferences("kumaflow_prefs", Context.MODE_PRIVATE)
                        val lastTime = prefs.getLong("last_background_time", 0L)
                        if (lastTime > 0L) {
                            val diff = System.currentTimeMillis() - lastTime
                            if (diff > 3 * 60 * 1000) { // 3 minutes
                                isAuthenticated = false
                            }
                            prefs.edit().putLong("last_background_time", 0L).apply()
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            val systemDark = isSystemInDarkTheme()
            val isAmoled = userProfile?.isAmoledMode == true

            val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
            val isJune = currentMonth == java.util.Calendar.JUNE
            val isPrideTriggered = userProfile?.userName?.contains("#pride", ignoreCase = true) == true
            val isBearTriggered = userProfile?.userName?.contains("#bear", ignoreCase = true) == true
            val isBrutalTriggered = userProfile?.userName?.contains("#brutal", ignoreCase = true) == true

            val rawThemeMode = userProfile?.themeMode ?: 0
            val activeThemeMode = when {
                rawThemeMode in 3..6 && isJune -> rawThemeMode
                rawThemeMode in 7..8 && isBrutalTriggered -> rawThemeMode
                rawThemeMode > 2 -> 0
                else -> rawThemeMode
            }

            val isOREasterEgg = userProfile?.userName?.contains("#OR", ignoreCase = true) == true

            val isDark = when {
                isOREasterEgg -> false // Pure Light Mode
                else -> when(activeThemeMode) {
                    1, 3, 5, 7 -> false
                    2, 4, 6, 8 -> true
                    else -> systemDark
                }
            }

            val hazeState = remember { dev.chrisbanes.haze.HazeState() }
            val tutorialState = remember { com.bearbones.kumaflow.ui.tutorial.TutorialState() }
            
            CompositionLocalProvider(
                LocalIsDark provides isDark,
                LocalIsAmoled provides isAmoled,
                LocalIsLiquidGlass provides (userProfile?.isLiquidGlass == true),
                LocalIsPremiumGlassBlur provides (userProfile?.isPremiumGlassBlur == true),
                LocalHazeState provides hazeState,
                com.bearbones.kumaflow.ui.theme.LocalIsBrutal provides (isBrutalTriggered && (activeThemeMode == 7 || activeThemeMode == 8)),
                com.bearbones.kumaflow.ui.tutorial.LocalTutorialState provides tutorialState
            ) {
                val colorScheme = when {
                    // NEW: #OR Easter Egg
                    isOREasterEgg -> lightColorScheme(
                        background = Color(0xFFFEE1F5), // Lavender blush
                        surface = Color.White, // Clean white surface for better contrast
                        primary = Color(0xFFEF71C3), // Hot pink
                        onPrimary = Color.White,
                        onBackground = Color(0xFF5E3F6B), // Darkened Lilac for readable text
                        onSurface = Color(0xFF5E3F6B)
                    )

                    // 1. Easter Egg Pride & Bear
                    isPrideTriggered && activeThemeMode == 3 -> lightColorScheme(background = Color(0xFFFCE4EC), surface = Color(0xFFF8BBD0), primary = Color(0xFFD81B60), onPrimary = Color.White, onBackground = Color(0xFF212121), onSurface = Color(0xFF212121))
                    isPrideTriggered && activeThemeMode == 4 -> darkColorScheme(background = Color(0xFF121212), surface = Color(0xFF263238), primary = Color(0xFFAA00FF), onPrimary = Color.White, onBackground = Color.White, onSurface = Color.White)
                    isBearTriggered && activeThemeMode == 5 -> lightColorScheme(background = Color(0xFFFFF3E0), surface = Color(0xFFFFE0B2), primary = Color(0xFFBF360C), onPrimary = Color.White, onBackground = Color(0xFF3E2723), onSurface = Color(0xFF3E2723))
                    isBearTriggered && activeThemeMode == 6 -> darkColorScheme(background = Color(0xFF3E2723), surface = Color(0xFF4E342E), primary = Color(0xFFFFCA28), onPrimary = Color.Black, onBackground = Color(0xFFEFEBE9), onSurface = Color(0xFFEFEBE9))
                    
                    // 2. Brutal
                    isBrutalTriggered && activeThemeMode == 7 -> lightColorScheme(background = com.bearbones.kumaflow.ui.theme.BrutalYellow, surface = com.bearbones.kumaflow.ui.theme.BrutalWhite, primary = com.bearbones.kumaflow.ui.theme.BrutalBlack, onPrimary = com.bearbones.kumaflow.ui.theme.BrutalWhite, onBackground = com.bearbones.kumaflow.ui.theme.BrutalBlack, onSurface = com.bearbones.kumaflow.ui.theme.BrutalBlack)
                    isBrutalTriggered && activeThemeMode == 8 -> darkColorScheme(background = com.bearbones.kumaflow.ui.theme.BrutalBlack, surface = com.bearbones.kumaflow.ui.theme.BrutalBlack, primary = com.bearbones.kumaflow.ui.theme.BrutalGreen, onPrimary = com.bearbones.kumaflow.ui.theme.BrutalBlack, onBackground = com.bearbones.kumaflow.ui.theme.BrutalWhite, onSurface = com.bearbones.kumaflow.ui.theme.BrutalWhite)

                    // ðŸ”¥ NEW LOGIC: DYNAMIC COLOR + AMOLED FUSION! ðŸ”¥
                    activeThemeMode == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                        // Extract the primary color palette from the device's wallpaper
                        val dynamicTheme = if (isDark) androidx.compose.material3.dynamicDarkColorScheme(context) else androidx.compose.material3.dynamicLightColorScheme(context)

                        // Enforce a pure black background if both Dark Mode and AMOLED Mode are enabled
                        if (isDark && isAmoled) {
                            dynamicTheme.copy(
                                background = Color(0xFF000000), // 100% Pure Black
                                surface = Color(0xFF121212) // Slight gray tint on surface to maintain visual hierarchy against the pure black background
                            )
                        } else {
                            dynamicTheme
                        }
                    }

                    // 3. Fallback Default
                    isDark -> if (isAmoled) darkColorScheme(background = Color(0xFF000000), surface = Color(0xFF121212), onBackground = Color(0xFFF5F7FA), onSurface = Color(0xFFF5F7FA), primary = Color(0xFF4F8CFF), onPrimary = Color.White, onSurfaceVariant = Color(0xFFA7B0BE), outline = Color(0xFF2C313C), error = Color(0xFFFF5A5F)) else darkColorScheme(background = Color(0xFF0F1115), surface = Color(0xFF171A21), onBackground = Color(0xFFF5F7FA), onSurface = Color(0xFFF5F7FA), primary = Color(0xFF4F8CFF), onPrimary = Color.White, onSurfaceVariant = Color(0xFFA7B0BE), outline = Color(0xFF2C313C), error = Color(0xFFFF5A5F))
                    else -> lightColorScheme(background = Color(0xFFF8F9FC), surface = Color(0xFFFFFFFF), onBackground = Color(0xFF1A1D24), onSurface = Color(0xFF1A1D24), primary = Color(0xFF2563EB), onPrimary = Color.White, onSurfaceVariant = Color(0xFF69707D), outline = Color(0xFFE3E7EF), error = Color(0xFFDC2626))
                }

                val activeTypography = when {
                    isOREasterEgg -> com.bearbones.kumaflow.ui.theme.ORTypography
                    isBrutalTriggered && (activeThemeMode == 7 || activeThemeMode == 8) -> com.bearbones.kumaflow.ui.theme.BrutalTypography
                    else -> com.bearbones.kumaflow.ui.theme.Typography
                }

                MaterialTheme(colorScheme = colorScheme, typography = activeTypography, shapes = MaterialTheme.shapes) {
                    val homeListState = androidx.compose.foundation.lazy.rememberLazyListState()
                    var isOverlayOpen by remember { mutableStateOf(false) }
                    val isWrappedOpen = wrappedTarget != null
                    val isAppLocked = userProfile?.isAppLocked == true && !isAuthenticated
                    
                    val isPaused = isOverlayOpen || isWrappedOpen || isAppLocked || homeListState.isScrollInProgress || tutorialState.currentStep != com.bearbones.kumaflow.ui.tutorial.TutorialStep.NONE
                    val scrollOffsetProvider = remember(homeListState) { { homeListState.firstVisibleItemScrollOffset.toFloat() } }

                    Box(modifier = Modifier.fillMaxSize().background(AppBg())) {
                        Box(modifier = Modifier.fillMaxSize().let { if (LocalIsLiquidGlass.current) it.haze(state = LocalHazeState.current) else it }) {
                            if (LocalIsLiquidGlass.current && !isOREasterEgg) {
                                com.bearbones.kumaflow.ui.components.BokehBackground(
                                    isPaused = isPaused,
                                    scrollOffsetProvider = scrollOffsetProvider
                                )
                            }
                        }

                        if (isAppLocked) {
                            LockScreen(userProfile?.appPin ?: "", this@MainActivity) {
                                isAuthenticated = true
                            }
                        } else {
                            Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                                Box(modifier = Modifier.fillMaxSize()) {

                                MainScreen(
                                    userProfileState = userProfile,
                                    dao = dao,
                                    onOpenWrapped = { m, y -> wrappedTarget = Pair(m, y) },
                                    homeListState = homeListState,
                                    onOverlayStateChange = { isOverlayOpen = it }
                                )

                                val currentProfile = userProfile ?: UserProfile(userName = "User")
                                var isNewUserDialogDismissed by remember { mutableStateOf(false) }
                                var txCount by remember { mutableIntStateOf(-1) }
                                
                                LaunchedEffect(Unit) {
                                    dao.getAllTransactionsWithSplits().collect { txs ->
                                        txCount = txs.size
                                    }
                                }

                                NewUserAnnouncementDialog(
                                    onDismissed = { isNewUserDialogDismissed = true }
                                )

                                var showTutorialFlow by remember { mutableStateOf(false) }

                                LaunchedEffect(currentProfile.hasSeenTutorial, txCount, isNewUserDialogDismissed, isAppLocked) {
                                    if (isNewUserDialogDismissed && !currentProfile.hasSeenTutorial && !isAppLocked && txCount == 0 && currentProfile.userName == "User" && !showTutorialFlow) {
                                        showTutorialFlow = true
                                        // Silently mark as seen in DB immediately so it doesn't show again if user force closes the app
                                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                            dao.saveProfile(currentProfile.copy(hasSeenTutorial = true))
                                        }
                                    }
                                }

                                if (showTutorialFlow) {
                                    LaunchedEffect(Unit) {
                                        tutorialState.advanceTo(com.bearbones.kumaflow.ui.tutorial.TutorialStep.HOME_SUMMARY)
                                    }
                                    com.bearbones.kumaflow.ui.tutorial.TutorialOverlay(
                                        onComplete = {
                                            showTutorialFlow = false
                                        }
                                    )
                                } else if (isNewUserDialogDismissed && !currentProfile.hasSeenTutorial && !isAppLocked && txCount >= 0) {
                                    // Existing user (has transactions or changed name), silently mark as seen
                                    LaunchedEffect(Unit) {
                                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                            dao.saveProfile(currentProfile.copy(hasSeenTutorial = true))
                                        }
                                    }
                                }

                                if (showUpdateDialog && updateInfo != null) {
                                    com.bearbones.kumaflow.ui.screens.UpdateDialog(
                                        updateInfo = updateInfo!!,
                                        currentVersionName = currentVersionName,
                                        downloadState = downloadState,
                                        onDismiss = { 
                                            showUpdateDialog = false 
                                        },
                                        onSnooze = {
                                            val prefs = context.getSharedPreferences("kumaflow_prefs", android.content.Context.MODE_PRIVATE)
                                            prefs.edit()
                                                .putLong("last_update_snooze", System.currentTimeMillis())
                                                .putInt("snoozed_version_code", updateInfo?.versionCode ?: 0)
                                                .apply()
                                            showUpdateDialog = false 
                                        },
                                        onUpdate = {
                                            val prefs = context.getSharedPreferences("kumaflow_prefs", android.content.Context.MODE_PRIVATE)
                                            prefs.edit().putInt("ignored_update_version_code", updateInfo?.versionCode ?: 0).apply()

                                            downloadJob = coroutineScope.launch {
                                                com.bearbones.kumaflow.utils.UpdateManager.downloadApk(context, updateInfo!!.apkUrl).collect { state ->
                                                    downloadState = state
                                                    if (state is com.bearbones.kumaflow.utils.DownloadState.Success) {
                                                        com.bearbones.kumaflow.utils.UpdateManager.installApk(context, state.file)
                                                    }
                                                }
                                            }
                                        },
                                        onCancelDownload = {
                                            downloadJob?.cancel()
                                            downloadState = com.bearbones.kumaflow.utils.DownloadState.Idle
                                        }
                                    )
                                }

                                // ðŸ”¥ DYNAMIC WRAPPED LOGIC ðŸ”¥
                                if (wrappedTarget != null && userProfile != null) {
                                    val targetMonth = wrappedTarget!!.first
                                    val targetYear = wrappedTarget!!.second
                                    val allTxs by dao.getAllTransactionsWithSplits().collectAsState(initial = emptyList())

                                    val cal = java.util.Calendar.getInstance()
                                    cal.set(java.util.Calendar.MONTH, targetMonth - 1)
                                    cal.set(java.util.Calendar.YEAR, targetYear)
                                    val targetMonthName = cal.getDisplayName(java.util.Calendar.MONTH, java.util.Calendar.LONG, java.util.Locale.forLanguageTag("id-ID")) ?: ""

                                    val targetMonthTxs = allTxs
                                        .map { it.transaction }
                                        .filter { tx ->
                                            try {
                                                val dt = java.time.LocalDateTime.parse(tx.timestamp, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                                dt.monthValue == targetMonth && dt.year == targetYear
                                            } catch (e: Exception) { false }
                                        }

                                    val closeWrapped = {
                                        wrappedTarget = null
                                        val todayCal = java.util.Calendar.getInstance()
                                        todayCal.add(java.util.Calendar.MONTH, -1)
                                        val pMonth = todayCal.get(java.util.Calendar.MONTH) + 1
                                        val pYear = todayCal.get(java.util.Calendar.YEAR)

                                        // Dismiss the banner exclusively if the navigated content belongs to the previous month
                                        if (targetMonth == pMonth && targetYear == pYear) {
                                            sharedPrefs.edit().putString("last_viewed_wrapped", "$pMonth-$pYear").apply()
                                        }
                                    }

                                    androidx.activity.compose.BackHandler(enabled = true) {
                                        closeWrapped()
                                    }

                                    WrappedScreen(
                                        profile = userProfile!!,
                                        prevMonthTransactions = targetMonthTxs,
                                        monthName = "$targetMonthName $targetYear",
                                        onClose = closeWrapped
                                    )
                                }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}




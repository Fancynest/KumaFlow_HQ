package com.bearbones.kumaflow
import androidx.compose.ui.draw.scale

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.bearbones.kumaflow.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.SimpleColorFilter
import com.airbnb.lottie.compose.*
import com.bearbones.kumaflow.AppBg
import com.bearbones.kumaflow.AppPrimary
import com.bearbones.kumaflow.AppText
import com.bearbones.kumaflow.LocalIsDark
import com.bearbones.kumaflow.VirtualWallet
import com.bearbones.kumaflow.ui.components.KumaButton
import com.bearbones.kumaflow.ui.theme.LocalIsBrutal

enum class WalletScreenRoute {
    MANAGE, SUCCESS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageWalletFullScreen(
    virtualWallets: List<VirtualWallet>,
    onSave: (oldName: String?, wallet: VirtualWallet) -> Unit,
    onDelete: (VirtualWallet) -> Unit,
    onDismiss: () -> Unit,
    userProfileName: String
) {
    var currentRoute by remember { mutableStateOf(WalletScreenRoute.MANAGE) }
    var successWallet by remember { mutableStateOf<VirtualWallet?>(null) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBg())
        ) {
            AnimatedContent(targetState = currentRoute, label = "WalletRoute") { route ->
                when (route) {
                    WalletScreenRoute.MANAGE -> {
                        ManageWalletContent(
                            userName = userProfileName,
                            wallets = virtualWallets,
                            onSave = { oldName, w -> 
                                onSave(oldName, w)
                                successWallet = w
                                currentRoute = WalletScreenRoute.SUCCESS
                            },
                            onDelete = onDelete,
                            onBack = onDismiss
                        )
                    }
                    WalletScreenRoute.SUCCESS -> {
                        WalletSuccessContent(
                            wallet = successWallet,
                            onDone = onDismiss,
                            userName = userProfileName
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ManageWalletContent(
    userName: String,
    wallets: List<VirtualWallet>,
    onSave: (String?, VirtualWallet) -> Unit,
    onDelete: (VirtualWallet) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    val pagerState = rememberPagerState(pageCount = { wallets.size + 1 })
    
    val solidColors = listOf("#2A2A2A", "#D32F2F", "#1976D2", "#388E3C", "#FBC02D", "#7B1FA2", "#111111", "#0288D1")
    val isAppPride = userName.contains("#pride", ignoreCase = true)
    val isAppBear = userName.contains("#bear", ignoreCase = true)
    val templateImages = remember(isAppPride, isAppBear) {
        val list = mutableListOf("minangkabau_card", "java_card", "papua_card", "bali_card", "bugis_card", "westkalimantan_card")
        if (isAppPride) list.add("pride")
        if (isAppBear) {
            list.add("bear")
            list.add("bear2")
        }
        list
    }
    var name by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var bgType by remember { mutableStateOf("SOLID") }
    var bgValue by remember { mutableStateOf("#D32F2F") }
    
    val isNewWallet = pagerState.currentPage == wallets.size
    val currentWallet = if (isNewWallet) null else wallets.getOrNull(pagerState.currentPage)

    var editPageIndex by remember { mutableIntStateOf(pagerState.currentPage) }
    
    LaunchedEffect(pagerState, wallets) {
        androidx.compose.runtime.snapshotFlow { pagerState.isScrollInProgress }.collect { isScrolling ->
            if (!isScrolling) {
                val page = pagerState.currentPage
                editPageIndex = page
                val isNew = page == wallets.size
                val w = if (isNew) null else wallets.getOrNull(page)
                if (isNew) {
                    name = ""
                    cardNumber = ""
                    notes = ""
                    bgType = "SOLID"
                    bgValue = "#D32F2F"
                } else {
                    w?.let {
                        name = it.name
                        cardNumber = it.cardNumber
                        notes = it.notes
                        bgType = it.backgroundType
                        bgValue = it.backgroundValue
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppText())
            }
            Text(
                text = if (isNewWallet) stringResource(R.string.manage_wallet_title_add) else stringResource(R.string.manage_wallet_title_edit),
                color = AppText(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            contentPadding = PaddingValues(horizontal = 32.dp),
            pageSpacing = 16.dp
        ) { page ->
            val isActive = page == editPageIndex
            val previewType = if (isActive) bgType else if (page == wallets.size) "SOLID" else wallets[page].backgroundType
            val previewVal = if (isActive) bgValue else if (page == wallets.size) "#D32F2F" else wallets[page].backgroundValue
            val previewName = if (isActive) name else if (page == wallets.size) "New Wallet" else wallets[page].name
            val previewNumber = if (isActive) cardNumber else if (page == wallets.size) "" else wallets[page].cardNumber

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (previewType == "SOLID") {
                            try { Color(android.graphics.Color.parseColor(previewVal)) } catch (e: Exception) { Color.DarkGray }
                        } else Color.Gray
                    )
            ) {
                if (previewType == "TEMPLATE") {
                    val isPrideReq = previewVal == "pride"
                    val isBearReq = previewVal == "bear" || previewVal == "bear2"
                    val isPrideAllowed = userName.contains("#pride", ignoreCase = true)
                    val isBearAllowed = userName.contains("#bear", ignoreCase = true)
                    
                    val shouldRender = when {
                        isPrideReq -> isPrideAllowed
                        isBearReq -> isBearAllowed
                        else -> true
                    }
                    
                    if (shouldRender) {
                        val resId = context.resources.getIdentifier(previewVal, "drawable", context.packageName)
                        if (resId != 0) {
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                            )
                        }
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
                        Text(previewName.ifBlank { "Wallet Name" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    if (previewNumber.isNotBlank()) {
                        Text(previewNumber, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp)
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            val baseFieldMod = Modifier
                .fillMaxWidth()
                .glassCard(16.dp, com.bearbones.kumaflow.AppSurfaceVariant())
                .padding(16.dp)

            Box(modifier = baseFieldMod) {
                Column {
                    Text(stringResource(R.string.manage_wallet_name), color = AppText().copy(alpha = 0.7f), fontSize = 14.sp)
                    androidx.compose.foundation.text.BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        textStyle = androidx.compose.ui.text.TextStyle(color = AppText(), fontSize = 16.sp),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp)
                    )
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.Gray.copy(alpha = 0.3f)))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(stringResource(R.string.manage_wallet_card_number), color = AppText().copy(alpha = 0.7f), fontSize = 14.sp)
                    androidx.compose.foundation.text.BasicTextField(
                        value = cardNumber,
                        onValueChange = { cardNumber = it },
                        textStyle = androidx.compose.ui.text.TextStyle(color = AppText(), fontSize = 16.sp),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp)
                    )
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.Gray.copy(alpha = 0.3f)))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(stringResource(R.string.manage_wallet_notes), color = AppText().copy(alpha = 0.7f), fontSize = 14.sp)
                    androidx.compose.foundation.text.BasicTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        textStyle = androidx.compose.ui.text.TextStyle(color = AppText(), fontSize = 16.sp),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(stringResource(R.string.manage_wallet_appearance), color = AppText(), fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(solidColors) { colorHex ->
                    val isSelected = bgType == "SOLID" && bgValue == colorHex
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(colorHex)))
                            .border(if (isSelected) 3.dp else 0.dp, if (isSelected) AppPrimary() else Color.Transparent, CircleShape)
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
            
            Spacer(modifier = Modifier.height(12.dp))
            
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
                            .border(if (isSelected) 3.dp else 0.dp, if (isSelected) AppPrimary() else Color.Transparent, RoundedCornerShape(8.dp))
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
                        }
                        if (isSelected) {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
                            Icon(Icons.Default.Check, null, tint = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Box(modifier = baseFieldMod) {
                Column {
                    Text(stringResource(R.string.manage_wallet_important_note), color = AppText(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.manage_wallet_important_note_desc), color = AppText().copy(alpha = 0.7f), fontSize = 14.sp, lineHeight = 20.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (!isNewWallet && currentWallet != null) {
                    KumaButton(
                        onClick = { onDelete(currentWallet) }, 
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        modifier = Modifier.weight(0.4f)
                    ) {
                        Icon(Icons.Default.Delete, null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
                
                KumaButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSave(
                                currentWallet?.name,
                                VirtualWallet(
                                    name = name.trim(),
                                    orderIndex = currentWallet?.orderIndex ?: wallets.size,
                                    backgroundType = bgType,
                                    backgroundValue = bgValue,
                                    cardNumber = cardNumber.trim(),
                                    notes = notes.trim()
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isNewWallet) stringResource(R.string.manage_wallet_btn_add) else stringResource(R.string.manage_wallet_btn_save), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(48.dp).navigationBarsPadding())
        }
    }
}

@Composable
fun WalletSuccessContent(
    wallet: VirtualWallet?,
    onDone: () -> Unit,
    userName: String
) {
    val context = LocalContext.current
    val isDark = LocalIsDark.current
    val isBrutal = LocalIsBrutal.current
    
    val isPride = userName.contains("#pride", ignoreCase = true)
    val isBear = userName.contains("#bear", ignoreCase = true)
    val isOR = userName.contains("#OR", ignoreCase = true)

    val tickColor = when {
        isOR -> Color(0xFFC2185B)
        isBrutal -> Color(0xFF000000)
        isPride -> Color(0xFF9C27B0)
        isBear -> Color(0xFF795548)
        isDark -> Color(0xFF4FC3F7)
        else -> Color(0xFF4CAF50)
    }

    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR_FILTER,
            value = SimpleColorFilter(tickColor.toArgb()),
            keyPath = arrayOf("**")
        )
    )

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(com.bearbones.kumaflow.R.raw.tick))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
        speed = 0.7f
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))
        
        Box(modifier = Modifier.size(150.dp)) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                dynamicProperties = dynamicProperties,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.manage_wallet_added), color = AppText(), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))

        if (wallet != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (wallet.backgroundType == "SOLID") {
                            try { Color(android.graphics.Color.parseColor(wallet.backgroundValue)) } catch (e: Exception) { Color.DarkGray }
                        } else Color.Gray
                    )
            ) {
                if (wallet.backgroundType == "TEMPLATE") {
                    val isPrideReq = wallet.backgroundValue == "pride"
                    val isBearReq = wallet.backgroundValue == "bear" || wallet.backgroundValue == "bear2"
                    val isPrideAllowed = userName.contains("#pride", ignoreCase = true)
                    val isBearAllowed = userName.contains("#bear", ignoreCase = true)
                    
                    val shouldRender = when {
                        isPrideReq -> isPrideAllowed
                        isBearReq -> isBearAllowed
                        else -> true
                    }
                    
                    if (shouldRender) {
                        val resId = context.resources.getIdentifier(wallet.backgroundValue, "drawable", context.packageName)
                        if (resId != 0) {
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                            )
                        }
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
                        Text(wallet.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    if (wallet.cardNumber.isNotBlank()) {
                        Text(wallet.cardNumber, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1.5f))

        KumaButton(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.manage_wallet_done), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp).navigationBarsPadding())
    }
}

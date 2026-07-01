package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.db.HistoryItem
import com.example.data.db.VoiceProfile
import com.example.data.repository.SubscriptionType
import com.example.ui.viewmodel.VoiceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin

// Theme Custom Colors (Geometric Balance Light Theme)
val DarkBackground = Color(0xFFFEF7FF) // Main light purple-grayish background
val SurfaceCard = Color(0xFFFFFFFF) // bg-white cards/containers
val GoldAccent = Color(0xFF6750A4) // Main Primary purple
val GreenAccent = Color(0xFF136B32) // High contrast M3 green for active/success states
val RedAccent = Color(0xFFBA1A1A) // High contrast M3 red for recording/alert states
val TextPrimary = Color(0xFF1D1B20) // Primary charcoal text
val TextSecondary = Color(0xFF49454F) // Secondary grey-charcoal text
val BorderColor = Color(0xFFCAC4D0) // Light gray-purple border outline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: VoiceViewModel) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsState()
    val currentSub by viewModel.currentSubscription.collectAsState()
    val todayUsage by viewModel.todayUsageCount.collectAsState()
    val showUpgradeDialog by viewModel.showUpgradeDialog.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    // Error Alert Handling
    LaunchedColorSnackbar(errorMessage, successMessage, viewModel, context)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_screen"),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(end = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Custom Vector Kurdish Sun Icon
                            KurdishSunLogo()
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "دەنگی کوردی",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "KURDISH AI SUITE",
                                    fontSize = 9.sp,
                                    color = GoldAccent,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        // Premium/Subscription Badge
                        SubscriptionBadge(
                            subscriptionType = currentSub,
                            onUpgradeClick = { viewModel.setShowUpgradeDialog(true) }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Limits progress meter for free users
            if (currentSub == SubscriptionType.FREE) {
                FreeLimitsBanner(
                    used = todayUsage,
                    limit = currentSub.dailyLimit,
                    onUpgradeClick = { viewModel.setShowUpgradeDialog(true) }
                )
            }

            // Central Navigation Tabs (Geometric Balance Pill-shaped Tab Selector)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(Color(0xFFEADDFF), RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tabs = listOf(
                    Triple("TTS", "دەق بۆ دەنگ", Icons.Default.PlayArrow),
                    Triple("STT", "دەنگ بۆ دەق", Icons.Default.Mic),
                    Triple("CLONE", "کۆپیکردن", Icons.Default.Add),
                    Triple("HISTORY", "مێژوو", Icons.Default.History)
                )

                tabs.forEach { (tabKey, tabLabel, icon) ->
                    val isSelected = currentTab == tabKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color.White else Color.Transparent)
                            .clickable { viewModel.setTab(tabKey) }
                            .padding(vertical = 8.dp)
                            .testTag("tab_$tabKey"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = icon,
                                contentDescription = tabLabel,
                                tint = if (isSelected) Color(0xFF21005D) else Color(0xFF49454F),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tabLabel,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF21005D) else Color(0xFF49454F),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Main Display Screen according to active tab
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(16.dp)
            ) {
                when (currentTab) {
                    "TTS" -> TtsScreenLayout(viewModel)
                    "STT" -> SttScreenLayout(viewModel)
                    "CLONE" -> VoiceCloningScreenLayout(viewModel)
                    "HISTORY" -> HistoryScreenLayout(viewModel)
                }
            }
        }
    }

    // Subscription Upgrade Dialog (Bottom Sheet Style)
    if (showUpgradeDialog) {
        UpgradeDialog(
            currentSub = currentSub,
            onDismiss = { viewModel.setShowUpgradeDialog(false) },
            onSelectPlan = { plan -> viewModel.selectSubscription(plan) }
        )
    }
}

// --- Dynamic Sub-Views ---

@Composable
fun TtsScreenLayout(viewModel: VoiceViewModel) {
    val inputText by viewModel.inputText.collectAsState()
    val isSynthesizing by viewModel.isSynthesizing.collectAsState()
    val voiceProfiles by viewModel.voiceProfiles.collectAsState()
    val selectedVoice by viewModel.selectedVoice.collectAsState()
    val subType by viewModel.currentSubscription.collectAsState()

    var dropdownExpanded by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Word calculations
    val wordCount = remember(inputText) {
        if (inputText.trim().isEmpty()) 0 else inputText.trim().split(Regex("\\s+")).size
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "کۆپیکردنی دەق بۆ دەنگی کوردی",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "دەقەکەت بنووسە بە پیتی کوردی بۆ بیستنی دەنگەکە بە لەرەلەری کالیبرەکراو.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
        }

        // Selected Voice Picker
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "دەنگی هەڵبژێردراو",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent,
                        modifier = Modifier.align(Alignment.End)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBackground)
                            .clickable { dropdownExpanded = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag("voice_picker_dropdown")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown",
                                tint = TextSecondary
                            )
                            Text(
                                text = selectedVoice?.name ?: "دەنگی سۆرانی سیستەم",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier
                                .background(SurfaceCard)
                                .fillMaxWidth(0.85f)
                        ) {
                            DropdownMenuItem(
                                text = { Text("دەنگی سۆرانی سیستەم", color = TextPrimary, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                                onClick = {
                                    viewModel.selectVoice(null)
                                    dropdownExpanded = false
                                }
                            )
                            voiceProfiles.forEach { profile ->
                                DropdownMenuItem(
                                    text = { 
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "(${profile.gender})",
                                                color = GoldAccent,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = profile.name,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectVoice(profile)
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Text input area
        item {
            OutlinedTextField(
                value = inputText,
                onValueChange = { viewModel.updateInputText(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp)
                    .testTag("tts_text_input"),
                placeholder = { 
                    Text(
                        "دەق لێرە بنووسە...", 
                        color = TextSecondary, 
                        textAlign = TextAlign.Right, 
                        modifier = Modifier.fillMaxWidth()
                    ) 
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = TextPrimary,
                    textDirection = TextDirection.Rtl,
                    fontSize = 15.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldAccent,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = SurfaceCard,
                    unfocusedContainerColor = SurfaceCard
                ),
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
            )
            
            // Character & Word Counter with relative Limits
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "پلانی ${subType.displayName} (ماکسیمم ${subType.maxWords} وشە)",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = "$wordCount / ${subType.maxWords} وشە",
                    color = if (wordCount > subType.maxWords) RedAccent else GoldAccent,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
            }
        }

        // Voice controls panel (Pitch, Tempo)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ڕێکخستنەکانی تۆنی دەنگ",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier.align(Alignment.End)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Pitch
                    val activePitch = selectedVoice?.pitch ?: 1.0f
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "%.2fx".format(activePitch), color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = "لەرەلەر (Pitch)", color = TextPrimary, fontSize = 13.sp)
                    }
                    Slider(
                        value = activePitch,
                        onValueChange = { /* Dynamic sliders overridden when voice is selected */ },
                        valueRange = 0.5f..2.0f,
                        enabled = selectedVoice == null, // Default can slide
                        colors = SliderDefaults.colors(
                            thumbColor = GoldAccent,
                            activeTrackColor = GoldAccent,
                            inactiveTrackColor = DarkBackground
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Speed/Tempo
                    val activeSpeed = selectedVoice?.speed ?: 1.0f
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "%.2fx".format(activeSpeed), color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = "خێرایی (Speed)", color = TextPrimary, fontSize = 13.sp)
                    }
                    Slider(
                        value = activeSpeed,
                        onValueChange = { },
                        valueRange = 0.5f..2.0f,
                        enabled = selectedVoice == null,
                        colors = SliderDefaults.colors(
                            thumbColor = GoldAccent,
                            activeTrackColor = GoldAccent,
                            inactiveTrackColor = DarkBackground
                        )
                    )
                }
            }
        }

        // Soundwave playing visualizer (Animated Canvas)
        if (isSynthesizing) {
            item {
                AnimatedSoundwaveCanvas()
            }
        }

        // Action Button
        item {
            Button(
                onClick = {
                    if (isSynthesizing) {
                        viewModel.stopSpeaking()
                    } else {
                        viewModel.speakText()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("tts_action_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSynthesizing) RedAccent else GreenAccent
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isSynthesizing) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Stop", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("وەستاندن و داخستنی دەنگ", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    } else {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("دەستپێکردنی گۆڕین بە دەنگ", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun SttScreenLayout(viewModel: VoiceViewModel) {
    val isRecording by viewModel.isRecording.collectAsState()
    val isTranscribing by viewModel.isTranscribing.collectAsState()
    val transcribedText by viewModel.transcribedText.collectAsState()
    val amplitude by viewModel.amplitude.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "گۆڕینی دەنگی کوردی بۆ دەق",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "تۆماری دەنگی سۆرانی بکە و بە شێوازێکی زۆر ورد لە ڕێگەی Gemini بیگۆڕە بۆ نوسین.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
        }

        // Big visual recording center
        item {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(vertical = 24.dp)
                    .size(180.dp)
            ) {
                // Outer breathing circles
                val infiniteTransition = rememberInfiniteTransition()
                val scaleFactor by if (isRecording) {
                    infiniteTransition.animateFloat(
                        initialValue = 1.0f,
                        targetValue = 1.35f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                } else {
                    remember { mutableStateOf(1.0f) }
                }

                Box(
                    modifier = Modifier
                        .scale(scaleFactor)
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(
                            if (isRecording) RedAccent.copy(alpha = 0.15f) 
                            else GreenAccent.copy(alpha = 0.1f)
                        )
                )

                Box(
                    modifier = Modifier
                        .scale(scaleFactor * 0.85f)
                        .size(115.dp)
                        .clip(CircleShape)
                        .background(
                            if (isRecording) RedAccent.copy(alpha = 0.3f) 
                            else GreenAccent.copy(alpha = 0.2f)
                        )
                )

                // Actual Button
                IconButton(
                    onClick = {
                        if (isRecording) {
                            viewModel.stopSpeechRecording()
                        } else {
                            viewModel.startSpeechRecording()
                        }
                    },
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(if (isRecording) RedAccent else GreenAccent)
                        .testTag("stt_record_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Mic",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // Recording status label
        item {
            Text(
                text = if (isRecording) "خەریکی تۆمارکردنی دەنگە... بۆ کۆتایی دابگرە" else "پەنجە دابگرە بۆ دەستپێکردنی تۆمارکردن",
                color = if (isRecording) RedAccent else TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }

        // Live Mic Amplitude waveform
        if (isRecording) {
            item {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(40.dp).fillMaxWidth()
                ) {
                    val bars = 8
                    // Map amplitude roughly (0-32767) to height pixels
                    val scaledAmp = (amplitude / 3000).coerceIn(1, 15)
                    for (i in 0 until bars) {
                        val heightMultiplier = if (i % 2 == 0) scaledAmp else (scaledAmp / 2).coerceAtLeast(1)
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .width(6.dp)
                                .height((10 + heightMultiplier * 4).dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(RedAccent)
                        )
                    }
                }
            }
        }

        // Processing indicators
        if (isTranscribing) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    CircularProgressIndicator(color = GoldAccent)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "کار لەسەر لێکدانەوەی لەرەلەرەکانی دەنگەکە دەکرێت لەلایەن Gemini...",
                        color = GoldAccent,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Transcribed Output Area
        if (transcribedText.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Kurdish Transcription", transcribedText)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "دەقەکە کۆپی کرا!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = "Copy", tint = TextSecondary)
                                }
                                IconButton(
                                    onClick = {
                                        val sendIntent: Intent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, transcribedText)
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, "ناردنی نوسین بە")
                                        context.startActivity(shareIntent)
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Share", tint = TextSecondary)
                                }
                            }
                            Text(
                                text = "دەقی دەرهێنراو (نوسین)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenAccent
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = transcribedText,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 26.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceCloningScreenLayout(viewModel: VoiceViewModel) {
    val cloningName by viewModel.cloningName.collectAsState()
    val isCloningRecording by viewModel.isCloningRecording.collectAsState()
    val analyzedMetrics by viewModel.analyzedMetrics.collectAsState()
    val voiceProfiles by viewModel.voiceProfiles.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "کۆپیکردنی دەنگی نوێ (Voice Cloning)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "پلانی Pro ڕێگەت پێدەدات دەنگی ڕاستەقینەی خۆت کۆپی بکەیت بە چەند هەنگاوێکی ئاسان.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
        }

        // Setup wizard card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "هەنگاوی یەکەم: ناونانی دەنگەکە",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent,
                        modifier = Modifier.align(Alignment.End)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = cloningName,
                        onValueChange = { viewModel.updateCloningName(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cloning_name_input"),
                        placeholder = { 
                            Text(
                                "ناونیشان بۆ نموونە: دەنگی خۆم...", 
                                color = TextSecondary, 
                                textAlign = TextAlign.Right, 
                                modifier = Modifier.fillMaxWidth()
                            ) 
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = TextPrimary,
                            textDirection = TextDirection.Rtl
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground
                        ),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "هەنگاوی دووەم: خوێندنەوەی دەقی کالیبرەکردن",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent,
                        modifier = Modifier.align(Alignment.End)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "تکایە ئەم ڕستەیەی خوارەوە بە ڕوونی بخوێنەرەوە کاتێک دوگمەی تۆمارکردن دەکەیتەوە:",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Training script in Kurdish Sorani
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBackground)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "« من شانازی بە زمان و کلتوری خۆمەوە دەکەم، و دەمەوێت دەنگی ڕاستەقینەی خۆم لێرەدا کۆپی بکەم تاوەکو بە تەواوی لێرەلەری گونجاو نیشان بدات »",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "هەنگاوی سێیەم: تۆمارکردن",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent,
                        modifier = Modifier.align(Alignment.End)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Record Button for cloning
                    Button(
                        onClick = {
                            if (isCloningRecording) {
                                viewModel.stopCloningRecording()
                            } else {
                                viewModel.startCloningRecording()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCloningRecording) RedAccent else GoldAccent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("cloning_record_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isCloningRecording) Icons.Default.Clear else Icons.Default.Mic,
                                contentDescription = "Mic",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isCloningRecording) "تەواوکردنی تۆمارکردنی کالیبرە" else "دەستپێکردنی تۆماری کالیبرە",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Voice analysis results if ready
        analyzedMetrics?.let { metrics ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, GoldAccent)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ئەنجامی شیکاری شەپۆلی دەنگەکە",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenAccent,
                            modifier = Modifier.align(Alignment.End)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Stats Grid
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("خێرایی", color = TextSecondary, fontSize = 11.sp)
                                Text("%.2fx".format(metrics.speedFactor), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("لەرەلەر", color = TextSecondary, fontSize = 11.sp)
                                Text("${metrics.pitchHz} Hz", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("دەنگدانەوە", color = TextSecondary, fontSize = 11.sp)
                                Text("%.2f".format(metrics.resonance), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("جۆری دەنگ", color = TextSecondary, fontSize = 11.sp)
                                Text(metrics.category.split(" ").firstOrNull() ?: "", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = metrics.description,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Save action
                        Button(
                            onClick = { viewModel.saveClonedVoice() },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("cloning_save_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("پاشەکەوتکردن و دروستکردنی دەنگەکە", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Active profiles header
        if (voiceProfiles.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "دەنگە کۆپیکراوەکانی تۆ",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Existing profiles
            items(voiceProfiles) { profile ->
                VoiceProfileRow(profile = profile, onDelete = { viewModel.deleteVoiceProfile(profile) })
            }
        }
    }
}

@Composable
fun VoiceProfileRow(profile: VoiceProfile, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = RedAccent)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = profile.name,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${profile.gender} • لەرەلەر: ${"%.2fx".format(profile.pitch)}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                
                // Micro Profile avatar
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(GoldAccent.copy(alpha = 0.15f))
                ) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = "Profile", tint = GoldAccent, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun HistoryScreenLayout(viewModel: VoiceViewModel) {
    val historyList by viewModel.historyList.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            if (historyList.isNotEmpty()) {
                TextButton(
                    onClick = { viewModel.clearAllHistory() },
                    modifier = Modifier.testTag("clear_history_button")
                ) {
                    Text("سڕینەوەی هەموو", color = RedAccent, fontSize = 13.sp)
                }
            } else {
                Spacer(modifier = Modifier.width(10.dp))
            }
            
            Text(
                text = "مێژووی کردارەکان",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        if (historyList.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize().weight(1f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Empty",
                        tint = TextSecondary,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "هیچ کردارێک ئەنجام نەدراوە هێشتا",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize().weight(1f)
            ) {
                items(historyList) { item ->
                    HistoryCard(item = item, onDelete = { viewModel.deleteHistoryItem(item) }, context = context)
                }
            }
        }
    }
}

@Composable
fun HistoryCard(item: HistoryItem, onDelete: () -> Unit, context: Context) {
    val dateString = remember(item.timestamp) {
        val formatter = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        formatter.format(Date(item.timestamp))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row {
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = RedAccent, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Kurdish Voice output", item.outputText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "کۆپی کرا!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = dateString, color = TextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Conversion type label
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (item.type == "TTS") GreenAccent.copy(alpha = 0.15f) else GoldAccent.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (item.type == "TTS") "دەق بۆ دەنگ" else "دەنگ بۆ دەق",
                            color = if (item.type == "TTS") GreenAccent else GoldAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            // Texts
            Text(
                text = "دەق: ${item.inputText}",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ئەنجام: ${item.outputText}",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "دەنگی کارپێکراو: ${item.voiceName}",
                color = GoldAccent,
                fontSize = 11.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// --- Auxiliary Components ---

@Composable
fun KurdishSunLogo() {
    Canvas(modifier = Modifier.size(28.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 3

        // Draw sun core (Gold)
        drawCircle(
            color = GoldAccent,
            radius = radius,
            center = center
        )

        // Draw rays of Kurdish sun (21 rays typically)
        val numRays = 12
        for (i in 0 until numRays) {
            val angle = (i * (360f / numRays)) * (Math.PI / 180f)
            val rayLength = radius * 1.6f
            val startX = center.x + (radius * 1.1f * sin(angle)).toFloat()
            val startY = center.y + (radius * 1.1f * kotlin.math.cos(angle)).toFloat()
            val endX = center.x + (rayLength * sin(angle)).toFloat()
            val endY = center.y + (rayLength * kotlin.math.cos(angle)).toFloat()

            drawLine(
                color = GoldAccent,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun SubscriptionBadge(subscriptionType: SubscriptionType, onUpgradeClick: () -> Unit) {
    val containerColor = when (subscriptionType) {
        SubscriptionType.FREE -> SurfaceCard
        SubscriptionType.PREMIUM -> GreenAccent.copy(alpha = 0.2f)
        SubscriptionType.PRO -> GoldAccent.copy(alpha = 0.2f)
    }

    val textColor = when (subscriptionType) {
        SubscriptionType.FREE -> TextSecondary
        SubscriptionType.PREMIUM -> GreenAccent
        SubscriptionType.PRO -> GoldAccent
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .border(1.dp, textColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .clickable { onUpgradeClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .testTag("subscription_badge")
    ) {
        if (subscriptionType == SubscriptionType.FREE) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Upgrade", tint = GoldAccent, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
        } else {
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Active", tint = textColor, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = subscriptionType.displayName,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun FreeLimitsBanner(used: Int, limit: Int, onUpgradeClick: () -> Unit) {
    val progress = (used.toFloat() / limit.toFloat()).coerceIn(0f, 1f)
    val color by animateColorAsState(if (used >= limit) RedAccent else GoldAccent)

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = onUpgradeClick,
                    modifier = Modifier.testTag("upgrade_link")
                ) {
                    Text("باشترکردن (Upgrade) ✦", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                
                Text(
                    text = "سنووری بەکارهێنانی بێ بەرامبەر: $used لە $limit",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = DarkBackground,
            )
        }
    }
}

@Composable
fun AnimatedSoundwaveCanvas() {
    val infiniteTransition = rememberInfiniteTransition()
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceCard)
            .padding(vertical = 4.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2

        // Draw multiple overlapping sine waves to represent complex voice synthesizer
        val waveColor1 = GoldAccent.copy(alpha = 0.85f)
        val waveColor2 = GreenAccent.copy(alpha = 0.5f)

        for (x in 0 until width.toInt() step 4) {
            val relativeX = x.toFloat() / width
            
            // First Sine Wave (Gold)
            val yOffset1 = sin(relativeX * 3 * Math.PI + phase) * (height / 2.8f) * sin(relativeX * Math.PI)
            drawCircle(
                color = waveColor1,
                radius = 2.5f,
                center = Offset(x.toFloat(), (centerY + yOffset1).toFloat())
            )

            // Second Wave (Emerald)
            val yOffset2 = sin(relativeX * 5 * Math.PI - phase * 1.2f) * (height / 4f) * sin(relativeX * Math.PI)
            drawCircle(
                color = waveColor2,
                radius = 2f,
                center = Offset(x.toFloat(), (centerY + yOffset2).toFloat())
            )
        }
    }
}

@Composable
fun UpgradeDialog(
    currentSub: SubscriptionType,
    onDismiss: () -> Unit,
    onSelectPlan: (SubscriptionType) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceCard,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "پلانی گۆڕینی بەکارهێنان",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "پلانی گونجاو بۆ کارەکانت هەڵبژێرە",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                val plans = listOf(
                    Triple(SubscriptionType.FREE, "$0", listOf("٤ گۆڕین بۆ دەنگ لە ڕۆژێکدا", "ماکسیمم ٥٠ وشە بۆ هەر جارێک", "دەنگی سۆرانی بنەڕەتی")),
                    Triple(SubscriptionType.PREMIUM, "$9.99/mo", listOf("بەکارهێنانی بێسنوور", "٥٠٠ وشە لە هەر داواکارییەکدا", "١ دەنگی کۆپیکراوی تایبەت", "سرعەتی سێرڤەری بەرز")),
                    Triple(SubscriptionType.PRO, "$24.99/mo", listOf("بەکارهێنانی تەواو بێسنوور", "١٠٠٠ وشە لە هەر داواکارییەکدا", "کۆپی تا ١٠ دەنگی جیاواز (Cloning)", "کالیبرەکردنی لەرەلەر و تەواوی پارامێتەرەکان"))
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.heightIn(max = 380.dp)
                ) {
                    items(plans) { (planType, price, features) ->
                        val isSelected = currentSub == planType
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) GoldAccent.copy(alpha = 0.1f) else DarkBackground
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) GoldAccent else BorderColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectPlan(planType) },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = price,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldAccent,
                                        fontSize = 14.sp
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = planType.displayName,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontSize = 14.sp
                                        )
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = GreenAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                features.forEach { feature ->
                                    Row(
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = feature,
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Right
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(GoldAccent)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("داخستن")
                }
            }
        }
    }
}

@Composable
fun LaunchedColorSnackbar(
    errorMessage: String?,
    successMessage: String?,
    viewModel: VoiceViewModel,
    context: Context
) {
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearErrorMessage()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearSuccessMessage()
        }
    }
}

// Helpers
private fun getTabIndex(tab: String): Int {
    return when (tab) {
        "TTS" -> 0
        "STT" -> 1
        "CLONE" -> 2
        "HISTORY" -> 3
        else -> 0
    }
}

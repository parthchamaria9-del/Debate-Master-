package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.DebateBattle
import com.example.data.SpeechPractice
import com.example.data.UserProfile
import com.example.viewmodel.DebateViewModel
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DebateAppUI(viewModel: DebateViewModel) {
    val context = LocalContext.current
    val currentScreen = viewModel.currentScreen
    val currentLanguage = viewModel.currentLanguage

    // Sync localization lang state
    Localization.currentLanguage = currentLanguage

    // Speech recognition controller
    var isRecordingByUser by remember { mutableStateOf(false) }
    var recordingTimerVal by remember { mutableStateOf(0) }
    var transcriptionBuffer by remember { mutableStateOf("") }
    
    // Waveform simulation
    var rmsValue by remember { mutableStateOf(0f) }

    val speechRecognizerHelper = remember {
        SpeechRecognizerHelper(
            context = context,
            onResult = { result ->
                isRecordingByUser = false
                rmsValue = 0f
                if (viewModel.currentScreen == "coach") {
                    viewModel.userSpeechInput = (viewModel.userSpeechInput + " " + result).trim()
                } else if (viewModel.currentScreen == "battle") {
                    viewModel.submitUserArgument(result)
                }
                Toast.makeText(context, "Speech Recognized successfully!", Toast.LENGTH_SHORT).show()
            },
            onError = { errormsg ->
                isRecordingByUser = false
                rmsValue = 0f
                Toast.makeText(context, errormsg, Toast.LENGTH_LONG).show()
            },
            onRmsChanged = { rms ->
                rmsValue = rms
            }
        )
    }

    // Mic Recording Permission Request
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isRecordingByUser = true
            speechRecognizerHelper.startListening(languageCode = currentLanguage)
        } else {
            Toast.makeText(context, "Audio Record permission is required for speech input. Standard keyboard remains fully enabled!", Toast.LENGTH_LONG).show()
        }
    }

    // Effect to increment counter when recording is active for display
    LaunchedEffect(isRecordingByUser) {
        if (isRecordingByUser) {
            recordingTimerVal = 0
            while (isRecordingByUser) {
                kotlinx.coroutines.delay(1000)
                recordingTimerVal++
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("app_scaffold"),
        bottomBar = {
            DebateBottomNavigation(
                activeScreen = currentScreen,
                onNavigate = { screen ->
                    viewModel.navigateTo(screen)
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A), // Slate 900
                            Color(0xFF030712)  // Gray 950
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            // Screen transit animation switcher
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) with fadeOut(animationSpec = tween(220))
                }
            ) { screen ->
                when (screen) {
                    "dashboard" -> DashboardView(viewModel)
                    "coach" -> SpeechCoachView(
                        viewModel = viewModel,
                        isRecording = isRecordingByUser,
                        recordingDuration = recordingTimerVal,
                        rmsLevel = rmsValue,
                        onToggleRecord = {
                            if (isRecordingByUser) {
                                speechRecognizerHelper.stopListening()
                                isRecordingByUser = false
                            } else {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                
                                if (hasPerm) {
                                    isRecordingByUser = true
                                    speechRecognizerHelper.startListening(languageCode = currentLanguage)
                                } else {
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        }
                    )
                    "battle" -> LiveDebateBattleView(
                        viewModel = viewModel,
                        isRecording = isRecordingByUser,
                        recordingDuration = recordingTimerVal,
                        rmsLevel = rmsValue,
                        onToggleRecord = {
                            if (isRecordingByUser) {
                                speechRecognizerHelper.stopListening()
                                isRecordingByUser = false
                            } else {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                
                                if (hasPerm) {
                                    isRecordingByUser = true
                                    speechRecognizerHelper.startListening(languageCode = currentLanguage)
                                } else {
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        }
                    )
                    "topics" -> TopicGeneratorView(viewModel)
                    "profile" -> ProgressCabinetView(viewModel)
                    else -> DashboardView(viewModel)
                }
            }
        }
    }
}

/**
 * Modern Active Bottom Navigation bar
 */
@Composable
fun DebateBottomNavigation(
    activeScreen: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF1E293B), // Slate 800
        tonalElevation = 8.dp,
        modifier = Modifier.height(72.dp).navigationBarsPadding().testTag("bottom_nav")
    ) {
        val navItems = listOf(
            Triple("dashboard", Icons.Default.Home, "app_title"),
            Triple("coach", Icons.Default.Edit, "speech_coach_header"),
            Triple("battle", Icons.Default.Notifications, "debate_battle_header"),
            Triple("topics", Icons.Default.Search, "topics_desc"),
            Triple("profile", Icons.Default.Person, "stats_header")
        )

        val isHindi = Localization.currentLanguage == "Hindi"

        navItems.forEach { (route, icon, labelKey) ->
            val isSelected = activeScreen == route
            val label = when (route) {
                "dashboard" -> if (isHindi) "होम" else "Home"
                "coach" -> if (isHindi) "भाषण कोच" else "Coach"
                "battle" -> if (isHindi) "डिबेट" else "Debate"
                "topics" -> if (isHindi) "विषय" else "Topics"
                "profile" -> if (isHindi) "प्रगति" else "Progress"
                else -> route
            }

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(route) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) Color(0xFFFF7E5F) else Color(0xFF94A3B8)
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color(0xFFFF7E5F) else Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0xFF334155) // Slate 700 selected pill
                )
            )
        }
    }
}

/**
 * 1. Arya Avatar Widget: Friendly Robot mentor coach visual representation
 */
@Composable
fun CoachAryaMentorWidget(
    title: String,
    subtitle: String,
    expression: String = "smiling" // smiling, analytical, talking, speaking, success
) {
    val infiniteTransition = rememberInfiniteTransition()
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF1E293B),
                        Color(0xFF334155)
                    )
                )
            )
            .border(1.dp, Color(0xFF475569), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Arya Icon Representation
        Box(
            modifier = Modifier
                .size(72.dp)
                .offset(y = floatAnim.dp)
                .background(Color(0xFF3B82F6).copy(alpha = 0.15f), CircleShape)
                .border(2.dp, Color(0xFF3B82F6), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Coach Arya friendly face emoji
                val emoji = when (expression) {
                    "analytical" -> "🧐"
                    "talking", "speaking" -> "🎙️"
                    "success" -> "👑"
                    else -> "🤖"
                }
                Text(emoji, fontSize = 34.sp)
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Coach Arya",
                color = Color(0xFFFF7E5F),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * 2. HOME SCREEN / DASHBOARD WINDOWS
 */
@Composable
fun DashboardView(viewModel: DebateViewModel) {
    val scrollState = rememberScrollState()
    val profile by viewModel.userProfile.collectAsState()
    val speeches by viewModel.speechPractices.collectAsState()
    val battles by viewModel.debateBattles.collectAsState()
    val isHindi = viewModel.currentLanguage == "Hindi"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top profile Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "${if (isHindi) "नमस्ते" else "Welcome back"}, ${profile?.name ?: "Speaker"}!",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = Localization.getText("slogan"),
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }

            // Language Switch Switcher Button
            Button(
                onClick = { viewModel.toggleLanguage() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Lang", modifier = Modifier.size(16.dp), tint = Color(0xFFFF7E5F))
                Spacer(modifier = Modifier.width(6.dp))
                Text(viewModel.currentLanguage, fontSize = 12.sp, color = Color.White)
            }
        }

        // Streak & XP Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stats XP
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFFBBF24).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⭐", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "${profile?.points ?: 10} XP", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(text = Localization.getText("overall_points"), color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 1)
                    }
                }
            }

            // Stats Streak
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFFF7E5F).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🔥", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "${profile?.streak ?: 1} ${Localization.getText("streak_count")}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(text = if (isHindi) "प्रतिदिन अभ्यास" else "Daily Streak", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                }
            }
        }

        // Arya Coach Mentor Welcome Greeting
        CoachAryaMentorWidget(
            title = if (isHindi) "आज आप कौन सा वाद-विवाद मंच जीतने जा रहे हैं?" else "What stage are we conquering today?",
            subtitle = if (isHindi) "भाषण लिखवाना हो या रोबोटिक विरोधी से आमने-सामने की डिबेट करनी हो, मैं तैयार हूँ!" else "Whether generating scripts, researching logic, or going head-to-head in our Debate battle - I'm in your corner!"
        )

        // Daily Challenge Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)), // Dark Indigo
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF4338CA))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF312E81), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = Localization.getText("daily_challenge"),
                            color = Color(0xFFC7D2FE),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text("⏳ 24h", color = Color(0xFF94A3B8), fontSize = 12.sp)
                }

                Text(
                    text = Localization.getText("challenge_topic"),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (isHindi) {
                        "पॉइंट्स बोनस: +150 XP! एक पक्ष चुनें और तर्क देकर कौशल बढ़ाएं।"
                    } else {
                        "Points Reward: +150 XP! Defeat the AI opponent with solid, factual counters."
                    },
                    color = Color(0xFF818CF8),
                    fontSize = 12.sp
                )

                Button(
                    onClick = { viewModel.loadDailyChallenge() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7E5F)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = Localization.getText("accept_challenge"), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Mini Navigation Shortcuts Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.navigateTo("coach") },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🗣️", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isHindi) "भाषण कोच" else "Speech Coach",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (isHindi) "लिखें और सीखें" else "Generate & Test",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.navigateTo("battle") },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("⚔️", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isHindi) "वाद-विवाद बैटल" else "Debate Battle",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (isHindi) "रोबोट से भिड़ें" else "Fight Opponent",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Saved Recent history overview
        Text(
            text = if (isHindi) "गतिविधि इतिहास" else "Recent Practice Logs",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        if (speeches.isEmpty() && battles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🌟", fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isHindi) "अभी तक कोई अभ्यास नहीं!" else "Your stage is empty!",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isHindi) "शुरू करने के लिए ऊपर दी गई श्रेणियों में से कोई एक चुनें।" else "Begin a speech training session or debate to view logs here.",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Render latest 3 unified items
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                speeches.take(2).forEach { speech ->
                    HistoryPracticeCard(speech, isHindi)
                }
                battles.take(2).forEach { battle ->
                    HistoryBattleCard(battle, isHindi)
                }
            }
        }
    }
}

@Composable
fun HistoryPracticeCard(item: SpeechPractice, isHindi: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🗣️ ${if (isHindi) "भाषण मूल्यांकन" else "Speech Practice"}",
                    color = Color(0xFFFF7E5F),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.topic,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Tone: ${item.tone} • Age: ${item.ageGroup}",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF10B981).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${item.score}",
                    color = Color(0xFF10B981),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun HistoryBattleCard(item: DebateBattle, isHindi: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "⚔️ ${if (isHindi) "डिबेट मुकाबला" else "Debate Match"}",
                    color = Color(0xFF3B82F6),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.topic,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Side: ${item.side} • ${item.difficulty}",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF3B82F6).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${item.score}",
                    color = Color(0xFF3B82F6),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 3. SPEECH COACH AND GENERATOR WINDOW
 */
@Composable
fun SpeechCoachView(
    viewModel: DebateViewModel,
    isRecording: Boolean,
    recordingDuration: Int,
    rmsLevel: Float,
    onToggleRecord: () -> Unit
) {
    val scrollState = rememberScrollState()
    val isHindi = viewModel.currentLanguage == "Hindi"
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Screen Header Label
        Text(
            text = Localization.getText("speech_coach_header"),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        // Arya Coach character speaking instructions
        CoachAryaMentorWidget(
            title = if (isHindi) "आइये, मिलकर आपका डर भगाते हैं!" else "Let's craft and test your next speech!",
            subtitle = if (isHindi) {
                "संकेत: बाईं ओर टॉपिक लिखें या आटोमेटिक स्पीच स्क्रिप्ट जनरेट करें। अभ्यास करने के बाद 'भाषण विश्लेषण' बटन दबाएं।"
            } else {
                "Fill out the options below for a polished script. Then read it aloud or write your transcript for live analytical evaluation!"
            },
            expression = if (viewModel.isSpeechGenerating || viewModel.isAnalyzingSpeech) "talking" else "smiling"
        )

        // Configuration Inputs Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Topic Selection Outliner Area
                Text(Localization.getText("topic_label"), color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = viewModel.enteredTopic,
                    onValueChange = { viewModel.enteredTopic = it },
                    placeholder = { Text(if (isHindi) "जैसे: खेलों का महत्व..." else "e.g., The futuristic impact of robotics...") },
                    modifier = Modifier.fillMaxWidth().testTag("speech_topic_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF7E5F)
                    )
                )

                // Age group, Speech length, Tone Selectors
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Age group Column dropdown simulator
                    Column(modifier = Modifier.weight(1f)) {
                        Text(Localization.getText("select_age"), color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF334155), RoundedCornerShape(10.dp))
                                .clickable {
                                    viewModel.selectedAgeGroup = when (viewModel.selectedAgeGroup) {
                                        "Child (8-12)" -> "Teen (13-17)"
                                        "Teen (13-17)" -> "Adult (18+)"
                                        else -> "Child (8-12)"
                                    }
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(viewModel.selectedAgeGroup, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Icon(Icons.Default.Add, contentDescription = "Switch", tint = Color(0xFFFF7E5F), modifier = Modifier.size(14.dp))
                        }
                    }

                    // Length Column dropdown simulator
                    Column(modifier = Modifier.weight(1f)) {
                        Text(Localization.getText("select_len"), color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF334155), RoundedCornerShape(10.dp))
                                .clickable {
                                    viewModel.selectedLength = when (viewModel.selectedLength) {
                                        "Short" -> "Medium"
                                        "Medium" -> "Long"
                                        else -> "Short"
                                    }
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(viewModel.selectedLength, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Icon(Icons.Default.Add, contentDescription = "Switch", tint = Color(0xFFFF7E5F), modifier = Modifier.size(14.dp))
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    // Tone Selector Dropdown simulator
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(Localization.getText("select_tone"), color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF334155), RoundedCornerShape(10.dp))
                                .clickable {
                                    viewModel.selectedTone = when (viewModel.selectedTone) {
                                        "Persuasive" -> "Informative"
                                        "Informative" -> "Humorous"
                                        "Humorous" -> "Inspiring"
                                        else -> "Persuasive"
                                    }
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(viewModel.selectedTone, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Icon(Icons.Default.Add, contentDescription = "Switch", tint = Color(0xFFFF7E5F), modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // AI Speech Generation trigger Button
                Button(
                    onClick = { viewModel.startGenerateSpeech() },
                    modifier = Modifier.fillMaxWidth().testTag("generate_speech_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !viewModel.isSpeechGenerating
                ) {
                    if (viewModel.isSpeechGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(if (isHindi) "भाषण स्क्रिप्ट तैयार हो रही है..." else "Generating Script...", color = Color.White)
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Generate", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Localization.getText("btn_generate_speech"), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Output of AI generated script container
        if (viewModel.generatedSpeechText.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Localization.getText("generated_speech_heading"),
                            color = Color(0xFF3B82F6),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(viewModel.generatedSpeechText))
                                Toast.makeText(context, "Speech copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Copy", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(
                        text = viewModel.generatedSpeechText,
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        // Practice Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isHindi) "अपना भाषण रिकॉर्ड करें या टाइप करें:" else "Record your voice rehearsal or type feedback script:",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                // Famous Speeches quick demo load row:
                Text(Localization.getText("load_preset"), color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { viewModel.loadPresetText("vivekananda") },
                        label = { Text(Localization.getText("preset_vivekananda"), color = Color.White, fontSize = 11.sp) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF334155))
                    )
                    AssistChip(
                        onClick = { viewModel.loadPresetText("homework") },
                        label = { Text(Localization.getText("preset_homework"), color = Color.White, fontSize = 11.sp) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF334155))
                    )
                    AssistChip(
                        onClick = { viewModel.loadPresetText("nature") },
                        label = { Text(Localization.getText("preset_nature"), color = Color.White, fontSize = 11.sp) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF334155))
                    )
                }

                // Microphones Speech Waveform and button layout
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Waveform animation circles representation
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .background(if (isRecording) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFFFF7E5F).copy(alpha = 0.15f))
                                .clickable { onToggleRecord() }
                                .drawBehind {
                                    if (isRecording) {
                                        // Draw sound waveforms with dynamic RMS circle stroke
                                        drawCircle(
                                            color = Color(0xFFEF4444),
                                            radius = 42.dp.toPx() + (rmsLevel * 2f),
                                            style = Stroke(width = 2.dp.toPx())
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home, // Using home as a safe core mic alternative
                                contentDescription = "Mic",
                                tint = if (isRecording) Color(0xFFEF4444) else Color(0xFFFF7E5F),
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isRecording) "Recording... ${recordingDuration}s" else Localization.getText("hold_to_speak"),
                            color = if (isRecording) Color(0xFFEF4444) else Color(0xFFFF7E5F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Script transcription body
                OutlinedTextField(
                    value = viewModel.userSpeechInput,
                    onValueChange = { viewModel.userSpeechInput = it },
                    modifier = Modifier.fillMaxWidth().height(140.dp).testTag("speech_transcript_input"),
                    placeholder = { Text(Localization.getText("speech_placeholder"), color = Color(0xFF64748B)) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF7E5F)
                    )
                )

                // Start active Analysis Button
                Button(
                    onClick = { viewModel.startEvaluateSpeech() },
                    modifier = Modifier.fillMaxWidth().testTag("analyze_speech_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !viewModel.isAnalyzingSpeech && viewModel.userSpeechInput.isNotEmpty()
                ) {
                    if (viewModel.isAnalyzingSpeech) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(Localization.getText("evaluating"), color = Color.White)
                    } else {
                        Icon(Icons.Default.Check, contentDescription = "Check", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Localization.getText("btn_analyze"), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Render response metrics & Coach Tips panel
        viewModel.speechAnalysisResultJson?.let { ratingString ->
            SpeechFeedbackMetricsReport(ratingString, isHindi)
        }
    }
}

/**
 * Beautiful Custom Score Meter / Metrics Card representation
 */
@Composable
fun SpeechFeedbackMetricsReport(jsonString: String, isHindi: Boolean) {
    val report = remember(jsonString) {
        try {
            JSONObject(jsonString)
        } catch (e: Exception) {
            JSONObject()
        }
    }

    val overallScore = report.optInt("score", 0)
    val vocabulary = report.optInt("vocabularyScore", 0)
    val logic = report.optInt("logicScore", 0)
    val grammar = report.optInt("grammarScore", 0)
    val persuasion = report.optInt("persuasionScore", 0)
    val clarity = report.optInt("clarityScore", 0)
    val critiqueText = report.optString("critique", "Outstanding attempt!")

    val strengths = remember(report) {
        val arr = report.optJSONArray("strongPoints") ?: JSONArray()
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) list.add(arr.getString(i))
        list
    }

    val improvements = remember(report) {
        val arr = report.optJSONArray("improvementPoints") ?: JSONArray()
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) list.add(arr.getString(i))
        list
    }

    val homeworkTasks = remember(report) {
        val arr = report.optJSONArray("exercises") ?: JSONArray()
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) list.add(arr.getString(i))
        list
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("speech_feedback_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color(0xFF10B981))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Circular Progress Score Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large Score widget
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFF10B981).copy(alpha = 0.15f), CircleShape)
                        .border(3.dp, Color(0xFF10B981), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$overallScore", color = Color(0xFF10B981), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text(text = "/100", color = Color(0xFF94A3B8), fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = if (isHindi) "Arya का समग्र मूल्यांकन" else "Arya's Adjudication Verdict",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = critiqueText,
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            // Metric Progress Bars grid (Logic, Vocab, Persuasion, Grammar, Clarity)
            Text(
                text = Localization.getText("stats_breakdown"),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            val barStats = listOf(
                Pair(Localization.getText("fluency"), clarity),
                Pair(Localization.getText("persuasion"), persuasion),
                Pair(Localization.getText("vocabulary"), vocabulary),
                Pair(Localization.getText("logic"), logic),
                Pair(Localization.getText("grammar"), grammar)
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                barStats.forEach { (name, score) ->
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(name, color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text("$score%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = score / 100f,
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = when {
                                score >= 85 -> Color(0xFF10B981)
                                score >= 70 -> Color(0xFF3B82F6)
                                else -> Color(0xFFFF7E5F)
                            },
                            trackColor = Color(0xFF334155)
                        )
                    }
                }
            }

            // Strengths and exercises lists:
            if (strengths.isNotEmpty()) {
                Divider(color = Color(0xFF334155))
                Text(
                    text = if (isHindi) "🤩 आपकी मुख्य खूबियाँ:" else "🤩 Your Core Strengths:",
                    color = Color(0xFF10B981),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                strengths.forEach { point ->
                    Text("• $point", color = Color.White, fontSize = 12.sp)
                }
            }

            if (improvements.isNotEmpty()) {
                Text(
                    text = if (isHindi) "💡 सुधार की जरूरतें:" else "💡 Specific Improvements:",
                    color = Color(0xFFFF7E5F),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                improvements.forEach { point ->
                    Text("• $point", color = Color.White, fontSize = 12.sp)
                }
            }

            if (homeworkTasks.isNotEmpty()) {
                Divider(color = Color(0xFF334155))
                Text(
                    text = if (isHindi) "🎯 अनुशंसित अभ्यास:" else "🎯 Improvement Exercises:",
                    color = Color(0xFFFBBF24),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                homeworkTasks.forEach { task ->
                    Text("📍 $task", color = Color.White, fontSize = 12.sp, lineHeight = 16.sp)
                }
            }
        }
    }
}

/**
 * 4. LIVE DEBATE RING BATTLE SCREEN
 */
@Composable
fun LiveDebateBattleView(
    viewModel: DebateViewModel,
    isRecording: Boolean,
    recordingDuration: Int,
    rmsLevel: Float,
    onToggleRecord: () -> Unit
) {
    val scrollState = rememberScrollState()
    val isHindi = viewModel.currentLanguage == "Hindi"
    var textInput by remember { mutableStateOf("") }
    val currentChatLogs = viewModel.debateChatList

    // Auto scrolled debate chat listing
    val chatScroll = rememberScrollState()
    LaunchedEffect(currentChatLogs.size) {
        if (currentChatLogs.isNotEmpty()) {
            chatScroll.animateScrollTo(chatScroll.maxValue)
        }
    }

    if (!viewModel.isBattleStarted) {
        // Setup initial matchmaking ring options
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = Localization.getText("debate_battle_header"),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            CoachAryaMentorWidget(
                title = if (isHindi) "डिबेट रिंग में उतरें!" else "Face the AI Opponent Arena!",
                subtitle = if (isHindi) {
                    "अपनी पसंद का विषय और कठिनाई स्तर चुनें। विरोधी रोबोट बुद्धिमान विचारों, जबरदस्त रीबटल्स और प्रति-तर्कों के साथ प्रहार करेगा।"
                } else {
                    "Choose variables below, choose your arguing side, and enter the ring. The AI opponent adjusts counters dynamically!"
                }
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Debate topic entry
                    Text(Localization.getText("topic_label"), color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = viewModel.debateTopic,
                        onValueChange = { viewModel.debateTopic = it },
                        modifier = Modifier.fillMaxWidth().testTag("debate_topic_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFF7E5F)
                        )
                    )

                    // Debate Side (FOR/AGAINST)
                    Text(Localization.getText("side"), color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.debateSide = "FOR / Pro-Topic" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.debateSide.contains("FOR")) Color(0xFF10B981) else Color(0xFF334155)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(Localization.getText("side_for"), color = Color.White)
                        }

                        Button(
                            onClick = { viewModel.debateSide = "AGAINST / Con-Topic" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.debateSide.contains("AGAINST")) Color(0xFFEF4444) else Color(0xFF334155)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(Localization.getText("side_against"), color = Color.White)
                        }
                    }

                    // Difficulty Dropdowns
                    Text(Localization.getText("difficulty"), color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val diffs = listOf("Beginner", "Intermediate", "Advanced")
                        diffs.forEach { d ->
                            val isSelected = viewModel.debateDifficulty == d
                            val readableLabel = when (d) {
                                "Beginner" -> Localization.getText("difficulty_low")
                                "Intermediate" -> Localization.getText("difficulty_mid")
                                else -> Localization.getText("difficulty_high")
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.debateDifficulty = d },
                                label = { Text(readableLabel, color = Color.White, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFF7E5F),
                                    containerColor = Color(0xFF334155)
                                )
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.startDebateBattle() },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag("start_debate_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7E5F)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = "Ring", tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Localization.getText("btn_start_battle"), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    } else {
        // Active Ongoing debate ring view
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Battle Header Banner info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { viewModel.resetDebateState() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        text = viewModel.debateTopic,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Side: ${viewModel.debateSide} • Level: ${viewModel.debateDifficulty}",
                        color = Color(0xFFFF7E5F),
                        fontSize = 11.sp
                    )
                }
                IconButton(onClick = { viewModel.concludeAndJudgeBattle() }) {
                    Icon(Icons.Default.Share, contentDescription = "Judge", tint = Color(0xFF10B981))
                }
            }

            // Realtime Coach Arya Hints indicator Drawer collapsible
            if (viewModel.currentCoachHint.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF6366F1))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💡", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${Localization.getText("battle_coaching_hint")}: ${viewModel.currentCoachHint}",
                            color = Color(0xFFC7D2FE),
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            // Dynamic Scrollable Dialogue card thread
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(chatScroll),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    currentChatLogs.forEach { log ->
                        val sender = log["sender"] ?: "user"
                        val textStr = log["message"] ?: ""

                        when (sender) {
                            "referee" -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF334155).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                        .padding(10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = textStr,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            "opponent" -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .background(Color(0xFFEF4444).copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                        .padding(12.dp)
                                        .align(Alignment.Start)
                                ) {
                                    Column {
                                        Text(
                                            text = if (isHindi) "🚨 विरोधी डिबेटर (AI Opponent)" else "🚨 AI Opponent",
                                            color = Color(0xFFFF7E5F),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(textStr, color = Color.White, fontSize = 13.sp, lineHeight = 18.sp)
                                    }
                                }
                            }
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .background(Color(0xFF3B82F6).copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                        .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                        .padding(12.dp)
                                        .align(Alignment.End)
                                ) {
                                    Column {
                                        Text(
                                            text = if (isHindi) "👤 मेरा तर्क (User)" else "👤 My Argument (User)",
                                            color = Color(0xFF60A5FA),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(textStr, color = Color.White, fontSize = 13.sp, lineHeight = 18.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Opponent speaking typing loader:
                    if (viewModel.isOpponentSpeaking) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFFFF7E5F))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHindi) "विरोधी विचार विमर्श कर रहे हैं..." else "Opponent preparing logic rebuttal...",
                                color = Color(0xFFFF7E5F),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Results Judgement card overlay if active
            viewModel.battleAdjudicationJson?.let { ratingString ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F172A))
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "🏁 Battle Complete Assessment",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        SpeechFeedbackMetricsReport(ratingString, isHindi)
                        Button(
                            onClick = { viewModel.resetDebateState() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7E5F)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (isHindi) "डिबेट रिंग से बाहर आएं" else "Exit Debate Arena", color = Color.White)
                        }
                    }
                }
            }

            // Dialogue interactive bottom control row (Mic / Speech and Typing)
            if (viewModel.battleAdjudicationJson == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Hold to mic column button shortcut
                        IconButton(
                            onClick = onToggleRecord,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isRecording) Color(0xFFEF4444) else Color(0xFFFF7E5F))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home, // Mic core representation fallback
                                contentDescription = "Mic",
                                tint = Color.White
                            )
                        }

                        // Text input field
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text(if (isHindi) "यहाँ डिबेट प्रत्युत्तर लिखें..." else "Draft logical argument rebuttal...") },
                            modifier = Modifier.weight(1f).testTag("debate_rebuttal_input"),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFF7E5F)
                            )
                        )

                        // Send statement trigger button
                        IconButton(
                            onClick = {
                                if (textInput.isNotEmpty()) {
                                    viewModel.submitUserArgument(textInput)
                                    textInput = ""
                                }
                            },
                            enabled = textInput.isNotEmpty() && !viewModel.isOpponentSpeaking,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (textInput.isNotEmpty()) Color(0xFF3B82F6) else Color(0xFF334155))
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Send", tint = Color.White)
                        }
                    }
                }

                // Finish / End button option
                Button(
                    onClick = { viewModel.concludeAndJudgeBattle() },
                    modifier = Modifier.fillMaxWidth().testTag("conclude_debate_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !viewModel.isEvaluatingBattle
                ) {
                    if (viewModel.isEvaluatingBattle) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Localization.getText("evaluating"), color = Color.White)
                    } else {
                        Text(Localization.getText("btn_conclude_battle"), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * 5. TOPIC GENERATOR AND FACT SHEET VIEWS
 */
@Composable
fun TopicGeneratorView(viewModel: DebateViewModel) {
    val scrollState = rememberScrollState()
    val isHindi = viewModel.currentLanguage == "Hindi"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = Localization.getText("topic_generator_header"),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        CoachAryaMentorWidget(
            title = if (isHindi) "तर्कों का खजाना यहाँ है!" else "Instant Debate Brainstorms!",
            subtitle = if (isHindi) {
                "विभिन्न श्रेणियों और स्तरों के वाद-विवाद विषय खोजें। प्रत्येक विषय के साथ पक्ष (Pros) और विपक्ष (Cons) के प्रमुख बिंदु भी उपलब्ध होंगे।"
            } else {
                "Pick a domain and level. I'll provide dynamic hot topics paired with double-sided fact sheets to fuel your logic!"
            }
        )

        // Topic Settings panel card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category tabs mapping
                Text(Localization.getText("category"), color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val cats = listOf("Education", "Tech & AI", "Ethics", "Society", "Sports")
                    cats.forEach { c ->
                        val isSelected = viewModel.selectedCategory == c
                        val labelStr = when (c) {
                            "Education" -> Localization.getText("cat_edu")
                            "Tech & AI" -> Localization.getText("cat_tech")
                            "Ethics" -> Localization.getText("cat_ethics")
                            "Society" -> Localization.getText("cat_society")
                            else -> Localization.getText("cat_sports")
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectedCategory = c },
                            label = { Text(labelStr, color = Color.White, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF3B82F6),
                                containerColor = Color(0xFF334155)
                            )
                        )
                    }
                }

                // Difficulty selectors
                Text(Localization.getText("difficulty"), color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val diffs = listOf("Beginner", "Intermediate", "Advanced")
                    diffs.forEach { d ->
                        val isSelected = viewModel.selectedLevel == d
                        val readableLabel = when (d) {
                            "Beginner" -> Localization.getText("difficulty_low")
                            "Intermediate" -> Localization.getText("difficulty_mid")
                            else -> Localization.getText("difficulty_high")
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectedLevel = d },
                            label = { Text(readableLabel, color = Color.White, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFF7E5F),
                                containerColor = Color(0xFF334155)
                            )
                        )
                    }
                }

                Button(
                    onClick = { viewModel.startGenerateTopics() },
                    modifier = Modifier.fillMaxWidth().testTag("generate_topics_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7E5F)),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !viewModel.isGeneratingTopics
                ) {
                    if (viewModel.isGeneratingTopics) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(if (isHindi) "विषय तैयार हो रहे हैं..." else "Curating hot topics...", color = Color.White)
                    } else {
                        Text(Localization.getText("generate_topics_btn"), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Render Topics Sheet results list view
        Text(Localization.getText("topics_desc"), color = Color(0xFF94A3B8), fontSize = 12.sp)

        if (viewModel.generatedTopicsList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isHindi) "कोई विषय नहीं मिला। खोज शुरू करने के लिए ऊपर दिए गए बटन को दबाएं।" else "Tap the generate button above to populate top-tier topics!",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                viewModel.generatedTopicsList.forEach { topic ->
                    val title = topic["title"] as? String ?: ""
                    val desc = topic["description"] as? String ?: ""
                    val pros = topic["pros"] as? List<String> ?: emptyList()
                    val cons = topic["cons"] as? List<String> ?: emptyList()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(desc, color = Color(0xFF94A3B8), fontSize = 12.sp, lineHeight = 16.sp)

                            // double columns for pros/cons with clean alignment
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Pros column green
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(Color(0xFF10B981).copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = if (isHindi) "✅ पक्ष (PROS)" else "✅ PROS / Support",
                                        color = Color(0xFF10B981),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    pros.forEach { p ->
                                        Text("• $p", color = Color.White, fontSize = 10.sp, lineHeight = 13.sp)
                                    }
                                }

                                // Cons column red
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(Color(0xFFEF4444).copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = if (isHindi) "❌ विपक्ष (CONS)" else "❌ CONS / Counter",
                                        color = Color(0xFFEF4444),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    cons.forEach { c ->
                                        Text("• $c", color = Color.White, fontSize = 10.sp, lineHeight = 13.sp)
                                    }
                                }
                            }

                            // Quick routing shortcuts
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.useTopicInSpeechOrDebate(title, "speech") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(if (isHindi) "भाषण अभ्यास" else "Practice Speech", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = { viewModel.useTopicInSpeechOrDebate(title, "debate") },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7E5F)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(if (isHindi) "डिबेट रिंग" else "Live Debate Ring", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 6. PROGRESS TRAININGS PLANS & HISTORIC ACHIEVEMENT BADGES
 */
@Composable
fun ProgressCabinetView(viewModel: DebateViewModel) {
    val scrollState = rememberScrollState()
    val isHindi = viewModel.currentLanguage == "Hindi"
    val profile by viewModel.userProfile.collectAsState()
    val allSpeeches by viewModel.speechPractices.collectAsState()
    val allBattles by viewModel.debateBattles.collectAsState()

    val earnedBadgesSet = remember(profile) {
        val list = mutableSetOf<String>()
        profile?.let { p ->
            try {
                val arr = JSONArray(p.badgesJson)
                for (i in 0 until arr.length()) {
                    list.add(arr.getString(i))
                }
            } catch (e: Exception) {
                // Ignore parsing errors
            }
        }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = Localization.getText("stats_header"),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        // Arya Coach training plan advice greeting block
        CoachAryaMentorWidget(
            title = if (isHindi) "आपकी शानदार वाक्पटुता प्रगति!" else "Your Orator Training Blueprint",
            subtitle = if (isHindi) {
                "संकेत: जैसे-जैसे आप भाषण अभ्यास और डिबेट पूर्ण करेंगे, आप उच्च मेडल और बैज अनलॉक करेंगे!"
            } else {
                "Consistent debate drills rewrite neurological neural links for speech conviction. Unlock all 6 master tags!"
            },
            expression = "success"
        )

        // Point Ledger / Stats Row Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(Localization.getText("current_level"), color = Color(0xFF94A3B8), fontSize = 12.sp)
                Text(
                    text = "${if (isHindi) "स्तर" else "Level"} ${profile?.level ?: 1}",
                    color = Color(0xFFFBBF24),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Battle EXP Score: ${profile?.points ?: 10} XP",
                    color = Color.White,
                    fontSize = 14.sp
                )

                // level progress calculator
                val pointsToNext = 250 - ((profile?.points ?: 0) % 250)
                Text(
                    text = if (isHindi) {
                        "अगले स्तर के लिए केवल $pointsToNext XP और चाहिए!"
                    } else {
                        "Only $pointsToNext XP remaining to level up!"
                    },
                    color = Color(0xFF10B981),
                    fontSize = 11.sp
                )
            }
        }

        // Dynamic Badge showcase chest mapping (unlocked check)
        Text(Localization.getText("unlocked_badges"), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)

        val masterBadgesModel = listOf(
            Triple("Ice Breaker", "🥶", if (isHindi) "पहला कदम - पहला भाषण अभ्यास संपन्न।" else "Ice Breaker - Finished first practice session."),
            Triple("Logic Guru", "🧐", if (isHindi) "तार्किक सम्राट - तार्किक सोच का स्कोर > 85!" else "Logic Guru - Logic accuracy evaluation > 85%!"),
            Triple("Persuasion Master", "🎭", if (isHindi) "मनाने का राजा - भाषण में मना लेने की क्षमता का स्कोर > 88!" else "Persuasion Master - Persuasion score > 88%!"),
            Triple("Hindi Orator", "🪈", if (isHindi) "हिंदी सम्राट - हिंदी भाषा के अभ्यास में स्कोर > 80!" else "Hindi Orator - Excel speech practice in Hindi!"),
            Triple("Debate Champion", "🏆", if (isHindi) "डिबेट चैंपियन - उन्नत कठिनाई में विरोधी रोबोट को हराया।" else "Debate Champion - Overpowered AI opponent in Advanced difficulty!"),
            Triple("Silver Tongue", "🗣️", if (isHindi) "जादुई वक्ता - संचित XP अंक 500+ पार!" else "Silver Tongue - Accumulated over 500+ XP points!")
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            masterBadgesModel.forEach { (badge, emoji, desc) ->
                val isUnlocked = earnedBadgesSet.contains(badge)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUnlocked) Color(0xFF1E293B) else Color(0xFF1E293B).copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.dp, if (isUnlocked) Color(0xFFFBBF24) else Color(0xFF334155))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(
                                    if (isUnlocked) Color(0xFFFBBF24).copy(alpha = 0.12f) else Color(0xFF334155),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = if (isUnlocked) 24.sp else 16.sp)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = badge,
                                    color = if (isUnlocked) Color.White else Color(0xFF64748B),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                if (isUnlocked) {
                                    Text("✓ Unlocked", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Text("🔒 Locked", color = Color(0xFF64748B), fontSize = 10.sp)
                                }
                            }
                            Text(
                                text = desc,
                                color = if (isUnlocked) Color(0xFF94A3B8) else Color(0xFF64748B),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

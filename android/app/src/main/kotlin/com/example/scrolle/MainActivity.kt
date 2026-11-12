package com.example.scrolle

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import java.util.Locale
import kotlinx.coroutines.delay
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.scrolle.ui.theme.ScrolleTheme

class MainActivity : ComponentActivity() {

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { renderUi() }

    private fun isAccessibilityEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            if (splitter.next().contains("ReelAccessibilityService", ignoreCase = true)) return true
        }
        return false
    }

    private fun startFloatingOverlay() {
        val intent = Intent(this, OverlayService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
            startForegroundService(intent)
        else startService(intent)
        moveTaskToBack(true)
    }

    private fun stopFloatingOverlay() = stopService(Intent(this, OverlayService::class.java))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        renderUi()
    }

    override fun onResume() {
        super.onResume()
        renderUi()
    }

    private fun renderUi() {
        setContent {
            ScrolleTheme {
                ScrolleApp(
                    hasOverlayPermission = Settings.canDrawOverlays(this),
                    hasAccessibilityPermission = isAccessibilityEnabled(this),
                    onRequestOverlay = {
                        overlayPermissionLauncher.launch(
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                        )
                    },
                    onRequestAccessibility = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    onStartOverlay = { startFloatingOverlay() },
                    onStopOverlay = { stopFloatingOverlay() }
                )
            }
        }
    }
}

// =========================================================================
// DESIGN TOKENS
// =========================================================================
val BgCream       = Color(0xFFF7F4EE)
val SurfaceWhite  = Color(0xFFFFFFFF)
val SurfaceSand   = Color(0xFFEFE9E0)
val HeroDark      = Color(0xFF232220)
val AccentCaramel = Color(0xFFB88E72)
val AccentMocha   = Color(0xFFA27B5C)
val BorderSand    = Color(0xFFE5DDD2)
val TextPrimary   = Color(0xFF1F1E1D)
val TextMuted     = Color(0xFF8A847C)
val GreenActive   = Color(0xFF2E7D32)
val GreenLight    = Color(0xFF4CAF50)
val RedDanger     = Color(0xFFDC2626)

// =========================================================================
// ROOT APP SHELL
// =========================================================================
@Composable
fun ScrolleApp(
    hasOverlayPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    onRequestOverlay: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit
) {
    val context = LocalContext.current
    val statsManager = remember { DailyStatsManager(context) }

    var selectedTab by remember { mutableStateOf(0) }
    var overlayActive by remember { mutableStateOf(OverlayService.instance != null) }
    var todayTotal by remember { mutableStateOf(statsManager.getTodayTotal()) }
    var dailyGoal by remember { mutableStateOf(statsManager.getDailyGoal()) }
    var isLoggedIn by remember { mutableStateOf(statsManager.isLoggedIn()) }
    var userEmail by remember { mutableStateOf(statsManager.getUserEmail()) }
    var userName by remember { mutableStateOf(statsManager.getUserName()) }
    var showReceiptDialog by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    // Live refresh every 3s to pick up overlay counts
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000L)
            todayTotal = statsManager.getTodayTotal()
            overlayActive = OverlayService.instance != null
        }
    }

    fun refreshStats() {
        todayTotal = statsManager.getTodayTotal()
        dailyGoal = statsManager.getDailyGoal()
        isLoggedIn = statsManager.isLoggedIn()
        userEmail = statsManager.getUserEmail()
        userName = statsManager.getUserName()
        overlayActive = OverlayService.instance != null
    }

    Scaffold(
        containerColor = BgCream,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(40.dp))
                        .background(SurfaceWhite)
                        .border(1.dp, BorderSand, RoundedCornerShape(40.dp))
                        .padding(horizontal = 6.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabs = listOf(
                        Triple("Counter", "🧠", 0),
                        Triple("Focus", "⏱️", 1),
                        Triple("Insights", "📊", 2),
                        Triple("Profile", "👤", 3)
                    )
                    tabs.forEach { (title, icon, index) ->
                        val isSel = selectedTab == index
                        val bgAnim by animateColorAsState(
                            if (isSel) HeroDark else Color.Transparent,
                            animationSpec = tween(220), label = "tab_bg"
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(30.dp))
                                .background(bgAnim)
                                .clickable { selectedTab = index; refreshStats() }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = icon, fontSize = 16.sp)
                            AnimatedVisibility(visible = isSel) {
                                Row {
                                    Spacer(Modifier.width(6.dp))
                                    Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SurfaceWhite)
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BgCream)
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(
                    userName = if (isLoggedIn) userName else "There",
                    todayTotal = todayTotal,
                    dailyGoal = dailyGoal,
                    overlayActive = overlayActive,
                    hasOverlayPermission = hasOverlayPermission,
                    hasAccessibilityPermission = hasAccessibilityPermission,
                    statsManager = statsManager,
                    onToggleOverlay = {
                        overlayActive = !overlayActive
                        if (overlayActive) onStartOverlay() else onStopOverlay()
                    },
                    onRequestOverlay = onRequestOverlay,
                    onRequestAccessibility = onRequestAccessibility,
                    onGoalChange = { statsManager.setDailyGoal(it); dailyGoal = it },
                    onOpenProfile = { selectedTab = 3 },
                    onShowReceipt = { showReceiptDialog = true },
                    onRefresh = { refreshStats() }
                )
                1 -> FocusScreen(statsManager = statsManager, onHealCompleted = { refreshStats() })
                2 -> InsightsScreen(statsManager = statsManager)
                3 -> ProfileScreen(
                    isLoggedIn = isLoggedIn,
                    userName = userName,
                    userEmail = userEmail,
                    statsManager = statsManager,
                    onOpenLogin = { showLoginDialog = true },
                    onLogout = { statsManager.logout(); refreshStats() },
                    onResetToday = { showResetConfirm = true }
                )            }
        }

        if (showReceiptDialog) {
            ScreenTimeReceiptDialog(
                receipt = statsManager.generateReceipt(),
                onDismiss = { showReceiptDialog = false },
                onCloseApp = {
                    showReceiptDialog = false
                    context.startActivity(Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }
            )
        }

        if (showLoginDialog) {
            EmailLoginDialog(
                onDismiss = { showLoginDialog = false },
                onLoginSuccess = { email, name ->
                    statsManager.login(email, name)
                    refreshStats()
                    showLoginDialog = false
                    Toast.makeText(context, "Welcome, $name! 🎉", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Reset confirmation dialog — prevents accidental data loss
        if (showResetConfirm) {
            Dialog(onDismissRequest = { showResetConfirm = false }) {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                        .background(SurfaceWhite).border(1.dp, BorderSand, RoundedCornerShape(24.dp)).padding(24.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚠️", fontSize = 36.sp)
                        Text("Reset Today's Count?", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = TextPrimary, textAlign = TextAlign.Center)
                        Text(
                            "This will permanently delete today's reel count and screen time. This cannot be undone.",
                            color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 17.sp
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { showResetConfirm = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Cancel", color = TextPrimary) }
                            Button(
                                onClick = {
                                    statsManager.resetToday()
                                    refreshStats()
                                    showResetConfirm = false
                                    Toast.makeText(context, "Today's count reset ✓", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = RedDanger),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Yes, Reset", color = SurfaceWhite, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// SCREEN 1: 🧠 DASHBOARD
// =========================================================================
@Composable
fun DashboardScreen(
    userName: String,
    todayTotal: Int,
    dailyGoal: Int,
    overlayActive: Boolean,
    hasOverlayPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    statsManager: DailyStatsManager,
    onToggleOverlay: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onGoalChange: (Int) -> Unit,
    onOpenProfile: () -> Unit,
    onShowReceipt: () -> Unit,
    onRefresh: () -> Unit
) {
    val brainrot = statsManager.getBrainrotStatus()
    val progressFraction = (todayTotal.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f)
    val tips = listOf(
        "Put your phone face down to break the habit loop.",
        "Every reel swipe is 30 seconds of your life — choose wisely.",
        "Replace one scroll session today with a 5-min walk.",
        "Your attention is the most valuable thing you own.",
        "Dopamine fasting for 30 mins resets your focus baseline.",
        "Notice how you feel after 10 reels vs. 10 pages of a book.",
        "Apps are designed to be addictive. Awareness is your shield.",
        "The best reel you'll ever watch is real life unfolding."
    )
    val tip = remember { tips.random() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ── TOP BAR ──────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Hello,", color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text("$userName 👋", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Live dot
                if (overlayActive) {
                    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
                        initialValue = 0.7f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pf"
                    )
                    Box(
                        modifier = Modifier.size(8.dp).scale(pulse)
                            .clip(CircleShape).background(GreenLight)
                    )
                }
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                        .background(SurfaceSand).border(1.dp, BorderSand, CircleShape)
                        .clickable { onOpenProfile() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(userName.take(1).uppercase(), color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }
        }

        // ── HERO DARK CARD ────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(HeroDark)
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "TODAY'S SCROLL",
                            color = AccentCaramel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            AnimatedContent(
                                targetState = todayTotal,
                                transitionSpec = { slideInVertically { -it } togetherWith slideOutVertically { it } },
                                label = "cnt"
                            ) { c ->
                                Text("$c", fontSize = 60.sp, fontWeight = FontWeight.Black, color = SurfaceWhite)
                            }
                            Text(
                                " reels",
                                color = SurfaceWhite.copy(alpha = 0.4f),
                                fontSize = 15.sp,
                                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                            )
                        }
                    }

                    // Brain Score ring on the right
                    Box(modifier = Modifier.size(68.dp).padding(top = 4.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(Color.White.copy(alpha = 0.1f), -90f, 360f, false,
                                style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
                            drawArc(
                                when {
                                    brainrot.healthPercent >= 75 -> GreenLight
                                    brainrot.healthPercent >= 40 -> AccentCaramel
                                    else -> RedDanger
                                },
                                -90f, 360f * (brainrot.healthPercent / 100f), false,
                                style = Stroke(6.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(brainrot.emoji, fontSize = 18.sp)
                            Text(
                                "${brainrot.healthPercent}%",
                                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                color = SurfaceWhite
                            )
                        }
                    }
                }

                // Progress bar toward daily goal
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Daily Limit", color = SurfaceWhite.copy(alpha = 0.5f), fontSize = 11.sp)
                        Text(
                            "$todayTotal / $dailyGoal",
                            color = if (progressFraction >= 1f) RedDanger else AccentCaramel,
                            fontSize = 11.sp, fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth().height(5.dp)
                            .clip(RoundedCornerShape(50)).background(SurfaceWhite.copy(alpha = 0.1f))
                    ) {
                        val animProg by animateFloatAsState(progressFraction, tween(600), label = "prog")
                        Box(
                            modifier = Modifier.fillMaxWidth(animProg).fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .background(
                                    when {
                                        progressFraction >= 1f -> RedDanger
                                        progressFraction >= 0.7f -> AccentCaramel
                                        else -> GreenLight
                                    }
                                )
                        )
                    }
                }

                // Time spent + mins estimate
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    val screenSecs = statsManager.getTodayScreenTimeSecs()
                    val timeLabel = if (screenSecs > 60) {
                        statsManager.formatScreenTime(screenSecs) + " in social apps"
                    } else {
                        // No session tracked yet — show reels × 20s estimate clearly marked as estimate
                        val estSecs = todayTotal * 20
                        if (estSecs < 60) "< 1 min (est.)" else statsManager.formatScreenTime(estSecs) + " (est.)"
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⏱", fontSize = 12.sp)
                        Spacer(Modifier.width(4.dp))
                        Text(timeLabel, color = SurfaceWhite.copy(alpha = 0.55f), fontSize = 12.sp)
                    }
                    if (overlayActive) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(20)).background(GreenLight.copy(alpha = 0.18f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("● LIVE", color = GreenLight, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                        }
                    }
                }

                // Launch / Stop Button
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val btnScale by animateFloatAsState(if (isPressed) 0.96f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "bs")

                Box(
                    modifier = Modifier.fillMaxWidth().scale(btnScale)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (overlayActive) Color(0xFF7F1D1D) else AccentCaramel)
                        .clickable(interactionSource = interactionSource, indication = null) { onToggleOverlay() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (overlayActive) "⏹  Stop Floating Counter" else "▶  Launch Floating Counter",
                        color = SurfaceWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp
                    )
                }
            }
        }

        // ── BRAIN STATUS CARD ─────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                .background(SurfaceWhite).border(1.dp, BorderSand, RoundedCornerShape(22.dp)).padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(brainrot.emoji, fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(brainrot.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(brainrot.subtitle, color = TextMuted, fontSize = 11.5.sp, lineHeight = 15.sp)
                    }
                }

                // Receipt button
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                        .background(SurfaceSand).clickable { onShowReceipt() }
                        .padding(vertical = 11.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🧾", fontSize = 15.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "View Today's Attention Receipt",
                            color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp
                        )
                        Spacer(Modifier.weight(1f))
                        Text("→", color = AccentCaramel, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ── PLATFORMS ─────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Platforms", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("Auto-detect ⚡", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreenActive)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlatformTile("🎬", "Instagram", statsManager.getTodayPlatformCount(0), Modifier.weight(1f))
                PlatformTile("▶️", "Shorts", statsManager.getTodayPlatformCount(1), Modifier.weight(1f))
                PlatformTile("🎵", "TikTok", statsManager.getTodayPlatformCount(2), Modifier.weight(1f))
                PlatformTile("👻", "Snapchat", statsManager.getTodayPlatformCount(3), Modifier.weight(1f))
            }
        }

        // ── DAILY GOAL ────────────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                .background(SurfaceWhite).border(1.dp, BorderSand, RoundedCornerShape(22.dp)).padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Daily Reel Limit", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Intervention shield activates when you hit the limit.", color = TextMuted, fontSize = 11.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(20, 50, 100, 150).forEach { goal ->
                        val isSel = dailyGoal == goal
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) HeroDark else SurfaceSand)
                                .border(1.dp, if (isSel) HeroDark else BorderSand, RoundedCornerShape(12.dp))
                                .clickable { onGoalChange(goal) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "$goal",
                                    color = if (isSel) SurfaceWhite else TextPrimary,
                                    fontWeight = FontWeight.Bold, fontSize = 13.sp
                                )
                                Text(
                                    if (goal <= 20) "zen" else if (goal <= 50) "mild" else if (goal <= 100) "avg" else "max",
                                    color = if (isSel) AccentCaramel else TextMuted, fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── MINDFUL TIP ───────────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFE8F5E9), Color(0xFFF1F8E9))
                    )
                ).border(1.dp, Color(0xFFBBDDBB), RoundedCornerShape(18.dp)).padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text("🌿", fontSize = 20.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Mindful Moment", color = Color(0xFF2E7D32), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(3.dp))
                    Text(tip, color = Color(0xFF388E3C), fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
        }

        // ── PERMISSIONS SETUP (only shown if needed) ──────────────
        if (!hasOverlayPermission || !hasAccessibilityPermission) {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(SurfaceWhite).border(1.5.dp, AccentCaramel.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚙️", fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Finish Setup", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                    }
                    Text(
                        "Grant 2 permissions to enable auto-counting across all social apps.",
                        color = TextMuted, fontSize = 12.sp
                    )
                    if (!hasOverlayPermission) {
                        SetupButton("1. Allow Overlay Display", HeroDark, onRequestOverlay)
                    }
                    if (!hasAccessibilityPermission) {
                        SetupButton("2. Enable Auto-Count (Accessibility)", AccentCaramel, onRequestAccessibility)
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
fun SetupButton(label: String, bg: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = bg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

// =========================================================================
// SCREEN 2: ⏱️ FOCUS & POMODORO
// =========================================================================
@Composable
fun FocusScreen(statsManager: DailyStatsManager, onHealCompleted: () -> Unit) {
    val context = LocalContext.current
    var isTimerRunning by remember { mutableStateOf(statsManager.isPomodoroActive()) }
    var remainingSeconds by remember { mutableStateOf(statsManager.getPomodoroRemainingSeconds()) }
    var bedtimeEnabled by remember { mutableStateOf(statsManager.isBedtimeModeEnabled()) }
    var selectedMinutes by remember { mutableStateOf(25) }

    // Animated ring progress
    val totalSecs = remember(selectedMinutes) { selectedMinutes * 60 }
    val ringProgress by animateFloatAsState(
        if (isTimerRunning && remainingSeconds > 0) remainingSeconds.toFloat() / totalSecs.toFloat() else 0f,
        tween(800), label = "ring"
    )

    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds = statsManager.getPomodoroRemainingSeconds()
            if (remainingSeconds <= 0) {
                isTimerRunning = false
                val healed = statsManager.completePomodoroAndHeal()
                onHealCompleted()
                Toast.makeText(context, "🎉 Focus complete! +$healed% Brain Score healed!", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Focus & Healing", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        Text("Step away from the feed. Heal your brain score.", color = TextMuted, fontSize = 13.sp)

        // Pomodoro Hero Card
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                .background(HeroDark).padding(26.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    if (isTimerRunning) "DEEP FOCUS ACTIVE" else "FOCUS TIMER",
                    color = AccentCaramel, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp
                )

                // Ring Timer
                Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(Color.White.copy(alpha = 0.08f), -90f, 360f, false,
                            style = Stroke(10.dp.toPx(), cap = StrokeCap.Round))
                        if (isTimerRunning) {
                            drawArc(AccentCaramel, -90f, 360f * ringProgress, false,
                                style = Stroke(10.dp.toPx(), cap = StrokeCap.Round))
                        }
                    }
                    val displayMins = remainingSeconds / 60
                    val displaySecs = remainingSeconds % 60
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (isTimerRunning) String.format(Locale.US, "%02d:%02d", displayMins, displaySecs) else "00:00",
                            fontSize = 42.sp, fontWeight = FontWeight.Black, color = SurfaceWhite
                        )
                        Text(
                            if (isTimerRunning) "remaining" else "ready",
                            color = SurfaceWhite.copy(alpha = 0.4f), fontSize = 12.sp
                        )
                    }
                }

                // Duration Selector
                if (!isTimerRunning) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(10, 25, 45, 60).forEach { mins ->
                            val isSel = selectedMinutes == mins
                            Box(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) AccentCaramel else SurfaceWhite.copy(alpha = 0.08f))
                                    .clickable { selectedMinutes = mins }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${mins}m", color = SurfaceWhite, fontSize = 13.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }

                Text(
                    "+15% Brain Score when complete. No social media during session.",
                    color = SurfaceWhite.copy(alpha = 0.5f), fontSize = 11.sp, textAlign = TextAlign.Center
                )

                Button(
                    onClick = {
                        if (isTimerRunning) {
                            statsManager.cancelPomodoro(); isTimerRunning = false; remainingSeconds = 0
                        } else {
                            statsManager.startPomodoroFocus(selectedMinutes)
                            remainingSeconds = selectedMinutes * 60; isTimerRunning = true
                            Toast.makeText(context, "Focus started! Put the phone down 💪", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isTimerRunning) Color(0xFF7F1D1D) else AccentCaramel),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        if (isTimerRunning) "⏹  Cancel Session" else "▶  Start ${selectedMinutes}-Min Focus",
                        color = SurfaceWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp
                    )
                }
            }
        }

        // Bedtime Shield Card
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                .background(SurfaceWhite).border(1.dp, BorderSand, RoundedCornerShape(22.dp)).padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceSand),
                            contentAlignment = Alignment.Center
                        ) { Text("🌙", fontSize = 20.sp) }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Bedtime Shield", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Auto-blocks scrolling 10:30 PM – 7 AM", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                    Switch(
                        checked = bedtimeEnabled,
                        onCheckedChange = { bedtimeEnabled = it; statsManager.setBedtimeModeEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = SurfaceWhite, checkedTrackColor = HeroDark)
                    )
                }
                if (bedtimeEnabled) {
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFE8F5E9)).padding(10.dp)
                    ) {
                        Text("🛡️ Bedtime Shield is ON. Doomscrolling is blocked during sleep hours.", color = Color(0xFF2E7D32), fontSize = 11.5.sp)
                    }
                }
            }
        }

        // Focus tips
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(SurfaceSand).padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("How it works", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                listOf(
                    "🧘 Close all social apps and start the timer",
                    "📵 Keep your phone face-down during the session",
                    "🏆 Complete the session to earn +15% Brain Score",
                    "🔁 Repeat daily to rebuild your attention span"
                ).forEach {
                    Text(it, color = TextMuted, fontSize = 12.sp, lineHeight = 16.sp)
                }
            }
        }
    }
}

// =========================================================================
// SCREEN 3: 📊 INSIGHTS
// =========================================================================
@Composable
fun InsightsScreen(statsManager: DailyStatsManager) {
    val history = remember { statsManager.getLast7DaysScores() }
    val allTime = remember { statsManager.getAllTimeTotal() }
    val streak = remember { statsManager.getStreakDays() }
    val allTimeMins = (allTime * 30) / 60
    val avgReels = if (history.isNotEmpty()) history.sumOf { it.count } / history.size else 0

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Insights", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        Text("Your scroll pattern. Honest, real, unfiltered.", color = TextMuted, fontSize = 13.sp)

        // Overview Stats
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBlock("All-Time", "$allTime", "reels counted", Modifier.weight(1f))
            StatBlock("Avg / Day", "$avgReels", "reels per day", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val screenSecs = statsManager.getTodayScreenTimeSecs()
            val timeStr = if (screenSecs > 60) statsManager.formatScreenTime(screenSecs)
                          else statsManager.formatScreenTime(allTime * 20)
            StatBlock("Today's Time", timeStr, "in social apps", Modifier.weight(1f))
            StatBlock("Streak", "$streak days", "tracking days", Modifier.weight(1f))
        }

        // 7-Day Chart
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .background(SurfaceWhite).border(1.dp, BorderSand, RoundedCornerShape(24.dp)).padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("7-Day Brain Score", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                Text("Green = Zen Mind (75%+)  •  Red = Brainrot danger", color = TextMuted, fontSize = 11.sp)

                // Bar chart
                Row(
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                        .clip(RoundedCornerShape(14.dp)).background(SurfaceSand)
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    history.forEach { item ->
                        val fraction = (item.brainScore / 100f).coerceIn(0.08f, 1f)
                        val barColor = when {
                            item.brainScore >= 75 -> GreenLight
                            item.brainScore >= 40 -> AccentCaramel
                            else -> RedDanger
                        }
                        val animH by animateFloatAsState(fraction * 0.85f, tween(700), label = "bar")

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("${item.brainScore}%", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = barColor)
                            Spacer(Modifier.height(3.dp))
                            Box(
                                modifier = Modifier.width(18.dp).fillMaxHeight(animH)
                                    .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                    .background(barColor)
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                item.dayLabel, fontSize = 9.sp, color = TextPrimary,
                                fontWeight = if (item.dayLabel == "Today") FontWeight.Black else FontWeight.Normal
                            )
                        }
                    }
                }

                // Line chart of swipe counts
                Text("Daily Reel Count", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                val maxCount = (history.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)
                Canvas(
                    modifier = Modifier.fillMaxWidth().height(80.dp)
                        .clip(RoundedCornerShape(10.dp)).background(SurfaceSand).padding(horizontal = 8.dp)
                ) {
                    if (history.size >= 2) {
                        val points = history.mapIndexed { i, item ->
                            Offset(
                                x = (i.toFloat() / (history.size - 1)) * size.width,
                                y = size.height - (item.count.toFloat() / maxCount) * size.height * 0.85f - 8.dp.toPx()
                            )
                        }
                        // Draw line
                        val path = Path().apply {
                            moveTo(points[0].x, points[0].y)
                            for (i in 1 until points.size) {
                                val cp1x = (points[i-1].x + points[i].x) / 2
                                cubicTo(cp1x, points[i-1].y, cp1x, points[i].y, points[i].x, points[i].y)
                            }
                        }
                        drawPath(path, AccentCaramel, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                        // Dots
                        points.forEach { pt ->
                            drawCircle(AccentCaramel, 4.dp.toPx(), pt)
                            drawCircle(Color.White, 2.dp.toPx(), pt)
                        }
                    }
                }
            }
        }

        // Platform breakdown
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                .background(SurfaceWhite).border(1.dp, BorderSand, RoundedCornerShape(22.dp)).padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Today's Platform Breakdown", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                val platforms = listOf(
                    Triple("🎬 Instagram Reels", statsManager.getTodayPlatformCount(0), Color(0xFFE1306C)),
                    Triple("▶️ YouTube Shorts", statsManager.getTodayPlatformCount(1), Color(0xFFFF0000)),
                    Triple("🎵 TikTok", statsManager.getTodayPlatformCount(2), Color(0xFF69C9D0)),
                    Triple("👻 Snapchat", statsManager.getTodayPlatformCount(3), Color(0xFFFFD600)),
                )
                val total = platforms.sumOf { it.second }.coerceAtLeast(1)
                platforms.forEach { (name, count, color) ->
                    val frac = count.toFloat() / total
                    val animF by animateFloatAsState(frac, tween(600), label = "pf")
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text(name, color = TextPrimary, fontSize = 12.sp)
                            Text("$count reels", color = TextMuted, fontSize = 11.sp)
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth().height(5.dp)
                                .clip(RoundedCornerShape(50)).background(SurfaceSand)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth(animF).fillMaxHeight()
                                .clip(RoundedCornerShape(50)).background(color))
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// SCREEN 4: 👤 PROFILE
// =========================================================================
@Composable
fun ProfileScreen(
    isLoggedIn: Boolean,
    userName: String,
    userEmail: String,
    statsManager: DailyStatsManager,
    onOpenLogin: () -> Unit,
    onLogout: () -> Unit,
    onResetToday: () -> Unit
) {
    var haptic by remember { mutableStateOf(statsManager.isHapticEnabled()) }
    var shieldEnabled by remember { mutableStateOf(statsManager.isBrainrotShieldEnabled()) }
    val allTime = remember { statsManager.getAllTimeTotal() }
    val streak = remember { statsManager.getStreakDays() }
    val brainrot = remember { statsManager.getBrainrotStatus() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Profile", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)

        // Profile hero card
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                .background(HeroDark).padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(60.dp).clip(CircleShape)
                            .background(AccentCaramel.copy(alpha = 0.25f))
                            .border(2.dp, AccentCaramel, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (isLoggedIn) userName.take(1).uppercase() else "👤",
                            color = SurfaceWhite, fontWeight = FontWeight.Black, fontSize = 22.sp
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            if (isLoggedIn) userName else "Guest User",
                            color = SurfaceWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp
                        )
                        Text(
                            if (isLoggedIn) userEmail else "Register to track across devices",
                            color = SurfaceWhite.copy(alpha = 0.5f), fontSize = 12.sp
                        )
                    }
                }

                // Stats row
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MiniStat("$allTime", "Total Reels", Modifier.weight(1f))
                    MiniStat("$streak", "Day Streak", Modifier.weight(1f))
                    MiniStat("${brainrot.healthPercent}%", "Brain Score", Modifier.weight(1f))
                }

                if (!isLoggedIn) {
                    Button(
                        onClick = onOpenLogin,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCaramel),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Register / Sign In with Email", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(SurfaceWhite.copy(alpha = 0.08f))
                            .clickable { onLogout() }.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Sign Out", color = RedDanger.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Settings card
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                .background(SurfaceWhite).border(1.dp, BorderSand, RoundedCornerShape(22.dp)).padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Intervention Settings", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)

                SettingRow(
                    icon = "🛡️",
                    title = "Brainrot Shield",
                    subtitle = "Show exit wall when daily limit is hit",
                    checked = shieldEnabled,
                    onToggle = { shieldEnabled = it; statsManager.setBrainrotShieldEnabled(it) }
                )

                HorizontalDivider(color = BorderSand)

                SettingRow(
                    icon = "📳",
                    title = "Haptic Feedback",
                    subtitle = "Gentle vibration on each swipe detected",
                    checked = haptic,
                    onToggle = { haptic = it; statsManager.setHapticEnabled(it) }
                )
            }
        }

        // Data & Privacy card
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                .background(SurfaceWhite).border(1.dp, BorderSand, RoundedCornerShape(22.dp)).padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Data & Privacy", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                Row(verticalAlignment = Alignment.Top) {
                    Text("🔒", fontSize = 18.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "All data is stored 100% on your device. Scrolle never sends or sells your usage data. Zero cloud cost. Zero tracking.",
                        color = TextMuted, fontSize = 12.sp, lineHeight = 17.sp
                    )
                }
                HorizontalDivider(color = BorderSand)

                // Reset today
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFF1F2)).border(1.dp, Color(0xFFFFCDD2), RoundedCornerShape(12.dp))
                        .clickable { onResetToday() }.padding(13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("↺  Reset Today's Count to 0", color = RedDanger, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // App version
        Text(
            "scrolle v1.2 • By Abhay • 100% private",
            color = TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
fun SettingRow(icon: String, title: String, subtitle: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(SurfaceSand),
                contentAlignment = Alignment.Center
            ) { Text(icon, fontSize = 18.sp) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = TextMuted, fontSize = 11.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = SurfaceWhite, checkedTrackColor = HeroDark)
        )
    }
}

@Composable
fun MiniStat(value: String, label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(12.dp))
            .background(SurfaceWhite.copy(alpha = 0.08f)).padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = SurfaceWhite, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Text(label, color = SurfaceWhite.copy(alpha = 0.5f), fontSize = 10.sp, textAlign = TextAlign.Center)
        }
    }
}

// =========================================================================
// 🧾 SCREEN TIME RECEIPT DIALOG
// =========================================================================
@Composable
fun ScreenTimeReceiptDialog(
    receipt: DailyStatsManager.ScreenTimeReceipt,
    onDismiss: () -> Unit,
    onCloseApp: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFFFFDF8))
                .border(1.5.dp, Color(0xFFD4C0A8), RoundedCornerShape(24.dp))
                .padding(22.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("🧾 ATTENTION RECEIPT", fontWeight = FontWeight.Black, fontSize = 15.sp,
                    color = Color(0xFF1E1E1E), letterSpacing = 0.5.sp)
                Text("OFFICIAL SCREEN TIME STATEMENT", fontSize = 9.sp,
                    color = Color(0xFF7A756F), letterSpacing = 1.5.sp)

                HorizontalDivider(color = Color(0xFFD4C0A8), thickness = 0.5.dp)

                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("REF: ${receipt.receiptNumber}", fontSize = 11.sp, color = Color(0xFF555555))
                    Text(receipt.dateTime, fontSize = 11.sp, color = Color(0xFF555555))
                }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("APP TARGET:", fontSize = 11.sp, color = Color(0xFF555555), fontWeight = FontWeight.Bold)
                    Text(receipt.primaryApp, fontSize = 11.sp, color = Color(0xFF1E1E1E), fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = Color(0xFFD4C0A8), thickness = 0.5.dp)

                ReceiptRow("Reels Watched", "${receipt.reelsScrolled}", "today")
                ReceiptRow("Time Consumed", "~${receipt.minutesWasted} min", "irreversible")
                ReceiptRow("Dopamine Hits", "${receipt.dopamineHits}", "artificial stimuli")
                ReceiptRow("Brain Health", "${receipt.brainScore}%", if (receipt.brainScore < 50) "⚠️ critical" else "⚠️ warning")

                HorizontalDivider(color = Color(0xFFD4C0A8), thickness = 0.5.dp)

                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("TOTAL COST:", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color(0xFF1E1E1E))
                    Text(receipt.attentionCost, fontWeight = FontWeight.Black, fontSize = 11.sp, color = RedDanger)
                }

                Text(
                    "\"Every swipe is 30 seconds of your life you can never get back.\"",
                    fontSize = 10.5.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = Color(0xFF666666), textAlign = TextAlign.Center
                )

                Button(
                    onClick = onCloseApp,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = HeroDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("🌿  Sign & Close App — Save My Brain", color = SurfaceWhite,
                        fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }

                Text("Continue Scrolling", color = Color(0xFF999999), fontSize = 11.sp,
                    modifier = Modifier.clickable { onDismiss() })
            }
        }
    }
}

@Composable
fun ReceiptRow(label: String, qty: String, note: String) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 11.5.sp, color = Color(0xFF222222), fontWeight = FontWeight.Medium)
            Text(note, fontSize = 10.sp, color = Color(0xFF888888))
        }
        Text(qty, fontSize = 12.sp, color = Color(0xFF1E1E1E), fontWeight = FontWeight.Bold)
    }
}

// =========================================================================
// REUSABLE COMPONENTS
// =========================================================================
@Composable
fun PlatformTile(icon: String, name: String, count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(16.dp))
            .background(SurfaceWhite).border(1.dp, BorderSand, RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(icon, fontSize = 20.sp)
            Text(name, color = TextMuted, fontSize = 10.sp, maxLines = 1)
            AnimatedContent(targetState = count, transitionSpec = { slideInVertically { -it } togetherWith slideOutVertically { it } }, label = "pc") { c ->
                Text("$c", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun StatBlock(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(18.dp))
            .background(SurfaceWhite).border(1.dp, BorderSand, RoundedCornerShape(18.dp)).padding(14.dp)
    ) {
        Column {
            Text(title, color = TextMuted, fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = AccentCaramel, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// =========================================================================
// EMAIL LOGIN DIALOG
// =========================================================================
@Composable
fun EmailLoginDialog(onDismiss: () -> Unit, onLoginSuccess: (email: String, name: String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                .background(SurfaceWhite).border(1.dp, BorderSand, RoundedCornerShape(28.dp)).padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("🧠", fontSize = 32.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text("Sign In / Register", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
                Text(
                    "Save your scroll history & Brain Score. Stored privately on your device.",
                    color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 16.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Your Name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                )
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("Email Address") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                )

                Button(
                    onClick = {
                        if (email.contains("@") && email.contains(".")) {
                            onLoginSuccess(email, name)
                        } else {
                            Toast.makeText(context, "Enter a valid email address", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = HeroDark),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Text("Save Profile ✓", color = SurfaceWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Text("Cancel", color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clickable { onDismiss() })
            }
        }
    }
}

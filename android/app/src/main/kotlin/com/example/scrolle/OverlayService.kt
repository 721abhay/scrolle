package com.example.scrolle

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.os.Vibrator
import android.os.VibrationEffect
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.animation.OvershootInterpolator
import android.graphics.drawable.GradientDrawable
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.widget.LinearLayout

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var countText: TextView
    private lateinit var platformEmoji: TextView
    private lateinit var brainMascot: TextView
    private lateinit var liveDot: View
    private lateinit var statusLabel: TextView
    private lateinit var statsManager: DailyStatsManager
    private var shieldView: View? = null

    private var count = 0
    private var activePlatform = 0 // 0=Reels, 1=Shorts, 2=TikTok, 3=Snapchat, 4=Facebook, 5=Other
    private val platforms = arrayOf("🎬", "🔴", "🎵", "👻", "📘", "📱")
    private val platformColors = arrayOf(
        Color.parseColor("#E1306C"), // Instagram Reels
        Color.parseColor("#FF0000"), // YouTube Shorts
        Color.parseColor("#69C9D0"), // TikTok
        Color.parseColor("#FFFC00"), // Snapchat Spotlight
        Color.parseColor("#1877F2"), // Facebook Reels
        Color.parseColor("#833AB4")  // Other
    )

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    var isPaused = false

    companion object {
        const val CHANNEL_ID = "scrolle_overlay"
        const val NOTIF_ID = 1001
        const val ACTION_PAUSE = "com.scrolle.PAUSE"
        const val ACTION_RESUME = "com.scrolle.RESUME"
        const val ACTION_STOP = "com.scrolle.STOP"

        @Volatile
        var instance: OverlayService? = null

        fun onReelScrolled(platformIndex: Int) {
            Handler(Looper.getMainLooper()).post {
                instance?.let {
                    if (!it.isPaused) {
                        it.setPlatform(platformIndex)
                        it.increment()
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        statsManager = DailyStatsManager(this)
        count = statsManager.getTodayTotal()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIF_ID,
                buildNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIF_ID, buildNotification())
        }
        createOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> {
                isPaused = true
                setOverlayVisible(false)
                dismissBrainrotShield()
                updateNotification("⏸ Paused for UPI & Banking")
            }
            ACTION_RESUME -> {
                isPaused = false
                setOverlayVisible(true)
                updateNotification("Scrolle Active: $count reels today")
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }
        return START_STICKY
    }

    fun setOverlayVisible(visible: Boolean) {
        if (::overlayView.isInitialized) {
            Handler(Looper.getMainLooper()).post {
                overlayView.visibility = if (visible && !isPaused) View.VISIBLE else View.GONE
            }
        }
    }

    fun setPlatform(index: Int) {
        if (index in platforms.indices && activePlatform != index) {
            activePlatform = index
            if (::platformEmoji.isInitialized) {
                platformEmoji.text = platforms[activePlatform]
            }
            if (::liveDot.isInitialized) {
                val dotBg = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(platformColors[activePlatform])
                }
                liveDot.background = dotBg
            }
        }
    }

    private fun createOverlay() {
        val pill = buildPillView()
        overlayView = pill

        val (savedX, savedY) = statsManager.getSavedPosition()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = savedX
            y = savedY
        }

        overlayView.alpha = statsManager.getPillOpacity()
        windowManager.addView(overlayView, params)

        overlayView.setOnTouchListener { v, event ->
            val lp = v.layoutParams as WindowManager.LayoutParams
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = lp.x
                    initialY = lp.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) isDragging = true
                    if (isDragging) {
                        lp.x = initialX + dx.toInt()
                        lp.y = initialY + dy.toInt()
                        windowManager.updateViewLayout(v, lp)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        statsManager.savePosition(lp.x, lp.y)
                    } else {
                        // Tapped pill: PURE AUTO-COUNT ONLY (show gentle feedback)
                        showTapFeedback()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun showTapFeedback() {
        if (::overlayView.isInitialized) {
            val scaleX = ObjectAnimator.ofFloat(overlayView, "scaleX", 1f, 1.08f, 1f)
            val scaleY = ObjectAnimator.ofFloat(overlayView, "scaleY", 1f, 1.08f, 1f)
            AnimatorSet().apply {
                playTogether(scaleX, scaleY)
                duration = 200
                interpolator = OvershootInterpolator(2f)
                start()
            }
        }
        if (::statusLabel.isInitialized) {
            val brainrot = statsManager.getBrainrotStatus()
            statusLabel.text = "${brainrot.healthPercent}%"
            Handler(Looper.getMainLooper()).postDelayed({
                if (::statusLabel.isInitialized) statusLabel.text = "today"
            }, 1200)
        }
    }

    private fun triggerHaptic(strong: Boolean = false) {
        try {
            if (!statsManager.isHapticEnabled()) return
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val duration = if (strong) 90L else 28L
                vibrator?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(if (strong) 90L else 28L)
            }
        } catch (_: Exception) {}
    }

    private fun buildPillView(): View {
        val ctx = this

        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 0)
        }

        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 100f
            setColor(Color.parseColor("#E61E1D1B")) // Warm charcoal espresso
            setStroke(2, Color.parseColor("#44B88E72")) // Subtle caramel border
        }
        container.background = bg

        val dp = ctx.resources.displayMetrics.density

        // Brainrot Avatar Mascot on the pill!
        val brain = TextView(ctx).apply {
            text = statsManager.getBrainrotStatus().emoji
            textSize = 14f
            setPadding((12 * dp).toInt(), (8 * dp).toInt(), (2 * dp).toInt(), (8 * dp).toInt())
        }
        brainMascot = brain
        container.addView(brain)

        // Platform emoji
        val emoji = TextView(ctx).apply {
            text = platforms[activePlatform]
            textSize = 15f
            setPadding((4 * dp).toInt(), (8 * dp).toInt(), 0, (8 * dp).toInt())
        }
        platformEmoji = emoji
        container.addView(emoji)

        // Divider
        val div1 = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                (1 * dp).toInt(), (18 * dp).toInt()
            ).apply {
                marginStart = (8 * dp).toInt()
                marginEnd = 0
            }
            setBackgroundColor(Color.parseColor("#33FFFFFF"))
        }
        container.addView(div1)

        // Today's Count number
        val cnt = TextView(ctx).apply {
            text = "$count"
            textSize = 18f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding((10 * dp).toInt(), (8 * dp).toInt(), (4 * dp).toInt(), (8 * dp).toInt())
        }
        countText = cnt
        container.addView(cnt)

        // Daily label
        val label = TextView(ctx).apply {
            text = "today"
            textSize = 11f
            setTextColor(Color.parseColor("#99FFFFFF"))
            setPadding(0, (8 * dp).toInt(), 0, (8 * dp).toInt())
        }
        statusLabel = label
        container.addView(label)

        // Divider 2
        val div2 = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                (1 * dp).toInt(), (18 * dp).toInt()
            ).apply {
                marginStart = (8 * dp).toInt()
            }
            setBackgroundColor(Color.parseColor("#33FFFFFF"))
        }
        container.addView(div2)

        // Close button (Instantly removes overlay)
        val close = TextView(ctx).apply {
            text = "✕"
            textSize = 12f
            setTextColor(Color.parseColor("#80FFFFFF"))
            setPadding((8 * dp).toInt(), (8 * dp).toInt(), (14 * dp).toInt(), (8 * dp).toInt())
            setOnClickListener { stopSelf() }
        }
        container.addView(close)

        return container
    }

    /**
     * Pure Auto-Count Increment with Brainrot Screen Time Control & Real Intervention Shield
     */
    fun increment() {
        if (isPaused) return

        count = statsManager.incrementToday(activePlatform)

        if (::countText.isInitialized) {
            countText.text = "$count"
        }

        val brainrot = statsManager.getBrainrotStatus()
        val isTotalBrainrot = brainrot.stageLevel >= 5 // 0% health / limit reached!
        val detoxMins = statsManager.getDetoxRemainingMinutes()

        // Update brain mascot immediately
        if (::brainMascot.isInitialized) {
            brainMascot.text = brainrot.emoji
        }

        // Haptic feedback (stronger double vibration if brainrot limit hit)
        triggerHaptic(strong = isTotalBrainrot || detoxMins > 0)

        // Bouncy pop animation on the whole pill
        if (::overlayView.isInitialized && overlayView.visibility == View.VISIBLE) {
            val scaleX = ObjectAnimator.ofFloat(overlayView, "scaleX", 1f, 1.25f, 1f)
            val scaleY = ObjectAnimator.ofFloat(overlayView, "scaleY", 1f, 1.25f, 1f)
            AnimatorSet().apply {
                playTogether(scaleX, scaleY)
                duration = 260
                interpolator = OvershootInterpolator(3.5f)
                start()
            }

            // Real Brainrot Intervention Shield!
            if ((isTotalBrainrot || detoxMins > 0) && statsManager.isBrainrotShieldEnabled()) {
                showBrainrotShield(isTotalBrainrot, detoxMins)
            }
        }

        updateNotification("Scrolle: $count reels • ${brainrot.emoji} ${brainrot.title} (${brainrot.healthPercent}%)")
    }

    /**
     * THE BRAINROT SHIELD (THE WALL): Full-Screen Intervention Dialog
     * Directly stops doomscrolling and allows 1-tap exit to home screen!
     */
    fun showBrainrotShield(isBrainrot: Boolean, detoxMins: Int) {
        if (shieldView != null) return

        val ctx = this
        val dp = ctx.resources.displayMetrics.density

        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding((22 * dp).toInt(), (22 * dp).toInt(), (22 * dp).toInt(), (20 * dp).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 28 * dp
                setColor(Color.parseColor("#FA1E1C1A")) // Rich Charcoal
                setStroke((2 * dp).toInt(), Color.parseColor("#B88E72")) // Caramel Accent
            }
        }

        // Brainrot Avatar Icon
        val icon = TextView(ctx).apply {
            text = if (isBrainrot) "💀" else "⏳"
            textSize = 50f
            gravity = Gravity.CENTER
        }
        layout.addView(icon)

        // Headline
        val title = TextView(ctx).apply {
            text = if (isBrainrot) "BRAINROT LIMIT REACHED" else "DETOX FOCUS ACTIVE"
            textSize = 16f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, (8 * dp).toInt(), 0, (4 * dp).toInt())
        }
        layout.addView(title)

        // Subtitle message
        val msg = TextView(ctx).apply {
            text = if (isBrainrot)
                "You've scrolled $count reels today. Brain Health is at 0%. Stop doomscrolling and save your focus!"
            else
                "You are in a 20-min Detox Focus session ($detoxMins mins remaining). Put your phone down!"
            textSize = 12.5f
            setTextColor(Color.parseColor("#B3FFFFFF"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, (16 * dp).toInt())
        }
        layout.addView(msg)

        // Button 1: Close App & Save My Brain (Takes user to phone home screen!)
        val closeBtn = TextView(ctx).apply {
            text = "🌿  Close App & Save My Brain"
            textSize = 13.5f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 14 * dp
                setColor(Color.parseColor("#B88E72")) // Caramel
            }
            setPadding((16 * dp).toInt(), (12 * dp).toInt(), (16 * dp).toInt(), (12 * dp).toInt())
            setOnClickListener {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                ctx.startActivity(homeIntent)
                dismissBrainrotShield()
            }
        }
        layout.addView(closeBtn)

        // Button 2: Emergency Pass (+5 More Reels)
        val passBtn = TextView(ctx).apply {
            text = "⏳  +5 More Reels (Emergency Pass)"
            textSize = 12f
            setTextColor(Color.parseColor("#B88E72"))
            gravity = Gravity.CENTER
            setPadding(0, (12 * dp).toInt(), 0, (4 * dp).toInt())
            setOnClickListener {
                statsManager.extendEmergencyPass(5)
                dismissBrainrotShield()
            }
        }
        layout.addView(passBtn)

        // WindowManager Params
        val params = WindowManager.LayoutParams(
            (300 * dp).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        shieldView = layout
        windowManager.addView(layout, params)
    }

    fun dismissBrainrotShield() {
        shieldView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            shieldView = null
        }
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Scrolle Floating Counter",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live count & Brainrot health status"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String = "Scrolle Active: $count reels today"): android.app.Notification {
        val appIntent = Intent(this, MainActivity::class.java)
        val appPendingIntent = PendingIntent.getActivity(
            this, 0, appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = Intent(this, OverlayService::class.java).apply { action = ACTION_PAUSE }
        val pausePendingIntent = PendingIntent.getService(
            this, 1, pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val resumeIntent = Intent(this, OverlayService::class.java).apply { action = ACTION_RESUME }
        val resumePendingIntent = PendingIntent.getService(
            this, 2, resumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, OverlayService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 3, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("scrolle 📱 • Brainrot Control")
            .setContentText(text)
            .setContentIntent(appPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (isPaused) {
            builder.addAction(android.R.drawable.ic_media_play, "▶ Resume Counter", resumePendingIntent)
        } else {
            builder.addAction(android.R.drawable.ic_media_pause, "⏸ Pause for UPI", pausePendingIntent)
        }

        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "✕ Stop", stopPendingIntent)

        return builder.build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID, buildNotification(text))
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        dismissBrainrotShield()
        if (::overlayView.isInitialized) {
            try {
                windowManager.removeView(overlayView)
            } catch (_: Exception) {}
        }
    }
}

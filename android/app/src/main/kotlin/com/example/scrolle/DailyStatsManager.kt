package com.example.scrolle

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DailyStatsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("scrolle_daily_stats", Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val receiptDateFormat = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.US)

    fun getTodayKey(): String = dateFormat.format(Date())

    fun getTodayTotal(): Int {
        return prefs.getInt("${getTodayKey()}_total", 0)
    }

    fun getTodayPlatformCount(platformIndex: Int): Int {
        return prefs.getInt("${getTodayKey()}_p$platformIndex", 0)
    }

    fun incrementToday(platformIndex: Int): Int {
        val today = getTodayKey()
        val currentTotal = prefs.getInt("${today}_total", 0) + 1
        val platformCount = prefs.getInt("${today}_p$platformIndex", 0) + 1
        val allTimeTotal = prefs.getInt("all_time_total", 0) + 1

        prefs.edit()
            .putInt("${today}_total", currentTotal)
            .putInt("${today}_p$platformIndex", platformCount)
            .putInt("all_time_total", allTimeTotal)
            .apply()

        checkAndUpdateStreak()

        return currentTotal
    }

    // ==========================================
    // ⏱️ REAL SCREEN TIME TRACKING (Actual seconds in social apps)
    // ==========================================

    /** Call this when the user enters a monitored app. */
    fun onAppSessionStart() {
        prefs.edit().putLong("session_start_ms", System.currentTimeMillis()).apply()
    }

    /** Call this when the user leaves the monitored app. Adds elapsed seconds to today's total. */
    fun onAppSessionEnd() {
        val start = prefs.getLong("session_start_ms", 0L)
        if (start == 0L) return
        val elapsed = ((System.currentTimeMillis() - start) / 1000L).toInt()
        if (elapsed in 1..7200) { // sanity-check: between 1s and 2 hours
            val today = getTodayKey()
            val current = prefs.getInt("${today}_screen_secs", 0)
            prefs.edit()
                .putInt("${today}_screen_secs", current + elapsed)
                .putLong("session_start_ms", 0L)
                .apply()
        } else {
            prefs.edit().putLong("session_start_ms", 0L).apply()
        }
    }

    /** Returns today's real screen time in whole minutes (actual time inside social apps). */
    fun getTodayScreenTimeMinutes(): Int {
        val today = getTodayKey()
        val storedSecs = prefs.getInt("${today}_screen_secs", 0)
        // If a session is currently open, include live elapsed time too
        val start = prefs.getLong("session_start_ms", 0L)
        val liveSecs = if (start > 0L) {
            val e = ((System.currentTimeMillis() - start) / 1000L).toInt()
            if (e in 1..7200) e else 0
        } else 0
        return (storedSecs + liveSecs) / 60
    }

    /** Returns today's real screen time in seconds for display. */
    fun getTodayScreenTimeSecs(): Int {
        val today = getTodayKey()
        val storedSecs = prefs.getInt("${today}_screen_secs", 0)
        val start = prefs.getLong("session_start_ms", 0L)
        val liveSecs = if (start > 0L) {
            val e = ((System.currentTimeMillis() - start) / 1000L).toInt()
            if (e in 1..7200) e else 0
        } else 0
        return storedSecs + liveSecs
    }

    /** Formats screen time as a readable string like "3 min" or "1h 12m". */
    fun formatScreenTime(totalSecs: Int): String {
        val mins = totalSecs / 60
        return when {
            mins < 1 -> "${totalSecs}s"
            mins < 60 -> "$mins min"
            else -> "${mins / 60}h ${mins % 60}m"
        }
    }

    // ==========================================
    // 🧠 BRAIN SCORE & DECAYING MASCOT ENGINE (thebrainrotapp.com)
    // ==========================================
    fun getBrainHealth(): Int {
        val total = getTodayTotal()
        val goal = getDailyGoal()
        if (goal <= 0) return 100
        val baseScore = 100 - ((total.toFloat() / goal.toFloat()) * 100).toInt()
        val bonus = prefs.getInt("${getTodayKey()}_focus_heal", 0)
        return (baseScore + bonus).coerceIn(0, 100)
    }

    data class BrainrotStatus(
        val emoji: String,
        val title: String,
        val subtitle: String,
        val healthPercent: Int,
        val stageLevel: Int // 0=Zen, 1=Healthy, 2=Mild, 3=Rotting, 4=Zombie, 5=Fried
    )

    fun getBrainrotStatus(): BrainrotStatus {
        val health = getBrainHealth()
        val total = getTodayTotal()
        return when {
            total == 0 -> BrainrotStatus("🧠✨", "Galaxy Brain", "0 reels scrolled. Laser focused and sharp!", 100, 0)
            health >= 80 -> BrainrotStatus("🧠", "Sharp Mind", "Light mindful usage. Brain is healthy and alert.", health, 1)
            health >= 60 -> BrainrotStatus("😐", "Mild Fatigue", "Dopamine levels climbing. Time to blink and stretch.", health, 2)
            health >= 35 -> BrainrotStatus("😵‍💫", "Brainrot Setting In", "Short attention span detected. Your focus is melting!", health, 3)
            health > 0 -> BrainrotStatus("🧟", "Zombie Doomscroll", "Heavy brainrot overload! Put your phone down.", health, 4)
            else -> BrainrotStatus("💀", "Total Brainrot", "Daily limit reached! Brain needs emergency detox.", 0, 5)
        }
    }

    // ==========================================
    // 🧾 SCREEN TIME RECEIPTS (thebrainrotapp.com signature feature)
    // ==========================================
    data class ScreenTimeReceipt(
        val receiptNumber: String,
        val dateTime: String,
        val reelsScrolled: Int,
        val minutesWasted: Int,
        val dopamineHits: Int,
        val brainScore: Int,
        val primaryApp: String,
        val attentionCost: String
    )

    fun generateReceipt(): ScreenTimeReceipt {
        val reels = getTodayTotal()
        val totalSecs = getTodayScreenTimeSecs()
        val mins = if (totalSecs > 60) totalSecs / 60 else (reels * 20) / 60 // fallback estimate if no session tracked
        val dopamine = reels * 14
        val brainScore = getBrainHealth()
        val p0 = getTodayPlatformCount(0)
        val p1 = getTodayPlatformCount(1)
        val p2 = getTodayPlatformCount(2)
        val p3 = getTodayPlatformCount(3)
        val maxApp = when {
            p0 >= p1 && p0 >= p2 && p0 >= p3 && p0 > 0 -> "Instagram Reels"
            p1 >= p0 && p1 >= p2 && p1 >= p3 && p1 > 0 -> "YouTube Shorts"
            p3 >= p0 && p3 >= p1 && p3 >= p2 && p3 > 0 -> "Snapchat Spotlight"
            p2 >= p0 && p2 >= p1 && p2 >= p3 && p2 > 0 -> "TikTok"
            else -> "Social Media"
        }

        return ScreenTimeReceipt(
            receiptNumber = "#BR-" + (1000..9999).random(),
            dateTime = receiptDateFormat.format(Date()),
            reelsScrolled = reels,
            minutesWasted = mins,
            dopamineHits = dopamine,
            brainScore = brainScore,
            primaryApp = maxApp,
            attentionCost = "$mins MINS OF CONSCIOUS LIFE"
        )
    }

    // ==========================================
    // ⏱️ DEEP WORK POMODORO & HEALING FOCUS TIMER
    // ==========================================
    fun startPomodoroFocus(minutes: Int = 25) {
        val endTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
        prefs.edit()
            .putLong("pomodoro_end_time", endTime)
            .putInt("pomodoro_initial_mins", minutes)
            .apply()
    }

    fun getPomodoroRemainingSeconds(): Int {
        val endTime = prefs.getLong("pomodoro_end_time", 0L)
        val diff = endTime - System.currentTimeMillis()
        return if (diff > 0) (diff / 1000L).toInt() else 0
    }

    fun isPomodoroActive(): Boolean = getPomodoroRemainingSeconds() > 0

    fun getDetoxRemainingMinutes(): Int {
        val secs = getPomodoroRemainingSeconds()
        return if (secs > 0) (secs + 59) / 60 else 0
    }

    fun cancelPomodoro() {
        prefs.edit().remove("pomodoro_end_time").apply()
    }

    fun completePomodoroAndHeal(): Int {
        val bonus = 15 // Heals +15% Brain Score for 25 mins of deep work!
        val today = getTodayKey()
        val currentBonus = prefs.getInt("${today}_focus_heal", 0) + bonus
        prefs.edit()
            .putInt("${today}_focus_heal", currentBonus)
            .remove("pomodoro_end_time")
            .apply()
        return bonus
    }

    // ==========================================
    // 🌙 BEDTIME BRAINROT SHIELD
    // ==========================================
    fun isBedtimeModeEnabled(): Boolean {
        return prefs.getBoolean("bedtime_mode_enabled", false)
    }

    fun setBedtimeModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("bedtime_mode_enabled", enabled).apply()
    }

    fun isInBedtimeWindow(): Boolean {
        if (!isBedtimeModeEnabled()) return false
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        // Default bedtime window: 10:30 PM (22) to 7:00 AM (7)
        return hour >= 22 || hour < 7
    }

    // ==========================================
    // 🚫 APP BLOCKING & HARD RESTRICTIONS
    // ==========================================
    fun isAppProtectionEnabled(pkgPrefix: String): Boolean {
        return prefs.getBoolean("protect_$pkgPrefix", true)
    }

    fun setAppProtectionEnabled(pkgPrefix: String, enabled: Boolean) {
        prefs.edit().putBoolean("protect_$pkgPrefix", enabled).apply()
    }

    fun isBrainrotShieldEnabled(): Boolean {
        return prefs.getBoolean("brainrot_shield", true)
    }

    fun setBrainrotShieldEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("brainrot_shield", enabled).apply()
    }

    fun extendEmergencyPass(additionalReels: Int = 5) {
        val currentGoal = getDailyGoal()
        setDailyGoal(currentGoal + additionalReels)
    }

    // ==========================================
    // USER ACCOUNT & EMAIL REGISTRATION
    // ==========================================
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("user_logged_in", false)
    }

    fun getUserEmail(): String {
        return prefs.getString("user_email", "") ?: ""
    }

    fun getUserName(): String {
        return prefs.getString("user_name", "Scroller") ?: "Scroller"
    }

    fun login(email: String, name: String) {
        prefs.edit()
            .putBoolean("user_logged_in", true)
            .putString("user_email", email.trim())
            .putString("user_name", if (name.isBlank()) email.substringBefore("@").replaceFirstChar { it.uppercase() } else name.trim())
            .apply()
    }

    fun logout() {
        prefs.edit()
            .putBoolean("user_logged_in", false)
            .apply()
    }

    // ==========================================
    // STREAKS & 7-DAY BRAIN SCORE HISTORY
    // ==========================================
    fun getStreakDays(): Int {
        return prefs.getInt("streak_days", 1)
    }

    private fun checkAndUpdateStreak() {
        val today = getTodayKey()
        val lastActiveDate = prefs.getString("last_active_date", "")

        if (lastActiveDate != today) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val yesterday = dateFormat.format(cal.time)

            val currentStreak = if (lastActiveDate == yesterday) {
                prefs.getInt("streak_days", 0) + 1
            } else if (lastActiveDate.isNullOrEmpty()) {
                1
            } else {
                1
            }

            prefs.edit()
                .putString("last_active_date", today)
                .putInt("streak_days", currentStreak)
                .apply()
        }
    }

    fun getDailyGoal(): Int {
        return prefs.getInt("daily_goal", 100)
    }

    fun setDailyGoal(goal: Int) {
        prefs.edit().putInt("daily_goal", goal).apply()
    }

    fun isHapticEnabled(): Boolean {
        return prefs.getBoolean("haptic_feedback", true)
    }

    fun setHapticEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("haptic_feedback", enabled).apply()
    }

    fun getBreakReminderInterval(): Int {
        return prefs.getInt("break_interval", 50)
    }

    fun setBreakReminderInterval(interval: Int) {
        prefs.edit().putInt("break_interval", interval).apply()
    }

    fun getPillOpacity(): Float {
        return prefs.getFloat("pill_opacity", 0.92f)
    }

    fun setPillOpacity(opacity: Float) {
        prefs.edit().putFloat("pill_opacity", opacity).apply()
    }

    fun getSavedPosition(): Pair<Int, Int> {
        val x = prefs.getInt("pill_pos_x", 0)
        val y = prefs.getInt("pill_pos_y", 70)
        return Pair(x, y)
    }

    fun savePosition(x: Int, y: Int) {
        prefs.edit().putInt("pill_pos_x", x).putInt("pill_pos_y", y).apply()
    }

    fun getAllTimeTotal(): Int {
        return prefs.getInt("all_time_total", 0)
    }

    /**
     * Returns last 7 days of counts and calculated Brain Score for that day.
     * e.g. [("Mon", count=25, score=85), ("Tue", count=120, score=0), ...]
     */
    data class DayScoreHistory(val dayLabel: String, val count: Int, val brainScore: Int)

    fun getLast7DaysScores(): List<DayScoreHistory> {
        val list = mutableListOf<DayScoreHistory>()
        val dayFormat = SimpleDateFormat("EEE", Locale.US)
        val goal = getDailyGoal()

        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val key = dateFormat.format(cal.time)
            val label = if (i == 0) "Today" else dayFormat.format(cal.time)
            val count = prefs.getInt("${key}_total", 0)
            val score = if (i == 0) getBrainHealth() else {
                if (goal <= 0) 100 else (100 - ((count.toFloat() / goal.toFloat()) * 100).toInt()).coerceIn(0, 100)
            }
            list.add(DayScoreHistory(label, count, score))
        }
        return list
    }

    fun resetToday() {
        val today = getTodayKey()
        prefs.edit()
            .putInt("${today}_total", 0)
            .putInt("${today}_p0", 0)
            .putInt("${today}_p1", 0)
            .putInt("${today}_p2", 0)
            .putInt("${today}_p3", 0)
            .putInt("${today}_p4", 0)
            .putInt("${today}_p5", 0)
            .putInt("${today}_focus_heal", 0)
            .putInt("${today}_screen_secs", 0)
            .putLong("session_start_ms", 0L)
            .apply()
    }
}

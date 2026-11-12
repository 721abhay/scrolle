package com.example.scrolle

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

class ReelAccessibilityService : AccessibilityService() {

    private var lastScrollTimestamp = 0L
    private val DEBOUNCE_MILLIS = 1000L
    private var lastPackage = ""
    private var lastVideoSignature = ""
    private lateinit var statsManager: DailyStatsManager

    // Packages we monitor for screen time & reel counting
    private val monitoredPrefixes = listOf(
        "instagram", "youtube", "musically", "tiktok", "trill", "aweme", "snapchat", "facebook"
    )

    companion object {
        const val TAG = "ReelAccessibility"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        statsManager = DailyStatsManager(applicationContext)
    }

    private fun isMonitoredApp(pkg: String): Boolean {
        val p = pkg.lowercase()
        return monitoredPrefixes.any { p.contains(it) }
    }

    private fun resolvePlatform(pkg: String): Int {
        val p = pkg.lowercase()
        return when {
            p.contains("instagram") -> 0
            p.contains("youtube") -> 1
            p.contains("musically") || p.contains("tiktok") || p.contains("trill") || p.contains("aweme") -> 2
            p.contains("snapchat") -> 3
            p.contains("facebook") -> 4
            else -> 5
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!::statsManager.isInitialized) statsManager = DailyStatsManager(applicationContext)

        val pkg = event.packageName?.toString() ?: return
        val platformIndex = resolvePlatform(pkg)

        // Window state changed: track app enter/exit for real screen time
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val entering = isMonitoredApp(pkg)
            val wasMonitored = isMonitoredApp(lastPackage)

            if (entering && !wasMonitored) {
                // Entered a social app → start session timer
                statsManager.onAppSessionStart()
            } else if (!entering && wasMonitored) {
                // Left a social app → end session timer, save real time
                statsManager.onAppSessionEnd()
            }

            if (entering) {
                OverlayService.instance?.setOverlayVisible(true)
                OverlayService.instance?.setPlatform(platformIndex)
            }

            if (pkg != lastPackage) {
                lastPackage = pkg
                lastVideoSignature = ""
            }
            return
        }

        val now = SystemClock.uptimeMillis()

        // Direct scroll & page selection events
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_SELECTED) {

            if (now - lastScrollTimestamp < DEBOUNCE_MILLIS) return

            lastScrollTimestamp = now
            lastPackage = pkg

            val sig = extractVideoSignature(rootInActiveWindow ?: event.source, pkg)
            if (!sig.isNullOrEmpty()) lastVideoSignature = sig

            triggerReelCount(platformIndex, pkg, "view_scroll")
            return
        }

        // Video swap detection via content changes (YouTube Shorts, Snapchat, TikTok)
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            if (now - lastScrollTimestamp < DEBOUNCE_MILLIS) return

            val root = rootInActiveWindow ?: event.source ?: return
            val currentSig = extractVideoSignature(root, pkg) ?: return

            if (currentSig == lastVideoSignature) return

            if (lastVideoSignature.isNotEmpty()) {
                lastScrollTimestamp = now
                lastVideoSignature = currentSig
                triggerReelCount(platformIndex, pkg, "video_swap")
            } else {
                lastVideoSignature = currentSig
            }
        }
    }

    private fun extractVideoSignature(root: AccessibilityNodeInfo?, pkg: String): String? {
        if (root == null) return null
        try {
            val parts = mutableListOf<String>()
            var visited = 0

            fun scan(node: AccessibilityNodeInfo?) {
                if (node == null || visited >= 60) return
                visited++
                val text = node.text?.toString()?.trim()
                val desc = node.contentDescription?.toString()?.trim()
                val resId = node.viewIdResourceName?.lowercase() ?: ""

                if (!text.isNullOrEmpty() && text.length in 2..100) {
                    if (text.startsWith("@") || resId.contains("author") || resId.contains("creator") ||
                        resId.contains("channel") || resId.contains("username") || resId.contains("sound") ||
                        resId.contains("music") || resId.contains("title") || resId.contains("reel_video")) {
                        parts.add(text)
                    }
                }
                if (!desc.isNullOrEmpty() && desc.length in 3..140) {
                    if (desc.startsWith("@") || desc.startsWith("Sound", ignoreCase = true) ||
                        desc.startsWith("original sound", ignoreCase = true) ||
                        desc.startsWith("Like this video", ignoreCase = true) ||
                        resId.contains("like") || resId.contains("sound")) {
                        parts.add(desc)
                    }
                }
                for (i in 0 until node.childCount) {
                    if (visited >= 60) break
                    scan(node.getChild(i))
                }
            }

            scan(root)
            return if (parts.isNotEmpty()) parts.distinct().sorted().joinToString("::") else null
        } catch (e: Exception) {
            Log.e(TAG, "Sig error: ${e.message}")
            return null
        }
    }

    private fun triggerReelCount(platformIndex: Int, pkg: String, reason: String) {
        Log.d(TAG, "Reel counted: pkg=$pkg platform=$platformIndex trigger=$reason")
        if (OverlayService.instance == null) {
            val intent = Intent(this, OverlayService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                startForegroundService(intent)
            else startService(intent)
        }
        OverlayService.onReelScrolled(platformIndex)
    }

    override fun onInterrupt() {
        // End session when service is interrupted (screen off, etc.)
        if (::statsManager.isInitialized && isMonitoredApp(lastPackage)) {
            statsManager.onAppSessionEnd()
        }
        Log.d(TAG, "ReelAccessibilityService interrupted")
    }
}

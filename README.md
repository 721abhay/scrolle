# scrolle 🧠

> **Be the first or be the best — so be the best.**

**Scrolle** is an open-source Android app that auto-counts Reels, Shorts, and Spotlights you scroll through every day — and helps you take back control of your attention.

---

## ✨ Features

| Feature | Description |
|---|---|
| 🧠 **Auto-Count** | Counts swipes across Instagram Reels, YouTube Shorts, TikTok, and Snapchat Spotlight — no manual tapping |
| 💊 **Floating Pill Overlay** | Draggable live counter overlay that sits on top of any app |
| 🧟 **Decaying Brain Mascot** | Brain emoji degrades from 🧠✨ → 😐 → 😵‍💫 → 💀 as you scroll more |
| 🧾 **Screen Time Receipt** | Personalized "attention receipt" showing dopamine hits, minutes wasted, and brain score |
| 🛡️ **Brainrot Intervention Shield** | Full-screen wall that appears when you hit your daily limit — 1-tap to close the app |
| ⏱️ **Focus Pomodoro Timer** | 10/25/45/60-min focus sessions that heal your Brain Score (+15%) |
| 🌙 **Bedtime Shield** | Auto-blocks doomscrolling 10:30 PM – 7 AM |
| 📊 **7-Day Insights** | Bar chart + Bezier line chart of your weekly scroll pattern |
| 🔒 **100% On-Device** | Zero cloud storage, zero tracking, zero server cost |

---

## 📱 Download APK

Latest release APK: see [**Releases**](https://github.com/721abhay/scrolle/releases)

---

## 🛠️ Build from Source

### Requirements
- Android Studio Hedgehog or newer
- JDK 21
- Android SDK 34

### Steps

```bash
git clone https://github.com/721abhay/scrolle.git
cd scrolle/android
./gradlew assembleRelease
```

APK will be at `android/app/build/outputs/apk/release/app-release.apk`

---

## 📐 Architecture

```
scrolle/
├── android/                        # Android app (Kotlin + Jetpack Compose)
│   └── app/src/main/kotlin/com/example/scrolle/
│       ├── MainActivity.kt         # 4-tab Compose UI
│       ├── ReelAccessibilityService.kt  # Swipe detection engine
│       ├── OverlayService.kt       # Floating pill + Brainrot Shield
│       └── DailyStatsManager.kt   # On-device stats & Brain Score
└── src/                            # Web landing page (React + Vite)
```

### How Auto-Count Works

1. `ReelAccessibilityService` listens to Android accessibility events
2. **Engine A** — catches `TYPE_VIEW_SCROLLED` on standard RecyclerView feeds (Instagram)
3. **Engine B** — extracts video metadata fingerprint (creator handle, audio title) for custom OpenGL feeds (YouTube Shorts, TikTok, Snapchat) and counts when the fingerprint changes
4. Strict 1.0s debounce ensures **1 swipe = exactly +1 count**

---

## 🔐 Permissions Required

| Permission | Why |
|---|---|
| `SYSTEM_ALERT_WINDOW` | Draw floating pill overlay on top of apps |
| `BIND_ACCESSIBILITY_SERVICE` | Detect swipe events across Instagram, YouTube, TikTok, Snapchat |
| `FOREGROUND_SERVICE` | Keep counter running when screen is on |
| `POST_NOTIFICATIONS` | Show persistent counter notification |
| `VIBRATE` | Haptic feedback on each swipe detected |

---

## 🗺️ Roadmap

- [ ] Widget for home screen
- [ ] Weekly summary push notification
- [ ] Export data as CSV
- [ ] iOS version (SwiftUI)

---

## 📄 License

MIT © 2026 Abhay Vishwakarma

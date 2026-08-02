# Dotz Launcher
<img width="1536" height="1024" alt="file_000000005ac871fa8803a95d954a8c13" src="https://github.com/user-attachments/assets/15b6b731-dc96-4b46-997c-e36a401a5f1d" />
<img width="1536" height="1024" alt="file_00000000a48071fa958b9ae9d456c2e2" src="https://github.com/user-attachments/assets/f24a378b-c2b5-4909-8356-e6771c3b97ae" />


**Built by a smartphone addict, for addicts.**
A minimalist, high-intentionality Android launcher designed to reduce digital clutter and foster calm, mindful smartphone usage.
Inspired by hardware minimalism and modern, focus-driven dashboard interfaces, Dotz Launcher transforms your device into a tool rather than a distraction. It retains instant access to core daily workflows—like payments, music, and communication—while eliminating the visual noise that triggers mindless scrolling.

## 📱 Screenshots

<img width="1080" height="746" alt="Untitled design (6)" src="https://github.com/user-attachments/assets/855c1ef0-7e65-4c31-ae2a-4b59bedc295a" />
<img width="1080" height="748" alt="Untitled design (7)" src="https://github.com/user-attachments/assets/b531608d-deac-4593-8dbe-f8b3825fb40f" />
<img width="1080" height="746" alt="Untitled design (5)" src="https://github.com/user-attachments/assets/abb3c855-489c-419b-af2c-32eb2e1fe515" />
<img width="1080" height="740" alt="Untitled design (4)" src="https://github.com/user-attachments/assets/39591962-8853-49b9-92b6-b35edb6b5052" />
<img width="1080" height="745" alt="Untitled design (3)" src="https://github.com/user-attachments/assets/ad50f187-85b1-4ec9-a3e9-3e2ff0edb091" />
<img width="1080" height="752" alt="Untitled design (11)" src="https://github.com/user-attachments/assets/86108f57-c97e-4151-9f71-e6e6935ccf62" />
<img width="1080" height="752" alt="Untitled design (10)" src="https://github.com/user-attachments/assets/0edfb4bb-c829-4519-9cb9-cf168fbc8aee" />
<img width="1080" height="746" alt="Untitled design (9)" src="https://github.com/user-attachments/assets/cc1f0856-70c7-4c0f-bc6b-75458c82393f" />
<img width="1080" height="746" alt="Untitled design (8)" src="https://github.com/user-attachments/assets/c173453d-550f-4092-bf46-9c7a3bcce614" />

## 🧠 Core Philosophy
Dotz Launcher is a minimalist, high-intentionality dashboard designed to reduce digital clutter. It transforms your phone into a tool for productivity rather than a source of distraction by neutralizing eye-catching branding and introducing intentional friction.

## 🔭 The Vision (Dream)
The development of Dotz Launcher is driven by a vision of reclaiming our focus in an age of digital distraction. These videos encapsulate the inspiration and "dream" behind this project:
*   [The Vision - Part 1](https://youtu.be/m39ZGAem1B8)
*   [The Vision - Part 2](https://youtu.be/FFMwIhez3xw)

## ✨ Comprehensive Features

### 1. Intentional Navigation
*   **Paged Home Screen:** A clean, tile-based 2x3 grid designed to reduce visual noise and prevent mindless tapping.
*   **Timeline View:** A searchable chronological "diary" of your digital day, including notification history, calls, messages, photo captures, and media playback.
*   **Component-Based Launching:** (v7.0.3) Precise targeting of specific app activities (e.g., opening the Dialer directly instead of the generic Contacts app).
*   **App Drawer with Friction:** Discourages mindless scrolling by tracking "Emergency Opens" and requiring confirmation before entering the full app list.
*   **A-Z Drawer:** An alphabetically grouped list for fast, muscle-memory access when you truly need an app.

### 2. Digital Wellness & Mindfulness
*   **Ultra-Focus Mode:** A timed session (15m to 2h) that locks you into a single-page layout with only 7 essential apps. It silences ringtones and shows a live countdown timer.
*   **Focus Score:** A real-time score (0–100) calculated based on unlocks, screen time, and emergency drawer usage.
*   **Weekly Reflection:** (v7.0.2) A Monday morning proactive summary showing usage trends, focus deltas, and wellness ratings (e.g., "EXCELLENT").
*   **Intention Pause:** An optional 3-second delay when opening distracting apps (like social media) to ask if you really need to open them.
*   **Notification Filter & Batching:** Automatically filters "distracting" notifications and allows you to batch them to be delivered at set intervals (e.g., every 4 hours).
*   **Mindful Usage Tracking:** Displays real-time usage time and launch counts directly on home screen tiles.

### 3. Appearance & Personalization
*   **Liquid Glass Theme:** A high-fidelity GPU-accelerated glassmorphism effect that can be applied globally.
*   **Circadian Theming (PRO):** UI colors that dynamically shift based on the time of day (cool tones in the morning, warm amber in the evening).
*   **Theme Modes:** Supports Light, Dark, Circadian, and Transparent (which allows your wallpaper to show through).
*   **Tile Customization:** Control over tile transparency and the ability to rearrange tiles via long-press.
*   **Layout Styles:** Choose between the "Classic Grid" or a "Modern List" layout for your home screen.
*   **Dynamic Profiles:** Create different launcher setups for "Work," "Home," or "Focused" modes, each with its own app assignments and themes.
*   **Icon Pack Support:** Full integration with third-party icon packs.

### 4. Utility & Integration
*   **Smart Contextual Header:** Intelligently switches between Focus Stats, Media Controls (when music is playing), and System Toggles.
*   **Detailed Weather:** Real-time insights including "Feels Like" temperature, daily highs/lows, and live Air Quality Index (AQI).
*   **Quick Capture:** A dedicated header in the Timeline to quickly save text notes and journals.
*   **Upcoming Section:** A real-time peek at your next 24 hours of calendar events and alarms.
*   **Integrated Media Controller:** A professional "Matte" finished controller with progress bars and transport controls.
*   **App Shortcuts:** Long-press any tile to access system-level app shortcuts (Android N+).

### 5. Technical Foundations
*   **Android 16 Ready:** Targeted at API 36 with full support for predictive back navigation.
*   **Pure Compose Navigation:** Built entirely on Jetpack Compose for buttery-smooth page transitions and animations.
*   **Performance Engine:** State memoization and background thread processing ensure zero-lag interactions and low battery impact.
*   **R8 Optimized:** Uses R8 Full Mode and class repackaging to ensure a minimal APK footprint.
*   **Privacy-First:** No data collection; weather and location data are handled locally or via privacy-respecting APIs.

## 🛠️ Technical Identity
* **Package Name:** `com.dotz.launcherpro`
* **Version:** `7.0.3`
* **Architecture:** Pure Jetpack Compose Navigation
* **Language:** 100% Kotlin
* **Minimum Android:** API 24 (Android 7.0)
* **Target Android:** API 36 (Android 16)

## ☕ Support the Project
If Dotz Launcher has helped you reclaim your focus, consider supporting its development!

**Official Website:** [amalsnair535.github.io/dotzlauncherPRO](https://amalsnair535.github.io/dotzlauncherPRO/)
**Google Play:** [Get it on Google Play](https://play.google.com/store/apps/details?id=com.dotz.launcherpro)

**Buy Me a Coffee via UPI:**
`amalsnair535-1@okhdfcbank`

---

# Changelog

All notable changes to this project will be documented in this file.

## [7.4.0] - 2026-08-02

### Added
- **Premium Clock Styles (PRO)**: Unlock 5 unique minimalist clock faces including Modern, Minimalist, Analog, and Words.
- **Atmospheric Custom Themes (PRO)**: A new theme engine that "soaks" your custom accent color into the background (15% tint) and tiles (25% tint) for a cohesive look.
- **Monthly Subscription**: Introduced a flexible Monthly Access plan with a **3-Day Free Trial**.
- **Performance Architecture**: Completely refactored the UI state engine into atomic flows. This dramatically reduces CPU usage and improves battery life by isolating UI updates.
- **In-App Updates**: Stay up to date with official Google Play integration; update banners now appear directly on your Timeline.
- **Accessibility Inclusivity**: Added full TalkBack support for the new Custom Color palette and Clock styles.

### Improved
- **Minimalist Weather**: Redesigned the weather widget to remove redundant time and summary text, focusing on essential data.
- **Universal Header Gestures**: Centralized gesture handling allows for reliable long-press and swipe actions across all header modes (Toggles, Stats, Weather, and Music).
- **Hybrid Media Sync**: Upgraded `MediaManager` with token-based tracking and a background sync loop to ensure 100% reliable metadata updates.
- **Robust System Toggles**: Implemented "Optimistic Updates" for the Flashlight and Ringer toggles for instant visual feedback. Added DND permission handling for Silent mode.
- **Fluid Navigation**: Added premium slide animations for settings navigation and resolved the "wallpaper flash" flickering.

### Fixed
- **Startup Crash**: Resolved a critical `SecurityException` caused by accessing media sessions before the "Notification Listener" permission was granted.
- **Settings Clarity**: Redesigned the Settings menu into a faster, sectioned list with a **Live Preview** card for instant feedback.

## [7.0.3] - 2026-07-28

### Added
- **Component-Based Launching**: Support for targeting specific activities within an app. This allows for precise discovery and targeted launching, especially for apps that bundle multiple tools (like a Dialer and Contacts) into a single package.
- **Enhanced App Discovery**: The launcher now scans for all available launcher activities, allowing distinct entries for different tools in the same package.

### Fixed
- **Oppo Dialer Discovery**: Fixed an issue where the "CALL" tile would erroneously open the Contacts activity instead of the Dialer on certain devices (Oppo, etc.).
- **Prioritized Intents**: Explicitly prioritized `ACTION_DIAL` intent matches for the "CALL" tile to ensure the best user experience.

## [7.0.2] - 2026-07-26

### Added
- **Weekly Reflection**: A new proactive digital wellness feature that appears every Monday morning.
    - **Proactive Insights**: Automatically triggers on the first unlock of the week to summarize your previous week's performance.
    - **Smart Metrics**: Tracks Focus Score trends, Unlock analytics (with percentage changes), and Notification noise (Received vs. Ignored).
    - **Achievements**: Highlights your "Longest Focus Session" and "Most Productive Day."
    - **Historical Tracking**: Implemented a rolling 30-day window for daily snapshots of unlocks, notifications, and screen time to power future comparisons.

## [7.0.0] - 2026-07-25

### Added
- **Android 16 Support**: Updated project to target Android 16 (API 36), ensuring compatibility with the latest platform features and predictive back navigation.
- **Google Play Billing 8.0.0**: Migrated to the latest Billing Library with support for automatic service reconnection and improved purchase flows.
- **Architectural Refactor**: Decoupled the monolithic `LauncherViewModel` into specialized managers (`SystemStateManager`, `WeatherManager`, `MediaManager`, `AdsManager`, `LocationManager`) for better maintainability and testability.
- **Ultra Focus UX Enhancements**: Added a dedicated "Select Apps" prompt when focus mode is empty and implemented real-time search for faster app selection.

### Improved
- **Performance Engine**: Implemented a new state memoization system that drastically reduces CPU and battery usage by preventing redundant re-calculations of the UI state.
- **R8 Optimizations**: Re-enabled **R8 Full Mode** and added **Class Repackaging** to significantly reduce APK size and improve runtime performance.

## [6.0.9] - 2026-07-04

### Added
- **AGP 9.3.0 Upgrade**: Updated Android Gradle Plugin to version 9.3.0 for enhanced build performance and R8 optimizations.
- **Deep Code Optimization**: Enabled `repackageclasses` and `allowaccessmodification` in R8 to further reduce app size and improve runtime speed.
- **Gradle 8.12**: Upgraded Gradle wrapper to 8.12 for better build stability and compatibility with modern build tools.

## [6.0.8] - 2026-07-04

### Added
- **R8 Full Mode**: Migrated to R8 Full Mode for more aggressive code optimization and smaller app footprints.
- **Advanced Resource Shrinking**: Enabled optimized resource shrinking to strip away unused assets more effectively.

### Fixed
- **Android 15 Compatibility**: Migrated away from deprecated system bar color APIs and display cutout modes to comply with Android 15 edge-to-edge enforcement.
- **Vector Resource Stability**: Cleaned up XML declarations and standardized vector path data for better cross-platform rendering performance.

### Improved
- **Performance**: Refined hardware-level detection for high refresh rate displays (120Hz/144Hz).
- **UI Experience**: Polished "Liquid Glass" tile rendering and improved Focus History chart accuracy in the Timeline.

## [6.0.6] - 2026-07-04

### Fixed
- **Build Resolution**: Fixed critical `AAR Metadata` mismatch by aligning `compileSdk` with library requirements.
- **Installation Fix**: Resolved `INSTALL_BASELINE_PROFILE_FAILED` by resetting optimization metadata and bumping version identity.

## [6.0.5] - 2026-07-04

### Added
- **Evolved Fastlane (Timeline)**: A complete overhaul of the activity feed into a persistent, searchable digital diary.
- **Deep System Integration**: Automatically logs call history, SMS notifications, and media playback into the chronological stream.
- **Creative Logging**: New photos and screenshots are now timestamped and recorded in the Timeline.
- **Notification History**: View and search notifications even after they are dismissed.
- **Contextual AI Summaries**: Time-of-day specific reflections (Morning/Afternoon/Night) to help analyze digital habits.
- **Upcoming Section**: Real-time peek at the next 24 hours of calendar events and alarms.
- **Smart App Predictions**: Suggests relevant tools based on time, network state, and usage patterns.
- **Global Activity Search**: Type to find any past interaction across your entire digital history.

### Fixed
- **Build Stability**: Resolved APK installation conflicts by standardizing Target SDK to 35.

## [6.0.4] - 2026-07-04

### Added
- **Detailed Weather Header**: Introduced a new "Weather" header mode that provides comprehensive real-time insights, including "Feels Like" temperature, daily highs/lows, and a conditions summary.
- **Air Quality Integration (AQI)**: Stay informed about your environment with live AQI tracking integrated directly into the new Weather header.

## [6.0.3] - 2026-07-04

### Added
- **Timeline by Default**: Timeline mode is now enabled by default for all new installations, providing immediate access to the launcher's core chronological activity tracking.
- **AdMob Verification**: Added `app-ads.txt` support to the root and documentation for seamless AdMob account verification.

### Improved
- **Settings Experience**: Streamlined the initial setup by pre-configuring essential features.

## [6.0.2] - 2026-07-04

### Added
- **Timeline Rebranding**: Officially renamed the "Fastlane" feature to **Timeline** across the entire app, settings, and documentation for a more intuitive user experience.
- **Expanded Control Panel Icons**: Added dedicated minimalist icons for `Mobile Data Off` and `Vibrate Mode`.

### Fixed
- **Dynamic Icon Logic**: 
    - Fixed an issue where the Mobile Data icon would not correctly reflect the system's "Disabled" state.
    - Updated the sound toggle to a 3-state system (Normal, Vibrate, Silent), ensuring the new Vibrate icon appears correctly when active.
- **Resource Compatibility Overhaul**: 
    - Converted all legacy SVG resources to **Android Vector Drawable (XML)** format, resolving the "file name must end with .xml or .png" build error.
    - Standardized all drawable filenames to strictly follow Android lowercase/underscore naming conventions, eliminating resource packaging failures.
    - Optimized vector path data for improved rendering performance across all device DPIs.

## [6.0.1] - 2026-07-03

### Fixed
- **Stability Improvements**: Minor bug fixes and UI performance optimizations.

## [6.0.0] - 2026-07-03

### Added
- **Liquid Glass Evolution**: 
    - Global Glassmorphism: New "Liquid Glass Effect" that can be applied to any theme.
    - GPU-Accelerated Backgrounds: Buttery-smooth animated "liquid" gradient backgrounds.
    - Adaptive Contrast: Glass effect intelligently switches to a "Subtle Dark Glass" in Light Mode.
- **Revolutionary Ultra-Focus Mode**:
    - Timed Focus Sessions: Start dedicated deep-work sessions (15m to 2h) from Settings.
    - Essentials-Only Dashboard: Single-page centered layout with 7 core apps (Phone, Camera, Maps, WhatsApp, Messaging, Music, Notes).
    - Real-Time Countdown: Live per-second timer for focus tracking.
    - Alphabetical Essentials: Core apps are now sorted for instant muscle-memory access.
- **Settings Overhaul**:
    - Expandable Category Cards: Reduced vertical length with a cleaner, organized architecture.
    - Unified Appearance Hub: Merged Navigation and Theme settings into one logical group.
- **Performance & High Refresh Rate**:
    - 120Hz Mastery: Hardware-level detection automatically requests the highest refresh rate (90Hz/120Hz/144Hz).
    - Zero-Lag Engine: Background state calculations and IPC caching for maximum responsiveness.
- **Refined About & Support**:
    - Enriched About Screen: Quick actions to Rate the App, view Changelog, and Support Development.
    - Guided Permissions: Improved Mindful Usage onboarding with clear disclosures.

### Fixed
- **Release Build Stability**: Resolved critical compilation errors and missing system imports.
- **Usage Tracking Accuracy**: Refined the screen-time engine to match system wellbeing metrics.

## [5.5.9] - 2026-07-02

### Added
- **Pure Compose Navigation**: Completely refactored the navigation architecture to remove legacy ViewPager2 and Fragments. The entire experience is now powered by a high-performance Compose engine for buttery-smooth page switching.
- **Deep Digital Wellbeing Integration**: The Focus Header now pulls real-time data directly from the system's wellbeing engine.
    - **Live Unlock Tracking**: See exactly how many times you've unlocked your device today.
    - **Total Notification Count**: Tracks all incoming alerts to help you measure digital noise.
- **Enhanced Legacy Support**: Lowered minimum requirements to **Android 24 (Nougat)**, allowing Dotz to run on a wider range of hardware.
- **Redesigned Media Controller**: A new two-row layout for the Music Widget, featuring centered controls and a more professional "Matte" aesthetic.
- **New Onboarding Experience**: A beautiful 3-page introduction to the launcher's core philosophy (Minimalism, Timeline, and Mindfulness) with visual aids.
- **Rewarded Ad Support**: Added options to support the developer and unlock 24-hour PRO access by watching a short video.
- **Sarcastic PRO Badge**: Added a touch of humor to the PRO upgrade screen (as requested by the community).

### Fixed
- **Persistent Compliance Popups**: Fixed a bug where the "App Visibility" disclosure and "Default Launcher" prompts would reappear on every launch.
- **Animation Fluidity**: Optimized the Timeline transition to use direct pager offset tracking, eliminating lag during swipes.
- **Header Layout Overlaps**: Corrected a regression where status bar text could overlap with the central clock on certain screen aspect ratios.

## [5.5.8] - 2026-07-01

### Added
- **Unified Build Architecture**: Removed `google` and `indus` flavors. Unified billing and location logic into a single, maintainable codebase.
- **Redesigned Dashboard Widgets**:
    - **Music Widget**: Enhanced with a progress bar, album art placeholder, and centered playback controls for better aesthetics and usability.
    - **Focus Stats**: Now features a circular progress indicator for the Focus Score, providing a clearer visual of daily mindfulness.
- **Material You Settings Overhaul**: A complete redesign of the Settings screen using Material 3 components. It now features a collapsible Large Top App Bar, grouped settings categories, and a cleaner visual hierarchy.
- **Actionable Timeline (Quick Reply)**: You can now reply to incoming messages (WhatsApp, Telegram, SMS) directly from the Timeline timeline.
- **Dynamic Icon Dimming**: App icons now automatically fade to 20% opacity after 30 minutes of usage today.
- **Grayscale Auto-Schedule (PRO)**: Automate your bedtime routine by scheduling grayscale mode between 10 PM and 6 AM.

### Fixed
- **Midnight Reset Accuracy**: Ensure all mindful limits and usage stats reset exactly at 00:00:00.
- **UI Consistency**: Improved contrast for media controls in both Light Mode and Transparent Mode.
- **Gesture Conflict Resolution**: Refined swiping logic to prevent conflicts between the App Drawer and Timeline navigation.

## [5.5.7] - 2026-06-28

### Added
- **Refined Navigation**: Dedicated Timeline timeline for chronological activity tracking.
- **Pro Feature Badging**: High-visibility [PRO] badges added to all premium settings for easier discovery.

### Fixed
- **State Flow Reliability**: Optimized data updates in the ViewModel to ensure real-time UI synchronization.

## [5.5.6] - 2026-06-29

### Added
- **Multiple Home Screen Profiles:** Create and switch between different launcher setups (e.g., Work, Home, Focused). Each profile saves its own tile layout, app assignments, grayscale settings, and layout styles.
- **Dynamic Profile Management:** Easily create new profiles by cloning your current setup or delete unused ones directly from Settings.

### Fixed
- **State Persistence:** Optimized the profile switching logic to ensure "Default" settings are preserved and never overwritten by custom profile data.

## [5.5.5] - 2026-06-28

### Added
- **Stability & Maintenance:** Improved backend tracking of screen time for 100% accuracy.
- **Enhanced Debugging:** Integrated native debug symbol generation to improve crash analysis in Google Play Console.

### Fixed
- **Screen Time Accuracy:** Resolved a final discrepancy in total usage time by refining foreground event tracking.
- **Optimized UI:** Further reduced lag on transition between home and dashboard.

## [5.5.4] - 2026-06-27

### Added
- **A-Z App Drawer:** The "All Apps" drawer is now alphabetically grouped with section headers (A, B, C...) for faster navigation.
- **Improved Usage Accuracy:** Screen time calculation has been rewritten to perfectly match system Digital Wellbeing metrics by excluding launcher and system UI time.
- **Background Optimization:** Computation of app lists and usage stats now runs on a dedicated background thread, eliminating lag in settings and menus.

### Fixed
- **App Selection Stability:** Fixed an issue where apps would occasionally not show up in the selection window during remapping.
- **Weather Reliability:** Fully migrated to Open-Meteo for more robust and accurate weather fetching without the need for API keys.

## [5.5.3] - 2026-06-26

### Added
- **Digital Balance Dashboard:** Replaced "Focus Stats" with a new **App Usage** card that displays your top 5 most-used apps and total screen time directly on the dashboard.
- **Mindful Usage Tracking:** Real-time indicators of time spent on each app now appear directly on home screen tiles and in the app drawer.
- **Selective Mindfulness Prompt:** A smart check-in appears when you try to open a social media app for the 11th time in a single day, helping you stay intentional with your digital habits.
- **Mindfulness Settings:** Added a dedicated toggle in settings to enable or disable usage tracking and launch prompts.

### Fixed
- **Navigation Conflict:** Implemented a gesture dead zone at the bottom of the screen to prevent the app drawer from interfering with system navigation (Home/Recents).
- **Accuracy Overhaul:** Rewrote usage tracking logic to match system Digital Wellbeing metrics exactly. Fixed a bug that caused "random" high screen time numbers.
- **Permission Flow:** Added a guided prompt to help users easily grant the required "Usage Access" permission.
- **App Usage Link:** Tapping the App Usage card on the dashboard now opens the system Digital Wellbeing settings.

## [5.5.2] - 2026-06-25

### Added
- **Digital Balance Dashboard:** Replaced "Focus Stats" with a new **App Usage** card that displays your top 5 most-used apps and total screen time directly on the dashboard.
- **Mindful Usage Tracking:** Real-time indicators of time spent on each app now appear directly on home screen tiles and in the app drawer.
- **Mindfulness Prompt:** A smart check-in appears when you try to open a social media app for the 11th time in a single day, helping you stay intentional with your digital habits.
- **Interactive Dashboard:** Refined the dashboard experience. Buttons now distinguish between a quick tap and a long press.
- **Direct Toggles:** Tapping dashboard icons (WiFi, Bluetooth, Dark Mode, Silent, Torch) now performs the action directly without opening settings where possible.
- **Haptic Feedback:** Added tactile vibration feedback for long-press actions on the dashboard and for opening the app drawer.
- **Movable Tiles:** Home screen tiles can now be rearranged via long-press and swap gesture.
- **Refined Data Control:** Integrated the new Internet Connectivity Panel for quicker network management on modern Android versions.

### Fixed
- **Build Stability:** Resolved critical "Unresolved Reference" errors in `HomeFragment` and `AppSelectionActivity` that were causing build failures.
- **ViewModel Logic:** Added missing `updateTileOverride` logic to properly handle custom app assignments for tiles.
- **PRO Transition:** Prevented the app from automatically switching to Transparent mode when upgrading to PRO; it now maintains the current theme mode.

### Changed
- **Sound Profile Feedback:** Re-enabled status Toasts for sound profile toggles (Normal, Vibrate, Silent) for better visibility.

## [5.5.1] - 2026-06-20

### Added
- **Rebrand:** Officially renamed the app to **Dotz Launcher** for a cleaner, more minimalist identity.
- **Circadian Theming (PRO):** Subtle, dynamic UI color shifts based on the time of day (Cool morning tones, warm evening amber).
- **Unified Theme Selector:** Redesigned theme settings with a 4-way mode selector (Light, Dark, Circadian, Transparent).
- **Exclusive Lifetime Offer:** Permanent PRO access now available for a limited-time promotional price of $0.99 / ₹110.

### Fixed
- **PRO Redemption Fix:** Resolved an issue where promo codes wouldn't persist or unlock features correctly in certain build flavors.
- **Context-Aware Wallpaper Settings:** Wallpaper controls now automatically hide when non-transparent themes are active to reduce clutter.

### Changed
- **Battery Optimization:** Suppressed high-frequency media polling when the dashboard is hidden and implemented 3-hour weather data caching.
- **Settings UI Polish:** Updated "Unlock PRO" banners with detailed feature lists and improved visual hierarchy.

## [5.5.0] - 2026-06-15

### Added
- **Modern List Layout:** New alternative layout for home screen tiles with optimized 72dp height and 8dp spacing, fitting 6 tiles perfectly.
- **Tile Transparency:** Added premium control for tile background opacity, allowing wallpapers to blend seamlessly.
- **Accurate Weather:** Migrated to Open-Meteo API for higher reliability and no-key access. Weather is now OFF by default for better privacy.
- **Enhanced Subscription UI:** Improved "Dotz Upgrade" screen with dynamic pricing.
- **Compliance & Privacy:** Added mandatory "QUERY_ALL_PACKAGES" and Location disclosures for store compliance.

### Fixed
- **UI Flickering:** Resolved flickering issues by synchronizing immersive mode across all activities and fragments.
- **Fragment Stability:** Fixed `LaunchedEffect` triggers in `HomeFragment` to prevent unnecessary UI refreshes.
- **Weather Permissions:** Weather logic now only requests location permissions when explicitly enabled by the user.

### Changed
- **Removed Self-Updater:** Offline update logic removed to comply with store policies.
- **SDK Target:** Updated `targetSdk` to 35 to meet the latest requirements.

## [5.4.1] - 2026-06-02

### Added
- **Clock Format Option:** Added a toggle in settings to switch between 12-hour and 24-hour clock formats.
- **Light Mode Support:** Added a new "Dotz Light" theme with a clean, high-contrast palette. Toggle it in settings.
- **Dynamic Theming:** All UI components now dynamically adapt to Light/Dark mode settings.

### Fixed
- **UI Overlapping:** Resolved layout issues where dashboard elements could overlap with the home screen during navigation.
- **Improved Spacing:** Optimized bottom padding on the home screen and dashboard for better balance on modern devices.
- **Enhanced Transitions:** Refined the fluid page transformer for smoother, ghosting-free swipes.

### Changed
- **Simplified UI:** Removed redundant "Dynamic Background" and "Tile Opacity" features to focus on minimalist performance.
- **Cleaned Dashboard:** Removed unnecessary "More" icons from dashboard cards for a cleaner look.

## [5.3.0] - 2026-06-02

### Added
- **Stability & Maintenance:** Improved backend tracking of screen time for 100% accuracy.

### Fixed
- **Screen Time Accuracy:** Resolved a final discrepancy in total usage time by refining foreground event tracking.
- **Optimized UI:** Further reduced lag on transition between home and dashboard.

## [5.2.1] - 2026-05-27

### Fixed
- **Experimental API Warning:** Resolved a compiler warning related to the experimental `combinedClickable` API in `DotzAboutActivity`.
- **Code Cleanup:** Removed unused imports and optimized `Uri.parse` usage.

## [5.2.0] - 2026-05-26

### Fixed
- **Build & Resource Stability:** Resolved critical XML parse errors in launcher icons and resource files by correcting processing instruction placement.
- **Documentation Update:** Comprehensive update to README with full PRO feature list.

## [5.1.0] - 2026-05-25

### Fixed
- Internal build stabilization and repository cleanup.

## [5.0.0] - 2026-05-25

### Added
- **Extended Tile Support:** Added an optional third page with 1-6 customizable extra app tiles.
- **Vertical Navigation:** Introduced a "Vertical Scrolling" mode as an alternative to horizontal paging.
- **Interactive Media Control:** New dashboard card with real-time metadata and transport controls (Play/Pause, Skip) for active media sessions.
- **Smart App Remapping:** Categorized app selection that suggests relevant apps based on the tile's purpose (e.g., suggesting dialers for the "CALL" tile).
- **Consolidated App Selection:** Added a centralized "App Selection" menu in settings to manage all tile overrides in one place.
- **Focus Statistics:** New dashboard widget to track launcher usage and notification metrics.
- **Refined System Shortcuts:** Added direct shortcuts for Mobile Data settings, Weather apps, and Default Launcher configuration.

### Fixed
- **Build & Lint Cleanup:** Resolved critical build errors related to manifest permissions, deprecated `onBackPressed` usage, and API 27+ theme attributes.
- **Immersive Mode Stability:** Fixed issues with status bar behavior across different activities.

---

## [4.2.0] - 2026-05-24

### Added
- **Swipe-Right Dashboard:** Implemented a new ViewPager2-based navigation system allowing users to swipe right for a dedicated dashboard.
- **Minimalist Dashboard UI:**
    - Top-centered minimalist clock and date display.
    - Text-based notification feed (removes distracting icons).
    - "Clear All" notifications button within the dashboard.
    - Integrated system calendar events list (Upcoming Events).
    - Bottom-positioned interactive monthly calendar widget.
- **Live Weather Integration:** Added real-time temperature and weather conditions to the Home Screen header.
- **Weather Customization:** Added a toggle in settings to show/hide weather information.
- **Dashboard Settings:** Added a toggle to enable or disable the swipe-right dashboard functionality.
- **Calendar Support:** Integrated `CalendarContract` to fetch and display local system events.

### Fixed
- **App Opening Issue:** Resolved a crash where the app wouldn't open by migrating to `AppCompatActivity` and updating the theme to `Theme.AppCompat.DayNight.NoActionBar`.
- **Type Mismatch:** Fixed a Kotlin compiler error in the `combine` flow operation within `LauncherViewModel`.
- **Gson References:** Corrected unresolved references to Gson methods (`getAsDouble`, `getAsInt`).

### Changed
- Migrated main navigation from a pure Compose layout to a `ViewPager2` hosting fragments (`HomeFragment`, `DashboardFragment`) for better gesture support.
- Updated Home Screen weather styling to match the minimalist high-intentionality design.
- Adjusted status bar and immersive mode handling for the new Fragment-based architecture.

---
## [4.1.0] - 2026-05-23

### Added
- Initial PRO features: Tile remapping, Notification Filter, and JSON Backup/Restore.
- Grayscale mode and Icon Pack support.

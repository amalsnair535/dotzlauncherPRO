# Changelog

All notable changes to this project will be documented in this file.

## [5.5.8] - 2026-07-01

### Added
- **Unified Build Architecture**: Removed `google` and `indus` flavors. Unified billing and location logic into a single, maintainable codebase.
- **Redesigned Dashboard Widgets**:
    - **Music Widget**: Enhanced with a progress bar, album art placeholder, and centered playback controls for better aesthetics and usability.
    - **Focus Stats**: Now features a circular progress indicator for the Focus Score, providing a clearer visual of daily mindfulness.
- **Material You Settings Overhaul**: A complete redesign of the Settings screen using Material 3 components. It now features a collapsible Large Top App Bar, grouped settings categories, and a cleaner visual hierarchy.
- **Actionable Fastlane (Quick Reply)**: You can now reply to incoming messages (WhatsApp, Telegram, SMS) directly from the Fastlane timeline.
- **Dynamic Icon Dimming**: App icons now automatically fade to 20% opacity after 30 minutes of usage today.
- **Grayscale Auto-Schedule (PRO)**: Automate your bedtime routine by scheduling grayscale mode between 10 PM and 6 AM.
- **Tavily AI Search**: Upgraded the Dotz AI backend with real-time web search capabilities for more accurate and current answers.

### Fixed
- **Midnight Reset Accuracy**: Ensure all mindful limits and usage stats reset exactly at 00:00:00.
- **UI Consistency**: Improved contrast for media controls in both Light Mode and Transparent Mode.
- **Gesture Conflict Resolution**: Refined swiping logic to prevent conflicts between the App Drawer and Fastlane navigation.

## [5.5.7] - 2026-06-28

### Added
- **Refined Navigation**: Dedicated Fastlane timeline for chronological activity tracking.
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
- **PRO Dashboard Experience:** AI Summary and Dotz AI Assistant are now premium features, creating a clear distinction between utility and intelligence.
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
- **Restored AI Features:** Re-implemented Dotz AI Assistant and AI Summary card on the dashboard.
- **Improved AI Logic:** Backend logic now uses a dedicated Cloudflare Worker for faster and more reliable responses.
- **Model Update:** Upgraded AI model to `llama-3.1-8b-instant` for improved reasoning and accuracy.

### Fixed
- **AI Backend Stability:** Resolved model deprecation issues and infrastructure conflicts between website assets and API endpoints.
- **Enhanced Error Reporting:** The dashboard now displays specific server-side errors to help troubleshoot AI connection issues.

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
- **Cloud-Powered Dotz AI:** Integrated an AI assistant on the dashboard for intelligent queries, including an "AI Summary" card.
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

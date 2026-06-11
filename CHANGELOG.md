# Changelog

All notable changes to this project will be documented in this file.

## [5.5.0] - 2026-06-15

### Added
- **Multi-Flavor Build System:** Now supporting `google` (Play Store) and `indus` (Lite/Standalone) variants.
- **Indus Appstore Support:** Fully optimized for the Indus Appstore with targeted SDK 36 support.
- **Modern List Layout:** New alternative layout for home screen tiles with optimized 72dp height and 8dp spacing, fitting 6 tiles perfectly.
- **Tile Transparency:** Added premium control for tile background opacity, allowing wallpapers to blend seamlessly.
- **Accurate Weather:** Migrated to MET Norway (Yr.no) API for higher accuracy. Weather is now OFF by default for better privacy.
- **Enhanced Subscription UI:** Dynamic pricing for Google Play variant with improved "Dotz Upgrade" screen.
- **Compliance & Privacy:** Added mandatory "QUERY_ALL_PACKAGES" and Location disclosures for store compliance.

### Fixed
- **UI Flickering:** Resolved flickering issues by synchronizing immersive mode across all activities and fragments.
- **Fragment Stability:** Fixed `LaunchedEffect` triggers in `HomeFragment` to prevent unnecessary UI refreshes.
- **Weather Permissions:** Weather logic now only requests location permissions when explicitly enabled by the user.

### Changed
- **Removed Self-Updater:** Offline update logic removed for the `google` flavor to comply with Play Store policies.
- **SDK Target:** Updated `targetSdk` to 36 (Android 16 DP) to meet the latest store requirements.

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

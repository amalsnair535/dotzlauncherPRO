# Changelog

All notable changes to this project will be documented in this file.

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
- Dynamic background system.

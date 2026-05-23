# Changelog

All notable changes to this project will be documented in this file.

## [4.1.0] - 2024-05-24

### Added
- **Permanent Dual-Page Layout**: Tiles 1-12 are now always available as the core launcher experience.
- **Optional Page 3**: Added a toggle in Settings to enable an additional 6 tiles (Total 18).
- **Refined Appearance Settings**: Replaced the tile slider with a dedicated "Enable Extra Tiles" toggle for better clarity.

## [4.0.1] - 2024-05-24

### Added
- **Expanded Tile Capacity**: Increased the maximum number of visible tiles from 12 to 18.
- **Third Page Support**: Enabling 13-18 tiles automatically creates a third screen for app organization.

## [4.0.0] - 2024-05-24

### Added
- **Dynamic Tile Layout**: New "Visible Tiles" slider in Appearance settings. Users can now choose between 1 and 12 visible tiles.
- **Single-Page Mode**: The launcher automatically adapts to a single-page view if 6 or fewer tiles are selected.
- **Universal App Selection**: Removed restrictions on app remapping. Every tile can now be assigned to any installed app on the device.
- **PRO Branding**: Complete rebranding of the application to "Dotz Launcher PRO".
- **GitHub Launch**: Project initialized as a standalone repository for the PRO version.

### Changed
- **Package Refactoring**: Migrated internal package structure to `com.dotz.launcherpro` for a clean project slate.
- **Improved App Picker**: Reorganized the app selection list to show suggested apps first, followed by all other installed apps.
- **UI Consistency**: Updated theme names and strings to align with the new PRO identity.

## [3.0.0] - 2024-05-22

### Added
- **About Section**: New dedicated "About" screen with app information, contact details, and project links.
- **Improved UI**: Subtle monochrome icons and matte divider lines for a cleaner aesthetic.
- **Privacy Assurance**: Clear statement on local data storage and privacy.

## [1.0.2] - 2024-05-22

### Added
- **Vertical Scrolling**: Users can now choose between horizontal and vertical scrolling for the app pages via a new setting.

## [1.0.1] - 2024-05-22

### Added
- **Direct App Assignment**: Clicking an unassigned tile now opens a popup dialog to quickly select an app.
- **Visual Assignment Cues**: Unassigned tiles are now automatically dimmed to indicate they need setup.
- **Self-Settings Shortcut**: Tapping the "SETTINGS" tile now opens Dotz Settings directly instead of system settings.

## [1.0.0] - 2024-05-22

### Added
- **Minimalist 8x8 Grid**: Core launcher interface.
- **Adaptive Icon**: Custom dot-grid adaptive icon for the launcher.
- **App Selection Screen**: New dedicated screen for remapping tile assignments.
- **Filtered App Picker**: Intelligent app suggestions based on tile type (e.g., Phone, Music, Maps).
- **Icon Pack Support**: Advanced parsing of `appfilter.xml` for full compatibility with third-party icon packs.
- **Set as Default Launcher**: Integrated system prompt and settings option to set Dotz as the primary home app.
- **Detox Panel**: Centered control panel for quick system toggles (WiFi, BT, Data, etc.).
- **Backup & Restore**: JSON-based export/import of launcher configurations.

### Fixed
- **Mobile Data Crash**: Resolved instability when opening system data settings across various Android versions.
- **Icon Rendering**: Fixed scaling issues where icons appeared too large or clipped.
- **State Synchronization**: Ensured "Set as Default" prompt disappears immediately after action.
- **Deprecation Warnings**: Updated various API calls to modern standards.

### Removed
- **Focus Mode**: Removed the manual focus timer feature to maintain the philosophy that the launcher itself is the focus tool.

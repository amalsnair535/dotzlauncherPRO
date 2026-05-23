# Changelog

All notable changes to this project will be documented in this file.

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

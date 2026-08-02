# Release Notes - v7.0.4 (Post-7.0.3 Stability Update)

This update focuses on improving the stability, performance, and maintainability of the Dotz Launcher core. We've modernized several internal systems and resolved numerous linting issues to ensure a smoother development experience and a more efficient app.

## Summary of Changes

### 🛠 Modernization & API Updates
- **Alarm System**: Migrated from deprecated `Settings.System.NEXT_ALARM_FORMATTED` to the modern `AlarmManager.getNextAlarmClock()` API, ensuring accurate alarm reporting on Android 12+.
- **Icon Library**: Updated UI components to use `AutoMirrored` material icons, improving support for Right-to-Left (RTL) layouts.
- **Kotlin Features**:
    - Implemented `kotlin.time` for more readable duration handling (`60.seconds`).
    - Leveraged `toUri()` KTX extension for safer URI parsing.
    - Used `abs()` instead of `Math.abs()` for idiomatic Kotlin.

### ⚡️ Performance & Stability
- **Data Streams**: Refined `combine` operations in `LauncherViewModel` to eliminate unchecked casts and intersection type inference issues, leading to more predictable state management.
- **Optimization**: Used `asSequence()` for efficient sorting and processing of large data sets, such as focus score history.
- **Code Cleanup**: Removed dead code, including unused functions and properties in the ViewModel, reducing the app's memory footprint and simplifying the logic.

### 🎨 UI & UX Improvements
- **Component Refactoring**: Optimized `AppGrid` and `DotzHomeScreen` parameter ordering and formatting to align with modern Jetpack Compose standards.
- **Visual Polish**: Fixed missing trailing commas and added clarifying parentheses in complex UI logic to prevent potential runtime behavior issues.
- **Named Arguments**: Enhanced code readability by using named arguments for boolean flags and configuration parameters.

### 🐛 Bug Fixes & Linting
- Resolved multiple warnings related to trailing lambdas and parameter names.
- Fixed potential null-safety issues in app launching logic using `isNullOrBlank()`.
- Improved hour-based logic (like Circadian theming) using range checks.

---
> [!NOTE]
> These changes are primarily under-the-hood and lay the foundation for future feature updates and performance enhancements.

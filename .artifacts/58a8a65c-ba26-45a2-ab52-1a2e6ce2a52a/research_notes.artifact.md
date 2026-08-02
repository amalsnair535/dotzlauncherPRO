# Research Notes - In-App Updates

## Objective
Implement a mechanism to alert users when a new version of Dotz Launcher is available on Google Play.

## Proposed Solution: Google Play In-App Updates
Google Play Core library provides `AppUpdateManager` which handles checking for updates and performing the update flow.

### Update Types:
1.  **Flexible**: Downloads the update in the background. The user can continue using the app. Once downloaded, the user is prompted to restart the app to apply the update.
2.  **Immediate**: A full-screen UI that blocks the user until the update is downloaded and installed.

### Implementation Steps:
1.  Add `com.google.android.play:app-update-ktx` to `libs.versions.toml` and `build.gradle.kts`.
2.  Update `LauncherViewModel` to check for updates using `AppUpdateManager`.
3.  Expose `isUpdateAvailable` through `LauncherUiState`.
4.  Update `DotzHomeScreen` to show an alert (e.g., a Snackbar or a special Tile) when `isUpdateAvailable` is true.
5.  Provide a button to trigger the update flow.

## Alternative: Simple Check (Current State)
Currently, `isUpdateAvailable` is hardcoded to `false`. We can manually check against a remote endpoint or Firebase Remote Config, but In-App Updates is more robust for Google Play distribution.

## Implementation Details in `LauncherViewModel`:
- Use `AppUpdateManagerFactory.create(context)`
- `appUpdateManager.appUpdateInfo.addOnSuccessListener { ... }`
- Check `updateAvailability == UpdateAvailability.UPDATE_AVAILABLE`
- Check if the update is allowed (`AppUpdateType.FLEXIBLE` or `AppUpdateType.IMMEDIATE`)

## UI Design:
- **DotzHomeScreen**: A small, non-intrusive banner at the top or a dedicated "Update Available" item in the timeline/pager.
- **DotzSettingsActivity**: Already has a placeholder: `label = if (isUpdateAvailable) stringResource(R.string.settings_item_update_available) else stringResource(R.string.settings_item_about_dotz)`.

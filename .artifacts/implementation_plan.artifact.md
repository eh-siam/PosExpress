# Support Both Dark and Light Mode Implementation Plan

This plan outlines the changes required to properly support both Dark Mode and Light Mode in the PosExpress app, allowing the app to automatically adapt to the user's system theme preferences rather than forcing Light Mode.

## User Review Required

> [!IMPORTANT]
> To support true Day/Night switching, we will create a dark color palette in `res/values-night/colors.xml` and update `res/values-night/themes.xml`, while removing the code that forces Light Mode.

## Open Questions

None. The requirements are clear: support both dark and light mode seamlessly.

## Proposed Changes

### Theme and Colors

#### [MODIFY] [SplashActivity.java](file:///home/simec-system-android/AndroidStudioProjects/PosApplicatinJava/app/src/main/java/com/max/posexpress/SplashActivity.java)
- Remove `AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);` so the app respects system dark/light mode settings.

#### [NEW] [colors.xml (night)](file:///home/simec-system-android/AndroidStudioProjects/PosApplicatinJava/app/src/main/res/values-night/colors.xml)
- Define dark theme color palette (e.g., dark surface colors, readable light text colors like `textPrimary` as `#E3E3E3`, `textSecondary` as `#C4C7EB`, dark window background like `#121212`, card background, etc.).

#### [MODIFY] [themes.xml (night)](file:///home/simec-system-android/AndroidStudioProjects/PosApplicatinJava/app/src/main/res/values-night/themes.xml)
- Configure `Base.Theme.PosExpress` to use the dark theme color resources (`colorPrimary`, `android:windowBackground`, `colorSurface`, etc.).

## Verification Plan

### Automated Tests
- Build the app with `app:assembleDebug` to verify compilation.

### Manual Verification
- Deploy to device/emulator and test switching between system Light Mode and Dark Mode to verify that text remains readable, backgrounds adapt correctly, and UI components look professional in both modes.

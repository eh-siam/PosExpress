# Walkthrough - Fixed InflateException in QrScannerActivity

I have fixed the crash that occurred when opening the `QrScannerActivity`.

## Changes Made

### Android Manifest
- Updated the theme for [QrScannerActivity](file:///home/simec-system-android/AndroidStudioProjects/PosApplicatinJava/app/src/main/AndroidManifest.xml) from `@style/Theme.AppCompat.Light.NoActionBar` to `@style/Theme.PosExpress` to fix the `InflateException`.

### QR Scanner Logic
- Improved `extractNumber` in [QrScannerActivity.java](file:///home/simec-system-android/AndroidStudioProjects/PosApplicatinJava/app/src/main/java/com/example/posexpress/ui/QrScannerActivity.java):
    - Added support for bKash URL format (`s=` parameter extraction).
    - Added removal of non-digit characters (spaces, dashes).
    - Added handling for country code `880`.
    - Added validation to ensure a clean 11-digit number starting with `01`.
    - Added logging for easier debugging of QR scanning issues.

## Reason for the fix
The `activity_qr_scanner.xml` layout uses `com.google.android.material.button.MaterialButton` with a Material 3 style (`@style/Widget.Material3.Button.IconButton`). Material Components require a theme derived from `Theme.MaterialComponents` or `Theme.Material3` to be inflated correctly. Using an `AppCompat` theme caused the `InflateException`.

## Verification Results

### Automated Tests
- Ran `app:assembleDebug` and it completed successfully.

### Manual Verification Required
- Please deploy the app and open the QR scanner to confirm that the crash is resolved and the "Close" button is rendered correctly.

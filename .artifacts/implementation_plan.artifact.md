# Comprehensive 16 KB and API 36 Compliance Fix

This plan provides a "perfect" fix for the Google Play Console errors by updating both build configurations and critical third-party dependencies that contain native code.

## User Review Required

> [!IMPORTANT]
> I am updating **Firebase** and **CameraX** to their latest stable versions. These libraries contain the native code (`.so` files) that usually trigger the 16 KB alignment error. Updating them ensures we are using modern, compliant binaries.

## Proposed Changes

### Build Configuration
#### [MODIFY] [app/build.gradle.kts](file:///home/simec-system-android/AndroidStudioProjects/PosApplicatinJava/app/build.gradle.kts)
- **SDK Targets:** Maintain `compileSdk = 36` and `targetSdk = 36`.
- **Version:** Increment to `versionCode = 5` and `versionName = "1.4"`.
- **Packaging:** Explicitly set `jniLibs.useLegacyPackaging = false` to ensure 16 KB alignment in the final bundle.
- **Dependencies Update:**
    - Update Firebase BoM from `33.1.2` to `34.18.0`.
    - Update CameraX from `1.3.4` to `1.6.2`.

#### [MODIFY] [gradle.properties](file:///home/simec-system-android/AndroidStudioProjects/PosApplicatinJava/gradle.properties)
- Add `android.bundle.jniLibs.useLegacyPackaging=false` (to be absolutely sure the bundle tool doesn't compress libs).

### Manifest Consistency
#### [MODIFY] [AndroidManifest.xml](file:///home/simec-system-android/AndroidStudioProjects/PosApplicatinJava/app/src/main/AndroidManifest.xml)
- Confirm `android:extractNativeLibs="false"` is present in the `<application>` tag.

## Verification Plan

### Automated Build
- Run `./gradlew clean` to purge all 4 KB-aligned artifacts.
- Run `./gradlew :app:bundleRelease` to generate the new compliant bundle.

### Manual Verification
- Verify the generated AAB size.
- The new bundle will have version 1.4, allowing for a fresh upload to the Play Console.

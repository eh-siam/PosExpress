# Fix Kotlin Extension Sync Error

The project is failing to sync with the error: `Cannot add extension with name 'kotlin', as there is an extension already registered with that name.` This typically indicates that the Kotlin plugin is being applied multiple times or there is a version conflict between the Kotlin plugin and the Android Gradle Plugin (AGP), especially given the very new versions of Gradle (9.5.0) and AGP (9.3.1) being used.

## Proposed Changes

### [Component Name] Gradle Configuration

#### [MODIFY] [libs.versions.toml](file:///home/simec-system-android/AndroidStudioProjects/PosApplicatinJava/gradle/libs.versions.toml)
- Update Kotlin version from `1.9.24` to `2.0.21` to better support newer Gradle/AGP versions.

#### [MODIFY] [app/build.gradle.kts](file:///home/simec-system-android/AndroidStudioProjects/PosApplicatinJava/app/build.gradle.kts)
- Replace `id("org.jetbrains.kotlin.android") version "1.9.24"` with `alias(libs.plugins.kotlin.android)` to ensure consistency with the root build file and Version Catalog.
- Remove redundant `implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.24")` as it is automatically managed by the Kotlin plugin.

#### [MODIFY] [build.gradle.kts](file:///home/simec-system-android/AndroidStudioProjects/PosApplicatinJava/build.gradle.kts)
- Ensure all root plugins use `alias` correctly.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:help` to verify sync completes successfully.
- Run `./gradlew assembleDebug` to ensure the project builds.

### Manual Verification
- Verify that Android Studio syncs without the "Cannot add extension with name 'kotlin'" error.

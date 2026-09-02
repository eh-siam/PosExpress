# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/simec-system-android/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# Firebase specific rules
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Keep models for Firebase serialization
-keep class com.max.posexpress.model.** { *; }

# Keep your ViewModels
-keep class com.max.posexpress.viewmodel.** { *; }

# Material Components
-keep class com.google.android.material.** { *; }

# ZXing (QR Scanner)
-keep class com.google.zxing.** { *; }

# ML Kit
-keep class com.google.mlkit.** { *; }

# Shimmer
-keep class com.facebook.shimmer.** { *; }

# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ========================================
# EyeDID SDK ProGuard Rules
# ========================================
-keep class camp.visual.eyedid.** { *; }
-keepclassmembers class camp.visual.eyedid.** { *; }
-dontwarn camp.visual.eyedid.**

# ========================================
# Accessibility Service Rules
# ========================================
-keep class camp.visual.android.sdk.sample.service.accessibility.MyAccessibilityService { *; }
-keep class * extends android.accessibilityservice.AccessibilityService { *; }

# ========================================
# Foreground Service Rules
# ========================================
-keep class camp.visual.android.sdk.sample.service.tracking.GazeTrackingService { *; }

# ========================================
# Keep BuildConfig
# ========================================
-keep class com.eorua.gazecursor.BuildConfig { *; }

# ========================================
# Keep Models and Data Classes
# ========================================
-keep class camp.visual.android.sdk.sample.domain.model.** { *; }
-keepclassmembers class camp.visual.android.sdk.sample.domain.model.** { *; }

# ========================================
# Keep Interface Implementations
# ========================================
-keep interface camp.visual.android.sdk.sample.** { *; }
-keepclassmembers interface camp.visual.android.sdk.sample.** { *; }

# ========================================
# Keep Native Methods
# ========================================
-keepclasseswithmembernames class * {
    native <methods>;
}

# ========================================
# Keep Serializable Classes
# ========================================
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ========================================
# Keep Parcelable Classes
# ========================================
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ========================================
# General Android Rules
# ========================================
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# ========================================
# Crash Reporting (스택 트레이스 보존)
# ========================================
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

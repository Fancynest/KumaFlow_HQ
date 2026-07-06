# ============================================================
# KumaFlow ProGuard/R8 Rules
# Carefully written to shrink APK without breaking any feature
# ============================================================

# --- GENERAL ANDROID ---
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod

# --- KOTLINX COROUTINES ---
# Keep coroutine intrinsics (flow, withContext, etc.)
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.flow.** { *; }

# --- ROOM DATABASE (Critical: entities must not be obfuscated) ---
-keep class com.bearbones.kumaflow.data.KumaDatabase { *; }
-keep class com.bearbones.kumaflow.data.KumaDatabase_Impl { *; }
-keep class com.bearbones.kumaflow.KumaTransaction { *; }
-keep class com.bearbones.kumaflow.TransactionSplit { *; }
-keep class com.bearbones.kumaflow.TransactionWithSplits { *; }
-keep class com.bearbones.kumaflow.UserProfile { *; }
-keep class com.bearbones.kumaflow.TransactionDao { *; }
-keep class com.bearbones.kumaflow.TransactionDao$DefaultImpls { *; }
-keep class com.bearbones.kumaflow.TransactionDao_Impl { *; }

# Room generated
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# --- JSON PARSING (org.json used in backup/restore) ---
# Keep all data class fields accessible for JSONObject.put/optString
-keepclassmembers class com.bearbones.kumaflow.KumaTransaction {
    <fields>;
}
-keepclassmembers class com.bearbones.kumaflow.UserProfile {
    <fields>;
}
-keepclassmembers class com.bearbones.kumaflow.TransactionSplit {
    <fields>;
}

# --- CUSTOM UPDATE SYSTEM (UpdateChecker + UpdateManager) ---
-keep class com.bearbones.kumaflow.utils.UpdateChecker { *; }
-keep class com.bearbones.kumaflow.utils.UpdateManager { *; }
-keep class com.bearbones.kumaflow.utils.UpdateInfo { *; }
-keep class com.bearbones.kumaflow.utils.DownloadState { *; }
-keep class com.bearbones.kumaflow.utils.DownloadState$* { *; }

# --- HAZE (Kuma Glass / Glassmorphism) ---
-keep class dev.chrisbanes.haze.** { *; }
-dontwarn dev.chrisbanes.haze.**

# --- LOTTIE (bear animations) ---
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# --- COMPOSE ---
# ProfileInstaller
-keep class androidx.profileinstaller.** { *; }

# Compose stability annotations
-keep @androidx.compose.runtime.Immutable class * { *; }
-keep @androidx.compose.runtime.Stable class * { *; }

# Google Fonts provider
-keep class androidx.compose.ui.text.googlefonts.** { *; }
-dontwarn androidx.compose.ui.text.googlefonts.**

# --- BIOMETRIC ---
-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**

# --- KUMAFLOW CUSTOM COMPONENTS ---
# Keep Activity aliases for dynamic icon switching
-keep class com.bearbones.kumaflow.MainActivity { *; }
-keep class com.bearbones.kumaflow.ui.screens.LockScreen { *; }

# Widget
-keep class com.bearbones.kumaflow.KumaWidgetProvider { *; }
-keep class com.bearbones.kumaflow.KumaReminder { *; }
-keep class com.bearbones.kumaflow.KumaService { *; }

# WalletLogoManager (network + file I/O)
-keep class com.bearbones.kumaflow.WalletLogoManager { *; }

# Reorderable library
-keep class org.burnoutcrew.reorderable.** { *; }
-dontwarn org.burnoutcrew.reorderable.**

# Okio / javax.annotation (Fix for R8 Missing class)
-dontwarn javax.annotation.**
-dontwarn okio.**

# --- MISC ---
# Keep R class (resource references)
-keepclassmembers class **.R$* {
    public static <fields>;
}

# FileProvider (for backup sharing)
-keep class androidx.core.content.FileProvider { *; }

# Prevent stripping of enum values used in sealed classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Fix for Compose ModifierLocalProvider and IncompatibleClassChangeError in R8
-keep class androidx.compose.ui.** { *; }
-keep interface androidx.compose.ui.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.animation.** { *; }
-keep class androidx.compose.material3.** { *; }
-dontwarn androidx.compose.**

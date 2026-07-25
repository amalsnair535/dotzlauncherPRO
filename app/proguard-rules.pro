# General Optimizations
-allowaccessmodification
-overloadaggressively
-repackageclasses ''

# Compose rules
-keepclassmembers class androidx.compose.ui.platform.InspectableValue {
   public *** getInspectableElements();
}

# Gson rules
# Keep names for JSON serialization
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Explicitly keep data models used for serialization
-keep class com.dotz.launcherpro.data.DotzSettings { *; }
-keep class com.dotz.launcherpro.data.LauncherProfile { *; }
-keep class com.dotz.launcherpro.data.AppTile { *; }
-keep class com.dotz.launcherpro.data.DotzThemePreset { *; }

# OkHttp rules
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**

# Google Play Billing
-keep class com.android.billingclient.api.** { *; }

# AdMob
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }

# WorkManager keep rules for R8
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-keep class androidx.work.impl.model.** { *; }

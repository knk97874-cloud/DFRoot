# ProGuard / R8 rules for FreeLLM SuperPro
-keepattributes *Annotation*
-keepclassmembers class * {
    @org.jetbrains.annotations.* <fields>;
    @org.jetbrains.annotations.* <methods>;
}

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# OkHttp & Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Gson
-keepattributes Signature
-keepclassmembers class * {
    @com.google.code.gson.annotations.SerializedName <fields>;
}

# Compose
-keep class androidx.compose.material3.** { *; }

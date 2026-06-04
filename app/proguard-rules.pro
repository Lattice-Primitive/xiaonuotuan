# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line numbers for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ==================== OkHttp ====================
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ==================== Retrofit ====================
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# ==================== Gson ====================
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
# Prevent R8 from stripping Gson's generic type info
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# ==================== Room ====================
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# ==================== WebRTC ====================
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# ==================== Rokid SDK ====================
-keep class com.rokid.** { *; }
-dontwarn com.rokid.**

# ==================== App data models ====================
# Keep Room entities and DAOs
-keep class com.nanackations2.papaya.data.db.** { *; }
# Keep API models
-keep class com.nanackations2.papaya.api.** { *; }
# Keep Rokid models
-keep class com.nanackations2.papaya.rokid.RokidDevice { *; }
-keep class com.nanackations2.papaya.rokid.RokidDeviceInfo { *; }
-keep class com.nanackations2.papaya.rokid.RokidCredentials { *; }
-keep class com.nanackations2.papaya.rokid.RokidConstants { *; }

# ==================== BroadcastReceivers ====================
-keep class com.nanackations2.papaya.receiver.** { *; }

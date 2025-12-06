# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# --- General ---
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,*Annotation*
-dontwarn javax.annotation.**
-dontwarn org.jetbrains.annotations.**

# --- Kotlinx Serialization ---
-keepattributes *Annotation*, InnerClasses
-dontobfuscate
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Retrofit ---
-keepattributes Signature
-keepattributes Exceptions
# Retain service method parameters when optimizing
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okio.**
-dontwarn javax.annotation.**

# --- DanmakuFlameMaster ---
-keep class master.flame.danmaku.** { *; }

# --- Media3 / ExoPlayer ---
# Generally handled by consumer rules, but safe to keep if issues arise
#-keep class androidx.media3.** { *; }

# --- Room ---
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# --- Data Models (Keep serialized classes) ---
-keep class com.android.purebilibili.data.model.** { *; }

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
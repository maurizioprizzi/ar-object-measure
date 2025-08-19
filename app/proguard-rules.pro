# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep the line number information for debugging stack traces.
-keepattributes LineNumberTable,SourceFile
-renamesourcefileattribute SourceFile

# ARCore specific rules
-keep class com.google.ar.core.** { *; }
-keep class com.google.ar.** { *; }
-dontwarn com.google.ar.**

# Hilt rules
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp

# Keep @HiltAndroidApp classes
-keep @dagger.hilt.android.HiltAndroidApp class * {
    *;
}

# Keep Hilt generated classes
-keep class **_HiltModules { *; }
-keep class **_HiltComponents { *; }
-keep class **_Impl { *; }

# Room rules
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Compose rules
-keep class androidx.compose.** { *; }
-keep class kotlin.Metadata { *; }

# Coroutines rules
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcher {}

# CameraX rules
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Keep data classes and models
-keep class com.objectmeasure.ar.domain.model.** { *; }
-keep class com.objectmeasure.ar.data.** { *; }

# Keep classes with @Serializable annotation (if you use Kotlin Serialization)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# General Android rules
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}
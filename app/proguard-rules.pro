# =============================================================================
# Youseif Player — حماية قوية (R8 full mode)
# تصغير + تشويش أسماء + صعوبة التفكيك مع تشغيل آمن
# =============================================================================

# الحد الأدنى الضروري فقط
-keep class com.example.MainActivity { public <init>(); }
-keep class com.example.player.CastOptionsProvider { *; }
-keep class com.example.security.** { *; }

# Room
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.RoomDatabase
-keep class **_Impl { *; }

# Kotlin
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class **$EnumEntries { *; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclasseswithmembernames class * { native <methods>; }

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

-dontwarn androidx.compose.**
-dontwarn androidx.media3.**
-dontwarn com.google.android.exoplayer2.**

# Google Cast
-dontwarn com.google.android.gms.cast.**
-dontwarn com.google.android.gms.common.**
-keep class com.google.android.gms.cast.** { *; }
-keep class com.google.android.gms.common.** { *; }

-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# تشويش أقوى
-repackageclasses 'y'
-allowaccessmodification
-optimizationpasses 5
-dontusemixedcaseclassnames
-verbose
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepattributes SourceFile,LineNumberTable

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-dontwarn coil.**
-keep class coil.** { *; }

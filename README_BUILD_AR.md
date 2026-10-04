# Yousef Player Pro — v25.4-hardened (build 1)

## الإصلاحات في هذه النسخة
1. **روابط HLS/m3u8 التوكنية** (زي `.../index-v1-a1.m3u8?t=...&s=...`):
   بصمة HLS أقوى في `player/StreamResolver.kt` (`isHlsStrong`) + تمرير نوع MIME
   `APPLICATION_M3U8` في مسار الـ fallback داخل `player/YouseifPlayerController.kt`
   (قبل كده الـ sniffing كان بيفشل مع الروابط اللي ملهاش Content-Type صريح).
2. **تقليب المحتوى من الأزرار** (قنوات/أفلام/أغاني/فيديو):
   `MainActivity.dispatchKeyEvent` + `player/KeyRouter.kt` + هاندلر في `ui/MainScreen.kt`.
   الأزرار المدعومة: CHANNEL UP/DOWN، MEDIA NEXT/PREVIOUS، PAGE UP/DOWN،
   وأزرار الاتجاهات (تعمل فقط وأنت داخل المشغل حتى لا تتعارض مع تنقل الواجهة).
3. **تأخر ظهور قنوات اللايف HD**:
   `ui/MainViewModel.kt` — تحميل `livetvMostWatched()` و`loadHomeCatalog()` بالتوازي
   (`async`) مع إعطاء أولوية لقنوات اللايف، + كاش 75 ثانية في `FaselHdApi`.
4. **التسمية**: `applicationId = com.youseif.player` · `versionCode = 1` ·
   `versionName = "Yousef 25.4-hardened"` · اسم المشغل: **Yousef Player Pro**.

لم تُحذف أي ميزة — كل الشاشات والإعدادات والثيمات كما هي.

## التوقيع (signing) — جاهز من المشروع
- keystore: `app/youseif-release.jks`
- storePassword: `youseif2025` · keyAlias: `youseif` · keyPassword: `youseif2025`

## نسختان للبناء

### نسخة 1 — Full Release (موصى بها للتوزيع)
في `app/build.gradle.kts`:
```kotlin
release {
    signingConfig = signingConfigs.getByName("release")
    isMinifyEnabled = true
    isShrinkResources = true
}
```
ثم:
```bash
./gradlew clean :app:assembleRelease
# الناتج: app/build/outputs/apk/release/
```

### نسخة 2 — Lighter Release (أخف حجمًا)
فعّل R8 full mode + ABI split (سطر واحد لكل معمارية):
```kotlin
// gradle.properties
android.enableR8.fullMode=true

// app/build.gradle.kts → android { }
splits { abi { isEnable = true; reset(); include("armeabi-v7a","arm64-v8a"); isUniversalApk = false } }
```
ثم:
```bash
./gradlew clean :app:assembleRelease
# app/build/outputs/apk/release/app-arm64-v8a-release.apk  ← الأصغر
# app/build/outputs/apk/release/app-armeabi-v7a-release.apk
```

## البناء من سطر الأوامر (Linux/macOS)
```bash
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=$HOME/android-sdk
echo "sdk.dir=$ANDROID_HOME" > local.properties
chmod +x gradlew && ./gradlew clean :app:assembleRelease
```
أو ببساطة:
```bash
bash build_release.sh
```

> ملاحظة: المشروع يحتاج ذاكرة ≥ 4GB للبناء (Gradle + Kotlin + R8).

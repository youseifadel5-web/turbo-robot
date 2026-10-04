# Youseif Player Pro

Premium Android media player (IPTV / channels / films / series / radio / local media)
built with Kotlin + Jetpack Compose.

- **App id:** `com.youseif.player`
- **Version:** `25.4` (CI bumps the version code from the build number)
- **Min / target SDK:** 24 / 35

## Build the APK automatically (GitHub Actions)

Every push to `main` (or a manual run from the **Actions** tab) builds a signed
release APK and sends it to Telegram. The APK is also kept as a workflow artifact.

Workflow: `.github/workflows/build-youseif-player.yml`

### Repository secrets

Add these in **Settings → Secrets and variables → Actions**:

| Name | Purpose |
|---|---|
| `BOT_TOKEN` | Telegram bot token (never stored in the repo) |
| `ADMIN_ID` | Telegram chat id the APK is sent to |
| `YOUSEIF_KEYSTORE_BASE64` | *(optional)* the release keystore, base64-encoded — so new APKs install **over** older ones |
| `YOUSEIF_STORE_PASSWORD` | *(optional)* keystore password (default `youseif2025`) |
| `YOUSEIF_KEY_ALIAS` | *(optional)* key alias (default `youseif`) |
| `YOUSEIF_KEY_PASSWORD` | *(optional)* key password (default `youseif2025`) |

If `YOUSEIF_KEYSTORE_BASE64` is not set, CI generates a throwaway key so the build
still works — but then each APK is signed with a different key and Android will
refuse to install it over a previous build (you would have to uninstall first).
To keep the signing key stable, add the keystore secret:

```bash
base64 -w0 app/youseif-release.jks   # copy the output into YOUSEIF_KEYSTORE_BASE64
```

The keystore itself is **not** committed to the repo (see `.gitignore`).

## Build locally

Requires JDK 17 + Android SDK (platform 35, build-tools 35.0.0).

```bash
echo "sdk.dir=$HOME/Android/Sdk" > local.properties
chmod +x gradlew
./gradlew clean :app:assembleRelease
# output: app/build/outputs/apk/release/
```

or simply `bash build_release.sh`.

Put your signing key at `app/youseif-release.jks` (or point
`YOUSEIF_KEYSTORE_PATH` at it) before building a release.

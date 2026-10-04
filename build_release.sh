#!/bin/bash
# بناء نسختين Release: arm64-v8a (الأخف) + armeabi-v7a  (ABI splits)
export JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}
export ANDROID_HOME=${ANDROID_HOME:-$HOME/android-sdk}
./gradlew clean :app:assembleRelease --no-daemon
echo "النواتج: app/build/outputs/apk/release/"
ls -lh app/build/outputs/apk/release/

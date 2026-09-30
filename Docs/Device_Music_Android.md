# Android Device Music

Recommended implementation:
1. Request only the Android access required by the chosen Android version.
2. Prefer MediaStore or Storage Access Framework where appropriate.
3. Keep selected content as URI/reference instead of copying the entire library.
4. Decode/play formats supported by the chosen Unity/Android playback path.
5. Handle unsupported codecs with a clear message.
6. Keep game SFX/engine audio independent from music volume.
7. Support Bluetooth headphones/car audio through Android's normal audio routing.

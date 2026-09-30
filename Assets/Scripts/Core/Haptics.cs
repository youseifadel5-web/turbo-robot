using UnityEngine;
using NeonRush.Save;

namespace NeonRush.Core
{
    /// <summary>
    /// Vibration wrapper. Uses Android VibrationEffect with amplitude when
    /// available, falls back to Handheld.Vibrate. Respects the player's
    /// vibration setting in SaveSystem.
    /// </summary>
    public static class Haptics
    {
        private static bool enabled = true;
        private static bool initialized;

        public static bool Enabled
        {
            get { if (!initialized) Init(); return enabled; }
            set { enabled = value; }
        }

        private static void Init()
        {
            initialized = true;
            try { enabled = SaveSystem.Current != null ? SaveSystem.Current.vibration : true; }
            catch { enabled = true; }
        }

        public static void Light() => Impact(12);
        public static void Medium() => Impact(35);
        public static void Heavy() => Impact(80);

        public static void Impact(int milliseconds, int amplitude = 120)
        {
            if (!Enabled) return;
#if UNITY_ANDROID && !UNITY_EDITOR
            try
            {
                using (var unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
                using (var activity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity"))
                using (var vibrator = activity.Call<AndroidJavaObject>("getSystemService", "vibrator"))
                {
                    if (vibrator == null || !vibrator.Call<bool>("hasVibrator")) return;
                    using (var effectClass = new AndroidJavaClass("android.os.VibrationEffect"))
                    {
                        if (SystemInfo.operatingSystem.Contains("API level 2") ||
                            GetSdkInt() >= 26)
                        {
                            var effect = effectClass.CallStatic<AndroidJavaObject>(
                                "createOneShot", (long)milliseconds, Mathf.Clamp(amplitude, 1, 255));
                            vibrator.Call("vibrate", effect);
                        }
                        else
                        {
                            vibrator.Call("vibrate", (long)milliseconds);
                        }
                    }
                }
            }
            catch { Handheld.Vibrate(); }
#else
            if (Application.isMobilePlatform) Handheld.Vibrate();
#endif
        }

        private static int GetSdkInt()
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            try
            {
                using (var version = new AndroidJavaClass("android.os.Build$VERSION"))
                    return version.GetStatic<int>("SDK_INT");
            }
            catch { return 24; }
#else
            return 0;
#endif
        }
    }
}

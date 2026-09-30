using System;
using System.Collections.Generic;
using UnityEngine;

namespace NeonRush.Audio
{
    [Serializable]
    public class DeviceTrack
    {
        public string title;
        public string artist;
        public string album;
        public string uri;
        public float durationMs;
    }

    /// <summary>
    /// Real device-music playback (Android). Streams from MediaStore content
    /// URIs via the bundled Java plugin — no file copying, and system audio
    /// routing (Bluetooth headphones / car audio) is never overridden.
    /// If the player denies music access the game keeps running; the UI
    /// shows a retry message.
    /// </summary>
    public class DeviceMusicBridge : MonoBehaviour
    {
        public bool enabledByPlayer = true;
        public List<DeviceTrack> Tracks { get; private set; } = new List<DeviceTrack>();
        public int CurrentIndex { get; private set; } = -1;
        public bool HasLibrary { get; private set; }
        public bool PermissionGranted { get; private set; }

        public event Action OnLibraryLoaded;
        public event Action<string> OnTrackStarted;      // "Title — Artist"
        public event Action OnPlaybackEnded;
        public event Action OnPermissionFailed;

        private const string PluginClass = "com.neonrush.devicemusic.DeviceMusicPlugin";
        private static DeviceMusicBridge instance;

        private void Awake()
        {
            if (instance != null && instance != this) { Destroy(gameObject); return; }
            instance = this;
            gameObject.name = "DeviceMusicBridge"; // UnitySendMessage target
            DontDestroyOnLoad(gameObject);
        }

        public static DeviceMusicBridge Instance => instance;

        // ---- permission ----
        public bool CheckPermission()
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            int sdk = GetSdkInt();
            string perm = sdk >= 33
                ? "android.permission.READ_MEDIA_AUDIO"
                : "android.permission.READ_EXTERNAL_STORAGE";
            PermissionGranted = UnityEngine.Permission.HasUserAuthorizedPermission(perm);
            return PermissionGranted;
#else
            PermissionGranted = false;
            return false;
#endif
        }

        public void RequestPermission()
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            int sdk = GetSdkInt();
            string perm = sdk >= 33
                ? "android.permission.READ_MEDIA_AUDIO"
                : "android.permission.READ_EXTERNAL_STORAGE";
            UnityEngine.Permission.RequestUserPermission(perm);
#else
            Debug.Log("[DeviceMusic] Permission requests only apply on Android device.");
#endif
        }

        // ---- library ----
        public void LoadLibrary()
        {
            if (!CheckPermission())
            {
                OnPermissionFailed?.Invoke();
                return;
            }
#if UNITY_ANDROID && !UNITY_EDITOR
            try
            {
                using (var plugin = new AndroidJavaClass(PluginClass))
                {
                    string json = plugin.CallStatic<string>("queryTracks");
                    Parse(json);
                }
            }
            catch (Exception e)
            {
                Debug.LogWarning("[DeviceMusic] query failed: " + e.Message);
                HasLibrary = false;
            }
#else
            Debug.Log("[DeviceMusic] Library only available on Android device.");
#endif
        }

        private void Parse(string json)
        {
            Tracks.Clear();
            try
            {
                var root = JsonUtility.FromJson<LibraryJson>(json);
                if (root?.tracks != null)
                {
                    foreach (var t in root.tracks)
                    {
                        if (t == null || string.IsNullOrEmpty(t.uri)) continue;
                        if (t.title == null) t.title = "Unknown Title";
                        if (t.artist == null) t.artist = "Unknown Artist";
                        Tracks.Add(t);
                    }
                }
            }
            catch { }

            HasLibrary = Tracks.Count > 0;
            Debug.Log($"[DeviceMusic] {Tracks.Count} tracks found");
            OnLibraryLoaded?.Invoke();
        }

        // ---- playback ----
        public void Play(int index)
        {
            if (!HasLibrary) return;
#if UNITY_ANDROID && !UNITY_EDITOR
            try
            {
                using (var plugin = new AndroidJavaClass(PluginClass))
                    if (plugin.CallStatic<bool>("playIndex", index))
                    {
                        CurrentIndex = index;
                        var t = Tracks[index];
                        OnTrackStarted?.Invoke(t.title + " — " + t.artist);
                    }
            }
            catch (Exception e) { Debug.LogWarning("[DeviceMusic] play failed: " + e.Message); }
#endif
        }

        public void Play() { if (CurrentIndex < 0) Play(0); else Resume(); }
        public void Pause() => CallPlugin("pause");
        public void Resume() => CallPlugin("resume");
        public void Next() => CallPlugin("next");
        public void Previous() => CallPlugin("previous");
        public void Seek(float normalized)
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            try
            {
                using (var plugin = new AndroidJavaClass(PluginClass))
                {
                    int dur = plugin.CallStatic<int>("durationMs");
                    if (dur > 0) plugin.CallStatic("seekTo", Mathf.RoundToInt(normalized * dur));
                }
            }
            catch { }
#endif
        }
        public void SetShuffle(bool on) => CallPlugin("setShuffle", on);
        public void SetRepeat(bool on) => CallPlugin("setRepeatAll", on);
        public bool IsPlaying()
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            try { using (var plugin = new AndroidJavaClass(PluginClass)) return plugin.CallStatic<bool>("isPlaying"); }
            catch { return false; }
#else
            return false;
#endif
        }
        public float Position01()
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            try
            {
                using (var plugin = new AndroidJavaClass(PluginClass))
                {
                    int pos = plugin.CallStatic<int>("positionMs");
                    int dur = plugin.CallStatic<int>("durationMs");
                    return dur > 0 ? Mathf.Clamp01(pos / (float)dur) : 0f;
                }
            }
            catch { return 0f; }
#else
            return 0f;
#endif
        }

        public void Shutdown() => CallPlugin("stopAndRelease");

        private void OnDestroy()
        {
            if (instance == this) Shutdown();
        }

        // ---- callbacks from Java ----
        private void OnTrackFinished()
        {
            OnPlaybackEnded?.Invoke();
            Next();
        }

        private void OnTrackError() => Debug.LogWarning("[DeviceMusic] playback error, skipping");

        // ---- helpers ----
        private static void CallPlugin(string method, object arg = null)
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            try
            {
                using (var plugin = new AndroidJavaClass(PluginClass))
                {
                    if (arg == null) plugin.CallStatic(method);
                    else plugin.CallStatic(method, arg);
                }
            }
            catch (Exception e) { Debug.LogWarning("[DeviceMusic] " + method + " failed: " + e.Message); }
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
            catch { return 29; }
#else
            return 0;
#endif
        }

        [Serializable]
        private class LibraryJson
        {
            public DeviceTrack[] tracks;
        }
    }
}

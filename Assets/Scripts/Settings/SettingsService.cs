using UnityEngine;
using NeonRush.Save;
using NeonRush.Audio;

namespace NeonRush.Settings
{
    /// <summary>
    /// One entry point for all runtime settings, persisted through SaveSystem
    /// (not the old PlayerPrefs GameSettings path). Binds audio, fps, quality
    /// and haptics together so every screen stays consistent.
    /// </summary>
    public static class SettingsService
    {
        public static string[] GraphicsNames = { "LOW", "MEDIUM", "HIGH", "ULTRA" };
        public static readonly int[] FpsOptions = { 30, 60, 90, 120 };
        public static string[] ControlNames = { "BUTTONS", "WHEEL", "TILT" };
        public static string[] DamageNames = { "OFF", "VISUAL", "FULL" };

        public static void ApplyAll()
        {
            var d = SaveSystem.Load();
            ApplyAudio(d);
            ApplyFps(d);
            ApplyGraphics(d);
            Core.Haptics.Enabled = d.vibration;
        }

        public static void ApplyAudio(SaveData d)
        {
            var bus = AudioBus.Instance;
            if (bus == null) return;
            bus.master = d.masterVolume;
            bus.music = d.musicVolume;
            bus.sfx = d.sfxVolume;
            bus.engine = d.engineVolume > 0f ? d.engineVolume : 1f;
        }

        public static void ApplyFps(SaveData d)
        {
            Application.targetFrameRate = d.targetFps;
            QualitySettings.vSyncCount = 0;
        }

        public static void ApplyGraphics(SaveData d)
        {
            var look = Object.FindObjectOfType<Performance.UrpLookApplier>();
            if (look != null)
            {
                look.Apply((Performance.GraphicsTier)Mathf.Clamp(d.graphicsLevel, 0, 3));
            }
            else
            {
                QualitySettings.SetQualityLevel(Mathf.Clamp(d.graphicsLevel, 0, 3), true);
            }
        }

        // ---- mutators (save + apply together) ----
        public static void SetGraphics(int tier)
        {
            var d = SaveSystem.Current; d.graphicsLevel = Mathf.Clamp(tier, 0, 3);
            SaveSystem.Save(); ApplyGraphics(d);
        }
        public static void CycleGraphics()
        {
            var d = SaveSystem.Current;
            SetGraphics((d.graphicsLevel + 1) % 4);
        }
        public static void SetFps(int fps)
        {
            var d = SaveSystem.Current; d.targetFps = fps;
            SaveSystem.Save(); ApplyFps(d);
        }
        public static void CycleFps()
        {
            var d = SaveSystem.Current;
            int i = 0;
            for (int k = 0; k < FpsOptions.Length; k++)
                if (FpsOptions[k] == d.targetFps) { i = k; break; }
            SetFps(FpsOptions[(i + 1) % FpsOptions.Length]);
        }
        public static void SetVibration(bool on)
        {
            var d = SaveSystem.Current; d.vibration = on;
            SaveSystem.Save(); Core.Haptics.Enabled = on;
        }
        public static void SetControlScheme(int scheme)
        {
            var d = SaveSystem.Current; d.controlScheme = Mathf.Clamp(scheme, 0, 2);
            SaveSystem.Save();
        }
        public static void SetSensitivity(float v)
        {
            var d = SaveSystem.Current; d.steeringSensitivity = Mathf.Clamp(v, 0.3f, 2f);
            SaveSystem.Save();
        }
        public static void SetMaster(float v) { var d = SaveSystem.Current; d.masterVolume = v; SaveSystem.Save(); ApplyAudio(d); }
        public static void SetMusic(float v) { var d = SaveSystem.Current; d.musicVolume = v; SaveSystem.Save(); ApplyAudio(d); }
        public static void SetSfx(float v)    { var d = SaveSystem.Current; d.sfxVolume = v; SaveSystem.Save(); ApplyAudio(d); }
        public static void SetEngine(float v) { var d = SaveSystem.Current; d.engineVolume = v; SaveSystem.Save(); ApplyAudio(d); }
    }
}

using System.Collections.Generic;
using UnityEngine;

namespace NeonRush.Audio
{
    /// <summary>
    /// Lazy-loading registry for all game audio. Clips live under
    /// Assets/Resources/Audio (Engine/SFX/Ambience). If a clip is missing
    /// the caller falls back to procedural tones — never crashes.
    /// </summary>
    public static class AudioClipRegistry
    {
        private static readonly Dictionary<string, AudioClip> cache = new Dictionary<string, AudioClip>();

        public static AudioClip Get(string key)
        {
            if (string.IsNullOrEmpty(key)) return null;
            if (cache.TryGetValue(key, out var clip)) return clip;

            clip = Resources.Load<AudioClip>("Audio/" + key);
            if (clip == null)
            {
                // try trimmed name (some Unity import setups drop folder prefix)
                var alt = key.Contains("/") ? key.Substring(key.IndexOf('/') + 1) : key;
                clip = Resources.Load<AudioClip>("Audio/" + alt);
            }
            cache[key] = clip;
            return clip;
        }

        /// <summary>Engine layer for a car class, e.g. ("S", "high").</summary>
        public static AudioClip Engine(Cars.CarClass carClass, string layer)
            => Get("Engine/engine_" + carClass + "_" + layer);

        public static AudioClip Turbo => Get("Engine/engine_turbo");
        public static AudioClip NitroLoop => Get("Engine/engine_nitro");
    }
}

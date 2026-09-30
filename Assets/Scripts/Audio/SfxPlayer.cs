using System.Collections.Generic;
using UnityEngine;

namespace NeonRush.Audio
{
    /// <summary>
    /// Pooled one-shot SFX player. Volumes route through AudioBus so the
    /// Settings sliders and crash-ducking affect everything.
    /// Usage: SfxPlayer.Play("SFX/sfx_ui_click", 0.8f);
    /// </summary>
    public static class SfxPlayer
    {
        private const int PoolSize = 10;
        private static readonly List<AudioSource> pool = new List<AudioSource>();
        private static GameObject host;
        private static int next;

        public static void Play(string key, float volume = 1f, float pitch = 1f, Vector3? worldPos = null)
        {
            var clip = AudioClipRegistry.Get(key);
            if (clip == null) return;

            var src = NextSource();
            if (src == null) return;

            float busVol = AudioBus.Instance != null ? AudioBus.Instance.SfxOut : 1f;
            src.transform.position = worldPos ?? (host != null ? host.transform.position : Vector3.zero);
            src.spatialBlend = worldPos.HasValue ? 1f : 0f;
            src.pitch = pitch;
            src.volume = Mathf.Clamp01(volume * busVol);
            src.clip = clip;
            src.Play();
        }

        /// <summary>UI sounds use the UI bus volume.</summary>
        public static void PlayUi(string key, float volume = 1f)
        {
            var clip = AudioClipRegistry.Get(key);
            if (clip == null) return;
            var src = NextSource();
            if (src == null) return;
            float busVol = AudioBus.Instance != null ? AudioBus.Instance.ui * AudioBus.Instance.master : 1f;
            src.spatialBlend = 0f;
            src.pitch = 1f;
            src.volume = Mathf.Clamp01(volume * busVol);
            src.clip = clip;
            src.Play();
        }

        private static AudioSource NextSource()
        {
            EnsureHost();
            for (int i = 0; i < PoolSize; i++)
            {
                var s = pool[next];
                next = (next + 1) % PoolSize;
                if (!s.isPlaying) return s;
            }
            return pool[next]; // steal oldest slot position
        }

        private static void EnsureHost()
        {
            if (host != null) return;
            host = new GameObject("SfxPlayerPool");
            Object.DontDestroyOnLoad(host);
            for (int i = 0; i < PoolSize; i++)
            {
                var go = new GameObject("sfx" + i);
                go.transform.SetParent(host.transform, false);
                var src = go.AddComponent<AudioSource>();
                src.playOnAwake = false;
                src.loop = false;
                src.rolloffMode = AudioRolloffMode.Linear;
                src.maxDistance = 90f;
                pool.Add(src);
            }
        }
    }
}

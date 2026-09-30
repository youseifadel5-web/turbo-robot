using UnityEngine;
using NeonRush.Cars;

namespace NeonRush.Audio
{
    /// <summary>
    /// Layered engine audio: idle / low / mid / high / nitro.
    /// Uses assigned clips if present; otherwise generates procedural placeholder tones.
    /// </summary>
    [RequireComponent(typeof(AudioSource))]
    public class EngineAudioController : MonoBehaviour
    {
        public ArcadeCarController car;
        public AudioBus bus;

        [Header("Optional real clips (leave empty for procedural)")]
        public AudioClip idleClip;
        public AudioClip lowClip;
        public AudioClip midClip;
        public AudioClip highClip;
        public AudioClip nitroClip;

        [Header("Mix")]
        public float maxDistance = 80f;
        public float spatialBlend = 0.35f;

        private AudioSource idleSrc, lowSrc, midSrc, highSrc, nitroSrc;
        private float smoothRpm;

        private void Start()
        {
            if (car == null) car = GetComponentInParent<ArcadeCarController>() ?? GetComponent<ArcadeCarController>();

            // Real WAV bank first (Assets/Resources/Audio/Engine), procedural tones only as fallback.
            string cls = car != null && car.stats != null ? car.stats.carClass.ToString() : "B";
            idleClip = idleClip ?? AudioClipRegistry.Get("Engine/engine_" + cls + "_idle");
            lowClip = lowClip ?? AudioClipRegistry.Get("Engine/engine_" + cls + "_low");
            midClip = midClip ?? AudioClipRegistry.Get("Engine/engine_" + cls + "_mid");
            highClip = highClip ?? AudioClipRegistry.Get("Engine/engine_" + cls + "_high");
            nitroClip = nitroClip ?? AudioClipRegistry.Get("Engine/engine_nitro");

            idleSrc = CreateLayer("Idle", idleClip ?? MakeTone(55f, 0.35f));
            lowSrc = CreateLayer("Low", lowClip ?? MakeTone(90f, 0.4f));
            midSrc = CreateLayer("Mid", midClip ?? MakeTone(140f, 0.45f));
            highSrc = CreateLayer("High", highClip ?? MakeTone(210f, 0.5f));
            nitroSrc = CreateLayer("Nitro", nitroClip ?? MakeTone(280f, 0.55f));
            nitroSrc.volume = 0f;
        }

        private AudioSource CreateLayer(string name, AudioClip clip)
        {
            var go = new GameObject("Engine_" + name);
            go.transform.SetParent(transform, false);
            var src = go.AddComponent<AudioSource>();
            src.clip = clip;
            src.loop = true;
            src.playOnAwake = true;
            src.spatialBlend = spatialBlend;
            src.maxDistance = maxDistance;
            src.rolloffMode = AudioRolloffMode.Linear;
            src.dopplerLevel = 0.4f;
            src.volume = 0f;
            src.Play();
            return src;
        }

        private void Update()
        {
            if (car == null) return;

            float speedNorm = Mathf.Clamp01(car.SpeedKmh / 280f);
            float throttleBoost = 0.15f; // no direct throttle read; approximate from accel intent via nitro/speed
            float rpm = Mathf.Clamp01(speedNorm * 0.85f + (car.IsNitroActive ? 0.2f : 0f));
            smoothRpm = Mathf.Lerp(smoothRpm, rpm, 1f - Mathf.Exp(-8f * Time.deltaTime));

            float engVol = bus != null ? bus.engine * bus.master : 1f;

            SetLayer(idleSrc, Curve(smoothRpm, 0f, 0.25f), 0.85f + smoothRpm * 0.2f, engVol);
            SetLayer(lowSrc, Curve(smoothRpm, 0.1f, 0.45f), 0.9f + smoothRpm * 0.3f, engVol);
            SetLayer(midSrc, Curve(smoothRpm, 0.35f, 0.7f), 1.0f + smoothRpm * 0.35f, engVol);
            SetLayer(highSrc, Curve(smoothRpm, 0.55f, 1f), 1.05f + smoothRpm * 0.45f, engVol);

            float n = car.IsNitroActive ? 1f : 0f;
            nitroSrc.volume = Mathf.Lerp(nitroSrc.volume, n * 0.7f * engVol, Time.deltaTime * 10f);
            nitroSrc.pitch = 1.1f + smoothRpm * 0.4f;
        }

        private static void SetLayer(AudioSource src, float weight, float pitch, float busVol)
        {
            if (src == null) return;
            src.volume = Mathf.Lerp(src.volume, weight * busVol * 0.55f, Time.deltaTime * 8f);
            src.pitch = pitch;
        }

        private static float Curve(float t, float a, float b)
        {
            if (t <= a) return 0f;
            if (t >= b) return Mathf.Clamp01(1f - (t - b) * 1.5f);
            return Mathf.SmoothStep(0f, 1f, (t - a) / Mathf.Max(0.01f, b - a));
        }

        /// <summary>Simple looping tone as placeholder until real engine WAVs are assigned.</summary>
        public static AudioClip MakeTone(float freq, float amp)
        {
            int sampleRate = 22050;
            int samples = sampleRate; // 1 second loop
            var clip = AudioClip.Create("proc_" + freq, samples, 1, sampleRate, false);
            var data = new float[samples];
            for (int i = 0; i < samples; i++)
            {
                float t = (float)i / sampleRate;
                // engine-ish: base + harmonics + light noise
                float s = Mathf.Sin(2f * Mathf.PI * freq * t) * 0.55f
                        + Mathf.Sin(2f * Mathf.PI * freq * 2f * t) * 0.25f
                        + Mathf.Sin(2f * Mathf.PI * freq * 3.01f * t) * 0.12f;
                s += (Random.value * 2f - 1f) * 0.04f;
                data[i] = s * amp;
            }
            clip.SetData(data, 0);
            return clip;
        }
    }
}

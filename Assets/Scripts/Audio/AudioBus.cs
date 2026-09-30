using UnityEngine;

namespace NeonRush.Audio
{
    /// <summary>
    /// Simple bus volumes. Hook UI sliders here. Optional: mirror to AudioMixer later.
    /// </summary>
    public class AudioBus : MonoBehaviour
    {
        public static AudioBus Instance { get; private set; }

        [Range(0f, 1f)] public float master = 1f;
        [Range(0f, 1f)] public float music = 0.7f;
        [Range(0f, 1f)] public float sfx = 1f;
        [Range(0f, 1f)] public float engine = 1f;
        [Range(0f, 1f)] public float environment = 0.8f;
        [Range(0f, 1f)] public float ui = 0.85f;

        [Header("Ducking")]
        public float crashDuckMusic = 0.55f;
        public float duckRecoverSpeed = 2f;
        private float musicDuck = 1f;

        private void Awake()
        {
            Instance = this;
        }

        public void SetMaster(float v) => master = Mathf.Clamp01(v);
        public void SetMusic(float v) => music = Mathf.Clamp01(v);
        public void SetSfx(float v) => sfx = Mathf.Clamp01(v);
        public void SetEngine(float v) => engine = Mathf.Clamp01(v);

        public float MusicOut => music * master * musicDuck;
        public float SfxOut => sfx * master;
        public float EngineOut => engine * master;

        public void PulseCrashDuck()
        {
            musicDuck = Mathf.Min(musicDuck, crashDuckMusic);
        }

        private void Update()
        {
            musicDuck = Mathf.MoveTowards(musicDuck, 1f, duckRecoverSpeed * Time.deltaTime);
            AudioListener.volume = master;
        }
    }
}

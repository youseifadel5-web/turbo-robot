using UnityEngine;
using NeonRush.Cars;

namespace NeonRush.Environment
{
    public enum WeatherType
    {
        Clear,
        Cloudy,
        Rain,
        HeavyRain,
        Fog,
        Snow
    }

    /// <summary>
    /// Weather that affects visuals AND grip (not just a filter).
    /// </summary>
    public class WeatherController : MonoBehaviour
    {
        public WeatherType current = WeatherType.Clear;
        public ParticleSystem rainFx;
        public ParticleSystem snowFx;
        public Light sunLight;
        public Color clearAmbient = new Color(0.4f, 0.45f, 0.55f);
        public Color rainAmbient = new Color(0.25f, 0.28f, 0.32f);
        public float fogDensityClear = 0.002f;
        public float fogDensityRain = 0.012f;
        public float fogDensityFog = 0.035f;

        [Header("Physics impact")]
        public ArcadeCarController[] affectedCars;

        public void SetWeather(WeatherType type)
        {
            current = type;
            Apply();
        }

        public void Apply()
        {
            float grip = 1f;
            float fog = fogDensityClear;
            Color amb = clearAmbient;
            bool rain = false;
            bool snow = false;

            switch (current)
            {
                case WeatherType.Clear:
                    grip = 1f; fog = fogDensityClear; amb = clearAmbient;
                    break;
                case WeatherType.Cloudy:
                    grip = 0.98f; fog = 0.005f; amb = clearAmbient * 0.85f;
                    break;
                case WeatherType.Rain:
                    grip = 0.82f; fog = fogDensityRain; amb = rainAmbient; rain = true;
                    break;
                case WeatherType.HeavyRain:
                    grip = 0.68f; fog = fogDensityRain * 1.4f; amb = rainAmbient * 0.8f; rain = true;
                    break;
                case WeatherType.Fog:
                    grip = 0.9f; fog = fogDensityFog; amb = rainAmbient;
                    break;
                case WeatherType.Snow:
                    grip = 0.55f; fog = 0.02f; amb = new Color(0.6f, 0.65f, 0.7f); snow = true;
                    break;
            }

            RenderSettings.ambientLight = amb;
            RenderSettings.fog = fog > 0.003f;
            RenderSettings.fogDensity = fog;

            if (rainFx != null)
            {
                if (rain) rainFx.Play(); else rainFx.Stop();
            }
            if (snowFx != null)
            {
                if (snow) snowFx.Play(); else snowFx.Stop();
            }

            if (affectedCars != null)
            {
                foreach (var c in affectedCars)
                    if (c != null) c.SetGripMultiplier(grip);
            }
        }

        private void Start() => Apply();
    }
}

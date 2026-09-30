using UnityEngine;

namespace NeonRush.Environment
{
    /// <summary>
    /// Simple day/night cycle or fixed presets for racing.
    /// </summary>
    public class DayNightController : MonoBehaviour
    {
        public Light sun;
        public Light[] neonLights;
        public Gradient skyColor;
        public AnimationCurve sunIntensity = AnimationCurve.Linear(0f, 0.05f, 1f, 1.1f);

        [Range(0f, 1f)] public float timeOfDay = 0.85f; // 0 = midnight, 0.5 = noon, default night for neon
        public bool autoCycle = false;
        public float cycleHoursPerSecond = 0.02f;

        private void Update()
        {
            if (autoCycle)
            {
                timeOfDay += cycleHoursPerSecond * Time.deltaTime;
                if (timeOfDay > 1f) timeOfDay -= 1f;
            }
            Apply();
        }

        public void SetNight() { timeOfDay = 0.88f; Apply(); }
        public void SetDay() { timeOfDay = 0.45f; Apply(); }
        public void SetSunset() { timeOfDay = 0.72f; Apply(); }

        public void Apply()
        {
            if (sun != null)
            {
                float angle = timeOfDay * 360f - 90f;
                sun.transform.rotation = Quaternion.Euler(angle, 30f, 0f);
                sun.intensity = sunIntensity.Evaluate(timeOfDay);
                bool night = timeOfDay < 0.22f || timeOfDay > 0.78f;
                sun.enabled = !night || sun.intensity > 0.08f;
            }

            bool neonOn = timeOfDay < 0.28f || timeOfDay > 0.72f;
            if (neonLights != null)
            {
                foreach (var l in neonLights)
                    if (l != null) l.enabled = neonOn;
            }
        }
    }
}

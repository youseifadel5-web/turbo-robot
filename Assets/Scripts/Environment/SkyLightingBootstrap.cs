using UnityEngine;

namespace NeonRush.Environment
{
    /// <summary>
    /// Night neon sky + fog + key lights without external skybox textures.
    /// </summary>
    public class SkyLightingBootstrap : MonoBehaviour
    {
        public bool forceNight = true;
        public Tracks.RaceTimeOfDay timeOfDay = Tracks.RaceTimeOfDay.Night;
        public Color skyZenith = new Color(0.02f, 0.03f, 0.08f);
        public Color skyHorizon = new Color(0.12f, 0.05f, 0.18f);
        public Color fogColor = new Color(0.05f, 0.06f, 0.1f);
        public float fogDensity = 0.008f;

        /// <summary>Applies lighting for a race preset (night neon / sunset / dawn / day).</summary>
        public void SetTimeOfDay(Tracks.RaceTimeOfDay value)
        {
            timeOfDay = forceNight ? Tracks.RaceTimeOfDay.Night : value;
            switch (timeOfDay)
            {
                case Tracks.RaceTimeOfDay.Sunset:
                    skyZenith = new Color(0.16f, 0.10f, 0.30f);
                    skyHorizon = new Color(1.00f, 0.45f, 0.20f);
                    fogColor = new Color(0.45f, 0.22f, 0.20f);
                    fogDensity = 0.006f;
                    break;
                case Tracks.RaceTimeOfDay.Dawn:
                    skyZenith = new Color(0.22f, 0.28f, 0.48f);
                    skyHorizon = new Color(1.00f, 0.72f, 0.45f);
                    fogColor = new Color(0.40f, 0.42f, 0.52f);
                    fogDensity = 0.007f;
                    break;
                case Tracks.RaceTimeOfDay.Day:
                    skyZenith = new Color(0.36f, 0.58f, 0.92f);
                    skyHorizon = new Color(0.78f, 0.86f, 0.96f);
                    fogColor = new Color(0.68f, 0.74f, 0.84f);
                    fogDensity = 0.0035f;
                    break;
                default: // Night neon
                    skyZenith = new Color(0.02f, 0.03f, 0.08f);
                    skyHorizon = new Color(0.12f, 0.05f, 0.18f);
                    fogColor = new Color(0.05f, 0.06f, 0.1f);
                    fogDensity = 0.008f;
                    break;
            }
        }

        public void Apply()
        {
            RenderSettings.fog = true;
            RenderSettings.fogMode = FogMode.ExponentialSquared;
            RenderSettings.fogColor = fogColor;
            RenderSettings.fogDensity = fogDensity;
            RenderSettings.ambientMode = UnityEngine.Rendering.AmbientMode.Flat;
            bool day = timeOfDay == Tracks.RaceTimeOfDay.Day;
            bool sunsetish = timeOfDay == Tracks.RaceTimeOfDay.Sunset || timeOfDay == Tracks.RaceTimeOfDay.Dawn;
            RenderSettings.ambientLight = day ? new Color(0.62f, 0.66f, 0.75f)
                                  : sunsetish ? new Color(0.42f, 0.34f, 0.38f)
                                  : new Color(0.18f, 0.2f, 0.32f);
            RenderSettings.subtractiveShadowColor = new Color(0.05f, 0.05f, 0.08f);

            // Solid camera background as sky substitute
            if (Camera.main != null)
            {
                Camera.main.clearFlags = CameraClearFlags.SolidColor;
                Camera.main.backgroundColor = Color.Lerp(skyHorizon, skyZenith, 0.55f);
                Camera.main.farClipPlane = 350f;
            }

            // Directional "moon"
            var sun = RenderSettings.sun;
            if (sun == null)
            {
                var existing = Object.FindObjectOfType<Light>();
                if (existing != null && existing.type == LightType.Directional) sun = existing;
            }
            if (sun == null)
            {
                var go = new GameObject("MoonLight");
                sun = go.AddComponent<Light>();
                sun.type = LightType.Directional;
            }
            switch (timeOfDay)
            {
                case Tracks.RaceTimeOfDay.Day:
                    sun.color = new Color(1f, 0.97f, 0.9f); sun.intensity = 1.35f;
                    sun.transform.rotation = Quaternion.Euler(55f, -35f, 0f);
                    break;
                case Tracks.RaceTimeOfDay.Sunset:
                    sun.color = new Color(1f, 0.55f, 0.3f); sun.intensity = 1.0f;
                    sun.transform.rotation = Quaternion.Euler(8f, -60f, 0f);
                    break;
                case Tracks.RaceTimeOfDay.Dawn:
                    sun.color = new Color(1f, 0.75f, 0.55f); sun.intensity = 0.85f;
                    sun.transform.rotation = Quaternion.Euler(12f, 140f, 0f);
                    break;
                default:
                    sun.color = new Color(0.55f, 0.65f, 1f);
                    sun.intensity = forceNight ? 0.35f : 1.1f;
                    sun.transform.rotation = Quaternion.Euler(28f, -30f, 0f);
                    break;
            }
            sun.shadows = LightShadows.Soft;
            RenderSettings.sun = sun;

            // Ambient neon fill light (unshadowed) — night only
            EnsureFill("CyanFill", new Vector3(12f, 8f, 0f), new Color(0.2f, 0.9f, 1f),
                timeOfDay == Tracks.RaceTimeOfDay.Night ? 1.1f : 0.25f);
            EnsureFill("MagentaFill", new Vector3(-12f, 6f, 15f), new Color(1f, 0.2f, 0.7f),
                timeOfDay == Tracks.RaceTimeOfDay.Night ? 0.8f : 0.18f);

            DynamicGI.UpdateEnvironment();
            Debug.Log("[SkyLighting] Night neon lighting applied");
        }

        private void EnsureFill(string name, Vector3 pos, Color c, float intensity)
        {
            var t = transform.Find(name);
            Light l;
            if (t == null)
            {
                var go = new GameObject(name);
                go.transform.SetParent(transform, false);
                l = go.AddComponent<Light>();
            }
            else l = t.GetComponent<Light>();

            l.type = LightType.Point;
            l.range = 40f;
            l.color = c;
            l.intensity = intensity;
            l.shadows = LightShadows.None;
            l.transform.localPosition = pos;
        }

        private void Start()
        {
            if (forceNight) Apply();
        }
    }
}

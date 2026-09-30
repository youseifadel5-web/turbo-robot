using UnityEngine;

namespace NeonRush.Cars
{
    /// <summary>
    /// Shockwave Nitro visual + camera punch + exhaust flames.
    /// </summary>
    public class NitroVFXController : MonoBehaviour
    {
        public ArcadeCarController car;
        public ChaseCamera chaseCamera;

        [Header("Exhaust Flames")]
        public ParticleSystem[] exhaustFlames;
        public Light[] exhaustLights;
        public float flameIdle = 0.3f;
        public float flameNitro = 1.4f;

        [Header("Shockwave")]
        public ParticleSystem shockwavePrefab;
        public float cameraPunch = 0.22f;

        [Header("Screen Edge (optional UI)")]
        public CanvasGroup nitroVignette;
        public float vignettePeak = 0.35f;

        private void OnEnable()
        {
            if (car == null) car = GetComponentInParent<ArcadeCarController>();
            if (car != null)
            {
                car.OnNitroStarted += PlayShockwave;
            }
        }

        private void OnDisable()
        {
            if (car != null) car.OnNitroStarted -= PlayShockwave;
        }

        private void Update()
        {
            bool active = car != null && car.IsNitroActive;
            float target = active ? flameNitro : flameIdle;

            if (exhaustFlames != null)
            {
                foreach (var ps in exhaustFlames)
                {
                    if (ps == null) continue;
                    var em = ps.emission;
                    em.rateOverTime = target * 40f;
                    if (active && !ps.isPlaying) ps.Play();
                }
            }

            if (exhaustLights != null)
            {
                foreach (var l in exhaustLights)
                {
                    if (l == null) continue;
                    l.intensity = Mathf.Lerp(l.intensity, active ? 3.5f : 0.4f, Time.deltaTime * 10f);
                    l.color = active ? new Color(0.2f, 0.9f, 1f) : new Color(1f, 0.4f, 0.1f);
                }
            }

            if (nitroVignette != null)
            {
                float targetA = active ? vignettePeak : 0f;
                nitroVignette.alpha = Mathf.Lerp(nitroVignette.alpha, targetA, Time.deltaTime * 8f);
            }
        }

        private void PlayShockwave()
        {
            if (chaseCamera != null)
            {
                chaseCamera.AddPunch(Vector3.forward * cameraPunch);
                chaseCamera.AddShake(0.35f);
            }
            if (shockwavePrefab != null)
            {
                var fx = Instantiate(shockwavePrefab, transform.position, Quaternion.identity);
                Destroy(fx.gameObject, 2f);
            }
        }
    }
}

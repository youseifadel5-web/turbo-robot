using UnityEngine;
using NeonRush.Cars;

namespace NeonRush.Environment
{
    /// <summary>
    /// Attaches runtime tire smoke + nitro flames and drives them from car state.
    /// </summary>
    public class CarVfxBinder : MonoBehaviour
    {
        public ArcadeCarController car;
        public float smokeRateWhenDrifting = 35f;

        private ParticleSystem smokeL;
        private ParticleSystem smokeR;
        private ParticleSystem flameL;
        private ParticleSystem flameR;
        private ParticleSystem shockwave;

        private void Start()
        {
            if (car == null) car = GetComponentInParent<ArcadeCarController>();
            if (car == null) car = GetComponent<ArcadeCarController>();

            smokeL = VfxFactory.CreateTireSmoke(transform, new Vector3(-0.85f, 0.1f, -1.3f));
            smokeR = VfxFactory.CreateTireSmoke(transform, new Vector3(0.85f, 0.1f, -1.3f));
            flameL = VfxFactory.CreateNitroFlame(transform, new Vector3(-0.35f, 0.28f, -2.15f));
            flameR = VfxFactory.CreateNitroFlame(transform, new Vector3(0.35f, 0.28f, -2.15f));
            shockwave = VfxFactory.CreateShockwaveBurst(transform);

            if (car != null)
                car.OnNitroStarted += () => { if (shockwave != null) shockwave.Play(); };
        }

        private void Update()
        {
            if (car == null) return;

            float smoke = car.IsDrifting ? smokeRateWhenDrifting : 0f;
            SetRate(smokeL, smoke);
            SetRate(smokeR, smoke);

            float flame = car.IsNitroActive ? 50f : 0f;
            SetRate(flameL, flame);
            SetRate(flameR, flame);
            if (car.IsNitroActive)
            {
                if (!flameL.isPlaying) flameL.Play();
                if (!flameR.isPlaying) flameR.Play();
            }
        }

        private static void SetRate(ParticleSystem ps, float rate)
        {
            if (ps == null) return;
            var e = ps.emission;
            e.rateOverTime = rate;
            if (rate > 0.1f && !ps.isPlaying) ps.Play();
            if (rate <= 0.1f && ps.isPlaying && ps.main.loop)
            {
                // keep system, just zero emission
            }
        }
    }
}

using UnityEngine;
using NeonRush.Cars;

namespace NeonRush.Racing
{
    public enum DamageMode { Off, Visual, Full }

    /// <summary>
    /// Collision damage: Off / Visual (scruffed paint + smoke) / Full
    /// (visual + performance penalties). Haptics + crash SFX included.
    /// </summary>
    [RequireComponent(typeof(Rigidbody))]
    public class DamageSystem : MonoBehaviour
    {
        public DamageMode mode = DamageMode.Visual;
        public float impactThreshold = 3f;
        public float maxDamage = 100f;
        public float visualStart = 15f;
        public float smokeStart = 45f;

        public float Damage { get; private set; }
        public float DamageNormalized => Damage / Mathf.Max(1f, maxDamage);

        private ArcadeCarController car;
        private Renderer bodyRenderer;
        private Color originalPaint = Color.white;
        private ParticleSystem smoke;
        private float baseAccel, baseMaxSpeed, baseGrip;
        private bool storedBase;

        private void Awake()
        {
            car = GetComponent<ArcadeCarController>();
            var rb = GetComponent<Rigidbody>();
            if (rb != null) rb.collisionDetectionMode = CollisionDetectionMode.Continuous;
            var rends = GetComponentsInChildren<Renderer>();
            if (rends.Length > 0)
            {
                bodyRenderer = rends[0];
                originalPaint = bodyRenderer.material.color; // instance material
            }
        }

        private void OnCollisionEnter(Collision collision)
        {
            if (mode == DamageMode.Off || car == null) return;

            float impulse = 0f;
            for (int i = 0; i < collision.contactCount; i++)
                impulse += collision.GetImpulse().magnitude;

            if (impulse < impactThreshold) return;

            float dmg = impulse * 2.2f;
            Damage = Mathf.Min(maxDamage, Damage + dmg);

            Audio.SfxPlayer.Play("SFX/sfx_collision_soft", Mathf.Clamp01(impulse / 20f));
            Core.Haptics.Impact(Mathf.RoundToInt(Mathf.Clamp(impulse * 5f, 20f, 90f)));
            Audio.AudioBus.Instance?.PulseCrashDuck();

            if (impulse > 12f)
            {
                Audio.SfxPlayer.Play("SFX/sfx_crash", Mathf.Clamp01(impulse / 30f));
                Core.Haptics.Impact(140);
            }

            ApplyVisual();
            ApplyPerformance();
        }

        private void ApplyVisual()
        {
            if (mode == DamageMode.Off) return;
            if (Damage < visualStart || bodyRenderer == null) return;

            float t = Mathf.Clamp01((Damage - visualStart) / maxDamage);
            Color scuffed = Color.Lerp(originalPaint, new Color(0.16f, 0.16f, 0.17f), t * 0.7f);
            bodyRenderer.material.color = scuffed;

            if (Damage >= smokeStart && smoke == null)
            {
                smoke = Environment.VfxFactory.CreateTireSmoke(transform, new Vector3(0f, 0.7f, -2f));
                smoke.transform.localScale = Vector3.one * 0.6f;
            }
        }

        private void ApplyPerformance()
        {
            if (mode != DamageMode.Full || car == null) return;
            if (!storedBase)
            {
                baseAccel = car.acceleration;
                baseMaxSpeed = car.maxSpeed;
                baseGrip = car.lateralGrip;
                storedBase = true;
            }
            float health = 1f - DamageNormalized;
            car.acceleration = baseAccel * Mathf.Lerp(0.65f, 1f, health);
            car.maxSpeed = baseMaxSpeed * Mathf.Lerp(0.8f, 1f, health);
            car.lateralGrip = baseGrip * Mathf.Lerp(0.85f, 1f, health);
        }

        public void SetMode(DamageMode m)
        {
            mode = m;
            if (m == DamageMode.Off)
            {
                Damage = 0f;
                if (bodyRenderer != null) bodyRenderer.material.color = originalPaint;
                if (smoke != null) { Destroy(smoke.gameObject); smoke = null; }
                if (storedBase && car != null)
                {
                    car.acceleration = baseAccel;
                    car.maxSpeed = baseMaxSpeed;
                    car.lateralGrip = baseGrip;
                }
            }
        }

        public void ResetDamage() => SetMode(mode);
    }
}

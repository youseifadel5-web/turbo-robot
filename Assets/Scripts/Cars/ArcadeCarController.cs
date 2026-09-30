using UnityEngine;

namespace NeonRush.Cars
{
    /// <summary>
    /// Arcade-style car controller with real drift, nitro shockwave hooks, and stats-driven physics.
    /// Designed for mobile + gamepad.
    /// </summary>
    [RequireComponent(typeof(Rigidbody))]
    public class ArcadeCarController : MonoBehaviour
    {
        [Header("Stats Source")]
        public CarStats stats;

        [Header("Driving (overridden by stats if assigned)")]
        public float acceleration = 28f;
        public float reverseAcceleration = 12f;
        public float maxSpeed = 55f;          // m/s (~198 km/h default)
        public float steeringPower = 75f;
        public float brakePower = 35f;
        public float lateralGrip = 8f;

        [Header("Nitro")]
        public float nitroForce = 22f;
        public float nitroCapacity = 100f;
        public float nitroDrainPerSecond = 28f;
        public float nitroRechargePerSecond = 8f;

        [Header("Drift")]
        public float driftGripMultiplier = 0.35f;
        public float driftSteerMultiplier = 1.25f;
        public float minDriftSpeed = 5f;

        [Header("Runtime Events")]
        public System.Action OnNitroStarted;
        public System.Action OnNitroEnded;
        public System.Action<float> OnDriftScoreTick; // angle * speed factor

        public float SpeedKmh => rb != null ? rb.linearVelocity.magnitude * 3.6f : 0f;
        public float SpeedMs => rb != null ? rb.linearVelocity.magnitude : 0f;
        public float NitroNormalized => nitro / Mathf.Max(1f, nitroCapacity);
        /// <summary>Current steering input -1..1 (for cockpit wheel, HUD).</summary>
        public float SteeringInput => steering;
        public bool IsDrifting { get; private set; }
        public bool IsNitroActive { get; private set; }
        public float DriftAngle { get; private set; }
        public int CurrentGear { get; private set; } = 1;

        private Rigidbody rb;
        private float throttle;
        private float steering;
        private float brake;
        private bool nitroPressed;
        private bool driftPressed;
        private float nitro;
        private float currentGrip;
        private bool wasNitroActive;

        private void Awake()
        {
            rb = GetComponent<Rigidbody>();
            ApplyStats();
            nitro = nitroCapacity;
            if (rb != null)
                rb.centerOfMass = new Vector3(0f, -0.45f, 0f);
        }

        public void ApplyStats()
        {
            if (stats == null) return;
            acceleration = stats.acceleration;
            reverseAcceleration = stats.reverseAcceleration;
            maxSpeed = stats.maxSpeedKmh / 3.6f;
            brakePower = stats.brakePower;
            steeringPower = stats.steeringPower;
            lateralGrip = stats.lateralGrip;
            driftGripMultiplier = stats.driftGripMultiplier;
            nitroCapacity = stats.nitroCapacity;
            nitroForce = stats.nitroForce;
            nitroDrainPerSecond = stats.nitroDrainPerSecond;
            nitroRechargePerSecond = stats.nitroRechargePerSecond;
            if (rb != null) rb.mass = stats.mass;
            nitro = Mathf.Min(nitro, nitroCapacity);
        }

        public void SetInput(float steer, float gas, float brakeInput, bool drift, bool boost)
        {
            steering = Mathf.Clamp(steer, -1f, 1f);
            throttle = Mathf.Clamp01(gas);
            brake = Mathf.Clamp01(brakeInput);
            driftPressed = drift;
            nitroPressed = boost;
        }

        private void FixedUpdate()
        {
            if (rb == null) return;

            Vector3 forward = transform.forward;
            float speed = rb.linearVelocity.magnitude;
            Vector3 localVelocity = transform.InverseTransformDirection(rb.linearVelocity);

            // Throttle / Brake
            if (throttle > 0.01f)
                rb.AddForce(forward * acceleration * throttle, ForceMode.Acceleration);
            if (brake > 0.01f)
                rb.AddForce(-forward * brakePower * brake, ForceMode.Acceleration);

            // Steering (speed-sensitive)
            float speedFactor = Mathf.Clamp01(speed / 6f);
            float steer = steering * steeringPower * (0.35f + speedFactor * 0.65f);
            if (driftPressed) steer *= driftSteerMultiplier;
            rb.AddTorque(Vector3.up * steer, ForceMode.Acceleration);

            // Lateral grip / Drift
            currentGrip = driftPressed ? lateralGrip * driftGripMultiplier : lateralGrip;
            localVelocity.x = Mathf.Lerp(localVelocity.x, 0f, currentGrip * Time.fixedDeltaTime);
            rb.linearVelocity = transform.TransformDirection(localVelocity);

            // Nitro
            IsNitroActive = nitroPressed && nitro > 0.5f && throttle > 0.1f;
            if (IsNitroActive)
            {
                rb.AddForce(forward * nitroForce, ForceMode.Acceleration);
                nitro -= nitroDrainPerSecond * Time.fixedDeltaTime;
                if (!wasNitroActive)
                {
                    OnNitroStarted?.Invoke();
                    wasNitroActive = true;
                }
            }
            else
            {
                if (wasNitroActive)
                {
                    OnNitroEnded?.Invoke();
                    wasNitroActive = false;
                }
                nitro += nitroRechargePerSecond * Time.fixedDeltaTime;
            }
            nitro = Mathf.Clamp(nitro, 0f, nitroCapacity);

            // Soft speed limit
            if (rb.linearVelocity.magnitude > maxSpeed)
                rb.linearVelocity = rb.linearVelocity.normalized * maxSpeed;

            // Drift detection + angle
            float sideSpeed = Mathf.Abs(localVelocity.x);
            IsDrifting = driftPressed && sideSpeed > 0.6f && speed > minDriftSpeed;
            if (IsDrifting)
            {
                DriftAngle = Vector3.SignedAngle(forward, rb.linearVelocity.normalized, Vector3.up);
                float scoreTick = Mathf.Abs(DriftAngle) * (speed * 0.15f) * Time.fixedDeltaTime;
                OnDriftScoreTick?.Invoke(scoreTick);
            }
            else
            {
                DriftAngle = 0f;
            }

            CurrentGear = GetGear(SpeedKmh);
        }

        private int GetGear(float kmh)
        {
            if (kmh < 20f) return 1;
            if (kmh < 45f) return 2;
            if (kmh < 75f) return 3;
            if (kmh < 110f) return 4;
            if (kmh < 150f) return 5;
            return 6;
        }

        /// <summary>Call from weather system to temporarily reduce grip (rain).</summary>
        public void SetGripMultiplier(float multiplier)
        {
            // Applied next frame via currentGrip; store as override if needed
            lateralGrip = (stats != null ? stats.lateralGrip : 8f) * Mathf.Clamp(multiplier, 0.4f, 1.2f);
        }
    }
}

using UnityEngine;

namespace NeonRush.Cars
{
    /// <summary>
    /// Touch + keyboard + future gamepad bridge.
    /// UI buttons call SetSteering / SetThrottle / SetBrake / SetDrift / SetNitro.
    /// </summary>
    public class MobileCarInput : MonoBehaviour
    {
        public ArcadeCarController car;

        [Header("Assist")]
        [Range(0f, 1f)] public float steeringAssist = 0.15f;
        public bool autoAccelInTouchDrive = false;

        [Header("Runtime Input")]
        [Range(-1f, 1f)] public float steering;
        [Range(0f, 1f)] public float throttle;
        [Range(0f, 1f)] public float brake;
        public bool drift;
        public bool nitro;

        [Header("Sensitivity")]
        [Range(0.5f, 2f)] public float steeringSensitivity = 1f;

        private void Reset()
        {
            car = GetComponent<ArcadeCarController>();
        }

        private void Update()
        {
            if (car == null) return;

            // Keyboard fallback (Editor + Bluetooth keyboards)
            float kbSteer = Input.GetAxisRaw("Horizontal");
            float kbVert = Input.GetAxisRaw("Vertical");
            float kbThrottle = Mathf.Clamp01(kbVert);
            float kbBrake = kbVert < -0.1f ? -kbVert : (Input.GetKey(KeyCode.Space) ? 1f : 0f);
            bool kbDrift = Input.GetKey(KeyCode.LeftShift);
            bool kbNitro = Input.GetKey(KeyCode.LeftControl);

            float finalSteer = Mathf.Abs(steering) > 0.01f ? steering : kbSteer;
            finalSteer *= steeringSensitivity;
            finalSteer = Mathf.Clamp(finalSteer, -1f, 1f);

            // Light steering assist toward velocity direction at high speed
            if (steeringAssist > 0.01f && car.SpeedKmh > 30f)
            {
                Vector3 vel = car.GetComponent<Rigidbody>().linearVelocity;
                if (vel.sqrMagnitude > 1f)
                {
                    float angle = Vector3.SignedAngle(car.transform.forward, vel.normalized, Vector3.up);
                    finalSteer = Mathf.Lerp(finalSteer, Mathf.Clamp(angle / 40f, -1f, 1f), steeringAssist * 0.25f);
                }
            }

            float finalThrottle = Mathf.Max(throttle, kbThrottle);
            if (autoAccelInTouchDrive && throttle < 0.01f && kbThrottle < 0.01f)
                finalThrottle = 0.55f; // optional soft auto-accel

            float finalBrake = Mathf.Max(brake, kbBrake);

            car.SetInput(
                finalSteer,
                finalThrottle,
                finalBrake,
                drift || kbDrift,
                nitro || kbNitro
            );
        }

        // --- Called by UI ---
        public void SetSteering(float value) => steering = Mathf.Clamp(value, -1f, 1f);
        public void SetThrottle(float value) => throttle = Mathf.Clamp01(value);
        public void SetBrake(float value) => brake = Mathf.Clamp01(value);
        public void SetDrift(bool value) => drift = value;
        public void SetNitro(bool value) => nitro = value;

        public void SteerLeft(bool pressed) => steering = pressed ? -1f : (steering < 0 ? 0f : steering);
        public void SteerRight(bool pressed) => steering = pressed ? 1f : (steering > 0 ? 0f : steering);
        public void ThrottlePressed(bool pressed) => throttle = pressed ? 1f : 0f;
        public void BrakePressed(bool pressed) => brake = pressed ? 1f : 0f;
    }
}

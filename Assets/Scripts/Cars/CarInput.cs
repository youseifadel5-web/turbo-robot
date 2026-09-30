using UnityEngine;

namespace NeonRush.Cars
{
    public class CarInput : MonoBehaviour
    {
        [Range(-1f, 1f)] public float steering;
        [Range(0f, 1f)] public float throttle;
        [Range(0f, 1f)] public float brake;
        public bool nitro;
        public bool handbrake;

        public void SetSteering(float value) => steering = Mathf.Clamp(value, -1f, 1f);
        public void SetThrottle(float value) => throttle = Mathf.Clamp01(value);
        public void SetBrake(float value) => brake = Mathf.Clamp01(value);
        public void SetNitro(bool value) => nitro = value;
        public void SetHandbrake(bool value) => handbrake = value;
    }
}

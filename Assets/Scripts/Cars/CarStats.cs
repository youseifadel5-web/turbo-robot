using UnityEngine;

namespace NeonRush.Cars
{
    /// <summary>
    /// Data-driven car performance. Upgrades modify these values and they feed the physics controller.
    /// </summary>
    [CreateAssetMenu(menuName = "NeonRush/Car Stats", fileName = "CarStats_")]
    public class CarStats : ScriptableObject
    {
        public string carId = "falcon_s";
        public string displayName = "Falcon S";
        public CarClass carClass = CarClass.S;

        [Header("Display Ratings 0-100")]
        [Range(0, 100)] public float topSpeedRating = 80;
        [Range(0, 100)] public float accelerationRating = 75;
        [Range(0, 100)] public float handlingRating = 70;
        [Range(0, 100)] public float brakingRating = 72;
        [Range(0, 100)] public float driftRating = 65;
        [Range(0, 100)] public float nitroRating = 70;

        [Header("Physics Values")]
        public float maxSpeedKmh = 280f;
        public float acceleration = 28f;
        public float reverseAcceleration = 12f;
        public float brakePower = 38f;
        public float steeringPower = 78f;
        public float lateralGrip = 9f;
        public float driftGripMultiplier = 0.32f;
        public float mass = 1280f;

        [Header("Nitro")]
        public float nitroCapacity = 100f;
        public float nitroForce = 24f;
        public float nitroDrainPerSecond = 26f;
        public float nitroRechargePerSecond = 7f;

        [Header("Visual")]
        public Color classGlowColor = new Color(0f, 0.94f, 1f, 0.45f);
        public bool hasUnderglow = true;
    }

    public enum CarClass
    {
        B = 0,
        A = 1,
        S = 2
    }
}

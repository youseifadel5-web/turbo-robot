using UnityEngine;
using NeonRush.Save;

namespace NeonRush.Cars
{
    public enum UpgradeType { Engine, Turbo, Transmission, Brakes, Suspension, Tires, Nitro, Weight }

    /// <summary>
    /// Upgrades change REAL physics values on CarStats + live ArcadeCarController.
    /// Not cosmetic numbers.
    /// </summary>
    public class CarUpgradeSystem : MonoBehaviour
    {
        public ArcadeCarController car;
        public CarStats baseStats;
        public CarStats runtimeStats;

        [Range(0, 10)] public int engineLevel;
        [Range(0, 10)] public int turboLevel;
        [Range(0, 10)] public int transmissionLevel;
        [Range(0, 10)] public int brakesLevel;
        [Range(0, 10)] public int suspensionLevel;
        [Range(0, 10)] public int tiresLevel;
        [Range(0, 10)] public int nitroLevel;
        [Range(0, 10)] public int weightLevel;

        public int CostForNext(UpgradeType type)
        {
            int lvl = GetLevel(type);
            return 400 + lvl * 350;
        }

        public int GetLevel(UpgradeType type)
        {
            switch (type)
            {
                case UpgradeType.Engine: return engineLevel;
                case UpgradeType.Turbo: return turboLevel;
                case UpgradeType.Transmission: return transmissionLevel;
                case UpgradeType.Brakes: return brakesLevel;
                case UpgradeType.Suspension: return suspensionLevel;
                case UpgradeType.Tires: return tiresLevel;
                case UpgradeType.Nitro: return nitroLevel;
                case UpgradeType.Weight: return weightLevel;
                default: return 0;
            }
        }

        public bool TryUpgrade(UpgradeType type)
        {
            int cost = CostForNext(type);
            if (!SaveSystem.SpendCoins(cost)) return false;
            SetLevel(type, GetLevel(type) + 1);
            ApplyToCar();
            Persist();
            return true;
        }

        public void ApplyUpgrade(UpgradeType type, int level)
        {
            SetLevel(type, Mathf.Clamp(level, 0, 10));
            ApplyToCar();
        }

        public void ApplyToCar()
        {
            if (baseStats == null) return;
            if (runtimeStats == null)
                runtimeStats = ScriptableObject.Instantiate(baseStats);

            CopyBase();

            runtimeStats.acceleration += engineLevel * 1.15f + turboLevel * 0.7f;
            runtimeStats.maxSpeedKmh += engineLevel * 4.5f + transmissionLevel * 3.2f;
            runtimeStats.brakePower += brakesLevel * 1.4f;
            runtimeStats.steeringPower += suspensionLevel * 1.1f + tiresLevel * 0.8f;
            runtimeStats.lateralGrip += tiresLevel * 0.18f + suspensionLevel * 0.08f;
            runtimeStats.nitroCapacity += nitroLevel * 8f;
            runtimeStats.nitroForce += nitroLevel * 1.1f + turboLevel * 0.4f;
            runtimeStats.mass = Mathf.Max(900f, baseStats.mass - weightLevel * 22f);

            runtimeStats.accelerationRating = Mathf.Clamp(baseStats.accelerationRating + engineLevel * 2.2f + turboLevel * 1.4f, 0, 100);
            runtimeStats.topSpeedRating = Mathf.Clamp(baseStats.topSpeedRating + engineLevel * 1.6f + transmissionLevel * 1.8f, 0, 100);
            runtimeStats.brakingRating = Mathf.Clamp(baseStats.brakingRating + brakesLevel * 2.4f, 0, 100);
            runtimeStats.handlingRating = Mathf.Clamp(baseStats.handlingRating + suspensionLevel * 2f + tiresLevel * 1.6f, 0, 100);
            runtimeStats.nitroRating = Mathf.Clamp(baseStats.nitroRating + nitroLevel * 2.5f, 0, 100);

            if (car != null)
            {
                car.stats = runtimeStats;
                car.ApplyStats();
            }
        }

        private void CopyBase()
        {
            runtimeStats.maxSpeedKmh = baseStats.maxSpeedKmh;
            runtimeStats.acceleration = baseStats.acceleration;
            runtimeStats.brakePower = baseStats.brakePower;
            runtimeStats.steeringPower = baseStats.steeringPower;
            runtimeStats.lateralGrip = baseStats.lateralGrip;
            runtimeStats.nitroCapacity = baseStats.nitroCapacity;
            runtimeStats.nitroForce = baseStats.nitroForce;
            runtimeStats.mass = baseStats.mass;
            runtimeStats.accelerationRating = baseStats.accelerationRating;
            runtimeStats.topSpeedRating = baseStats.topSpeedRating;
            runtimeStats.brakingRating = baseStats.brakingRating;
            runtimeStats.handlingRating = baseStats.handlingRating;
            runtimeStats.nitroRating = baseStats.nitroRating;
        }

        private void SetLevel(UpgradeType type, int level)
        {
            level = Mathf.Clamp(level, 0, 10);
            switch (type)
            {
                case UpgradeType.Engine: engineLevel = level; break;
                case UpgradeType.Turbo: turboLevel = level; break;
                case UpgradeType.Transmission: transmissionLevel = level; break;
                case UpgradeType.Brakes: brakesLevel = level; break;
                case UpgradeType.Suspension: suspensionLevel = level; break;
                case UpgradeType.Tires: tiresLevel = level; break;
                case UpgradeType.Nitro: nitroLevel = level; break;
                case UpgradeType.Weight: weightLevel = level; break;
            }
        }

        private void Persist()
        {
            var data = SaveSystem.Current;
            if (data.ownedCars == null) return;
            string id = baseStats != null ? baseStats.carId : data.selectedCarId;
            foreach (var c in data.ownedCars)
            {
                if (c.carId != id) continue;
                c.engineLevel = engineLevel;
                c.turboLevel = turboLevel;
                c.brakesLevel = brakesLevel;
                c.nitroLevel = nitroLevel;
            }
            SaveSystem.Save();
        }
    }
}

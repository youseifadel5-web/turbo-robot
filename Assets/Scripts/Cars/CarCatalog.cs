using System.Collections.Generic;
using UnityEngine;
using NeonRush.Save;

namespace NeonRush.Cars
{
    /// <summary>
    /// Central data-driven car catalog. Adding a car = one entry here + an OBJ
    /// (optional — falls back to procedural). Keeps SaveSystem ids stable.
    /// </summary>
    public static class CarCatalog
    {
        public class CarDefinition
        {
            public string carId;
            public string displayName;
            public CarClass carClass;
            public int priceCoins;
            public string defaultPaintHex;
            public string defaultNeonHex;

            public float topSpeedRating, accelerationRating, handlingRating,
                  brakingRating, driftRating, nitroRating;
            public float maxSpeedKmh, acceleration, reverseAcceleration, brakePower,
                  steeringPower, lateralGrip, driftGripMultiplier, mass;
            public float nitroCapacity, nitroForce, nitroDrainPerSecond, nitroRechargePerSecond;
        }

        public static readonly List<CarDefinition> All = new List<CarDefinition>
        {
            new CarDefinition { carId = "falcon_s", displayName = "Falcon S", carClass = CarClass.B, priceCoins = 0,
                defaultPaintHex = "#0A0C10", defaultNeonHex = "#00F0FF",
                topSpeedRating = 74, accelerationRating = 68, handlingRating = 72, brakingRating = 70, driftRating = 66, nitroRating = 70,
                maxSpeedKmh = 265, acceleration = 26, reverseAcceleration = 11, brakePower = 36, steeringPower = 78,
                lateralGrip = 9, driftGripMultiplier = 0.32f, mass = 1280,
                nitroCapacity = 100, nitroForce = 23, nitroDrainPerSecond = 26, nitroRechargePerSecond = 7 },
            new CarDefinition { carId = "vortex_gt", displayName = "Vortex GT", carClass = CarClass.A, priceCoins = 32000,
                defaultPaintHex = "#12121A", defaultNeonHex = "#FF2BD6",
                topSpeedRating = 82, accelerationRating = 78, handlingRating = 78, brakingRating = 75, driftRating = 74, nitroRating = 76,
                maxSpeedKmh = 295, acceleration = 30, reverseAcceleration = 12, brakePower = 40, steeringPower = 80,
                lateralGrip = 10, driftGripMultiplier = 0.30f, mass = 1320,
                nitroCapacity = 110, nitroForce = 26, nitroDrainPerSecond = 25, nitroRechargePerSecond = 8 },
            new CarDefinition { carId = "titan_x", displayName = "Titan X", carClass = CarClass.A, priceCoins = 45000,
                defaultPaintHex = "#3A0B0B", defaultNeonHex = "#FF6A00",
                topSpeedRating = 86, accelerationRating = 88, handlingRating = 60, brakingRating = 62, driftRating = 84, nitroRating = 74,
                maxSpeedKmh = 285, acceleration = 34, reverseAcceleration = 13, brakePower = 34, steeringPower = 72,
                lateralGrip = 8, driftGripMultiplier = 0.26f, mass = 1650,
                nitroCapacity = 100, nitroForce = 24, nitroDrainPerSecond = 24, nitroRechargePerSecond = 7 },
            new CarDefinition { carId = "aurora_r", displayName = "Aurora R", carClass = CarClass.S, priceCoins = 90000,
                defaultPaintHex = "#E8E8F2", defaultNeonHex = "#B44CFF",
                topSpeedRating = 95, accelerationRating = 92, handlingRating = 90, brakingRating = 86, driftRating = 80, nitroRating = 92,
                maxSpeedKmh = 330, acceleration = 36, reverseAcceleration = 14, brakePower = 44, steeringPower = 84,
                lateralGrip = 11, driftGripMultiplier = 0.34f, mass = 1240,
                nitroCapacity = 130, nitroForce = 30, nitroDrainPerSecond = 24, nitroRechargePerSecond = 9 },
            new CarDefinition { carId = "nomad_x", displayName = "Nomad X", carClass = CarClass.B, priceCoins = 26000,
                defaultPaintHex = "#0E2418", defaultNeonHex = "#7CFF4D",
                topSpeedRating = 66, accelerationRating = 60, handlingRating = 64, brakingRating = 68, driftRating = 58, nitroRating = 62,
                maxSpeedKmh = 235, acceleration = 22, reverseAcceleration = 12, brakePower = 38, steeringPower = 70,
                lateralGrip = 8.5f, driftGripMultiplier = 0.30f, mass = 1900,
                nitroCapacity = 90, nitroForce = 21, nitroDrainPerSecond = 28, nitroRechargePerSecond = 6 }
        };

        public static CarDefinition Get(string carId) =>
            All.Find(c => c.carId == carId) ?? All[0];

        public static bool IsOwned(string carId)
        {
            var data = SaveSystem.Current;
            return data.ownedCars.Exists(o => o.carId == carId);
        }

        public static bool TryBuy(string carId)
        {
            var def = Get(carId);
            if (IsOwned(carId)) return true;
            if (!SaveSystem.SpendCoins(def.priceCoins)) return false;
            SaveSystem.Current.ownedCars.Add(new OwnedCarSave
            {
                carId = def.carId,
                paintId = "default",
                rimId = "stock"
            });
            SaveSystem.Save();
            return true;
        }

        public static OwnedCarSave GetOwned(string carId)
        {
            var o = SaveSystem.Current.ownedCars.Find(x => x.carId == carId);
            if (o == null)
            {
                o = new OwnedCarSave { carId = carId, paintId = "default", rimId = "stock" };
                SaveSystem.Current.ownedCars.Add(o);
            }
            return o;
        }

        /// <summary>Builds a runtime CarStats (ScriptableObject instance) for a car.</summary>
        public static CarStats CreateStats(string carId)
        {
            var d = Get(carId);
            var s = ScriptableObject.CreateInstance<CarStats>();
            s.carId = d.carId;
            s.displayName = d.displayName;
            s.carClass = d.carClass;
            s.topSpeedRating = d.topSpeedRating;
            s.accelerationRating = d.accelerationRating;
            s.handlingRating = d.handlingRating;
            s.brakingRating = d.brakingRating;
            s.driftRating = d.driftRating;
            s.nitroRating = d.nitroRating;
            s.maxSpeedKmh = d.maxSpeedKmh;
            s.acceleration = d.acceleration;
            s.reverseAcceleration = d.reverseAcceleration;
            s.brakePower = d.brakePower;
            s.steeringPower = d.steeringPower;
            s.lateralGrip = d.lateralGrip;
            s.driftGripMultiplier = d.driftGripMultiplier;
            s.mass = d.mass;
            s.nitroCapacity = d.nitroCapacity;
            s.nitroForce = d.nitroForce;
            s.nitroDrainPerSecond = d.nitroDrainPerSecond;
            s.nitroRechargePerSecond = d.nitroRechargePerSecond;
            if (ColorUtility.TryParseHtmlString(d.defaultNeonHex, out var glow))
                s.classGlowColor = new Color(glow.r, glow.g, glow.b, 0.45f);
            return s;
        }

        // ---- customization catalogs ----
        public static readonly (string id, string name, string hex)[] Paints =
        {
            ("default", "Factory", "#0A0C10"),
            ("midnight", "Midnight", "#101826"),
            ("crimson", "Crimson", "#8E1220"),
            ("solar", "Solar Flare", "#FF7A18"),
            ("acid", "Acid Lime", "#7CFF4D"),
            ("aqua", "Aqua Shock", "#00F0FF"),
            ("violet", "Violet Haze", "#B44CFF"),
            ("magenta", "Hot Magenta", "#FF2BD6"),
            ("pearl", "Pearl White", "#E8E8F2"),
            ("graphite", "Graphite", "#3A3F4A"),
            ("sand", "Dune Sand", "#C9B48A"),
            ("teal", "Deep Teal", "#0E5F63")
        };

        public static readonly (string id, string name, string hex)[] Rims =
        {
            ("stock", "Stock Silver", "#9AA0AA"),
            ("black", "Stealth Black", "#15161A"),
            ("gold", "Gold Rush", "#E5B54A"),
            ("chrome", "Chrome", "#DCE4F0"),
            ("neon", "Neon Core", "#00F0FF")
        };

        public static readonly (string id, string name, string hex)[] NeonColors =
        {
            ("cyan", "Cyan", "#00F0FF"),
            ("magenta", "Magenta", "#FF2BD6"),
            ("green", "Toxic Green", "#7CFF4D"),
            ("orange", "Blaze Orange", "#FF6A00"),
            ("purple", "Ultraviolet", "#B44CFF"),
            ("red", "Redline", "#FF2038")
        };

        public static Color HexToColor(string hex)
        {
            if (!hex.StartsWith("#")) hex = "#" + hex;
            return ColorUtility.TryParseHtmlString(hex, out var c) ? c : Color.white;
        }

        public static string PaintHex(string paintId)
        {
            foreach (var p in Paints) if (p.id == paintId) return p.hex;
            return Paints[0].hex;
        }

        public static string RimHex(string rimId)
        {
            foreach (var r in Rims) if (r.id == rimId) return r.hex;
            return Rims[0].hex;
        }
    }
}

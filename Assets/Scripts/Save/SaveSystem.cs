using System;
using System.Collections.Generic;
using UnityEngine;

namespace NeonRush.Save
{
    [Serializable]
    public class OwnedCarSave
    {
        public string carId;
        public int engineLevel;
        public int turboLevel;
        public int brakesLevel;
        public int nitroLevel;
        public string paintId;
        public string rimId;
        public string neonId;
    }

    [Serializable]
    public class SaveData
    {
        public int level = 1;
        public int xp = 0;
        public int coins = 2500;
        public int premiumCurrency = 0;
        public int careerChapter = 1;
        public int careerEvent = 1;
        public int careerStars = 0;
        public string selectedCarId = "falcon_s";
        public List<OwnedCarSave> ownedCars = new List<OwnedCarSave>();
        public float bestTime = 0f;
        public float bestDrift = 0f;
        public int graphicsLevel = 2;
        public int targetFps = 60;
        public float masterVolume = 1f;
        public float musicVolume = 0.7f;
        public float sfxVolume = 1f;
        public float engineVolume = 1f;
        public bool vibration = true;
        public float steeringSensitivity = 1f;
        public int controlScheme = 0; // 0 buttons, 1 wheel, 2 tilt
        public int language = 0; // 0 EN, 1 AR
        public bool musicShuffle = false;
        public bool musicRepeat = false;
        public bool premiumUnlocked = false;
        public List<TrackBest> trackBests = new List<TrackBest>();
    }

    [Serializable]
    public class TrackBest
    {
        public string trackId;
        public float bestTime;
    }

    public static class SaveSystem
    {
        private const string Key = "NeonRush_Save_v2";

        public static SaveData Current { get; private set; } = new SaveData();

        public static void Save(SaveData data = null)
        {
            if (data != null) Current = data;
            PlayerPrefs.SetString(Key, JsonUtility.ToJson(Current));
            PlayerPrefs.Save();
        }

        public static SaveData Load()
        {
            if (!PlayerPrefs.HasKey(Key))
            {
                Current = new SaveData();
                EnsureStarterCar();
                return Current;
            }
            Current = JsonUtility.FromJson<SaveData>(PlayerPrefs.GetString(Key)) ?? new SaveData();
            EnsureStarterCar();
            return Current;
        }

        public static void AddCoins(int amount)
        {
            Current.coins = Mathf.Max(0, Current.coins + amount);
            Save();
        }

        public static bool SpendCoins(int amount)
        {
            if (Current.coins < amount) return false;
            Current.coins -= amount;
            Save();
            return true;
        }

        public static void AddXp(int amount)
        {
            Current.xp += Mathf.Max(0, amount);
            while (Current.xp >= XpToNext(Current.level))
            {
                Current.xp -= XpToNext(Current.level);
                Current.level++;
            }
            Save();
        }

        public static int XpToNext(int level) => 200 + level * 75;

        public static void RecordRace(float time, float driftScore)
        {
            if (Current.bestTime <= 0f || time < Current.bestTime) Current.bestTime = time;
            if (driftScore > Current.bestDrift) Current.bestDrift = driftScore;
            Save();
        }

        private static void EnsureStarterCar()
        {
            if (Current.ownedCars == null) Current.ownedCars = new List<OwnedCarSave>();
            if (Current.ownedCars.Count == 0)
            {
                Current.ownedCars.Add(new OwnedCarSave { carId = "falcon_s", paintId = "default", rimId = "stock" });
                Current.selectedCarId = "falcon_s";
            }
        }

        public static void DeleteSave()
        {
            PlayerPrefs.DeleteKey(Key);
            Current = new SaveData();
            EnsureStarterCar();
        }
    }
}

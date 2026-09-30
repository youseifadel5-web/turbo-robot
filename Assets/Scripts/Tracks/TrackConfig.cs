using UnityEngine;

namespace NeonRush.Tracks
{
    public enum TrackLengthPreset
    {
        Short = 0,   // ~2-4 km
        Medium = 1,  // ~5-8 km
        Long = 2,    // ~10-20 km
        Extreme = 3, // map max
        Custom = 4
    }

    public enum TrackEnvironment
    {
        CityNight,
        MountainCoast,
        Tunnel,
        Bridge,
        Industrial,
        Desert,
        RainCity,
        Snow
    }

    public enum RaceTimeOfDay
    {
        Night = 0,
        Sunset = 1,
        Dawn = 2,
        Day = 3
    }

    /// <summary>
    /// Data for building a race: length preset, laps, environment, weather.
    /// </summary>
    [CreateAssetMenu(menuName = "NeonRush/Track Config", fileName = "TrackConfig_")]
    public class TrackConfig : ScriptableObject
    {
        public string trackId = "neon_city_01";
        public string displayName = "Neon City Freeway";
        public TrackEnvironment environment = TrackEnvironment.CityNight;

        [Header("Length")]
        public TrackLengthPreset lengthPreset = TrackLengthPreset.Medium;
        [Tooltip("Used when preset = Custom")]
        public float customTargetKm = 6f;
        public int laps = 3;

        [Header("Gameplay")]
        public int checkpointCount = 8;
        public bool hasTraffic = true;
        public int trafficDensity = 1; // 0 low, 1 med, 2 high
        public bool allowNitro = true;
        public bool scoringDrift = true;
        [Tooltip("Sky / sun / ambient preset for this race")]
        public RaceTimeOfDay timeOfDay = RaceTimeOfDay.Night;

        [Header("Estimated distance (km) after build")]
        public float builtDistanceKm;

        public float GetTargetDistanceKm()
        {
            switch (lengthPreset)
            {
                case TrackLengthPreset.Short: return 3f;
                case TrackLengthPreset.Medium: return 6.5f;
                case TrackLengthPreset.Long: return 14f;
                case TrackLengthPreset.Extreme: return 22f;
                case TrackLengthPreset.Custom: return Mathf.Max(1f, customTargetKm);
                default: return 6f;
            }
        }
    }
}

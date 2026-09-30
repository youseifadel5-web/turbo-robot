using UnityEngine;

namespace NeonRush.Career
{
    /// <summary>
    /// All career chapters/events (data-driven, offline). CareerSystem
    /// consumes this; adding a chapter = add a block here.
    /// </summary>
    public static class CareerCatalog
    {
        public static int TotalChapters => 3;

        public static CareerEvent[] GetEvents(int chapter)
        {
            switch (Mathf.Clamp(chapter, 1, TotalChapters))
            {
                case 1: return Chapter1;
                case 2: return Chapter2;
                case 3: return Chapter3;
                default: return Chapter1;
            }
        }

        static CareerEvent Ev(string id, string title, CareerEventType type, int chapter,
            Tracks.TrackLengthPreset length, int laps, int coins, int xp) =>
            new CareerEvent
            {
                id = id, title = title, type = type, chapter = chapter,
                length = length, laps = laps, rewardCoins = coins, rewardXp = xp
            };

        static readonly CareerEvent[] Chapter1 =
        {
            Ev("c1e1", "Neon Warmup",      CareerEventType.Race,       1, Tracks.TrackLengthPreset.Short,  2,  600,  30),
            Ev("c1e2", "First Drift",      CareerEventType.Drift,     1, Tracks.TrackLengthPreset.Short,  1,  700,  35),
            Ev("c1e3", "Time Attack",     CareerEventType.TimeTrial, 1, Tracks.TrackLengthPreset.Medium, 3,  900,  45),
            Ev("c1e4", "Checkpoint Rush",  CareerEventType.Checkpoint,1, Tracks.TrackLengthPreset.Medium, 1,  850,  40),
            Ev("c1e5", "Street Boss",     CareerEventType.BossRace,   1, Tracks.TrackLengthPreset.Medium, 3, 1500,  80)
        };

        static readonly CareerEvent[] Chapter2 =
        {
            Ev("c2e1", "Downtown Rush",    CareerEventType.Race,       2, Tracks.TrackLengthPreset.Medium, 3, 1000,  50),
            Ev("c2e2", "Slalom Master",    CareerEventType.Drift,     2, Tracks.TrackLengthPreset.Medium, 2, 1100,  55),
            Ev("c2e3", "Clockwork",       CareerEventType.TimeTrial, 2, Tracks.TrackLengthPreset.Long,   2, 1300,  65),
            Ev("c2e4", "Gate Runner",     CareerEventType.Checkpoint, 2, Tracks.TrackLengthPreset.Long,   1, 1250,  60),
            Ev("c2e5", "Highway Boss",    CareerEventType.BossRace,   2, Tracks.TrackLengthPreset.Long,   3, 2100, 110)
        };

        static readonly CareerEvent[] Chapter3 =
        {
            Ev("c3e1", "Neon Grand Prix",  CareerEventType.Race,       3, Tracks.TrackLengthPreset.Long,    3, 1500,  75),
            Ev("c3e2", "Total Slide",      CareerEventType.Drift,     3, Tracks.TrackLengthPreset.Long,    2, 1600,  80),
            Ev("c3e3", "Redline Trial",   CareerEventType.TimeTrial,  3, Tracks.TrackLengthPreset.Extreme,2, 1900,  95),
            Ev("c3e4", "Marathon Gates",  CareerEventType.Checkpoint, 3, Tracks.TrackLengthPreset.Extreme, 1, 1800,  90),
            Ev("c3e5", "City Legend",     CareerEventType.BossRace,   3, Tracks.TrackLengthPreset.Extreme, 3, 3200, 160)
        };
    }
}

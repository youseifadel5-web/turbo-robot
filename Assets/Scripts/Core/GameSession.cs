using UnityEngine;

namespace NeonRush.Core
{
    /// <summary>
    /// Cross-scene race request. Set before loading a scene; the Race scene
    /// (SceneAssembler) consumes it. Offline-first by design.
    /// </summary>
    public static class GameSession
    {
        public static Tracks.TrackConfig NextTrack;
        public static string SelectedCarId = "falcon_s";
        public static string RaceType = "Quick";
        public static AI.AIDifficulty Difficulty = AI.AIDifficulty.Normal;
        public static bool GhostRace = false;

        public static void Consume()
        {
            NextTrack = null;
            RaceType = "Quick";
            GhostRace = false;
        }
    }
}

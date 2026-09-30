using UnityEngine;

namespace NeonRush.Racing
{
    /// <summary>
    /// Plain data for end-of-race screen. Not a MonoBehaviour.
    /// </summary>
    [System.Serializable]
    public class RaceResults
    {
        public int position;
        public int totalRacers;
        public float totalTime;
        public float bestLap;
        public float topSpeedKmh;
        public float driftScore;
        public int maxCombo;
        public int nitroUses;
        public int overtakes;
        public int rewardCoins;
        public int rewardXp;
    }

    /// <summary>
    /// Optional component to hold last result on a results screen object.
    /// </summary>
    public class RaceResultsHolder : MonoBehaviour
    {
        public RaceResults lastResult = new RaceResults();

        public void Set(RaceResults r) => lastResult = r;
    }
}

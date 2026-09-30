using UnityEngine;
using System.Collections.Generic;
using NeonRush.Cars;

namespace NeonRush.Racing
{
    /// <summary>
    /// Accurate race positions: progress along the racing line (with lap
    /// wrap detection), not world-Z comparison. Feeds RaceManager and HUD.
    /// </summary>
    public class RacePositionTracker : MonoBehaviour
    {
        public RacingLineBuilder line;
        public ArcadeCarController player;
        public List<ArcadeCarController> rivals = new List<ArcadeCarController>();

        private class RacerState
        {
            public ArcadeCarController car;
            public float lastProgress;
            public int laps;
            public float totalProgress;
        }

        private readonly List<RacerState> racers = new List<RacerState>();

        public int PlayerPosition { get; private set; } = 1;
        public int PlayerLaps { get; private set; }
        public float PlayerTotalProgress { get; private set; }
        public int TotalRacers => racers.Count;

        public void Setup(RacingLineBuilder racingLine, ArcadeCarController playerCar, List<ArcadeCarController> aiCars)
        {
            line = racingLine;
            player = playerCar;
            rivals = aiCars ?? new List<ArcadeCarController>();
            racers.Clear();
            if (player != null) racers.Add(new RacerState { car = player });
            foreach (var r in rivals)
                if (r != null) racers.Add(new RacerState { car = r });
        }

        private void LateUpdate()
        {
            if (line == null || !line.IsValid) return;

            foreach (var st in racers)
            {
                if (st.car == null) continue;
                float p = line.GetProgress(st.car.transform.position);
                // lap wrap detection (with tolerance for short tracks)
                if (st.lastProgress > 0.85f && p < 0.15f) st.laps++;
                else if (st.lastProgress < 0.15f && p > 0.85f && st.laps > 0) st.laps--;
                st.lastProgress = p;
                st.totalProgress = st.laps + p;
            }

            var pState = racers.Find(s => s.car == player);
            if (pState != null)
            {
                PlayerLaps = pState.laps;
                PlayerTotalProgress = pState.totalProgress;
                int ahead = 0;
                foreach (var st in racers)
                {
                    if (st == pState || st.car == null) continue;
                    if (st.totalProgress > pState.totalProgress) ahead++;
                }
                PlayerPosition = ahead + 1;
            }
        }

        public int GetPositionOf(ArcadeCarController car)
        {
            var s = racers.Find(x => x.car == car);
            if (s == null) return 1;
            int ahead = 0;
            foreach (var other in racers)
                if (other != s && other.car != null && other.totalProgress > s.totalProgress) ahead++;
            return ahead + 1;
        }

        public int GetLapsOf(ArcadeCarController car)
        {
            var s = racers.Find(x => x.car == car);
            return s != null ? s.laps : 0;
        }

        /// <summary>Total progress (laps + fraction) of any tracked racer; 0 if unknown.</summary>
        public float GetRivalTotalProgress(ArcadeCarController car)
        {
            var s = racers.Find(x => x.car == car);
            return s != null ? s.totalProgress : 0f;
        }
    }
}

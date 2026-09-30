using UnityEngine;
using System.Collections.Generic;
using NeonRush.Cars;
using NeonRush.Tracks;
using NeonRush.UI;
using NeonRush.Save;

namespace NeonRush.Racing
{
    public enum RaceState
    {
        Idle,
        Countdown,
        Racing,
        Finished
    }

    /// <summary>
    /// Full race lifecycle: countdown → race → results.
    /// Hooks track, checkpoints, HUD, drift score, AI.
    /// </summary>
    public class RaceManager : MonoBehaviour
    {
        [Header("Config")]
        public TrackConfig trackConfig;
        public TrackBuilder trackBuilder;
        public int countdownSeconds = 3;

        [Header("Player")]
        public ArcadeCarController playerCar;
        public DriftScoreManager driftScore;
        public RuntimeRaceHUD hud;

        [Header("AI")]
        public List<ArcadeCarController> aiCars = new List<ArcadeCarController>();

        [Header("v0.9")]
        public RacePositionTracker positionTracker;
        public GhostRecorder ghostRecorder;
        private int lastCountdownShown;
        private float lapStartTime;
        private float bestLapTime;

        public RaceState State { get; private set; } = RaceState.Idle;
        public float RaceTime { get; private set; }
        public int CurrentLap { get; private set; } = 1;
        public int PlayerPosition { get; private set; } = 1;

        public System.Action<RaceResults> OnRaceFinished;

        private float countdownTimer;
        private int totalLaps = 3;
        private int lastCheckpointIndex = -1;
        private float progress;

        public void StartRace()
        {
            if (trackConfig != null && trackBuilder != null)
                trackBuilder.BuildFromConfig(trackConfig);

            totalLaps = trackConfig != null ? Mathf.Max(1, trackConfig.laps) : 3;
            CurrentLap = 1;
            RaceTime = 0f;
            lastCheckpointIndex = -1;
            progress = 0f;
            PlayerPosition = 1;

            if (driftScore != null) driftScore.ResetScore();
            if (hud != null)
            {
                hud.StartRaceTimer();
                hud.SetLap(1, totalLaps);
                hud.SetPosition(1, 1 + aiCars.Count);
            }

            State = RaceState.Countdown;
            countdownTimer = countdownSeconds;
            lastCountdownShown = countdownSeconds + 1;
            lapStartTime = 0f;
            bestLapTime = 0f;
            SetCarsFrozen(true);
        }

        private void Update()
        {
            switch (State)
            {
                case RaceState.Countdown:
                    countdownTimer -= Time.deltaTime;
                    if (hud != null && countdownTimer > 0f)
                        hud.FlashCheckpoint(Mathf.CeilToInt(countdownTimer).ToString());
                    int shown = Mathf.CeilToInt(Mathf.Max(0f, countdownTimer));
                    if (shown != lastCountdownShown && countdownTimer > 0f)
                    {
                        lastCountdownShown = shown;
                        Audio.SfxPlayer.Play("SFX/sfx_countdown", 0.8f);
                        Core.Haptics.Light();
                    }
                    if (countdownTimer <= 0f)
                    {
                        State = RaceState.Racing;
                        SetCarsFrozen(false);
                        lapStartTime = RaceTime;
                        if (hud != null) hud.FlashCheckpoint("GO");
                        Audio.SfxPlayer.Play("SFX/sfx_race_start", 0.9f);
                        Core.Haptics.Heavy();
                        if (ghostRecorder != null)
                            ghostRecorder.Begin(playerCar != null ? playerCar.transform : null,
                                trackConfig != null ? trackConfig.trackId : "race");
                    }
                    break;

                case RaceState.Racing:
                    RaceTime += Time.deltaTime;
                    UpdateProgressAndPosition();
                    if (hud != null)
                    {
                        hud.SetProgress(progress);
                        hud.SetPosition(PlayerPosition, 1 + aiCars.Count);
                    }
                    break;
            }
        }

        public void OnPlayerCheckpoint(int index)
        {
            if (State != RaceState.Racing) return;
            if (index <= lastCheckpointIndex) return;
            lastCheckpointIndex = index;
            if (hud != null) hud.FlashCheckpoint("CHECKPOINT");
        }

        public void OnPlayerFinishLine()
        {
            if (State != RaceState.Racing) return;

            if (CurrentLap < totalLaps)
            {
                float lapTime = RaceTime - lapStartTime;
                if (lapTime > 5f && (bestLapTime <= 0f || lapTime < bestLapTime)) bestLapTime = lapTime;
                lapStartTime = RaceTime;
                CurrentLap++;
                lastCheckpointIndex = -1;
                if (hud != null)
                {
                    hud.SetLap(CurrentLap, totalLaps);
                    hud.FlashCheckpoint("LAP " + CurrentLap);
                }
                Audio.SfxPlayer.Play("SFX/sfx_checkpoint", 0.7f);
            }
            else
            {
                FinishRace();
            }
        }

        private void FinishRace()
        {
            State = RaceState.Finished;
            SetCarsFrozen(true);
            if (hud != null) hud.StopRaceTimer();
            Audio.SfxPlayer.Play("SFX/sfx_finish", 0.9f);
            Core.Haptics.Medium();

            if (ghostRecorder != null && playerCar != null)
            {
                ghostRecorder.Stop();
                GhostRecorder.Save(ghostRecorder.GetData(
                    SaveSystem.Current != null ? SaveSystem.Current.selectedCarId : "falcon_s"));
            }

            // local leaderboard best time
            if (trackConfig != null)
                Services.GameServices.Leaderboards.SubmitTime(trackConfig.trackId, RaceTime);

            var results = new RaceResults
            {
                position = PlayerPosition,
                totalRacers = 1 + aiCars.Count,
                totalTime = RaceTime,
                bestLap = bestLapTime > 0f ? bestLapTime : RaceTime / Mathf.Max(1, totalLaps),
                topSpeedKmh = playerCar != null ? playerCar.SpeedKmh : 0f,
                driftScore = driftScore != null ? driftScore.TotalScore : 0f,
                maxCombo = driftScore != null ? driftScore.MaxCombo : 0
            };

            OnRaceFinished?.Invoke(results);
            Debug.Log($"[RaceManager] Finished P{results.position} Time={results.totalTime:F2} Drift={results.driftScore:F0}");
        }

        private void UpdateProgressAndPosition()
        {
            // v0.9: accurate progress along the racing line when a tracker is wired
            if (positionTracker != null)
            {
                progress = Mathf.Clamp01(positionTracker.PlayerTotalProgress / Mathf.Max(1, totalLaps));
                PlayerPosition = positionTracker.PlayerPosition;
                return;
            }

            // Fallback: distance-based estimate (v0.8 behaviour)
            if (trackBuilder != null && trackBuilder.BuiltDistanceMeters > 1f && playerCar != null)
            {
                if (trackBuilder.StartPoint != null)
                {
                    float dist = Vector3.Distance(playerCar.transform.position, trackBuilder.StartPoint.position);
                    progress = Mathf.Clamp01(dist / trackBuilder.BuiltDistanceMeters);
                }
            }

            int better = 0;
            foreach (var ai in aiCars)
            {
                if (ai == null) continue;
                if (ai.transform.position.z > playerCar.transform.position.z) better++;
            }
            PlayerPosition = better + 1;
        }

        private void SetCarsFrozen(bool frozen)
        {
            if (playerCar != null)
            {
                var rb = playerCar.GetComponent<Rigidbody>();
                if (rb != null) rb.isKinematic = frozen;
            }
            foreach (var ai in aiCars)
            {
                if (ai == null) continue;
                var rb = ai.GetComponent<Rigidbody>();
                if (rb != null) rb.isKinematic = frozen;
            }
        }
    }
}

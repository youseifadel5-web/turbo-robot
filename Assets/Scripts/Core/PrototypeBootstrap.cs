using UnityEngine;
using NeonRush.Cars;
using NeonRush.Tracks;
using NeonRush.Racing;
using NeonRush.UI;
using NeonRush.Environment;

namespace NeonRush.Core
{
    /// <summary>
    /// Wires a minimal playable race when references are assigned.
    /// Attach to an empty "Bootstrap" object in the race scene.
    /// </summary>
    public class PrototypeBootstrap : MonoBehaviour
    {
        [Header("Core")]
        public ArcadeCarController playerCar;
        public ChaseCamera chaseCamera;
        public RuntimeRaceHUD hud;
        public RaceManager raceManager;
        public TrackBuilder trackBuilder;
        public TrackConfig defaultTrack;

        [Header("Feel")]
        public WeatherController weather;
        public DayNightController dayNight;
        public int targetFps = 60;
        public bool startRaceOnPlay = true;
        public bool forceNight = true;

        private void Awake()
        {
            Application.targetFrameRate = targetFps;
            Screen.sleepTimeout = SleepTimeout.NeverSleep;
            QualitySettings.vSyncCount = 0;
        }

        private void Start()
        {
            AutoFind();

            if (forceNight && dayNight != null) dayNight.SetNight();
            if (weather != null && weather.current == WeatherType.Clear)
            {
                // default clear; user can switch
            }

            if (playerCar != null)
            {
                if (playerCar.GetComponent<NeonTrailSystem>() == null)
                    playerCar.gameObject.AddComponent<NeonTrailSystem>();
                if (playerCar.GetComponent<DriftScoreManager>() == null)
                    playerCar.gameObject.AddComponent<DriftScoreManager>();
                if (playerCar.GetComponent<NitroVFXController>() == null)
                {
                    var vfx = playerCar.gameObject.AddComponent<NitroVFXController>();
                    vfx.car = playerCar;
                    vfx.chaseCamera = chaseCamera;
                }
            }

            if (hud != null && playerCar != null) hud.car = playerCar;
            if (chaseCamera != null && playerCar != null) chaseCamera.target = playerCar.transform;

            if (startRaceOnPlay && raceManager != null)
            {
                if (raceManager.playerCar == null) raceManager.playerCar = playerCar;
                if (raceManager.hud == null) raceManager.hud = hud;
                if (raceManager.trackBuilder == null) raceManager.trackBuilder = trackBuilder;
                if (raceManager.trackConfig == null) raceManager.trackConfig = defaultTrack;
                raceManager.StartRace();
            }

            Debug.Log("[Neon Rush] Prototype ready — WASD drive, Shift drift, Ctrl nitro.");
        }

        private void AutoFind()
        {
            if (playerCar == null) playerCar = FindObjectOfType<ArcadeCarController>();
            if (chaseCamera == null) chaseCamera = FindObjectOfType<ChaseCamera>();
            if (hud == null) hud = FindObjectOfType<RuntimeRaceHUD>();
            if (raceManager == null) raceManager = FindObjectOfType<RaceManager>();
            if (trackBuilder == null) trackBuilder = FindObjectOfType<TrackBuilder>();
            if (weather == null) weather = FindObjectOfType<WeatherController>();
            if (dayNight == null) dayNight = FindObjectOfType<DayNightController>();
        }
    }
}

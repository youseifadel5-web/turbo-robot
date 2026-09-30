using UnityEngine;
using NeonRush.Cars;
using NeonRush.Tracks;
using NeonRush.Racing;
using NeonRush.UI;
using NeonRush.Core;
using NeonRush.Audio;
using NeonRush.Save;
using System.Collections.Generic;

namespace NeonRush.Environment
{
    /// <summary>
    /// One-click: neon city + car + VFX + engine audio + HUD + race.
    /// </summary>
    public class SceneAssembler : MonoBehaviour
    {
        public bool buildOnStart = true;
        public float cityLength = 600f;
        public Color carPaint = new Color(0.04f, 0.05f, 0.07f);
        public Color carNeon = new Color(0f, 0.94f, 1f);
        public TrackConfig trackConfig;
        public bool buildHud = true;
        public bool buildEngineAudio = true;
        public bool startRace = true;

        private void Start()
        {
            if (buildOnStart) BuildAll();
        }

        [ContextMenu("Build All")]
        public void BuildAll()
        {
            // consume a pending race request (e.g. from Career)
            if (GameSession.NextTrack != null)
            {
                trackConfig = GameSession.NextTrack;
                GameSession.Consume();
            }

            Settings.SettingsService.ApplyAll();

            var skyGo = GameObject.Find("SkyLighting") ?? new GameObject("SkyLighting");
            var sky = skyGo.GetComponent<SkyLightingBootstrap>() ?? skyGo.AddComponent<SkyLightingBootstrap>();
            var tod = trackConfig != null ? trackConfig.timeOfDay : Tracks.RaceTimeOfDay.Night;
            sky.forceNight = tod == Tracks.RaceTimeOfDay.Night;
            sky.SetTimeOfDay(tod);
            sky.Apply();

            var cityGo = GameObject.Find("NeonCity") ?? new GameObject("NeonCity");
            var city = cityGo.GetComponent<NeonCityBuilder>() ?? cityGo.AddComponent<NeonCityBuilder>();
            city.roadLength = cityLength;
            city.Build(cityGo.transform);

            var trackGo = GameObject.Find("TrackRoot") ?? new GameObject("TrackRoot");
            var builder = trackGo.GetComponent<TrackBuilder>() ?? trackGo.AddComponent<TrackBuilder>();
            if (trackConfig == null)
            {
                trackConfig = ScriptableObject.CreateInstance<TrackConfig>();
                trackConfig.lengthPreset = TrackLengthPreset.Medium;
                trackConfig.laps = 2;
                trackConfig.displayName = "Neon Corridor";
            }
            builder.segmentLength = 50f;
            builder.BuildFromConfig(trackConfig);

            GameObject carGo = GameObject.FindGameObjectWithTag("Player");
            if (carGo == null)
            {
                string carId = SaveSystem.Current != null ? SaveSystem.Current.selectedCarId : "falcon_s";
                carGo = CarModelLoader.LoadCar(carId, true);
                if (carGo.GetComponent<ArcadeCarController>() != null)
                    carGo.GetComponent<ArcadeCarController>().stats = Cars.CarCatalog.CreateStats(carId);
                carGo.transform.position = new Vector3(0f, 0.5f, 8f);
            }

            if (carGo.GetComponent<CarVfxBinder>() == null)
            {
                var binder = carGo.AddComponent<CarVfxBinder>();
                binder.car = carGo.GetComponent<ArcadeCarController>();
            }

            Camera cam = Camera.main;
            if (cam == null)
            {
                var camGo = new GameObject("Main Camera");
                camGo.tag = "MainCamera";
                cam = camGo.AddComponent<Camera>();
                camGo.AddComponent<AudioListener>();
            }
            var chase = cam.GetComponent<ChaseCamera>() ?? cam.gameObject.AddComponent<ChaseCamera>();
            chase.target = carGo.transform;
            cam.transform.position = carGo.transform.position + new Vector3(0f, 3.2f, -7.5f);

            var carCtrl = carGo.GetComponent<ArcadeCarController>();
            var nitro = carGo.GetComponent<NitroVFXController>();
            if (nitro != null) nitro.chaseCamera = chase;

            // Audio bus + engine layers
            var audioGo = GameObject.Find("AudioBus") ?? new GameObject("AudioBus");
            var bus = audioGo.GetComponent<AudioBus>() ?? audioGo.AddComponent<AudioBus>();
            if (buildEngineAudio && carGo.GetComponent<EngineAudioController>() == null)
            {
                var eng = carGo.AddComponent<EngineAudioController>();
                eng.car = carCtrl;
                eng.bus = bus;
            }

            // HUD
            RuntimeRaceHUD hud = FindObjectOfType<RuntimeRaceHUD>();
            if (buildHud && hud == null)
            {
                var hudBuilder = gameObject.GetComponent<RaceHudBuilder>() ?? gameObject.AddComponent<RaceHudBuilder>();
                hudBuilder.car = carCtrl;
                hudBuilder.input = carGo.GetComponent<MobileCarInput>();
                hud = hudBuilder.BuildAndBind();
            }
            else if (hud != null)
            {
                hud.BindCar(carCtrl);
            }

            var weatherGo = GameObject.Find("Weather") ?? new GameObject("Weather");
            var weather = weatherGo.GetComponent<WeatherController>() ?? weatherGo.AddComponent<WeatherController>();
            if (carCtrl != null) weather.affectedCars = new[] { carCtrl };

            var raceGo = GameObject.Find("RaceManager") ?? new GameObject("RaceManager");
            var race = raceGo.GetComponent<RaceManager>() ?? raceGo.AddComponent<RaceManager>();
            race.playerCar = carCtrl;
            race.trackBuilder = builder;
            race.trackConfig = trackConfig;
            race.driftScore = carGo.GetComponent<DriftScoreManager>();
            race.hud = hud;

            var boot = FindObjectOfType<PrototypeBootstrap>();
            if (boot == null)
                boot = new GameObject("Bootstrap").AddComponent<PrototypeBootstrap>();
            boot.playerCar = carCtrl;
            boot.chaseCamera = chase;
            boot.raceManager = race;
            boot.trackBuilder = builder;
            boot.defaultTrack = trackConfig;
            boot.weather = weather;
            boot.hud = hud;
            boot.forceNight = true;
            boot.startRaceOnPlay = false;

            var flow = FindObjectOfType<NeonRush.UI.MainMenuFlow>() ?? gameObject.AddComponent<NeonRush.UI.MainMenuFlow>();
            flow.assembler = this;
            flow.OnPlayRequested = () =>
            {
                if (hud != null) hud.StartRaceTimer();
                race.StartRace();
            };
            flow.Build();

            BuildAdvancedSystems(builder, carGo, carCtrl, hud, race, chase);

            race.OnRaceFinished += r =>
            {
                string body = r == null ? "" :
                    $"P{r.position}/{r.totalRacers}\nTIME  {r.totalTime:00.00}\nDRIFT  {r.driftScore:0}\nTOP  {r.topSpeedKmh:0} KM/H";
                flow.ShowResults(body);
            };

            if (startRace && race != null)
            {
                flow.HideMenu();
                race.StartRace();
            }

            Debug.Log("[SceneAssembler] Menu + City + Car + HUD + Audio ready.");
        }

        // ===================================================================
        //  v0.9 advanced systems: racing line, AI rivals, accurate positions,
        //  wrong-way, minimap, damage, ghost, traffic, settings, career,
        //  garage studio, device-music radio.
        // ===================================================================
        private void BuildAdvancedSystems(Tracks.TrackBuilder builder, GameObject carGo,
            ArcadeCarController carCtrl, RuntimeRaceHUD hud, RaceManager race, ChaseCamera chase)
        {
            if (carGo == null || race == null) return;

            // ---- racing line ----
            var lineGo = GameObject.Find("RacingLine") ?? new GameObject("RacingLine");
            var line = lineGo.GetComponent<RacingLineBuilder>() ?? lineGo.AddComponent<RacingLineBuilder>();
            line.Build(builder);

            // ---- AI rivals ----
            var rivalIds = new[] { "vortex_gt", "titan_x", "falcon_s" };
            var rivals = new List<ArcadeCarController>();
            if (line.IsValid && trackConfig != null)
            {
                for (int i = 0; i < rivalIds.Length; i++)
                {
                    var rivalGo = CarModelLoader.LoadCar(rivalIds[i], false);
                    rivalGo.name = "AI_" + rivalIds[i];
                    rivalGo.transform.position = carGo.transform.position + new Vector3((i - 1) * 4f, 0f, -6f - i * 4f);
                    rivalGo.transform.rotation = carGo.transform.rotation;

                    var rivalCtrl = rivalGo.GetComponent<ArcadeCarController>() ?? rivalGo.AddComponent<ArcadeCarController>();
                    rivalCtrl.stats = Cars.CarCatalog.CreateStats(rivalIds[i]);
                    var rb = rivalGo.GetComponent<Rigidbody>();
                    if (rb != null) rb.isKinematic = true; // until race starts

                    var ai = rivalGo.AddComponent<AI.RacingAI>();
                    ai.SetDifficulty(GameSession.Difficulty);
                    ai.SetWaypoints(line.CreateWaypointTransforms(lineGo.transform));
                    rivals.Add(rivalCtrl);
                }
                race.aiCars = rivals;
            }

            // ---- accurate positions ----
            var posGo = GameObject.Find("RacePositions") ?? new GameObject("RacePositions");
            var tracker = posGo.GetComponent<RacePositionTracker>() ?? posGo.AddComponent<RacePositionTracker>();
            tracker.Setup(line, carCtrl, rivals);
            race.positionTracker = tracker;

            // ---- live leaderboard (reference HUD look) ----
            if (hud != null)
            {
                var board = posGo.GetComponent<LeaderboardController>() ?? posGo.AddComponent<LeaderboardController>();
                board.Setup(tracker, hud.transform);
            }

            // ---- cockpit interior (first-person modes) ----
            if (chase != null && carGo.GetComponent<Cars.CockpitInterior>() == null)
            {
                var cockpit = carGo.AddComponent<Cars.CockpitInterior>();
                cockpit.car = carCtrl;
                cockpit.chase = chase;
            }

            // ---- wrong way ----
            var wrongWay = carGo.GetComponent<WrongWayDetector>() ?? carGo.AddComponent<WrongWayDetector>();
            wrongWay.line = line;
            wrongWay.car = carCtrl;
            wrongWay.hud = hud;

            // ---- damage ----
            if (carGo.GetComponent<DamageSystem>() == null)
            {
                var dmg = carGo.AddComponent<DamageSystem>();
                dmg.mode = DamageMode.Visual;
            }

            // ---- ghost recorder ----
            var ghostGo = GameObject.Find("GhostRecorder");
            var ghost = ghostGo != null ? ghostGo.GetComponent<GhostRecorder>() : null;
            if (ghost == null)
            {
                ghost = posGo.AddComponent<GhostRecorder>();
                if (trackConfig != null) ghost.trackId = trackConfig.trackId;
            }
            race.ghostRecorder = ghost;

            // ---- minimap (creates the HUD slot if missing) ----
            if (hud != null)
            {
                if (hud.minimapImage == null)
                {
                    var hudCanvas = hud.GetComponentInParent<Canvas>();
                    if (hudCanvas != null)
                    {
                        var slot = new GameObject("MinimapSlot", typeof(RawImage));
                        slot.transform.SetParent(hudCanvas.transform, false);
                        var srt = slot.GetComponent<RectTransform>();
                        srt.anchorMin = srt.anchorMax = new Vector2(1f, 1f);
                        srt.pivot = new Vector2(1f, 1f);
                        srt.sizeDelta = new Vector2(210, 210);
                        srt.anchoredPosition = new Vector2(-30, -30);
                        var img = slot.GetComponent<RawImage>();
                        img.color = new Color(1f, 1f, 1f, 0.92f);
                        hud.minimapImage = img;
                    }
                }
                if (hud.minimapImage != null)
                {
                    var mapGo = GameObject.Find("Minimap") ?? new GameObject("Minimap");
                    var minimap = mapGo.GetComponent<MinimapController>() ?? mapGo.AddComponent<MinimapController>();
                    minimap.Setup(carGo.transform, hud.minimapImage);
                }
            }

            // ---- traffic ----
            if (trackConfig != null && trackConfig.hasTraffic)
            {
                var trafficGo = GameObject.Find("Traffic") ?? new GameObject("Traffic");
                var traffic = trafficGo.GetComponent<AI.TrafficManager>() ?? trafficGo.AddComponent<AI.TrafficManager>();
                traffic.player = carGo.transform;
                traffic.trafficPrefab = Resources.Load<GameObject>("Prefabs/Traffic_Sedan");
                traffic.SetDensity((AI.TrafficDensity)Mathf.Clamp(trackConfig.trafficDensity, 0, 2));
            }

            // ---- adaptive performance ----
            if (FindObjectOfType<Performance.AdaptiveFpsGuard>() == null)
                posGo.AddComponent<Performance.AdaptiveFpsGuard>();

            // ---- meta screens ----
            var canvasTr = FindMenuCanvas();

            if (canvasTr != null)
            {
                // settings
                var settingsScreen = GetComponent<SettingsScreen>() ?? gameObject.AddComponent<SettingsScreen>();
                settingsScreen.Build(canvasTr);
                // career
                var careerScreen = GetComponent<CareerScreen>() ?? gameObject.AddComponent<CareerScreen>();
                careerScreen.Build(canvasTr);
                careerScreen.OnEventSelected = (cfg, raceType) =>
                {
                    if (cfg == null) return;
                    cfg.hasTraffic = trackConfig == null || trackConfig.hasTraffic;
                    race.trackConfig = cfg;
                    var flowC = GetComponent<MainMenuFlow>();
                    if (flowC != null) flowC.HideMenu();
                    race.StartRace();
                };
                // garage
                var garageStudio = GetComponent<GarageStudioBuilder>() ?? gameObject.AddComponent<GarageStudioBuilder>();
                var garageScreen = GetComponent<GarageScreen>() ?? gameObject.AddComponent<GarageScreen>();
                garageScreen.Build(canvasTr, garageStudio, null);
                garageScreen.onRaceRequested = () =>
                {
                    if (garageOpen) ToggleGarage(garageStudio, garageScreen, chase, hud);
                    if (race != null) race.StartRace();
                };

                // radio
                var radio = GetComponent<InGameRadio>() ?? gameObject.AddComponent<InGameRadio>();
                radio.Build(canvasTr);

                var flow = GetComponent<MainMenuFlow>();
                if (flow != null)
                {
                    flow.OnSettingsRequested += () =>
                    {
                        if (settingsScreen.IsVisible) settingsScreen.Hide(); else settingsScreen.Show();
                    };
                    flow.OnEventsRequested += () =>
                    {
                        if (careerScreen.IsVisible) careerScreen.Hide(); else careerScreen.Show();
                    };
                    flow.OnMusicRequested += radio.Toggle;
                    flow.OnGarageRequested += () => ToggleGarage(garageStudio, garageScreen, chase, hud);
                }
            }
        }

        private Transform FindMenuCanvas()
        {
            var canvases = FindObjectsOfType<Canvas>();
            foreach (var c in canvases)
                if (c.name == "MenuCanvas") return c.transform;
            foreach (var c in canvases)
                if (c.renderMode == RenderMode.ScreenSpaceOverlay && c.sortingOrder >= 200) return c.transform;
            return canvases.Length > 0 ? canvases[0].transform : null;
        }

        private bool garageOpen;

        private void ToggleGarage(GarageStudioBuilder studio, GarageScreen screen, ChaseCamera chase, RuntimeRaceHUD hud)
        {
            garageOpen = !garageOpen;
            if (garageOpen)
            {
                var studioRoot = GameObject.Find("GarageStudio");
                if (studioRoot == null)
                {
                    studioRoot = studio.Build(null);
                    studioRoot.SetActive(false);
                }
                studioRoot.SetActive(true);
                studio.Enter();
                screen.Show();
                if (chase != null) chase.enabled = false;
                if (hud != null) hud.gameObject.SetActive(false);
            }
            else
            {
                screen.Hide();
                studio.Exit();
                var studioRoot = GameObject.Find("GarageStudio");
                if (studioRoot != null) studioRoot.SetActive(false);
                if (chase != null) chase.enabled = true;
                if (hud != null) hud.gameObject.SetActive(true);
            }
        }
    }
}

using UnityEngine;
using UnityEngine.UI;
using NeonRush.Cars;
using NeonRush.Save;
using NeonRush.Environment;
using NeonRush.Audio;

namespace NeonRush.UI
{
    /// <summary>
    /// Boots the dedicated Garage scene: studio + garage UI + back button.
    /// Assigned to a single empty GameObject in Garage.unity.
    /// </summary>
    public class GarageSceneBoot : MonoBehaviour
    {
        private GarageScreen garageScreen;
        private GarageStudioBuilder studio;

        private void Start()
        {
            Settings.SettingsService.ApplyAll();

            var canvasGo = new GameObject("GarageCanvas");
            var canvas = canvasGo.AddComponent<Canvas>();
            canvas.renderMode = RenderMode.ScreenSpaceOverlay;
            var scaler = canvasGo.AddComponent<CanvasScaler>();
            scaler.uiScaleMode = CanvasScaler.ScaleMode.ScaleWithScreenSize;
            scaler.referenceResolution = new Vector2(1920, 1080);
            canvasGo.AddComponent<GraphicRaycaster>();
            canvasGo.AddComponent<SafeAreaFitter>();

            if (FindObjectOfType<UnityEngine.EventSystems.EventSystem>() == null)
            {
                var es = new GameObject("EventSystem");
                es.AddComponent<UnityEngine.EventSystems.EventSystem>();
                es.AddComponent<UnityEngine.EventSystems.StandaloneInputModule>();
            }

            studio = gameObject.AddComponent<GarageStudioBuilder>();
            studio.Build(null);

            garageScreen = gameObject.AddComponent<GarageScreen>();
            garageScreen.Build(canvas.transform, studio, () => Core.SceneLoader.LoadScene("MainMenu"));
            garageScreen.onRaceRequested = () => Core.SceneLoader.LoadRace(null, "Quick");
            garageScreen.Show();
        }
    }
}

using System.Collections;
using UnityEngine;
using UnityEngine.SceneManagement;
using UnityEngine.UI;

namespace NeonRush.Core
{
    /// <summary>
    /// Async scene loading with a neon loading screen (progress + rotating tips).
    /// Never blocks the main thread while the track builds.
    /// </summary>
    public class SceneLoader : MonoBehaviour
    {
        public static readonly string[] Tips =
        {
            "Drift through corners to charge combo multipliers.",
            "Nitro recharges automatically — save it for straights.",
            "Rain reduces grip. Brake earlier, steer smoother.",
            "Near-miss traffic at speed for bonus drift points.",
            "Upgrading Tires improves both grip and drift control.",
            "A clean lap with no collisions earns bonus stars.",
            "Watch the minimap for hairpins before they arrive.",
            "Engine, Turbo and Nitro upgrades stack — plan your build.",
            "Longer tracks mean more checkpoints and more coin rewards.",
            "Wrong way? Follow the neon arrows on the barriers."
        };

        private static SceneLoader instance;

        public static void LoadScene(string sceneName)
        {
            EnsureInstance();
            instance.StartCoroutine(instance.LoadRoutine(sceneName));
        }

        public static void LoadRace(Tracks.TrackConfig config, string raceType = "Quick")
        {
            GameSession.NextTrack = config;
            GameSession.RaceType = raceType;
            LoadScene("Race_NeonCity");
        }

        private static void EnsureInstance()
        {
            if (instance != null) return;
            var go = new GameObject("SceneLoader");
            DontDestroyOnLoad(go);
            instance = go.AddComponent<SceneLoader>();
        }

        private IEnumerator LoadRoutine(string sceneName)
        {
            Canvas canvas = BuildOverlay();

            var tip = GameObject.Find("LoadingTip")?.GetComponent<Text>();
            var bar = GameObject.Find("LoadingBarFill")?.GetComponent<Image>();

            if (tip != null) tip.text = "TIP — " + Tips[Random.Range(0, Tips.Length)];
            if (bar != null) bar.fillAmount = 0f;

            var op = SceneManager.LoadSceneAsync(sceneName);
            op.allowSceneActivation = false;

            while (op.progress < 0.9f)
            {
                if (bar != null) bar.fillAmount = op.progress / 0.9f;
                yield return null;
            }
            if (bar != null) bar.fillAmount = 1f;
            yield return new WaitForSecondsRealtime(0.25f);

            op.allowSceneActivation = true;
            while (!op.isDone) yield return null;

            // give the new scene one frame to build its runtime content
            yield return null;
            Destroy(canvas.gameObject);
        }

        private static Canvas BuildOverlay()
        {
            var go = new GameObject("LoadingOverlay", typeof(Canvas), typeof(CanvasScaler), typeof(GraphicRaycaster));
            var canvas = go.GetComponent<Canvas>();
            canvas.renderMode = RenderMode.ScreenSpaceOverlay;
            canvas.sortingOrder = 500;
            var scaler = go.GetComponent<CanvasScaler>();
            scaler.uiScaleMode = CanvasScaler.ScaleMode.ScaleWithScreenSize;
            scaler.referenceResolution = new Vector2(1920, 1080);

            var dim = new GameObject("Dim", typeof(Image));
            dim.transform.SetParent(go.transform, false);
            var dimRt = dim.GetComponent<RectTransform>();
            dimRt.anchorMin = Vector2.zero; dimRt.anchorMax = Vector2.one;
            dimRt.offsetMin = dimRt.offsetMax = Vector2.zero;
            dim.GetComponent<Image>().color = new Color(0.01f, 0.015f, 0.03f, 1f);

            var tipText = new GameObject("LoadingTip", typeof(Text));
            tipText.transform.SetParent(go.transform, false);
            var tipRt = tipText.GetComponent<RectTransform>();
            tipRt.anchorMin = tipRt.anchorMax = new Vector2(0.5f, 0.5f);
            tipRt.sizeDelta = new Vector2(1100, 60);
            tipRt.anchoredPosition = new Vector2(0, -120);
            var tt = tipText.GetComponent<Text>();
            tt.color = UI.UiTheme.Cyan;
            tt.fontSize = 26;
            tt.alignment = TextAnchor.MiddleCenter;
            tt.font = UI.UiTheme.Font;

            var title = new GameObject("LoadingTitle", typeof(Text));
            title.transform.SetParent(go.transform, false);
            var titleRt = title.GetComponent<RectTransform>();
            titleRt.anchorMin = titleRt.anchorMax = new Vector2(0.5f, 0.5f);
            titleRt.sizeDelta = new Vector2(1100, 80);
            titleRt.anchoredPosition = new Vector2(0, -50);
            var tx = title.GetComponent<Text>();
            tx.text = "NEON RUSH";
            tx.color = UI.UiTheme.Magenta;
            tx.fontSize = 52;
            tx.alignment = TextAnchor.MiddleCenter;
            tx.font = UI.UiTheme.Font;

            var barBg = new GameObject("LoadingBarBg", typeof(Image));
            barBg.transform.SetParent(go.transform, false);
            var bgRt = barBg.GetComponent<RectTransform>();
            bgRt.anchorMin = bgRt.anchorMax = new Vector2(0.5f, 0.5f);
            bgRt.sizeDelta = new Vector2(900, 14);
            bgRt.anchoredPosition = new Vector2(0, -190);
            barBg.GetComponent<Image>().color = new Color(0.1f, 0.12f, 0.18f, 1f);

            var fill = new GameObject("LoadingBarFill", typeof(Image));
            fill.transform.SetParent(barBg.transform, false);
            var fillImg = fill.GetComponent<Image>();
            fillImg.color = UI.UiTheme.Cyan;
            fillImg.type = Image.Type.Filled;
            fillImg.fillMethod = Image.FillMethod.Horizontal;
            var fillRt = fill.GetComponent<RectTransform>();
            fillRt.anchorMin = Vector2.zero; fillRt.anchorMax = Vector2.one;
            fillRt.offsetMin = fillRt.offsetMax = Vector2.zero;

            return canvas;
        }
    }
}

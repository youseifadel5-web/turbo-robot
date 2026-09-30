using UnityEngine;
using UnityEngine.UI;
using NeonRush.Environment;
using NeonRush.Save;

namespace NeonRush.UI
{
    /// <summary>
    /// Overlay main menu + results so the prototype feels like a full loop.
    /// </summary>
    public class MainMenuFlow : MonoBehaviour
    {
        public SceneAssembler assembler;
        public System.Action OnPlayRequested;
        public System.Action OnGarageRequested;
        public System.Action OnEventsRequested;
        public System.Action OnSettingsRequested;
        public System.Action OnMusicRequested;

        private GameObject menuRoot;
        private GameObject resultsRoot;
        private Text resultsBody;

        public void Build()
        {
            var canvas = GameObject.Find("MenuCanvas");
            if (canvas == null)
            {
                canvas = new GameObject("MenuCanvas");
                var c = canvas.AddComponent<Canvas>();
                c.renderMode = RenderMode.ScreenSpaceOverlay;
                c.sortingOrder = 200;
                var sc = canvas.AddComponent<CanvasScaler>();
                sc.uiScaleMode = CanvasScaler.ScaleMode.ScaleWithScreenSize;
                sc.referenceResolution = new Vector2(1920, 1080);
                canvas.AddComponent<GraphicRaycaster>();
            }

            if (FindObjectOfType<UnityEngine.EventSystems.EventSystem>() == null)
            {
                var es = new GameObject("EventSystem");
                es.AddComponent<UnityEngine.EventSystems.EventSystem>();
                es.AddComponent<UnityEngine.EventSystems.StandaloneInputModule>();
            }

            menuRoot = new GameObject("MainMenu");
            menuRoot.transform.SetParent(canvas.transform, false);
            var full = menuRoot.AddComponent<RectTransform>();
            Stretch(full);

            var dim = menuRoot.AddComponent<Image>();
            dim.color = new Color(0.02f, 0.02f, 0.05f, 0.35f);

            UiTheme.Label(menuRoot.transform, "Logo", "NEON RUSH",
                new Vector2(0, 1), new Vector2(80, -70), new Vector2(700, 70),
                48, UiTheme.Magenta, TextAnchor.UpperLeft);
            UiTheme.Label(menuRoot.transform, "Sub", "RACING",
                new Vector2(0, 1), new Vector2(80, -120), new Vector2(400, 40),
                18, UiTheme.Cyan, TextAnchor.UpperLeft);

            var data = SaveSystem.Load();
            UiTheme.Label(menuRoot.transform, "Coins", "●  " + data.coins.ToString("N0"),
                new Vector2(1, 1), new Vector2(-80, -70), new Vector2(320, 48),
                22, UiTheme.Gold, TextAnchor.UpperRight);
            UiTheme.Label(menuRoot.transform, "Lv", "LV " + data.level,
                new Vector2(1, 1), new Vector2(-80, -110), new Vector2(320, 36),
                18, UiTheme.Muted, TextAnchor.UpperRight);

            MakeMenuButton(menuRoot.transform, "Play", Localization.L10n.Get("menu.play"), new Vector2(80, -280), () =>
            {
                HideMenu();
                OnPlayRequested?.Invoke();
            }, true);
            MakeMenuButton(menuRoot.transform, "Garage", Localization.L10n.Get("menu.garage"), new Vector2(80, -370), () =>
            {
                Audio.SfxPlayer.PlayUi("SFX/sfx_ui_click", 0.8f);
                OnGarageRequested?.Invoke();
            });
            MakeMenuButton(menuRoot.transform, "Events", Localization.L10n.Get("menu.events"), new Vector2(80, -460), () =>
            {
                Audio.SfxPlayer.PlayUi("SFX/sfx_ui_click", 0.8f);
                OnEventsRequested?.Invoke();
            });
            MakeMenuButton(menuRoot.transform, "Settings", Localization.L10n.Get("menu.settings"), new Vector2(80, -550), () =>
            {
                Audio.SfxPlayer.PlayUi("SFX/sfx_ui_click", 0.8f);
                OnSettingsRequested?.Invoke();
            });
            MakeMenuButton(menuRoot.transform, "Music", Localization.L10n.Get("menu.music"), new Vector2(80, -640), () =>
            {
                Audio.SfxPlayer.PlayUi("SFX/sfx_ui_click", 0.8f);
                OnMusicRequested?.Invoke();
            });

            BuildResults(canvas.transform);
            HideResults();
        }

        public void HideMenu()
        {
            if (menuRoot) menuRoot.SetActive(false);
        }

        public void ShowMenu()
        {
            if (menuRoot) menuRoot.SetActive(true);
        }

        public void ShowResults(string body)
        {
            if (resultsRoot) resultsRoot.SetActive(true);
            if (resultsBody) resultsBody.text = body;
        }

        public void HideResults()
        {
            if (resultsRoot) resultsRoot.SetActive(false);
        }

        private void BuildResults(Transform canvas)
        {
            resultsRoot = new GameObject("Results");
            resultsRoot.transform.SetParent(canvas, false);
            var rt = resultsRoot.AddComponent<RectTransform>();
            Stretch(rt);
            resultsRoot.AddComponent<Image>().color = new Color(0.02f, 0.03f, 0.06f, 0.72f);

            UiTheme.Label(resultsRoot.transform, "Title", "RACE COMPLETE",
                new Vector2(0.5f, 1), new Vector2(0, -80), new Vector2(800, 60),
                36, UiTheme.Cyan, TextAnchor.UpperCenter);
            resultsBody = UiTheme.Label(resultsRoot.transform, "Body", "",
                new Vector2(0.5f, 0.55f), Vector2.zero, new Vector2(700, 360),
                24, UiTheme.Text, TextAnchor.MiddleCenter);

            MakeMenuButton(resultsRoot.transform, "Retry", "RETRY", new Vector2(-140, 140), () =>
            {
                HideResults();
                OnPlayRequested?.Invoke();
            }, false, new Vector2(0.5f, 0));
            MakeMenuButton(resultsRoot.transform, "Menu", "MENU", new Vector2(140, 140), () =>
            {
                HideResults();
                ShowMenu();
            }, true, new Vector2(0.5f, 0));
        }

        private void MakeMenuButton(Transform parent, string name, string label, Vector2 pos, UnityEngine.Events.UnityAction click, bool primary = false, Vector2? anchor = null)
        {
            var a = anchor ?? new Vector2(0, 1);
            var img = UiTheme.PanelBox(parent, name, a, pos, new Vector2(360, 70),
                primary ? new Color(0f, 0.94f, 1f, 0.18f) : UiTheme.Panel,
                primary ? UiTheme.Cyan : new Color(0.18f, 0.22f, 0.3f));
            var t = UiTheme.Label(img.transform, "L", label, new Vector2(0.5f, 0.5f), Vector2.zero,
                new Vector2(360, 70), 24, primary ? UiTheme.Cyan : UiTheme.Text, TextAnchor.MiddleCenter);
            t.rectTransform.anchorMin = Vector2.zero;
            t.rectTransform.anchorMax = Vector2.one;
            t.rectTransform.offsetMin = Vector2.zero;
            t.rectTransform.offsetMax = Vector2.zero;
            var btn = img.gameObject.AddComponent<Button>();
            btn.targetGraphic = img;
            btn.onClick.AddListener(click);
        }

        private static void Stretch(RectTransform rt)
        {
            rt.anchorMin = Vector2.zero;
            rt.anchorMax = Vector2.one;
            rt.offsetMin = Vector2.zero;
            rt.offsetMax = Vector2.zero;
        }
    }
}

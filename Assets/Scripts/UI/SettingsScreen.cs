using UnityEngine;
using UnityEngine.UI;
using UnityEngine.Events;
using NeonRush.Save;
using NeonRush.Settings;
using NeonRush.Audio;
using NeonRush.Localization;

namespace NeonRush.UI
{
    /// <summary>
    /// Fully functional settings panel (runtime-built, neon style):
    /// graphics tier, FPS, volumes, vibration, control scheme, sensitivity.
    /// Bound to SaveSystem through SettingsService.
    /// </summary>
    public class SettingsScreen : MonoBehaviour
    {
        public System.Action OnClosed;

        private GameObject root;
        private Text graphicsValue, fpsValue, controlValue, langValue;
        private Toggle vibToggle;
        private bool building;

        public void Build(Transform canvas)
        {
            if (root != null) return;
            building = true;

            root = new GameObject("SettingsPanel");
            root.transform.SetParent(canvas, false);
            var rt = root.AddComponent<RectTransform>();
            Stretch(rt);
            root.AddComponent<Image>().color = new Color(0.01f, 0.02f, 0.04f, 0.92f);

            UiTheme.Label(root.transform, "Title", L10n.Get("settings.title"), new Vector2(0.5f, 1f),
                new Vector2(0, -70), new Vector2(800, 60), 38, UiTheme.Cyan, TextAnchor.UpperCenter);

            var panel = UiTheme.PanelBox(root.transform, "Body", new Vector2(0.5f, 0.5f), Vector2.zero,
                new Vector2(860, 720), UiTheme.Panel, UiTheme.Cyan);

            float y = -60f;
            // Graphics
            graphicsValue = AddCycleRow(panel.transform, L10n.Get("settings.graphics"), ref y, () =>
            {
                SettingsService.CycleGraphics();
                return SettingsService.GraphicsNames[SaveSystem.Current.graphicsLevel];
            });
            // FPS
            fpsValue = AddCycleRow(panel.transform, L10n.Get("settings.fps"), ref y, () =>
            {
                SettingsService.CycleFps();
                return SaveSystem.Current.targetFps + " FPS";
            });
            // Vibration
            vibToggle = AddToggleRow(panel.transform, L10n.Get("settings.vibration"), ref y, on =>
            {
                SettingsService.SetVibration(on);
                if (on) Core.Haptics.Medium();
            });
            // Language
            langValue = AddCycleRow(panel.transform, L10n.Get("settings.language"), ref y, () =>
            {
                L10n.Cycle();
                return L10n.Current == Language.EN ? "ENGLISH" : "العربية";
            });
            // Controls
            controlValue = AddCycleRow(panel.transform, L10n.Get("settings.controls"), ref y, () =>
            {
                var d = SaveSystem.Current;
                SettingsService.SetControlScheme((d.controlScheme + 1) % 3);
                return SettingsService.ControlNames[d.controlScheme];
            });
            // Sensitivity
            AddSliderRow(panel.transform, L10n.Get("settings.sensitivity"), ref y,
                SaveSystem.Current.steeringSensitivity, 0.3f, 2f, v => SettingsService.SetSensitivity(v));
            // Volumes
            AddSliderRow(panel.transform, L10n.Get("settings.master"), ref y, SaveSystem.Current.masterVolume, 0f, 1f, SettingsService.SetMaster);
            AddSliderRow(panel.transform, L10n.Get("settings.music"), ref y, SaveSystem.Current.musicVolume, 0f, 1f, SettingsService.SetMusic);
            AddSliderRow(panel.transform, L10n.Get("settings.sfx"), ref y, SaveSystem.Current.sfxVolume, 0f, 1f, SettingsService.SetSfx);
            AddSliderRow(panel.transform, L10n.Get("settings.engine"), ref y, SaveSystem.Current.engineVolume, 0f, 1f, SettingsService.SetEngine);

            var back = MakeButton(panel.transform, "Back", "BACK", new Vector2(0, -660), () => Hide());
            back.transform.SetAsLastSibling();

            building = false;
            root.SetActive(false);
        }

        public void Show()
        {
            if (root == null) return;
            Refresh();
            root.SetActive(true);
        }

        public void Hide()
        {
            if (root != null) root.SetActive(false);
            OnClosed?.Invoke();
        }

        public bool IsVisible => root != null && root.activeSelf;

        private void Refresh()
        {
            var d = SaveSystem.Current;
            if (graphicsValue) graphicsValue.text = SettingsService.GraphicsNames[Mathf.Clamp(d.graphicsLevel, 0, 3)];
            if (fpsValue) fpsValue.text = d.targetFps + " FPS";
            if (controlValue) controlValue.text = SettingsService.ControlNames[d.controlScheme];
            if (vibToggle) vibToggle.isOn = d.vibration;
        }

        // ---- row builders ----
        private Text AddCycleRow(Transform parent, string label, ref float y, System.Func<string> onCycle)
        {
            UiTheme.Label(parent, label + "L", label, new Vector2(0, 1), new Vector2(50, y),
                new Vector2(380, 50), 20, UiTheme.Muted, TextAnchor.MiddleLeft);
            var btnImg = UiTheme.PanelBox(parent, label + "B", new Vector2(1, 1), new Vector2(-50, y),
                new Vector2(320, 52), new Color(0.05f, 0.08f, 0.12f, 0.9f), UiTheme.Cyan);
            var txt = UiTheme.Label(btnImg.transform, "V", "", new Vector2(0.5f, 0.5f), Vector2.zero,
                new Vector2(320, 52), 20, UiTheme.Text, TextAnchor.MiddleCenter);
            txt.rectTransform.anchorMin = Vector2.zero; txt.rectTransform.anchorMax = Vector2.one;
            txt.rectTransform.offsetMin = txt.rectTransform.offsetMax = Vector2.zero;
            var btn = btnImg.gameObject.AddComponent<Button>();
            btn.targetGraphic = btnImg;
            btn.onClick.AddListener(() =>
            {
                if (building) return;
                SfxPlayer.PlayUi("SFX/sfx_ui_click", 0.8f);
                txt.text = onCycle();
            });
            y -= 74f;
            return txt;
        }

        private Toggle AddToggleRow(Transform parent, string label, ref float y, UnityAction<bool> onChange)
        {
            UiTheme.Label(parent, label + "L", label, new Vector2(0, 1), new Vector2(50, y),
                new Vector2(380, 50), 20, UiTheme.Muted, TextAnchor.MiddleLeft);
            var tgo = new GameObject(label + "T");
            tgo.transform.SetParent(parent, false);
            var trt = tgo.AddComponent<RectTransform>();
            trt.anchorMin = trt.anchorMax = new Vector2(1, 1);
            trt.pivot = new Vector2(1, 1);
            trt.sizeDelta = new Vector2(60, 52);
            trt.anchoredPosition = new Vector2(-50, y);
            var bg = tgo.AddComponent<Image>();
            bg.color = new Color(0.05f, 0.08f, 0.12f, 0.9f);
            var toggle = tgo.AddComponent<Toggle>();
            var check = new GameObject("Check");
            check.transform.SetParent(tgo.transform, false);
            var crt = check.AddComponent<RectTransform>();
            crt.anchorMin = crt.anchorMax = Vector2.one * 0.5f;
            crt.sizeDelta = new Vector2(28, 28);
            var checkImg = check.AddComponent<Image>();
            checkImg.color = UiTheme.Cyan;
            toggle.graphic = checkImg;
            toggle.targetGraphic = bg;
            toggle.onValueChanged.AddListener(v => { if (!building) { SfxPlayer.PlayUi("SFX/sfx_ui_click", 0.8f); onChange(v); } });
            y -= 74f;
            return toggle;
        }

        private void AddSliderRow(Transform parent, string label, ref float y, float value, float min, float max, UnityAction<float> onChange)
        {
            UiTheme.Label(parent, label + "L", label, new Vector2(0, 1), new Vector2(50, y),
                new Vector2(380, 44), 18, UiTheme.Muted, TextAnchor.MiddleLeft);
            var sliderGo = new GameObject(label + "S");
            sliderGo.transform.SetParent(parent, false);
            var srt = sliderGo.AddComponent<RectTransform>();
            srt.anchorMin = srt.anchorMax = new Vector2(1, 1);
            srt.pivot = new Vector2(1, 1);
            srt.sizeDelta = new Vector2(320, 44);
            srt.anchoredPosition = new Vector2(-50, y);
            var bg = new GameObject("Bg", typeof(Image));
            bg.transform.SetParent(sliderGo.transform, false);
            var brt = bg.GetComponent<RectTransform>();
            brt.anchorMin = new Vector2(0, 0.5f); brt.anchorMax = new Vector2(1, 0.5f);
            brt.sizeDelta = new Vector2(0, 8); brt.anchoredPosition = Vector2.zero;
            bg.GetComponent<Image>().color = new Color(0.1f, 0.12f, 0.18f);
            var fill = new GameObject("Fill", typeof(Image));
            fill.transform.SetParent(bg.transform, false);
            var frt = fill.GetComponent<RectTransform>();
            frt.anchorMin = new Vector2(0, 0); frt.anchorMax = new Vector2(0, 1);
            frt.sizeDelta = new Vector2(16, 0);
            fill.GetComponent<Image>().color = UiTheme.Cyan;
            var slider = sliderGo.AddComponent<Slider>();
            slider.fillRect = frt;
            slider.targetGraphic = bg.GetComponent<Image>();
            slider.direction = Slider.Direction.LeftToRight;
            slider.minValue = min; slider.maxValue = max;
            slider.value = value;
            slider.onValueChanged.AddListener(v => { if (!building) onChange(v); });
            y -= 64f;
        }

        private Image MakeButton(Transform parent, string name, string label, Vector2 pos, UnityEngine.Events.UnityAction click)
        {
            var img = UiTheme.PanelBox(parent, name, new Vector2(0.5f, 1f), pos, new Vector2(300, 64),
                UiTheme.Panel, UiTheme.Magenta);
            var t = UiTheme.Label(img.transform, "L", label, new Vector2(0.5f, 0.5f), Vector2.zero,
                new Vector2(300, 64), 22, UiTheme.Text, TextAnchor.MiddleCenter);
            t.rectTransform.anchorMin = Vector2.zero; t.rectTransform.anchorMax = Vector2.one;
            t.rectTransform.offsetMin = t.rectTransform.offsetMax = Vector2.zero;
            var btn = img.gameObject.AddComponent<Button>();
            btn.targetGraphic = img;
            btn.onClick.AddListener(() => { SfxPlayer.PlayUi("SFX/sfx_ui_back", 0.8f); click(); });
            return img;
        }

        private static void Stretch(RectTransform rt)
        {
            rt.anchorMin = Vector2.zero;
            rt.anchorMax = Vector2.one;
            rt.offsetMin = rt.offsetMax = Vector2.zero;
        }
    }
}

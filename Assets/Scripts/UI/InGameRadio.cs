using UnityEngine;
using UnityEngine.UI;
using NeonRush.Audio;
using NeonRush.Save;

namespace NeonRush.UI
{
    /// <summary>
    /// In-car radio: device-music chip (song name shown briefly like a real
    /// head unit), plus a playlist browser. Uses DeviceMusicBridge; if music
    /// permission is missing it shows the retry message instead of crashing.
    /// </summary>
    public class InGameRadio : MonoBehaviour
    {
        public float chipVisibleSeconds = 4f;

        private GameObject root;
        private GameObject chip;
        private Text chipTitle;
        private Text chipArtist;
        private float chipTimer;
        private bool libraryLoaded;

        public void Build(Transform canvas)
        {
            if (root != null) return;

            root = new GameObject("RadioPanel");
            root.transform.SetParent(canvas, false);
            var rt = root.AddComponent<RectTransform>();
            rt.anchorMin = rt.anchorMax = Vector2.one;
            rt.offsetMin = rt.offsetMax = Vector2.zero;
            root.AddComponent<Image>().color = new Color(0.01f, 0.02f, 0.04f, 0.94f);
            root.SetActive(false);

            UiTheme.Label(root.transform, "Title", "DEVICE MUSIC", new Vector2(0.5f, 1f),
                new Vector2(0, -70), new Vector2(800, 60), 34, UiTheme.Cyan, TextAnchor.UpperCenter);
            UiTheme.Label(root.transform, "Sub", "Plays from MediaStore — nothing is copied. Bluetooth routing untouched.",
                new Vector2(0.5f, 1f), new Vector2(0, -118), new Vector2(900, 36), 16, UiTheme.Muted, TextAnchor.UpperCenter);

            // status / retry message
            var statusText = UiTheme.Label(root.transform, "Status", "", new Vector2(0.5f, 0.5f),
                new Vector2(0, 150), new Vector2(800, 40), 20, UiTheme.Magenta, TextAnchor.MiddleCenter);
            statusText.name = "RadioStatus";

            var list = UiTheme.PanelBox(root.transform, "List", new Vector2(0.5f, 0.5f), Vector2.zero,
                new Vector2(900, 560), UiTheme.Panel, UiTheme.Cyan);
            list.name = "RadioList";

            MakeCtrl(root.transform, "Close", "CLOSE", new Vector2(0, -90), () => Hide());

            var bridge = EnsureBridge();
            if (bridge != null)
            {
                bridge.OnLibraryLoaded -= OnLoaded;
                bridge.OnLibraryLoaded += OnLoaded;
                bridge.OnTrackStarted -= OnTrack;
                bridge.OnTrackStarted += OnTrack;
                bridge.OnPermissionFailed -= OnPermFail;
                bridge.OnPermissionFailed += OnPermFail;
            }

            BuildChip(canvas);
        }

        /// <summary>Small song chip visible briefly while racing.</summary>
        private void BuildChip(Transform canvas)
        {
            chip = new GameObject("RadioChip");
            chip.transform.SetParent(canvas, false);
            var img = chip.AddComponent<Image>();
            img.color = new Color(0.02f, 0.03f, 0.06f, 0.85f);
            var rt = chip.GetComponent<RectTransform>();
            rt.anchorMin = rt.anchorMax = new Vector2(0.5f, 1f);
            rt.pivot = new Vector2(0.5f, 1f);
            rt.sizeDelta = new Vector2(460, 84);
            rt.anchoredPosition = new Vector2(0, -170);

            UiTheme.Label(chip.transform, "I", "♪", new Vector2(0, 1), new Vector2(24, -14),
                new Vector2(40, 40), 30, UiTheme.Cyan, TextAnchor.MiddleLeft);
            chipTitle = UiTheme.Label(chip.transform, "T", "", new Vector2(0, 1), new Vector2(70, -12),
                new Vector2(370, 34), 20, UiTheme.Text, TextAnchor.MiddleLeft);
            chipArtist = UiTheme.Label(chip.transform, "A", "", new Vector2(0, 1), new Vector2(70, -46),
                new Vector2(370, 26), 16, UiTheme.Muted, TextAnchor.MiddleLeft);
            chip.SetActive(false);
        }

        private void Update()
        {
            if (chip != null && chip.activeSelf)
            {
                chipTimer -= Time.unscaledDeltaTime;
                if (chipTimer <= 0f) chip.SetActive(false);
            }
        }

        public void Toggle()
        {
            if (root == null) return;
            bool show = !root.activeSelf;
            if (show)
            {
                root.SetActive(true);
                var bridge = EnsureBridge();
                if (bridge == null) return;
                if (bridge.CheckPermission())
                {
                    if (!libraryLoaded) bridge.LoadLibrary();
                }
                else
                {
                    SetStatus("Music access is required to select songs from your device.");
                }
            }
            else root.SetActive(false);
        }

        public void Hide() { if (root != null) root.SetActive(false); }

        private void OnLoaded()
        {
            libraryLoaded = true;
            var bridge = DeviceMusicBridge.Instance;
            if (bridge == null) return;
            var list = root?.transform.Find("RadioList");
            if (list == null) return;

            for (int i = list.childCount - 1; i >= 0; i--)
                Destroy(list.GetChild(i).gameObject);

            float y = -40f;
            int shown = 0;
            foreach (var t in bridge.Tracks)
            {
                if (shown++ >= 9) break;
                int idx = shown - 1;
                var row = UiTheme.PanelBox(list, "Track" + idx, new Vector2(0.5f, 1f),
                    new Vector2(0, y), new Vector2(840, 48), UiTheme.Panel, UiTheme.Cyan);
                UiTheme.Label(row.transform, "T", t.title, new Vector2(0, 0.5f), new Vector2(20, 0),
                    new Vector2(500, 30), 19, UiTheme.Text, TextAnchor.MiddleLeft);
                UiTheme.Label(row.transform, "A", t.artist, new Vector2(1, 0.5f), new Vector2(-20, 0),
                    new Vector2(280, 26), 15, UiTheme.Muted, TextAnchor.MiddleRight);
                var btn = row.gameObject.AddComponent<Button>();
                btn.targetGraphic = row;
                btn.onClick.AddListener(() =>
                {
                    Audio.SfxPlayer.PlayUi("SFX/sfx_ui_click", 0.8f);
                    bridge.Play(idx);
                });
                y -= 58f;
            }
            SetStatus("");
        }

        private void OnTrack(string info)
        {
            var parts = info.Split(new[] { " — " }, System.StringSplitOptions.None);
            if (chipTitle != null && parts.Length > 0) chipTitle.text = parts[0];
            if (chipArtist != null) chipArtist.text = parts.Length > 1 ? parts[1] : "";
            if (chip != null) { chip.SetActive(true); chipTimer = chipVisibleSeconds; }
        }

        private void OnPermFail() => SetStatus("Music access is required to select songs from your device.");

        private void SetStatus(string msg)
        {
            var st = root?.transform.Find("RadioStatus")?.GetComponent<Text>();
            if (st != null) st.text = msg;

            // TRY AGAIN button only appears while permission missing
            var retry = root?.transform.Find("Retry")?.gameObject;
            if (retry != null) retry.SetActive(!string.IsNullOrEmpty(msg));
            if (!string.IsNullOrEmpty(msg) && retry == null)
            {
                var img = UiTheme.PanelBox(root.transform, "Retry", new Vector2(0.5f, 0.5f),
                    new Vector2(0, 90), new Vector2(260, 60), new Color(0f, 0.94f, 1f, 0.18f), UiTheme.Cyan);
                var t = UiTheme.Label(img.transform, "L", "TRY AGAIN", new Vector2(0.5f, 0.5f), Vector2.zero,
                    new Vector2(260, 60), 20, UiTheme.Cyan, TextAnchor.MiddleCenter);
                t.rectTransform.anchorMin = Vector2.zero; t.rectTransform.anchorMax = Vector2.one;
                t.rectTransform.offsetMin = t.rectTransform.offsetMax = Vector2.zero;
                var btn = img.gameObject.AddComponent<Button>();
                btn.targetGraphic = img;
                btn.onClick.AddListener(() =>
                {
                    Audio.SfxPlayer.PlayUi("SFX/sfx_ui_click", 0.8f);
                    var bridge = DeviceMusicBridge.Instance;
                    if (bridge != null) bridge.RequestPermission();
                });
            }
        }

        private static DeviceMusicBridge EnsureBridge()
        {
            var bridge = DeviceMusicBridge.Instance;
            if (bridge == null)
            {
                var go = new GameObject("DeviceMusicBridge");
                bridge = go.AddComponent<DeviceMusicBridge>();
            }
            return bridge;
        }

        private void MakeCtrl(Transform parent, string name, string label, Vector2 pos, UnityEngine.Events.UnityAction click)
        {
            var img = UiTheme.PanelBox(parent, name, new Vector2(0.5f, 0), pos, new Vector2(240, 60),
                UiTheme.Panel, UiTheme.Magenta);
            var t = UiTheme.Label(img.transform, "L", label, new Vector2(0.5f, 0.5f), Vector2.zero,
                new Vector2(240, 60), 20, UiTheme.Text, TextAnchor.MiddleCenter);
            t.rectTransform.anchorMin = Vector2.zero; t.rectTransform.anchorMax = Vector2.one;
            t.rectTransform.offsetMin = t.rectTransform.offsetMax = Vector2.zero;
            var btn = img.gameObject.AddComponent<Button>();
            btn.targetGraphic = img;
            btn.onClick.AddListener(() => { Audio.SfxPlayer.PlayUi("SFX/sfx_ui_back", 0.8f); click(); });
        }
    }
}

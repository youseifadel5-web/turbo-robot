using UnityEngine;
using UnityEngine.UI;
using NeonRush.Career;
using NeonRush.Save;
using NeonRush.Audio;
using NeonRush.Tracks;

namespace NeonRush.UI
{
    /// <summary>
    /// Career browser (runtime-built): chapters 1-3, event list with stars,
    /// current-progress highlight, rewards preview. Selecting an event asks
    /// the race flow to build a matching track.
    /// </summary>
    public class CareerScreen : MonoBehaviour
    {
        public System.Action<TrackConfig, string> OnEventSelected;

        private GameObject root;
        private Transform listHolder;
        private Text chapterText;

        public void Build(Transform canvas)
        {
            if (root != null) return;
            root = new GameObject("CareerPanel");
            root.transform.SetParent(canvas, false);
            var rt = root.AddComponent<RectTransform>();
            rt.anchorMin = Vector2.zero; rt.anchorMax = Vector2.one;
            rt.offsetMin = rt.offsetMax = Vector2.zero;
            root.AddComponent<Image>().color = new Color(0.01f, 0.02f, 0.04f, 0.92f);

            UiTheme.Label(root.transform, "Title", "CAREER", new Vector2(0.5f, 1f),
                new Vector2(0, -70), new Vector2(800, 60), 38, UiTheme.Magenta, TextAnchor.UpperCenter);
            chapterText = UiTheme.Label(root.transform, "Chapter", "", new Vector2(0.5f, 1f),
                new Vector2(0, -125), new Vector2(800, 40), 22, UiTheme.Muted, TextAnchor.UpperCenter);

            var panel = UiTheme.PanelBox(root.transform, "Body", new Vector2(0.5f, 0.5f), Vector2.zero,
                new Vector2(900, 700), UiTheme.Panel, UiTheme.Magenta);
            listHolder = panel.transform;

            // nav buttons
            MakeBtn(root.transform, "Prev", "< PREV", new Vector2(-260, 40), new Vector2(0, 0), () => CycleChapter(-1));
            MakeBtn(root.transform, "Next", "NEXT >", new Vector2(260, 40), new Vector2(0, 0), () => CycleChapter(1));
            MakeBtn(root.transform, "Close", "CLOSE", new Vector2(0, -40), new Vector2(0, 0), Hide);

            root.SetActive(false);
        }

        private int viewChapter;

        public void Show()
        {
            var d = SaveSystem.Load();
            viewChapter = Mathf.Clamp(d.careerChapter, 1, CareerCatalog.TotalChapters);
            RefreshList();
            if (root != null) root.SetActive(true);
        }

        public void Hide() { if (root != null) root.SetActive(false); }
        public bool IsVisible => root != null && root.activeSelf;

        private void CycleChapter(int dir)
        {
            viewChapter = Mathf.Clamp(viewChapter + dir, 1, CareerCatalog.TotalChapters);
            SfxPlayer.PlayUi("SFX/sfx_ui_click", 0.7f);
            RefreshList();
        }

        private void RefreshList()
        {
            if (listHolder == null) return;
            // clear previous rows
            for (int i = listHolder.childCount - 1; i >= 0; i--)
            {
                var c = listHolder.GetChild(i);
                if (c.name.StartsWith("EV_")) Destroy(c.gameObject);
            }

            var data = SaveSystem.Current;
            if (chapterText != null)
                chapterText.text = $"CHAPTER {viewChapter} / {CareerCatalog.TotalChapters}   •   TOTAL STARS {data.careerStars}";

            var events = CareerCatalog.GetEvents(viewChapter);
            bool chapterUnlocked = viewChapter <= data.careerChapter;

            float y = -70f;
            foreach (var ev in events)
            {
                bool isCurrent = chapterUnlocked && viewChapter == data.careerChapter &&
                                 ev.id == CurrentEventId(data);
                bool unlocked = chapterUnlocked && EventReached(data, ev);

                var row = UiTheme.PanelBox(listHolder, "EV_" + ev.id, new Vector2(0.5f, 1f),
                    new Vector2(0, y), new Vector2(820, 92),
                    unlocked ? (isCurrent ? new Color(1f, 0.17f, 0.84f, 0.18f) : UiTheme.Panel)
                            : new Color(0.03f, 0.04f, 0.06f, 0.85f),
                    isCurrent ? UiTheme.Magenta : (unlocked ? UiTheme.Cyan : new Color(0.2f, 0.22f, 0.28f)));

                UiTheme.Label(row.transform, "T", ev.title, new Vector2(0, 1), new Vector2(30, -12),
                    new Vector2(500, 34), 22, unlocked ? UiTheme.Text : UiTheme.Muted, TextAnchor.MiddleLeft);
                UiTheme.Label(row.transform, "S", EventSubtitle(ev), new Vector2(0, 1), new Vector2(30, -48),
                    new Vector2(500, 28), 16, UiTheme.Muted, TextAnchor.MiddleLeft);
                UiTheme.Label(row.transform, "R", "★".PadRight(1) + "  " + ev.rewardCoins + " ●",
                    new Vector2(1, 1), new Vector2(-30, -30), new Vector2(200, 60), 20,
                    unlocked ? UiTheme.Gold : UiTheme.Muted, TextAnchor.MiddleRight);

                if (unlocked)
                {
                    var btn = row.gameObject.AddComponent<Button>();
                    btn.targetGraphic = row;
                    btn.onClick.AddListener(() =>
                    {
                        SfxPlayer.PlayUi("SFX/sfx_ui_click", 0.8f);
                        Hide();
                        OnEventSelected?.Invoke(TrackFor(ev), ev.type.ToString());
                    });
                }
                else
                {
                    UiTheme.Label(row.transform, "Lock", "LOCKED", new Vector2(1, 0.5f), new Vector2(-30, 0),
                        new Vector2(180, 30), 18, new Color(0.45f, 0.47f, 0.55f), TextAnchor.MiddleRight);
                }
                y -= 104f;
            }
        }

        private static string CurrentEventId(SaveData d)
        {
            var evs = CareerCatalog.GetEvents(Mathf.Clamp(d.careerChapter, 1, CareerCatalog.TotalChapters));
            int i = Mathf.Clamp(d.careerEvent - 1, 0, evs.Length - 1);
            return evs[i].id;
        }

        private static bool EventReached(SaveData d, CareerEvent ev)
        {
            if (d.careerChapter > ev.chapter) return true; // previous chapters fully done
            if (d.careerChapter < ev.chapter) return false;
            var evs = CareerCatalog.GetEvents(ev.chapter);
            int idx = System.Array.FindIndex(evs, x => x.id == ev.id);
            return idx < d.careerEvent - 1 || ev.id == CurrentEventId(d);
        }

        private static string EventSubtitle(CareerEvent ev)
        {
            string type = ev.type.ToString().ToUpper();
            return $"{type}  •  {ev.laps} LAP{(ev.laps > 1 ? "S" : "")}  •  {ev.rewardXp} XP";
        }

        private static TrackConfig TrackFor(CareerEvent ev)
        {
            var cfg = ScriptableObject.CreateInstance<TrackConfig>();
            cfg.trackId = "career_" + ev.id;
            cfg.displayName = ev.title;
            cfg.lengthPreset = ev.length;
            cfg.laps = Mathf.Max(1, ev.laps);
            return cfg;
        }

        private void MakeBtn(Transform parent, string name, string label, Vector2 pos, Vector2 anchor, UnityEngine.Events.UnityAction click)
        {
            var img = UiTheme.PanelBox(parent, name, new Vector2(0.5f, 0), pos, new Vector2(220, 60),
                UiTheme.Panel, UiTheme.Cyan);
            var t = UiTheme.Label(img.transform, "L", label, new Vector2(0.5f, 0.5f), Vector2.zero,
                new Vector2(220, 60), 20, UiTheme.Text, TextAnchor.MiddleCenter);
            t.rectTransform.anchorMin = Vector2.zero; t.rectTransform.anchorMax = Vector2.one;
            t.rectTransform.offsetMin = t.rectTransform.offsetMax = Vector2.zero;
            var btn = img.gameObject.AddComponent<Button>();
            btn.targetGraphic = img;
            btn.onClick.AddListener(() => { SfxPlayer.PlayUi("SFX/sfx_ui_click", 0.7f); click(); });
        }
    }
}

using UnityEngine;
using UnityEngine.UI;
using System.Collections.Generic;
using NeonRush.Cars;

namespace NeonRush.UI
{
    /// <summary>
    /// Live race leaderboard (reference look): position numbers + names,
    /// the player row highlighted in cyan, updated from RacePositionTracker.
    /// </summary>
    public class LeaderboardController : MonoBehaviour
    {
        public RacePositionTracker tracker;
        public float refreshSeconds = 0.25f;

        private class Row
        {
            public Text text;
            public Image bg;
            public ArcadeCarController car;
        }

        private readonly List<Row> rows = new List<Row>();
        private Transform panel;
        private float timer;

        public void Setup(RacePositionTracker raceTracker, Transform hudCanvas)
        {
            tracker = raceTracker;
            if (hudCanvas == null || tracker == null) return;

            var go = new GameObject("Leaderboard");
            go.transform.SetParent(hudCanvas, false);
            var rt = go.AddComponent<RectTransform>();
            rt.anchorMin = rt.anchorMax = new Vector2(0f, 1f);
            rt.pivot = new Vector2(0f, 1f);
            rt.anchoredPosition = new Vector2(40f, -110f);
            rt.sizeDelta = new Vector2(330f, 420f);

            panel = go.transform;
            Rebuild();
        }

        private void Rebuild()
        {
            foreach (var r in rows)
                if (r.text != null) Destroy(r.text.gameObject);
            if (rows.Count > 0 && rows[0].bg != null) Destroy(rows[0].bg.gameObject);
            rows.Clear();

            if (tracker == null) return;

            var entries = new List<(ArcadeCarController car, string name)>();
            entries.Add((tracker.player, "YOU"));
            foreach (var r in tracker.rivals)
            {
                string n = r != null && r.stats != null ? r.stats.displayName.ToUpper() : "RIVAL";
                entries.Add((r, Short(n)));
            }
            if (entries.Count == 0) return;

            float y = 0f;
            foreach (var e in entries)
            {
                bool isPlayer = e.car == tracker.player;

                // background pill
                var bgGo = new GameObject("RowBg_" + e.name);
                bgGo.transform.SetParent(panel, false);
                var brt = bgGo.AddComponent<RectTransform>();
                brt.anchorMin = brt.anchorMax = new Vector2(0f, 1f);
                brt.pivot = new Vector2(0f, 1f);
                brt.anchoredPosition = new Vector2(0f, y);
                brt.sizeDelta = new Vector2(300f, 44f);
                var bg = bgGo.AddComponent<Image>();
                bg.color = isPlayer ? new Color(0f, 0.5f, 0.6f, 0.42f)
                                   : new Color(0.02f, 0.03f, 0.05f, 0.5f);

                var t = UiTheme.Label(bgGo.transform, "L", "", new Vector2(0.5f, 0.5f), Vector2.zero,
                    new Vector2(300f, 40f), 20, isPlayer ? UiTheme.Cyan : UiTheme.Text, TextAnchor.MiddleLeft);
                t.rectTransform.anchorMin = Vector2.zero; t.rectTransform.anchorMax = Vector2.one;
                t.rectTransform.offsetMin = new Vector2(14f, 0f);
                t.rectTransform.offsetMax = new Vector2(-14f, 0f);

                rows.Add(new Row { text = t, bg = bg, car = e.car });
                y -= 52f;
            }
        }

        private static string Short(string name)
        {
            var parts = name.Split(' ');
            if (parts.Length == 1) return name.Length > 9 ? name.Substring(0, 9) : name;
            return (parts[0][0] + parts[1]).ToUpper();
        }

        private void Update()
        {
            if (tracker == null) return;
            timer -= Time.unscaledDeltaTime;
            if (timer > 0f) return;
            timer = refreshSeconds;

            // sort rows by total progress
            var sorted = new List<Row>(rows);
            sorted.Sort((a, b) => tracker.GetTotalProgress(b.car).CompareTo(tracker.GetTotalProgress(a.car)));

            for (int i = 0; i < sorted.Count; i++)
            {
                var row = sorted[i];
                if (row == null || row.text == null) continue;
                string name = i == 0 && row.car == tracker.player ? "YOU" :
                    (row.car != null && row.car.stats != null ? Short(row.car.stats.displayName.ToUpper()) : "RIVAL");
                row.text.text = $"{i + 1}   {name}";
            }
        }
    }

    public static class LeaderboardTrackerExtensions
    {
        /// <summary>Total progress of a car (0 if unknown).</summary>
        public static float GetTotalProgress(this RacePositionTracker t, ArcadeCarController car)
        {
            if (t == null || car == null) return 0f;
            if (car == t.player) return t.PlayerTotalProgress;
            // rivals: approximate via line progress + lap data exposed by tracker
            return t.GetRivalTotalProgress(car);
        }
    }
}

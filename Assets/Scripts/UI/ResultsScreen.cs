using UnityEngine;
using UnityEngine.UI;
using NeonRush.Racing;
using NeonRush.Career;
using NeonRush.Save;

namespace NeonRush.UI
{
    /// <summary>
    /// End-of-race panel: position, time, drift, rewards.
    /// Bind texts in Inspector. RaceManager.OnRaceFinished += Show.
    /// </summary>
    public class ResultsScreen : MonoBehaviour
    {
        public GameObject root;
        public Text positionText;
        public Text timeText;
        public Text bestLapText;
        public Text topSpeedText;
        public Text driftText;
        public Text comboText;
        public Text coinsText;
        public Text xpText;
        public CareerSystem career;

        private void OnEnable()
        {
            var rm = FindObjectOfType<RaceManager>();
            if (rm != null) rm.OnRaceFinished += Show;
        }

        private void OnDisable()
        {
            var rm = FindObjectOfType<RaceManager>();
            if (rm != null) rm.OnRaceFinished -= Show;
        }

        public void Show(RaceResults r)
        {
            if (r == null) return;
            if (root != null) root.SetActive(true);

            if (positionText) positionText.text = "P" + r.position + " / " + r.totalRacers;
            if (timeText) timeText.text = Format(r.totalTime);
            if (bestLapText) bestLapText.text = Format(r.bestLap);
            if (topSpeedText) topSpeedText.text = Mathf.RoundToInt(r.topSpeedKmh) + " KM/H";
            if (driftText) driftText.text = Mathf.RoundToInt(r.driftScore).ToString();
            if (comboText) comboText.text = "x" + r.maxCombo;

            int coins = 400 + Mathf.Max(0, (r.totalRacers - r.position + 1) * 180) + Mathf.RoundToInt(r.driftScore * 0.05f);
            int xp = 20 + (r.position == 1 ? 30 : 10);
            r.rewardCoins = coins;
            r.rewardXp = xp;

            SaveSystem.AddCoins(coins);
            SaveSystem.AddXp(xp);
            SaveSystem.RecordRace(r.totalTime, r.driftScore);

            if (career != null) career.CompleteEvent(r);

            if (coinsText) coinsText.text = "+" + coins;
            if (xpText) xpText.text = "+" + xp + " XP";
        }

        public void Hide()
        {
            if (root != null) root.SetActive(false);
        }

        private static string Format(float t)
        {
            int m = (int)(t / 60f);
            return string.Format("{0:00}:{1:00.00}", m, t % 60f);
        }
    }
}

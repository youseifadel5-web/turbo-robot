using UnityEngine;
using UnityEngine.UI;
using NeonRush.Cars;

namespace NeonRush.UI
{
    /// <summary>
    /// Race HUD using uGUI Text (works without TextMeshPro installed).
    /// RaceHudBuilder can create and bind this at runtime.
    /// </summary>
    public class RuntimeRaceHUD : MonoBehaviour
    {
        public ArcadeCarController car;

        public Text positionText;
        public Text lapText;
        public Text timerText;
        public Text progressText;
        public Text speedText;
        public Text unitText;
        public Text gearText;
        public Slider nitroBar;
        public Text nitroLabel;
        public UnityEngine.UI.Image speedRing;
        public UnityEngine.UI.Image nitroRing;
        public GameObject nitroShockwaveFx;
        public GameObject driftPanel;
        public Text driftScoreText;
        public Text driftComboText;
        public Text driftPopupText;
        public GameObject wrongWayPanel;
        public Text checkpointFlashText;
        public RawImage minimapImage;

        private float driftScore;
        private int combo;
        private float comboTimer;
        private float popupTimer;
        private float raceTime;
        private bool racing;

        private void OnEnable()
        {
            if (car != null)
            {
                car.OnNitroStarted += HandleNitroStart;
                car.OnDriftScoreTick += AddDriftScore;
            }
        }

        private void OnDisable()
        {
            if (car != null)
            {
                car.OnNitroStarted -= HandleNitroStart;
                car.OnDriftScoreTick -= AddDriftScore;
            }
        }

        public void BindCar(ArcadeCarController c)
        {
            if (car != null)
            {
                car.OnNitroStarted -= HandleNitroStart;
                car.OnDriftScoreTick -= AddDriftScore;
            }
            car = c;
            if (car != null)
            {
                car.OnNitroStarted += HandleNitroStart;
                car.OnDriftScoreTick += AddDriftScore;
            }
        }

        public void StartRaceTimer()
        {
            raceTime = 0f;
            racing = true;
            driftScore = 0f;
            combo = 0;
        }

        public void StopRaceTimer() => racing = false;

        private void Update()
        {
            if (car == null) return;
            if (racing) raceTime += Time.deltaTime;

            if (speedText) speedText.text = Mathf.RoundToInt(car.SpeedKmh).ToString("000");
            if (gearText) gearText.text = "G" + car.CurrentGear;
            if (nitroBar) nitroBar.value = car.NitroNormalized;
            if (nitroRing) nitroRing.fillAmount = car.NitroNormalized * 0.92f;
            if (speedRing) speedRing.fillAmount = Mathf.Clamp01(car.SpeedKmh / 320f) * 0.92f;
            if (nitroLabel)
                nitroLabel.text = car.IsNitroActive ? "NITRO SHOCKWAVE" :
                    (car.NitroNormalized > 0.98f ? "NITRO READY" : "RECHARGING");

            if (driftPanel) driftPanel.SetActive(car.IsDrifting || comboTimer > 0f);
            if (car.IsDrifting) comboTimer = 1.2f;
            else
            {
                comboTimer -= Time.deltaTime;
                if (comboTimer <= 0f) combo = 0;
            }

            if (driftScoreText) driftScoreText.text = Mathf.RoundToInt(driftScore).ToString();
            if (driftComboText) driftComboText.text = combo > 1 ? "x" + combo : "";

            if (popupTimer > 0f)
            {
                popupTimer -= Time.deltaTime;
                if (popupTimer <= 0f && driftPopupText)
                    driftPopupText.gameObject.SetActive(false);
            }

            if (timerText && racing) timerText.text = FormatTime(raceTime);
        }

        private void AddDriftScore(float tick)
        {
            float mult = 1f + combo * 0.15f;
            driftScore += tick * mult;
            if (combo < 9) combo++;
            comboTimer = 1.2f;
            if (driftPopupText)
            {
                int meters = Mathf.RoundToInt(tick * 8f);
                driftPopupText.text = (meters > 15 ? "PERFECT DRIFT " : "GOOD DRIFT ") + meters + "M";
                driftPopupText.gameObject.SetActive(true);
                popupTimer = 1.1f;
            }
        }

        private void HandleNitroStart()
        {
            if (nitroShockwaveFx != null)
            {
                nitroShockwaveFx.SetActive(false);
                nitroShockwaveFx.SetActive(true);
            }
        }

        public void SetPosition(int pos, int total)
        {
            if (positionText) positionText.text = pos + "/" + total;
        }

        public void SetLap(int current, int total)
        {
            if (lapText) lapText.text = "LAP " + current + "/" + total;
        }

        public void SetProgress(float normalized)
        {
            if (progressText)
                progressText.text = "DIST " + Mathf.RoundToInt(normalized * 100f) + "%";
        }

        public void ShowWrongWay(bool show)
        {
            if (wrongWayPanel) wrongWayPanel.SetActive(show);
        }

        public void FlashCheckpoint(string msg = "CHECKPOINT")
        {
            if (checkpointFlashText)
            {
                checkpointFlashText.text = msg;
                checkpointFlashText.gameObject.SetActive(true);
                CancelInvoke(nameof(HideCheckpoint));
                Invoke(nameof(HideCheckpoint), 1.2f);
            }
        }

        private void HideCheckpoint()
        {
            if (checkpointFlashText) checkpointFlashText.gameObject.SetActive(false);
        }

        private static string FormatTime(float t)
        {
            int m = (int)(t / 60f);
            return string.Format("{0:00}:{1:00.00}", m, t % 60f);
        }

        public float GetDriftScore() => driftScore;
        public float GetRaceTime() => raceTime;
    }
}

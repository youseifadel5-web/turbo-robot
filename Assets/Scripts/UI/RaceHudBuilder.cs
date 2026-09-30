using UnityEngine;
using UnityEngine.UI;
using NeonRush.Cars;

namespace NeonRush.UI
{
    /// <summary>
    /// Builds race HUD Canvas + touch controls and binds RuntimeRaceHUD.
    /// </summary>
    public class RaceHudBuilder : MonoBehaviour
    {
        public ArcadeCarController car;
        public MobileCarInput input;

        public RuntimeRaceHUD BuildAndBind()
        {
            if (car == null) car = FindObjectOfType<ArcadeCarController>();
            if (input == null && car != null) input = car.GetComponent<MobileCarInput>();

            var canvasGo = new GameObject("RaceHUD_Canvas");
            var canvas = canvasGo.AddComponent<Canvas>();
            canvas.renderMode = RenderMode.ScreenSpaceOverlay;
            canvas.sortingOrder = 100;
            var scaler = canvasGo.AddComponent<CanvasScaler>();
            scaler.uiScaleMode = CanvasScaler.ScaleMode.ScaleWithScreenSize;
            scaler.referenceResolution = new Vector2(1920, 1080);
            scaler.matchWidthOrHeight = 0.5f;
            canvasGo.AddComponent<GraphicRaycaster>();

            if (FindObjectOfType<UnityEngine.EventSystems.EventSystem>() == null)
            {
                var es = new GameObject("EventSystem");
                es.AddComponent<UnityEngine.EventSystems.EventSystem>();
                es.AddComponent<UnityEngine.EventSystems.StandaloneInputModule>();
            }

            var hud = canvasGo.AddComponent<RuntimeRaceHUD>();
            hud.car = car;

            hud.positionText = MakeText(canvasGo.transform, "Pos", "1/8",
                new Vector2(0, 1), new Vector2(40, -40), 42, TextAnchor.UpperLeft, new Color(0f, 0.94f, 1f));
            hud.lapText = MakeText(canvasGo.transform, "Lap", "LAP 1/3",
                new Vector2(0.5f, 1), new Vector2(-80, -36), 26, TextAnchor.UpperCenter, Color.white);
            hud.timerText = MakeText(canvasGo.transform, "Timer", "00:00.00",
                new Vector2(0.5f, 1), new Vector2(100, -36), 26, TextAnchor.UpperCenter, new Color(0f, 0.94f, 1f));

            // ---- Speedometer cluster (bottom-right, reference look) ----
            var cluster = new GameObject("SpeedCluster");
            cluster.transform.SetParent(canvasGo.transform, false);
            var crt = cluster.AddComponent<RectTransform>();
            crt.anchorMin = crt.anchorMax = new Vector2(1, 0);
            crt.pivot = new Vector2(0.5f, 0.5f);
            crt.sizeDelta = new Vector2(300, 300);
            crt.anchoredPosition = new Vector2(-190, 190);

            var disc = new GameObject("Disc", typeof(Image));
            disc.transform.SetParent(cluster.transform, false);
            var drt = disc.GetComponent<RectTransform>();
            drt.anchorMin = drt.anchorMax = Vector2.one * 0.5f;
            drt.sizeDelta = new Vector2(272, 272);
            disc.GetComponent<Image>().color = new Color(0.015f, 0.02f, 0.035f, 0.85f);

            hud.speedRing = MakeRing(cluster.transform, "SpeedRing", 260f, new Color(0f, 0.94f, 1f, 0.95f));
            hud.nitroRing = MakeRing(cluster.transform, "NitroRing", 208f, new Color(1f, 0.17f, 0.84f, 0.95f));

            hud.speedText = MakeText(cluster.transform, "Speed", "000",
                new Vector2(0.5f, 0.5f), new Vector2(0, 22), 72, TextAnchor.MiddleCenter, Color.white);
            hud.unitText = MakeText(cluster.transform, "Unit", "KM/H",
                new Vector2(0.5f, 0.5f), new Vector2(0, -26), 20, TextAnchor.MiddleCenter, new Color(0.6f, 0.65f, 0.7f));
            hud.gearText = MakeText(cluster.transform, "Gear", "G1",
                new Vector2(0.5f, 0.5f), new Vector2(0, -58), 24, TextAnchor.MiddleCenter, new Color(0f, 0.94f, 1f));
            hud.nitroLabel = MakeText(cluster.transform, "NitroLbl", "NITRO",
                new Vector2(0.5f, 0.5f), new Vector2(0, 62), 15, TextAnchor.MiddleCenter, new Color(0.75f, 0.45f, 1f));

            hud.nitroBar = MakeSlider(canvasGo.transform, "NitroBar",
                new Vector2(1, 0), new Vector2(-160, 60), new Vector2(220, 14));
            hud.driftPopupText = MakeText(canvasGo.transform, "DriftPopup", "",
                new Vector2(0.5f, 0.75f), Vector2.zero, 28, TextAnchor.MiddleCenter, new Color(0f, 0.94f, 1f));
            hud.driftPopupText.gameObject.SetActive(false);
            hud.driftScoreText = MakeText(canvasGo.transform, "DriftScore", "0",
                new Vector2(0, 0), new Vector2(40, 120), 22, TextAnchor.LowerLeft, new Color(1f, 0.17f, 0.84f));
            hud.driftComboText = MakeText(canvasGo.transform, "Combo", "",
                new Vector2(0, 0), new Vector2(40, 90), 18, TextAnchor.LowerLeft, new Color(1f, 0.17f, 0.84f));
            hud.driftPanel = hud.driftScoreText.gameObject;
            hud.checkpointFlashText = MakeText(canvasGo.transform, "Flash", "",
                new Vector2(0.5f, 0.6f), Vector2.zero, 36, TextAnchor.MiddleCenter, Color.white);
            hud.checkpointFlashText.gameObject.SetActive(false);

            if (input != null)
            {
                MakeHold(canvasGo.transform, "SteerL", "‹", new Vector2(0, 0), new Vector2(90, 90), new Vector2(100, 100),
                    () => input.SteerLeft(true), () => input.SteerLeft(false));
                MakeHold(canvasGo.transform, "SteerR", "›", new Vector2(0, 0), new Vector2(210, 90), new Vector2(100, 100),
                    () => input.SteerRight(true), () => input.SteerRight(false));
                MakeHold(canvasGo.transform, "Accel", "▲", new Vector2(1, 0), new Vector2(-90, 90), new Vector2(100, 100),
                    () => input.ThrottlePressed(true), () => input.ThrottlePressed(false));
                MakeHold(canvasGo.transform, "Brake", "■", new Vector2(1, 0), new Vector2(-90, 210), new Vector2(90, 90),
                    () => input.BrakePressed(true), () => input.BrakePressed(false));
                MakeHold(canvasGo.transform, "NitroBtn", "N", new Vector2(1, 0), new Vector2(-210, 90), new Vector2(90, 90),
                    () => input.SetNitro(true), () => input.SetNitro(false));
                MakeHold(canvasGo.transform, "DriftBtn", "D", new Vector2(0, 0), new Vector2(90, 210), new Vector2(90, 90),
                    () => input.SetDrift(true), () => input.SetDrift(false));
            }

            // Camera mode button (chase / close / far / hood / cockpit / bumper)
            var chase = car != null ? car.GetComponentInParent<ChaseCamera>() : null;
            if (chase == null && Camera.main != null) chase = Camera.main.GetComponent<ChaseCamera>();
            if (chase != null)
            {
                var camGo = new GameObject("CamModeBtn");
                camGo.transform.SetParent(canvasGo.transform, false);
                var camRt = camGo.AddComponent<RectTransform>();
                camRt.anchorMin = camRt.anchorMax = new Vector2(1, 1);
                camRt.pivot = new Vector2(0.5f, 0.5f);
                camRt.sizeDelta = new Vector2(150, 54);
                camRt.anchoredPosition = new Vector2(-110, -100);
                var camImg = camGo.AddComponent<Image>();
                camImg.color = new Color(0.08f, 0.1f, 0.14f, 0.8f);
                var camLabel = MakeText(camGo.transform, "L", "CAM: CHASE",
                    new Vector2(0.5f, 0.5f), Vector2.zero, 17, TextAnchor.MiddleCenter, new Color(0f, 0.94f, 1f));
                camLabel.rectTransform.anchorMin = Vector2.zero; camLabel.rectTransform.anchorMax = Vector2.one;
                camLabel.rectTransform.offsetMin = Vector2.zero; camLabel.rectTransform.offsetMax = Vector2.zero;
                var camBtn = camGo.AddComponent<Button>();
                camBtn.targetGraphic = camImg;
                camBtn.onClick.AddListener(() =>
                {
                    chase.CycleMode();
                    Audio.SfxPlayer.PlayUi("SFX/sfx_ui_click", 0.6f);
                    camLabel.text = "CAM: " + chase.mode.ToString().ToUpper();
                });
            }

            return hud;
        }

        /// <summary>Radial gauge ring (Image Radial360 fill).</summary>
        private static Image MakeRing(Transform parent, string name, float diameter, Color color)
        {
            // dark track ring
            var trackGo = new GameObject(name + "_Track");
            trackGo.transform.SetParent(parent, false);
            var trt = trackGo.AddComponent<RectTransform>();
            trt.anchorMin = trt.anchorMax = Vector2.one * 0.5f;
            trt.sizeDelta = new Vector2(diameter, diameter);
            var trackImg = trackGo.AddComponent<Image>();
            trackImg.type = Image.Type.Filled;
            trackImg.fillMethod = Image.FillMethod.Radial360;
            trackImg.fillOrigin = (int)Image.Origin360.Top;
            trackImg.fillClockwise = true;
            trackImg.fillAmount = 0.92f;
            trackImg.color = new Color(0.06f, 0.08f, 0.12f, 0.9f);

            var go = new GameObject(name);
            go.transform.SetParent(trackGo.transform, false);
            var rt = go.AddComponent<RectTransform>();
            rt.anchorMin = rt.anchorMax = Vector2.one * 0.5f;
            rt.sizeDelta = new Vector2(diameter, diameter);
            var img = go.AddComponent<Image>();
            img.type = Image.Type.Filled;
            img.fillMethod = Image.FillMethod.Radial360;
            img.fillOrigin = (int)Image.Origin360.Top;
            img.fillClockwise = true;
            img.fillAmount = 0f;
            img.color = color;
            return img;
        }

        private static Text MakeText(Transform parent, string name, string value, Vector2 anchor, Vector2 pos, int size, TextAnchor align, Color color)
        {
            var go = new GameObject(name);
            go.transform.SetParent(parent, false);
            var rt = go.AddComponent<RectTransform>();
            rt.anchorMin = anchor;
            rt.anchorMax = anchor;
            rt.pivot = anchor;
            rt.anchoredPosition = pos;
            rt.sizeDelta = new Vector2(400, 80);
            var t = go.AddComponent<Text>();
            t.text = value;
            t.fontSize = size;
            t.alignment = align;
            t.color = color;
            t.font = Resources.GetBuiltinResource<Font>("Arial.ttf");
            t.horizontalOverflow = HorizontalWrapMode.Overflow;
            t.verticalOverflow = VerticalWrapMode.Overflow;
            t.raycastTarget = false;
            return t;
        }

        private static Slider MakeSlider(Transform parent, string name, Vector2 anchor, Vector2 pos, Vector2 size)
        {
            var go = new GameObject(name);
            go.transform.SetParent(parent, false);
            var rt = go.AddComponent<RectTransform>();
            rt.anchorMin = anchor;
            rt.anchorMax = anchor;
            rt.pivot = new Vector2(1, 0);
            rt.anchoredPosition = pos;
            rt.sizeDelta = size;
            go.AddComponent<Image>().color = new Color(0.1f, 0.12f, 0.16f, 0.85f);
            var slider = go.AddComponent<Slider>();
            slider.minValue = 0f;
            slider.maxValue = 1f;
            var fillGo = new GameObject("Fill");
            fillGo.transform.SetParent(go.transform, false);
            var frt = fillGo.AddComponent<RectTransform>();
            frt.anchorMin = Vector2.zero;
            frt.anchorMax = Vector2.one;
            frt.offsetMin = new Vector2(2, 2);
            frt.offsetMax = new Vector2(-2, -2);
            var fill = fillGo.AddComponent<Image>();
            fill.color = new Color(0.45f, 0.35f, 1f);
            slider.fillRect = frt;
            slider.targetGraphic = fill;
            return slider;
        }

        private static void MakeHold(Transform parent, string name, string label, Vector2 anchor, Vector2 pos, Vector2 size, System.Action down, System.Action up)
        {
            var go = new GameObject(name);
            go.transform.SetParent(parent, false);
            var rt = go.AddComponent<RectTransform>();
            rt.anchorMin = anchor;
            rt.anchorMax = anchor;
            rt.pivot = new Vector2(0.5f, 0.5f);
            rt.anchoredPosition = pos;
            rt.sizeDelta = size;
            var img = go.AddComponent<Image>();
            img.color = new Color(0.08f, 0.1f, 0.14f, 0.8f);
            go.AddComponent<Button>().targetGraphic = img;
            var t = MakeText(go.transform, "L", label, new Vector2(0.5f, 0.5f), Vector2.zero, 34, TextAnchor.MiddleCenter, Color.white);
            t.rectTransform.anchorMin = Vector2.zero;
            t.rectTransform.anchorMax = Vector2.one;
            t.rectTransform.offsetMin = Vector2.zero;
            t.rectTransform.offsetMax = Vector2.zero;
            var hold = go.AddComponent<HoldButton>();
            hold.onDown = down;
            hold.onUp = up;
        }
    }

    public class HoldButton : MonoBehaviour, UnityEngine.EventSystems.IPointerDownHandler, UnityEngine.EventSystems.IPointerUpHandler, UnityEngine.EventSystems.IPointerExitHandler
    {
        public System.Action onDown;
        public System.Action onUp;
        public void OnPointerDown(UnityEngine.EventSystems.PointerEventData e) => onDown?.Invoke();
        public void OnPointerUp(UnityEngine.EventSystems.PointerEventData e) => onUp?.Invoke();
        public void OnPointerExit(UnityEngine.EventSystems.PointerEventData e) => onUp?.Invoke();
    }
}

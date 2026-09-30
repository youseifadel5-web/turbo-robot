using UnityEngine;
using UnityEngine.UI;
using NeonRush.Cars;
using NeonRush.Save;
using NeonRush.Environment;
using NeonRush.Audio;

namespace NeonRush.UI
{
    /// <summary>
    /// Garage UI (reference look): colored CLASS badges, coins pill, stat bars,
    /// car carousel strip, RACE (cyan) / CUSTOMIZE (magenta) buttons and a live
    /// customize panel (paint / rims / neon) applied through CustomizeApplier.
    /// </summary>
    public class GarageScreen : MonoBehaviour
    {
        public System.Action onRaceRequested;

        private GameObject root;
        private Text nameText, priceText, coinsText, classBadgeText, paintText, rimText, neonText;
        private Image classBadgeImg;
        private Slider barTop, barAcc, barHan, barBra, barDri, barNit;
        private GameObject customizePanel;
        private GameObject[] carouselCells;
        private int index;
        private GameObject currentCar;
        private GarageStudioBuilder studio;
        private System.Action onClosed;
        private Vector3 lastSlot = new Vector3(300f, 0.35f, 0f);
        private bool customizeOpen;

        public static Color ClassBadgeColor(CarClass c)
        {
            switch (c)
            {
                case CarClass.S: return CarCatalog.HexToColor("#B44CFF");
                case CarClass.A: return CarCatalog.HexToColor("#FF6A00");
                default: return CarCatalog.HexToColor("#7CFF4D");
            }
        }

        public void Build(Transform canvas, GarageStudioBuilder studioBuilder, System.Action closed)
        {
            if (root != null) return;
            studio = studioBuilder;
            onClosed = closed;

            root = new GameObject("GaragePanel");
            root.transform.SetParent(canvas, false);
            var rt = root.AddComponent<RectTransform>();
            rt.anchorMin = rt.anchorMax = Vector2.one;
            rt.offsetMin = rt.offsetMax = Vector2.zero;

            // ---- top-left: name + class badge + price ----
            nameText = UiTheme.Label(root.transform, "Name", "", new Vector2(0, 1), new Vector2(70, -90),
                new Vector2(560, 60), 40, UiTheme.Text, TextAnchor.UpperLeft);

            var badgeGo = new GameObject("ClassBadge", typeof(Image));
            badgeGo.transform.SetParent(root.transform, false);
            var brt = badgeGo.GetComponent<RectTransform>();
            brt.anchorMin = brt.anchorMax = new Vector2(0, 1);
            brt.pivot = new Vector2(0, 1);
            brt.sizeDelta = new Vector2(150, 34);
            brt.anchoredPosition = new Vector2(74, -152);
            classBadgeImg = badgeGo.GetComponent<Image>();
            classBadgeText = badgeGo.AddComponent<Text>();
            classBadgeText.font = UiTheme.Font;
            classBadgeText.fontSize = 19;
            classBadgeText.alignment = TextAnchor.MiddleCenter;
            classBadgeText.color = Color.black;

            priceText = UiTheme.Label(root.transform, "Price", "", new Vector2(0, 1), new Vector2(238, -150),
                new Vector2(380, 32), 20, UiTheme.Gold, TextAnchor.UpperLeft);

            // ---- top-right: coins pill ----
            var pill = UiTheme.PanelBox(root.transform, "CoinsPill", new Vector2(1, 1), new Vector2(-70, -90),
                new Vector2(300, 54), new Color(0.04f, 0.05f, 0.08f, 0.92f), UiTheme.Gold);
            coinsText = UiTheme.Label(pill.transform, "C", "0", new Vector2(0.5f, 0.5f), Vector2.zero,
                new Vector2(300, 54), 24, UiTheme.Gold, TextAnchor.MiddleCenter);
            coinsText.rectTransform.anchorMin = Vector2.zero; coinsText.rectTransform.anchorMax = Vector2.one;
            coinsText.rectTransform.offsetMin = coinsText.rectTransform.offsetMax = Vector2.zero;

            // ---- left: stats ----
            var stats = UiTheme.PanelBox(root.transform, "Stats", new Vector2(0, 0.5f), new Vector2(70, 0),
                new Vector2(360, 430), UiTheme.Panel, UiTheme.Cyan);
            barTop = MakeBar(stats.transform, "TOP SPEED", -40);
            barAcc = MakeBar(stats.transform, "ACCELERATION", -105);
            barHan = MakeBar(stats.transform, "HANDLING", -170);
            barBra = MakeBar(stats.transform, "BRAKING", -235);
            barDri = MakeBar(stats.transform, "DRIFT", -300);
            barNit = MakeBar(stats.transform, "NITRO", -365);

            // ---- bottom: RACE + CUSTOMIZE ----
            MakeBtn(root.transform, "Race", Localization.L10n.Get("garage.race"), new Vector2(-170, 90), new Vector2(0, 0), 300, OnRace,
                new Color(0f, 0.94f, 1f, 0.18f), UiTheme.Cyan);
            MakeBtn(root.transform, "Customize", Localization.L10n.Get("garage.customize"), new Vector2(170, 90), new Vector2(0, 0), 300, ToggleCustomize,
                new Color(1f, 0.17f, 0.84f, 0.16f), UiTheme.Magenta);

            // ---- carousel strip ----
            BuildCarousel(root.transform);

            // ---- customize panel (paint / rims / neon) ----
            BuildCustomizePanel(root.transform);

            // back
            MakeBtn(root.transform, "Back", Localization.L10n.Get("garage.back"), new Vector2(-360, -60), new Vector2(0.5f, 1), 200, () => Hide(),
                UiTheme.Panel, new Color(0.18f, 0.22f, 0.3f));

            root.SetActive(false);
        }

        private void BuildCarousel(Transform parent)
        {
            var strip = new GameObject("Carousel");
            strip.transform.SetParent(parent, false);
            var srt = strip.AddComponent<RectTransform>();
            srt.anchorMin = srt.anchorMax = new Vector2(0.5f, 0);
            srt.pivot = new Vector2(0.5f, 0.5f);
            srt.sizeDelta = new Vector2(1460, 120);
            srt.anchoredPosition = new Vector2(0, 220);

            carouselCells = new GameObject[CarCatalog.All.Count];
            for (int i = 0; i < CarCatalog.All.Count; i++)
            {
                var def = CarCatalog.All[i];
                var cell = UiTheme.PanelBox(strip.transform, "Cell_" + def.carId, new Vector2(0, 0.5f),
                    new Vector2(-660 + i * 296, 0), new Vector2(280, 116), UiTheme.Panel, UiTheme.Cyan);
                var t = UiTheme.Label(cell.transform, "N", def.displayName.ToUpper(), new Vector2(0.5f, 1),
                    new Vector2(0, -10), new Vector2(270, 34), 19, UiTheme.Text, TextAnchor.UpperCenter);
                t.rectTransform.anchoredPosition = new Vector2(0, -14);

                // mini class badge
                var mini = new GameObject("B", typeof(Image));
                mini.transform.SetParent(cell.transform, false);
                var mrt = mini.GetComponent<RectTransform>();
                mrt.anchorMin = mrt.anchorMax = new Vector2(0.5f, 0);
                mrt.pivot = new Vector2(0.5f, 0.5f);
                mrt.sizeDelta = new Vector2(110, 26);
                mrt.anchoredPosition = new Vector2(0, 28);
                var mimg = mini.GetComponent<Image>();
                mimg.color = ClassBadgeColor(def.carClass);
                var mt = mini.AddComponent<Text>();
                mt.font = UiTheme.Font; mt.fontSize = 16; mt.alignment = TextAnchor.MiddleCenter;
                mt.color = Color.black; mt.text = "CLASS " + def.carClass;

                var btn = cell.gameObject.AddComponent<Button>();
                btn.targetGraphic = cell;
                int captured = i;
                btn.onClick.AddListener(() => SelectIndex(captured));
                carouselCells[i] = cell.gameObject;
            }
        }

        private void BuildCustomizePanel(Transform parent)
        {
            customizePanel = new GameObject("CustomizePanel");
            customizePanel.transform.SetParent(parent, false);
            var prt = customizePanel.AddComponent<RectTransform>();
            prt.anchorMin = prt.anchorMax = new Vector2(1, 0.5f);
            prt.pivot = new Vector2(1, 0.5f);
            prt.sizeDelta = new Vector2(430, 330);
            prt.anchoredPosition = new Vector2(-70, 120);
            customizePanel.AddComponent<Image>().color = new Color(0.02f, 0.03f, 0.05f, 0.94f);

            UiTheme.Label(customizePanel.transform, "T", Localization.L10n.Get("garage.customize"), new Vector2(0.5f, 1),
                new Vector2(0, -16), new Vector2(400, 36), 24, UiTheme.Magenta, TextAnchor.UpperCenter);

            paintText = MakeCycleRow(customizePanel.transform, "PAINT", -70, d => CyclePaint(d));
            rimText = MakeCycleRow(customizePanel.transform, "RIMS", -150, d => CycleRim(d));
            neonText = MakeCycleRow(customizePanel.transform, "NEON", -230, d => CycleNeon(d));

            customizePanel.SetActive(false);
        }

        private Text MakeCycleRow(Transform parent, string label, float y, System.Action<int> onCycle)
        {
            UiTheme.Label(parent, label + "_L", label, new Vector2(0, 1), new Vector2(28, y),
                new Vector2(120, 40), 17, UiTheme.Muted, TextAnchor.MiddleLeft);

            var txt = UiTheme.Label(parent, label + "_V", "", new Vector2(0.5f, 1), new Vector2(30, y),
                new Vector2(240, 40), 19, UiTheme.Text, TextAnchor.MiddleCenter);

            var left = UiTheme.PanelBox(parent, label + "_Prev", new Vector2(1, 1), new Vector2(-150, y + 4),
                new Vector2(44, 32), UiTheme.Panel, UiTheme.Cyan);
            var lt = UiTheme.Label(left.transform, "L", "<", new Vector2(0.5f, 0.5f), Vector2.zero,
                new Vector2(44, 32), 20, UiTheme.Text, TextAnchor.MiddleCenter);
            Stretch(lt);
            left.gameObject.AddComponent<Button>().onClick.AddListener(() => { SfxPlayer.PlayUi("SFX/sfx_ui_click", 0.7f); onCycle(-1); });

            var right = UiTheme.PanelBox(parent, label + "_Next", new Vector2(1, 1), new Vector2(-40, y + 4),
                new Vector2(44, 32), UiTheme.Panel, UiTheme.Cyan);
            var rt2 = UiTheme.Label(right.transform, "R", ">", new Vector2(0.5f, 0.5f), Vector2.zero,
                new Vector2(44, 32), 20, UiTheme.Text, TextAnchor.MiddleCenter);
            Stretch(rt2);
            right.gameObject.AddComponent<Button>().onClick.AddListener(() => { SfxPlayer.PlayUi("SFX/sfx_ui_click", 0.7f); onCycle(1); });

            return txt;
        }

        private static void Stretch(Text t)
        {
            t.rectTransform.anchorMin = Vector2.zero; t.rectTransform.anchorMax = Vector2.one;
            t.rectTransform.offsetMin = t.rectTransform.offsetMax = Vector2.zero;
        }

        public void Show()
        {
            var data = SaveSystem.Load();
            index = Mathf.Max(0, CarCatalog.All.FindIndex(c => c.carId == data.selectedCarId));
            root.SetActive(true);
            RefreshCar();
        }

        public void Hide()
        {
            root.SetActive(false);
            onClosed?.Invoke();
        }

        private void SelectIndex(int i)
        {
            SfxPlayer.PlayUi("SFX/sfx_ui_click", 0.8f);
            index = i;
            RefreshCar();
        }

        private void ToggleCustomize()
        {
            SfxPlayer.PlayUi("SFX/sfx_ui_click", 0.8f);
            customizeOpen = !customizeOpen;
            customizePanel.SetActive(customizeOpen);
            if (customizeOpen) RefreshCustomizeTexts();
        }

        // ---- customization cycles ----
        private int paintIdx, rimIdx, neonIdx;

        private static int Cycle(int i, int dir, int len) => (i + dir + len) % len;

        private void CyclePaint(int dir)
        {
            paintIdx = Cycle(paintIdx, dir, CarCatalog.Paints.Length);
            ApplyCustom(CarCatalog.Paints[paintIdx].id, null, null);
        }

        private void CycleRim(int dir)
        {
            rimIdx = Cycle(rimIdx, dir, CarCatalog.Rims.Length);
            ApplyCustom(null, CarCatalog.Rims[rimIdx].id, null);
        }

        private void CycleNeon(int dir)
        {
            neonIdx = Cycle(neonIdx, dir, CarCatalog.NeonColors.Length);
            ApplyCustom(null, null, CarCatalog.NeonColors[neonIdx].id);
        }

        private void ApplyCustom(string paintId, string rimId, string neonId)
        {
            var def = CarCatalog.All[index];
            var owned = CarCatalog.GetOwned(def.carId);
            if (paintId != null) owned.paintId = paintId;
            if (rimId != null) owned.rimId = rimId;
            if (neonId != null) owned.neonId = neonId;
            SaveSystem.Save();

            if (currentCar != null)
                CustomizeApplier.Apply(currentCar, owned.paintId, owned.rimId, owned.neonId);
            RefreshCustomizeTexts();
        }

        private void RefreshCustomizeTexts()
        {
            var owned = CarCatalog.GetOwned(CarCatalog.All[index].carId);
            paintIdx = IndexOf(CarCatalog.Paints, p => p.id == owned.paintId);
            rimIdx = IndexOf(CarCatalog.Rims, r => r.id == owned.rimId);
            neonIdx = IndexOf(CarCatalog.NeonColors, n => n.id == owned.neonId);
            if (paintText != null) paintText.text = CarCatalog.Paints[paintIdx].name.ToUpper();
            if (rimText != null) rimText.text = CarCatalog.Rims[rimIdx].name.ToUpper();
            if (neonText != null) neonText.text = CarCatalog.NeonColors[neonIdx].name.ToUpper();
        }

        private static int IndexOf<T>(T[] arr, System.Predicate<T> match)
        {
            int i = System.Array.FindIndex(arr, match);
            return i < 0 ? 0 : i;
        }

        public void OnRace()
        {
            var def = CarCatalog.All[index];
            if (!CarCatalog.IsOwned(def.carId))
            {
                OnAction();
                return;
            }
            var data = SaveSystem.Current;
            data.selectedCarId = def.carId;
            SaveSystem.Save();
            SfxPlayer.PlayUi("SFX/sfx_checkpoint", 0.8f);
            onRaceRequested?.Invoke();
        }

        private void OnAction()
        {
            var def = CarCatalog.All[index];
            var data = SaveSystem.Current;

            if (!CarCatalog.IsOwned(def.carId))
            {
                if (CarCatalog.TryBuy(def.carId))
                {
                    SfxPlayer.PlayUi("SFX/sfx_coin", 0.9f);
                    RefreshCar();
                }
                else
                {
                    SfxPlayer.PlayUi("SFX/sfx_ui_back", 0.7f);
                    if (priceText != null)
                        priceText.text = "NOT ENOUGH ●  (" + data.coins.ToString("N0") + ")";
                }
                return;
            }

            data.selectedCarId = def.carId;
            SaveSystem.Save();
            SfxPlayer.PlayUi("SFX/sfx_checkpoint", 0.8f);
            RefreshCar();
        }

        private void RefreshCar()
        {
            var def = CarCatalog.All[index];
            bool owned = CarCatalog.IsOwned(def.carId);

            if (nameText != null) nameText.text = def.displayName.ToUpper();
            if (classBadgeText != null)
            {
                classBadgeText.text = "CLASS " + def.carClass;
                classBadgeImg.color = ClassBadgeColor(def.carClass);
            }
            if (priceText != null)
                priceText.text = owned ? Localization.L10n.Get("garage.owned") : def.priceCoins.ToString("N0") + " ●";
            if (coinsText != null) coinsText.text = "● " + SaveSystem.Current.coins.ToString("N0");

            var stats = CarCatalog.CreateStats(def.carId);
            SetBar(barTop, stats.topSpeedRating);
            SetBar(barAcc, stats.accelerationRating);
            SetBar(barHan, stats.handlingRating);
            SetBar(barBra, stats.brakingRating);
            SetBar(barDri, stats.driftRating);
            SetBar(barNit, stats.nitroRating);

            // carousel highlight
            if (carouselCells != null)
                for (int i = 0; i < carouselCells.Length; i++)
                {
                    var img = carouselCells[i] != null ? carouselCells[i].GetComponent<Image>() : null;
                    if (img != null) img.color = i == index
                        ? new Color(1f, 0.17f, 0.84f, 0.2f) : UiTheme.Panel;
                }

            // swap 3D model on the studio platform
            if (studio != null)
            {
                Vector3 slot = studio.carPivot != null && studio.carPivot != currentCar.transform
                    ? studio.carPivot.position
                    : lastSlot;
                lastSlot = slot;
                if (currentCar != null) Destroy(currentCar);
                currentCar = CarModelLoader.LoadCar(def.carId, false);
                currentCar.transform.position = slot + new Vector3(0f, 0.1f, 0f);
                currentCar.transform.rotation = Quaternion.Euler(0f, 180f, 0f);
                var rb = currentCar.GetComponent<Rigidbody>();
                if (rb != null) rb.isKinematic = true;
                studio.carPivot = currentCar.transform;
            }

            RefreshCustomizeTexts();
        }

        private static void SetBar(Slider s, float v) { if (s != null) s.value = Mathf.Clamp01(v / 100f); }

        private Slider MakeBar(Transform parent, string label, float y)
        {
            UiTheme.Label(parent, label, label, new Vector2(0, 1), new Vector2(24, y - 6),
                new Vector2(310, 28), 15, UiTheme.Muted, TextAnchor.MiddleLeft);
            var track = new GameObject(label + "_Bar", typeof(Image));
            track.transform.SetParent(parent, false);
            var trt = track.GetComponent<RectTransform>();
            trt.anchorMin = trt.anchorMax = new Vector2(0.5f, 1);
            trt.pivot = new Vector2(0.5f, 1);
            trt.sizeDelta = new Vector2(310, 12);
            trt.anchoredPosition = new Vector2(0, y - 44);
            track.GetComponent<Image>().color = new Color(0.09f, 0.11f, 0.16f);
            var fill = new GameObject("Fill", typeof(Image));
            fill.transform.SetParent(track.transform, false);
            var frt = fill.GetComponent<RectTransform>();
            frt.anchorMin = new Vector2(0, 0); frt.anchorMax = new Vector2(0, 1);
            frt.sizeDelta = new Vector2(10, 0);
            var slider = track.AddComponent<Slider>();
            slider.fillRect = frt;
            slider.interactable = false;
            fill.GetComponent<Image>().color = UiTheme.Cyan;
            return slider;
        }

        private Image MakeBtn(Transform parent, string name, string label, Vector2 pos, Vector2 anchor, float width,
            UnityEngine.Events.UnityAction click, Color bg, Color border)
        {
            var img = UiTheme.PanelBox(parent, name, anchor, pos, new Vector2(width, 64), bg, border);
            var t = UiTheme.Label(img.transform, "L", label, new Vector2(0.5f, 0.5f), Vector2.zero,
                new Vector2(width, 64), 24, UiTheme.Text, TextAnchor.MiddleCenter);
            Stretch(t);
            var btn = img.gameObject.AddComponent<Button>();
            btn.targetGraphic = img;
            btn.onClick.AddListener(() => { SfxPlayer.PlayUi("SFX/sfx_ui_click", 0.8f); click(); });
            return img;
        }
    }
}

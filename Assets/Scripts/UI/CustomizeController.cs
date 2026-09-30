using UnityEngine;
using UnityEngine.UI;
using NeonRush.Cars;

namespace NeonRush.UI
{
    public enum CustomizeCategory
    {
        Engine, Brake, Agility, Turbo, Nos, Rims, Paint, Body, Spoiler, Neon, Lights, Wrap
    }

    /// <summary>
    /// Customize screen: categories, live stats, locked parts, cost.
    /// Matches the purple/neon customize panels from reference games.
    /// </summary>
    public class CustomizeController : MonoBehaviour
    {
        [Header("References")]
        public GarageController garage;
        public Text categoryTitle;
        public Text partNameText;
        public Text costText;
        public Button buyButton;
        public GameObject lockIcon;

        [Header("Stat Bars (live)")]
        public Slider topSpeedBar;
        public Slider accelerationBar;
        public Slider handlingBar;
        public Slider brakingBar;

        [Header("Category Buttons")]
        public CustomizeCategory currentCategory = CustomizeCategory.Paint;

        private CarStats workingStats;
        private int selectedPartIndex;
        private int currentCost;

        public void Open(CarStats source)
        {
            if (source == null) return;
            workingStats = ScriptableObject.Instantiate(source);
            selectedPartIndex = 0;
            RefreshUI();
        }

        public void SetCategory(int categoryIndex)
        {
            currentCategory = (CustomizeCategory)Mathf.Clamp(categoryIndex, 0, 11);
            selectedPartIndex = 0;
            RefreshUI();
        }

        public void SelectPart(int index)
        {
            selectedPartIndex = index;
            // In full implementation: load part data from ScriptableObject catalog
            currentCost = 500 + index * 750;
            RefreshUI();
        }

        public void TryBuy()
        {
            // Hook to economy / save system
            Debug.Log($"[Customize] Buy {currentCategory} part {selectedPartIndex} for {currentCost}");
            // After purchase: apply visual + update workingStats ratings and physics values
            RefreshUI();
        }

        private void RefreshUI()
        {
            if (categoryTitle) categoryTitle.text = currentCategory.ToString().ToUpper();
            if (partNameText) partNameText.text = currentCategory + " #" + (selectedPartIndex + 1);
            if (costText) costText.text = currentCost.ToString("N0");
            if (lockIcon) lockIcon.SetActive(currentCost > 0); // placeholder logic

            if (workingStats != null)
            {
                SetBar(topSpeedBar, workingStats.topSpeedRating / 100f);
                SetBar(accelerationBar, workingStats.accelerationRating / 100f);
                SetBar(handlingBar, workingStats.handlingRating / 100f);
                SetBar(brakingBar, workingStats.brakingRating / 100f);
            }
        }

        private void SetBar(Slider s, float v)
        {
            if (s) s.value = v;
        }

        public CarStats GetWorkingStats() => workingStats;
    }
}

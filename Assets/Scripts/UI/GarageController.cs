using UnityEngine;
using UnityEngine.UI;
using NeonRush.Cars;

namespace NeonRush.UI
{
    /// <summary>
    /// Garage screen: 3D car showcase, carousel, live stats bars, currency.
    /// Style matches premium mobile racing garages (dark + neon).
    /// </summary>
    public class GarageController : MonoBehaviour
    {
        [Header("Car Display")]
        public Transform carPivot;           // rotate this for 360
        public Transform cameraPivot;
        public float autoRotateSpeed = 12f;
        public bool autoRotate = true;

        [Header("Stats UI")]
        public Slider topSpeedBar;
        public Slider accelerationBar;
        public Slider handlingBar;
        public Slider brakingBar;
        public Slider driftBar;
        public Slider nitroBar;
        public Text carNameText;
        public Text classText;

        [Header("Currency")]
        public Text coinsText;
        public Text premiumText;

        [Header("Data")]
        public CarStats[] availableCars;
        public int selectedIndex;

        private float rotateInput;
        private CarStats current;

        private void Start()
        {
            if (availableCars != null && availableCars.Length > 0)
                SelectCar(0);
        }

        private void Update()
        {
            if (carPivot == null) return;

            if (Mathf.Abs(rotateInput) > 0.01f)
            {
                carPivot.Rotate(0f, -rotateInput * 90f * Time.deltaTime, 0f, Space.World);
                autoRotate = false;
            }
            else if (autoRotate)
            {
                carPivot.Rotate(0f, autoRotateSpeed * Time.deltaTime, 0f, Space.World);
            }
        }

        public void SetRotateInput(float value) => rotateInput = value;

        public void SelectCar(int index)
        {
            if (availableCars == null || availableCars.Length == 0) return;
            selectedIndex = Mathf.Clamp(index, 0, availableCars.Length - 1);
            current = availableCars[selectedIndex];
            RefreshStats();
        }

        public void NextCar() => SelectCar(selectedIndex + 1);
        public void PrevCar() => SelectCar(selectedIndex - 1);

        public void RefreshStats()
        {
            if (current == null) return;

            if (carNameText) carNameText.text = current.displayName;
            if (classText) classText.text = "CLASS " + current.carClass;

            AnimateBar(topSpeedBar, current.topSpeedRating / 100f);
            AnimateBar(accelerationBar, current.accelerationRating / 100f);
            AnimateBar(handlingBar, current.handlingRating / 100f);
            AnimateBar(brakingBar, current.brakingRating / 100f);
            AnimateBar(driftBar, current.driftRating / 100f);
            AnimateBar(nitroBar, current.nitroRating / 100f);
        }

        private void AnimateBar(Slider bar, float target)
        {
            if (bar == null) return;
            // Simple set; can be replaced with DOTween or coroutine lerp
            bar.value = target;
        }

        public void SetCurrency(int coins, int premium)
        {
            if (coinsText) coinsText.text = coins.ToString("N0");
            if (premiumText) premiumText.text = premium.ToString("N0");
        }

        public CarStats GetSelectedStats() => current;
    }
}

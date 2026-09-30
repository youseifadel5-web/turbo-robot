using UnityEngine;

namespace NeonRush.UI
{
    public class RacingHUDController : MonoBehaviour
    {
        public void SetPosition(int position, int total) { }
        public void SetLap(int lap, int total) { }
        public void SetSpeed(float kmh) { }
        public void SetGear(int gear) { }
        public void SetNitro(float normalized) { }
        public void ShowWrongWay(bool show) { }
    }
}

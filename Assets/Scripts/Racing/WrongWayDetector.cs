using UnityEngine;
using NeonRush.Cars;
using NeonRush.UI;

namespace NeonRush.Racing
{
    /// <summary>
    /// Detects the player driving against track direction and warns via
    /// HUD + sound. Ignores low speed and spin-outs.
    /// </summary>
    public class WrongWayDetector : MonoBehaviour
    {
        public RacingLineBuilder line;
        public ArcadeCarController car;
        public RuntimeRaceHUD hud;
        public float minSpeedKmh = 20f;
        public float triggerDot = -0.35f;
        public float holdSeconds = 0.9f;

        public bool IsWrongWay { get; private set; }

        private float holdTimer;
        private float beepCooldown;

        private void Update()
        {
            if (line == null || !line.IsValid || car == null) return;

            if (car.SpeedKmh < minSpeedKmh)
            {
                holdTimer = 0f;
                SetState(false);
                return;
            }

            Vector3 tangent = line.GetTangent(car.transform.position);
            float dot = Vector3.Dot(car.transform.forward, tangent);
            if (dot < triggerDot) holdTimer += Time.deltaTime;
            else holdTimer = 0f;

            SetState(holdTimer >= holdSeconds);

            if (IsWrongWay)
            {
                beepCooldown -= Time.deltaTime;
                if (beepCooldown <= 0f)
                {
                    Audio.SfxPlayer.Play("SFX/sfx_wrongway", 0.7f);
                    beepCooldown = 1.6f;
                }
            }
        }

        private void SetState(bool wrong)
        {
            if (wrong == IsWrongWay) return;
            IsWrongWay = wrong;
            if (hud != null) hud.ShowWrongWay(wrong);
        }
    }
}

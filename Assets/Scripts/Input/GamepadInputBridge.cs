using UnityEngine;
using UnityEngine.InputSystem;
using NeonRush.Cars;
using NeonRush.UI;

namespace NeonRush.Input
{
    /// <summary>
    /// Unity Input System gamepad / Bluetooth controller mapping.
    /// Does not touch Bluetooth audio routing.
    /// </summary>
    public class GamepadInputBridge : MonoBehaviour
    {
        public MobileCarInput carInput;
        public CanvasGroup toast;
        public UnityEngine.UI.Text toastText;
        public float toastSeconds = 2.2f;

        public bool Connected { get; private set; }

        private float toastTimer;
        private bool lastConnected;

        private void Update()
        {
            var pad = Gamepad.current;
            bool now = pad != null;
            if (now != lastConnected)
            {
                lastConnected = now;
                Connected = now;
                ShowToast(now ? "CONTROLLER CONNECTED" : "CONTROLLER DISCONNECTED");
            }

            if (toastTimer > 0f)
            {
                toastTimer -= Time.deltaTime;
                if (toast != null && toastTimer <= 0f) toast.alpha = 0f;
            }

            if (pad == null || carInput == null) return;

            Vector2 stick = pad.leftStick.ReadValue();
            float steer = stick.x;
            if (Mathf.Abs(steer) < 0.12f) steer = 0f;

            float throttle = Mathf.Max(pad.rightTrigger.ReadValue(), pad.aButton.isPressed ? 1f : 0f);
            float brake = Mathf.Max(pad.leftTrigger.ReadValue(), pad.bButton.isPressed ? 1f : 0f);
            bool drift = pad.xButton.isPressed || pad.leftShoulder.isPressed;
            bool nitro = pad.yButton.isPressed || pad.rightShoulder.isPressed;

            carInput.SetSteering(steer);
            carInput.SetThrottle(throttle);
            carInput.SetBrake(brake);
            carInput.SetDrift(drift);
            carInput.SetNitro(nitro);
        }

        private void ShowToast(string msg)
        {
            toastTimer = toastSeconds;
            if (toastText != null) toastText.text = msg;
            if (toast != null) toast.alpha = 1f;
            Debug.Log("[Neon Rush] " + msg);
        }
    }
}

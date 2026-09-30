using UnityEngine;
using UnityEngine.InputSystem;

namespace NeonRush.Input
{
    /// <summary>
    /// Lightweight connected-state helper. Prefer GamepadInputBridge for full mapping + toast.
    /// </summary>
    public class ControllerStatus : MonoBehaviour
    {
        public bool Connected => Gamepad.current != null;

        public string DeviceName => Gamepad.current != null ? Gamepad.current.displayName : "None";

        public System.Action<bool> OnChanged;
        private bool last;

        private void Update()
        {
            bool now = Gamepad.current != null;
            if (now == last) return;
            last = now;
            OnChanged?.Invoke(now);
        }
    }
}

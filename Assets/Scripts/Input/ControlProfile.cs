using UnityEngine;

namespace NeonRush.Input
{
    public enum ControlMode { SteeringWheel, Buttons, Tilt }

    public class ControlProfile : MonoBehaviour
    {
        public ControlMode mode = ControlMode.SteeringWheel;
        [Range(0.1f, 2f)] public float sensitivity = 1f;
        public bool steeringAssist = true;
        public bool brakeAssist = false;
    }
}

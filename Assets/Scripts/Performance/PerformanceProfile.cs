using UnityEngine;

namespace NeonRush.Performance
{
    public enum QualityPreset { Low, Medium, High, Ultra }

    public class PerformanceProfile : MonoBehaviour
    {
        public QualityPreset preset = QualityPreset.Medium;
        public int targetFps = 60;

        public void Apply(QualityPreset value)
        {
            preset = value;
            QualitySettings.SetQualityLevel((int)value, true);
            Application.targetFrameRate = targetFps;
        }
    }
}

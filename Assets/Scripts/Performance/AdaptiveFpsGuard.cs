using UnityEngine;

namespace NeonRush.Performance
{
    /// <summary>
    /// Watches frame rate in 4-second windows; if the device can't hold the
    /// target FPS it steps the URP look tier down automatically (min Low).
    /// Designed for mid-range Android survival.
    /// </summary>
    public class AdaptiveFpsGuard : MonoBehaviour
    {
        public float windowSeconds = 4f;
        public float dropRatio = 0.72f; // avg below 72% of target → downgrade
        public int minTier = 0;

        private float timer;
        private int frames;
        private UrpLookApplier look;
        private bool enabledGuard = true;

        private void Start()
        {
            look = FindObjectOfType<UrpLookApplier>();
            if (look == null) enabledGuard = false;
        }

        private void Update()
        {
            if (!enabledGuard || look == null) return;
            timer += Time.unscaledDeltaTime;
            frames++;

            if (timer < windowSeconds) return;

            float avg = frames / timer;
            int target = Application.targetFrameRate <= 0 ? 60 : Application.targetFrameRate;
            if (avg < target * dropRatio)
            {
                int tier = (int)look.tier;
                if (tier > minTier)
                {
                    look.Apply((GraphicsTier)(tier - 1));
                    Debug.Log($"[AdaptiveFps] avg {avg:F0} < {target * dropRatio:F0}, tier -> {tier - 1}");
                }
            }

            timer = 0f;
            frames = 0;
        }
    }
}

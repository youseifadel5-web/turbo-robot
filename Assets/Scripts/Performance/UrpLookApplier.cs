using UnityEngine;
using UnityEngine.Rendering;

namespace NeonRush.Performance
{
    public enum GraphicsTier { Low, Medium, High, Ultra }

    /// <summary>
    /// Applies quality + URP-friendly look knobs at runtime.
    /// Pair with a Global Volume in the scene (Bloom / Color / Vignette).
    /// </summary>
    public class UrpLookApplier : MonoBehaviour
    {
        public GraphicsTier tier = GraphicsTier.High;
        public Light sun;
        public Volume globalVolume;

        public void Apply(GraphicsTier t)
        {
            tier = t;
            Application.targetFrameRate = t == GraphicsTier.Low ? 30 : 60;
            QualitySettings.shadows = t == GraphicsTier.Low ? ShadowQuality.Disable : ShadowQuality.All;
            QualitySettings.shadowResolution =
                t == GraphicsTier.Ultra ? ShadowResolution.VeryHigh :
                t == GraphicsTier.High ? ShadowResolution.High : ShadowResolution.Medium;
            QualitySettings.lodBias = t == GraphicsTier.Low ? 0.6f : t == GraphicsTier.Medium ? 0.85f : 1.1f;
            QualitySettings.particleRaycastBudget = t == GraphicsTier.Low ? 32 : 256;
            QualitySettings.skinWeights = t == GraphicsTier.Low ? SkinQuality.Bone2 : SkinQuality.Bone4;

            if (sun != null)
            {
                sun.shadows = t == GraphicsTier.Low ? LightShadows.None : LightShadows.Soft;
                sun.shadowStrength = t == GraphicsTier.Ultra ? 0.85f : 0.65f;
            }

            Debug.Log("[Neon Rush] Graphics tier " + t);
        }

        private void Start() => Apply(tier);
    }
}

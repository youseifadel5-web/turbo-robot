using UnityEngine;

namespace NeonRush.UI
{
    /// <summary>
    /// Keeps a full-screen overlay inside the device safe area (notches,
    /// punch-holes, rounded corners). Attach to the canvas root panel.
    /// </summary>
    [RequireComponent(typeof(RectTransform))]
    public class SafeAreaFitter : MonoBehaviour
    {
        private Rect lastSafe;
        private RectTransform rt;
        private Vector2 lastSize;

        private void OnEnable()
        {
            rt = GetComponent<RectTransform>();
            Apply();
        }

        private void OnRectTransformDimensionsChange()
        {
            if (rt != null) Apply();
        }

        private void Apply()
        {
            if (rt == null) return;
            Rect safe = Screen.safeArea;
            if (safe == lastSafe && rt.rect.size == lastSize) return;
            lastSafe = safe;
            lastSize = rt.rect.size;

            Vector2 anchorMin = safe.position;
            Vector2 anchorMax = safe.position + safe.size;
            anchorMin.x /= Screen.width; anchorMin.y /= Screen.height;
            anchorMax.x /= Screen.width; anchorMax.y /= Screen.height;

            rt.anchorMin = anchorMin;
            rt.anchorMax = anchorMax;
        }
    }
}

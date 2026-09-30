using UnityEngine;
using UnityEngine.UI;
using NeonRush.Cars;

namespace NeonRush.Racing
{
    /// <summary>
    /// Live top-down minimap rendered into a RenderTexture and bound to the
    /// HUD's RawImage. Updates at reduced rate for mobile perf.
    /// </summary>
    public class MinimapController : MonoBehaviour
    {
        public Transform target;
        public RawImage minimapImage;
        public float orthoSize = 90f;
        public int textureSize = 256;
        public float updateRate = 12f; // renders per second

        private Camera mapCam;
        private RenderTexture rt;
        private float timer;

        public void Setup(Transform followTarget, RawImage image)
        {
            target = followTarget;
            minimapImage = image;

            rt = new RenderTexture(textureSize, textureSize, 16, RenderTextureFormat.RGB565)
            {
                name = "MinimapRT"
            };
            rt.Create();

            var go = new GameObject("MinimapCamera");
            mapCam = go.AddComponent<Camera>();
            mapCam.orthographic = true;
            mapCam.orthographicSize = orthoSize;
            mapCam.transform.rotation = Quaternion.Euler(90f, 0f, 0f);
            mapCam.targetTexture = rt;
            mapCam.clearFlags = CameraClearFlags.SolidColor;
            mapCam.backgroundColor = new Color(0.015f, 0.02f, 0.04f, 1f);
            mapCam.cullingMask = ~(1 << 5); // skip UI layer
            mapCam.enabled = false; // manual render only
            mapCam.nearClipPlane = 1f;
            mapCam.farClipPlane = 400f;

            if (minimapImage != null)
            {
                minimapImage.texture = rt;
                minimapImage.enabled = true;
            }
        }

        private void LateUpdate()
        {
            if (mapCam == null || target == null) return;
            timer -= Time.unscaledDeltaTime;
            if (timer > 0f) return;
            timer = 1f / Mathf.Max(2f, updateRate);

            mapCam.transform.position = new Vector3(target.position.x, 150f, target.position.z);
            mapCam.Render();
        }

        private void OnDestroy()
        {
            if (rt != null)
            {
                rt.Release();
                Destroy(rt);
            }
            if (mapCam != null) Destroy(mapCam.gameObject);
        }
    }
}

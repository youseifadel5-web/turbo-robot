using UnityEngine;
using NeonRush.Cars;
using NeonRush.Save;

namespace NeonRush.Environment
{
    /// <summary>
    /// Builds the premium garage studio at runtime: reflective dark floor,
    /// rotating platform, neon rings, cinematic spotlights, backdrop walls
    /// + a 360° orbit camera with zoom. Works in-place or as its own scene.
    /// </summary>
    public class GarageStudioBuilder : MonoBehaviour
    {
        public Transform carPivot;
        public bool autoRotate = true;
        public float autoRotateSpeed = 14f;

        private Transform platform;
        private GarageOrbitCamera orbit;

        public GameObject Build(Transform car)
        {
            var rootGo = GameObject.Find("GarageStudio");
            if (rootGo != null) return rootGo; // already built

            rootGo = new GameObject("GarageStudio");
            var root = rootGo.transform;
            root.position = car != null ? car.position + Vector3.right * 60f : new Vector3(-400f, 0f, -400f);
            Vector3 center = root.position;

            // --- materials ---
            var lit = Shader.Find("Universal Render Pipeline/Lit");
            var floorMat = new Material(lit) { name = "Garage_Floor" };
            floorMat.color = new Color(0.015f, 0.018f, 0.026f);
            if (floorMat.HasProperty("_Smoothness")) floorMat.SetFloat("_Smoothness", 0.93f);
            if (floorMat.HasProperty("_Metallic")) floorMat.SetFloat("_Metallic", 0.6f);

            var wallMat = new Material(lit) { name = "Garage_Wall" };
            wallMat.color = new Color(0.02f, 0.024f, 0.04f);

            var emissive = new Material(lit) { name = "Garage_Neon" };
            emissive.color = Color.black;
            if (emissive.HasProperty("_EmissionColor"))
            {
                emissive.EnableKeyword("_EMISSION");
                emissive.SetColor("_EmissionColor", new Color(0f, 0.94f, 1f) * 2.4f);
            }

            var ringMat = new Material(emissive) { name = "Garage_Ring" };
            if (ringMat.HasProperty("_EmissionColor"))
                ringMat.SetColor("_EmissionColor", new Color(1f, 0.17f, 0.84f) * 2.2f);

            // --- floor ---
            var floor = GameObject.CreatePrimitive(PrimitiveType.Plane);
            floor.name = "StudioFloor";
            floor.transform.SetParent(root, false);
            floor.transform.position = center;
            floor.transform.localScale = new Vector3(8f, 1f, 8f);
            floor.GetComponent<Renderer>().sharedMaterial = floorMat;

            // --- backdrop walls (open box) ---
            foreach (var (pos, scale) in new[] {
                (center + new Vector3(0f, 6f, -14f), new Vector3(40f, 12f, 0.5f)),
                (center + new Vector3(-14f, 6f, 0f), new Vector3(0.5f, 12f, 28f)),
                (center + new Vector3(14f, 6f, 0f), new Vector3(0.5f, 12f, 28f))
            })
            {
                var wall = GameObject.CreatePrimitive(PrimitiveType.Cube);
                wall.name = "Wall";
                wall.transform.SetParent(root, false);
                wall.transform.position = pos;
                wall.transform.localScale = scale;
                wall.GetComponent<Renderer>().sharedMaterial = wallMat;
            }

            // --- rotating platform ---
            platform = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
            platform.name = "Platform";
            platform.transform.SetParent(root, false);
            platform.transform.position = center + new Vector3(0f, 0.06f, 0f);
            platform.transform.localScale = new Vector3(7f, 0.06f, 7f);
            platform.GetComponent<Renderer>().sharedMaterial = wallMat;
            Destroy(platform.GetComponent<Collider>());

            // --- neon floor strips ---
            for (int i = -3; i <= 3; i++)
            {
                var strip = GameObject.CreatePrimitive(PrimitiveType.Cube);
                strip.name = "NeonStrip";
                strip.transform.SetParent(root, false);
                strip.transform.position = center + new Vector3(i * 3f, 0.02f, 13f);
                strip.transform.localScale = new Vector3(0.12f, 0.02f, 8f);
                strip.GetComponent<Renderer>().sharedMaterial = emissive;
                Destroy(strip.GetComponent<Collider>());
            }

            // --- neon rings behind the car ---
            for (int i = 0; i < 4; i++)
            {
                var ring = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
                ring.name = "NeonRing" + i;
                ring.transform.SetParent(root, false);
                ring.transform.position = center + new Vector3(0f, 1.6f + i * 1.9f, -13.4f);
                ring.transform.localScale = new Vector3(3.2f + i * 0.8f, 0.03f, 3.2f + i * 0.8f);
                ring.GetComponent<Renderer>().sharedMaterial = i % 2 == 0 ? emissive : ringMat;
                Destroy(ring.GetComponent<Collider>());
            }

            // --- cinematic lights ---
            var keyGo = new GameObject("KeyLight");
            keyGo.transform.SetParent(root, false);
            keyGo.transform.position = center + new Vector3(4f, 8f, 5f);
            keyGo.transform.rotation = Quaternion.LookRotation(center - keyGo.transform.position);
            var key = keyGo.AddComponent<Light>();
            key.type = LightType.Spot;
            key.color = new Color(0.85f, 0.9f, 1f);
            key.intensity = 320f;
            key.spotAngle = 70f;
            key.range = 30f;
            key.shadows = LightShadows.Soft;

            var rim1Go = new GameObject("RimCyan");
            rim1Go.transform.SetParent(root, false);
            rim1Go.transform.position = center + new Vector3(-8f, 3.5f, -4f);
            rim1Go.transform.rotation = Quaternion.LookRotation(center - rim1Go.transform.position);
            var rim1 = rim1Go.AddComponent<Light>();
            rim1.type = LightType.Spot;
            rim1.color = new Color(0f, 0.94f, 1f);
            rim1.intensity = 200f;
            rim1.spotAngle = 55f;
            rim1.range = 26f;

            var rim2Go = new GameObject("RimMagenta");
            rim2Go.transform.SetParent(root, false);
            rim2Go.transform.position = center + new Vector3(8f, 3.2f, -3f);
            rim2Go.transform.rotation = Quaternion.LookRotation(center - rim2Go.transform.position);
            var rim2 = rim2Go.AddComponent<Light>();
            rim2.type = LightType.Spot;
            rim2.color = new Color(1f, 0.17f, 0.84f);
            rim2.intensity = 190f;
            rim2.spotAngle = 55f;
            rim2.range = 26f;

            // --- orbit camera rig ---
            var camGo = new GameObject("GarageCamera");
            camGo.transform.SetParent(root, false);
            orbit = camGo.AddComponent<GarageOrbitCamera>();
            orbit.target = center + new Vector3(0f, 0.8f, 0f);
            orbit.distance = 8.5f;
            orbit.minDistance = 5.5f;
            orbit.maxDistance = 14f;

            // position the car on the platform
            if (car != null)
            {
                car.position = center + new Vector3(0f, 0.35f, 0f);
                car.rotation = Quaternion.Euler(0f, 180f, 0f);
                carPivot = car;
            }

            return rootGo;
        }

        private void Update()
        {
            if (carPivot != null && autoRotate)
                carPivot.Rotate(0f, autoRotateSpeed * Time.deltaTime, 0f, Space.World);
        }

        public void Enter()
        {
            if (orbit != null) orbit.SetActive(true);
        }

        public void Exit()
        {
            if (orbit != null) orbit.SetActive(false);
        }
    }

    /// <summary>360° orbit + zoom camera for the garage. Drag = rotate, pinch/scroll = zoom.</summary>
    public class GarageOrbitCamera : MonoBehaviour
    {
        public Vector3 target;
        public float distance = 8.5f;
        public float minDistance = 5.5f;
        public float maxDistance = 14f;
        public float yawSpeed = 0.35f;
        public float pitchSpeed = 0.25f;

        private float yaw = 30f;
        private float pitch = 14f;
        private Camera cam;
        private bool active;

        public void SetActive(bool on)
        {
            active = on;
            if (cam == null) cam = gameObject.AddComponent<Camera>();
            cam.enabled = on;
            var listener = GetComponent<AudioListener>();
            if (on && listener == null) gameObject.AddComponent<AudioListener>();
        }

        private void Update()
        {
            if (!active || cam == null) return;

            if (Input.touchCount == 1 && Input.GetTouch(0).phase == TouchPhase.Moved)
            {
                yaw += Input.GetTouch(0).deltaPosition.x * yawSpeed * 0.35f;
                pitch -= Input.GetTouch(0).deltaPosition.y * pitchSpeed * 0.35f;
            }
            else if (Input.GetMouseButton(0))
            {
                yaw += Input.GetAxis("Mouse X") * yawSpeed;
                pitch -= Input.GetAxis("Mouse Y") * pitchSpeed;
            }

            if (Input.touchCount == 2)
            {
                var t0 = Input.GetTouch(0); var t1 = Input.GetTouch(1);
                float d = Vector2.Distance(t0.position, t1.position);
                float prev = Vector2.Distance(t0.position - t0.deltaPosition, t1.position - t1.deltaPosition);
                distance = Mathf.Clamp(distance - (d - prev) * 0.03f, minDistance, maxDistance);
            }
            distance = Mathf.Clamp(distance - Input.GetAxis("Mouse ScrollWheel") * 3f, minDistance, maxDistance);

            pitch = Mathf.Clamp(pitch, -5f, 55f);
            var rot = Quaternion.Euler(pitch, yaw, 0f);
            transform.position = target + rot * new Vector3(0f, 0f, -distance);
            transform.LookAt(target);
        }
    }
}

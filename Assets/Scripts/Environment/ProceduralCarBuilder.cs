using UnityEngine;
using NeonRush.Cars;

namespace NeonRush.Environment
{
    /// <summary>
    /// Builds a stylized original supercar from primitives + neon accents.
    /// Replace Body root with a licensed FBX later; keep same component layout.
    /// </summary>
    public class ProceduralCarBuilder : MonoBehaviour
    {
        public Color paintColor = new Color(0.05f, 0.06f, 0.08f);
        public Color neonColor = new Color(0f, 0.94f, 1f);
        public bool addPlayerComponents = true;

        public GameObject Build(Transform parent = null)
        {
            var root = new GameObject("ProceduralCar");
            if (parent != null) root.transform.SetParent(parent, false);
            root.tag = "Player";
            root.layer = 0;

            var rb = root.AddComponent<Rigidbody>();
            rb.mass = 1280f;
            rb.interpolation = RigidbodyInterpolation.Interpolate;
            rb.collisionDetectionMode = CollisionDetectionMode.Continuous;
            rb.centerOfMass = new Vector3(0f, -0.4f, 0f);

            // Main collider
            var box = root.AddComponent<BoxCollider>();
            box.center = new Vector3(0f, 0.55f, 0f);
            box.size = new Vector3(1.9f, 1.0f, 4.4f);

            var paint = MaterialFactory.CarPaint(paintColor);
            var neon = MaterialFactory.NeonEmissive(neonColor, 4f);
            var rubber = MaterialFactory.Rubber();
            var glass = MaterialFactory.Glass();
            var chrome = MaterialFactory.Chrome();

            // Body
            CreateBox(root.transform, "Body", new Vector3(0f, 0.55f, 0f), new Vector3(1.85f, 0.55f, 4.2f), paint);
            // Cabin
            CreateBox(root.transform, "Cabin", new Vector3(0f, 1.05f, -0.15f), new Vector3(1.55f, 0.45f, 1.8f), paint);
            // Glass
            CreateBox(root.transform, "Windshield", new Vector3(0f, 1.15f, 0.55f), new Vector3(1.4f, 0.08f, 0.9f), glass);
            // Hood neon strip
            CreateBox(root.transform, "NeonHood", new Vector3(0f, 0.84f, 1.1f), new Vector3(1.2f, 0.04f, 0.08f), neon);
            // Side skirts neon
            CreateBox(root.transform, "NeonL", new Vector3(-0.95f, 0.25f, 0f), new Vector3(0.06f, 0.06f, 3.2f), neon);
            CreateBox(root.transform, "NeonR", new Vector3(0.95f, 0.25f, 0f), new Vector3(0.06f, 0.06f, 3.2f), neon);
            // Rear wing
            CreateBox(root.transform, "Wing", new Vector3(0f, 1.15f, -1.95f), new Vector3(1.7f, 0.08f, 0.35f), paint);
            CreateBox(root.transform, "WingSupport", new Vector3(0f, 0.95f, -1.85f), new Vector3(0.15f, 0.35f, 0.15f), chrome);
            // Exhaust tips
            CreateCylinder(root.transform, "ExL", new Vector3(-0.35f, 0.28f, -2.15f), new Vector3(0.12f, 0.12f, 0.2f), chrome);
            CreateCylinder(root.transform, "ExR", new Vector3(0.35f, 0.28f, -2.15f), new Vector3(0.12f, 0.12f, 0.2f), chrome);
            // Underglow
            CreateBox(root.transform, "Underglow", new Vector3(0f, 0.08f, 0f), new Vector3(1.6f, 0.03f, 3.6f), neon);

            // Wheels
            float[] zx = { 1.35f, 1.35f, -1.35f, -1.35f };
            float[] xx = { -0.85f, 0.85f, -0.85f, 0.85f };
            for (int i = 0; i < 4; i++)
            {
                var w = CreateCylinder(root.transform, "Wheel" + i,
                    new Vector3(xx[i], 0.35f, zx[i]),
                    new Vector3(0.55f, 0.22f, 0.55f), rubber);
                w.transform.localRotation = Quaternion.Euler(0f, 0f, 90f);
            }

            // Headlights
            CreateBox(root.transform, "HL", new Vector3(-0.55f, 0.65f, 2.05f), new Vector3(0.35f, 0.12f, 0.08f), neon);
            CreateBox(root.transform, "HR", new Vector3(0.55f, 0.65f, 2.05f), new Vector3(0.35f, 0.12f, 0.08f), neon);
            // Taillights magenta
            var tail = MaterialFactory.NeonEmissive(new Color(1f, 0.15f, 0.45f), 5f);
            CreateBox(root.transform, "TL", new Vector3(-0.55f, 0.7f, -2.12f), new Vector3(0.4f, 0.1f, 0.06f), tail);
            CreateBox(root.transform, "TR", new Vector3(0.55f, 0.7f, -2.12f), new Vector3(0.4f, 0.1f, 0.06f), tail);

            // Emit points for trails
            var leftEmit = new GameObject("TrailEmitL").transform;
            leftEmit.SetParent(root.transform, false);
            leftEmit.localPosition = new Vector3(-0.7f, 0.05f, -2.0f);
            var rightEmit = new GameObject("TrailEmitR").transform;
            rightEmit.SetParent(root.transform, false);
            rightEmit.localPosition = new Vector3(0.7f, 0.05f, -2.0f);

            if (addPlayerComponents)
            {
                var controller = root.AddComponent<ArcadeCarController>();
                root.AddComponent<MobileCarInput>().car = controller;
                var trails = root.AddComponent<NeonTrailSystem>();
                trails.car = controller;
                trails.leftEmitPoint = leftEmit;
                trails.rightEmitPoint = rightEmit;
                root.AddComponent<DriftScoreManager>().car = controller;
                var vfx = root.AddComponent<NitroVFXController>();
                vfx.car = controller;
                // Exhaust flame anchors
                var fl = new GameObject("FlameL").transform;
                fl.SetParent(root.transform, false);
                fl.localPosition = new Vector3(-0.35f, 0.28f, -2.2f);
                var fr = new GameObject("FlameR").transform;
                fr.SetParent(root.transform, false);
                fr.localPosition = new Vector3(0.35f, 0.28f, -2.2f);
            }

            return root;
        }

        private static GameObject CreateBox(Transform parent, string name, Vector3 localPos, Vector3 scale, Material mat)
        {
            var go = GameObject.CreatePrimitive(PrimitiveType.Cube);
            go.name = name;
            go.transform.SetParent(parent, false);
            go.transform.localPosition = localPos;
            go.transform.localScale = scale;
            var col = go.GetComponent<Collider>();
            if (col) Object.Destroy(col);
            var r = go.GetComponent<Renderer>();
            if (r && mat) r.sharedMaterial = mat;
            return go;
        }

        private static GameObject CreateCylinder(Transform parent, string name, Vector3 localPos, Vector3 scale, Material mat)
        {
            var go = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
            go.name = name;
            go.transform.SetParent(parent, false);
            go.transform.localPosition = localPos;
            go.transform.localScale = scale;
            var col = go.GetComponent<Collider>();
            if (col) Object.Destroy(col);
            var r = go.GetComponent<Renderer>();
            if (r && mat) r.sharedMaterial = mat;
            return go;
        }
    }
}

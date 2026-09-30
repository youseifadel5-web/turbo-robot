using UnityEngine;
using NeonRush.Cars;

namespace NeonRush.Cars
{
    /// <summary>
    /// Procedural cockpit interior for first-person / cockpit camera modes:
    /// dark dashboard shell, steering wheel that turns with player input,
    /// red neon ambient strips (matches the reference cockpit look).
    /// Enabled automatically when ChaseCamera switches to Cockpit/Hood.
    /// </summary>
    public class CockpitInterior : MonoBehaviour
    {
        public ArcadeCarController car;
        public ChaseCamera chase;

        private Transform wheel;
        private GameObject root;
        private bool wasVisible;

        private void Start()
        {
            if (car == null) car = GetComponent<ArcadeCarController>();
            Build();
            SetVisible(false);
        }

        private void Build()
        {
            root = new GameObject("CockpitInterior");
            root.transform.SetParent(transform, false);

            var lit = Shader.Find("Universal Render Pipeline/Lit");
            var unlit = Shader.Find("NeonRush/AdditiveUnlit");

            var dashMat = new Material(lit) { name = "Cockpit_Dash" };
            dashMat.color = new Color(0.035f, 0.035f, 0.04f);
            if (dashMat.HasProperty("_Smoothness")) dashMat.SetFloat("_Smoothness", 0.35f);

            var leatherMat = new Material(lit) { name = "Cockpit_Leather" };
            leatherMat.color = new Color(0.06f, 0.05f, 0.05f);

            Material glowMat = null;
            if (unlit != null)
            {
                glowMat = new Material(unlit) { name = "Cockpit_RedStrip" };
                glowMat.SetColor("_BaseColor", new Color(1f, 0.12f, 0.16f, 0.8f));
                glowMat.SetFloat("_Intensity", 1.6f);
            }

            // Dashboard slab (across the cabin, at windshield base)
            var dash = GameObject.CreatePrimitive(PrimitiveType.Cube);
            dash.name = "Dashboard";
            Destroy(dash.GetComponent<Collider>());
            dash.transform.SetParent(root.transform, false);
            dash.transform.localPosition = new Vector3(0f, 0.86f, 0.62f);
            dash.transform.localScale = new Vector3(1.72f, 0.26f, 0.5f);
            dash.GetComponent<Renderer>().material = dashMat;

            // Hump over instruments
            var hump = GameObject.CreatePrimitive(PrimitiveType.Cube);
            hump.name = "InstrumentHood";
            Destroy(hump.GetComponent<Collider>());
            hump.transform.SetParent(root.transform, false);
            hump.transform.localPosition = new Vector3(0f, 0.92f, 0.5f);
            hump.transform.localScale = new Vector3(0.55f, 0.12f, 0.3f);
            hump.GetComponent<Renderer>().material = dashMat;

            // Center console
            var console = GameObject.CreatePrimitive(PrimitiveType.Cube);
            console.name = "Console";
            Destroy(console.GetComponent<Collider>());
            console.transform.SetParent(root.transform, false);
            console.transform.localPosition = new Vector3(0f, 0.62f, 0.05f);
            console.transform.localScale = new Vector3(0.34f, 0.18f, 1.1f);
            console.GetComponent<Renderer>().material = dashMat;

            // Steering wheel: rim from 3 curved segments approximated by a squashed torus
            // built from a rotated cylinder ring — simple + cheap: flat cylinder + hub.
            var wheelGo = new GameObject("SteeringWheel");
            wheelGo.transform.SetParent(root.transform, false);
            wheelGo.transform.localPosition = new Vector3(0f, 0.92f, 0.28f);
            wheelGo.transform.localRotation = Quaternion.Euler(68f, 0f, 0f);
            wheelGo.transform.localScale = Vector3.one * 0.36f;

            var rim = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
            rim.name = "Rim";
            Destroy(rim.GetComponent<Collider>());
            rim.transform.SetParent(wheelGo.transform, false);
            rim.transform.localPosition = Vector3.zero;
            rim.transform.localScale = new Vector3(1f, 0.05f, 1f);
            rim.GetComponent<Renderer>().material = leatherMat;

            // hollow-look: three spokes + hub
            var hub = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
            hub.name = "Hub";
            Destroy(hub.GetComponent<Collider>());
            hub.transform.SetParent(wheelGo.transform, false);
            hub.transform.localPosition = Vector3.zero;
            hub.transform.localScale = new Vector3(0.34f, 0.14f, 0.34f);
            hub.GetComponent<Renderer>().material = dashMat;

            for (int i = 0; i < 3; i++)
            {
                var spoke = GameObject.CreatePrimitive(PrimitiveType.Cube);
                spoke.name = "Spoke" + i;
                Destroy(spoke.GetComponent<Collider>());
                spoke.transform.SetParent(wheelGo.transform, false);
                spoke.transform.localRotation = Quaternion.Euler(0f, i * 120f, 0f);
                spoke.transform.localPosition = Quaternion.Euler(0f, i * 120f, 0f) * new Vector3(0.35f, 0f, 0f);
                spoke.transform.localScale = new Vector3(0.68f, 0.07f, 0.09f);
                spoke.GetComponent<Renderer>().material = dashMat;
            }
            wheel = wheelGo.transform;

            // Red neon strip across the dash (reference-style ambient)
            if (glowMat != null)
            {
                var strip = GameObject.CreatePrimitive(PrimitiveType.Cube);
                strip.name = "RedStrip";
                Destroy(strip.GetComponent<Collider>());
                strip.transform.SetParent(root.transform, false);
                strip.transform.localPosition = new Vector3(0f, 0.99f, 0.38f);
                strip.transform.localScale = new Vector3(1.5f, 0.012f, 0.03f);
                strip.GetComponent<Renderer>().material = glowMat;

                var strip2 = GameObject.CreatePrimitive(PrimitiveType.Cube);
                strip2.name = "RedStripSide";
                Destroy(strip2.GetComponent<Collider>());
                strip2.transform.SetParent(root.transform, false);
                strip2.transform.localPosition = new Vector3(0.8f, 0.95f, 0.1f);
                strip2.transform.localScale = new Vector3(0.03f, 0.012f, 0.6f);
                strip2.GetComponent<Renderer>().material = glowMat;
            }
        }

        private void LateUpdate()
        {
            bool visible = chase != null &&
                (chase.mode == CameraMode.Cockpit || chase.mode == CameraMode.Hood);
            if (visible != wasVisible) SetVisible(visible);
            wasVisible = visible;

            if (visible && wheel != null && car != null)
            {
                float target = -car.SteeringInput * 200f;
                var r = wheel.localRotation;
                r.eulerAngles = new Vector3(68f, 0f, target);
                wheel.localRotation = Quaternion.Slerp(wheel.localRotation, r, Time.deltaTime * 14f);
            }
        }

        private void SetVisible(bool on)
        {
            if (root != null) root.SetActive(on);
        }
    }
}

using UnityEngine;

namespace NeonRush.Environment
{
    /// <summary>
    /// Procedural neon night city around a road corridor — buildings, neon signs, lamps.
    /// </summary>
    public class NeonCityBuilder : MonoBehaviour
    {
        public float roadLength = 800f;
        public float roadWidth = 14f;
        public float buildingSpacing = 28f;
        public int rows = 2;

        public void Build(Transform parent = null)
        {
            var root = parent != null ? parent : transform;
            ClearChildren(root);

            var roadMat = MaterialFactory.RoadWet();
            var lineMat = MaterialFactory.AsphaltMarking(new Color(0.95f, 0.95f, 0.85f));
            var cyan = MaterialFactory.NeonEmissive(new Color(0f, 0.94f, 1f), 4f);
            var magenta = MaterialFactory.NeonEmissive(new Color(1f, 0.17f, 0.84f), 4f);
            var buildingDark = MaterialFactory.Building(new Color(0.07f, 0.08f, 0.11f));
            var buildingMid = MaterialFactory.Building(new Color(0.1f, 0.11f, 0.15f));

            // Ground plane
            var ground = GameObject.CreatePrimitive(PrimitiveType.Cube);
            ground.name = "Ground";
            ground.transform.SetParent(root, false);
            ground.transform.localPosition = new Vector3(0f, -0.2f, roadLength * 0.5f);
            ground.transform.localScale = new Vector3(120f, 0.4f, roadLength + 40f);
            ground.GetComponent<Renderer>().sharedMaterial = MaterialFactory.Building(new Color(0.04f, 0.05f, 0.06f));

            // Main road
            var road = GameObject.CreatePrimitive(PrimitiveType.Cube);
            road.name = "Road";
            road.transform.SetParent(root, false);
            road.transform.localPosition = new Vector3(0f, 0.02f, roadLength * 0.5f);
            road.transform.localScale = new Vector3(roadWidth, 0.08f, roadLength);
            road.GetComponent<Renderer>().sharedMaterial = roadMat;

            // Center dashed line
            int dashes = Mathf.FloorToInt(roadLength / 8f);
            for (int i = 0; i < dashes; i++)
            {
                if (i % 2 == 1) continue;
                var d = GameObject.CreatePrimitive(PrimitiveType.Cube);
                d.name = "Dash";
                d.transform.SetParent(root, false);
                d.transform.localPosition = new Vector3(0f, 0.07f, 4f + i * 8f);
                d.transform.localScale = new Vector3(0.25f, 0.02f, 3.5f);
                Object.Destroy(d.GetComponent<Collider>());
                d.GetComponent<Renderer>().sharedMaterial = lineMat;
            }

            // Side neon curb strips
            for (int side = -1; side <= 1; side += 2)
            {
                var curb = GameObject.CreatePrimitive(PrimitiveType.Cube);
                curb.name = side < 0 ? "CurbL" : "CurbR";
                curb.transform.SetParent(root, false);
                curb.transform.localPosition = new Vector3(side * (roadWidth * 0.5f + 0.15f), 0.12f, roadLength * 0.5f);
                curb.transform.localScale = new Vector3(0.2f, 0.15f, roadLength);
                Object.Destroy(curb.GetComponent<Collider>());
                curb.GetComponent<Renderer>().sharedMaterial = side < 0 ? cyan : magenta;
            }

            // Buildings both sides
            float z = 20f;
            int idx = 0;
            while (z < roadLength - 20f)
            {
                for (int side = -1; side <= 1; side += 2)
                {
                    for (int r = 0; r < rows; r++)
                    {
                        float x = side * (roadWidth * 0.5f + 8f + r * 14f + Random.Range(0f, 3f));
                        float h = Random.Range(12f, 42f);
                        float w = Random.Range(6f, 12f);
                        float d = Random.Range(6f, 14f);
                        var b = GameObject.CreatePrimitive(PrimitiveType.Cube);
                        b.name = "Bld_" + idx++;
                        b.transform.SetParent(root, false);
                        b.transform.localPosition = new Vector3(x, h * 0.5f, z + Random.Range(-4f, 4f));
                        b.transform.localScale = new Vector3(w, h, d);
                        b.GetComponent<Renderer>().sharedMaterial = Random.value > 0.5f ? buildingDark : buildingMid;

                        // Neon vertical strip
                        var strip = GameObject.CreatePrimitive(PrimitiveType.Cube);
                        strip.name = "NeonStrip";
                        strip.transform.SetParent(b.transform, false);
                        strip.transform.localPosition = new Vector3(side < 0 ? 0.51f : -0.51f, 0.1f, 0f);
                        strip.transform.localScale = new Vector3(0.06f, 0.7f, 0.08f);
                        Object.Destroy(strip.GetComponent<Collider>());
                        strip.GetComponent<Renderer>().sharedMaterial = Random.value > 0.45f ? cyan : magenta;

                        // Window grid glow (simple quads)
                        if (h > 18f && Random.value > 0.4f)
                        {
                            var win = GameObject.CreatePrimitive(PrimitiveType.Cube);
                            win.name = "Windows";
                            win.transform.SetParent(b.transform, false);
                            win.transform.localPosition = new Vector3(side < 0 ? 0.52f : -0.52f, 0.05f, 0f);
                            win.transform.localScale = new Vector3(0.03f, 0.55f, 0.7f);
                            Object.Destroy(win.GetComponent<Collider>());
                            win.GetComponent<Renderer>().sharedMaterial = MaterialFactory.NeonEmissive(
                                new Color(0.4f, 0.7f, 1f), 1.2f);
                        }
                    }
                }

                // Street lamp
                SpawnLamp(root, -roadWidth * 0.55f - 1.2f, z, cyan);
                SpawnLamp(root, roadWidth * 0.55f + 1.2f, z + buildingSpacing * 0.5f, magenta);

                z += buildingSpacing + Random.Range(-4f, 6f);
            }

            // Billboards
            for (int i = 0; i < 8; i++)
            {
                float bz = Random.Range(40f, roadLength - 40f);
                int side = Random.value > 0.5f ? 1 : -1;
                var board = GameObject.CreatePrimitive(PrimitiveType.Cube);
                board.name = "Billboard";
                board.transform.SetParent(root, false);
                board.transform.localPosition = new Vector3(side * 18f, 8f + Random.Range(0f, 6f), bz);
                board.transform.localScale = new Vector3(0.3f, 4f, 8f);
                Object.Destroy(board.GetComponent<Collider>());
                board.GetComponent<Renderer>().sharedMaterial = Random.value > 0.5f ? cyan : magenta;
            }


            // Extra wet highlight strip down the lane
            var wet = GameObject.CreatePrimitive(PrimitiveType.Cube);
            wet.name = "WetSheen";
            wet.transform.SetParent(root, false);
            wet.transform.localPosition = new Vector3(0f, 0.06f, roadLength * 0.5f);
            wet.transform.localScale = new Vector3(roadWidth * 0.35f, 0.01f, roadLength);
            Object.Destroy(wet.GetComponent<Collider>());
            wet.GetComponent<Renderer>().sharedMaterial = MaterialFactory.NeonEmissive(new Color(0.05f, 0.15f, 0.22f), 0.6f);

            Debug.Log("[NeonCity] Built corridor length=" + roadLength);
        }

        private void SpawnLamp(Transform root, float x, float z, Material neonMat)
        {
            var pole = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
            pole.name = "LampPole";
            pole.transform.SetParent(root, false);
            pole.transform.localPosition = new Vector3(x, 3f, z);
            pole.transform.localScale = new Vector3(0.15f, 3f, 0.15f);
            Object.Destroy(pole.GetComponent<Collider>());
            pole.GetComponent<Renderer>().sharedMaterial = MaterialFactory.Building(new Color(0.12f, 0.12f, 0.14f));

            var head = GameObject.CreatePrimitive(PrimitiveType.Cube);
            head.name = "LampHead";
            head.transform.SetParent(root, false);
            head.transform.localPosition = new Vector3(x, 6.1f, z);
            head.transform.localScale = new Vector3(0.8f, 0.15f, 0.4f);
            Object.Destroy(head.GetComponent<Collider>());
            head.GetComponent<Renderer>().sharedMaterial = neonMat;

            var lightGo = new GameObject("PointLight");
            lightGo.transform.SetParent(root, false);
            lightGo.transform.localPosition = new Vector3(x * 0.7f, 5.5f, z);
            var pl = lightGo.AddComponent<Light>();
            pl.type = LightType.Point;
            pl.range = 18f;
            pl.intensity = 2.2f;
            pl.color = neonMat.HasProperty("_EmissionColor")
                ? neonMat.GetColor("_EmissionColor").gamma
                : Color.cyan;
            pl.shadows = LightShadows.None;
        }

        private static void ClearChildren(Transform root)
        {
            for (int i = root.childCount - 1; i >= 0; i--)
                Object.Destroy(root.GetChild(i).gameObject);
        }
    }
}

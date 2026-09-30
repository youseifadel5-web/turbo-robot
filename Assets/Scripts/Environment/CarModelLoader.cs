using UnityEngine;
using NeonRush.Cars;
using NeonRush.Save;

namespace NeonRush.Environment
{
    /// <summary>
    /// Loads car models at runtime. Priority:
    /// 1. Resources/Prefabs/Car_{id}  (built by the Setup Wizard from the OBJ assets)
    /// 2. ProceduralCarBuilder fallback (always works, zero dependencies)
    /// Applies saved customization (paint / rims) to the result.
    /// </summary>
    public static class CarModelLoader
    {
        public static GameObject LoadCar(string carId, bool addPlayerComponents)
        {
            GameObject carGo = null;
            var stats = CarCatalog.CreateStats(carId);

            var prefab = Resources.Load<GameObject>($"Prefabs/Car_{carId}");
            if (prefab != null)
            {
                carGo = Object.Instantiate(prefab);
                carGo.name = "Car_" + carId;
                var ctrl = carGo.GetComponent<ArcadeCarController>() ?? carGo.AddComponent<ArcadeCarController>();
                ctrl.stats = stats;
                if (carGo.GetComponent<Rigidbody>() == null)
                {
                    var rb = carGo.AddComponent<Rigidbody>();
                    rb.mass = stats.mass;
                }
                if (carGo.GetComponent<BoxCollider>() == null)
                {
                    var col = carGo.AddComponent<BoxCollider>();
                    col.size = new Vector3(1.9f, 1.0f, 4.4f);
                    col.center = new Vector3(0f, 0.5f, 0f);
                }
            }
            else
            {
                var host = new GameObject("CarBuilderHost");
                var builder = host.AddComponent<ProceduralCarBuilder>();
                var def = CarCatalog.Get(carId);
                builder.paintColor = CarCatalog.HexToColor(def.defaultPaintHex);
                builder.neonColor = CarCatalog.HexToColor(def.defaultNeonHex);
                builder.addPlayerComponents = addPlayerComponents;
                carGo = builder.Build();
                carGo.name = "Car_" + carId;
                var ctrl = carGo.GetComponent<ArcadeCarController>();
                if (ctrl != null) ctrl.stats = stats;
                Object.Destroy(host);
            }

            var owned = SaveSystem.Current.ownedCars.Find(o => o.carId == carId);
            if (owned != null)
                CustomizeApplier.Apply(carGo, owned.paintId, owned.rimId,
                    string.IsNullOrEmpty(owned.neonId) ? "cyan" : owned.neonId);

            return carGo;
        }

        public static GameObject LoadTraffic(int variant)
        {
            var prefab = Resources.Load<GameObject>(variant % 2 == 0 ? "Prefabs/Traffic_Sedan" : "Prefabs/Traffic_Van");
            if (prefab != null)
            {
                var go = Object.Instantiate(prefab);
                go.name = "TrafficCar";
                if (go.GetComponent<Rigidbody>() == null)
                {
                    var rb = go.AddComponent<Rigidbody>();
                    rb.mass = 1300f;
                }
                if (go.GetComponent<BoxCollider>() == null)
                {
                    var col = go.AddComponent<BoxCollider>();
                    col.size = new Vector3(1.8f, 1.2f, 4.3f);
                    col.center = new Vector3(0f, 0.6f, 0f);
                }
                return go;
            }

            // fallback: plain box car
            var box = GameObject.CreatePrimitive(PrimitiveType.Cube);
            box.name = "TrafficCar";
            box.transform.localScale = new Vector3(1.8f, 1.0f, 4.2f);
            box.GetComponent<Renderer>().material.color = Color.HSVToRGB(Random.value, 0.35f, 0.5f);
            var rb2 = box.AddComponent<Rigidbody>();
            rb2.mass = 1300f;
            box.AddComponent<BoxCollider>();
            return box;
        }
    }
}

using UnityEngine;
using UnityEngine.Rendering;
using NeonRush.Cars;
using NeonRush.Save;

namespace NeonRush.Cars
{
    /// <summary>
    /// Applies customization choices to a car instance as real material
    /// changes (paint, rims, neon underglow) and persists them to the save.
    /// Works with wizard-built OBJ prefabs (named materials) and with the
    /// procedural car (falls back to the first renderer material).
    /// </summary>
    public static class CustomizeApplier
    {
        public static void Apply(GameObject car, string paintId, string rimId, string neonId)
        {
            if (car == null) return;

            var paint = CarCatalog.HexToColor(CarCatalog.PaintHex(paintId));
            var rim = CarCatalog.HexToColor(CarCatalog.RimHex(rimId));
            Color neon = Color.clear;
            if (neonId != null)
            {
                foreach (var n in CarCatalog.NeonColors)
                    if (n.id == neonId) neon = CarCatalog.HexToColor(n.hex);
            }

            foreach (var rend in car.GetComponentsInChildren<Renderer>(true))
            {
                var mats = rend.sharedMaterials;
                bool changed = false;
                for (int i = 0; i < mats.Length; i++)
                {
                    var m = mats[i];
                    if (m == null) continue;
                    string n = m.name;

                    if (Contains(n, "Body"))
                    {
                        m = Instance(rend, i, m);
                        m.color = paint;
                        if (m.HasProperty("_BaseColor")) m.SetColor("_BaseColor", paint);
                        mats[i] = m; changed = true;
                    }
                    else if (Contains(n, "Rims"))
                    {
                        m = Instance(rend, i, m);
                        if (m.HasProperty("_BaseColor")) m.SetColor("_BaseColor", rim);
                        else m.color = rim;
                        mats[i] = m; changed = true;
                    }
                    else if (Contains(n, "Glow"))
                    {
                        m = Instance(rend, i, m);
                        var glowCol = neon != Color.clear ? neon : DefaultNeon(car);
                        if (m.HasProperty("_BaseColor")) m.SetColor("_BaseColor", glowCol);
                        else m.color = glowCol;
                        if (m.HasProperty("_EmissionColor")) m.SetColor("_EmissionColor", glowCol * 2f);
                        mats[i] = m; changed = true;
                    }
                }
                if (changed) rend.sharedMaterials = mats;
            }

            // procedural fallback: first renderer's first material = paint
            if (!HasNamedMaterial(car))
            {
                var rend = car.GetComponentInChildren<Renderer>();
                if (rend != null)
                {
                    var m = rend.material;
                    m.color = paint;
                    if (m.HasProperty("_BaseColor")) m.SetColor("_BaseColor", paint);
                }
            }
        }

        public static void SaveChoice(string carId, string paintId, string rimId)
        {
            var owned = CarCatalog.GetOwned(carId);
            owned.paintId = paintId;
            owned.rimId = rimId;
            SaveSystem.Save();
        }

        public static void SaveChoice(string carId, string paintId, string rimId, string neonId)
        {
            var owned = CarCatalog.GetOwned(carId);
            owned.paintId = paintId;
            owned.rimId = rimId;
            owned.neonId = neonId;
            SaveSystem.Save();
        }

        private static Material Instance(Renderer rend, int index, Material source)
        {
            // work on the instance copy so prefabs are never mutated
            var mats = rend.sharedMaterials;
            var m = InstantiateSafe(source);
            mats[index] = m;
            rend.sharedMaterials = mats;
            return m;
        }

        private static Material InstantiateSafe(Material source)
        {
            return new Material(source) { name = source.name + "_inst" };
        }

        private static bool HasNamedMaterial(GameObject car)
        {
            foreach (var rend in car.GetComponentsInChildren<Renderer>(true))
                foreach (var m in rend.sharedMaterials)
                    if (m != null && (Contains(m.name, "Body") || Contains(m.name, "Glow")))
                        return true;
            return false;
        }

        private static Color DefaultNeon(GameObject car)
        {
            var ctrl = car.GetComponent<ArcadeCarController>();
            if (ctrl != null && ctrl.stats != null) return ctrl.stats.classGlowColor;
            return new Color(0f, 0.94f, 1f, 1f);
        }

        private static bool Contains(string s, string term) =>
            !string.IsNullOrEmpty(s) && s.ToLowerInvariant().Contains(term.ToLowerInvariant());
    }
}

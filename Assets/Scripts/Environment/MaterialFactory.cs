using UnityEngine;

namespace NeonRush.Environment
{
    /// <summary>
    /// Runtime URP/Standard-friendly materials for neon prototype without external assets.
    /// </summary>
    public static class MaterialFactory
    {
        private static readonly Shader Lit = Shader.Find("Universal Render Pipeline/Lit")
            ?? Shader.Find("Standard")
            ?? Shader.Find("Diffuse");

        public static Material RoadWet()
        {
            var m = new Material(Lit);
            m.name = "NR_RoadWet";
            SetColor(m, new Color(0.08f, 0.09f, 0.11f));
            SetFloat(m, "_Metallic", 0.05f);
            SetFloat(m, "_Smoothness", 0.88f);
            SetFloat(m, "_Glossiness", 0.88f);
            return m;
        }

        public static Material AsphaltMarking(Color c)
        {
            var m = new Material(Lit);
            m.name = "NR_Marking";
            SetColor(m, c);
            SetFloat(m, "_Smoothness", 0.4f);
            SetFloat(m, "_Glossiness", 0.4f);
            return m;
        }

        public static Material Building(Color c)
        {
            var m = new Material(Lit);
            m.name = "NR_Building";
            SetColor(m, c);
            SetFloat(m, "_Metallic", 0.15f);
            SetFloat(m, "_Smoothness", 0.35f);
            SetFloat(m, "_Glossiness", 0.35f);
            return m;
        }

        public static Material NeonEmissive(Color c, float intensity = 3.5f)
        {
            var m = new Material(Lit);
            m.name = "NR_Neon";
            var bright = c * intensity;
            bright.a = 1f;
            SetColor(m, c);
            SetColor(m, "_EmissionColor", bright);
            m.EnableKeyword("_EMISSION");
            if (m.HasProperty("_EmissionColor"))
                m.SetColor("_EmissionColor", bright);
            return m;
        }

        public static Material CarPaint(Color c)
        {
            var m = new Material(Lit);
            m.name = "NR_CarPaint";
            SetColor(m, c);
            SetFloat(m, "_Metallic", 0.65f);
            SetFloat(m, "_Smoothness", 0.9f);
            SetFloat(m, "_Glossiness", 0.9f);
            return m;
        }

        public static Material Glass()
        {
            var m = new Material(Lit);
            m.name = "NR_Glass";
            SetColor(m, new Color(0.15f, 0.2f, 0.28f, 0.55f));
            SetFloat(m, "_Metallic", 0.1f);
            SetFloat(m, "_Smoothness", 0.95f);
            SetFloat(m, "_Glossiness", 0.95f);
            SetFloat(m, "_Surface", 1f);
            return m;
        }

        public static Material Rubber()
        {
            var m = new Material(Lit);
            m.name = "NR_Rubber";
            SetColor(m, new Color(0.05f, 0.05f, 0.05f));
            SetFloat(m, "_Metallic", 0f);
            SetFloat(m, "_Smoothness", 0.25f);
            SetFloat(m, "_Glossiness", 0.25f);
            return m;
        }

        public static Material Chrome()
        {
            var m = new Material(Lit);
            m.name = "NR_Chrome";
            SetColor(m, new Color(0.75f, 0.78f, 0.82f));
            SetFloat(m, "_Metallic", 1f);
            SetFloat(m, "_Smoothness", 0.95f);
            SetFloat(m, "_Glossiness", 0.95f);
            return m;
        }

        private static void SetColor(Material m, Color c)
        {
            if (m.HasProperty("_BaseColor")) m.SetColor("_BaseColor", c);
            if (m.HasProperty("_Color")) m.SetColor("_Color", c);
        }

        private static void SetColor(Material m, string prop, Color c)
        {
            if (m.HasProperty(prop)) m.SetColor(prop, c);
        }

        private static void SetFloat(Material m, string prop, float v)
        {
            if (m.HasProperty(prop)) m.SetFloat(prop, v);
        }
    }
}

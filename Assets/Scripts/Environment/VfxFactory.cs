using UnityEngine;

namespace NeonRush.Environment
{
    /// <summary>
    /// Runtime particle VFX without prefabs: tire smoke, nitro flame, sparks, rain.
    /// Attach outputs to NitroVFXController / weather.
    /// </summary>
    public static class VfxFactory
    {
        public static ParticleSystem CreateTireSmoke(Transform parent, Vector3 localPos)
        {
            var go = new GameObject("TireSmoke");
            go.transform.SetParent(parent, false);
            go.transform.localPosition = localPos;
            var ps = go.AddComponent<ParticleSystem>();
            var main = ps.main;
            main.startLifetime = 1.2f;
            main.startSpeed = 0.8f;
            main.startSize = 0.9f;
            main.startColor = new Color(0.75f, 0.75f, 0.78f, 0.55f);
            main.simulationSpace = ParticleSystemSimulationSpace.World;
            main.maxParticles = 80;
            main.loop = true;
            main.playOnAwake = false;

            var emission = ps.emission;
            emission.rateOverTime = 0f;

            var shape = ps.shape;
            shape.shapeType = ParticleSystemShapeType.Cone;
            shape.angle = 18f;
            shape.radius = 0.15f;

            var colorOver = ps.colorOverLifetime;
            colorOver.enabled = true;
            var grad = new Gradient();
            grad.SetKeys(
                new[] {
                    new GradientColorKey(Color.white, 0f),
                    new GradientColorKey(new Color(0.4f, 0.9f, 1f), 0.6f),
                    new GradientColorKey(new Color(1f, 0.2f, 0.8f), 1f)
                },
                new[] {
                    new GradientAlphaKey(0.5f, 0f),
                    new GradientAlphaKey(0.25f, 0.5f),
                    new GradientAlphaKey(0f, 1f)
                });
            colorOver.color = grad;

            var renderer = go.GetComponent<ParticleSystemRenderer>();
            renderer.material = CreateParticleMat(new Color(0.8f, 0.85f, 0.9f, 0.4f));
            return ps;
        }

        public static ParticleSystem CreateNitroFlame(Transform parent, Vector3 localPos)
        {
            var go = new GameObject("NitroFlame");
            go.transform.SetParent(parent, false);
            go.transform.localPosition = localPos;
            go.transform.localRotation = Quaternion.Euler(90f, 0f, 0f);
            var ps = go.AddComponent<ParticleSystem>();
            var main = ps.main;
            main.startLifetime = 0.25f;
            main.startSpeed = 6f;
            main.startSize = 0.35f;
            main.startColor = new Color(0.2f, 0.9f, 1f, 0.9f);
            main.simulationSpace = ParticleSystemSimulationSpace.Local;
            main.maxParticles = 60;
            main.loop = true;
            main.playOnAwake = false;

            var emission = ps.emission;
            emission.rateOverTime = 0f;

            var shape = ps.shape;
            shape.shapeType = ParticleSystemShapeType.Cone;
            shape.angle = 8f;
            shape.radius = 0.05f;

            var colorOver = ps.colorOverLifetime;
            colorOver.enabled = true;
            var grad = new Gradient();
            grad.SetKeys(
                new[] {
                    new GradientColorKey(Color.white, 0f),
                    new GradientColorKey(new Color(0.2f, 0.95f, 1f), 0.3f),
                    new GradientColorKey(new Color(0.1f, 0.3f, 1f), 1f)
                },
                new[] {
                    new GradientAlphaKey(1f, 0f),
                    new GradientAlphaKey(0.6f, 0.5f),
                    new GradientAlphaKey(0f, 1f)
                });
            colorOver.color = grad;

            var renderer = go.GetComponent<ParticleSystemRenderer>();
            renderer.material = CreateParticleMat(new Color(0.3f, 0.95f, 1f, 0.85f));
            return ps;
        }

        public static ParticleSystem CreateShockwaveBurst(Transform parent)
        {
            var go = new GameObject("NitroShockwave");
            go.transform.SetParent(parent, false);
            var ps = go.AddComponent<ParticleSystem>();
            var main = ps.main;
            main.startLifetime = 0.4f;
            main.startSpeed = 12f;
            main.startSize = 0.5f;
            main.startColor = new Color(0.4f, 1f, 1f, 0.7f);
            main.loop = false;
            main.playOnAwake = false;
            main.duration = 0.35f;
            main.maxParticles = 40;

            var emission = ps.emission;
            emission.SetBursts(new[] { new ParticleSystem.Burst(0f, 30) });
            emission.rateOverTime = 0f;

            var shape = ps.shape;
            shape.shapeType = ParticleSystemShapeType.Circle;
            shape.radius = 0.4f;

            var renderer = go.GetComponent<ParticleSystemRenderer>();
            renderer.material = CreateParticleMat(new Color(0.3f, 1f, 1f, 0.6f));
            return ps;
        }

        public static ParticleSystem CreateRain(Transform parent)
        {
            var go = new GameObject("RainFX");
            go.transform.SetParent(parent, false);
            go.transform.localPosition = new Vector3(0f, 12f, 20f);
            var ps = go.AddComponent<ParticleSystem>();
            var main = ps.main;
            main.startLifetime = 0.6f;
            main.startSpeed = 18f;
            main.startSize = 0.05f;
            main.startColor = new Color(0.7f, 0.8f, 0.95f, 0.35f);
            main.simulationSpace = ParticleSystemSimulationSpace.World;
            main.maxParticles = 800;
            main.loop = true;

            var emission = ps.emission;
            emission.rateOverTime = 400f;

            var shape = ps.shape;
            shape.shapeType = ParticleSystemShapeType.Box;
            shape.scale = new Vector3(40f, 1f, 60f);

            var renderer = go.GetComponent<ParticleSystemRenderer>();
            renderer.material = CreateParticleMat(new Color(0.7f, 0.85f, 1f, 0.3f));
            return ps;
        }

        private static Material CreateParticleMat(Color c)
        {
            var sh = Shader.Find("Particles/Standard Unlit")
                ?? Shader.Find("Universal Render Pipeline/Particles/Unlit")
                ?? Shader.Find("Sprites/Default")
                ?? Shader.Find("Diffuse");
            var m = new Material(sh);
            if (m.HasProperty("_BaseColor")) m.SetColor("_BaseColor", c);
            if (m.HasProperty("_Color")) m.SetColor("_Color", c);
            if (m.HasProperty("_TintColor")) m.SetColor("_TintColor", c);
            return m;
        }
    }
}

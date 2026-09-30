using UnityEngine;
using System.Collections.Generic;

namespace NeonRush.Cars
{
    /// <summary>
    /// Object-pooled neon light trails under the car while Nitro is active.
    /// Distinctive Neon Rush visual — cyan to magenta fade.
    /// </summary>
    public class NeonTrailSystem : MonoBehaviour
    {
        [Header("References")]
        public ArcadeCarController car;
        public Transform leftEmitPoint;
        public Transform rightEmitPoint;

        [Header("Trail Settings")]
        public float spawnInterval = 0.04f;
        public float trailLifetime = 1.8f;
        public float trailWidth = 0.35f;
        public Color startColor = new Color(0f, 0.94f, 1f, 0.85f);
        public Color endColor = new Color(1f, 0.17f, 0.84f, 0f);
        public int poolSize = 48;

        [Header("Optional Prefab")]
        public GameObject trailSegmentPrefab; // simple quad or line with additive material

        private float spawnTimer;
        private readonly Queue<TrailSegment> pool = new Queue<TrailSegment>();
        private readonly List<TrailSegment> active = new List<TrailSegment>();

        private class TrailSegment
        {
            public GameObject go;
            public Transform tr;
            public float age;
            public MeshRenderer renderer;
            public MaterialPropertyBlock block;
        }

        private void Awake()
        {
            if (car == null) car = GetComponentInParent<ArcadeCarController>();
            WarmPool();
        }

        private void OnEnable()
        {
            if (car != null)
            {
                car.OnNitroStarted += OnNitroOn;
                car.OnNitroEnded += OnNitroOff;
            }
        }

        private void OnDisable()
        {
            if (car != null)
            {
                car.OnNitroStarted -= OnNitroOn;
                car.OnNitroEnded -= OnNitroOff;
            }
        }

        private void WarmPool()
        {
            for (int i = 0; i < poolSize; i++)
            {
                var seg = CreateSegment();
                seg.go.SetActive(false);
                pool.Enqueue(seg);
            }
        }

        private TrailSegment CreateSegment()
        {
            GameObject go;
            if (trailSegmentPrefab != null)
                go = Instantiate(trailSegmentPrefab, transform);
            else
            {
                go = GameObject.CreatePrimitive(PrimitiveType.Quad);
                go.name = "NeonTrailSeg";
                go.transform.SetParent(transform);
                var col = go.GetComponent<Collider>();
                if (col) Destroy(col);
            }

            var seg = new TrailSegment
            {
                go = go,
                tr = go.transform,
                renderer = go.GetComponent<MeshRenderer>(),
                block = new MaterialPropertyBlock()
            };
            return seg;
        }

        private void OnNitroOn() { /* trails start in Update when IsNitroActive */ }
        private void OnNitroOff() { }

        private void Update()
        {
            if (car == null) return;

            // Age active trails
            for (int i = active.Count - 1; i >= 0; i--)
            {
                var s = active[i];
                s.age += Time.deltaTime;
                float t = s.age / trailLifetime;
                if (t >= 1f)
                {
                    s.go.SetActive(false);
                    active.RemoveAt(i);
                    pool.Enqueue(s);
                    continue;
                }
                if (s.renderer != null)
                {
                    Color c = Color.Lerp(startColor, endColor, t);
                    s.block.SetColor("_BaseColor", c);
                    s.block.SetColor("_Color", c);
                    s.renderer.SetPropertyBlock(s.block);
                }
            }

            if (!car.IsNitroActive) return;

            spawnTimer -= Time.deltaTime;
            if (spawnTimer > 0f) return;
            spawnTimer = spawnInterval;

            SpawnAt(leftEmitPoint != null ? leftEmitPoint.position : transform.position + transform.right * -0.7f);
            SpawnAt(rightEmitPoint != null ? rightEmitPoint.position : transform.position + transform.right * 0.7f);
        }

        private void SpawnAt(Vector3 pos)
        {
            TrailSegment seg;
            if (pool.Count > 0) seg = pool.Dequeue();
            else seg = CreateSegment();

            seg.age = 0f;
            seg.tr.position = pos + Vector3.up * 0.05f;
            seg.tr.rotation = Quaternion.LookRotation(Vector3.up, car.transform.forward);
            seg.tr.localScale = new Vector3(trailWidth, trailWidth * 2.2f, 1f);
            seg.go.SetActive(true);
            active.Add(seg);
        }
    }
}

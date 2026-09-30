using UnityEngine;
using System.Collections.Generic;

namespace NeonRush.AI
{
    public enum TrafficDensity { Low, Medium, High }

    /// <summary>
    /// Pooled traffic cars that cruise lanes. Performance-friendly for mid Android.
    /// </summary>
    public class TrafficManager : MonoBehaviour
    {
        public TrafficDensity density = TrafficDensity.Medium;
        public GameObject trafficPrefab;
        public Transform[] laneAnchors; // points along the road
        public int pooledVehicles = 20;
        public float minSpeed = 12f;
        public float maxSpeed = 22f;
        public float despawnBehind = 80f;
        public float spawnAhead = 140f;
        public Transform player;

        private readonly List<TrafficCar> pool = new List<TrafficCar>();
        private readonly List<TrafficCar> active = new List<TrafficCar>();

        private class TrafficCar
        {
            public GameObject go;
            public Transform tr;
            public float speed;
            public int lane;
            public bool inUse;
        }

        public void SetDensity(TrafficDensity value)
        {
            density = value;
            pooledVehicles = value == TrafficDensity.Low ? 8 : value == TrafficDensity.Medium ? 16 : 28;
        }

        private void Start()
        {
            if (player == null)
            {
                var p = GameObject.FindGameObjectWithTag("Player");
                if (p != null) player = p.transform;
            }
            WarmPool();
        }

        private void WarmPool()
        {
            for (int i = 0; i < pooledVehicles; i++)
            {
                var tc = CreateCar();
                tc.go.SetActive(false);
                pool.Add(tc);
            }
        }

        private TrafficCar CreateCar()
        {
            GameObject go;
            if (trafficPrefab != null)
                go = Instantiate(trafficPrefab, transform);
            else
            {
                go = GameObject.CreatePrimitive(PrimitiveType.Cube);
                go.name = "Traffic";
                go.tag = "Traffic";
                go.transform.localScale = new Vector3(2f, 1.2f, 4.2f);
                go.transform.SetParent(transform);
                var r = go.GetComponent<Renderer>();
                if (r) r.material.color = new Color(Random.value * 0.4f + 0.1f, Random.value * 0.3f, Random.value * 0.5f + 0.2f);
            }

            return new TrafficCar { go = go, tr = go.transform };
        }

        private void Update()
        {
            if (player == null) return;

            int desired = density == TrafficDensity.Low ? 4 : density == TrafficDensity.Medium ? 8 : 14;
            desired = Mathf.Min(desired, pooledVehicles);

            while (active.Count < desired)
                SpawnAhead();

            for (int i = active.Count - 1; i >= 0; i--)
            {
                var tc = active[i];
                tc.tr.position += tc.tr.forward * tc.speed * Time.deltaTime;

                float along = Vector3.Dot(tc.tr.position - player.position, player.forward);
                if (along < -despawnBehind)
                {
                    Recycle(tc);
                    active.RemoveAt(i);
                }
            }
        }

        private void SpawnAhead()
        {
            TrafficCar tc = null;
            foreach (var p in pool)
            {
                if (!p.inUse) { tc = p; break; }
            }
            if (tc == null) return;

            int lane = Random.Range(-1, 2); // -1 left, 0 mid, 1 right
            Vector3 pos = player.position + player.forward * Random.Range(40f, spawnAhead);
            pos += player.right * (lane * 3.4f);
            pos.y = player.position.y;

            tc.tr.position = pos;
            tc.tr.rotation = player.rotation;
            tc.speed = Random.Range(minSpeed, maxSpeed);
            tc.lane = lane;
            tc.inUse = true;
            tc.go.SetActive(true);
            active.Add(tc);
        }

        private void Recycle(TrafficCar tc)
        {
            tc.inUse = false;
            tc.go.SetActive(false);
        }
    }
}

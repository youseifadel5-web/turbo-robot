using UnityEngine;
using System.Collections.Generic;

namespace NeonRush.Racing
{
    /// <summary>
    /// Ghost race architecture: records the player's best line and can
    /// play it back as a translucent rival. Data stored as JSON in
    /// persistentDataPath/ghosts. Ready for online leaderboards later.
    /// </summary>
    [System.Serializable]
    public class GhostData
    {
        public string trackId;
        public string carId;
        public float lapTime;
        public float[] times;
        public float[] x, y, z;
        public float[] qx, qy, qz, qw;
    }

    public class GhostRecorder : MonoBehaviour
    {
        public string trackId = "neon_city_01";
        public float sampleInterval = 0.1f;
        public bool autoRestartOnRaceStart = true;

        private readonly List<float> times = new List<float>();
        private readonly List<Vector3> positions = new List<Vector3>();
        private readonly List<Quaternion> rotations = new List<Quaternion>();
        private Transform car;
        private float timer;
        private float elapsed;
        private bool recording;

        public void Begin(Transform carTransform, string id)
        {
            car = carTransform;
            trackId = string.IsNullOrEmpty(id) ? trackId : id;
            times.Clear(); positions.Clear(); rotations.Clear();
            elapsed = 0f; timer = 0f; recording = carTransform != null;
        }

        public void Stop() => recording = false;

        private void Update()
        {
            if (!recording || car == null) return;
            elapsed += Time.deltaTime;
            timer += Time.deltaTime;
            if (timer < sampleInterval) return;
            timer = 0f;
            times.Add(elapsed);
            positions.Add(car.position);
            rotations.Add(car.rotation);
        }

        public GhostData GetData(string carId)
        {
            var d = new GhostData
            {
                trackId = trackId,
                carId = carId,
                lapTime = elapsed,
                times = times.ToArray(),
                x = new float[positions.Count],
                y = new float[positions.Count],
                z = new float[positions.Count],
                qx = new float[rotations.Count],
                qy = new float[rotations.Count],
                qz = new float[rotations.Count],
                qw = new float[rotations.Count]
            };
            for (int i = 0; i < positions.Count; i++)
            {
                d.x[i] = positions[i].x; d.y[i] = positions[i].y; d.z[i] = positions[i].z;
                d.qx[i] = rotations[i].x; d.qy[i] = rotations[i].y;
                d.qz[i] = rotations[i].z; d.qw[i] = rotations[i].w;
            }
            return d;
        }

        public static void Save(GhostData data)
        {
            if (data == null || data.times == null || data.times.Length == 0) return;
            var dir = System.IO.Path.Combine(Application.persistentDataPath, "ghosts");
            System.IO.Directory.CreateDirectory(dir);
            var path = System.IO.Path.Combine(dir, data.trackId + ".json");
            bool better = true;
            if (System.IO.File.Exists(path))
            {
                var existing = JsonUtility.FromJson<GhostData>(System.IO.File.ReadAllText(path));
                better = existing == null || data.lapTime < existing.lapTime;
            }
            if (better)
                System.IO.File.WriteAllText(path, JsonUtility.ToJson(data));
        }

        public static GhostData Load(string trackId)
        {
            var path = System.IO.Path.Combine(Application.persistentDataPath, "ghosts", trackId + ".json");
            if (!System.IO.File.Exists(path)) return null;
            try { return JsonUtility.FromJson<GhostData>(System.IO.File.ReadAllText(path)); }
            catch { return null; }
        }
    }

    /// <summary>Plays a GhostData as a translucent car. Attach to a simple car mesh.</summary>
    public class GhostPlayer : MonoBehaviour
    {
        private GhostData data;
        private float time;
        private int index;

        public void Play(GhostData ghost)
        {
            data = ghost;
            time = 0f; index = 0;
            enabled = data != null && data.times != null && data.times.Length > 1;
            var r = GetComponent<Renderer>();
            if (r != null)
            {
                foreach (var m in r.materials)
                {
                    m.SetFloat("_Surface", 1f); // transparent if URP Lit
                    m.SetOverrideTag("RenderType", "Transparent");
                    m.color = new Color(0f, 0.94f, 1f, 0.35f);
                }
            }
        }

        private void Update()
        {
            if (data == null || index >= data.times.Length - 1) return;
            time += Time.deltaTime;
            while (index < data.times.Length - 1 && data.times[index + 1] < time) index++;
            int next = Mathf.Min(index + 1, data.times.Length - 1);
            float t0 = data.times[index];
            float t1 = Mathf.Max(t0 + 0.0001f, data.times[next]);
            float k = Mathf.Clamp01((time - t0) / (t1 - t0));
            transform.SetPositionAndRotation(
                Vector3.Lerp(new Vector3(data.x[index], data.y[index], data.z[index]),
                             new Vector3(data.x[next], data.y[next], data.z[next]), k),
                Quaternion.Slerp(new Quaternion(data.qx[index], data.qy[index], data.qz[index], data.qw[index]),
                                 new Quaternion(data.qx[next], data.qy[next], data.qz[next], data.qw[next]), k));
        }
    }
}

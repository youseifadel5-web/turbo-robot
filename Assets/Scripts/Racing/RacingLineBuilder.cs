using UnityEngine;
using System.Collections.Generic;

namespace NeonRush.Racing
{
    /// <summary>
    /// Builds a dense racing line from TrackBuilder segments and answers
    /// "how far along the track is this position?" Used by AI, position
    /// ranking, wrong-way detection and the minimap.
    /// </summary>
    public class RacingLineBuilder : MonoBehaviour
    {
        public TrackBuilder trackBuilder;
        public float sampleSpacing = 10f;

        public List<Vector3> Points { get; private set; } = new List<Vector3>();
        public List<float> Cumulative { get; private set; } = new List<float>();
        public float TotalDistance { get; private set; }

        private int cacheIndex;

        public bool Build(TrackBuilder source)
        {
            trackBuilder = source;
            Points.Clear(); Cumulative.Clear(); TotalDistance = 0f;
            if (source == null || source.BuiltSegments == null || source.BuiltSegments.Count == 0)
                return false;

            var nodes = new List<Vector3>();
            foreach (var seg in source.BuiltSegments)
            {
                if (seg == null) continue;
                Vector3 p = seg.position;
                p.y = 0f;
                if (nodes.Count == 0 || (p - nodes[nodes.Count - 1]).sqrMagnitude > 1f)
                    nodes.Add(p);
            }
            if (nodes.Count < 2) return false;

            for (int i = 0; i < nodes.Count - 1; i++)
            {
                Vector3 a = nodes[i], b = nodes[i + 1];
                float d = Vector3.Distance(a, b);
                int steps = Mathf.Max(1, Mathf.CeilToInt(d / Mathf.Max(2f, sampleSpacing)));
                for (int s = 0; s < steps; s++)
                    Points.Add(Vector3.Lerp(a, b, (float)s / steps));
            }
            Points.Add(nodes[nodes.Count - 1]);

            Cumulative.Add(0f);
            for (int i = 1; i < Points.Count; i++)
            {
                TotalDistance += Vector3.Distance(Points[i - 1], Points[i]);
                Cumulative.Add(TotalDistance);
            }
            cacheIndex = 0;
            Debug.Log($"[RacingLine] {Points.Count} points, {TotalDistance:F0} m");
            return true;
        }

        public bool IsValid => Points != null && Points.Count > 1 && TotalDistance > 1f;

        public Transform[] CreateWaypointTransforms(Transform parent)
        {
            var list = new List<Transform>();
            var holder = new GameObject("AIWaypoints");
            holder.transform.SetParent(parent, false);
            for (int i = 0; i < Points.Count; i += Mathf.Max(1, Mathf.RoundToInt(sampleSpacing / 5f)))
            {
                var go = new GameObject("wp" + list.Count);
                go.transform.SetParent(holder.transform, false);
                go.transform.position = Points[i];
                list.Add(go.transform);
            }
            return list.ToArray();
        }

        /// <summary>Nearest point index (windowed search with wrap-around fallback).</summary>
        public int GetNearestIndex(Vector3 pos, int hint = -1)
        {
            if (!IsValid) return -1;
            int n = Points.Count;
            if (hint >= 0 && hint < n)
            {
                int best = hint; float bestSqr = float.MaxValue;
                for (int o = -12; o <= 12; o++)
                {
                    int i = (hint + o + n) % n;
                    float d = (Points[i] - pos).sqrMagnitude;
                    if (d < bestSqr) { bestSqr = d; best = i; }
                }
                cacheIndex = best;
                return best;
            }
            int bestFull = 0; float bestFullSqr = float.MaxValue;
            for (int i = 0; i < n; i++)
            {
                float d = (Points[i] - pos).sqrMagnitude;
                if (d < bestFullSqr) { bestFullSqr = d; bestFull = i; }
            }
            cacheIndex = bestFull;
            return bestFull;
        }

        public float GetProgress(Vector3 pos)
        {
            int i = GetNearestIndex(pos, cacheIndex);
            if (i < 0 || !IsValid) return 0f;
            return Mathf.Clamp01(Cumulative[i] / TotalDistance);
        }

        public Vector3 GetTangent(Vector3 pos)
        {
            int i = GetNearestIndex(pos, cacheIndex);
            if (i < 0) return Vector3.forward;
            int n = Points.Count;
            Vector3 a = Points[(i - 1 + n) % n];
            Vector3 b = Points[(i + 1) % n];
            Vector3 t = (b - a).normalized;
            t.y = 0f;
            return t.sqrMagnitude < 0.001f ? Vector3.forward : t;
        }
    }
}

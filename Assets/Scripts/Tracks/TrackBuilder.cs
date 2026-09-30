using UnityEngine;
using System.Collections.Generic;

namespace NeonRush.Tracks
{
    public enum SegmentType
    {
        Start,
        Straight,
        CurveLeft,
        CurveRight,
        Hairpin,
        Tunnel,
        Bridge,
        Jump,
        Checkpoint,
        Finish
    }

    /// <summary>
    /// Builds a simple modular route from segments toward a target distance.
    /// Validates basic connectivity. Replace segment prefabs with real art later.
    /// </summary>
    public class TrackBuilder : MonoBehaviour
    {
        [Header("Prefabs (optional — uses primitives if null)")]
        public GameObject straightPrefab;
        public GameObject curveLeftPrefab;
        public GameObject curveRightPrefab;
        public GameObject hairpinPrefab;
        public GameObject tunnelPrefab;
        public GameObject bridgePrefab;
        public GameObject jumpPrefab;
        public GameObject checkpointPrefab;
        public GameObject startPrefab;
        public GameObject finishPrefab;

        [Header("Layout")]
        public float segmentLength = 40f;
        public float roadWidth = 12f;
        public Transform root;

        public List<Transform> BuiltSegments { get; private set; } = new List<Transform>();
        public List<Transform> Checkpoints { get; private set; } = new List<Transform>();
        public Transform StartPoint { get; private set; }
        public Transform FinishPoint { get; private set; }
        public float BuiltDistanceMeters { get; private set; }

        public void BuildFromConfig(TrackConfig config)
        {
            Clear();
            if (root == null) root = transform;

            float targetMeters = config.GetTargetDistanceKm() * 1000f;
            var sequence = GenerateSequence(targetMeters, config.checkpointCount);

            Vector3 pos = Vector3.zero;
            Quaternion rot = Quaternion.identity;
            BuiltDistanceMeters = 0f;

            for (int i = 0; i < sequence.Count; i++)
            {
                var type = sequence[i];
                var seg = SpawnSegment(type, pos, rot);
                BuiltSegments.Add(seg);

                if (type == SegmentType.Start) StartPoint = seg;
                if (type == SegmentType.Finish) FinishPoint = seg;
                if (type == SegmentType.Checkpoint) Checkpoints.Add(seg);

                float advance = GetAdvance(type);
                BuiltDistanceMeters += advance;

                // Simple advance + turn
                if (type == SegmentType.CurveLeft)
                    rot *= Quaternion.Euler(0f, -25f, 0f);
                else if (type == SegmentType.CurveRight)
                    rot *= Quaternion.Euler(0f, 25f, 0f);
                else if (type == SegmentType.Hairpin)
                    rot *= Quaternion.Euler(0f, 140f, 0f);

                pos += rot * Vector3.forward * advance;
            }

            config.builtDistanceKm = BuiltDistanceMeters / 1000f;
            Debug.Log($"[TrackBuilder] Built {BuiltSegments.Count} segments, ~{config.builtDistanceKm:F2} km, CPs={Checkpoints.Count}");
        }

        private List<SegmentType> GenerateSequence(float targetMeters, int cpCount)
        {
            var list = new List<SegmentType>();
            list.Add(SegmentType.Start);

            float acc = segmentLength;
            int cpPlaced = 0;
            float nextCpAt = targetMeters / Mathf.Max(1, cpCount + 1);
            int safety = 0;

            while (acc < targetMeters - segmentLength * 1.5f && safety++ < 400)
            {
                // Weighted random but avoid impossible chains
                float r = Random.value;
                SegmentType t;
                if (r < 0.45f) t = SegmentType.Straight;
                else if (r < 0.62f) t = SegmentType.CurveLeft;
                else if (r < 0.79f) t = SegmentType.CurveRight;
                else if (r < 0.86f) t = SegmentType.Tunnel;
                else if (r < 0.92f) t = SegmentType.Bridge;
                else if (r < 0.96f) t = SegmentType.Jump;
                else t = SegmentType.Hairpin;

                // Prevent double hairpin
                if (t == SegmentType.Hairpin && list[list.Count - 1] == SegmentType.Hairpin)
                    t = SegmentType.Straight;

                list.Add(t);
                acc += GetAdvance(t);

                if (cpPlaced < cpCount && acc >= nextCpAt * (cpPlaced + 1))
                {
                    list.Add(SegmentType.Checkpoint);
                    cpPlaced++;
                }
            }

            list.Add(SegmentType.Finish);
            return list;
        }

        private float GetAdvance(SegmentType t)
        {
            switch (t)
            {
                case SegmentType.Hairpin: return segmentLength * 0.7f;
                case SegmentType.Jump: return segmentLength * 1.1f;
                case SegmentType.Checkpoint:
                case SegmentType.Start:
                case SegmentType.Finish: return segmentLength * 0.5f;
                default: return segmentLength;
            }
        }

        private Transform SpawnSegment(SegmentType type, Vector3 pos, Quaternion rot)
        {
            GameObject prefab = ResolvePrefab(type);
            GameObject go;
            if (prefab != null)
                go = Instantiate(prefab, pos, rot, root);
            else
            {
                go = GameObject.CreatePrimitive(PrimitiveType.Cube);
                go.name = type.ToString();
                go.transform.SetParent(root);
                go.transform.position = pos;
                go.transform.rotation = rot;
                go.transform.localScale = new Vector3(roadWidth, 0.3f, GetAdvance(type));
                // Neon strip color hint
                var r = go.GetComponent<Renderer>();
                if (r != null)
                {
                    r.material.color = type == SegmentType.Checkpoint ? new Color(0f, 1f, 0.8f) :
                        type == SegmentType.Finish ? new Color(1f, 0.2f, 0.6f) :
                        type == SegmentType.Start ? new Color(0.2f, 0.8f, 1f) :
                        new Color(0.12f, 0.12f, 0.14f);
                }
            }
            return go.transform;
        }

        private GameObject ResolvePrefab(SegmentType t)
        {
            switch (t)
            {
                case SegmentType.Straight: return straightPrefab;
                case SegmentType.CurveLeft: return curveLeftPrefab;
                case SegmentType.CurveRight: return curveRightPrefab;
                case SegmentType.Hairpin: return hairpinPrefab;
                case SegmentType.Tunnel: return tunnelPrefab;
                case SegmentType.Bridge: return bridgePrefab;
                case SegmentType.Jump: return jumpPrefab;
                case SegmentType.Checkpoint: return checkpointPrefab;
                case SegmentType.Start: return startPrefab;
                case SegmentType.Finish: return finishPrefab;
                default: return straightPrefab;
            }
        }

        public void Clear()
        {
            for (int i = BuiltSegments.Count - 1; i >= 0; i--)
            {
                if (BuiltSegments[i] != null)
                    Destroy(BuiltSegments[i].gameObject);
            }
            BuiltSegments.Clear();
            Checkpoints.Clear();
            StartPoint = null;
            FinishPoint = null;
            BuiltDistanceMeters = 0f;
        }
    }
}

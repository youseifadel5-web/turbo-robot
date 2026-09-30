using UnityEngine;
using NeonRush.Cars;

namespace NeonRush.AI
{
    public enum AIDifficulty { Easy, Normal, Hard, Expert }

    /// <summary>
    /// Waypoint racing AI: follows a line, brakes for corners, occasional overtakes,
    /// difficulty affects speed cap and mistake chance — not raw cheat speed.
    /// </summary>
    [RequireComponent(typeof(ArcadeCarController))]
    public class RacingAI : MonoBehaviour
    {
        public AIDifficulty difficulty = AIDifficulty.Normal;
        [Range(0f, 1f)] public float aggression = 0.5f;
        [Range(0f, 1f)] public float mistakeRate = 0.1f;

        [Header("Racing Line")]
        public Transform[] waypoints;
        public float waypointReach = 8f;
        public float lookAhead = 18f;

        [Header("Overtake")]
        public float overtakeCheckDistance = 12f;
        public LayerMask carMask = ~0;

        private ArcadeCarController car;
        private int wpIndex;
        private float mistakeTimer;
        private float mistakeSteer;
        private float nitroHold;

        private void Awake()
        {
            car = GetComponent<ArcadeCarController>();
        }

        public void SetDifficulty(AIDifficulty value)
        {
            difficulty = value;
            switch (value)
            {
                case AIDifficulty.Easy:
                    aggression = 0.25f; mistakeRate = 0.28f; break;
                case AIDifficulty.Normal:
                    aggression = 0.5f; mistakeRate = 0.14f; break;
                case AIDifficulty.Hard:
                    aggression = 0.75f; mistakeRate = 0.06f; break;
                case AIDifficulty.Expert:
                    aggression = 0.92f; mistakeRate = 0.02f; break;
            }
        }

        public void SetWaypoints(Transform[] line)
        {
            waypoints = line;
            wpIndex = 0;
        }

        private void FixedUpdate()
        {
            if (car == null || waypoints == null || waypoints.Length == 0) return;

            AdvanceWaypoint();
            Vector3 target = GetLookAheadPoint();
            Vector3 toTarget = target - transform.position;
            toTarget.y = 0f;

            float steer = Vector3.SignedAngle(transform.forward, toTarget.normalized, Vector3.up) / 35f;
            steer = Mathf.Clamp(steer, -1f, 1f);

            // Mistakes: brief wrong steer
            mistakeTimer -= Time.fixedDeltaTime;
            if (mistakeTimer <= 0f && Random.value < mistakeRate * Time.fixedDeltaTime * 8f)
            {
                mistakeSteer = Random.Range(-0.55f, 0.55f);
                mistakeTimer = Random.Range(0.2f, 0.6f);
            }
            if (mistakeTimer > 0f) steer = Mathf.Clamp(steer + mistakeSteer, -1f, 1f);

            float cornerFactor = 1f - Mathf.Clamp01(Mathf.Abs(steer));
            float throttle = Mathf.Lerp(0.35f, 1f, cornerFactor);
            float brake = Mathf.Abs(steer) > 0.7f && car.SpeedKmh > 90f ? 0.45f : 0f;

            // Skill cap: Easy never uses full throttle on straights for long
            float skillCap = difficulty == AIDifficulty.Easy ? 0.82f :
                             difficulty == AIDifficulty.Normal ? 0.92f : 1f;
            throttle *= skillCap;

            bool wantOvertake = aggression > 0.45f && ShouldOvertake();
            if (wantOvertake)
            {
                steer += (Random.value > 0.5f ? 0.25f : -0.25f) * aggression;
                throttle = Mathf.Max(throttle, 0.9f);
            }

            nitroHold -= Time.fixedDeltaTime;
            bool useNitro = false;
            if (nitroHold <= 0f && cornerFactor > 0.85f && car.SpeedKmh > 60f && aggression > 0.4f)
            {
                useNitro = car.NitroNormalized > 0.25f;
                if (useNitro) nitroHold = 1.4f;
            }

            bool drift = Mathf.Abs(steer) > 0.55f && car.SpeedKmh > 50f && aggression > 0.5f;
            car.SetInput(steer, throttle, brake, drift, useNitro);
        }

        private void AdvanceWaypoint()
        {
            var wp = waypoints[wpIndex];
            if (wp == null) { wpIndex = (wpIndex + 1) % waypoints.Length; return; }
            Vector3 flat = wp.position; flat.y = transform.position.y;
            if ((flat - transform.position).sqrMagnitude < waypointReach * waypointReach)
                wpIndex = (wpIndex + 1) % waypoints.Length;
        }

        private Vector3 GetLookAheadPoint()
        {
            int next = (wpIndex + 1) % waypoints.Length;
            Vector3 a = waypoints[wpIndex].position;
            Vector3 b = waypoints[next].position;
            Vector3 dir = (b - a).normalized;
            return a + dir * lookAhead;
        }

        private bool ShouldOvertake()
        {
            if (Physics.Raycast(transform.position + Vector3.up, transform.forward, out var hit, overtakeCheckDistance, carMask))
            {
                if (hit.collider.transform != transform && hit.collider.GetComponentInParent<ArcadeCarController>())
                    return true;
            }
            return false;
        }
    }
}

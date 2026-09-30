using UnityEngine;

namespace NeonRush.Cars
{
    /// <summary>
    /// Tracks drift score, combo, near-miss bonuses.
    /// Feeds HUD and race results.
    /// </summary>
    public class DriftScoreManager : MonoBehaviour
    {
        public ArcadeCarController car;
        public float nearMissRadius = 2.6f;
        public LayerMask nearMissMask = ~0;
        public float nearMissCooldown = 0.8f;

        public float TotalScore { get; private set; }
        public int MaxCombo { get; private set; }
        public int CurrentCombo { get; private set; }
        public float BestAngle { get; private set; }

        public System.Action<float, int> OnScoreChanged; // score, combo
        public System.Action<int> OnNearMiss;            // bonus points

        private float comboTimer;
        private float nearMissTimer;
        private readonly Collider[] overlap = new Collider[12];

        private void OnEnable()
        {
            if (car == null) car = GetComponentInParent<ArcadeCarController>();
            if (car != null) car.OnDriftScoreTick += HandleDriftTick;
        }

        private void OnDisable()
        {
            if (car != null) car.OnDriftScoreTick -= HandleDriftTick;
        }

        private void Update()
        {
            if (comboTimer > 0f)
            {
                comboTimer -= Time.deltaTime;
                if (comboTimer <= 0f) CurrentCombo = 0;
            }

            nearMissTimer -= Time.deltaTime;
            if (car != null && car.SpeedKmh > 40f && nearMissTimer <= 0f)
                CheckNearMiss();
        }

        private void HandleDriftTick(float tick)
        {
            float mult = 1f + CurrentCombo * 0.18f;
            float add = tick * mult * 10f;
            TotalScore += add;
            CurrentCombo = Mathf.Min(CurrentCombo + 1, 12);
            MaxCombo = Mathf.Max(MaxCombo, CurrentCombo);
            comboTimer = 1.25f;

            if (Mathf.Abs(car.DriftAngle) > BestAngle)
                BestAngle = Mathf.Abs(car.DriftAngle);

            OnScoreChanged?.Invoke(TotalScore, CurrentCombo);
        }

        private void CheckNearMiss()
        {
            int count = Physics.OverlapSphereNonAlloc(transform.position, nearMissRadius, overlap, nearMissMask);
            for (int i = 0; i < count; i++)
            {
                var c = overlap[i];
                if (c == null || c.transform.IsChildOf(transform) || c.transform == transform) continue;
                // Traffic or AI car
                if (c.CompareTag("Traffic") || c.CompareTag("AI") || c.GetComponentInParent<ArcadeCarController>() != null)
                {
                    int bonus = 350 + CurrentCombo * 40;
                    TotalScore += bonus;
                    nearMissTimer = nearMissCooldown;
                    OnNearMiss?.Invoke(bonus);
                    OnScoreChanged?.Invoke(TotalScore, CurrentCombo);
                    break;
                }
            }
        }

        public void ResetScore()
        {
            TotalScore = 0f;
            CurrentCombo = 0;
            MaxCombo = 0;
            BestAngle = 0f;
        }
    }
}

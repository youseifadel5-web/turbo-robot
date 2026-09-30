using UnityEngine;

namespace NeonRush.Racing
{
    public class RaceFinish : MonoBehaviour
    {
        public RaceManager raceManager;

        private void OnTriggerEnter(Collider other)
        {
            if (other.GetComponentInParent<Cars.ArcadeCarController>() != null)
            {
                if (raceManager != null) raceManager.EndRace();
                Debug.Log("[Race] FINISH!");
            }
        }
    }
}

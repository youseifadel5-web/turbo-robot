using UnityEngine;

namespace NeonRush.Racing
{
    public class CheckpointGate : MonoBehaviour
    {
        public int checkpointIndex;

        private void OnTriggerEnter(Collider other)
        {
            CheckpointSystem system = other.GetComponentInParent<CheckpointSystem>();
            if (system != null)
                system.PassCheckpoint(checkpointIndex);
        }
    }
}

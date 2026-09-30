using UnityEngine;

namespace NeonRush.Racing
{
    public class CheckpointSystem : MonoBehaviour
    {
        public int currentCheckpoint;
        public int totalCheckpoints;

        public void PassCheckpoint(int index)
        {
            if (index == currentCheckpoint + 1)
                currentCheckpoint = index;
        }

        public bool IsFinished() => totalCheckpoints > 0 && currentCheckpoint >= totalCheckpoints;
    }
}

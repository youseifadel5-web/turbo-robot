using UnityEngine;

namespace NeonRush.Cars
{
    public enum CameraMode
    {
        Chase,
        CloseChase,
        FarChase,
        Hood,
        Cockpit,
        Bumper
    }

    /// <summary>
    /// Multi-mode chase camera with light shake on nitro / collision / landing.
    /// </summary>
    public class ChaseCamera : MonoBehaviour
    {
        public Transform target;
        public CameraMode mode = CameraMode.Chase;

        [Header("Chase offsets")]
        public Vector3 chaseOffset = new Vector3(0f, 3.2f, -7.5f);
        public Vector3 closeOffset = new Vector3(0f, 2.4f, -5.2f);
        public Vector3 farOffset = new Vector3(0f, 4.5f, -11f);
        public Vector3 hoodOffset = new Vector3(0f, 1.1f, 0.8f);
        public Vector3 cockpitOffset = new Vector3(0f, 1.35f, 0.15f);
        public Vector3 bumperOffset = new Vector3(0f, 0.7f, -1.2f);

        public float positionSmooth = 8f;
        public float rotationSmooth = 8f;
        public float lookHeight = 1.1f;

        [Header("Shake")]
        public float shakeDecay = 6f;
        private float shakeStrength;
        private Vector3 shakeOffset;

        // Exposed so NitroVFX can punch
        public Vector3 offset
        {
            get => GetOffsetForMode();
            set { /* nitro punch writes via temporary — kept for compatibility */ }
        }

        private Vector3 runtimePunch;

        public void SetMode(CameraMode m) => mode = m;
        public void CycleMode()
        {
            mode = (CameraMode)(((int)mode + 1) % 6);
        }

        public void AddShake(float strength)
        {
            shakeStrength = Mathf.Max(shakeStrength, strength);
        }

        public void AddPunch(Vector3 localPunch)
        {
            runtimePunch = localPunch;
        }

        private void LateUpdate()
        {
            if (target == null) return;

            Vector3 baseOffset = GetOffsetForMode();
            Vector3 desired = target.TransformPoint(baseOffset + runtimePunch);

            // Shake
            if (shakeStrength > 0.001f)
            {
                shakeOffset = Random.insideUnitSphere * shakeStrength * 0.15f;
                shakeStrength = Mathf.Lerp(shakeStrength, 0f, 1f - Mathf.Exp(-shakeDecay * Time.deltaTime));
            }
            else shakeOffset = Vector3.zero;

            runtimePunch = Vector3.Lerp(runtimePunch, Vector3.zero, Time.deltaTime * 8f);

            float posSmooth = (mode == CameraMode.Cockpit || mode == CameraMode.Hood) ? 14f : positionSmooth;
            transform.position = Vector3.Lerp(transform.position, desired + shakeOffset,
                1f - Mathf.Exp(-posSmooth * Time.deltaTime));

            Vector3 lookPoint;
            if (mode == CameraMode.Cockpit || mode == CameraMode.Hood)
                lookPoint = target.position + target.forward * 25f + target.up * 0.3f;
            else
                lookPoint = target.position + target.up * lookHeight + target.forward * 8f;

            Quaternion desiredRot = Quaternion.LookRotation(lookPoint - transform.position, Vector3.up);
            float rotSmooth = (mode == CameraMode.Cockpit) ? 16f : rotationSmooth;
            transform.rotation = Quaternion.Slerp(transform.rotation, desiredRot,
                1f - Mathf.Exp(-rotSmooth * Time.deltaTime));
        }

        private Vector3 GetOffsetForMode()
        {
            switch (mode)
            {
                case CameraMode.CloseChase: return closeOffset;
                case CameraMode.FarChase: return farOffset;
                case CameraMode.Hood: return hoodOffset;
                case CameraMode.Cockpit: return cockpitOffset;
                case CameraMode.Bumper: return bumperOffset;
                default: return chaseOffset;
            }
        }
    }
}

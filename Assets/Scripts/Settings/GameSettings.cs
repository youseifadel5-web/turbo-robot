using UnityEngine;

namespace NeonRush.Settings
{
    public class GameSettings : MonoBehaviour
    {
        public bool vibration = true;
        public bool deviceMusicEnabled = true;
        public int targetFps = 60;
        public float masterVolume = 1f;

        public void Save()
        {
            PlayerPrefs.SetInt("vibration", vibration ? 1 : 0);
            PlayerPrefs.SetInt("deviceMusic", deviceMusicEnabled ? 1 : 0);
            PlayerPrefs.SetInt("targetFps", targetFps);
            PlayerPrefs.SetFloat("masterVolume", masterVolume);
            PlayerPrefs.Save();
        }

        public void Load()
        {
            vibration = PlayerPrefs.GetInt("vibration", 1) == 1;
            deviceMusicEnabled = PlayerPrefs.GetInt("deviceMusic", 1) == 1;
            targetFps = PlayerPrefs.GetInt("targetFps", 60);
            masterVolume = PlayerPrefs.GetFloat("masterVolume", 1f);
        }
    }
}

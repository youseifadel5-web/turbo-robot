using UnityEngine;
using NeonRush.Save;
using NeonRush.Racing;
using NeonRush.Tracks;

namespace NeonRush.Career
{
    public enum CareerEventType
    {
        Race, Drift, TimeTrial, Checkpoint, BossRace
    }

    [System.Serializable]
    public class CareerEvent
    {
        public string id;
        public string title;
        public CareerEventType type;
        public int chapter;
        public TrackLengthPreset length = TrackLengthPreset.Short;
        public int laps = 2;
        public int rewardCoins = 800;
        public int rewardXp = 40;
        public bool unlocked = true;
        public int starsEarned;
    }

    /// <summary>
    /// Simple offline career: chapters of events with 0-3 stars and rewards.
    /// </summary>
    public class CareerSystem : MonoBehaviour
    {
        public int chapter = 1;
        public int eventIndex = 1;
        public int stars;

        public CareerEvent[] chapter1 =
        {
            new CareerEvent { id="c1e1", title="Neon Warmup", type=CareerEventType.Race, chapter=1, laps=2, rewardCoins=600, rewardXp=30 },
            new CareerEvent { id="c1e2", title="First Drift", type=CareerEventType.Drift, chapter=1, laps=1, rewardCoins=700, rewardXp=35 },
            new CareerEvent { id="c1e3", title="Time Attack", type=CareerEventType.TimeTrial, chapter=1, laps=3, rewardCoins=900, rewardXp=45 },
            new CareerEvent { id="c1e4", title="Checkpoint Rush", type=CareerEventType.Checkpoint, chapter=1, laps=1, rewardCoins=850, rewardXp=40 },
            new CareerEvent { id="c1e5", title="Street Boss", type=CareerEventType.BossRace, chapter=1, laps=3, rewardCoins=1500, rewardXp=80 }
        };

        private void Start()
        {
            var data = SaveSystem.Load();
            chapter = data.careerChapter;
            eventIndex = data.careerEvent;
            stars = data.careerStars;
        }

        public CareerEvent GetCurrentEvent()
        {
            var list = CareerCatalog.GetEvents(chapter);
            int i = Mathf.Clamp(eventIndex - 1, 0, list.Length - 1);
            return list[i];
        }

        public void CompleteEvent(RaceResults results)
        {
            var ev = GetCurrentEvent();
            int earned = EvaluateStars(ev, results);
            ev.starsEarned = Mathf.Max(ev.starsEarned, earned);
            stars += earned;

            int coins = ev.rewardCoins + earned * 150;
            int xp = ev.rewardXp + earned * 10;
            SaveSystem.AddCoins(coins);
            SaveSystem.AddXp(xp);
            if (results != null) SaveSystem.RecordRace(results.totalTime, results.driftScore);

            eventIndex++;
            var chapterEvents = CareerCatalog.GetEvents(chapter);
            if (eventIndex > chapterEvents.Length)
            {
                if (chapter < CareerCatalog.TotalChapters)
                {
                    chapter++;
                    eventIndex = 1;
                }
                else
                {
                    eventIndex = chapterEvents.Length; // career complete: replay final boss
                }
            }

            var data = SaveSystem.Current;
            data.careerChapter = chapter;
            data.careerEvent = eventIndex;
            data.careerStars = stars;
            SaveSystem.Save(data);
        }

        public void CompleteEvent(int earnedStars)
        {
            stars += Mathf.Clamp(earnedStars, 0, 3);
            eventIndex++;
        }

        private int EvaluateStars(CareerEvent ev, RaceResults r)
        {
            if (r == null) return 1;
            int s = 1;
            if (r.position <= 3) s = 2;
            if (r.position == 1) s = 3;
            if (ev.type == CareerEventType.Drift && r.driftScore > 4000f) s = Mathf.Max(s, 3);
            return s;
        }
    }
}

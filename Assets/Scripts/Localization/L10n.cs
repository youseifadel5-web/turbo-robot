using System.Collections.Generic;
using UnityEngine;
using NeonRush.Save;

namespace NeonRush.Localization
{
    public enum Language { EN = 0, AR = 1 }

    /// <summary>
    /// Minimal string table for EN/AR. Architecture note: proper Arabic
    /// rendering (letter joining + RTL) requires TextMeshPro with an Arabic
    /// font asset — see Docs/V10_Notes.md. Values are stored and switchable
    /// now so the pipeline is ready.
    /// </summary>
    public static class L10n
    {
        public static Language Current
        {
            get => (Language)Mathf.Clamp(SaveSystem.Current.language, 0, 1);
            set { SaveSystem.Current.language = (int)value; SaveSystem.Save(); }
        }

        private static readonly Dictionary<string, string[]> Table = new Dictionary<string, string[]>
        {
            // key: [EN, AR]
            { "menu.play",     new[] { "PLAY",          "ابدأ السباق" } },
            { "menu.garage",   new[] { "GARAGE",       "الجراج" } },
            { "menu.events",   new[] { "EVENTS",       "الأحداث" } },
            { "menu.settings", new[] { "SETTINGS",     "الإعدادات" } },
            { "menu.music",    new[] { "DEVICE MUSIC", "موسيقى الجهاز" } },
            { "menu.coins",    new[] { "COINS",        "العملات" } },
            { "menu.level",    new[] { "LV",           "المستوى" } },
            { "garage.race",     new[] { "RACE",       "سباق" } },
            { "garage.customize", new[] { "CUSTOMIZE", "تخصيص" } },
            { "garage.back",     new[] { "BACK",       "رجوع" } },
            { "garage.owned",    new[] { "OWNED",     "مملوكة" } },
            { "garage.buy",      new[] { "BUY",       "شراء" } },
            { "garage.selected", new[] { "SELECTED",   "مختارة" } },
            { "garage.paint",   new[] { "PAINT",      "الطلاء" } },
            { "garage.rims",     new[] { "RIMS",       "الجنوط" } },
            { "garage.neon",     new[] { "NEON",       "النيون" } },
            { "settings.title",      new[] { "SETTINGS", "الإعدادات" } },
            { "settings.graphics",   new[] { "GRAPHICS",  "الرسوميات" } },
            { "settings.fps",        new[] { "FRAME RATE", "معدل الإطارات" } },
            { "settings.vibration",  new[] { "VIBRATION", "الاهتزاز" } },
            { "settings.controls",   new[] { "CONTROLS",  "التحكم" } },
            { "settings.sensitivity", new[] { "STEERING SENSITIVITY", "حساسية التوجيه" } },
            { "settings.language",   new[] { "LANGUAGE",  "اللغة" } },
            { "settings.master",     new[] { "MASTER VOLUME", "الصوت العام" } },
            { "settings.music",      new[] { "MUSIC VOLUME", "صوت الموسيقى" } },
            { "settings.sfx",         new[] { "SFX VOLUME", "المؤثرات" } },
            { "settings.engine",      new[] { "ENGINE VOLUME", "صوت المحرك" } },
            { "results.title",  new[] { "RACE COMPLETE", "انتهى السباق" } },
            { "results.retry",  new[] { "RETRY", "إعادة" } },
            { "results.menu",   new[] { "MENU", "القائمة" } },
            { "hud.lap",       new[] { "LAP", "لفة" } },
            { "hud.nitro",     new[] { "NITRO", "نيترو" } },
            { "hud.speed",     new[] { "KM/H", "كم/س" } }
        };

        public static string Get(string key)
        {
            if (Table.TryGetValue(key, out var v))
                return v[(int)Current];
            return key;
        }

        public static void Cycle()
        {
            Current = Current == Language.EN ? Language.AR : Language.EN;
        }
    }
}

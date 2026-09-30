using System;
using System.Collections.Generic;
using UnityEngine;
using NeonRush.Save;

namespace NeonRush.Services
{
    /// <summary>
    /// Service architecture for monetization + leaderboards.
    ///
    /// What is REAL here:
    ///   - LocalLeaderboard: per-track best times persisted in SaveSystem.
    ///   - Premium unlock flag persisted in SaveSystem.
    /// What is a HONEST STUB (needs SDK setup that cannot be done from source):
    ///   - AdService: add the Unity Ads / AdMob package, then replace
    ///     NoOpAdService with a real implementation of IAdService.
    ///   - IapService: add com.unity.purchasing + configure the $5.99
    ///     "premium_unlock" product in the IAP catalog, then implement
    ///     IIapService.PurchasePremium with the actual purchase flow.
    ///   - Online leaderboards: plug any backend (PlayFab/Unity Gaming
    ///     Services) behind ILeaderboardService — the game code never
    ///     changes.
    /// The offline game never calls the network: all No-Op by default.
    /// </summary>
    public static class GameServices
    {
        public static IAdService Ads { get; set; } = new NoOpAdService();
        public static IIapService Iap { get; set; } = new NoOpIapService();
        public static ILeaderboardService Leaderboards { get; set; } = new LocalLeaderboard();
    }

    public interface IAdService
    {
        bool IsAvailable { get; }
        /// <summary>Show a rewarded ad; callback receives true when the reward is earned.</summary>
        void ShowRewardedAd(Action<bool> onFinished);
    }

    public class NoOpAdService : IAdService
    {
        public bool IsAvailable => false;
        public void ShowRewardedAd(Action<bool> onFinished)
        {
            Debug.Log("[GameServices] Ad SDK not configured (NoOp). Replace via GameServices.Ads.");
            // Editor/dev behavior: simulate a completed ad so reward flows can be tested.
            onFinished?.Invoke(Application.isEditor);
        }
    }

    public interface IIapService
    {
        bool IsPremiumUnlocked { get; }
        /// <summary>Purchase the one-time premium unlock (~$5.99).</summary>
        void PurchasePremium(Action<bool> onFinished);
    }

    public class NoOpIapService : IIapService
    {
        public const string PremiumProductId = "premium_unlock_599";

        public bool IsPremiumUnlocked => SaveSystem.Current.premiumUnlocked;

        public void PurchasePremium(Action<bool> onFinished)
        {
            Debug.Log("[GameServices] IAP SDK not configured (NoOp). Product: " + PremiumProductId);
            // Editor/dev behavior: grant immediately so the UI flow can be tested.
            bool granted = Application.isEditor;
            if (granted)
            {
                SaveSystem.Current.premiumUnlocked = true;
                SaveSystem.Save();
            }
            onFinished?.Invoke(granted);
        }
    }

    public interface ILeaderboardService
    {
        void SubmitTime(string trackId, float seconds);
        List<TrackBest> GetTopTimes(string trackId, int max = 10);
    }

    /// <summary>REAL: local per-track best times, persisted. Swap for an online backend later.</summary>
    public class LocalLeaderboard : ILeaderboardService
    {
        public void SubmitTime(string trackId, float seconds)
        {
            if (string.IsNullOrEmpty(trackId) || seconds <= 0f) return;
            var data = SaveSystem.Current;
            var entry = data.trackBests.Find(b => b.trackId == trackId);
            if (entry == null)
            {
                data.trackBests.Add(new TrackBest { trackId = trackId, bestTime = seconds });
            }
            else if (seconds < entry.bestTime)
            {
                entry.bestTime = seconds;
            }
            SaveSystem.Save();
        }

        public List<TrackBest> GetTopTimes(string trackId, int max = 10)
        {
            var list = SaveSystem.Current.trackBests.FindAll(b => b.trackId == trackId);
            list.Sort((a, b) => a.bestTime.CompareTo(b.bestTime));
            return list;
        }
    }
}

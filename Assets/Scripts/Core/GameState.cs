using UnityEngine;

namespace NeonRush.Core
{
    public enum GameState { Boot, MainMenu, Garage, LoadingRace, Racing, Results, Paused }

    public class GameStateController : MonoBehaviour
    {
        public GameState CurrentState { get; private set; } = GameState.Boot;

        public void SetState(GameState state)
        {
            CurrentState = state;
            Debug.Log($"[GameState] {state}");
        }

        public void EnterMainMenu() => SetState(GameState.MainMenu);
        public void EnterGarage() => SetState(GameState.Garage);
        public void StartRace() => SetState(GameState.LoadingRace);
        public void PauseRace() => SetState(GameState.Paused);
    }
}

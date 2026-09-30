class_name CareerCatalog
## Port of NeonRush.Career.CareerCatalog — 3 chapters × 5 events.
## Event types: 0 Race, 1 Drift, 2 TimeTrial, 3 Checkpoint, 4 BossRace.

const TYPE_NAMES := ["RACE", "DRIFT", "TIME TRIAL", "CHECKPOINT", "BOSS RACE"]


static func get_events(chapter: int) -> Array:
	match clampi(chapter, 1, 3):
		1: return CHAPTER1
		2: return CHAPTER2
		3: return CHAPTER3
	return CHAPTER1


const CHAPTER1 := [
	{"id": "c1e1", "title": "Neon Warmup", "type": 0, "chapter": 1, "length": 0, "laps": 2, "rewardCoins": 600, "rewardXp": 30},
	{"id": "c1e2", "title": "First Drift", "type": 1, "chapter": 1, "length": 0, "laps": 1, "rewardCoins": 700, "rewardXp": 35},
	{"id": "c1e3", "title": "Time Attack", "type": 2, "chapter": 1, "length": 1, "laps": 3, "rewardCoins": 900, "rewardXp": 45},
	{"id": "c1e4", "title": "Checkpoint Rush", "type": 3, "chapter": 1, "length": 1, "laps": 1, "rewardCoins": 850, "rewardXp": 40},
	{"id": "c1e5", "title": "Street Boss", "type": 4, "chapter": 1, "length": 1, "laps": 3, "rewardCoins": 1500, "rewardXp": 80},
]

const CHAPTER2 := [
	{"id": "c2e1", "title": "Downtown Rush", "type": 0, "chapter": 2, "length": 1, "laps": 3, "rewardCoins": 1000, "rewardXp": 50},
	{"id": "c2e2", "title": "Slalom Master", "type": 1, "chapter": 2, "length": 1, "laps": 2, "rewardCoins": 1100, "rewardXp": 55},
	{"id": "c2e3", "title": "Clockwork", "type": 2, "chapter": 2, "length": 2, "laps": 2, "rewardCoins": 1300, "rewardXp": 65},
	{"id": "c2e4", "title": "Gate Runner", "type": 3, "chapter": 2, "length": 2, "laps": 1, "rewardCoins": 1250, "rewardXp": 60},
	{"id": "c2e5", "title": "Highway Boss", "type": 4, "chapter": 2, "length": 2, "laps": 3, "rewardCoins": 2100, "rewardXp": 110},
]

const CHAPTER3 := [
	{"id": "c3e1", "title": "Neon Grand Prix", "type": 0, "chapter": 3, "length": 2, "laps": 3, "rewardCoins": 1500, "rewardXp": 75},
	{"id": "c3e2", "title": "Total Slide", "type": 1, "chapter": 3, "length": 2, "laps": 2, "rewardCoins": 1600, "rewardXp": 80},
	{"id": "c3e3", "title": "Redline Trial", "type": 2, "chapter": 3, "length": 3, "laps": 2, "rewardCoins": 1900, "rewardXp": 95},
	{"id": "c3e4", "title": "Marathon Gates", "type": 3, "chapter": 3, "length": 3, "laps": 1, "rewardCoins": 1800, "rewardXp": 90},
	{"id": "c3e5", "title": "City Legend", "type": 4, "chapter": 3, "length": 3, "laps": 3, "rewardCoins": 3200, "rewardXp": 160},
]

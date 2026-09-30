class_name CarCatalog
## Port of NeonRush.Cars.CarCatalog — all 5 cars with identical physics values,
## plus paint / rim / neon customization catalogs.

const CLASSES := ["B", "A", "S"]

const CARS := [
	{
		"carId": "falcon_s", "displayName": "Falcon S", "carClass": 0, "priceCoins": 0,
		"defaultPaintHex": "0A0C10", "defaultNeonHex": "00F0FF",
		"topSpeedRating": 74.0, "accelerationRating": 68.0, "handlingRating": 72.0,
		"brakingRating": 70.0, "driftRating": 66.0, "nitroRating": 70.0,
		"maxSpeedKmh": 265.0, "acceleration": 26.0, "reverseAcceleration": 11.0,
		"brakePower": 36.0, "steeringPower": 78.0, "lateralGrip": 9.0,
		"driftGripMultiplier": 0.32, "mass": 1280.0,
		"nitroCapacity": 100.0, "nitroForce": 23.0, "nitroDrainPerSecond": 26.0, "nitroRechargePerSecond": 7.0,
	},
	{
		"carId": "vortex_gt", "displayName": "Vortex GT", "carClass": 1, "priceCoins": 32000,
		"defaultPaintHex": "12121A", "defaultNeonHex": "FF2BD6",
		"topSpeedRating": 82.0, "accelerationRating": 78.0, "handlingRating": 78.0,
		"brakingRating": 75.0, "driftRating": 74.0, "nitroRating": 76.0,
		"maxSpeedKmh": 295.0, "acceleration": 30.0, "reverseAcceleration": 12.0,
		"brakePower": 40.0, "steeringPower": 80.0, "lateralGrip": 10.0,
		"driftGripMultiplier": 0.30, "mass": 1320.0,
		"nitroCapacity": 110.0, "nitroForce": 26.0, "nitroDrainPerSecond": 25.0, "nitroRechargePerSecond": 8.0,
	},
	{
		"carId": "titan_x", "displayName": "Titan X", "carClass": 1, "priceCoins": 45000,
		"defaultPaintHex": "3A0B0B", "defaultNeonHex": "FF6A00",
		"topSpeedRating": 86.0, "accelerationRating": 88.0, "handlingRating": 60.0,
		"brakingRating": 62.0, "driftRating": 84.0, "nitroRating": 74.0,
		"maxSpeedKmh": 285.0, "acceleration": 34.0, "reverseAcceleration": 13.0,
		"brakePower": 34.0, "steeringPower": 72.0, "lateralGrip": 8.0,
		"driftGripMultiplier": 0.26, "mass": 1650.0,
		"nitroCapacity": 100.0, "nitroForce": 24.0, "nitroDrainPerSecond": 24.0, "nitroRechargePerSecond": 7.0,
	},
	{
		"carId": "aurora_r", "displayName": "Aurora R", "carClass": 2, "priceCoins": 90000,
		"defaultPaintHex": "E8E8F2", "defaultNeonHex": "B44CFF",
		"topSpeedRating": 95.0, "accelerationRating": 92.0, "handlingRating": 90.0,
		"brakingRating": 86.0, "driftRating": 80.0, "nitroRating": 92.0,
		"maxSpeedKmh": 330.0, "acceleration": 36.0, "reverseAcceleration": 14.0,
		"brakePower": 44.0, "steeringPower": 84.0, "lateralGrip": 11.0,
		"driftGripMultiplier": 0.34, "mass": 1240.0,
		"nitroCapacity": 130.0, "nitroForce": 30.0, "nitroDrainPerSecond": 24.0, "nitroRechargePerSecond": 9.0,
	},
	{
		"carId": "nomad_x", "displayName": "Nomad X", "carClass": 0, "priceCoins": 26000,
		"defaultPaintHex": "0E2418", "defaultNeonHex": "7CFF4D",
		"topSpeedRating": 66.0, "accelerationRating": 60.0, "handlingRating": 64.0,
		"brakingRating": 68.0, "driftRating": 58.0, "nitroRating": 62.0,
		"maxSpeedKmh": 235.0, "acceleration": 22.0, "reverseAcceleration": 12.0,
		"brakePower": 38.0, "steeringPower": 70.0, "lateralGrip": 8.5,
		"driftGripMultiplier": 0.30, "mass": 1900.0,
		"nitroCapacity": 90.0, "nitroForce": 21.0, "nitroDrainPerSecond": 28.0, "nitroRechargePerSecond": 6.0,
	},
]

const PAINTS := [
	["default", "Factory", "0A0C10"],
	["midnight", "Midnight", "101826"],
	["crimson", "Crimson", "8E1220"],
	["solar", "Solar Flare", "FF7A18"],
	["acid", "Acid Lime", "7CFF4D"],
	["aqua", "Aqua Shock", "00F0FF"],
	["violet", "Violet Haze", "B44CFF"],
	["magenta", "Hot Magenta", "FF2BD6"],
	["pearl", "Pearl White", "E8E8F2"],
	["graphite", "Graphite", "3A3F4A"],
	["sand", "Dune Sand", "C9B48A"],
	["teal", "Deep Teal", "0E5F63"],
]

const RIMS := [
	["stock", "Stock Silver", "9AA0AA"],
	["black", "Stealth Black", "15161A"],
	["gold", "Gold Rush", "E5B54A"],
	["chrome", "Chrome", "DCE4F0"],
	["neon", "Neon Core", "00F0FF"],
]

const NEON_COLORS := [
	["cyan", "Cyan", "00F0FF"],
	["magenta", "Magenta", "FF2BD6"],
	["green", "Toxic Green", "7CFF4D"],
	["orange", "Blaze Orange", "FF6A00"],
	["purple", "Ultraviolet", "B44CFF"],
	["red", "Redline", "FF2038"],
]


static func get_car(car_id: String) -> Dictionary:
	for c in CARS:
		if c["carId"] == car_id:
			return c
	return CARS[0]


static func car_class_letter(idx: int) -> String:
	return CLASSES[clampi(idx, 0, 2)]


static func hex_color(hex: String) -> Color:
	if hex.begins_with("#"):
		hex = hex.substr(1)
	if hex.length() == 6:
		var r := hex.substr(0, 2).to_int() / 255.0
		var g := hex.substr(2, 2).to_int() / 255.0
		var b := hex.substr(4, 2).to_int() / 255.0
		return Color(r, g, b)
	return Color.WHITE


static func paint_hex(paint_id: String) -> String:
	for p in PAINTS:
		if p[0] == paint_id:
			return p[2]
	return PAINTS[0][2]


static func rim_hex(rim_id: String) -> String:
	for r in RIMS:
		if r[0] == rim_id:
			return r[2]
	return RIMS[0][2]


static func neon_hex(neon_id: String) -> String:
	for n in NEON_COLORS:
		if n[0] == neon_id:
			return n[2]
	return NEON_COLORS[0][2]


## Applies saved upgrade levels onto the base stats (port of CarUpgradeSystem.ApplyToCar).
static func apply_upgrades(base: Dictionary, engine_l: int, turbo_l: int, brakes_l: int, nitro_l: int) -> Dictionary:
	var s := base.duplicate(true)
	s["acceleration"] = s["acceleration"] + engine_l * 1.15 + turbo_l * 0.7
	s["maxSpeedKmh"] = s["maxSpeedKmh"] + engine_l * 4.5
	s["brakePower"] = s["brakePower"] + brakes_l * 1.4
	s["lateralGrip"] = s["lateralGrip"] + 0.0
	s["nitroCapacity"] = s["nitroCapacity"] + nitro_l * 8.0
	s["nitroForce"] = s["nitroForce"] + nitro_l * 1.1 + turbo_l * 0.4
	s["mass"] = maxf(900.0, s["mass"])
	s["accelerationRating"] = clampf(s["accelerationRating"] + engine_l * 2.2 + turbo_l * 1.4, 0, 100)
	s["topSpeedRating"] = clampf(s["topSpeedRating"] + engine_l * 1.6, 0, 100)
	s["brakingRating"] = clampf(s["brakingRating"] + brakes_l * 2.4, 0, 100)
	s["handlingRating"] = clampf(s["handlingRating"], 0, 100)
	s["nitroRating"] = clampf(s["nitroRating"] + nitro_l * 2.5, 0, 100)
	return s


static func upgrade_cost(current_level: int) -> int:
	return 400 + current_level * 350

class_name PositionTracker
## Port of RacePositionTracker: laps + line progress → accurate positions.

var line: RacingLine = null
var player: RigidBody3D = null
var rivals: Array = []

var player_position := 1
var player_laps := 0
var player_total_progress := 0.0


class RacerState:
	var car: RigidBody3D
	var last_progress := 0.0
	var laps := 0
	var total_progress := 0.0
	var line_index := 0


var racers: Array[RacerState] = []


func setup(racing_line: RacingLine, player_car: RigidBody3D, ai_cars: Array) -> void:
	line = racing_line
	player = player_car
	rivals = ai_cars
	racers.clear()
	if player != null:
		var s := RacerState.new()
		s.car = player
		racers.append(s)
	for r in ai_cars:
		if r != null:
			var s := RacerState.new()
			s.car = r
			racers.append(s)


func update() -> void:
	if line == null or not line.is_valid():
		return
	for st in racers:
		if st.car == null:
			continue
		var res := line.query(st.car.global_position, st.line_index)
		st.line_index = res[1]
		var p: float = res[0]
		if st.last_progress > 0.85 and p < 0.15:
			st.laps += 1
		elif st.last_progress < 0.15 and p > 0.85 and st.laps > 0:
			st.laps -= 1
		st.last_progress = p
		st.total_progress = st.laps + p

	var player_state: RacerState = null
	for st in racers:
		if st.car == player:
			player_state = st
			break
	if player_state != null:
		player_laps = player_state.laps
		player_total_progress = player_state.total_progress
		var ahead := 0
		for st in racers:
			if st == player_state or st.car == null:
				continue
			if st.total_progress > player_state.total_progress:
				ahead += 1
		player_position = ahead + 1


func get_position_of(car: RigidBody3D) -> int:
	for st in racers:
		if st.car != car:
			continue
		var ahead := 0
		for other in racers:
			if other != st and other.car != null and other.total_progress > st.total_progress:
				ahead += 1
		return ahead + 1
	return 1

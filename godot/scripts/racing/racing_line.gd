class_name RacingLine
## Port of RacingLineBuilder: dense line from track segments; progress + tangent.

const SAMPLE_SPACING := 10.0

var points: PackedVector3Array = []
var cumulative: PackedFloat32Array = []
var total_distance := 0.0
var _cache_index := 0


func build(builder: TrackBuilder) -> bool:
	points = PackedVector3Array()
	cumulative = PackedFloat32Array()
	total_distance = 0.0
	if builder == null or builder.segments.is_empty():
		return false

	var nodes: Array[Vector3] = []
	for seg in builder.segments:
		var p := seg.position
		p.y = 0.0
		if nodes.is_empty() or p.distance_squared_to(nodes[nodes.size() - 1]) > 1.0:
			nodes.append(p)
	if nodes.size() < 2:
		return false

	for i in nodes.size() - 1:
		var a: Vector3 = nodes[i]
		var b: Vector3 = nodes[i + 1]
		var d := a.distance_to(b)
		var steps := maxi(1, int(ceil(d / maxf(2.0, SAMPLE_SPACING))))
		for s in steps:
			points.append(a.lerp(b, float(s) / float(steps)))
	points.append(nodes[nodes.size() - 1])

	cumulative.append(0.0)
	for i in range(1, points.size()):
		total_distance += points[i - 1].distance_to(points[i])
		cumulative.append(total_distance)
	_cache_index = 0
	return is_valid()


func is_valid() -> bool:
	return points.size() > 1 and total_distance > 1.0


func get_nearest_index(pos: Vector3, hint := -1) -> int:
	if not is_valid():
		return -1
	var n := points.size()
	if hint >= 0 and hint < n:
		var best := hint
		var best_sqr := 1e20
		for o in range(-12, 13):
			var i := (hint + o + n) % n
			var d := pos.distance_squared_to(points[i])
			if d < best_sqr:
				best_sqr = d
				best = i
		_cache_index = best
		return best
	var best_full := 0
	var best_full_sqr := 1e20
	for i in n:
		var d := pos.distance_squared_to(points[i])
		if d < best_full_sqr:
			best_full_sqr = d
			best_full = i
	_cache_index = best_full
	return best_full


func get_progress(pos: Vector3) -> float:
	var i := get_nearest_index(pos, _cache_index)
	if i < 0 or not is_valid():
		return 0.0
	return clampf(cumulative[i] / total_distance, 0.0, 1.0)


## Per-caller query (avoids the shared cache being overwritten by other
## racers querying the same line) — returns [progress, nearest_index].
func query(pos: Vector3, hint: int) -> Array:
	var i := get_nearest_index(pos, hint)
	if i < 0 or not is_valid():
		return [0.0, maxf(0, hint)]
	return [clampf(cumulative[i] / total_distance, 0.0, 1.0), i]


func get_tangent(pos: Vector3) -> Vector3:
	var i := get_nearest_index(pos, _cache_index)
	if i < 0:
		return Vector3.FORWARD
	var n := points.size()
	var a: Vector3 = points[(i - 1 + n) % n]
	var b: Vector3 = points[(i + 1) % n]
	var t := (b - a).normalized()
	t.y = 0.0
	return t if t.length_squared() > 0.001 else Vector3.FORWARD


## Waypoints for the AI (every ~5 m along the line).
func waypoints(step := 5.0) -> PackedVector3Array:
	var out: PackedVector3Array = []
	var stride := maxi(1, int(round(SAMPLE_SPACING / step)))
	for i in range(0, points.size(), stride):
		out.append(points[i])
	return out

extends Node
## TrafficManager port — pooled traffic cars cruising ahead of the player.
## Density presets: 0 low (8), 1 medium (16), 2 high (28).

var density := 1
var player: Node3D = null

var _pool: Array[Node3D] = []
var _active: Array = []   # [node, speed]

const MIN_SPEED := 12.0
const MAX_SPEED := 22.0
const DESPAWN_BEHIND := 80.0
const SPAWN_AHEAD := 140.0


func set_density(value: int) -> void:
	density = clampi(value, 0, 2)


func setup(player_node: Node3D, root: Node3D, density_value: int) -> void:
	player = player_node
	set_density(density_value)
	var pooled := 8 if density == 0 else (16 if density == 1 else 28)
	for i in pooled:
		var t := CarBuilder.build_traffic(i)
		t.visible = false
		root.add_child(t)
		_pool.append(t)


func _process(delta: float) -> void:
	if player == null:
		return
	var desired := mini(4 if density == 0 else (8 if density == 1 else 14), _pool.size())
	while _active.size() < desired:
		if not _spawn_ahead():
			break

	for i in range(_active.size() - 1, -1, -1):
		var entry: Array = _active[i]
		var node: Node3D = entry[0]
		var speed: float = entry[1]
		node.global_position += -node.global_transform.basis.z * speed * delta
		var along: float = (node.global_position - player.global_position).dot(-player.global_transform.basis.z)
		if along < -DESPAWN_BEHIND:
			node.visible = false
			_active.remove_at(i)


func _spawn_ahead() -> bool:
	var node: Node3D = null
	for p in _pool:
		if not p.visible:
			node = p
			break
	if node == null:
		return false
	var lane := randi_range(-1, 1)
	var fwd := -player.global_transform.basis.z
	var right := player.global_transform.basis.x
	var pos := player.global_position + fwd * randf_range(40.0, SPAWN_AHEAD)
	pos += right * (lane * 3.4)
	pos.y = player.global_position.y
	node.global_position = pos
	node.global_transform.basis = player.global_transform.basis
	node.visible = true
	_active.append([node, randf_range(MIN_SPEED, MAX_SPEED)])
	return true

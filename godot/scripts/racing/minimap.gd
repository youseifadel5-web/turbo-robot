extends Node
## Minimap — top-down SubViewport rendered into a TextureRect at reduced rate.

var target: Node3D = null
var image: TextureRect = null
const ORTHO_SIZE := 90.0
const UPDATE_RATE := 12.0

var _cam: Camera3D = null
var _accum := 0.0


func setup(follow_target: Node3D, texture_rect: TextureRect) -> void:
	target = follow_target
	image = texture_rect

	var vp := SubViewport.new()
	vp.size = Vector2i(256, 256)
	vp.render_target_update_mode = SubViewport.UPDATE_ONCE
	add_child(vp)

	_cam = Camera3D.new()
	_cam.projection = Camera3D.PROJECTION_ORTHOGONAL
	_cam.size = ORTHO_SIZE
	_cam.rotation_degrees = Vector3(-90, 0, 0)
	_cam.position = Vector3(0, 150, 0)
	_cam.near = 1.0
	_cam.far = 400.0
	_cam.cull_mask = 1  # world layer only
	vp.add_child(_cam)

	image.texture = vp.get_texture()


func _process(delta: float) -> void:
	_accum += delta
	if _accum < 1.0 / UPDATE_RATE:
		return
	_accum = 0.0
	if _cam == null or target == null:
		return
	_cam.position = Vector3(target.global_position.x, 150.0, target.global_position.z)
	var vp := _cam.get_parent() as SubViewport
	if vp:
		vp.render_target_update_mode = SubViewport.UPDATE_ONCE

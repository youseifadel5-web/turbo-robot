class_name DayNight
## Port of SkyLightingBootstrap + DayNightController: 4 time-of-day presets.
## 0 Night (default, neon showcase), 1 Sunset, 2 Dawn, 3 Day.

static func apply(env: WorldEnvironment, sun: DirectionalLight3D, preset: int, sky_path: String) -> void:
	var bg := env.environment
	if bg == null:
		bg = Environment.new()
		env.environment = bg

	var sky := Sky.new()
	if preset == 0 and ResourceLoader.exists(sky_path):
		var mat := PanoramaSkyMaterial.new()
		mat.panorama = load(sky_path)
		sky.sky_material = mat
	else:
		var proc := ProceduralSkyMaterial.new()
		match preset:
			1:  # sunset
				proc.sky_top_color = Color(0.18, 0.08, 0.22)
				proc.sky_horizon_color = Color(0.9, 0.4, 0.15)
				proc.ground_bottom_color = Color(0.08, 0.05, 0.07)
				proc.ground_horizon_color = Color(0.35, 0.15, 0.12)
			2:  # dawn
				proc.sky_top_color = Color(0.2, 0.25, 0.4)
				proc.sky_horizon_color = Color(0.85, 0.6, 0.4)
				proc.ground_bottom_color = Color(0.06, 0.06, 0.08)
				proc.ground_horizon_color = Color(0.4, 0.3, 0.25)
			3:  # day
				proc.sky_top_color = Color(0.35, 0.55, 0.85)
				proc.sky_horizon_color = Color(0.7, 0.8, 0.9)
				proc.ground_bottom_color = Color(0.15, 0.16, 0.18)
				proc.ground_horizon_color = Color(0.5, 0.55, 0.6)
		sky.sky_material = proc
	bg.sky = sky

	match preset:
		0:
			bg.background_mode = Environment.BG_SKY
			bg.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
			bg.ambient_light_color = Color(0.10, 0.12, 0.18)
			bg.ambient_light_energy = 1.2
			bg.fog_enabled = true
			bg.fog_light_color = Color(0.02, 0.03, 0.06)
			bg.fog_density = 0.004
			sun.rotation_degrees = Vector3(-35, 30, 0)
			sun.light_energy = 0.15
			sun.light_color = Color(0.5, 0.6, 1.0)
		1:
			bg.background_mode = Environment.BG_SKY
			bg.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
			bg.ambient_light_color = Color(0.35, 0.22, 0.2)
			bg.ambient_light_energy = 1.4
			bg.fog_enabled = true
			bg.fog_light_color = Color(0.25, 0.12, 0.1)
			bg.fog_density = 0.006
			sun.rotation_degrees = Vector3(-6, 30, 0)
			sun.light_energy = 1.4
			sun.light_color = Color(1.0, 0.55, 0.3)
		2:
			bg.background_mode = Environment.BG_SKY
			bg.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
			bg.ambient_light_color = Color(0.3, 0.32, 0.4)
			bg.ambient_light_energy = 1.3
			bg.fog_enabled = true
			bg.fog_light_color = Color(0.2, 0.22, 0.3)
			bg.fog_density = 0.005
			sun.rotation_degrees = Vector3(-12, 60, 0)
			sun.light_energy = 1.0
			sun.light_color = Color(1.0, 0.85, 0.7)
		3:
			bg.background_mode = Environment.BG_SKY
			bg.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
			bg.ambient_light_color = Color(0.55, 0.6, 0.68)
			bg.ambient_light_energy = 1.5
			bg.fog_enabled = false
			sun.rotation_degrees = Vector3(-55, 40, 0)
			sun.light_energy = 1.6
			sun.light_color = Color(1.0, 0.98, 0.92)

	# Mobile-friendly glow for the neon look
	bg.glow_enabled = preset != 3
	if bg.glow_enabled:
		bg.glow_intensity = 0.6
		bg.glow_bloom = 0.05
		bg.glow_hdr_threshold = 0.9

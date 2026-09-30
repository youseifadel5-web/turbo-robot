# Audio Architecture

Mixer groups:
Master
Music
SFX
Engine
Environment
UI

Engine layers:
Idle, Low RPM, Mid RPM, High RPM, Redline, Gear Shift, Turbo, Nitro.

Device Music:
Use Android MediaStore/SAF-compatible access where appropriate.
Do not copy the entire music library into the app.
Handle denied permissions gracefully.

Bluetooth:
Treat Bluetooth Gamepad input separately from Bluetooth Audio.
Prefer Unity Input System for supported Android controllers.
Do not force Android audio routing.

#!/usr/bin/env bash
# Copies the original assets (meshes, textures, audio) from the Unity project
# folder into the Godot project. Run once from the repo root, or let CI do it.
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/Assets"
DST="$ROOT/godot/assets"

mkdir -p "$DST/cars" "$DST/skybox" "$DST/vfx" "$DST/audio/engine" "$DST/audio/sfx" "$DST/audio/ambience"

cp -f "$SRC"/Art/Cars/*.obj "$DST/cars/"
cp -f "$SRC"/Art/Skybox/*.png "$DST/skybox/"
cp -f "$SRC"/Art/VFX/*.png "$DST/vfx/"

# Unity wav files are double-extension (.wav.wav) — normalize
for f in "$SRC"/Resources/Audio/Engine/*; do cp -f "$f" "$DST/audio/engine/$(basename "$f" .wav)"; done
for f in "$SRC"/Resources/Audio/SFX/*;    do cp -f "$f" "$DST/audio/sfx/$(basename "$f" .wav)"; done
for f in "$SRC"/Resources/Audio/Ambience/*; do cp -f "$f" "$DST/audio/ambience/$(basename "$f" .wav)"; done

echo "Assets copied to $DST"
ls "$DST/cars" | wc -l

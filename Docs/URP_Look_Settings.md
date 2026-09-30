# URP Look Settings — Neon Rush (copy into Inspector)

Target: wet night city, cyan/magenta bloom, mid-range Android safe.

## Pipeline asset
- Render Scale: Low 0.75 / Med 0.85 / High 1.0 / Ultra 1.0
- HDR: On (High/Ultra), Off (Low)
- MSAA: Off Low, 2x Med, 2x High, 4x Ultra
- Main Light Shadows: Off Low, On others, 1024 / 2048
- Additional Lights: Per Vertex Low, Per Pixel others, cap 2–4
- Soft Shadows: High/Ultra only

## Volume (Global)
Bloom
- Threshold 0.95
- Intensity 0.45 (Low 0.2, Ultra 0.7)
- Scatter 0.65
- Tint slightly cyan (0.85, 1, 1)

Color Adjustments
- Post Exposure -0.15
- Contrast 12
- Saturation 8
- Color Filter (0.92, 0.96, 1)

Vignette
- Intensity 0.22
- Smoothness 0.35
- Color near-black

Film Grain
- Off on Low, 0.12 on High

Motion Blur
- Off Low/Med
- Low quality High, Camera + 0.15 Ultra only

Ambient Occlusion
- Off Low, Medium 0.35 High

## Road material (URP Lit)
- Base Color #1A1C22
- Metallic 0.05
- Smoothness 0.86 (wet)
- Occlusion 1
- Optional wet mask in smoothness map

## Neon emissive
- Emission color #00F0FF or #FF2BD6
- Intensity 2.5–6 nits-equivalent (HDR)
- Do not flood every prop — strips and signs only

## Garage lights
- Key spot: intensity 4, color white-cyan, 35°
- Rim magenta strip, intensity 2
- Floor plane: same Lit, smoothness 0.9 + reflection probe

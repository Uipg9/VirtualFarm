# Virtual Farm — PocketUICore Test Mod

A feature-rich **virtual farming simulator** built as a Minecraft GUI mod to exercise every API in [PocketUICore](https://github.com/Uipg9/PocketUICore) v1.8.0. Runs on **Minecraft 1.21.11** (Fabric).

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen)
![Fabric](https://img.shields.io/badge/Fabric%20API-0.141.3-blue)
![Version](https://img.shields.io/badge/Version-2.0.0-gold)

## Features

### Core Gameplay
- **6 crop plots** — plant wheat, carrots, potatoes, beetroot, pumpkins, or melons
- **Seasonal weather cycle** — Spring, Summer (1.5× growth), Autumn, Winter (0.3× growth)
- **Gold economy** — earn gold from harvests, spend in the shop
- **Crop bank & export** — harvest to bank, export crops as real Minecraft items

### Shop System (3 pages, 9 items)
- **Consumables** — Fertilizer (+50%), Super Fertilizer (+100%), Lucky Water (3× speed)
- **Permanent Upgrades** — Auto-Water, Gold Magnet (+50% rewards)
- **Procedural Upgrades** — Soil Quality, Crop Quality, Lucky Harvest (each 5 levels)
- **Advanced** — Export crops, Prestige system
- **Interactive shop** — click descriptions or press 1/2/3 to buy, hover highlighting

### Quality of Life
- **Keyboard shortcuts** — P (Plant), W (Water), H (Harvest), S (Shop)
- **Bulk actions** — Shift+W (Water All), Shift+H (Harvest All)
- **Double-click replant** — double-click a ripe crop to harvest + auto-replant
- **Smart actions** — Enter/Space performs the best action for the selected plot
- **Arrow keys** — navigate the 3×2 plot grid
- **Number keys** — 1-6 select plots, Tab cycles buttons

### Prestige System
- Reset farm at 50 harvests for permanent +15% harvest bonus per level
- Two-click safety confirmation prevents accidental resets
- Starting gold increases with each prestige level

### PocketUICore APIs Exercised
- `DarkPanel`, `TextLabel`, `HoverButton`, `PercentageBar`
- `FocusManager` with `pushContext`/`popContext` for modal isolation
- `AnimationTicker` with `EasingType` presets (EASE_OUT_BACK, EASE_IN_OUT, etc.)
- `ObservableState<T>` for reactive gold/season/harvest tracking
- `ScreenShakeHelper` (light, medium, heavy haptics)
- `FloatingText` toast notifications
- `UISoundManager` presets (click, select, create, success, error, boundary, ready, celebration, gong)
- `ProceduralRenderer` — rounded rects, dividers, scaled text, color utilities
- `UIComponent.renderTooltip()` for 3-line hover tooltips

## Installation

### Requirements
- Minecraft 1.21.11
- Fabric Loader 0.18.4+
- Fabric API 0.141.3+1.21.11
- PocketUICore 1.8.0 (included in `libs/`)
- Java 21

### Build from Source
```bash
git clone https://github.com/Uipg9/VirtualFarm.git
cd VirtualFarm
./gradlew build
```
The built JAR will be in `build/libs/`.

### Play
1. Copy the mod JAR + PocketUICore JAR + Fabric API into your `.minecraft/mods/` folder
2. Launch Minecraft with Fabric
3. Press **G** in-game to open the Virtual Farm GUI

## Controls

| Key | Action |
|-----|--------|
| G | Open/close Virtual Farm |
| P | Plant crop in selected plot |
| W | Water selected plot |
| H | Harvest selected plot |
| S | Open shop |
| Shift+W | Water all plots |
| Shift+H | Harvest all plots |
| 1-6 | Select plot |
| Arrow keys | Navigate plot grid |
| Tab | Cycle focused button |
| Enter/Space | Smart action |
| Esc | Close shop/picker/screen |
| Double-click | Harvest + auto-replant |

## License

See [LICENSE](LICENSE).

## Author

Built by **Uipg9** as a comprehensive test mod for PocketUICore v1.8.0.

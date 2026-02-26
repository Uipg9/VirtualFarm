# Changelog

All notable changes to Virtual Farm are documented here.

## [2.0.0] — 2025-02-25

### Added — Major Feature Expansion
- **Shop system** — 3 pages with 9 buyable items (consumables, permanent upgrades, procedural upgrades)
- **Procedural upgrades** — Soil Quality, Crop Quality, Lucky Harvest (each up to level 5)
- **Prestige system** — reset farm at 50 harvests for permanent +15% harvest bonus
- **Prestige confirmation** — two-click safety prevents accidental resets (v7)
- **Crop bank & export** — harvested crops accumulate in a bank, exportable as real MC items
- **Seasonal cycle** — Spring, Summer (+50% growth), Autumn, Winter (-70% growth) with per-season durations
- **Offline growth catch-up** — crops grow while UI is closed, including season cycling
- **Keyboard shortcuts** — P/W/H/S for actions, Shift+W/H for bulk actions
- **Bulk actions** — Water All and Harvest All
- **Double-click replant** — double-click a ripe crop to harvest and auto-replant the same crop (v7)
- **Interactive shop** — click item descriptions or press 1/2/3 to buy, hover highlighting (v7)
- **Shop arrow key navigation** — Left/Right arrows change shop pages
- **Seasonal panel tint** — subtle color wash per season (green/gold/orange/blue)
- **Tabbed info panel** — Log / Stats / Guide tabs replace old settings section
- **Guide tab** — shows keyboard shortcuts and gameplay tips
- **Stats tab** — farm statistics, upgrade levels, crop bank count

### Changed
- Panel height reduced from 370 → 290 to fit all GUI scales
- HUD overlay removed (season/gold shown in panel instead)
- Activity log compacted (5 entries, 10px spacing)
- Stats row spacing optimized (10px)
- Build target: PocketUICore v1.8.0

### Fixed
- GUI overflowing screen at GUI scale 3 (1080p)
- Bank indicator text overlapping action buttons
- Gold displayed as misleading durability bar in HUD
- Double-click triggering in shop/picker mode

## [1.0.0] — Initial Release

### Added
- 6-plot crop farming with wheat, carrots, potatoes, beetroot, pumpkins, melons
- Gold economy with harvest rewards
- Fertilizer consumable
- Auto-Water and Gold Magnet permanent upgrades
- DarkPanel-based responsive UI
- Full keyboard navigation
- Activity log with color-coded entries
- FocusManager integration
- AnimationTicker animations
- Screen shake effects
- Sound feedback

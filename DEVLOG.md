# TestCoreMod — Development Process Documentation

> **Author:** Uipg9  
> **Date:** February 25, 2026  
> **Target:** Minecraft 1.21.11 (Fabric) — PocketUICore v1.6.0 UI framework test mod

---

## 1. Objective

Build a **virtual farm UI test mod** exercising the full PocketUICore v1.6.0 API. The screen features a crop-farming simulator with 6 plots, seasonal cycles, gold economy, and an in-GUI activity log. Tests: DarkPanel, TextLabel, HoverButton, PercentageBar, FocusManager, AnimationTicker, ObservableState, HudOverlayComponent, and ProceduralRenderer's full rendering API.

---

## 2. Research Phase — What Was Read

| Source | Key Takeaways |
|--------|---------------|
| `Core Info/POCKETUICORE_REFERENCE.md` | Full API surface for every component class, method signatures for UIComponent, HoverButton, TextLabel, VerticalListPanel, FocusManager, ProceduralRenderer |
| `MC_REF_1.21.11/00_OVERVIEW.md` | Verified exact dependency versions: yarn `1.21.11+build.4`, loader `0.18.4`, Fabric API `0.141.3+1.21.11`, Loom `1.15-SNAPSHOT`, Gradle 9.x |
| `MC_REF_1.21.11/02_API_CHANGES_1_21_11.md` | **Critical 1.21.11 breaking changes:** `mouseClicked(Click, boolean)` not `(double, double, int)`; `keyPressed(KeyInput)` not `(int, int, int)`; blur crash fix via `renderBackground` override |
| `MC_REF_1.21.11/03_SCREENS_AND_GUI.md` | Full Screen boilerplate, the blur-once-per-frame crash explanation, DrawContext methods |
| `MC_REF_1.21.11/04_EVENTS_AND_CALLBACKS.md` | Client command registration pattern via `ClientCommandRegistrationCallback.EVENT` |
| `MC_REF_1.21.11/CHEATSHEET.md` | Verified copy-paste patterns for Screen, input, rendering |
| `Depend/build.gradle` + `gradle.properties` | PocketUICore's own build setup — confirmed `com.pocketuicore` package, version scheme, Controlify compile-only dep |
| `Depend/build/libs/` | Confirmed `pocketuicore-1.5.0.jar` exists and is ready to use |

---

## 3. Decisions Made

### 3.1 — Client-Only Mod
PocketUICore is a UI framework. This test has zero server-side logic, so the mod is declared `"environment": "client"` in `fabric.mod.json` and has only a `client` entrypoint. No `ModInitializer` is needed.

### 3.2 — Local JAR Dependency (not Maven)
Rather than publishing PocketUICore to Maven Local and referencing coordinates, the JAR is placed directly in `libs/pocketuicore-1.5.0.jar` and loaded with `modImplementation files(...)`. This is simpler for a quick test mod and avoids the `publishToMavenLocal` step.

### 3.3 — Input Forwarding Strategy
The 1.21.11 Screen input methods use wrapper records (`Click`, `KeyInput`), but UIComponent's `mouseClicked()` still takes raw `(double, double, int)`. The screen unwraps the records and forwards the raw values down to the component tree. This is the same pattern PocketMenuScreen uses internally.

### 3.4 — FocusManager Lifecycle
`FocusManager` is a singleton. It must be `clear()`ed when the screen opens (in `init()`) and when it closes (in `removed()`), otherwise stale references from a previous screen could cause navigation to target dead components.

### 3.5 — Blur Crash Mitigation
MC 1.21.11's `Screen.renderBackground()` calls `applyBlur()` which throws if invoked twice per frame. Our override replaces it with `ProceduralRenderer.drawFullScreenOverlay()` — a single `fill()` call with `COL_OVERLAY` (`0xC0000000`), matching PocketUICore's visual style.

### 3.6 — Sound Playback
`SoundEvents.UI_BUTTON_CLICK` is a `RegistryEntry<SoundEvent>` in 1.21.11 (not a raw `SoundEvent`). We call `.value()` to unwrap it for `player.playSound()`.

---

## 4. File Structure Produced

```
TestCoreMod/
├── build.gradle                         # Fabric Loom + local JAR dep
├── gradle.properties                    # MC 1.21.11 versions
├── settings.gradle                      # Fabric Maven repos
├── LICENSE                              # MIT
├── DEVLOG.md                            # ← This file
│
├── libs/
│   └── pocketuicore-1.6.0.jar           # PocketUICore dependency (fixed identifier bug)
│
└── src/main/
    ├── java/com/testcoremod/
    │   ├── SampleModClient.java          # ClientModInitializer + /uisample command
    │   └── gui/
    │       └── SampleMenuScreen.java     # Virtual Farm screen (abs-positioned components)
    │
    └── resources/
        ├── fabric.mod.json               # Mod descriptor (client-only)
        └── assets/testcoremod/
            └── lang/en_us.json           # English translations
```

---

## 5. How It Works (A → B Walkthrough)

### Step 1: Player types `/uisample` in chat
`ClientCommandRegistrationCallback` fires the registered handler in `SampleModClient`.

### Step 2: Command schedules screen open on render thread
`MinecraftClient.getInstance().execute(() -> setScreen(new SampleMenuScreen()))` ensures GL context safety.

### Step 3: `SampleMenuScreen.init()` builds the component tree
- Creates a `VerticalListPanel` centered on screen
- Adds a `TextLabel` ("PocketUICore Framework Test")
- Adds a `HoverButton` ("Click Me!") with:
  - `onClick` → `System.out.println("Button Clicked!")` + vanilla click sound
  - `.setTooltip("Testing procedural tooltips!")`
- Registers components with `FocusManager` for gamepad/keyboard nav
- Calls `fm.focusFirst()` so the first component is auto-selected

### Step 4: Every frame — `render()` fires
1. `renderBackground()` → `ProceduralRenderer.drawFullScreenOverlay()` (dark overlay, no blur crash)
2. `mainPanel.render()` → renders the panel + all children (TextLabel drawn, HoverButton with hover animation)
3. `super.render()` → renders any vanilla widgets (none in this case, but keeps compatibility)
4. `UIComponent.renderTooltip()` → if hovering over the button for 0.5s, draws the tooltip

### Step 5: Input flows
- **Mouse click** → `mouseClicked(Click, boolean)` → unwraps → `mainPanel.mouseClicked(x, y, button)` → bubbles to HoverButton → fires onClick
- **Keyboard** → `keyPressed(KeyInput)` → Tab/arrows → FocusManager navigation; Enter/Space → activates focused; Escape → close
- **Mouse scroll** → forwarded to `mainPanel.mouseScrolled()` (VerticalListPanel inherits DarkPanel scroll)
- **Mouse release** → forwarded for any drag-end handling

### Step 6: Screen closed
`removed()` → `FocusManager.getInstance().clear()` → cleans up all references.

---

## 6. Ease Factors (What Made This Simple)

| Factor | Why It Helped |
|--------|---------------|
| **PocketUICore's auto-layout** | `VerticalListPanel` handles all child positioning automatically — no manual X/Y math for each component |
| **FocusManager singleton** | One-liner registration, instant keyboard/gamepad nav with zero configuration |
| **ProceduralRenderer's palette constants** | No need to pick colors — `COL_OVERLAY`, `COL_BG_SURFACE`, etc. are already dark-mode perfected |
| **Component tooltip API** | `.setTooltip(String)` on any UIComponent — tooltip rendering is handled by `UIComponent.renderTooltip()` |
| **HoverButton encapsulation** | Hover animation, focus ring, click sound zone — all baked in. Just provide label + Runnable |
| **Verified MC reference docs** | The `MC_REF_1.21.11/` folder eliminated hours of API guesswork — every method signature was battle-tested |

---

## 7. Potential Improvements

| Improvement | Description |
|-------------|-------------|
| **Add a PercentageBar** | Include a `PercentageBar` bound to an `ObservableState<Float>` with a slider button to test reactive data binding |
| **Test GridPanel layout** | Add a `GridPanel` with multiple HoverButtons to validate grid auto-layout and focus navigation across a 2D grid |
| **AnimationTicker demo** | Start a named animation on button click (e.g., slide-in panel) to test the easing engine |
| **HUD overlay test** | Register a `HudOverlayComponent` to verify in-game HUD rendering outside of Screen context |
| **ObservableState binding** | Create a reactive label that updates when state changes, proving the `map()` + `bindText()` pipeline |
| **Controller stick scrolling** | Add enough items to overflow the VerticalListPanel and set `ControllerHandler.setScrollTarget()` to test analog scrolling |
| **DarkPanel scroll test** | Enable `setScrollable(true)` on e panel with many children to test scissor-clipped scrolling |
| **Multi-screen navigation** | Add a second screen reachable from a button press, testing FocusManager cleanup/re-init across screen transitions |
| **CachedShape optimization** | Bake the panel background with `ProceduralRenderer.bakePanel()` in `init()` and render the cached shape each frame to test VBO-style caching |

---

## 8. Build & Run

```powershell
# Set environment
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.7.6-hotspot"
$env:GRADLE_OPTS = "-Djava.net.preferIPv4Stack=true"

# Navigate to project
cd "C:\Users\baesp\Downloads\Minecraft stuff\TestCoreMod"

# Generate Gradle wrapper (requires Gradle 9.x installed globally)
gradle wrapper --gradle-version 9.3.1

# Build
.\gradlew build --no-daemon

# Run client
.\gradlew runClient --no-daemon
```

Once in-game, open chat and type `/uisample` to open the test screen.

---

## 9. Version Compatibility Matrix

| Component | Version | Notes |
|-----------|---------|-------|
| Minecraft | 1.21.11 | "Mounts of Mayhem" |
| Fabric Loader | 0.18.4 | |
| Fabric API | 0.141.3+1.21.11 | |
| Fabric Loom | 1.15-SNAPSHOT | Requires Gradle 9.x |
| Gradle | 9.3.1 | Via wrapper |
| Java | 21 | OpenJDK 21 (Microsoft build tested) |
| PocketUICore | 1.6.0 | Local JAR in `libs/` (upgraded from 1.5.0, see §10) |
| Yarn Mappings | 1.21.11+build.4 | |

---

## 10. PocketUICore v1.5.0 → v1.6.0 Upgrade (Runtime Fix)

### Problem
Both `pocketuicore-1.5.0.jar` and the initial `pocketuicore-1.6.0.jar` contained a **fatal identifier bug** in the economy subsystem:

```
java.lang.ExceptionInInitializerError
  at com.pocketuicore.PocketUICore.onInitialize(PocketUICore.java:28)
Caused by: net.minecraft.util.InvalidIdentifierException:
  Non [a-z0-9/._-] character in path of location: minecraft:pocketuicore:sync_balance
  at com.pocketuicore.economy.SyncBalancePayload.<clinit>(SyncBalancePayload.java:25)
```

### Root Cause
`SyncBalancePayload.java` and `SyncEstatePayload.java` both used:

```java
CustomPayload.id("pocketuicore:sync_balance")  // BUG
```

In MC 1.21.11, `CustomPayload.id(String)` calls `Identifier.ofVanilla()`, which forces the `minecraft:` namespace and treats the entire argument as the **path**. The colon inside `pocketuicore:sync_balance` is not a valid path character (only `[a-z0-9/._-]` allowed).

### Fix Applied (in `Depend/` source)
Changed both payload files to use explicit namespace + path:

```java
// SyncBalancePayload.java
public static final CustomPayload.Id<SyncBalancePayload> ID =
        new CustomPayload.Id<>(Identifier.of("pocketuicore", "sync_balance"));

// SyncEstatePayload.java  
public static final CustomPayload.Id<SyncEstatePayload> ID =
        new CustomPayload.Id<>(Identifier.of("pocketuicore", "sync_estate"));
```

This creates `pocketuicore:sync_balance` as a proper Identifier (namespace=`pocketuicore`, path=`sync_balance`).

### Resolution
1. Fixed both files in `Depend/src/main/java/com/pocketuicore/economy/`
2. Rebuilt PocketUICore: `gradlew build --no-daemon`
3. Copied fixed `pocketuicore-1.6.0.jar` to `TestCoreMod/libs/`
4. Updated `build.gradle` to reference v1.6.0
5. Rebuilt TestCoreMod → **runClient launched successfully**, both mods loaded

### Compilation Errors Fixed Earlier (v1.5.0 build phase)
| Error | Cause | Fix |
|-------|-------|-----|
| `VerticalListPanel` constructor mismatch | Reference doc showed 5 params, actual JAR needs 6 (x, y, w, h, **padding**, spacing) | Added `8` as padding param |
| `mouseReleased` wrong signature | 1.21.11 uses `Click` record, not `(double, double, int)` | Changed to `mouseReleased(Click click)` |
| `@Override` on non-existent method | Consequence of wrong mouseReleased signature | Fixed with correct signature |

---

## 11. v3 Redesign — Absolute Positioning & Activity Log Fix

### Problem Report
User reported three critical issues with the v2 virtual farm screen:
1. **"Buttons aren't on the background screen, they're all off of it"** — all interactive elements rendered at the wrong screen position
2. **"I don't get any info when doing anything in the GUI"** — clicking buttons produced no visible feedback
3. **"Very ugly, nothing of value to look at"** — layout lacked visual polish and structure

### Root Cause Analysis

#### Bug 1 — Buttons off-screen
**PocketUICore's container components (`DarkPanel`, `VerticalListPanel`, `GridPanel`) do NOT auto-offset children relative to parent.** When a child is added with `new TextLabel(0, 0, w, h, ...)`, it renders at absolute screen position `(0, 0)` — the top-left corner of the window — regardless of where the parent panel is centered. The panel background was correctly centered at `(px, py)`, but every child rendered at `(0, 0)`.

This means children must use **absolute screen coordinates** calculated from the parent's position:
```java
// WRONG — renders at screen (0, 0)
new TextLabel(0, 0, w, h, ...)

// CORRECT — renders inside the panel at (px+10, py+8)
new TextLabel(px + 10, py + 8, w, h, ...)
```

#### Bug 2 — No feedback
Minecraft **hides the HUD and chat overlay when a Screen is open**. The code was calling `player.sendMessage(text, true)` (action bar) and `player.sendMessage(text, false)` (chat), but both are invisible behind the screen's fullscreen overlay. Action bar messages only appear after the screen closes.

#### Bug 3 — Ugly design
The previous version used `VerticalListPanel` / `GridPanel` for auto-layout (which didn't work), resulting in all elements stacking at `(0, 0)`. No `ProceduralRenderer` decorations (dividers, gradients, rounded rects) were applied.

### Solution Applied

#### Absolute positioning
All components now use coordinates calculated from the panel's `(px, py)` origin:
- `px = (screenWidth - 320) / 2`
- `py = (screenHeight - 280) / 2`
- Every child: `new Component(px + PAD + offset, py + rowDY, ...)`

Panel layout grid:
```
py + 8:    Title "☘ Virtual Farm ☘"
py + 24:   Stats row (gold / season / harvest count)
py + 42:   ═══ Divider ═══
py + 46:   Plot grid row 1 (3 cells, 96×44 each)
py + 94:   Plot grid row 2 (3 cells)
py + 142:  ═══ Divider ═══
py + 148:  Selected plot info
py + 162:  P1-P6 selector buttons
py + 180:  Plant / Water / Harvest / Close buttons
py + 202:  ═══ Divider ═══
py + 208:  Activity Log header
py + 222:  4 log entries (newest at bottom, chat-like)
py + 280:  Panel bottom
```

#### In-GUI activity log
Added a 4-line scrolling log rendered directly with `ProceduralRenderer.drawScaledText()` in the `render()` method. Every action (plant, water, harvest, errors, season changes, milestones, water evaporation) writes a color-coded entry:

| Color | Meaning |
|-------|---------|
| Green `0xFF238636` | Planted successfully |
| Blue `0xFF58A6FF` | Watered / water evaporated |
| Gold `0xFFD29922` | Crop ready / harvested |
| Red `0xFFFF6B6B` | Error (no gold, already planted, etc.) |
| Yellow `0xFFE3B341` | Warning (already watered, not ready) |
| Purple `0xFFBB86FC` | Milestone (every 5 harvests) |

#### Visual polish
- `ProceduralRenderer.drawDivider()` — 3 horizontal dividers separating sections
- `DarkPanel` stats background bar — subtle darker bg behind stats row
- Per-cell `DarkPanel` backgrounds with dynamic colors (empty → planted → ready)
- Selected cell highlighted with green/gold border
- Welcome log messages on screen open

### ProceduralRenderer API Reference (from javap)
Full API surface verified via `javap -public`:
```
fillRect(DrawContext, x, y, w, h, color)
fillRoundedRect(DrawContext, x, y, w, h, radius, color)
fillRoundedRectWithBorder(DrawContext, x, y, w, h, radius, fillColor, borderColor)
drawBorder(DrawContext, x, y, w, h, color)
drawRoundedBorder(DrawContext, x, y, w, h, radius, color)
drawDivider(DrawContext, x, y, w, color)
fillGradientV(DrawContext, x, y, w, h, topColor, bottomColor)
fillGradientH(DrawContext, x, y, w, h, leftColor, rightColor)
drawDropShadow(DrawContext, x, y, w, h, size, color, radius)
drawDropShadow(DrawContext, x, y, w, h, size)
drawText(DrawContext, TextRenderer, text, x, y, color)
drawCenteredText(DrawContext, TextRenderer, text, centerX, y, color)
drawScaledText(DrawContext, TextRenderer, text, x, y, color, scale)
drawScaledCenteredText(DrawContext, TextRenderer, text, centerX, y, color, scale)
drawFullScreenOverlay(DrawContext, w, h, color)
withAlpha(color, alpha) → int
lerpColor(from, to, t) → int
hex(String) → int

Color constants: COL_BG_PRIMARY, COL_BG_SURFACE, COL_BG_ELEVATED,
  COL_ACCENT, COL_ACCENT_TEAL, COL_SUCCESS, COL_WARNING, COL_ERROR,
  COL_TEXT_PRIMARY, COL_TEXT_MUTED, COL_BORDER, COL_HOVER, COL_OVERLAY,
  COL_SHADOW_BASE
```

### Lesson Learned
> **PocketUICore uses absolute screen coordinates, NOT relative-to-parent.**
> Every child component's `(x, y)` is where it renders on the actual screen.
> Container components (`DarkPanel`, `VerticalListPanel`) only draw their own background at their own `(x, y)` — they do NOT translate children.
> This differs from most UI frameworks and must be accounted for in every layout.

---

## 12. Virtual Farm v4 — Interactive UX Overhaul

### What Changed

v4 transforms the farm from a functional demo into a genuinely **interactive, game-feel experience**. Every user interaction now produces multi-layered audio-visual feedback.

#### 1. Direct Plot Click Selection
- **Removed** the P1-P6 selector buttons entirely.
- Plots are selected by **clicking directly on the cell**. `mouseClicked()` iterates `plotCells[i].isHovered(click.x(), click.y())` to detect which cell was tapped.
- **Hover glow**: Non-selected cells gain a subtle `0x18FFFFFF` overlay when the cursor is over them (rendered per-frame in `render()`).
- **Selection pulse**: `AnimationTicker.start("select_pulse", ...)` with `EASE_OUT` draws an expanding green glow around the newly selected cell.
- Number keys 1-6 still work as a shortcut.

#### 2. Crop Picker Mode
Instead of planting a random crop, clicking **Plant ▼** opens a **crop picker** — 6 color-coded buttons + a Cancel button that replace the action row:

| Crop     | Symbol | Cost | Reward   | Speed    |
|----------|--------|------|----------|----------|
| Wheat    | ▒      | 3g   | 6-14g    | Fast     |
| Carrot   | ◆      | 4g   | 8-16g    | Fast     |
| Potato   | ●      | 4g   | 8-18g    | Medium   |
| Beetroot | ♦      | 6g   | 12-22g   | Medium   |
| Melon    | ■      | 8g   | 16-28g   | Slow     |
| Pumpkin  | ▲      | 10g  | 20-35g   | Slow     |

This creates **strategic decision-making**: cheap crops grow fast with small returns; expensive crops are high-risk, high-reward.

##### Implementation — `setCropPickerMode(boolean)`
Uses `setVisible(true/false)` and `setEnabled(true/false)` on both action buttons and crop picker buttons to toggle modes. `FocusManager.clear()` + `fm.register(...)` is called each time to rebuild the keyboard-navigable set. The selected-info TextLabel changes to "Choose a crop to plant:" during picker mode.

#### 3. Prominent Balance Display
- Gold amount is rendered at `0.9f` scale (larger than other stats) in the left third of the stats bar: `⭐ 50 Gold`.
- **Gold flash**: `AnimationTicker.start("gold_flash", ...)` triggers a gold-tinted `fillRoundedRect` overlay on the balance bar whenever gold changes.
- **Floating popup**: A "+Xg" or "-Xg" text floats upward from the balance and fades out over 900ms using `AnimationTicker("gold_popup")`. Green/gold for earnings, red for spending.

#### 4. Sound Design
All sounds use `SoundEvents.UI_BUTTON_CLICK.value()` with **varied pitch × volume** to create distinct audio profiles:

| Action   | Pitch(es) | Volume | Feel                  |
|----------|-----------|--------|-----------------------|
| Select   | 1.0       | 0.25   | Soft click            |
| Plant    | 1.4 + 1.8 | 0.5/0.3 | Double-click sprout  |
| Water    | 0.5 + 0.7 | 0.5/0.3 | Deep watery thud     |
| Harvest  | 1.5+1.8+2.0 | 0.6/0.4/0.3 | Celebratory cascade |
| Error    | 0.3       | 0.5    | Low warning thud      |
| Season   | 0.4 + 0.6 | 0.6/0.4 | Deep gong            |
| Ready    | 1.6 + 2.0 | 0.4/0.2 | Bright ding          |
| Focus    | 1.0       | 0.15   | Nearly-silent tick    |

Using one SoundEvent at different pitches is guaranteed safe — no risk of missing RegistryEntry names.

#### 5. Screen Shake (Visual Haptic Feedback)
Since **PocketUICore's `ControllerHandler` does not expose rumble/vibration API** (confirmed via javap — only `enable()`, `disable()`, `setScrollTarget()`, `isActive()`, `tick()`), screen shake provides visual haptic feedback as a substitute.

```java
private void triggerHaptic(float intensity) {
    AnimationTicker.getInstance().start("shake", intensity, 0f, 250, EasingType.EASE_OUT);
}
```

In `render()`, shake offsets are computed from `System.currentTimeMillis()` sinusoidal oscillation × animation intensity, then applied via `context.getMatrices().pushMatrix()` / `translate(shakeX, shakeY)` / `popMatrix()`. Mouse coordinates are adjusted (`adjMX = mouseX - shakeX`) so hover detection remains accurate during shake.

Shake intensity varies by action:
- Select: 0.8 (subtle tap)
- Plant/Water: 1.5-2.0 (medium)
- Harvest: 4.0 (strong)
- Milestone: 6.0 (extra strong)
- Error: 1.0-1.5 (warning buzz)

#### 6. Visual Effects
- **Cell action flash**: `AnimationTicker("cell_flash_N")` — brief color overlay on a cell after plant/water/harvest. Color varies: green for plant, blue for water, gold for harvest.
- **Ready-to-harvest sparkle**: Animated `✦` and `★` characters orbit ready cells using `tickCounter + delta` sinusoidal waves with per-cell phase offsets.
- **Watered tint**: Blue `0x12589EFF` overlay and a `☂` icon rendered on watered cells.
- **Season flash**: Full-screen tint in the season's color (`AnimationTicker("season_flash")`).
- **Mode-switch transition**: `AnimationTicker("mode_switch")` provides a smooth crossfade feel when toggling between action buttons and crop picker.
- **Crop-ready notification pulse**: `AnimationTicker("ready_N")` with `EASE_OUT_BACK` fires when any crop hits 100%.

#### 7. Multi-Line Plot Tooltips
Each plot cell has dynamic tooltips via `UIComponent.setTooltip(String...)`:
```
Plot 3: Melon
Growth: 67%
☂ Watered (2× growth)
Reward: 16-28g
```
Tooltips are updated in `updatePlotTooltip(i)` whenever crop state changes (planted, watered, grown, harvested).

#### 8. FocusChangeListener for Keyboard Navigation
```java
fm.addFocusChangeListener((prev, next) -> {
    if (next != null) playClick(1.0f, 0.15f);
});
```
A nearly-silent tick plays when keyboard focus moves between buttons, providing audio confirmation for Tab/Arrow key navigation.

### 1.21.11 Matrix API Note
`DrawContext.getMatrices()` returns `org.joml.Matrix3x2fStack` in 1.21.11 (not the old `MatrixStack`). The correct methods are `pushMatrix()` / `popMatrix()` and `translate(float, float)` — **not** `push()` / `pop()` / `translate(float, float, float)`. This tripped us up during the screen shake implementation.

### Layout (v4)
```
PW = 340, PH = 300, PAD = 10, IW = 320

py + 6:    ☘ Virtual Farm ☘              (title, green, 1.0 scale)
py + 22:   ┌── balance/stats bar ──────┐  (DarkPanel bg)
py + 26:   │ ⭐ 50 Gold │ ☀ Spring │ ✔0│  (0.9 / 0.8 / 0.8 scale)
py + 40:   └──────────────────────────┘
py + 44:   ═══ Divider ═══
py + 48:   [Plot 1] [Plot 2] [Plot 3]     (3×102px, 52px tall, 4px gap)
py + 104:  [Plot 4] [Plot 5] [Plot 6]
py + 160:  ═══ Divider ═══
py + 166:  ▶ Plot 1: Empty — Click Plant!  (selected info)
py + 180:  [Plant▼] [Water] [Harvest] [Close]   (normal mode)
     OR    [▒3g] [◆4g] [●4g] [♦6g] [■8g] [▲10g] [✖]  (crop picker)
py + 204:  ═══ Divider ═══
py + 210:  ── Activity Log ──
py + 222:  5 log entries (12px each, oldest at top)
```

### PocketUICore APIs Used in This Version

| API | Usage |
|-----|-------|
| `DarkPanel` | Root panel, stats bar, plot cells, all with `.setCornerRadius()`, `.setDrawBorder()`, `.setDrawShadow()` |
| `TextLabel` | Title, gold/season/harvest stats, plot numbers, crop labels, selected info, all with `.setScale()`, `.setAlign()` |
| `HoverButton` | 4 action buttons + 6 crop picker + cancel. `.setNormalColor/Hover/Pressed()`, `.setTooltip()`, `.setVisible()`, `.setEnabled()` |
| `PercentageBar` | Per-cell growth bars with `.setProgress()`, `.snapTo()`, `.setEasingSpeed()`, `.setBarColor()` |
| `FocusManager` | Full keyboard navigation. `.register()`, `.unregister()`, `.clear()`, `.focusFirst()`, `.navigateNext/Previous/Direction()`, `.activateFocused()`, `.addFocusChangeListener()`, `.clearFocusChangeListeners()` |
| `AnimationTicker` | 10+ named animations: `farm_open`, `gold_flash`, `gold_popup`, `select_pulse`, `cell_flash_N`, `plant_N`, `water_N`, `harvest_N`, `ready_N`, `shake`, `season_flash`, `mode_switch`. Uses 4 easing types: `EASE_OUT`, `EASE_OUT_BACK`, `EASE_IN_OUT`, `EASE_IN_OUT_SINE` |
| `ObservableState<T>` | Reactive binding for gold, season, harvest count. `.addListener()` triggers label/HUD updates |
| `HudOverlayComponent` | Anchored HUD overlay: `.setDurability()`, `.setTier()`, `.setDurabilityCurrent()`, `.remove()` |
| `ProceduralRenderer` | Rendering: `fillRect`, `fillRoundedRect`, `drawDivider`, `drawScaledText`, `drawScaledCenteredText`, `drawFullScreenOverlay`, `withAlpha`, `lerpColor` |
| `UIComponent` | Base class: `.isHovered()` for click-to-select, `.setTooltip(String...)` for multi-line tooltips, `.setVisible()` for mode-switching, `.renderTooltip()` for rendering |

---

## 12.5. Farm State Persistence (v4.1)

### Problem
When the player closed the UI (Escape or Close button), all progress was lost — gold, planted crops, growth, harvest count, season. Opening `/uisample` again started from scratch with 50 gold and empty plots.

### Root Cause
Every time `/uisample` runs, `new SampleMenuScreen()` creates a fresh instance. All farm state (cropGrowth, cropType, watered, gold, etc.) was stored as instance fields, so it was garbage-collected when the screen closed.

### Solution — Two-Layer Persistence

#### Layer 1: Static Singleton (`FarmData`)
A private static inner class `FarmData` holds all farm state in static memory. As long as the Minecraft client is running, reopening the screen reconnects to the same data.

```java
private static FarmData farmData;  // lives across screen instances
```

The constructor loads: `static singleton → file on disk → fresh defaults`:
```java
if (farmData == null) {
    farmData = new FarmData();
    if (!farmData.loadFromFile()) { /* start fresh */ }
}
cropGrowth = farmData.cropGrowth;  // instance points to static arrays
cropType   = farmData.cropType;
```

#### Layer 2: Disk Save (`farm_save.json`)
When the screen closes (`removed()`), all state is written to `<gameDir>/farm_save.json` using hand-rolled JSON (no Gson dependency). This survives game restarts.

```json
{
  "gold": 127,
  "harvests": 8,
  "seasonIndex": 2,
  "seasonTicks": 183,
  "selectedPlot": 3,
  "cropType": [-1,0,4,-1,2,5],
  "cropGrowth": [0.0,0.87,0.34,0.0,1.0,0.12],
  "watered": [false,true,false,false,false,true]
}
```

Save/load uses simple string parsing — no JSON library required. This is safe for a test mod and avoids any dependency.

#### Reset Command
`/uisample reset` — clears static data, deletes `farm_save.json`, and prints confirmation. Next `/uisample` opens a fresh farm with 50 gold.

### Visual Restoration
When reopening the screen, `init()` iterates all plots and restores:
- Cell background color (empty/planted/ready)
- Growth bar progress (with `snapTo()` so it appears instantly, no easing)
- Bar colors (green for growing, blue for watered, gold for ready)
- Crop labels and tooltips
- Stats labels show saved gold/season/harvest count
- HUD overlay shows correct tier

The welcome message changes to "Welcome back! Xg, Y harvests" if the farm has been played before.

### PocketUICore Suggestion: Built-in State Persistence
Currently there is no built-in way to persist UI state. A `PocketUIState` utility could provide:
```java
PocketUIState.save("farm", Map.of("gold", 50, "plots", plotArray));
Map<String, Object> data = PocketUIState.load("farm");
```
Wrapping file I/O and JSON serialization so each mod doesn't have to reimplement it.

---

## 13. Suggestions for PocketUICore Improvements

These are feature additions that would make PocketUICore more powerful and easier to use for mod developers building interactive UIs.

### 13.1 — Controller Rumble/Haptic API
**Problem:** `ControllerHandler` currently only exposes:
```java
enable(), disable(), setScrollTarget(DarkPanel), isActive(), tick()
```
There is no way to trigger controller vibration/rumble from mod code.

**Suggestion:** Add rumble methods to `ControllerHandler`:
```java
public void rumble(float intensity, int durationMs);
public void rumblePattern(float[] intensities, int[] durations);
public boolean isRumbleSupported();
```
These should internally delegate to Controlify's `RumbleEffect` API when Controlify is present, and silently no-op otherwise. This would let UIs provide haptic feedback on button press, harvest, errors, etc. without each mod reimplementing Controlify integration.

### 13.2 — Relative Positioning Mode for Containers
**Problem:** All child coordinates are absolute screen positions. This requires manual offset math (`px + PAD + offset`) for every component, making layouts verbose and error-prone.

**Suggestion:** Add an optional `setRelativePositioning(boolean)` method to `DarkPanel` and `VerticalListPanel`. When enabled, child `(x, y)` values would be relative to the parent's content origin (`parent.x + padding, parent.y + padding`). The container would translate coordinates internally during `render()` and `mouseClicked()`.

This would let devs write:
```java
panel.setRelativePositioning(true);
panel.addChild(new TextLabel(0, 0, ...)); // relative to panel, not screen
```
instead of computing absolute positions for every element.

### 13.3 — Built-in Screen Shake Utility
**Problem:** Implementing screen shake requires knowledge of JOML's `Matrix3x2fStack` API and manual mouse coordinate adjustment. Each developer must reimplement this.

**Suggestion:** Add a `ScreenShake` utility class or integrate it into `AnimationTicker`:
```java
AnimationTicker.screenShake(float intensity, int durationMs);
```
With automatic `pushMatrix/translate/popMatrix` application in the rendering pipeline and mouse offset adjustment.

### 13.4 — Clickable DarkPanel (OnClick Support)
**Problem:** `DarkPanel` doesn't have an `onClick` callback. To make panels clickable (like our plot cells), devs must manually check `isHovered()` in the Screen's `mouseClicked()`. This works but feels like a workaround.

**Suggestion:** Add `setOnClick(Runnable)` to `DarkPanel` (or even `UIComponent` base class). When set, `mouseClicked()` would invoke the callback if the click is inside bounds, making any component natively clickable.

### 13.5 — Sound Manager Integration
**Problem:** Every mod must manually call `player.playSound(SoundEvents.*.value(), volume, pitch)` for UI audio feedback. There's no standardized way to play UI sounds.

**Suggestion:** Add a `UISoundManager` utility:
```java
UISoundManager.playClick();
UISoundManager.playClick(float pitch, float volume);
UISoundManager.playSuccess();
UISoundManager.playError();
UISoundManager.playCustom(SoundEvent event, float pitch, float volume);
```
This centralizes sound playback, handles the `.value()` unwrapping for `RegistryEntry<SoundEvent>`, and provides consistent default sounds for common UI actions.

### 13.6 — Floating Text / Toast Notification Component
**Problem:** Implementing floating "+Xg" popups requires manual rendering with `drawScaledText` + `AnimationTicker` + alpha/position calculation in `render()`. This is a common UX pattern that every mod has to rebuild.

**Suggestion:** Add a `FloatingText` or `ToastNotification` component:
```java
FloatingText.show(root, x, y, "+15g", GOLD_COLOR, 800, Direction.UP);
```
Handles animation, alpha fade, and floating direction automatically. Could support stacking multiple notifications.

### 13.7 — Grid Layout Component
**Problem:** Building a grid (like our 3×2 plot grid) requires manual coordinate math for each cell position.

**Suggestion:** Add a `GridPanel` component:
```java
GridPanel grid = new GridPanel(x, y, cols, rows, cellW, cellH, gap);
grid.setCell(0, 0, myComponent);  // places at correct position automatically
```
With relative positioning within each cell and automatic size calculation.

### 13.8 — Color Utility Expansion
**Problem:** Darkening/lightening colors requires manual per-channel math. We wrote a `darken(int color, float factor)` helper.

**Suggestion:** Add to `ProceduralRenderer`:
```java
public static int darken(int argb, float factor);
public static int lighten(int argb, float factor);
public static int saturate(int argb, float factor);
```

---

## 14. Live-Testing Session — Background Growth, Notifications, & UX Polish (v4.2)

This section documents features implemented during iterative play-testing sessions where the client was launched repeatedly, bugs/requests were discovered in real time, and fixes were applied between runs.

### 14.1 — Offline Growth Catch-Up

**Problem:** Crops only grew inside `SampleMenuScreen.tick()`. Closing the UI stopped all progress.

**Solution:** Two-pronged approach:
1. **`catchUpOfflineGrowth()`** — On screen reopen, calculates elapsed real-time since `lastClosedTime`, converts to ticks, and retroactively advances seasons (respecting per-season durations), grows crops with season/watered modifiers, and evaporates water.
2. **`lastClosedTime`** field in `FarmData` — Set in `removed()`, saved/loaded in JSON via `readLong()` helper.

### 14.2 — Background Ticker (Offscreen Growth)

**Problem:** Even with catch-up, the player had no way to know crops matured while exploring.

**Solution:**
- `tickOffscreen()` static method — Called every client tick via `ClientTickEvents.END_CLIENT_TICK` in `SampleModClient`. Grows crops, advances seasons, evaporates water, saves periodically (every 600 ticks).
- Returns a notification string when a crop matures (e.g., `"✔ Wheat ready in P5!"`).
- Skips execution when `SampleMenuScreen` is actually open.

### 14.3 — Harvest-Ready Notifications

**Solution:** When `tickOffscreen()` returns a notification:
- **Action bar** message in green (`§a`)
- **Chat message** with `[Farm]` prefix
- **Sound:** `ENTITY_EXPERIENCE_ORB_PICKUP` at 0.5f volume, 1.4f pitch

**Technical note:** `SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP` is a `SoundEvent` directly (not `RegistryEntry<SoundEvent>`), so `.value()` must NOT be called on it.

### 14.4 — Arrow Key Plot Navigation

- **3×2 grid navigation:** UP/DOWN moves between rows, LEFT/RIGHT wraps within row
- **Smart Enter/Space:** Context-sensitive action — harvest if ready, water if growing+unwatered, plant if empty
- **Boundary feedback:** Playing at grid edges produces a soft thud sound + light haptic
- **Number keys 1-6** still work for direct plot selection

### 14.5 — Per-Season Durations

**Change:** Seasons now have individual tick durations via `SEASON_TICKS = {800, 800, 600, 400}`:
- Spring: 800 ticks (40s)
- Summer: 800 ticks (40s) 
- Autumn: 600 ticks (30s)
- Winter: 400 ticks (20s) — kept short by request

All hardcoded `400` references replaced. `catchUpOfflineGrowth()` uses a while-loop to correctly advance through variable-length seasons.

### 14.6 — Double-Click Smart Action

**Feature:** Double-clicking a plot (within 400ms) triggers a smart action:
- If crop is ready → harvest
- If crop is growing and unwatered → water
- If plot is empty → open crop picker

**Implementation:** `lastPlotClickTime` and `lastClickedPlot` tracking in `mouseClicked()`.

### 14.7 — Shop & Upgrade System

**Feature:** Gold now has a purpose beyond planting! A new **Shop** button (purple, ⬆ icon) opens an upgrade picker:

| Upgrade | Cost | Effect | Type |
|---------|------|--------|------|
| Fertilizer | 20g | +50% instant growth on selected crop | Consumable (repeatable) |
| Auto-Water | 60g | Newly planted crops start pre-watered | Permanent (one-time) |
| Gold Magnet | 80g | +50% harvest rewards | Permanent (one-time) |

**Technical details:**
- `FarmData` stores `autoWater` and `goldMagnet` booleans, serialized to JSON via `readBool()` helper
- Shop mode is a third button-row state alongside normal mode and crop picker mode
- Purchased permanent upgrades show ✔ and are disabled in the shop
- Fertilizer validates: must have a growing (non-ready) crop in selected plot
- Gold Magnet applies `reward = reward + reward / 2` (integer arithmetic, rounds down)
- Auto-Water sets `watered[p] = farmData.autoWater` in `plantCrop()`, with blue growth bar

### 14.8 — Button Layout Change

**Change:** Action buttons went from 4 (76px each) to 5 (60px each) to accommodate the Shop button:
- ☘ Plant, ☂ Water, ⭐ Harvest, ⬆ Shop, ✖ Close
- Shop button uses purple color scheme (`#6E40C9` / `#8957E5` / `#553098`)

---

*End of DEVLOG — Virtual Farm v4.2*

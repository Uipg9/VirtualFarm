# PocketUICore — Improvement & Feature Suggestions

> Based on patterns discovered while building Virtual Farm v2.0.0, a comprehensive
> test mod exercising all PocketUICore v1.8.0 APIs across 8 test rounds.

---

## 1. Confirmation Dialog / Modal Component

**Problem:** Prestige reset required a custom two-click confirmation system with
timeout tracking, manual button text swaps, and state flags — all hand-rolled.

**Suggestion:** Add a built-in `ConfirmationDialog` or `ModalOverlay` component:
```java
ConfirmationDialog.show(screen, "Reset all progress?",
    "This will prestige your farm.", 
    () -> doPrestige(),   // onConfirm
    () -> {}              // onCancel
);
```
- Darkened background overlay (already have `drawFullScreenOverlay`)
- Centered dialog box with title, message, Confirm/Cancel buttons
- Auto-focus on Cancel for safety
- Optional timeout parameter (auto-dismiss after N seconds)
- Escape key cancels

---

## 2. Clickable List / Table Component

**Problem:** The shop system required manual hit-testing for 3 item description
zones per page, manual hover tracking (`shopHoveredItem`), manual highlight
rendering, and manual click-to-action routing — all in raw `mouseClicked()`.

**Suggestion:** Add a `SelectableList` or `ClickableTable` component:
```java
SelectableList<ShopItem> list = new SelectableList<>(x, y, width, itemHeight, items);
list.setOnClick(item -> buyItem(item));
list.setOnHover(item -> showPreview(item));
list.setRenderer((ctx, item, hovered, selected) -> {
    // custom per-item rendering
});
```
- Built-in hover detection and highlight rendering
- Click-to-select with callback
- Keyboard navigation (up/down arrows, Enter to select)
- Optional numbered shortcuts (1-9 to select items)
- Scrollable when items exceed visible area (integrate with `ScrollablePanel`)

---

## 3. Tabbed Panel / Tab Bar Component

**Problem:** The info panel (Log/Stats/Guide tabs) required manual tab button
creation, manual `infoTab` state tracking, manual conditional rendering per tab,
and manual keyboard shortcut (`Tab` key) cycling.

**Suggestion:** Add a `TabbedPanel` component:
```java
TabbedPanel tabs = new TabbedPanel(x, y, width, height);
tabs.addTab("Log",   logContent);
tabs.addTab("Stats", statsContent);
tabs.addTab("Guide", guideContent);
tabs.setOnTabChange(index -> { /* optional callback */ });
```
- Tab bar with automatic button sizing/styling
- Content area that swaps on tab selection
- Built-in Tab key cycling
- Optional numbered hotkeys
- Underline/highlight on active tab

---

## 4. Number Formatter / Display Utility

**Problem:** Gold amounts, costs, and stats are displayed throughout the UI.
Large numbers need formatting (comma separation, abbreviation like 1.2K, 3.5M).
Currently done inline everywhere.

**Suggestion:** Add `UIFormatUtils`:
```java
UIFormatUtils.formatGold(1500)     // "1,500"
UIFormatUtils.formatCompact(15000) // "15K"
UIFormatUtils.formatTime(3600)     // "1:00:00"
UIFormatUtils.formatPercent(0.75f) // "75%"
```

---

## 5. Grid / Cell Layout Component

**Problem:** The 6-plot crop grid required manual coordinate calculation for a
3×2 grid with per-cell hit-testing, per-cell rendering, selection state, and
keyboard navigation (number keys 1-6). This is a very common UI pattern.

**Suggestion:** Add a `GridLayout` component:
```java
GridLayout<PlotData> grid = new GridLayout<>(x, y, cols, rows, cellW, cellH, gap);
grid.setItems(plots);
grid.setRenderer((ctx, item, col, row, hovered, selected) -> {
    // custom per-cell rendering
});
grid.setOnClick((item, index) -> selectPlot(index));
grid.setKeyboardNav(true); // number keys 1-N to select
```
- Automatic cell positioning with configurable gap
- Built-in click detection per cell
- Selection state (single or multi-select)
- Keyboard number shortcuts
- Hover highlight

---

## 6. Progress/Growth Bar with Label

**Problem:** Each crop plot has a growth bar with percentage text overlay. The
`PercentageBar` exists but the pattern of "bar + centered text label on top"
was reimplemented for every plot.

**Suggestion:** Enhance `PercentageBar` with:
- Built-in label overlay (centered text showing percentage or custom text)
- Label formatting options (show %, show fraction, custom format)
- Optional animation on value change (smooth fill transition)
- Color gradient based on fill percentage (e.g., red → yellow → green)
- Pulsing/glow effect when bar reaches 100%

---

## 7. Paginated Container

**Problem:** The shop has 3 pages with Left/Right arrow navigation and page
indicator dots. This pagination logic (page state, bounds checking, dot
rendering, arrow key handlers) was all hand-written.

**Suggestion:** Add a `PaginatedContainer`:
```java
PaginatedContainer pages = new PaginatedContainer(x, y, w, h);
pages.addPage(consumablesPanel);
pages.addPage(permanentPanel);
pages.addPage(proceduralPanel);
pages.setShowDots(true);       // page indicator dots
pages.setShowArrows(true);     // left/right arrow buttons
pages.setKeyboardNav(true);    // arrow keys to navigate
```
- Automatic page indicator rendering (dots, numbers, or text)
- Built-in left/right arrow buttons
- Arrow key navigation
- Swipe animation between pages
- Current page callback

---

## 8. Toast / Notification System Enhancement

**Problem:** `FloatingText.show()` works for simple popups, but the farm needed
different notification types (gold earned, milestone achieved, error, seasonal
change) with different colors, durations, and positions.

**Suggestion:** Enhance `FloatingText` or add `NotificationManager`:
```java
NotificationManager.toast("Earned 15 gold!", NotificationType.SUCCESS, 2000);
NotificationManager.toast("Not enough gold!", NotificationType.ERROR, 1500);
NotificationManager.toast("MILESTONE: 100 harvests!", NotificationType.MILESTONE, 3000);
```
- Predefined notification types with distinct colors/icons
- Queue system (stack multiple notifications, auto-dismiss)
- Position options (top-center, bottom-center, above element)
- Optional sound integration with `UISoundManager`

---

## 9. Keyboard Shortcut Manager

**Problem:** The mod registers many keyboard shortcuts (G to open, 1-6 for
plots, B for bulk, Tab for tabs, 1-3 for shop buy, arrows for shop pages,
Escape variants). These are all handled in a large `keyPressed()` method
with manual conditional checks based on current mode.

**Suggestion:** Add a `KeyShortcutManager`:
```java
KeyShortcutManager keys = new KeyShortcutManager();
keys.register("shop", GLFW.GLFW_KEY_1, () -> buyItem(0));  // context-aware
keys.register("farm", GLFW.GLFW_KEY_1, () -> selectPlot(0));
keys.setContext("shop");  // activates shop shortcuts, deactivates farm ones
```
- Context-based shortcut groups (switch based on mode/state)
- Automatic conflict detection
- Built-in help display (show all active shortcuts)
- Modifier key support (Ctrl+, Shift+, Alt+)

---

## 10. Seasonal/Theme Tint System

**Problem:** The seasonal tint overlay (Spring green, Summer yellow, Autumn
orange, Winter blue) was implemented as a manual `drawFullScreenOverlay` with
hand-calculated alpha values and per-season color constants.

**Suggestion:** Add a `ThemeOverlay` or `ScreenTintManager`:
```java
ScreenTintManager.setTint(0x3300FF00, 500); // green tint, 500ms transition
ScreenTintManager.clearTint(500);           // fade out over 500ms
ScreenTintManager.pulse(0x44FFD700, 1000);  // pulse gold once
```
- Smooth transition between tints
- Pulse/flash effects
- Layer multiple tints (ambient + event)
- Integration with `AnimationTicker` for easing

---

## 11. Data Persistence Helper

**Problem:** Farm state is saved/loaded via hand-written JSON serialization
(StringBuilder for save, manual string parsing for load) — fragile and verbose.

**Suggestion:** Add `UIDataStore` for simple key-value persistence:
```java
UIDataStore store = UIDataStore.forMod("virtualfarm");
store.putInt("gold", gold);
store.putIntArray("cropTypes", cropTypes);
store.putString("farmName", name);
store.save();  // writes to .minecraft/config/virtualfarm.json

// Load
int gold = store.getInt("gold", 50);  // default 50
```
- Type-safe getters/setters with defaults
- Auto-save on screen close
- Versioned data (migration support for mod updates)
- No external JSON dependency needed

---

## 12. Hover Tooltip Enhancements

**Problem:** Many elements needed rich tooltips with multiple lines, colored
text, item previews, and cost breakdowns. The current `setTooltip(String)`
is single-line plain text.

**Suggestion:** Enhance tooltip system:
```java
widget.setTooltip(RichTooltip.builder()
    .title("Super Fertilizer", 0xFFE3B341)
    .line("Instantly adds +100% growth")
    .line("Cost: 40 gold", 0xFFDBA53A)
    .separator()
    .line("Tip: Use on slow crops!", 0xFF8B949E)
    .build());
```
- Multi-line support with per-line colors
- Title line (bold/colored)
- Horizontal separator
- Item icon support
- Max width with word wrap
- Rich text formatting (bold, italic via Formatting codes)

---

## 13. Sound Preset Expansion

**Problem:** `UISoundManager` has good presets (playClick, playSuccess, etc.)
but the farm needed additional feedback sounds for harvesting, planting,
seasonal transitions, and prestige. Currently these reuse generic presets.

**Suggestion:** Add more contextual presets:
```java
UISoundManager.playHarvest();    // satisfying collect sound
UISoundManager.playPlant();      // soft placement sound
UISoundManager.playUpgrade();    // level-up fanfare
UISoundManager.playWarning();    // attention-getting alert
UISoundManager.playTransition(); // smooth state change
UISoundManager.playCustom(soundEvent, volume, pitch); // fully custom
```

---

## 14. Screen Shake Directional Control

**Problem:** `ScreenShakeHelper` has Light/Medium/Heavy intensities but no
directional control. Harvesting would benefit from a brief downward shake,
while errors could use a horizontal shake.

**Suggestion:** Add directional variants:
```java
ScreenShakeHelper.triggerHorizontal(intensity, duration);
ScreenShakeHelper.triggerVertical(intensity, duration);
ScreenShakeHelper.triggerDirectional(angle, intensity, duration);
```

---

## 15. Animated Value Display

**Problem:** Gold balance changes are shown with FloatingText "+15g" popups,
but the balance number itself doesn't animate. A counting-up animation when
gold changes would add polish.

**Suggestion:** Add `AnimatedValue` component:
```java
AnimatedValue goldDisplay = new AnimatedValue(x, y, 0);
goldDisplay.setFormat(v -> "💰 " + (int)v + "g");
goldDisplay.animateTo(newGold, 500, EasingType.EASE_OUT);
// smoothly counts from current to new value over 500ms
```
- Smooth number transitions with easing
- Color flash on change (green for increase, red for decrease)
- Scale bounce effect on significant changes
- Format callback for custom display

---

## Priority Ranking

| Priority | Item | Impact | Effort |
|----------|------|--------|--------|
| **HIGH** | 1. Confirmation Dialog | Every mod needs this | Medium |
| **HIGH** | 2. Clickable List | Extremely common pattern | Medium |
| **HIGH** | 3. Tabbed Panel | Very common UI pattern | Medium |
| **HIGH** | 12. Rich Tooltips | Currently very limited | Low |
| **MEDIUM** | 5. Grid Layout | Common for inventories | Medium |
| **MEDIUM** | 7. Paginated Container | Common for multi-page UI | Medium |
| **MEDIUM** | 9. Keyboard Shortcut Mgr | Reduces boilerplate | Medium |
| **MEDIUM** | 11. Data Persistence | Every mod needs save/load | Medium |
| **MEDIUM** | 6. Enhanced Progress Bar | Small quality-of-life | Low |
| **MEDIUM** | 8. Notification System | Enhances FloatingText | Low |
| **LOW** | 4. Number Formatter | Simple utility | Low |
| **LOW** | 10. Screen Tint Manager | Niche but polished | Low |
| **LOW** | 13. Sound Presets | Nice to have | Low |
| **LOW** | 14. Directional Shake | Polish feature | Low |
| **LOW** | 15. Animated Value | Polish feature | Low |

---

*Generated from Virtual Farm v2.0.0 development experience — 8 test rounds,
2500+ lines of UI code, full PocketUICore v1.8.0 API coverage.*

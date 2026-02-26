package com.testcoremod.gui;

import com.pocketuicore.animation.AnimationTicker;
import com.pocketuicore.animation.AnimationTicker.EasingType;
import com.pocketuicore.component.*;
import com.pocketuicore.data.ObservableState;
import com.pocketuicore.render.ProceduralRenderer;
import com.pocketuicore.screen.ScreenShakeHelper;
import com.pocketuicore.sound.UISoundManager;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.*;
import java.nio.file.*;
import java.util.Random;

/**
 * Virtual Farm v5 — fully interactive crop-farming demo for PocketUICore v1.8.0.
 *
 * v5 changes from v4 (exercising v1.8.0 APIs):
 *   - UISoundManager: replaced manual playClick/playError with library presets
 *   - ScreenShakeHelper: replaced manual AnimationTicker shake with dedicated helper
 *   - FloatingText: replaced manual gold-popup with toast notifications
 *   - FocusManager.pushContext/popContext: cleaner mode switching (picker/shop)
 *   - ProceduralRenderer.darken(): removed duplicate local darken() helper
 *   - TextInputComponent: added farm-rename text input
 *   - SliderComponent: added growth-speed multiplier slider
 *   - TextLabel Text constructors: use Minecraft Text where applicable
 *   - Configurable tooltip delay: faster tooltips on action buttons
 *
 * PocketUICore APIs exercised:
 *   DarkPanel, TextLabel, HoverButton, PercentageBar, FocusManager (push/popContext),
 *   AnimationTicker (6 easing types), ObservableState, HudOverlayComponent,
 *   ProceduralRenderer (fillRect, fillRoundedRect, drawDivider, drawScaledText,
 *     drawScaledCenteredText, drawDropShadow, withAlpha, lerpColor, darken,
 *     drawFullScreenOverlay, fillGradientV), UIComponent (isHovered, setTooltip,
 *     renderTooltip, setVisible, setPosition, setEnabled, setTooltipDelayMs),
 *   UISoundManager (playClick, playError, playSuccess, playCelebration, playSelect,
 *     playCreate, playBoundary, playGong, playReady),
 *   ScreenShakeHelper (triggerLight, triggerMedium, triggerHeavy),
 *   FloatingText (show, renderAll), TextInputComponent, SliderComponent
 */
public class SampleMenuScreen extends Screen {

    // ═══════════════════════════════════════════════════════════════════
    //  COLOUR PALETTE
    // ═══════════════════════════════════════════════════════════════════
    private static final int BG             = 0xE60D1117;
    private static final int BORDER         = 0xFF1B2028;
    private static final int STATS_BG       = 0xFF161B22;
    private static final int ACCENT_GREEN   = 0xFF238636;
    private static final int ACCENT_GREEN_H = 0xFF2EA043;
    private static final int ACCENT_GREEN_P = 0xFF196C2E;
    private static final int ACCENT_BLUE    = 0xFF58A6FF;
    private static final int ACCENT_BLUE_H  = 0xFF79C0FF;
    private static final int ACCENT_BLUE_P  = 0xFF388BFD;
    private static final int ACCENT_GOLD    = 0xFFD29922;
    private static final int ACCENT_GOLD_H  = 0xFFE3B341;
    private static final int ACCENT_GOLD_P  = 0xFFBB8009;
    private static final int BTN_NORMAL     = 0xFF161B22;
    private static final int BTN_HOVER      = 0xFF21262D;
    private static final int BTN_PRESS      = 0xFF0D1117;
    private static final int BTN_RED        = 0xFF8B2252;
    private static final int BTN_RED_H      = 0xFFA83264;
    private static final int BTN_RED_P      = 0xFF6B1A42;
    private static final int TEXT_PRIMARY   = 0xFFE6EDF3;
    private static final int TEXT_MUTED     = 0xFF8B949E;
    private static final int TEXT_DIM       = 0xFF484F58;
    private static final int BAR_TRACK      = 0xFF21262D;
    private static final int PLOT_EMPTY     = 0xFF30363D;
    private static final int PLOT_PLANTED   = 0xFF1B3826;
    private static final int PLOT_READY     = 0xFF2E4A1B;
    private static final int PLOT_SELECTED  = 0xFF1A3050;
    private static final int DIVIDER_COL    = 0xFF30363D;
    private static final int LOG_ERR        = 0xFFFF6B6B;
    private static final int LOG_WARN       = 0xFFE3B341;
    private static final int LOG_MILESTONE  = 0xFFBB86FC;

    // ═══════════════════════════════════════════════════════════════════
    //  CROP DATA — varied economics for strategic choice
    // ═══════════════════════════════════════════════════════════════════
    private static final String[] CROP_NAMES   = {"Wheat", "Carrot", "Potato", "Beetroot", "Melon", "Pumpkin"};
    private static final String[] CROP_SYMBOLS = {"\u2592", "\u25C6", "\u25CF", "\u2666", "\u25A0", "\u25B2"};
    private static final int[]    CROP_COLORS  = {0xFFDBA53A, 0xFFE8832A, 0xFFD4A760, 0xFF9B2335, 0xFF5DAE5C, 0xFFD9781A};
    private static final float[]  GROW_SPEEDS  = {0.10f, 0.08f, 0.07f, 0.05f, 0.04f, 0.03f};
    private static final int[]    CROP_COSTS   = {3, 4, 4, 6, 8, 10};
    private static final int[]    REWARD_MIN   = {6, 8, 8, 12, 16, 20};
    private static final int[]    REWARD_MAX   = {14, 16, 18, 22, 28, 35};

    // Season durations in ticks (20 ticks = 1 second)
    //   Spring=40s  Summer=40s  Autumn=30s  Winter=20s (kept short by request)
    private static final int[] SEASON_TICKS = {800, 800, 600, 400};

    // ═══════════════════════════════════════════════════════════════════
    //  UPGRADE & ECONOMY SYSTEM — procedural scaling costs
    // ═══════════════════════════════════════════════════════════════════
    private static final int SUPER_FERT_COST   = 40;   // +100% growth instantly
    private static final int LUCKY_WATER_COST  = 30;   // 3× growth while watered
    private static final int SOIL_BASE         = 25;   // +20% growth speed / level
    private static final int QUALITY_BASE      = 30;   // +20% harvest reward / level
    private static final int LUCKY_BASE        = 40;   // +10% double-harvest chance / level
    private static final int MAX_UPG           = 5;    // max upgrade level
    private static final int PRESTIGE_HARVEST_REQ = 50; // harvests needed to prestige
    private static final int SHOP_PAGES        = 3;    // number of shop pages

    // Crop → real Minecraft item mapping (for crop export)
    @SuppressWarnings("deprecation")
    private static final net.minecraft.item.Item[] CROP_ITEMS = {
        Items.WHEAT, Items.CARROT, Items.POTATO, Items.BEETROOT, Items.MELON_SLICE, Items.PUMPKIN
    };

    // Additional colours
    private static final int ACCENT_PURPLE   = 0xFF8957E5;
    private static final int ACCENT_PURPLE_H = 0xFFA371F7;
    private static final int ACCENT_PURPLE_P = 0xFF6E40C9;

    // ═══════════════════════════════════════════════════════════════════
    //  LAYOUT — ABSOLUTE screen coords (px + offset, py + offset)
    // ═══════════════════════════════════════════════════════════════════
    private static final int PW = 340, PH = 290, PAD = 10;
    private static final int IW = PW - 2 * PAD;   // 320
    private static final int NUM_PLOTS = 6;
    private static final int CELL_W = 102, CELL_H = 52, CELL_GAP = 4;

    // Y offsets relative to panel top
    private static final int TITLE_DY   = 6;
    private static final int BAL_BG_DY  = 22;
    private static final int STATS_DY   = 26;
    private static final int DIV1_DY    = 44;
    private static final int GRID1_DY   = 48;
    private static final int GRID2_DY   = GRID1_DY + CELL_H + CELL_GAP;   // 104
    private static final int DIV2_DY    = GRID2_DY + CELL_H + CELL_GAP;   // 160
    private static final int INFO_DY    = 166;
    private static final int BTNS_DY    = 180;
    private static final int DIV3_DY    = 204;
    private static final int LOG_HDR_DY = 220;   // below tab buttons (tabs at 206-218)
    private static final int LOG_DY     = 230;

    // ═══════════════════════════════════════════════════════════════════
    //  PERSISTENT FARM STATE (static — survives screen close/reopen)
    //  Also saved to disk as farm_save.json for cross-session persistence
    // ═══════════════════════════════════════════════════════════════════
    private static final class FarmData {
        float[]   cropGrowth  = new float[NUM_PLOTS];
        int[]     cropType    = new int[NUM_PLOTS];   // -1 = empty
        boolean[] watered     = new boolean[NUM_PLOTS];
        int       gold        = 50;
        int       harvests    = 0;
        int       seasonIndex = 0;
        int       seasonTicks = 0;
        int       selectedPlot = 0;
        long      lastClosedTime = 0;   // System.currentTimeMillis() at close
        boolean   autoWater  = false;  // Upgrade: auto-water on plant
        boolean   goldMagnet = false;  // Upgrade: +50% harvest rewards
        String    farmName   = "My Farm";  // Legacy field (kept for save compat)
        float     speedMult  = 1.0f;       // Growth speed multiplier (internal, no UI)

        // v6: Crop bank (harvested crops awaiting export)
        int[]     cropBank   = new int[NUM_PLOTS];  // one slot per crop type
        // v6: Procedural upgrades
        int       soilLevel     = 0;   // +20% growth speed per level (max 5)
        int       qualityLevel  = 0;   // +20% harvest reward per level (max 5)
        int       luckyLevel    = 0;   // +10% double-harvest chance per level (max 5)
        // v6: Prestige system
        int       prestigeLevel = 0;   // resets farm for permanent bonuses
        // v6: Lifetime stats
        int       totalGoldEarned   = 0;
        int       totalCropsExported = 0;

        FarmData() {
            for (int i = 0; i < NUM_PLOTS; i++) cropType[i] = -1;
        }

        /** Save to JSON manually (no Gson dependency). */
        void saveToFile() {
            try {
                Path dir = MinecraftClient.getInstance().runDirectory.toPath();
                Path file = dir.resolve("farm_save.json");
                StringBuilder sb = new StringBuilder();
                sb.append("{\n");
                sb.append("  \"gold\": ").append(gold).append(",\n");
                sb.append("  \"harvests\": ").append(harvests).append(",\n");
                sb.append("  \"seasonIndex\": ").append(seasonIndex).append(",\n");
                sb.append("  \"seasonTicks\": ").append(seasonTicks).append(",\n");
                sb.append("  \"selectedPlot\": ").append(selectedPlot).append(",\n");
                sb.append("  \"lastClosedTime\": ").append(lastClosedTime).append(",\n");
                sb.append("  \"autoWater\": ").append(autoWater).append(",\n");
                sb.append("  \"goldMagnet\": ").append(goldMagnet).append(",\n");
                sb.append("  \"farmName\": \"").append(farmName.replace("\"", "\\\"")).append("\",\n");
                sb.append("  \"speedMult\": ").append(speedMult).append(",\n");
                sb.append("  \"soilLevel\": ").append(soilLevel).append(",\n");
                sb.append("  \"qualityLevel\": ").append(qualityLevel).append(",\n");
                sb.append("  \"luckyLevel\": ").append(luckyLevel).append(",\n");
                sb.append("  \"prestigeLevel\": ").append(prestigeLevel).append(",\n");
                sb.append("  \"totalGoldEarned\": ").append(totalGoldEarned).append(",\n");
                sb.append("  \"totalCropsExported\": ").append(totalCropsExported).append(",\n");
                sb.append("  \"cropBank\": [").append(arrToStr(cropBank)).append("],\n");
                sb.append("  \"cropType\": [").append(arrToStr(cropType)).append("],\n");
                sb.append("  \"cropGrowth\": [").append(fArrToStr(cropGrowth)).append("],\n");
                sb.append("  \"watered\": [").append(bArrToStr(watered)).append("]\n");
                sb.append("}");
                Files.writeString(file, sb.toString());
            } catch (Exception e) {
                System.err.println("[TestCoreMod] Failed to save farm: " + e.getMessage());
            }
        }

        /** Load from JSON. Returns true if a save was found and loaded. */
        boolean loadFromFile() {
            try {
                Path dir = MinecraftClient.getInstance().runDirectory.toPath();
                Path file = dir.resolve("farm_save.json");
                if (!Files.exists(file)) return false;
                String json = Files.readString(file);
                gold        = readInt(json, "gold", 50);
                harvests    = readInt(json, "harvests", 0);
                seasonIndex = readInt(json, "seasonIndex", 0);
                seasonTicks = readInt(json, "seasonTicks", 0);
                selectedPlot = readInt(json, "selectedPlot", 0);
                lastClosedTime = readLong(json, "lastClosedTime", 0);
                autoWater  = readBool(json, "autoWater", false);
                goldMagnet = readBool(json, "goldMagnet", false);
                farmName   = readString(json, "farmName", "My Farm");
                speedMult  = readFloat(json, "speedMult", 1.0f);
                soilLevel     = readInt(json, "soilLevel", 0);
                qualityLevel  = readInt(json, "qualityLevel", 0);
                luckyLevel    = readInt(json, "luckyLevel", 0);
                prestigeLevel = readInt(json, "prestigeLevel", 0);
                totalGoldEarned   = readInt(json, "totalGoldEarned", 0);
                totalCropsExported = readInt(json, "totalCropsExported", 0);
                readIntArray(json, "cropBank", cropBank);
                readIntArray(json, "cropType", cropType);
                readFloatArray(json, "cropGrowth", cropGrowth);
                readBoolArray(json, "watered", watered);
                return true;
            } catch (Exception e) {
                System.err.println("[TestCoreMod] Failed to load farm: " + e.getMessage());
                return false;
            }
        }

        // ── Tiny JSON helpers (no library needed) ────────────────────
        private static String arrToStr(int[] a) {
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < a.length; i++) { if (i > 0) s.append(","); s.append(a[i]); }
            return s.toString();
        }
        private static String fArrToStr(float[] a) {
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < a.length; i++) { if (i > 0) s.append(","); s.append(a[i]); }
            return s.toString();
        }
        private static String bArrToStr(boolean[] a) {
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < a.length; i++) { if (i > 0) s.append(","); s.append(a[i]); }
            return s.toString();
        }
        private static int readInt(String json, String key, int def) {
            String pat = "\"" + key + "\": ";
            int idx = json.indexOf(pat);
            if (idx < 0) return def;
            int start = idx + pat.length();
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
            try { return Integer.parseInt(json.substring(start, end)); } catch (Exception e) { return def; }
        }
        private static long readLong(String json, String key, long def) {
            String pat = "\"" + key + "\": ";
            int idx = json.indexOf(pat);
            if (idx < 0) return def;
            int start = idx + pat.length();
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
            try { return Long.parseLong(json.substring(start, end)); } catch (Exception e) { return def; }
        }
        private static boolean readBool(String json, String key, boolean def) {
            String pat = "\"" + key + "\": ";
            int idx = json.indexOf(pat);
            if (idx < 0) return def;
            int start = idx + pat.length();
            return json.regionMatches(start, "true", 0, 4);
        }
        private static String readString(String json, String key, String def) {
            String pat = "\"" + key + "\": \"";
            int idx = json.indexOf(pat);
            if (idx < 0) return def;
            int start = idx + pat.length();
            int end = json.indexOf('"', start);
            if (end < 0) return def;
            return json.substring(start, end).replace("\\\"", "\"");
        }
        private static float readFloat(String json, String key, float def) {
            String pat = "\"" + key + "\": ";
            int idx = json.indexOf(pat);
            if (idx < 0) return def;
            int start = idx + pat.length();
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-')) end++;
            try { return Float.parseFloat(json.substring(start, end)); } catch (Exception e) { return def; }
        }
        private static void readIntArray(String json, String key, int[] out) {
            String pat = "\"" + key + "\": [";
            int idx = json.indexOf(pat);
            if (idx < 0) return;
            int start = idx + pat.length();
            int end = json.indexOf(']', start);
            if (end < 0) return;
            String[] parts = json.substring(start, end).split(",");
            for (int i = 0; i < Math.min(parts.length, out.length); i++) {
                try { out[i] = Integer.parseInt(parts[i].trim()); } catch (Exception ignored) {}
            }
        }
        private static void readFloatArray(String json, String key, float[] out) {
            String pat = "\"" + key + "\": [";
            int idx = json.indexOf(pat);
            if (idx < 0) return;
            int start = idx + pat.length();
            int end = json.indexOf(']', start);
            if (end < 0) return;
            String[] parts = json.substring(start, end).split(",");
            for (int i = 0; i < Math.min(parts.length, out.length); i++) {
                try { out[i] = Float.parseFloat(parts[i].trim()); } catch (Exception ignored) {}
            }
        }
        private static void readBoolArray(String json, String key, boolean[] out) {
            String pat = "\"" + key + "\": [";
            int idx = json.indexOf(pat);
            if (idx < 0) return;
            int start = idx + pat.length();
            int end = json.indexOf(']', start);
            if (end < 0) return;
            String[] parts = json.substring(start, end).split(",");
            for (int i = 0; i < Math.min(parts.length, out.length); i++) {
                out[i] = "true".equals(parts[i].trim());
            }
        }
    }

    /** Singleton farm data — lives as long as the JVM. Also saved to disk. */
    private static FarmData farmData;
    /** Offscreen tick counter — for water evaporation while UI is closed. */
    private static int offscreenTicks = 0;

    // ── Instance references into the static data ─────────────────────
    private float[]   cropGrowth;
    private int[]     cropType;
    private boolean[] watered;
    private int tickCounter  = 0;
    private boolean cropPickerMode = false;
    private boolean shopMode = false;
    private int shopPage = 0;   // v6: shop page index (0..2)
    private int selectedPlot;
    private long lastPlotClickTime = 0;
    private int  lastClickedPlot = -1;

    // v7: Prestige confirmation (two-click safety)
    private boolean prestigeConfirmPending = false;
    private long    prestigeConfirmTime    = 0;

    // v7: Shop hover tracking for interactive descriptions
    private int shopHoveredItem = -1;  // 0..2 = item on current page, -1 = none

    // v7: Last crop type per plot (for double-click auto-replant)
    private final int[] lastCropType = new int[NUM_PLOTS];

    // Upgrade costs
    private static final int FERTILIZER_COST  = 20;
    private static final int AUTO_WATER_COST  = 60;
    private static final int GOLD_MAGNET_COST = 80;

    // ── Observable reactive state (re-created per screen instance) ────
    private ObservableState<Integer> goldState;
    private ObservableState<String>  seasonState;
    private ObservableState<Integer> harvestCount;

    // ── v1.8.0: ScreenShakeHelper (replaces manual AnimationTicker shake) ──
    private ScreenShakeHelper shakeHelper;

    private final String[] SEASONS = {"Spring", "Summer", "Autumn", "Winter"};
    private int seasonIndex;
    private int seasonTicks;

    // ═══════════════════════════════════════════════════════════════════
    //  COMPONENTS
    // ═══════════════════════════════════════════════════════════════════
    private DarkPanel root;
    private final DarkPanel[]      plotCells     = new DarkPanel[NUM_PLOTS];
    private final TextLabel[]      plotNumLabels = new TextLabel[NUM_PLOTS];
    private final TextLabel[]      plotLabels    = new TextLabel[NUM_PLOTS];
    private final PercentageBar[]  growthBars    = new PercentageBar[NUM_PLOTS];
    private TextLabel goldLabel, seasonLabel, harvestLabel, selectedInfo;
    // HUD overlay removed per user feedback (season/gold shown in panel instead)

    // v6: Bottom panel tabs (Log / Stats / Guide) - replaced settings
    private int                activeTab = 0;  // 0=Log, 1=Stats, 2=Guide
    private HoverButton        tabLogBtn, tabStatsBtn, tabGuideBtn;

    // Action buttons (normal mode)
    private HoverButton plantBtn, waterBtn, harvestBtn, shopBtn, closeBtn;
    // Crop picker buttons (picker mode)
    private final HoverButton[] cropPickBtns = new HoverButton[NUM_PLOTS];
    private HoverButton cancelPickBtn;
    // Shop buttons (shop mode)
    private HoverButton fertShopBtn, autoWaterShopBtn, goldMagnetShopBtn, cancelShopBtn;
    // v6: Shop page buttons
    private HoverButton superFertBtn, luckyWaterBtn;  // page 0 extra
    private HoverButton soilUpgBtn, qualityUpgBtn, luckyUpgBtn;  // page 1
    private HoverButton exportBtn, prestigeBtn, shopInfoBtn;     // page 2
    private HoverButton shopPrevBtn, shopNextBtn;  // navigation
    private TextLabel   shopPageLabel;             // "Page 1/3"

    // ═══════════════════════════════════════════════════════════════════
    //  ACTIVITY LOG (5 entries — in-GUI feedback)
    // ═══════════════════════════════════════════════════════════════════
    private static final int LOG_MAX = 5;
    private final String[] logText  = new String[LOG_MAX];
    private final int[]    logColor = new int[LOG_MAX];
    private int logCount = 0;

    // ═══════════════════════════════════════════════════════════════════
    //  GOLD POPUP — now uses v1.8.0 FloatingText toast system
    // ═══════════════════════════════════════════════════════════════════

    // ═══════════════════════════════════════════════════════════════════
    //  STORED CELL POSITIONS (for render-time hover/glow effects)
    // ═══════════════════════════════════════════════════════════════════
    private final int[] cellXPos = new int[NUM_PLOTS];
    private final int[] cellYPos = new int[NUM_PLOTS];

    // Panel origin (computed in init)
    private int px, py;
    private final Random rng = new Random();

    // ═══════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════
    public SampleMenuScreen() {
        super(Text.literal("Virtual Farm"));

        // Load persistent state: static singleton → file → fresh defaults
        if (farmData == null) {
            farmData = new FarmData();
            if (!farmData.loadFromFile()) {
                System.out.println("[TestCoreMod] No save found, starting fresh farm.");
            } else {
                System.out.println("[TestCoreMod] Farm loaded from disk!");
            }
        }

        // Catch up offline growth before wiring instance
        catchUpOfflineGrowth();

        // Point instance fields at static data
        cropGrowth   = farmData.cropGrowth;
        cropType     = farmData.cropType;
        watered      = farmData.watered;
        selectedPlot = farmData.selectedPlot;
        seasonIndex  = farmData.seasonIndex;
        seasonTicks  = farmData.seasonTicks;

        // v7: Initialize lastCropType from current state (for auto-replant)
        for (int i = 0; i < NUM_PLOTS; i++) {
            lastCropType[i] = cropType[i];  // remember what's planted now
        }

        // Re-create ObservableState from persisted values
        goldState    = ObservableState.of(farmData.gold);
        seasonState  = ObservableState.of(SEASONS[seasonIndex]);
        harvestCount = ObservableState.of(farmData.harvests);

        // v1.8.0: ScreenShakeHelper instance (replaces manual AnimationTicker shake)
        shakeHelper = new ScreenShakeHelper();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  INIT — build UI tree with ABSOLUTE screen coordinates
    // ═══════════════════════════════════════════════════════════════════
    @Override
    protected void init() {
        super.init();

        px = (this.width  - PW) / 2;
        py = (this.height - PH) / 2;
        int cx = px + PAD;   // content-area left edge

        AnimationTicker anim = AnimationTicker.getInstance();
        FocusManager fm = FocusManager.getInstance();
        fm.clear();
        fm.clearFocusChangeListeners();

        // ── Root panel ───────────────────────────────────────────────
        root = new DarkPanel(px, py, PW, PH);
        root.setBackgroundColor(BG);
        root.setBorderColor(BORDER);
        root.setCornerRadius(8);
        root.setDrawBorder(true);
        root.setDrawShadow(true);

        // ── Title (v1.8.0: using Text constructor) ─────────────────────
        String titleStr = "\u2618 Virtual Farm \u2618";
        if (farmData.prestigeLevel > 0) titleStr += " \u2605\u00D7" + farmData.prestigeLevel;
        root.addChild(new TextLabel(cx, py + TITLE_DY, IW, 14,
                Text.literal(titleStr).formatted(Formatting.GREEN),
                ACCENT_GREEN, TextLabel.Align.CENTER, 1.0f));

        // ── Stats/balance background bar ─────────────────────────────
        DarkPanel statsBg = new DarkPanel(cx, py + BAL_BG_DY, IW, 18);
        statsBg.setBackgroundColor(STATS_BG);
        statsBg.setCornerRadius(4);
        statsBg.setDrawBorder(true);
        statsBg.setBorderColor(0xFF1E2530);
        statsBg.setDrawShadow(false);
        root.addChild(statsBg);

        // ── Stats labels ─────────────────────────────────────────────
        int third = IW / 3;
        goldLabel = new TextLabel(cx + 6, py + STATS_DY, third - 8, 12,
                "\u2B50 " + goldState.get() + " Gold", ACCENT_GOLD, TextLabel.Align.LEFT, 0.9f);
        seasonLabel = new TextLabel(cx + third, py + STATS_DY, third, 12,
                seasonSymbol(SEASONS[seasonIndex]) + " " + SEASONS[seasonIndex],
                seasonColor(SEASONS[seasonIndex]), TextLabel.Align.CENTER, 0.8f);
        harvestLabel = new TextLabel(cx + third * 2, py + STATS_DY, third - 6, 12,
                "\u2714 " + harvestCount.get() + " harvested", TEXT_MUTED, TextLabel.Align.RIGHT, 0.8f);
        root.addChild(goldLabel);
        root.addChild(seasonLabel);
        root.addChild(harvestLabel);

        // ── Reactive listeners ───────────────────────────────────────
        goldState.addListener(g -> {
            goldLabel.setText("\u2B50 " + g + " Gold");
            anim.start("gold_flash", 0f, 1f, 500, EasingType.EASE_OUT);
        });
        seasonState.addListener(s -> {
            seasonLabel.setText(seasonSymbol(s) + " " + s);
            seasonLabel.setColor(seasonColor(s));
        });
        harvestCount.addListener(h -> {
            harvestLabel.setText("\u2714 " + h + " harvested");
            // v6: Show/hide prestige button when eligible
            boolean eligible = h >= PRESTIGE_HARVEST_REQ;
            if (prestigeBtn != null) {
                prestigeBtn.setVisible(eligible && !shopMode && !cropPickerMode);
                prestigeBtn.setEnabled(eligible);
            }
        });

        // ── Plot grid (3×2, clickable cells) ─────────────────────────
        int gridOffset = (IW - (3 * CELL_W + 2 * CELL_GAP)) / 2;  // center grid
        int[] rowDY = { GRID1_DY, GRID2_DY };
        for (int i = 0; i < NUM_PLOTS; i++) {
            int row = i / 3, col = i % 3;
            int cellX = cx + gridOffset + col * (CELL_W + CELL_GAP);
            int cellY = py + rowDY[row];
            cellXPos[i] = cellX;
            cellYPos[i] = cellY;

            // Cell background
            DarkPanel cell = new DarkPanel(cellX, cellY, CELL_W, CELL_H);
            cell.setBackgroundColor(PLOT_EMPTY);
            cell.setCornerRadius(5);
            cell.setDrawBorder(true);
            cell.setBorderColor(DIVIDER_COL);
            cell.setDrawShadow(false);
            cell.setTooltip("Plot " + (i + 1) + ": Empty", "Click to select");

            // Plot number label (top-left)
            TextLabel numLbl = new TextLabel(cellX + 4, cellY + 3, CELL_W - 8, 8,
                    "Plot " + (i + 1), TEXT_MUTED, TextLabel.Align.LEFT, 0.6f);
            cell.addChild(numLbl);

            // Crop info label (center)
            TextLabel cropLbl = new TextLabel(cellX + 4, cellY + 15, CELL_W - 8, 14,
                    "\u2022 Empty", TEXT_DIM, TextLabel.Align.CENTER, 0.85f);
            cell.addChild(cropLbl);

            // Growth bar (bottom)
            PercentageBar bar = new PercentageBar(
                    cellX + 5, cellY + 35, CELL_W - 10, 10, 0.0f);
            bar.setBarColor(ACCENT_GREEN);
            bar.setTrackColor(BAR_TRACK);
            bar.setCornerRadius(4);
            bar.setShowPercentage(false);
            bar.setEasingSpeed(0.06f);
            cell.addChild(bar);

            root.addChild(cell);
            plotCells[i]     = cell;
            plotNumLabels[i] = numLbl;
            plotLabels[i]    = cropLbl;
            growthBars[i]    = bar;
        }

        // ── Selected-plot info ───────────────────────────────────────
        selectedInfo = new TextLabel(cx, py + INFO_DY, IW, 10,
                "\u25B6 Plot 1: Empty \u2014 Click a plot to select!",
                TEXT_MUTED, TextLabel.Align.CENTER, 0.75f);
        root.addChild(selectedInfo);

        // ── ACTION BUTTONS (normal mode) ─────────────────────────────
        int actW = 60, actGap = 3;
        int actRowW = 5 * actW + 4 * actGap;
        int actX0 = cx + (IW - actRowW) / 2;

        plantBtn = new HoverButton(
                actX0, py + BTNS_DY, actW, 20,
                "\u2618 Plant", this::onPlantClicked,
                ACCENT_GREEN, ACCENT_GREEN_H, ACCENT_GREEN_P, TEXT_PRIMARY, 4);
        plantBtn.setTooltip("Choose a crop to plant");
        plantBtn.setTooltipDelayMs(200);  // v1.8.0: faster tooltips on action buttons

        waterBtn = new HoverButton(
                actX0 + actW + actGap, py + BTNS_DY, actW, 20,
                "\u2602 Water", this::onWater,
                ACCENT_BLUE, ACCENT_BLUE_H, ACCENT_BLUE_P, TEXT_PRIMARY, 4);
        waterBtn.setTooltip("Water selected plot (2\u00D7 growth)");
        waterBtn.setTooltipDelayMs(200);

        harvestBtn = new HoverButton(
                actX0 + 2 * (actW + actGap), py + BTNS_DY, actW, 20,
                "\u2B50 Harvest", this::onHarvest,
                ACCENT_GOLD, ACCENT_GOLD_H, ACCENT_GOLD_P, TEXT_PRIMARY, 4);
        harvestBtn.setTooltip("Harvest a fully-grown crop");
        harvestBtn.setTooltipDelayMs(200);

        shopBtn = new HoverButton(
                actX0 + 3 * (actW + actGap), py + BTNS_DY, actW, 20,
                "\u2B06 Shop", this::onShopClicked,
                0xFF6E40C9, 0xFF8957E5, 0xFF553098, TEXT_PRIMARY, 4);
        shopBtn.setTooltip("Buy upgrades with gold");
        shopBtn.setTooltipDelayMs(200);

        closeBtn = new HoverButton(
                actX0 + 4 * (actW + actGap), py + BTNS_DY, actW, 20,
                "\u2716 Close", this::close,
                BTN_NORMAL, BTN_HOVER, BTN_PRESS, TEXT_MUTED, 4);

        root.addChild(plantBtn);
        root.addChild(waterBtn);
        root.addChild(harvestBtn);
        root.addChild(shopBtn);
        root.addChild(closeBtn);

        fm.register(plantBtn);
        fm.register(waterBtn);
        fm.register(harvestBtn);
        fm.register(shopBtn);
        fm.register(closeBtn);

        // ── PRESTIGE BUTTON (visible only when eligible) ─────────────
        prestigeBtn = new HoverButton(
                cx + IW - 80, py + LOG_HDR_DY, 76, 12,
                "\u2605 Prestige", this::onPrestige,
                0xFFB8860B, 0xFFDAA520, 0xFF8B6914, TEXT_PRIMARY, 3);
        prestigeBtn.setTooltip("Prestige \u2014 Reset farm for permanent +15% bonus",
                "Requires " + PRESTIGE_HARVEST_REQ + " harvests",
                farmData.harvests >= PRESTIGE_HARVEST_REQ ? "\u2714 Ready!" : farmData.harvests + "/" + PRESTIGE_HARVEST_REQ);
        prestigeBtn.setVisible(farmData.harvests >= PRESTIGE_HARVEST_REQ);
        prestigeBtn.setEnabled(farmData.harvests >= PRESTIGE_HARVEST_REQ);
        root.addChild(prestigeBtn);
        if (farmData.harvests >= PRESTIGE_HARVEST_REQ) fm.register(prestigeBtn);

        // ── CROP PICKER BUTTONS (hidden until picker mode) ───────────
        int cropW = 42, cropGap = 3;
        int cancelW = 38;
        int cropRowW = 6 * cropW + 5 * cropGap + cropGap + cancelW;
        int cropX0 = cx + (IW - cropRowW) / 2;

        for (int i = 0; i < NUM_PLOTS; i++) {
            final int cropIdx = i;
            int btnColor   = ProceduralRenderer.darken(CROP_COLORS[i], 0.5f);
            int hoverColor = CROP_COLORS[i];
            int pressColor = ProceduralRenderer.darken(CROP_COLORS[i], 0.3f);

            cropPickBtns[i] = new HoverButton(
                    cropX0 + i * (cropW + cropGap), py + BTNS_DY, cropW, 20,
                    CROP_SYMBOLS[i] + CROP_COSTS[i] + "g",
                    () -> plantCrop(cropIdx),
                    btnColor, hoverColor, pressColor, TEXT_PRIMARY, 3);
            cropPickBtns[i].setTooltip(
                    CROP_NAMES[i] + " \u2014 " + CROP_COSTS[i] + "g",
                    "Reward: " + REWARD_MIN[i] + "-" + REWARD_MAX[i] + "g",
                    speedLabel(i));
            cropPickBtns[i].setVisible(false);
            root.addChild(cropPickBtns[i]);
        }

        cancelPickBtn = new HoverButton(
                cropX0 + 6 * (cropW + cropGap), py + BTNS_DY, cancelW, 20,
                "\u2716", () -> setCropPickerMode(false),
                BTN_RED, BTN_RED_H, BTN_RED_P, TEXT_PRIMARY, 3);
        cancelPickBtn.setTooltip("Cancel");
        cancelPickBtn.setVisible(false);
        root.addChild(cancelPickBtn);

        // ── SHOP BUTTONS — paginated (3 pages × 3 items + nav + cancel) ──
        int shopW = 68, shopGap = 3, navW = 22, shopCancelW = 30;
        // 3×68 + 2×3 + 3 + 22 + 3 + 22 + 3 + 30 = 204+6+83 = 293
        int shopRowW = 3 * shopW + 2 * shopGap + shopGap + navW + shopGap + navW + shopGap + shopCancelW;
        int shopX0 = cx + (IW - shopRowW) / 2;
        int shopBY = py + BTNS_DY;
        int sX1 = shopX0 + shopW + shopGap;
        int sX2 = shopX0 + 2 * (shopW + shopGap);
        int navStart = shopX0 + 3 * (shopW + shopGap);

        // ── PAGE 0: Consumables ──
        fertShopBtn = new HoverButton(shopX0, shopBY, shopW, 20,
                "\u2B06 Fert " + FERTILIZER_COST + "g", this::onBuyFertilizer,
                ACCENT_GREEN, ACCENT_GREEN_H, ACCENT_GREEN_P, TEXT_PRIMARY, 3);
        fertShopBtn.setTooltip("Fertilizer \u2014 " + FERTILIZER_COST + "g",
                "+50% growth on selected crop", "(Consumable)");
        fertShopBtn.setVisible(false);
        root.addChild(fertShopBtn);

        superFertBtn = new HoverButton(sX1, shopBY, shopW, 20,
                "\u2B06 SFert " + SUPER_FERT_COST + "g", this::onBuySuperFertilizer,
                0xFF8B6914, 0xFFA88420, 0xFF6E5310, TEXT_PRIMARY, 3);
        superFertBtn.setTooltip("Super Fertilizer \u2014 " + SUPER_FERT_COST + "g",
                "+100% growth on selected crop!", "(Consumable)");
        superFertBtn.setVisible(false);
        root.addChild(superFertBtn);

        luckyWaterBtn = new HoverButton(sX2, shopBY, shopW, 20,
                "\u2602 LWater " + LUCKY_WATER_COST + "g", this::onBuyLuckyWater,
                ACCENT_BLUE, ACCENT_BLUE_H, ACCENT_BLUE_P, TEXT_PRIMARY, 3);
        luckyWaterBtn.setTooltip("Lucky Water \u2014 " + LUCKY_WATER_COST + "g",
                "3\u00D7 growth speed (vs normal 2\u00D7)", "(Consumable)");
        luckyWaterBtn.setVisible(false);
        root.addChild(luckyWaterBtn);

        // ── PAGE 1: Permanent Upgrades (existing + new procedural) ──
        autoWaterShopBtn = new HoverButton(shopX0, shopBY, shopW, 20,
                farmData.autoWater ? "AutoW \u2714" : "AutoW " + AUTO_WATER_COST + "g", this::onBuyAutoWater,
                ACCENT_BLUE, ACCENT_BLUE_H, ACCENT_BLUE_P, TEXT_PRIMARY, 3);
        autoWaterShopBtn.setTooltip("Auto-Water \u2014 " + AUTO_WATER_COST + "g",
                "Crops start pre-watered when planted",
                farmData.autoWater ? "\u2714 Already purchased!" : "(Permanent)");
        autoWaterShopBtn.setVisible(false);
        root.addChild(autoWaterShopBtn);

        goldMagnetShopBtn = new HoverButton(sX1, shopBY, shopW, 20,
                farmData.goldMagnet ? "Magnet \u2714" : "Magnet " + GOLD_MAGNET_COST + "g", this::onBuyGoldMagnet,
                ACCENT_GOLD, ACCENT_GOLD_H, ACCENT_GOLD_P, TEXT_PRIMARY, 3);
        goldMagnetShopBtn.setTooltip("Gold Magnet \u2014 " + GOLD_MAGNET_COST + "g",
                "+50% harvest rewards",
                farmData.goldMagnet ? "\u2714 Already purchased!" : "(Permanent)");
        goldMagnetShopBtn.setVisible(false);
        root.addChild(goldMagnetShopBtn);

        soilUpgBtn = new HoverButton(sX2, shopBY, shopW, 20,
                soilUpgLabel(), this::onBuySoilUpgrade,
                0xFF3D6B2E, 0xFF4E8A3A, 0xFF2E5220, TEXT_PRIMARY, 3);
        soilUpgBtn.setTooltip("Soil Quality Lv" + farmData.soilLevel + "/" + MAX_UPG,
                "+20% growth speed per level",
                farmData.soilLevel >= MAX_UPG ? "\u2714 MAX" : "Next: " + upgradeCost(SOIL_BASE, farmData.soilLevel) + "g");
        soilUpgBtn.setVisible(false);
        root.addChild(soilUpgBtn);

        // ── PAGE 2: Advanced ──
        qualityUpgBtn = new HoverButton(shopX0, shopBY, shopW, 20,
                qualityUpgLabel(), this::onBuyQualityUpgrade,
                ACCENT_GOLD, ACCENT_GOLD_H, ACCENT_GOLD_P, TEXT_PRIMARY, 3);
        qualityUpgBtn.setTooltip("Crop Quality Lv" + farmData.qualityLevel + "/" + MAX_UPG,
                "+20% harvest reward per level",
                farmData.qualityLevel >= MAX_UPG ? "\u2714 MAX" : "Next: " + upgradeCost(QUALITY_BASE, farmData.qualityLevel) + "g");
        qualityUpgBtn.setVisible(false);
        root.addChild(qualityUpgBtn);

        luckyUpgBtn = new HoverButton(sX1, shopBY, shopW, 20,
                luckyUpgLabel(), this::onBuyLuckyUpgrade,
                ACCENT_PURPLE, ACCENT_PURPLE_H, ACCENT_PURPLE_P, TEXT_PRIMARY, 3);
        luckyUpgBtn.setTooltip("Lucky Harvest Lv" + farmData.luckyLevel + "/" + MAX_UPG,
                "+10% chance for 2\u00D7 harvest per level",
                farmData.luckyLevel >= MAX_UPG ? "\u2714 MAX" : "Next: " + upgradeCost(LUCKY_BASE, farmData.luckyLevel) + "g");
        luckyUpgBtn.setVisible(false);
        root.addChild(luckyUpgBtn);

        exportBtn = new HoverButton(sX2, shopBY, shopW, 20,
                "\u2709 Export", this::onExportCrops,
                0xFF2D6A4F, 0xFF40916C, 0xFF1B4332, TEXT_PRIMARY, 3);
        exportBtn.setTooltip("Export Crops",
                "Convert crop bank to real items!",
                totalBankCount() > 0 ? totalBankCount() + " crops ready" : "Bank empty \u2014 harvest first!");
        exportBtn.setVisible(false);
        root.addChild(exportBtn);

        // ── NAV + CANCEL (all pages) ──
        shopPrevBtn = new HoverButton(navStart, shopBY, navW, 20,
                "\u25C0", this::prevShopPage,
                BTN_NORMAL, BTN_HOVER, BTN_PRESS, TEXT_MUTED, 3);
        shopPrevBtn.setTooltip("Previous page");
        shopPrevBtn.setVisible(false);
        root.addChild(shopPrevBtn);

        shopNextBtn = new HoverButton(navStart + navW + shopGap, shopBY, navW, 20,
                "\u25B6", this::nextShopPage,
                BTN_NORMAL, BTN_HOVER, BTN_PRESS, TEXT_MUTED, 3);
        shopNextBtn.setTooltip("Next page");
        shopNextBtn.setVisible(false);
        root.addChild(shopNextBtn);

        cancelShopBtn = new HoverButton(navStart + 2 * (navW + shopGap), shopBY, shopCancelW, 20,
                "\u2716", () -> setShopMode(false),
                BTN_RED, BTN_RED_H, BTN_RED_P, TEXT_PRIMARY, 3);
        cancelShopBtn.setTooltip("Back");
        cancelShopBtn.setVisible(false);
        root.addChild(cancelShopBtn);

        // Shop page label (below buttons)
        shopPageLabel = new TextLabel(cx, py + BTNS_DY + 22, IW, 8,
                "Page 1/" + SHOP_PAGES, TEXT_DIM, TextLabel.Align.CENTER, 0.6f);
        shopPageLabel.setVisible(false);
        root.addChild(shopPageLabel);

        // ── v6: TAB BUTTONS (Log / Stats / Guide) ────────────────────
        // Replace old settings section with tabbed info panel
        int tabW = 50, tabGap = 3;
        int tabRowW = 3 * tabW + 2 * tabGap;
        int tabX0 = cx + (IW - tabRowW) / 2;
        int tabY = py + DIV3_DY + 2;

        tabLogBtn = new HoverButton(tabX0, tabY, tabW, 12,
                "\u270E Log", () -> switchTab(0),
                ACCENT_GREEN, ACCENT_GREEN_H, ACCENT_GREEN_P, TEXT_PRIMARY, 2);
        tabLogBtn.setTooltip("Activity Log");

        tabStatsBtn = new HoverButton(tabX0 + tabW + tabGap, tabY, tabW, 12,
                "\u2606 Stats", () -> switchTab(1),
                ACCENT_GOLD, ACCENT_GOLD_H, ACCENT_GOLD_P, TEXT_PRIMARY, 2);
        tabStatsBtn.setTooltip("Farm Statistics & Progress");

        tabGuideBtn = new HoverButton(tabX0 + 2 * (tabW + tabGap), tabY, tabW, 12,
                "\u2709 Guide", () -> switchTab(2),
                ACCENT_BLUE, ACCENT_BLUE_H, ACCENT_BLUE_P, TEXT_PRIMARY, 2);
        tabGuideBtn.setTooltip("Gameplay Tips & Help");

        root.addChild(tabLogBtn);
        root.addChild(tabStatsBtn);
        root.addChild(tabGuideBtn);
        fm.register(tabLogBtn);
        fm.register(tabStatsBtn);
        fm.register(tabGuideBtn);

        // ── FocusChangeListener (audio cue on keyboard navigation) ───
        fm.addFocusChangeListener((prev, next) -> {
            if (next != null) UISoundManager.playSelect();  // v1.8.0
        });
        fm.focusFirst();

        // ── Entrance animation ───────────────────────────────────────
        anim.start("farm_open", 0f, 1f, 400, EasingType.EASE_OUT_BACK);

        // ── Welcome / resume log ─────────────────────────────────────
        if (farmData.harvests > 0 || farmData.gold != 50) {
            addLog("\u2618 Welcome back! " + goldState.get() + "g, "
                    + harvestCount.get() + " harvests", ACCENT_GREEN);
        } else {
            addLog("\u2618 Welcome to Virtual Farm!", ACCENT_GREEN);
            addLog("Click a plot, then choose a crop!", TEXT_MUTED);
        }

        // ── Restore plot visuals from persisted state ─────────────────
        for (int i = 0; i < NUM_PLOTS; i++) {
            if (cropType[i] >= 0) {
                // Restore cell background colours
                if (cropGrowth[i] >= 1.0f) {
                    plotCells[i].setBackgroundColor(PLOT_READY);
                    plotCells[i].setBorderColor(ACCENT_GOLD);
                    growthBars[i].setBarColor(ACCENT_GOLD);
                } else {
                    plotCells[i].setBackgroundColor(PLOT_PLANTED);
                    growthBars[i].setBarColor(watered[i] ? ACCENT_BLUE : ACCENT_GREEN);
                }
                growthBars[i].setProgress(cropGrowth[i]);
                growthBars[i].snapTo(cropGrowth[i]);
                updatePlotLabel(i);
                updatePlotTooltip(i);
            }
        }

        updatePlotHighlight();
        updateSelectedInfo();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  CROP PICKER MODE TOGGLE
    // ═══════════════════════════════════════════════════════════════════
    private void setCropPickerMode(boolean picking) {
        cropPickerMode = picking;

        // Toggle button visibility
        plantBtn.setVisible(!picking);
        waterBtn.setVisible(!picking);
        harvestBtn.setVisible(!picking);
        shopBtn.setVisible(!picking);
        closeBtn.setVisible(!picking);
        for (HoverButton cb : cropPickBtns) cb.setVisible(picking);
        cancelPickBtn.setVisible(picking);

        // v6: Hide prestige button when in picker
        if (prestigeBtn != null) {
            boolean showPrestige = !picking && !shopMode && farmData.harvests >= PRESTIGE_HARVEST_REQ;
            prestigeBtn.setVisible(showPrestige);
        }

        // Enable/disable to prevent phantom clicks
        plantBtn.setEnabled(!picking);
        waterBtn.setEnabled(!picking);
        harvestBtn.setEnabled(!picking);
        shopBtn.setEnabled(!picking);
        closeBtn.setEnabled(!picking);
        for (HoverButton cb : cropPickBtns) cb.setEnabled(picking);
        cancelPickBtn.setEnabled(picking);

        // v1.8.0: pushContext/popContext for clean focus isolation
        FocusManager fm = FocusManager.getInstance();
        if (picking) {
            fm.pushContext("crop-picker");
            for (HoverButton cb : cropPickBtns) fm.register(cb);
            fm.register(cancelPickBtn);
            fm.focusFirst();
            selectedInfo.setText("\u25BC Choose a crop to plant:");
            selectedInfo.setColor(ACCENT_GREEN);
        } else {
            fm.popContext();
            fm.focusFirst();
            updateSelectedInfo();
        }

        // Mode transition animation
        AnimationTicker.getInstance().start("mode_switch", 0f, 1f, 200, EasingType.EASE_OUT);

        if (picking) {
            UISoundManager.playSelect();  // v1.8.0
            addLog("\u25BC Select a crop to plant...", TEXT_MUTED);
        } else {
            UISoundManager.playClick(0.8f, 0.3f);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SHOP MODE
    // ═══════════════════════════════════════════════════════════════════
    private void onShopClicked() {
        cancelPrestigeConfirm();  // v7
        setShopMode(true);
    }

    private void setShopMode(boolean shopping) {
        shopMode = shopping;
        if (shopping) shopPage = 0;

        // Toggle plot grid visibility (hide plots in shop mode)
        for (int i = 0; i < NUM_PLOTS; i++) {
            plotCells[i].setVisible(!shopping);
            growthBars[i].setVisible(!shopping);
            plotLabels[i].setVisible(!shopping);
            plotNumLabels[i].setVisible(!shopping);
        }
        selectedInfo.setVisible(!shopping || shopping); // always visible (shows shop header)

        // Toggle action buttons vs shop
        plantBtn.setVisible(!shopping);
        waterBtn.setVisible(!shopping);
        harvestBtn.setVisible(!shopping);
        shopBtn.setVisible(!shopping);
        closeBtn.setVisible(!shopping);
        plantBtn.setEnabled(!shopping);
        waterBtn.setEnabled(!shopping);
        harvestBtn.setEnabled(!shopping);
        shopBtn.setEnabled(!shopping);
        closeBtn.setEnabled(!shopping);

        // Nav + cancel always visible in shop mode
        shopPrevBtn.setVisible(shopping);
        shopNextBtn.setVisible(shopping);
        cancelShopBtn.setVisible(shopping);
        shopPageLabel.setVisible(shopping);
        shopPrevBtn.setEnabled(shopping);
        shopNextBtn.setEnabled(shopping);
        cancelShopBtn.setEnabled(shopping);

        // v6: Hide prestige button when in shop/picker
        if (prestigeBtn != null) {
            boolean showPrestige = !shopping && !cropPickerMode && farmData.harvests >= PRESTIGE_HARVEST_REQ;
            prestigeBtn.setVisible(showPrestige);
        }

        if (shopping) {
            showShopPage(shopPage);
        } else {
            hideAllShopItems();
        }

        // v1.8.0: pushContext/popContext for clean focus isolation
        FocusManager fm = FocusManager.getInstance();
        if (shopping) {
            fm.pushContext("shop");
            registerShopPageButtons(shopPage);
            fm.register(shopPrevBtn);
            fm.register(shopNextBtn);
            fm.register(cancelShopBtn);
            fm.focusFirst();
            selectedInfo.setText("\u2B06 Shop \u2014 Page " + (shopPage + 1) + "/" + SHOP_PAGES);
            selectedInfo.setColor(ACCENT_PURPLE);
        } else {
            fm.popContext();
            fm.focusFirst();
            updateSelectedInfo();
        }

        AnimationTicker.getInstance().start("mode_switch", 0f, 1f, 200, EasingType.EASE_OUT);

        if (shopping) {
            UISoundManager.playSelect();
            addLog("\u2B06 Shop opened! Use \u25C0\u25B6 for pages.", ACCENT_PURPLE);
        } else {
            UISoundManager.playClick(0.8f, 0.3f);
        }
    }

    /** Show the correct 3 item buttons for the given shop page. */
    private void showShopPage(int page) {
        hideAllShopItems();
        switch (page) {
            case 0 -> { fertShopBtn.setVisible(true); fertShopBtn.setEnabled(true);
                         superFertBtn.setVisible(true); superFertBtn.setEnabled(true);
                         luckyWaterBtn.setVisible(true); luckyWaterBtn.setEnabled(true); }
            case 1 -> { autoWaterShopBtn.setVisible(true); autoWaterShopBtn.setEnabled(true);
                         goldMagnetShopBtn.setVisible(true); goldMagnetShopBtn.setEnabled(true);
                         soilUpgBtn.setVisible(true); soilUpgBtn.setEnabled(true); }
            case 2 -> { qualityUpgBtn.setVisible(true); qualityUpgBtn.setEnabled(true);
                         luckyUpgBtn.setVisible(true); luckyUpgBtn.setEnabled(true);
                         exportBtn.setVisible(true); exportBtn.setEnabled(true); }
        }
        shopPageLabel.setText("Page " + (page + 1) + "/" + SHOP_PAGES);
        selectedInfo.setText("\u2B06 Shop \u2014 Page " + (page + 1) + "/" + SHOP_PAGES);
        selectedInfo.setColor(ACCENT_PURPLE);
    }

    private void hideAllShopItems() {
        for (HoverButton b : new HoverButton[]{fertShopBtn, superFertBtn, luckyWaterBtn,
                autoWaterShopBtn, goldMagnetShopBtn, soilUpgBtn,
                qualityUpgBtn, luckyUpgBtn, exportBtn}) {
            if (b != null) { b.setVisible(false); b.setEnabled(false); }
        }
    }

    private void registerShopPageButtons(int page) {
        FocusManager fm = FocusManager.getInstance();
        switch (page) {
            case 0 -> { fm.register(fertShopBtn); fm.register(superFertBtn); fm.register(luckyWaterBtn); }
            case 1 -> { fm.register(autoWaterShopBtn); fm.register(goldMagnetShopBtn); fm.register(soilUpgBtn); }
            case 2 -> { fm.register(qualityUpgBtn); fm.register(luckyUpgBtn); fm.register(exportBtn); }
        }
    }

    private void nextShopPage() {
        shopPage = (shopPage + 1) % SHOP_PAGES;
        showShopPage(shopPage);
        // Re-push focus context for new page
        FocusManager fm = FocusManager.getInstance();
        fm.popContext();
        fm.pushContext("shop");
        registerShopPageButtons(shopPage);
        fm.register(shopPrevBtn);
        fm.register(shopNextBtn);
        fm.register(cancelShopBtn);
        fm.focusFirst();
        UISoundManager.playSelect();
    }

    private void prevShopPage() {
        shopPage = (shopPage - 1 + SHOP_PAGES) % SHOP_PAGES;
        showShopPage(shopPage);
        FocusManager fm = FocusManager.getInstance();
        fm.popContext();
        fm.pushContext("shop");
        registerShopPageButtons(shopPage);
        fm.register(shopPrevBtn);
        fm.register(shopNextBtn);
        fm.register(cancelShopBtn);
        fm.focusFirst();
        UISoundManager.playSelect();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  TAB SWITCHING (Log / Stats / Guide)
    // ═══════════════════════════════════════════════════════════════════
    private void switchTab(int tab) {
        if (activeTab == tab) return;
        activeTab = tab;
        UISoundManager.playClick(0.9f, 0.4f);
    }

    private void onBuyFertilizer() {
        int p = selectedPlot;
        if (cropType[p] < 0) {
            addLog("\u2716 No crop to fertilize!", LOG_ERR);
            UISoundManager.playError();
            return;
        }
        if (cropGrowth[p] >= 1.0f) {
            addLog("\u26A0 Already fully grown!", LOG_WARN);
            UISoundManager.playBoundary();
            return;
        }
        if (goldState.get() < FERTILIZER_COST) {
            addLog("\u2716 Need " + FERTILIZER_COST + "g!", LOG_ERR);
            UISoundManager.playError();
            return;
        }
        goldState.set(goldState.get() - FERTILIZER_COST);
        cropGrowth[p] = Math.min(1.0f, cropGrowth[p] + 0.50f);
        growthBars[p].setProgress(cropGrowth[p]);
        updatePlotLabel(p);
        updatePlotTooltip(p);

        if (cropGrowth[p] >= 1.0f) {
            plotCells[p].setBackgroundColor(PLOT_READY);
            plotCells[p].setBorderColor(ACCENT_GOLD);
            growthBars[p].setBarColor(ACCENT_GOLD);
            addLog("\u2714 " + CROP_NAMES[cropType[p]] + " ready in P" + (p + 1) + "!", ACCENT_GOLD);
        }

        AnimationTicker anim = AnimationTicker.getInstance();
        anim.start("cell_flash_" + p, 0f, 1f, 400, EasingType.EASE_OUT);
        shakeHelper.triggerMedium();  // v1.8.0

        FloatingText.show("-" + FERTILIZER_COST + "g", FloatingText.Anchor.TOP_CENTER, LOG_ERR, 900L);

        UISoundManager.playCreate();  // v1.8.0 preset
        addLog("\u2B06 Fertilized P" + (p + 1) + "! +50% growth", ACCENT_GREEN);
        setShopMode(false);
    }

    private void onBuyAutoWater() {
        if (farmData.autoWater) {
            addLog("\u26A0 Already purchased!", LOG_WARN);
            UISoundManager.playBoundary();
            return;
        }
        if (goldState.get() < AUTO_WATER_COST) {
            addLog("\u2716 Need " + AUTO_WATER_COST + "g!", LOG_ERR);
            UISoundManager.playError();
            return;
        }
        goldState.set(goldState.get() - AUTO_WATER_COST);
        farmData.autoWater = true;

        autoWaterShopBtn.setEnabled(false);
        autoWaterShopBtn.setTooltip("Auto-Water", "Crops start pre-watered when planted", "\u2714 Already purchased!");

        FloatingText.show("-" + AUTO_WATER_COST + "g", FloatingText.Anchor.TOP_CENTER, LOG_ERR, 900L);
        shakeHelper.triggerMedium();  // v1.8.0

        UISoundManager.playSuccess();  // v1.8.0 preset
        addLog("\u2714 Auto-Water unlocked! Crops start watered.", ACCENT_BLUE);
        sendChat(Formatting.AQUA + "\u2714 Upgrade: Auto-Water!");
        setShopMode(false);
    }

    private void onBuyGoldMagnet() {
        if (farmData.goldMagnet) {
            addLog("\u26A0 Already purchased!", LOG_WARN);
            UISoundManager.playBoundary();
            return;
        }
        if (goldState.get() < GOLD_MAGNET_COST) {
            addLog("\u2716 Need " + GOLD_MAGNET_COST + "g!", LOG_ERR);
            UISoundManager.playError();
            return;
        }
        goldState.set(goldState.get() - GOLD_MAGNET_COST);
        farmData.goldMagnet = true;

        goldMagnetShopBtn.setEnabled(false);
        goldMagnetShopBtn.setTooltip("Gold Magnet", "+50% harvest rewards", "\u2714 Already purchased!");

        FloatingText.show("-" + GOLD_MAGNET_COST + "g", FloatingText.Anchor.TOP_CENTER, LOG_ERR, 900L);
        shakeHelper.triggerMedium();  // v1.8.0

        UISoundManager.playSuccess();  // v1.8.0 preset
        addLog("\u2714 Gold Magnet unlocked! +50% rewards!", ACCENT_GOLD);
        sendChat(Formatting.GOLD + "\u2714 Upgrade: Gold Magnet!");
        setShopMode(false);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  v6 SHOP ACTIONS — new consumables, procedural upgrades, export
    // ═══════════════════════════════════════════════════════════════════

    private void onBuySuperFertilizer() {
        int p = selectedPlot;
        if (cropType[p] < 0) { addLog("\u2716 No crop to fertilize!", LOG_ERR); UISoundManager.playError(); return; }
        if (cropGrowth[p] >= 1.0f) { addLog("\u26A0 Already fully grown!", LOG_WARN); UISoundManager.playBoundary(); return; }
        if (goldState.get() < SUPER_FERT_COST) { addLog("\u2716 Need " + SUPER_FERT_COST + "g!", LOG_ERR); UISoundManager.playError(); return; }
        spendGold(SUPER_FERT_COST);
        cropGrowth[p] = Math.min(1.0f, cropGrowth[p] + 1.0f);
        growthBars[p].setProgress(cropGrowth[p]);
        updatePlotLabel(p);
        updatePlotTooltip(p);
        if (cropGrowth[p] >= 1.0f) {
            plotCells[p].setBackgroundColor(PLOT_READY);
            plotCells[p].setBorderColor(ACCENT_GOLD);
            growthBars[p].setBarColor(ACCENT_GOLD);
            addLog("\u2714 " + CROP_NAMES[cropType[p]] + " ready in P" + (p + 1) + "!", ACCENT_GOLD);
        }
        AnimationTicker.getInstance().start("cell_flash_" + p, 0f, 1f, 400, EasingType.EASE_OUT);
        shakeHelper.triggerHeavy();
        FloatingText.show("-" + SUPER_FERT_COST + "g", FloatingText.Anchor.TOP_CENTER, LOG_ERR, 900L);
        UISoundManager.playCelebration();
        addLog("\u2B06 Super Fert P" + (p + 1) + "! +100% growth!", ACCENT_GOLD);
        setShopMode(false);
    }

    private void onBuyLuckyWater() {
        int p = selectedPlot;
        if (cropType[p] < 0) { addLog("\u2716 Nothing to water!", LOG_ERR); UISoundManager.playError(); return; }
        if (cropGrowth[p] >= 1.0f) { addLog("\u26A0 Already grown!", LOG_WARN); UISoundManager.playBoundary(); return; }
        if (watered[p]) { addLog("\u26A0 Already watered!", LOG_WARN); UISoundManager.playBoundary(); return; }
        if (goldState.get() < LUCKY_WATER_COST) { addLog("\u2716 Need " + LUCKY_WATER_COST + "g!", LOG_ERR); UISoundManager.playError(); return; }
        spendGold(LUCKY_WATER_COST);
        watered[p] = true;
        growthBars[p].setBarColor(ACCENT_PURPLE);  // purple = lucky water
        updatePlotTooltip(p);
        shakeHelper.triggerMedium();
        FloatingText.show("-" + LUCKY_WATER_COST + "g", FloatingText.Anchor.TOP_CENTER, LOG_ERR, 900L);
        UISoundManager.playCreate();
        addLog("\u2602 Lucky Water P" + (p + 1) + "! 3\u00D7 growth!", ACCENT_PURPLE);
        setShopMode(false);
    }

    private void onBuySoilUpgrade() {
        if (farmData.soilLevel >= MAX_UPG) { addLog("\u2714 Soil Quality maxed!", LOG_WARN); UISoundManager.playBoundary(); return; }
        int cost = upgradeCost(SOIL_BASE, farmData.soilLevel);
        if (goldState.get() < cost) { addLog("\u2716 Need " + cost + "g!", LOG_ERR); UISoundManager.playError(); return; }
        spendGold(cost);
        farmData.soilLevel++;
        soilUpgBtn.setText(soilUpgLabel());
        soilUpgBtn.setTooltip("Soil Quality Lv" + farmData.soilLevel + "/" + MAX_UPG,
                "+20% growth speed per level",
                farmData.soilLevel >= MAX_UPG ? "\u2714 MAX" : "Next: " + upgradeCost(SOIL_BASE, farmData.soilLevel) + "g");
        shakeHelper.triggerMedium();
        FloatingText.show("Soil Lv" + farmData.soilLevel, FloatingText.Anchor.TOP_CENTER, ACCENT_GREEN, 900L);
        UISoundManager.playSuccess();
        addLog("\u2B06 Soil Quality \u2192 Lv" + farmData.soilLevel + "! +20% speed.", ACCENT_GREEN);
        sendChat(Formatting.GREEN + "\u2B06 Soil Quality Lv" + farmData.soilLevel + "!");
    }

    private void onBuyQualityUpgrade() {
        if (farmData.qualityLevel >= MAX_UPG) { addLog("\u2714 Crop Quality maxed!", LOG_WARN); UISoundManager.playBoundary(); return; }
        int cost = upgradeCost(QUALITY_BASE, farmData.qualityLevel);
        if (goldState.get() < cost) { addLog("\u2716 Need " + cost + "g!", LOG_ERR); UISoundManager.playError(); return; }
        spendGold(cost);
        farmData.qualityLevel++;
        qualityUpgBtn.setText(qualityUpgLabel());
        qualityUpgBtn.setTooltip("Crop Quality Lv" + farmData.qualityLevel + "/" + MAX_UPG,
                "+20% harvest reward per level",
                farmData.qualityLevel >= MAX_UPG ? "\u2714 MAX" : "Next: " + upgradeCost(QUALITY_BASE, farmData.qualityLevel) + "g");
        shakeHelper.triggerMedium();
        FloatingText.show("Quality Lv" + farmData.qualityLevel, FloatingText.Anchor.TOP_CENTER, ACCENT_GOLD, 900L);
        UISoundManager.playSuccess();
        addLog("\u2B06 Crop Quality \u2192 Lv" + farmData.qualityLevel + "! +20% reward.", ACCENT_GOLD);
        sendChat(Formatting.GOLD + "\u2B06 Crop Quality Lv" + farmData.qualityLevel + "!");
    }

    private void onBuyLuckyUpgrade() {
        if (farmData.luckyLevel >= MAX_UPG) { addLog("\u2714 Lucky Harvest maxed!", LOG_WARN); UISoundManager.playBoundary(); return; }
        int cost = upgradeCost(LUCKY_BASE, farmData.luckyLevel);
        if (goldState.get() < cost) { addLog("\u2716 Need " + cost + "g!", LOG_ERR); UISoundManager.playError(); return; }
        spendGold(cost);
        farmData.luckyLevel++;
        luckyUpgBtn.setText(luckyUpgLabel());
        luckyUpgBtn.setTooltip("Lucky Harvest Lv" + farmData.luckyLevel + "/" + MAX_UPG,
                "+10% chance for 2\u00D7 harvest per level",
                farmData.luckyLevel >= MAX_UPG ? "\u2714 MAX" : "Next: " + upgradeCost(LUCKY_BASE, farmData.luckyLevel) + "g");
        shakeHelper.triggerMedium();
        FloatingText.show("Lucky Lv" + farmData.luckyLevel, FloatingText.Anchor.TOP_CENTER, ACCENT_PURPLE, 900L);
        UISoundManager.playSuccess();
        addLog("\u2B06 Lucky Harvest \u2192 Lv" + farmData.luckyLevel + "! +10% 2\u00D7 chance.", ACCENT_PURPLE);
        sendChat(Formatting.LIGHT_PURPLE + "\u2B06 Lucky Harvest Lv" + farmData.luckyLevel + "!");
    }

    private void onExportCrops() {
        int total = totalBankCount();
        if (total == 0) {
            addLog("\u2716 Crop bank empty! Harvest first.", LOG_ERR);
            UISoundManager.playError();
            return;
        }
        // Try to give real items (works in singleplayer)
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean gavItems = false;
        if (mc.getServer() != null && mc.player != null) {
            ServerPlayerEntity sp = mc.getServer().getPlayerManager().getPlayer(mc.player.getUuid());
            if (sp != null) {
                for (int i = 0; i < NUM_PLOTS; i++) {
                    if (farmData.cropBank[i] > 0) {
                        sp.getInventory().insertStack(new ItemStack(CROP_ITEMS[i], farmData.cropBank[i]));
                    }
                }
                gavItems = true;
            }
        }

        // Build summary
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < NUM_PLOTS; i++) {
            if (farmData.cropBank[i] > 0) {
                if (summary.length() > 0) summary.append(", ");
                summary.append(farmData.cropBank[i]).append("\u00D7").append(CROP_NAMES[i]);
                farmData.totalCropsExported += farmData.cropBank[i];
                farmData.cropBank[i] = 0;
            }
        }

        shakeHelper.triggerHeavy();
        FloatingText.show("\u2709 Exported!", FloatingText.Anchor.TOP_CENTER, ACCENT_GREEN, 1200L);
        UISoundManager.playCelebration();
        if (gavItems) {
            addLog("\u2709 Exported to inventory: " + summary, ACCENT_GREEN);
            sendChat(Formatting.GREEN + "\u2709 Crops exported to inventory!");
        } else {
            // Multiplayer fallback — just clear bank, give bonus gold
            int bonusGold = total * 3;
            goldState.set(goldState.get() + bonusGold);
            farmData.totalGoldEarned += bonusGold;
            addLog("\u2709 Exported " + total + " crops! +" + bonusGold + "g bonus", ACCENT_GREEN);
            sendChat(Formatting.GREEN + "\u2709 " + total + " crops exported! +" + bonusGold + "g");
        }

        exportBtn.setTooltip("Export Crops", "Convert crop bank to real items!", "Bank empty \u2014 harvest first!");
    }

    private void onPrestige() {
        if (farmData.harvests < PRESTIGE_HARVEST_REQ) {
            addLog("\u2716 Need " + PRESTIGE_HARVEST_REQ + " harvests to prestige!", LOG_ERR);
            addLog("  Current: " + farmData.harvests + "/" + PRESTIGE_HARVEST_REQ, TEXT_MUTED);
            UISoundManager.playError();
            return;
        }

        // v7: Two-click confirmation — first click asks, second click confirms
        long now = System.currentTimeMillis();
        if (!prestigeConfirmPending || (now - prestigeConfirmTime) > 5000) {
            // First click (or timeout expired): enter confirmation mode
            prestigeConfirmPending = true;
            prestigeConfirmTime = now;
            prestigeBtn.setText("\u26A0 Confirm?");
            prestigeBtn.setTooltip("Are you sure?",
                    "This resets ALL crops, upgrades & gold!",
                    "Click again within 5s to confirm.");
            addLog("\u26A0 Click Prestige again to confirm reset!", LOG_WARN);
            addLog("  All crops, upgrades & gold will be lost!", LOG_ERR);
            UISoundManager.playBoundary();
            shakeHelper.triggerLight();
            // Flash the button
            AnimationTicker.getInstance().start("prestige_flash", 0f, 1f, 800, EasingType.EASE_IN_OUT);
            return;
        }

        // Second click within 5s — actually prestige
        prestigeConfirmPending = false;

        // Perform prestige
        int newLevel = farmData.prestigeLevel + 1;
        int startGold = 50 + 25 * newLevel;

        // Reset most data but keep prestige + lifetime stats
        int savedTotalGold = farmData.totalGoldEarned;
        int savedExported = farmData.totalCropsExported;
        for (int i = 0; i < NUM_PLOTS; i++) {
            farmData.cropType[i] = -1;
            farmData.cropGrowth[i] = 0f;
            farmData.watered[i] = false;
            farmData.cropBank[i] = 0;
        }
        farmData.gold = startGold;
        farmData.harvests = 0;
        farmData.seasonIndex = 0;
        farmData.seasonTicks = 0;
        farmData.autoWater = false;
        farmData.goldMagnet = false;
        farmData.soilLevel = 0;
        farmData.qualityLevel = 0;
        farmData.luckyLevel = 0;
        farmData.speedMult = 1.0f;
        farmData.prestigeLevel = newLevel;
        farmData.totalGoldEarned = savedTotalGold;
        farmData.totalCropsExported = savedExported;
        farmData.saveToFile();

        shakeHelper.triggerHeavy();
        UISoundManager.playGong();
        sendChat(Formatting.LIGHT_PURPLE + "\u2605 PRESTIGE " + newLevel + "! Farm reset with permanent bonuses!");
        sendChat(Formatting.GOLD + "  +15% harvest bonus per prestige, +" + startGold + "g starting gold");

        // Re-open screen to refresh everything
        MinecraftClient.getInstance().execute(() -> {
            MinecraftClient.getInstance().setScreen(new SampleMenuScreen());
        });
    }

    /** v7: Cancel prestige confirmation (called when other actions are taken). */
    private void cancelPrestigeConfirm() {
        if (prestigeConfirmPending) {
            prestigeConfirmPending = false;
            if (prestigeBtn != null) {
                prestigeBtn.setText("\u2605 Prestige");
                prestigeBtn.setTooltip("Prestige \u2014 Reset farm for permanent +15% bonus",
                        "Requires " + PRESTIGE_HARVEST_REQ + " harvests",
                        farmData.harvests >= PRESTIGE_HARVEST_REQ ? "\u2714 Ready!" : farmData.harvests + "/" + PRESTIGE_HARVEST_REQ);
            }
        }
    }

    /** v7: Buy a shop item by page and item index (0-2). Used by click-on-description and number keys. */
    private void buyShopItem(int page, int item) {
        switch (page) {
            case 0 -> { switch (item) { case 0 -> onBuyFertilizer(); case 1 -> onBuySuperFertilizer(); case 2 -> onBuyLuckyWater(); } }
            case 1 -> { switch (item) { case 0 -> onBuyAutoWater(); case 1 -> onBuyGoldMagnet(); case 2 -> onBuySoilUpgrade(); } }
            case 2 -> { switch (item) { case 0 -> onBuyQualityUpgrade(); case 1 -> onBuyLuckyUpgrade(); case 2 -> onExportCrops(); } }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  TICK — crop growth, seasons, water evaporation
    // ═══════════════════════════════════════════════════════════════════
    @Override
    public void tick() {
        super.tick();
        tickCounter++;
        AnimationTicker.getInstance().tick();

        // ── Grow crops ───────────────────────────────────────────────
        for (int i = 0; i < NUM_PLOTS; i++) {
            if (cropType[i] >= 0 && cropGrowth[i] < 1.0f) {
                // Lucky Water: growth bar is purple → 3× speed; normal water → 2×
                boolean isLucky = watered[i] && growthBars[i].getBarColor() == ACCENT_PURPLE;
                float waterMult = watered[i] ? (isLucky ? 3.0f : 2.0f) : 1.0f;
                float speed = GROW_SPEEDS[cropType[i]] * waterMult;
                speed *= farmData.speedMult;  // v5: slider-controlled speed multiplier
                speed *= (1.0f + 0.2f * farmData.soilLevel);  // v6: Soil Quality bonus
                if (seasonIndex == 1) speed *= 1.5f;       // Summer boost
                else if (seasonIndex == 3) speed *= 0.3f;  // Winter penalty

                cropGrowth[i] = Math.min(1.0f, cropGrowth[i] + speed * 0.02f);
                growthBars[i].setProgress(cropGrowth[i]);
                updatePlotLabel(i);
                updatePlotTooltip(i);

                if (cropGrowth[i] >= 1.0f) {
                    plotCells[i].setBackgroundColor(PLOT_READY);
                    plotCells[i].setBorderColor(ACCENT_GOLD);
                    growthBars[i].setBarColor(ACCENT_GOLD);
                    addLog("\u2714 " + CROP_NAMES[cropType[i]] + " ready in P" + (i + 1) + "!", ACCENT_GOLD);
                    sendActionBar(Formatting.GREEN + "\u2714 " + CROP_NAMES[cropType[i]] + " ready to harvest!");
                    // Ready pulse
                    AnimationTicker.getInstance().start("ready_" + i, 0f, 1f, 600, EasingType.EASE_OUT_BACK);
                    UISoundManager.playReady();  // v1.8.0 preset
                }
            }
        }

        // ── Season cycle (per-season duration) ─────────────────────
        seasonTicks++;
        if (seasonTicks >= SEASON_TICKS[seasonIndex]) {
            seasonTicks = 0;
            seasonIndex = (seasonIndex + 1) % 4;
            seasonState.set(SEASONS[seasonIndex]);
            addLog("\u2600 Season: " + SEASONS[seasonIndex], seasonColor(SEASONS[seasonIndex]));
            sendChat(Formatting.YELLOW + "\u2600 Season \u2192 " + SEASONS[seasonIndex]);
            if (seasonIndex == 3) {
                addLog("\u2744 Winter! Growth slowed.", ACCENT_BLUE);
            } else if (seasonIndex == 1) {
                addLog("\u2600 Summer! Growth boosted!", ACCENT_GOLD);
            }
            AnimationTicker.getInstance().start("season_flash", 1f, 0f, 600, EasingType.EASE_IN_OUT);
            UISoundManager.playGong();  // v1.8.0 preset
        }

        // ── Water evaporation (every 200 ticks = 10s) ────────────────
        if (tickCounter % 200 == 0) {
            boolean any = false;
            for (int i = 0; i < NUM_PLOTS; i++) {
                if (watered[i]) {
                    watered[i] = false;
                    any = true;
                    if (cropType[i] >= 0 && cropGrowth[i] < 1.0f) {
                        growthBars[i].setBarColor(ACCENT_GREEN);
                    }
                    updatePlotTooltip(i);
                }
            }
            if (any) addLog("\u2602 Water evaporated.", TEXT_MUTED);
        }

        updateSelectedInfo();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  ACTIONS
    // ═══════════════════════════════════════════════════════════════════

    /** Called when "Plant ▼" button is clicked — opens crop picker. */
    private void onPlantClicked() {
        cancelPrestigeConfirm();  // v7
        int p = selectedPlot;
        if (cropType[p] >= 0) {
            addLog("\u2716 P" + (p + 1) + " already planted!", LOG_ERR);
            UISoundManager.playError();
            shakeHelper.triggerLight();  // v1.8.0
            return;
        }
        if (goldState.get() < CROP_COSTS[0]) {
            addLog("\u2716 Not enough gold!", LOG_ERR);
            UISoundManager.playError();
            shakeHelper.triggerLight();  // v1.8.0
            return;
        }
        setCropPickerMode(true);
    }

    /** Plant a specific crop in the selected plot. */
    private void plantCrop(int type) {
        int p = selectedPlot;
        int cost = CROP_COSTS[type];

        if (goldState.get() < cost) {
            addLog("\u2716 Need " + cost + "g for " + CROP_NAMES[type] + "!", LOG_ERR);
            UISoundManager.playError();
            shakeHelper.triggerLight();  // v1.8.0
            setCropPickerMode(false);
            return;
        }

        goldState.set(goldState.get() - cost);
        cropType[p]   = type;
        lastCropType[p] = type;  // v7: remember for auto-replant
        cropGrowth[p] = 0.0f;
        watered[p]    = farmData.autoWater;  // Auto-Water upgrade: start pre-watered

        plotCells[p].setBackgroundColor(PLOT_PLANTED);
        growthBars[p].setBarColor(farmData.autoWater ? ACCENT_BLUE : ACCENT_GREEN);
        growthBars[p].setProgress(0.0f);
        growthBars[p].snapTo(0.0f);
        updatePlotLabel(p);
        updatePlotTooltip(p);
        updatePlotHighlight();

        setCropPickerMode(false);

        // Animations
        AnimationTicker anim = AnimationTicker.getInstance();
        anim.start("plant_" + p, 0f, 1f, 350, EasingType.EASE_OUT_BACK);
        anim.start("cell_flash_" + p, 0f, 1f, 400, EasingType.EASE_OUT);
        shakeHelper.triggerMedium();  // v1.8.0

        // v1.8.0: FloatingText toast notification
        FloatingText.show("-" + cost + "g", FloatingText.Anchor.TOP_CENTER, LOG_ERR, 900L);

        // Sound: v1.8.0 create preset
        UISoundManager.playCreate();

        addLog("\u2618 Planted " + CROP_NAMES[type] + " in P" + (p + 1) + " (-" + cost + "g)", ACCENT_GREEN);
        sendActionBar(Formatting.GREEN + "\u2618 Planted " + CROP_NAMES[type] + "!");
        if (farmData.autoWater) {
            addLog("\u2602 Auto-watered! (2\u00D7 speed)", ACCENT_BLUE);
        }
    }

    private void onWater() {
        cancelPrestigeConfirm();  // v7
        int p = selectedPlot;
        if (cropType[p] < 0) {
            addLog("\u2716 Nothing to water!", LOG_ERR);
            UISoundManager.playError();
            shakeHelper.triggerLight();  // v1.8.0
            return;
        }
        if (cropGrowth[p] >= 1.0f) {
            addLog("\u26A0 Already grown \u2014 harvest it!", LOG_WARN);
            UISoundManager.playBoundary();
            return;
        }
        if (watered[p]) {
            addLog("\u26A0 Already watered!", LOG_WARN);
            UISoundManager.playBoundary();
            return;
        }

        watered[p] = true;
        growthBars[p].setBarColor(ACCENT_BLUE);
        updatePlotTooltip(p);

        AnimationTicker anim = AnimationTicker.getInstance();
        anim.start("water_" + p, 0f, 1f, 300, EasingType.EASE_IN_OUT_SINE);
        anim.start("cell_flash_" + p, 0f, 1f, 350, EasingType.EASE_OUT);
        shakeHelper.triggerLight();  // v1.8.0

        // Sound: v1.8.0 deep water click
        UISoundManager.playClick(0.5f, 0.5f);

        addLog("\u2602 Watered " + CROP_NAMES[cropType[p]] + " (2\u00D7 speed)", ACCENT_BLUE);
    }

    /** Bulk water — waters all planted, unwatered, growing crops. */
    private void onWaterAll() {
        int count = 0;
        for (int i = 0; i < NUM_PLOTS; i++) {
            if (cropType[i] >= 0 && !watered[i] && cropGrowth[i] < 1.0f) {
                int prev = selectedPlot;
                selectPlot(i);
                onWater();
                count++;
            }
        }
        if (count == 0) {
            addLog("\u2716 Nothing to water!", LOG_ERR);
            UISoundManager.playBoundary();
        } else {
            addLog("\u2602 Watered " + count + " plots! (Shift+W)", ACCENT_BLUE);
        }
    }

    /** Bulk harvest — harvests all fully-grown crops. */
    private void onHarvestAll() {
        int count = 0;
        for (int i = 0; i < NUM_PLOTS; i++) {
            if (cropType[i] >= 0 && cropGrowth[i] >= 1.0f) {
                selectPlot(i);
                onHarvest();
                count++;
            }
        }
        if (count == 0) {
            addLog("\u2716 Nothing to harvest!", LOG_ERR);
            UISoundManager.playBoundary();
        } else {
            addLog("\u2B50 Harvested " + count + " crops! (Shift+H)", ACCENT_GOLD);
        }
    }

    private void onHarvest() {
        cancelPrestigeConfirm();  // v7
        int p = selectedPlot;
        if (cropType[p] < 0) {
            addLog("\u2716 Nothing to harvest!", LOG_ERR);
            UISoundManager.playError();
            shakeHelper.triggerLight();
            return;
        }
        if (cropGrowth[p] < 1.0f) {
            int pct = (int) (cropGrowth[p] * 100);
            addLog("\u26A0 Not ready (" + pct + "% grown)", LOG_WARN);
            UISoundManager.playBoundary();
            return;
        }

        int type = cropType[p];
        int reward = REWARD_MIN[type] + rng.nextInt(REWARD_MAX[type] - REWARD_MIN[type] + 1);
        if (farmData.goldMagnet) reward = reward + reward / 2;  // Gold Magnet: +50%
        reward = (int)(reward * (1.0f + 0.2f * farmData.qualityLevel));  // v6: Crop Quality bonus
        reward = (int)(reward * (1.0f + 0.15f * farmData.prestigeLevel)); // v6: Prestige bonus

        // v6: Lucky Harvest — chance for 2× reward
        boolean luckyProc = false;
        if (farmData.luckyLevel > 0 && rng.nextFloat() < farmData.luckyLevel * 0.10f) {
            reward *= 2;
            luckyProc = true;
        }

        String name = CROP_NAMES[type];
        goldState.set(goldState.get() + reward);
        farmData.totalGoldEarned += reward;
        harvestCount.set(harvestCount.get() + 1);

        // v6: Add to crop bank for later export
        farmData.cropBank[type]++;

        // Reset plot
        cropType[p]   = -1;
        cropGrowth[p] = 0f;
        watered[p]    = false;
        plotCells[p].setBackgroundColor(PLOT_EMPTY);
        plotCells[p].setBorderColor(DIVIDER_COL);
        growthBars[p].setProgress(0f);
        growthBars[p].snapTo(0f);
        growthBars[p].setBarColor(ACCENT_GREEN);
        plotLabels[p].setText("\u2022 Empty");
        plotLabels[p].setColor(TEXT_DIM);
        updatePlotTooltip(p);
        updatePlotHighlight();

        // Big harvest animation + haptic shake
        AnimationTicker anim = AnimationTicker.getInstance();
        anim.start("harvest_" + p, 0f, 1f, 500, EasingType.EASE_OUT);
        anim.start("cell_flash_" + p, 0f, 1f, 400, EasingType.EASE_OUT);
        shakeHelper.triggerHeavy();

        // Toast notification
        String toastText = "+" + reward + "g";
        if (luckyProc) toastText += " \u2605LUCKY!";
        FloatingText.show(toastText, FloatingText.Anchor.TOP_CENTER, luckyProc ? ACCENT_PURPLE : ACCENT_GOLD, 1000L);

        // Sound
        UISoundManager.playCelebration();
        if (luckyProc) {
            addLog("\u2605 LUCKY 2\u00D7! " + name + " +" + reward + "g!", ACCENT_PURPLE);
        } else {
            addLog("\u2B50 Harvested " + name + "! +" + reward + "g", ACCENT_GOLD);
        }
        addLog("  \u2709 +" + CROP_NAMES[type] + " in crop bank (" + totalBankCount() + " total)", TEXT_MUTED);
        sendChat(Formatting.GREEN + "\u2714 " + name + " +" + reward + "g (Total: " + goldState.get() + "g)");

        if (harvestCount.get() % 5 == 0) {
            addLog("\u2605 MILESTONE: " + harvestCount.get() + " harvested!", LOG_MILESTONE);
            sendChat(Formatting.LIGHT_PURPLE + "\u2605 " + harvestCount.get() + " crops harvested!");
            shakeHelper.triggerHeavy();
        }
        // v6: Check if prestige is now available
        if (farmData.harvests >= PRESTIGE_HARVEST_REQ && farmData.prestigeLevel == 0) {
            addLog("\u2605 Prestige available! Check Shop P3.", LOG_MILESTONE);
        }
    }

    private void selectPlot(int idx) {
        if (idx == selectedPlot) return;  // already selected
        int prev = selectedPlot;
        selectedPlot = idx;
        updatePlotHighlight();
        updateSelectedInfo();

        // Selection pulse animation
        AnimationTicker.getInstance().start("select_pulse", 0f, 1f, 300, EasingType.EASE_OUT);
        shakeHelper.triggerLight();  // v1.8.0: light tap

        // Soft select sound
        UISoundManager.playSelect();  // v1.8.0

        // If picker mode was open, update info text
        if (cropPickerMode) {
            selectedInfo.setText("\u25BC Choose crop for Plot " + (idx + 1) + ":");
            selectedInfo.setColor(ACCENT_GREEN);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════════

    private void updatePlotLabel(int i) {
        if (cropType[i] < 0) {
            plotLabels[i].setText("\u2022 Empty");
            plotLabels[i].setColor(TEXT_DIM);
        } else {
            int pct = (int) (cropGrowth[i] * 100);
            String sym = CROP_SYMBOLS[cropType[i]];
            String name = CROP_NAMES[cropType[i]];
            plotLabels[i].setText(sym + " " + name + " " + pct + "%");
            plotLabels[i].setColor(cropGrowth[i] >= 1.0f ? ACCENT_GOLD : CROP_COLORS[cropType[i]]);
        }
    }

    private void updatePlotTooltip(int i) {
        if (cropType[i] < 0) {
            plotCells[i].setTooltip("Plot " + (i + 1) + ": Empty", "Click to select, then plant!");
        } else {
            int pct = (int) (cropGrowth[i] * 100);
            String status = cropGrowth[i] >= 1.0f ? "\u2714 Ready to harvest!"
                    : (watered[i] ? "\u2602 Watered (2\u00D7 growth)" : "Growing...");
            String reward = "Reward: " + REWARD_MIN[cropType[i]] + "-" + REWARD_MAX[cropType[i]] + "g";
            plotCells[i].setTooltip(
                    "Plot " + (i + 1) + ": " + CROP_NAMES[cropType[i]],
                    "Growth: " + pct + "%",
                    status,
                    reward);
        }
    }

    private void updatePlotHighlight() {
        for (int i = 0; i < NUM_PLOTS; i++) {
            if (i == selectedPlot) {
                plotCells[i].setBorderColor(cropGrowth[i] >= 1.0f ? ACCENT_GOLD : ACCENT_GREEN);
                if (cropType[i] < 0) {
                    plotCells[i].setBackgroundColor(PLOT_SELECTED);
                }
            } else {
                plotCells[i].setBorderColor(cropGrowth[i] >= 1.0f ? ACCENT_GOLD : DIVIDER_COL);
                if (cropType[i] < 0) {
                    plotCells[i].setBackgroundColor(PLOT_EMPTY);
                }
            }
        }
    }

    private void updateSelectedInfo() {
        if (cropPickerMode) return;  // don't overwrite picker text
        int p = selectedPlot;
        if (cropType[p] < 0) {
            selectedInfo.setText("\u25B6 Plot " + (p + 1) + ": Empty \u2014 Click Plant to sow!");
            selectedInfo.setColor(TEXT_MUTED);
        } else {
            int pct = (int) (cropGrowth[p] * 100);
            String w = watered[p] ? " \u2602" : "";
            String ready = cropGrowth[p] >= 1.0f ? " \u2714 READY!" : "";
            selectedInfo.setText("\u25B6 P" + (p + 1) + ": "
                    + CROP_NAMES[cropType[p]] + " " + pct + "%" + w + ready);
            selectedInfo.setColor(cropGrowth[p] >= 1.0f ? ACCENT_GOLD : CROP_COLORS[cropType[p]]);
        }
    }

    /** Oldest entry scrolls off; newest at bottom. */
    private void addLog(String text, int color) {
        for (int i = 0; i < LOG_MAX - 1; i++) {
            logText[i]  = logText[i + 1];
            logColor[i] = logColor[i + 1];
        }
        logText[LOG_MAX - 1]  = text;
        logColor[LOG_MAX - 1] = color;
        logCount = Math.min(logCount + 1, LOG_MAX);
    }

    private String seasonSymbol(String s) {
        return switch (s) {
            case "Spring" -> "\u2618";
            case "Summer" -> "\u2600";
            case "Autumn" -> "\u2663";
            case "Winter" -> "\u2744";
            default       -> "\u2022";
        };
    }

    private int seasonColor(String s) {
        return switch (s) {
            case "Spring" -> ACCENT_GREEN;
            case "Summer" -> ACCENT_GOLD;
            case "Autumn" -> 0xFFD4763B;
            case "Winter" -> ACCENT_BLUE;
            default       -> TEXT_MUTED;
        };
    }

    // ── v6 Upgrade/Economy helpers ──────────────────────────────────

    /** Compute scaling cost for an upgrade at the given level. */
    private static int upgradeCost(int base, int level) {
        return base * (level + 1);
    }

    /** Total items in the crop bank across all types. */
    private int totalBankCount() {
        int total = 0;
        for (int c : farmData.cropBank) total += c;
        return total;
    }

    /** Deduct gold via observable state (triggers UI update). */
    private void spendGold(int amount) {
        goldState.set(goldState.get() - amount);
    }

    private String soilUpgLabel() {
        return farmData.soilLevel >= MAX_UPG
                ? "\u2B06 Soil \u2714" : "\u2B06 Soil " + upgradeCost(SOIL_BASE, farmData.soilLevel) + "g";
    }

    private String qualityUpgLabel() {
        return farmData.qualityLevel >= MAX_UPG
                ? "\u2605 Qual \u2714" : "\u2605 Qual " + upgradeCost(QUALITY_BASE, farmData.qualityLevel) + "g";
    }

    private String luckyUpgLabel() {
        return farmData.luckyLevel >= MAX_UPG
                ? "\u2618 Lucky \u2714" : "\u2618 Lucky " + upgradeCost(LUCKY_BASE, farmData.luckyLevel) + "g";
    }

    private String speedLabel(int cropIdx) {
        float s = GROW_SPEEDS[cropIdx];
        if (s >= 0.08f) return "Speed: Fast";
        if (s >= 0.05f) return "Speed: Medium";
        return "Speed: Slow (high value)";
    }

    // ── Sound helpers (varied pitch/volume for distinct audio cues) ──

    private void sendActionBar(String msg) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.player != null) c.player.sendMessage(Text.literal(msg), true);
    }

    private void sendChat(String msg) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.player != null) c.player.sendMessage(Text.literal(msg), false);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  RENDERING — with screen shake, hover glows, sparkles, popups
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        ProceduralRenderer.drawFullScreenOverlay(context, this.width, this.height,
                ProceduralRenderer.COL_OVERLAY);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        AnimationTicker anim = AnimationTicker.getInstance();
        var tr = MinecraftClient.getInstance().textRenderer;
        int cx = px + PAD;

        // ── Season-change flash (full screen tint) ───────────────────
        if (anim.isActive("season_flash")) {
            float a = anim.get("season_flash", 0f);
            int alpha = (int) (a * 35);
            if (alpha > 0) {
                ProceduralRenderer.fillRect(context, 0, 0, this.width, this.height,
                        ProceduralRenderer.withAlpha(seasonColor(SEASONS[seasonIndex]), alpha));
            }
        }

        // ── Screen shake (v1.8.0 ScreenShakeHelper) ──────────────────
        shakeHelper.apply(context);
        int adjMX = mouseX;
        int adjMY = mouseY;

        // ── Render panel + all children ──────────────────────────────
        if (root != null) {
            root.render(context, adjMX, adjMY, delta);
        }

        // ── Seasonal panel tint — subtle color wash per season ───────
        int seasonTint = switch (seasonIndex) {
            case 0 -> 0x0840C040;  // Spring: faint green
            case 1 -> 0x0AFFD700;  // Summer: warm gold
            case 2 -> 0x08FF8C00;  // Autumn: soft orange
            case 3 -> 0x0A6EC6FF;  // Winter: cool blue
            default -> 0x00000000;
        };
        if (seasonTint != 0) {
            ProceduralRenderer.fillRoundedRect(context,
                    px + 2, py + 2, PW - 4, PH - 4, 7, seasonTint);
        }

        // ── Hover glow on plot cells (skip in shop mode) ─────────────
        if (!shopMode) for (int i = 0; i < NUM_PLOTS; i++) {
            boolean hovered = plotCells[i].isHovered(adjMX, adjMY);
            boolean selected = (i == selectedPlot);

            // Hover glow (non-selected cells)
            if (hovered && !selected) {
                ProceduralRenderer.fillRoundedRect(context,
                        cellXPos[i], cellYPos[i], CELL_W, CELL_H, 5,
                        0x18FFFFFF);
            }

            // Selection pulse glow
            if (selected && anim.isActive("select_pulse")) {
                float pulse = anim.get("select_pulse", 0f);
                int ga = (int) ((1f - pulse) * 50);
                if (ga > 0) {
                    ProceduralRenderer.fillRoundedRect(context,
                            cellXPos[i] - 2, cellYPos[i] - 2,
                            CELL_W + 4, CELL_H + 4, 7,
                            ProceduralRenderer.withAlpha(ACCENT_GREEN, ga));
                }
            }

            // Cell action flash (plant/water/harvest)
            if (anim.isActive("cell_flash_" + i)) {
                float f = 1f - anim.get("cell_flash_" + i, 0f);
                int fa = (int) (f * 70);
                if (fa > 0) {
                    int flashCol = (cropType[i] >= 0 && watered[i]) ? ACCENT_BLUE
                            : (cropType[i] >= 0) ? ACCENT_GREEN : ACCENT_GOLD;
                    ProceduralRenderer.fillRoundedRect(context,
                            cellXPos[i], cellYPos[i], CELL_W, CELL_H, 5,
                            ProceduralRenderer.withAlpha(flashCol, fa));
                }
            }

            // Watered tint
            if (watered[i] && cropType[i] >= 0 && cropGrowth[i] < 1.0f) {
                ProceduralRenderer.fillRoundedRect(context,
                        cellXPos[i], cellYPos[i], CELL_W, CELL_H, 5,
                        0x12589EFF);
                // Water drop indicator
                ProceduralRenderer.drawScaledText(context, tr,
                        "\u2602", cellXPos[i] + CELL_W - 14, cellYPos[i] + 3,
                        ACCENT_BLUE, 0.65f);
            }

            // Ready-to-harvest sparkle
            if (cropType[i] >= 0 && cropGrowth[i] >= 1.0f) {
                float t = (tickCounter + delta) * 0.12f;
                // Animated star at varying positions
                int starX1 = cellXPos[i] + CELL_W - 14;
                int starY1 = cellYPos[i] + 2 + (int) (Math.sin(t + i) * 3);
                int starAlpha = 120 + (int) (Math.sin(t * 1.5 + i * 2) * 100);
                ProceduralRenderer.drawScaledText(context, tr, "\u2726",
                        starX1, starY1,
                        ProceduralRenderer.withAlpha(ACCENT_GOLD, Math.max(20, starAlpha)), 0.7f);
                // Second sparkle
                int starX2 = cellXPos[i] + 4;
                int starY2 = cellYPos[i] + CELL_H - 14 + (int) (Math.cos(t * 0.8 + i) * 2);
                ProceduralRenderer.drawScaledText(context, tr, "\u2605",
                        starX2, starY2,
                        ProceduralRenderer.withAlpha(ACCENT_GOLD_H, Math.max(20, 220 - starAlpha)), 0.55f);
            }
        }

        // ── Dividers ─────────────────────────────────────────────────
        ProceduralRenderer.drawDivider(context, cx, py + DIV1_DY, IW, DIVIDER_COL);
        if (!shopMode) {
            ProceduralRenderer.drawDivider(context, cx, py + DIV2_DY, IW, DIVIDER_COL);
        }
        ProceduralRenderer.drawDivider(context, cx, py + DIV3_DY, IW, DIVIDER_COL);

        // ── Shop panel (renders in the grid area when shop is open) ──
        if (shopMode) {
            // Background tint for shop area
            ProceduralRenderer.fillRoundedRect(context,
                    cx + 2, py + GRID1_DY, IW - 4, DIV2_DY - GRID1_DY, 6,
                    0x20B388FF);

            // Category header
            String[] catHeaders = {"\u2697 Consumables", "\u2B06 Permanent Upgrades", "\u2728 Advanced"};
            int catColor = shopPage == 0 ? ACCENT_GREEN : shopPage == 1 ? ACCENT_BLUE : ACCENT_PURPLE;
            ProceduralRenderer.drawScaledCenteredText(context, tr,
                    catHeaders[shopPage], px + PW / 2, py + GRID1_DY + 4, catColor, 0.85f);

            // Item descriptions (3 items per page, vertical list)
            String[][] descriptions = {
                    { "\u2618 Fertilizer (" + FERTILIZER_COST + "g)",
                      "  Boosts selected crop growth by 50%.",
                      "  Single use — applied to current plot." },
                    { "\u2618 Super Fertilizer (" + SUPER_FERT_COST + "g)",
                      "  Doubles growth instantly (+100%)!",
                      "  Premium single-use consumable." },
                    { "\u2602 Lucky Water (" + LUCKY_WATER_COST + "g)",
                      "  3\u00D7 growth speed (vs normal 2\u00D7 water).",
                      "  Shows purple bar while active." },
                    // Page 1
                    { "\u2602 Auto-Water (" + AUTO_WATER_COST + "g)",
                      "  Crops start watered when planted.",
                      farmData.autoWater ? "  \u2714 Already owned!" : "  One-time permanent upgrade." },
                    { "\u2B50 Gold Magnet (" + GOLD_MAGNET_COST + "g)",
                      "  +50% gold from every harvest.",
                      farmData.goldMagnet ? "  \u2714 Already owned!" : "  One-time permanent upgrade." },
                    { "\u2B06 Soil Quality (Lv" + farmData.soilLevel + "/" + MAX_UPG + ")",
                      "  +20% growth speed per level.",
                      farmData.soilLevel >= MAX_UPG ? "  \u2714 MAX level!" : "  Next: " + upgradeCost(SOIL_BASE, farmData.soilLevel) + "g" },
                    // Page 2
                    { "\u2B50 Crop Quality (Lv" + farmData.qualityLevel + "/" + MAX_UPG + ")",
                      "  +20% harvest gold per level.",
                      farmData.qualityLevel >= MAX_UPG ? "  \u2714 MAX level!" : "  Next: " + upgradeCost(QUALITY_BASE, farmData.qualityLevel) + "g" },
                    { "\u2605 Lucky Harvest (Lv" + farmData.luckyLevel + "/" + MAX_UPG + ")",
                      "  +10% chance for 2\u00D7 rewards.",
                      farmData.luckyLevel >= MAX_UPG ? "  \u2714 MAX level!" : "  Next: " + upgradeCost(LUCKY_BASE, farmData.luckyLevel) + "g" },
                    { "\u2709 Export Crops",
                      "  Convert banked crops to real MC items.",
                      totalBankCount() > 0 ? "  \u2714 " + totalBankCount() + " crops ready to export!" : "  Bank empty \u2014 harvest crops first." },
            };
            // v7: Track hovered shop item from mouse position
            int descStartY = py + GRID1_DY + 18;
            int itemHeight = 33;
            shopHoveredItem = -1;
            if (mouseX >= cx && mouseX <= cx + IW - 4 && mouseY >= descStartY && mouseY < descStartY + 3 * itemHeight) {
                shopHoveredItem = (mouseY - descStartY) / itemHeight;
                if (shopHoveredItem < 0 || shopHoveredItem > 2) shopHoveredItem = -1;
            }

            int pageStart = shopPage * 3;
            int descY = py + GRID1_DY + 18;
            for (int item = 0; item < 3; item++) {
                int idx = pageStart + item;
                String[] lines = descriptions[idx];

                // v7: Hover highlight background
                if (item == shopHoveredItem) {
                    ProceduralRenderer.fillRoundedRect(context,
                            cx + 3, descY - 2, IW - 6, itemHeight - 2, 4,
                            0x30FFFFFF);
                }

                // Item number + title (show [1] [2] [3] prefixes)
                String prefix = "[" + (item + 1) + "] ";
                int titleColor = (item == shopHoveredItem) ? catColor : TEXT_PRIMARY;
                ProceduralRenderer.drawScaledText(context, tr,
                        prefix + lines[0], cx + 8, descY, titleColor, 0.7f);
                descY += 10;
                // Description line 1
                ProceduralRenderer.drawScaledText(context, tr,
                        lines[1], cx + 8, descY, TEXT_MUTED, 0.6f);
                descY += 9;
                // Description line 2 (status/cost) + click hint on hover
                int statusCol = lines[2].contains("\u2714") ? ACCENT_GREEN : TEXT_DIM;
                String line3 = lines[2];
                if (item == shopHoveredItem && !lines[2].contains("\u2714")) {
                    line3 += "  \u25B6 Click to buy!";
                    statusCol = catColor;
                }
                ProceduralRenderer.drawScaledText(context, tr,
                        line3, cx + 8, descY, statusCol, 0.6f);
                descY += 14;  // extra gap between items
            }
        }

        // ── Gold flash overlay on balance bar ────────────────────────
        if (anim.isActive("gold_flash")) {
            float f = 1f - anim.get("gold_flash", 0f);
            int fa = (int) (f * 40);
            if (fa > 0) {
                ProceduralRenderer.fillRoundedRect(context,
                        cx, py + BAL_BG_DY, IW, 18, 4,
                        ProceduralRenderer.withAlpha(ACCENT_GOLD, fa));
            }
        }

        // ── Gold popup — v1.8.0 FloatingText toast system ─────────────
        FloatingText.renderAll(context, this.width, this.height, delta);

        // v6: Bank count for Stats tab (prestige/bank indicator removed — shown in Stats tab)
        int bankCount = totalBankCount();

        // ── v6: Tab content (Log / Stats / Guide) ────────────────────
        int contentY = py + LOG_HDR_DY + 2;
        switch (activeTab) {
            case 0 -> { // LOG TAB
                ProceduralRenderer.drawScaledCenteredText(context, tr,
                        "\u2500\u2500 Activity Log \u2500\u2500",
                        px + PW / 2, py + LOG_HDR_DY, TEXT_DIM, 0.75f);
                for (int i = 0; i < LOG_MAX; i++) {
                    if (logText[i] != null) {
                        float fade = (float) (i + 1) / LOG_MAX;
                        int entryColor = ProceduralRenderer.lerpColor(
                                ProceduralRenderer.withAlpha(logColor[i], 80),
                                logColor[i], fade);
                        ProceduralRenderer.drawScaledText(context, tr,
                                logText[i], cx + 4, py + LOG_DY + i * 10,
                                entryColor, 0.7f);
                    }
                }
            }
            case 1 -> { // STATS TAB
                ProceduralRenderer.drawScaledCenteredText(context, tr,
                        "\u2500\u2500 Farm Statistics \u2500\u2500",
                        px + PW / 2, py + LOG_HDR_DY, TEXT_DIM, 0.75f);
                int sY = py + LOG_DY;
                int sCol1 = cx + 6;
                int sCol2 = cx + IW / 2 + 6;

                // Row 1: Gold & Harvests
                ProceduralRenderer.drawScaledText(context, tr,
                        "\u2B50 Gold: " + goldState.get(), sCol1, sY, ACCENT_GOLD, 0.7f);
                ProceduralRenderer.drawScaledText(context, tr,
                        "\u2714 Harvests: " + harvestCount.get(), sCol2, sY, TEXT_MUTED, 0.7f);
                sY += 10;

                // Row 2: Total Earned & Exported
                ProceduralRenderer.drawScaledText(context, tr,
                        "\u2211 Earned: " + farmData.totalGoldEarned + "g", sCol1, sY, ACCENT_GOLD, 0.7f);
                ProceduralRenderer.drawScaledText(context, tr,
                        "\u2709 Exported: " + farmData.totalCropsExported, sCol2, sY, ACCENT_GREEN, 0.7f);
                sY += 10;

                // Row 3: Prestige & Season
                ProceduralRenderer.drawScaledText(context, tr,
                        "\u2605 Prestige: Lv" + farmData.prestigeLevel +
                                " (+" + farmData.prestigeLevel * 15 + "%)", sCol1, sY, 0xFFDAA520, 0.7f);
                ProceduralRenderer.drawScaledText(context, tr,
                        seasonSymbol(SEASONS[seasonIndex]) + " " + SEASONS[seasonIndex], sCol2, sY,
                        seasonColor(SEASONS[seasonIndex]), 0.7f);
                sY += 10;

                // Row 4: Upgrade levels + bank
                ProceduralRenderer.drawScaledText(context, tr,
                        "\u2B06 Soil:" + farmData.soilLevel + " Qual:" + farmData.qualityLevel +
                                " Lucky:" + farmData.luckyLevel +
                                "  \u2709 Bank:" + (bankCount > 0 ? bankCount : "empty"),
                        sCol1, sY, ACCENT_PURPLE, 0.6f);
            }
            case 2 -> { // GUIDE TAB
                ProceduralRenderer.drawScaledCenteredText(context, tr,
                        "\u2500\u2500 Gameplay Guide \u2500\u2500",
                        px + PW / 2, py + LOG_HDR_DY, TEXT_DIM, 0.75f);
                int gY = py + LOG_DY;
                String[] tips = {
                        "\u2618 P=Plant  W=Water  H=Harvest  S=Shop",
                        "\u2B50 Shift+W=Water All  Shift+H=Harvest All",
                        "\u21BB Double-click plot = harvest + replant!",
                        "\u2B06 Shop: click items or press 1/2/3 to buy",
                        "\u2605 Prestige at " + PRESTIGE_HARVEST_REQ + " harvests (+15%)",
                };
                for (int i = 0; i < tips.length; i++) {
                    int tipColor = i % 2 == 0 ? TEXT_MUTED : 0xFFB0B8C4;
                    ProceduralRenderer.drawScaledText(context, tr,
                            tips[i], cx + 4, gY + i * 10, tipColor, 0.65f);
                }
            }
        }

        // Pop shake transform (v1.8.0)
        shakeHelper.restore(context);

        // ── Render vanilla widgets + tooltips (outside shake) ────────
        super.render(context, mouseX, mouseY, delta);
        if (root != null) {
            UIComponent.renderTooltip(context, root, mouseX, mouseY);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  INPUT — click-to-select plots, 1.21.11 signatures
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public boolean mouseClicked(Click click, boolean fromKeyboard) {
        // v7: Click-to-buy in shop description area
        if (click.button() == 0 && shopMode) {
            int px = (this.width - PW) / 2;
            int py = (this.height - PH) / 2;
            int cx = px + PAD;
            int descStartY = py + GRID1_DY + 18;
            int itemHeight = 33;  // 10 + 9 + 14 spacing per item
            float mx = (float) click.x(), my = (float) click.y();
            if (mx >= cx && mx <= cx + IW - 4 && my >= descStartY && my < descStartY + 3 * itemHeight) {
                int itemIdx = (int)((my - descStartY) / itemHeight);
                if (itemIdx >= 0 && itemIdx <= 2) {
                    buyShopItem(shopPage, itemIdx);
                    return true;
                }
            }
        }

        // Check plot cell clicks FIRST — root.mouseClicked() on the DarkPanel
        // consumes any click inside its bounds, so the plot check would never
        // run if we let root handle clicks first.
        // Skip plot clicks when in shop or crop picker mode
        if (click.button() == 0 && !shopMode && !cropPickerMode) {
            for (int i = 0; i < NUM_PLOTS; i++) {
                if (plotCells[i].isHovered(click.x(), click.y())) {
                    long now = System.currentTimeMillis();
                    // Double-click detection: same plot within 400ms = smart action
                    if (i == lastClickedPlot && (now - lastPlotClickTime) < 400) {
                        lastClickedPlot = -1;
                        lastPlotClickTime = 0;
                        selectedPlot = i;  // ensure correct plot
                        updatePlotHighlight();
                        updateSelectedInfo();
                        // Smart action: harvest > water > plant
                        if (cropType[i] >= 0 && cropGrowth[i] >= 1.0f) {
                            // v7: Auto-replant on double-click harvest
                            int savedType = lastCropType[i];
                            onHarvest();
                            // If harvest succeeded (plot is now empty) and we can afford replant
                            if (cropType[i] < 0 && savedType >= 0 && goldState.get() >= CROP_COSTS[savedType]) {
                                plantCrop(savedType);
                                addLog("\u21BB Auto-replanted " + CROP_NAMES[savedType] + "!", ACCENT_GREEN);
                            }
                        } else if (cropType[i] >= 0 && !watered[i] && cropGrowth[i] < 1.0f) {
                            onWater();
                        } else if (cropType[i] < 0) {
                            onPlantClicked();
                        }
                        return true;
                    }
                    lastClickedPlot = i;
                    lastPlotClickTime = now;
                    selectPlot(i);
                    return true;
                }
            }
        }
        // Then let buttons handle
        if (root != null && root.mouseClicked(click.x(), click.y(), click.button())) {
            return true;
        }
        return super.mouseClicked(click, fromKeyboard);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (root != null && root.mouseReleased(click.x(), click.y(), click.button())) {
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        if (root != null && root.mouseScrolled(mouseX, mouseY, hAmount, vAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, hAmount, vAmount);
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int key = keyInput.key();
        int mod = keyInput.modifiers();
        FocusManager fm = FocusManager.getInstance();

        // Tab / Shift-Tab — cycle action buttons
        if (key == 258) {
            if ((mod & 1) != 0) fm.navigatePrevious(); else fm.navigateNext();
            return true;
        }
        // Arrow keys — navigate plot grid (3 cols × 2 rows)
        switch (key) {
            case 265 -> { // UP — move to row above
                if (selectedPlot >= 3) {
                    selectPlot(selectedPlot - 3);
                } else {
                    UISoundManager.playBoundary();  // v1.8.0
                    shakeHelper.triggerLight();  // v1.8.0
                }
                return true;
            }
            case 264 -> { // DOWN — move to row below
                if (selectedPlot < 3) {
                    selectPlot(selectedPlot + 3);
                } else {
                    UISoundManager.playBoundary();  // v1.8.0
                    shakeHelper.triggerLight();  // v1.8.0
                }
                return true;
            }
            case 263 -> { // LEFT — previous plot (wrap within row)
                int row = selectedPlot / 3;
                int col = selectedPlot % 3;
                selectPlot(row * 3 + (col == 0 ? 2 : col - 1));
                return true;
            }
            case 262 -> { // RIGHT — next plot (wrap within row)
                int row = selectedPlot / 3;
                int col = selectedPlot % 3;
                selectPlot(row * 3 + (col == 2 ? 0 : col + 1));
                return true;
            }
        }
        // Enter / Space — smart action on selected plot, or activate focused button
        if (key == 257 || key == 32) {
            if (cropPickerMode || shopMode) {
                // In picker/shop mode, try focused button first
                if (fm.activateFocused()) return true;
            } else {
                // Smart action: harvest > water > plant
                int p = selectedPlot;
                if (cropType[p] >= 0 && cropGrowth[p] >= 1.0f) {
                    onHarvest();
                } else if (cropType[p] >= 0 && !watered[p] && cropGrowth[p] < 1.0f) {
                    onWater();
                } else if (cropType[p] < 0) {
                    onPlantClicked();
                } else {
                    // Already watered + growing — no action, try button
                    if (fm.activateFocused()) return true;
                }
                return true;
            }
        }
        // Number keys 1-6 select plots (skip in shop mode — 1/2/3 buy items instead)
        if (key >= 49 && key <= 54 && !shopMode) {
            selectPlot(key - 49);
            return true;
        }
        // v7: Number keys 1/2/3 buy shop items when shop is open
        if (shopMode && key >= 49 && key <= 51) {
            buyShopItem(shopPage, key - 49);
            return true;
        }
        // v7: Left/Right arrow keys navigate shop pages when shop is open
        if (shopMode) {
            if (key == 262) { nextShopPage(); return true; }   // RIGHT = next page
            if (key == 263) { prevShopPage(); return true; }   // LEFT = prev page
        }
        // Action hotkeys (when not in picker/shop mode)
        if (!cropPickerMode && !shopMode) {
            boolean shift = (mod & 1) != 0;
            switch (key) {
                case 80 -> { onPlantClicked(); return true; }  // P = Plant
                case 87 -> {                                     // W / Shift+W
                    if (shift) { onWaterAll(); } else { onWater(); }
                    return true;
                }
                case 72 -> {                                     // H / Shift+H
                    if (shift) { onHarvestAll(); } else { onHarvest(); }
                    return true;
                }
                case 83 -> { onShopClicked(); return true; }   // S = Shop
            }
        }
        // Escape — close picker/shop first, then screen
        if (key == 256) {
            if (cropPickerMode) {
                setCropPickerMode(false);
                return true;
            }
            if (shopMode) {
                setShopMode(false);
                return true;
            }
            this.close();
            return true;
        }

        return super.keyPressed(keyInput);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  CLEANUP
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public void removed() {
        // ── Persist farm state back to static + disk ──────────────────
        farmData.gold        = goldState.get();
        farmData.harvests    = harvestCount.get();
        farmData.seasonIndex = seasonIndex;
        farmData.seasonTicks = seasonTicks;
        farmData.selectedPlot = selectedPlot;
        farmData.lastClosedTime = System.currentTimeMillis();
        // cropGrowth, cropType, watered are already references into farmData
        // v6 upgrade fields (soilLevel, etc.) update farmData directly
        farmData.saveToFile();

        FocusManager fm = FocusManager.getInstance();
        fm.clear();
        fm.clearFocusChangeListeners();
        AnimationTicker.getInstance().cancelAll();
        FloatingText.clearAll();  // v1.8.0: clear any active toasts
        goldState.clearListeners();
        seasonState.clearListeners();
        harvestCount.clearListeners();
        super.removed();
    }

    /**
     * Simulate crop growth that would have occurred while the UI was closed.
     * Uses saved lastClosedTime to compute elapsed ticks.
     */
    private static void catchUpOfflineGrowth() {
        if (farmData.lastClosedTime <= 0) return;
        long elapsedMs = System.currentTimeMillis() - farmData.lastClosedTime;
        if (elapsedMs <= 500) return; // ignore sub-second gaps

        int elapsedTicks = (int) (elapsedMs / 50); // 20 ticks/second
        if (elapsedTicks <= 0) return;

        // ── Advance seasons (variable duration per season) ────────
        int remainingTicks = elapsedTicks;
        int seasonsAdvanced = 0;
        int si = farmData.seasonIndex;
        int st = farmData.seasonTicks;
        while (remainingTicks > 0) {
            int left = SEASON_TICKS[si] - st;
            if (remainingTicks >= left) {
                remainingTicks -= left;
                si = (si + 1) % 4;
                st = 0;
                seasonsAdvanced++;
            } else {
                st += remainingTicks;
                remainingTicks = 0;
            }
        }
        farmData.seasonIndex = si;
        farmData.seasonTicks = st;

        // ── Grow crops ──────────────────────────────────────────────
        // Simplified: uses the final season modifier as an average
        float seasonMod = 1.0f;
        if (farmData.seasonIndex == 1) seasonMod = 1.5f;       // Summer
        else if (farmData.seasonIndex == 3) seasonMod = 0.3f;  // Winter

        int cropsGrown = 0;
        int cropsMatured = 0;
        for (int i = 0; i < NUM_PLOTS; i++) {
            if (farmData.cropType[i] >= 0 && farmData.cropGrowth[i] < 1.0f) {
                float speed = GROW_SPEEDS[farmData.cropType[i]];
                // v6: Soil upgrade bonus (+20% per level)
                float soilBonus = 1.0f + farmData.soilLevel * 0.2f;
                // Watered bonus: applies for first 200 ticks (10s), then evaporates
                int wateredTicks = farmData.watered[i] ? Math.min(elapsedTicks, 200) : 0;
                int regularTicks = elapsedTicks - wateredTicks;
                float growth = speed * seasonMod * soilBonus * 0.02f * (wateredTicks * 2.0f + regularTicks);
                float before = farmData.cropGrowth[i];
                farmData.cropGrowth[i] = Math.min(1.0f, farmData.cropGrowth[i] + growth);
                cropsGrown++;
                if (before < 1.0f && farmData.cropGrowth[i] >= 1.0f) cropsMatured++;
            }
        }

        // ── Evaporate water after 200 offline ticks ─────────────────
        if (elapsedTicks >= 200) {
            for (int i = 0; i < NUM_PLOTS; i++) farmData.watered[i] = false;
        }

        farmData.lastClosedTime = 0; // consumed

        int secs = (int) (elapsedMs / 1000);
        System.out.println("[TestCoreMod] Offline catch-up: " + secs + "s elapsed, "
                + cropsGrown + " crops grew, " + cropsMatured + " matured, "
                + seasonsAdvanced + " season(s) passed.");
    }

    /** Reset farm to fresh state (for testing). Called by /uisample reset. */
    public static void resetFarm() {
        farmData = null;
        offscreenTicks = 0;
        try {
            Path dir = MinecraftClient.getInstance().runDirectory.toPath();
            Files.deleteIfExists(dir.resolve("farm_save.json"));
        } catch (Exception ignored) {}
    }

    // ═══════════════════════════════════════════════════════════════════
    //  BACKGROUND (OFFSCREEN) TICK — grows crops when UI is closed
    //  Called by SampleModClient via ClientTickEvents.END_CLIENT_TICK.
    //  Returns a notification string if a crop just matured, else null.
    // ═══════════════════════════════════════════════════════════════════
    public static String tickOffscreen() {
        if (farmData == null) return null;
        // Skip if the farm screen is currently open (its own tick handles growth)
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen instanceof SampleMenuScreen) return null;

        offscreenTicks++;
        String notification = null;

        // ── Grow crops ───────────────────────────────────────────────
        for (int i = 0; i < NUM_PLOTS; i++) {
            if (farmData.cropType[i] >= 0 && farmData.cropGrowth[i] < 1.0f) {
                float speed = GROW_SPEEDS[farmData.cropType[i]] * (farmData.watered[i] ? 2.0f : 1.0f);
                // v6: Soil upgrade bonus (+20% per level)
                speed *= (1.0f + farmData.soilLevel * 0.2f);
                if (farmData.seasonIndex == 1) speed *= 1.5f;       // Summer
                else if (farmData.seasonIndex == 3) speed *= 0.3f;  // Winter

                farmData.cropGrowth[i] = Math.min(1.0f, farmData.cropGrowth[i] + speed * 0.02f);

                if (farmData.cropGrowth[i] >= 1.0f) {
                    notification = "\u2714 " + CROP_NAMES[farmData.cropType[i]] + " ready in P" + (i + 1) + "!";
                }
            }
        }

        // ── Season cycle (per-season duration) ─────────────────────
        farmData.seasonTicks++;
        if (farmData.seasonTicks >= SEASON_TICKS[farmData.seasonIndex]) {
            farmData.seasonTicks = 0;
            farmData.seasonIndex = (farmData.seasonIndex + 1) % 4;
        }

        // ── Water evaporation (every 200 ticks = 10s) ────────────────
        if (offscreenTicks % 200 == 0) {
            for (int i = 0; i < NUM_PLOTS; i++) farmData.watered[i] = false;
        }

        // ── Periodic save (every 600 ticks = 30s) ────────────────────
        if (offscreenTicks % 600 == 0) {
            farmData.lastClosedTime = System.currentTimeMillis();
            farmData.saveToFile();
        }

        return notification;
    }
}

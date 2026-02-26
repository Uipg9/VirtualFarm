package com.testcoremod;

import com.testcoremod.gui.SampleMenuScreen;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * SampleModClient — Client entrypoint for the PocketUICore test mod.
 *
 * Registers client-side commands:
 *   /uisample        →  opens SampleMenuScreen
 *   /uisample reset  →  wipes saved farm data and starts fresh
 *
 * Uses the modern Fabric ClientCommandRegistrationCallback (1.21.11).
 */
public class SampleModClient implements ClientModInitializer {

    public static final String MOD_ID = "testcoremod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Keybinding: G key to open Virtual Farm (configurable in Controls)
    private static KeyBinding openFarmKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[TestCoreMod] Client initializing...");

        // ── Register hotkey (default: G) ─────────────────────────────
        openFarmKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.testcoremod.open_farm",       // Translation key
                GLFW.GLFW_KEY_G,                   // Default: G key
                KeyBinding.Category.MISC           // Miscellaneous category
        ));

        // ── Register /uisample client command ────────────────────────
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                literal("uisample")
                    .executes(context -> {
                        MinecraftClient.getInstance().execute(() -> {
                            MinecraftClient.getInstance().setScreen(new SampleMenuScreen());
                        });
                        return 1;
                    })
                    .then(literal("reset").executes(context -> {
                        SampleMenuScreen.resetFarm();
                        context.getSource().sendFeedback(
                                Text.literal("\u00A7a\u2618 Farm data reset! Open /uisample to start fresh."));
                        return 1;
                    }))
            );
        });
        // ── Background crop growth ticker + hotkey check ───────────────
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Hotkey: open Virtual Farm screen
            while (openFarmKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new SampleMenuScreen());
                }
            }

            String notification = SampleMenuScreen.tickOffscreen();
            if (notification != null && client.player != null) {
                // Action bar (visible above hotbar)
                client.player.sendMessage(
                        Text.literal("\u00A7a" + notification), true);
                // Chat message (persistent in chat log)
                client.player.sendMessage(
                        Text.literal("\u00A7a\u00A7l[Farm] \u00A7r\u00A7a" + notification), false);
                // Ding sound
                client.player.playSound(
                        net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                        0.5f, 1.4f);
            }
        });
        LOGGER.info("[TestCoreMod] Client initialized — use /uisample to open the test UI.");
    }
}

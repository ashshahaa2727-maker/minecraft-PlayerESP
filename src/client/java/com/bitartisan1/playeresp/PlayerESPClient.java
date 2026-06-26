package com.bitartisan1.playeresp;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerESPClient implements ClientModInitializer {

    public static final String MOD_ID = "playeresp";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding hitboxKey;
    private static KeyBinding nameKey;
    private static KeyBinding configKey;
    private static KeyBinding chestKey;

    private static boolean shouldShowHitbox = true;
    private static boolean shouldShowName = true;
    private static boolean chestESPEnabled = true;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[PlayerESP] Client init started...");

        hitboxKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.playeresp.toggle_hitbox",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "category.playeresp.main"
        ));

        nameKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.playeresp.toggle_name",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            "category.playeresp.main"
        ));

        configKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.playeresp.open_config",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "category.playeresp.main"
        ));

        chestKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.playeresp.toggle_chest",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "category.playeresp.main"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            TargetESPManager.tick();

            if (hitboxKey.wasPressed()) {
                shouldShowHitbox = !shouldShowHitbox;
                if (client.player != null)
                    client.player.sendMessage(
                        net.minecraft.text.Text.literal("Hitbox ESP: " + (shouldShowHitbox ? "§aON" : "§cOFF")), true);
            }

            if (nameKey.wasPressed()) {
                shouldShowName = !shouldShowName;
                if (client.player != null)
                    client.player.sendMessage(
                        net.minecraft.text.Text.literal("Name ESP: " + (shouldShowName ? "§aON" : "§cOFF")), true);
            }

            if (chestKey.wasPressed()) {
                chestESPEnabled = !chestESPEnabled;
                if (client.player != null)
                    client.player.sendMessage(
                        net.minecraft.text.Text.literal("Chest ESP: " + (chestESPEnabled ? "§aON" : "§cOFF")), true);
            }

            if (configKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new com.bitartisan1.playeresp.gui.PlayerESPConfigScreen(null));
                }
            }
        });

        WorldRenderEvents.AFTER_TRANSLUCENT.register(new PlayerESPRenderer());
        // Строка изменена обратно на AFTER_TRANSLUCENT
        WorldRenderEvents.AFTER_TRANSLUCENT.register(new ChestESPRenderer());

        ClientCommandRegistrationCallback.EVENT.register(PlayerESPCommand::register);
    }

    public static boolean isShowHitbox() { return shouldShowHitbox; }
    public static boolean isShowName() { return shouldShowName; }
    public static boolean isChestESPEnabled() { return chestESPEnabled; }

    public static void setShowHitbox(boolean value) { shouldShowHitbox = value; }
    public static void setShowName(boolean value) { shouldShowName = value; }
    public static void setChestESPEnabled(boolean value) { chestESPEnabled = value; }
}

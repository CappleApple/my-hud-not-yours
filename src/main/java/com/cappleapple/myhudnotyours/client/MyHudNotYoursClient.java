package com.cappleapple.myhudnotyours.client;

import com.cappleapple.myhudnotyours.MyHudNotYours;
import com.cappleapple.myhudnotyours.bar.BarSourceRegistry;
import com.cappleapple.myhudnotyours.config.LayoutStore;
import com.cappleapple.myhudnotyours.discovery.GenericLayerInterceptor;
import com.cappleapple.myhudnotyours.editor.HudEditorScreen;
import com.cappleapple.myhudnotyours.editor.TextureBrowserScreen;
import com.cappleapple.myhudnotyours.texture.ManagedTextureResolver;
import com.cappleapple.myhudnotyours.texture.TextureCatalog;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

@Mod(value = MyHudNotYours.MOD_ID, dist = Dist.CLIENT)
public final class MyHudNotYoursClient {
    private static boolean autoOpenEditor = Boolean.getBoolean("myhudnotyours.autoOpenEditor");
    private static boolean autoOpenTextureBrowser = Boolean.getBoolean("myhudnotyours.autoOpenTextureBrowser");
    private static final KeyMapping EDIT_HUD = new KeyMapping(
            "key.myhudnotyours.edit_hud",
            KeyConflictContext.UNIVERSAL,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.myhudnotyours");

    public MyHudNotYoursClient(IEventBus modBus, ModContainer ignored) {
        BarSourceRegistry.initializeOptionalIntegrations();
        LayoutStore.load();
        modBus.addListener(MyHudNotYoursClient::registerKeys);
        modBus.addListener(MyHudNotYoursClient::registerReloadListener);
        NeoForge.EVENT_BUS.addListener(GenericLayerInterceptor::onGuiPre);
        NeoForge.EVENT_BUS.addListener(GenericLayerInterceptor::onLayerPre);
        NeoForge.EVENT_BUS.addListener(GenericLayerInterceptor::onLayerPost);
        NeoForge.EVENT_BUS.addListener(MyHudNotYoursClient::clientTick);
        NeoForge.EVENT_BUS.addListener(MyHudNotYoursClient::loggedOut);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(EDIT_HUD);
    }

    private static void registerReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> {
            ManagedTextureResolver.get().clear();
            TextureCatalog.invalidate();
        });
    }

    private static void clientTick(ClientTickEvent.Post event) {
        LayoutStore.tick();
        Minecraft minecraft = Minecraft.getInstance();
        if (autoOpenTextureBrowser && minecraft.player != null && minecraft.screen == null) {
            autoOpenTextureBrowser = false;
            minecraft.setScreen(new TextureBrowserScreen(new HudEditorScreen(), ignored -> { }));
            MyHudNotYours.LOGGER.info("Opened texture browser through the development validation hook");
        }
        if (autoOpenEditor && minecraft.player != null && minecraft.screen == null) {
            autoOpenEditor = false;
            minecraft.setScreen(new HudEditorScreen());
            MyHudNotYours.LOGGER.info("Opened HUD editor through the development validation hook");
        }
        while (EDIT_HUD.consumeClick()) {
            if (minecraft.screen instanceof HudEditorScreen) minecraft.setScreen(null);
            else if (minecraft.screen == null && minecraft.player != null) minecraft.setScreen(new HudEditorScreen());
        }
    }

    private static void loggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        LayoutStore.saveNow();
    }
}

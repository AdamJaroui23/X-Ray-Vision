package com.adamjaroui.xrayvision;

import com.mojang.blaze3d.platform.InputConstants;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;

import com.adamjaroui.xrayvision.config.XRayConfig;
import com.adamjaroui.xrayvision.config.XRayConfigManager;
import com.adamjaroui.xrayvision.hud.XRayHud;
import com.adamjaroui.xrayvision.render.OreHighlightCache;
import com.adamjaroui.xrayvision.render.XRayBlocks;
import com.adamjaroui.xrayvision.render.XRayHighlightRenderer;

/**
 * Client entrypoint for X-Ray Vision.
 *
 * <p>The mod is client side only: it never sends packets, never writes to the world and never changes
 * block or chunk data. With X-Ray switched off, no mixin in this mod alters any return value and no
 * event handler does any work, so the game renders exactly as vanilla.
 */
public final class XRayVisionClient implements ClientModInitializer {
	public static final String MOD_ID = "xrayvision";
	public static final Logger LOGGER = LoggerFactory.getLogger("X-Ray Vision");

	public static final Identifier HUD_ELEMENT_ID = Identifier.fromNamespaceAndPath(MOD_ID, "status");

	/** Own category so the binding is easy to find in Options &rarr; Controls. */
	private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "xray"));

	/** The toggle keybinding. Bound to {@code X} by default and rebindable in the vanilla controls screen. */
	public static final KeyMapping TOGGLE_XRAY = KeyBindingHelper.registerKeyBinding(
			new KeyMapping("key.xrayvision.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, KEY_CATEGORY));

	@Override
	public void onInitializeClient() {
		XRayConfigManager.load(FabricLoader.getInstance().getConfigDir());

		ClientTickEvents.END_CLIENT_TICK.register(XRayVisionClient::onEndClientTick);
		ClientLifecycleEvents.CLIENT_STARTED.register(XRayVisionClient::onClientStarted);
		ClientLifecycleEvents.CLIENT_STOPPING.register(XRayVisionClient::onClientStopping);
		WorldRenderEvents.BEFORE_TRANSLUCENT.register(XRayHighlightRenderer::render);

		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, HUD_ELEMENT_ID, XRayHud.INSTANCE);

		LOGGER.info("X-Ray Vision loaded. Config: {}", XRayConfigManager.getConfigFile());
	}

	private static void onEndClientTick(Minecraft minecraft) {
		if (minecraft.screen == null && TOGGLE_XRAY.consumeClick()) {
			XRayState.toggle(minecraft);
		}

		XRayState.tick();

		if (XRayState.isEnabled() && XRayConfigManager.get().highlightingEnabled) {
			OreHighlightCache.INSTANCE.tick(minecraft);
		}
	}

	private static void onClientStarted(Minecraft minecraft) {
		// The block catalogue needs the registries, which are only ready once the game has started.
		XRayBlocks.build();
		applyConfiguredKey();
		XRayState.applyFromConfig(minecraft);
	}

	private static void onClientStopping(Minecraft minecraft) {
		XRayState.restoreSmartCull(minecraft);
		OreHighlightCache.INSTANCE.clear();

		XRayConfig config = XRayConfigManager.get();
		config.toggleKey = TOGGLE_XRAY.saveString();
		config.validate();
		XRayConfigManager.save();
	}

	/**
	 * Applies the key stored in the configuration file. An unparseable value keeps the current
	 * binding instead of resetting it.
	 */
	private static void applyConfiguredKey() {
		String keyName = XRayConfigManager.get().toggleKey;
		try {
			InputConstants.Key key = InputConstants.getKey(keyName);
			if (key != null) {
				TOGGLE_XRAY.setKey(key);
			}
		} catch (RuntimeException e) {
			LOGGER.warn("Ignoring invalid toggle key '{}' in the config file", keyName, e);
		}
	}
}

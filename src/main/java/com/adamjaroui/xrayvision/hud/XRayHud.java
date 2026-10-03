package com.adamjaroui.xrayvision.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;

import com.adamjaroui.xrayvision.XRayState;
import com.adamjaroui.xrayvision.config.XRayConfig;
import com.adamjaroui.xrayvision.config.XRayConfigManager;

/**
 * A small {@code X-RAY ON} / {@code X-RAY OFF} indicator in the top left corner of the screen.
 *
 * <p>Registered through {@code HudElementRegistry} (the non deprecated replacement for
 * {@code HudRenderCallback}) and attached before the chat layer, so it inherits the vanilla
 * {@code hideGui} condition and is not drawn while the debug screen hides the HUD.
 */
public final class XRayHud implements HudElement {
	public static final XRayHud INSTANCE = new XRayHud();

	private static final Component TEXT_ON = Component.literal("X-RAY ON");
	private static final Component TEXT_OFF = Component.literal("X-RAY OFF");

	private static final int COLOR_ON = 0xFF55FF55;
	private static final int COLOR_OFF = 0xFFAAAAAA;
	private static final int COLOR_FLASH = 0xFFFFFFFF;
	private static final int BACKDROP = 0x60000000;

	private static final int MARGIN = 3;

	private XRayHud() {
	}

	@Override
	public void render(GuiGraphics guiGraphics, DeltaTracker tickCounter) {
		XRayConfig config = XRayConfigManager.get();
		if (!config.hudEnabled) {
			return;
		}

		boolean enabled = XRayState.isEnabled();
		if (!enabled && config.hudHideWhenOff) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.options == null || minecraft.font == null) {
			return;
		}

		Component text = enabled ? TEXT_ON : TEXT_OFF;
		int color = enabled ? (XRayState.isNotifying() ? COLOR_FLASH : COLOR_ON) : COLOR_OFF;
		Font font = minecraft.font;
		int width = font.width(text);
		int textX = MARGIN;
		int textY = MARGIN;

		guiGraphics.fill(textX - 2, textY - 2, textX + width + 2, textY + font.lineHeight + 1, BACKDROP);
		guiGraphics.drawString(font, text, textX, textY, color, true);
	}
}

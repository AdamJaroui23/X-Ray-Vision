package com.adamjaroui.xrayvision.config.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.adamjaroui.xrayvision.XRayState;
import com.adamjaroui.xrayvision.config.XRayConfig;
import com.adamjaroui.xrayvision.config.XRayConfigManager;
import com.adamjaroui.xrayvision.render.XRayBlocks;

/**
 * The in-game configuration screen, opened from ModMenu (or programmatically).
 *
 * <p>Boolean options use vanilla {@link CycleButton}s and numeric options use {@link XRaySlider}, so
 * no extra GUI library is required. Values are edited live on the in-memory config and are persisted
 * (and any needed renderer state refreshed) when {@code Done} is pressed; {@code Cancel} reloads the
 * file from disk, discarding the edits.
 */
public final class XRayConfigScreen extends Screen {
	private static final int BUTTON_WIDTH = 150;
	private static final int BUTTON_HEIGHT = 20;
	private static final int GAP = 6;

	private final Screen parent;

	public XRayConfigScreen(Screen parent) {
		super(Component.literal("X-Ray Vision Settings"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		super.init();
		XRayConfig config = XRayConfigManager.get();
		int left = this.width / 2 - BUTTON_WIDTH - GAP / 2;
		int right = this.width / 2 + GAP / 2;
		int y = this.height / 6;

		// Row 1: master switch + HUD
		this.addRenderableWidget(CycleButton.booleanBuilder(Component.literal("On"), Component.literal("Off"), config.enabled)
				.create(left, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.literal("X-Ray"),
						(button, value) -> XRayState.setEnabled(this.minecraft, value)));
		this.addRenderableWidget(CycleButton.booleanBuilder(Component.literal("On"), Component.literal("Off"), config.hudEnabled)
				.create(right, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.literal("HUD Indicator"),
						(button, value) -> config.hudEnabled = value));
		y += BUTTON_HEIGHT + GAP;

		// Row 2: highlighting + outlines
		this.addRenderableWidget(CycleButton.booleanBuilder(Component.literal("On"), Component.literal("Off"), config.highlightingEnabled)
				.create(left, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.literal("Highlighting"),
						(button, value) -> config.highlightingEnabled = value));
		this.addRenderableWidget(CycleButton.booleanBuilder(Component.literal("On"), Component.literal("Off"), config.outlinesEnabled)
				.create(right, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.literal("Block Outlines"),
						(button, value) -> config.outlinesEnabled = value));
		y += BUTTON_HEIGHT + GAP;

		// Row 3: fill + hide HUD when off
		this.addRenderableWidget(CycleButton.booleanBuilder(Component.literal("On"), Component.literal("Off"), config.fillEnabled)
				.create(left, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.literal("Colour Fill"),
						(button, value) -> config.fillEnabled = value));
		this.addRenderableWidget(CycleButton.booleanBuilder(Component.literal("On"), Component.literal("Off"), config.hudHideWhenOff)
				.create(right, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.literal("Hide HUD When Off"),
						(button, value) -> config.hudHideWhenOff = value));
		y += BUTTON_HEIGHT + GAP + 4;

		// Row 4: opacity slider (full width)
		this.addRenderableWidget(new XRaySlider(left, y, BUTTON_WIDTH * 2 + GAP, BUTTON_HEIGHT,
				0.0D, 1.0D, config.highlightOpacity,
				v -> config.highlightOpacity = (float) v,
				v -> Component.literal("Highlight Opacity: " + Math.round(v * 100.0D) + "%")));
		y += BUTTON_HEIGHT + GAP;

		// Row 5: scan radius + max highlights
		this.addRenderableWidget(new XRaySlider(left, y, BUTTON_WIDTH, BUTTON_HEIGHT,
				16, 256, config.scanRadius,
				v -> config.scanRadius = (int) Math.round(v),
				v -> Component.literal("Scan Radius: " + (int) Math.round(v))));
		this.addRenderableWidget(new XRaySlider(right, y, BUTTON_WIDTH, BUTTON_HEIGHT,
				0, 8192, config.maxHighlights,
				v -> config.maxHighlights = (int) Math.round(v),
				v -> Component.literal("Max Highlights: " + (int) Math.round(v))));
		y += BUTTON_HEIGHT + GAP + 6;

		// Row 6: block list
		this.addRenderableWidget(Button.builder(Component.literal("Visible Blocks..."), button ->
						this.minecraft.setScreen(new XRayBlockListScreen(this)))
				.bounds(left, y, BUTTON_WIDTH * 2 + GAP, BUTTON_HEIGHT).build());
		y = this.height - BUTTON_HEIGHT - 10;

		// Bottom: done / cancel
		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.finish(true))
				.bounds(this.width / 2 - BUTTON_WIDTH - GAP / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
		this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> this.finish(false))
				.bounds(this.width / 2 + GAP / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
	}

	private void finish(boolean save) {
		if (save) {
			XRayConfig config = XRayConfigManager.get();
			config.validate();
			XRayConfigManager.save();
			XRayBlocks.refresh();

			if (XRayState.isEnabled() && this.minecraft.levelRenderer != null) {
				this.minecraft.levelRenderer.allChanged();
			}
		} else {
			// Discard edits by reloading whatever is on disk.
			XRayConfigManager.load(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir());
			XRayBlocks.refresh();
		}

		this.minecraft.setScreen(this.parent);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics, mouseX, mouseY, partialTick);
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}

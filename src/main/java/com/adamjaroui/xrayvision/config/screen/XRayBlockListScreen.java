package com.adamjaroui.xrayvision.config.screen;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.adamjaroui.xrayvision.config.XRayConfig;
import com.adamjaroui.xrayvision.config.XRayConfigManager;
import com.adamjaroui.xrayvision.render.XRayBlocks;

/**
 * A paginated screen for choosing which blocks X-Ray Vision keeps visible.
 *
 * <p>Each row shows a block identifier and a {@code Visible/Hidden} toggle. Changes are applied to the
 * in-memory config immediately and persisted by the parent screen's {@code Done} button.
 */
public final class XRayBlockListScreen extends Screen {
	private static final int ROWS_PER_PAGE = 8;
	private static final int ROW_HEIGHT = 22;
	private static final int TOGGLE_WIDTH = 80;

	private final Screen parent;
	private int page = 0;
	private int pageCount = 1;

	public XRayBlockListScreen(Screen parent) {
		super(Component.literal("Visible Blocks"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		super.init();
		this.clearWidgets();

		XRayConfig config = XRayConfigManager.get();
		List<XRayBlocks.Entry> entries = XRayBlocks.entries();
		this.pageCount = Math.max(1, (entries.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
		if (this.page >= this.pageCount) {
			this.page = this.pageCount - 1;
		}

		int left = this.width / 2 - 170;
		int y = this.height / 6;

		int start = this.page * ROWS_PER_PAGE;
		for (int i = start; i < Math.min(entries.size(), start + ROWS_PER_PAGE); i++) {
			XRayBlocks.Entry entry = entries.get(i);
			boolean enabled = config.isBlockEnabled(entry.id());
			int rowY = y + (i - start) * ROW_HEIGHT;

			this.addRenderableWidget(new StringWidget(left, rowY + 5, 220, 10,
					Component.literal(entry.id()), this.font));

			this.addRenderableWidget(Button.builder(
							Component.literal(enabled ? "Visible" : "Hidden"),
							button -> {
								config.setBlockEnabled(entry.id(), !config.isBlockEnabled(entry.id()));
								this.rebuildWidgets();
							})
					.bounds(left + 250, rowY, TOGGLE_WIDTH, 20).build());
		}

		int bottom = this.height - 30;

		Button prev = Button.builder(Component.literal("< Prev"), button -> {
			this.page--;
			this.rebuildWidgets();
		}).bounds(left, bottom, 70, 20).build();
		prev.active = this.page > 0;
		this.addRenderableWidget(prev);

		Button next = Button.builder(Component.literal("Next >"), button -> {
			this.page++;
			this.rebuildWidgets();
		}).bounds(left + 76, bottom, 70, 20).build();
		next.active = this.page < this.pageCount - 1;
		this.addRenderableWidget(next);

		this.addRenderableWidget(Button.builder(Component.translatable("gui.back"),
						button -> this.minecraft.setScreen(this.parent))
				.bounds(this.width / 2 + 60, bottom, 110, 20).build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics, mouseX, mouseY, partialTick);
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
		graphics.drawString(this.font, "Page " + (this.page + 1) + "/" + this.pageCount,
				this.width / 2 + 120, 14, 0xAAAAAA, true);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}

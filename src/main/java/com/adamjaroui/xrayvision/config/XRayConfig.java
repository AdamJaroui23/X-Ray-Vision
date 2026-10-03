package com.adamjaroui.xrayvision.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Persistent, user editable settings for X-Ray Vision.
 *
 * <p>This class is deliberately free of Minecraft types so that it can be unit tested on a plain JVM.
 * File handling lives in {@link XRayConfigManager}, block resolution lives in
 * {@code com.adamjaroui.xrayvision.render.XRayBlocks}.
 */
public final class XRayConfig {
	/** Bumped whenever the on-disk layout changes in an incompatible way. */
	public static final int CURRENT_VERSION = 1;

	public static final String DEFAULT_TOGGLE_KEY = "key.keyboard.x";

	public static final float MIN_HIGHLIGHT_OPACITY = 0.0F;
	public static final float MAX_HIGHLIGHT_OPACITY = 1.0F;
	public static final int MIN_SCAN_RADIUS = 16;
	public static final int MAX_SCAN_RADIUS = 256;
	public static final int MIN_MAX_HIGHLIGHTS = 0;
	public static final int MAX_MAX_HIGHLIGHTS = 65536;

	/** Layout version written to disk; used to detect files from an older release. */
	public int configVersion = CURRENT_VERSION;

	/** Master switch. When {@code false} the mod does not touch rendering at all. */
	public boolean enabled = false;

	/** Name of the {@code InputConstants.Key} bound to the toggle, e.g. {@code key.keyboard.x}. */
	public String toggleKey = DEFAULT_TOGGLE_KEY;

	/** Draws the coloured ESP overlay on top of the visible blocks. */
	public boolean highlightingEnabled = true;

	/** Draws a coloured wireframe box around every visible block. */
	public boolean outlinesEnabled = true;

	/** Draws a translucent coloured box inside every visible block. */
	public boolean fillEnabled = true;

	/** Alpha used by the translucent fill, in the range {@code [0, 1]}. */
	public float highlightOpacity = 0.22F;

	/** Shows the small {@code X-RAY ON/OFF} indicator in the top left corner. */
	public boolean hudEnabled = true;

	/** Hides the HUD indicator while X-Ray is off, so nothing is drawn during normal play. */
	public boolean hudHideWhenOff = false;

	/** Horizontal radius, in blocks, around the player that is scanned for highlights. */
	public int scanRadius = 48;

	/** Upper bound on the number of highlight boxes drawn in a single frame. */
	public int maxHighlights = 4096;

	/**
	 * Per block visibility, keyed by block identifier such as {@code minecraft:diamond_ore}.
	 * A block that is absent from this map is treated as visible.
	 */
	public Map<String, Boolean> blocks = new LinkedHashMap<>();

	/**
	 * @return {@code true} when the block id should stay visible while X-Ray is active
	 */
	public boolean isBlockEnabled(String blockId) {
		Boolean value = this.blocks.get(blockId);
		return value == null || value.booleanValue();
	}

	public void setBlockEnabled(String blockId, boolean visible) {
		this.blocks.put(blockId, Boolean.valueOf(visible));
	}

	/**
	 * Clamps every value into the supported range. Called after loading and before saving so that a
	 * hand edited file can never put the mod into an invalid state.
	 */
	public void validate() {
		if (this.toggleKey == null || this.toggleKey.isBlank()) {
			this.toggleKey = DEFAULT_TOGGLE_KEY;
		}

		if (Float.isNaN(this.highlightOpacity)) {
			this.highlightOpacity = 0.22F;
		}
		this.highlightOpacity = clamp(this.highlightOpacity, MIN_HIGHLIGHT_OPACITY, MAX_HIGHLIGHT_OPACITY);
		this.scanRadius = clamp(this.scanRadius, MIN_SCAN_RADIUS, MAX_SCAN_RADIUS);
		this.maxHighlights = clamp(this.maxHighlights, MIN_MAX_HIGHLIGHTS, MAX_MAX_HIGHLIGHTS);

		if (this.blocks == null) {
			this.blocks = new LinkedHashMap<>();
		}
		this.blocks.remove(null);
	}

	private static float clamp(float value, float min, float max) {
		return value < min ? min : (value > max ? max : value);
	}

	private static int clamp(int value, int min, int max) {
		return value < min ? min : (value > max ? max : value);
	}

	/**
	 * @return an alpha value in {@code [0, 255]} derived from {@link #highlightOpacity}
	 */
	public int fillAlpha() {
		int alpha = Math.round(this.highlightOpacity * 255.0F);
		return alpha < 0 ? 0 : (alpha > 255 ? 255 : alpha);
	}
}

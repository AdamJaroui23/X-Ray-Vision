package com.adamjaroui.xrayvision.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure JVM tests for the configuration model. {@link XRayConfig} intentionally has no Minecraft
 * dependency so that it can run anywhere Gradle runs a {@code test} task.
 */
class XRayConfigTest {
	@Test
	void defaultsAreSane() {
		XRayConfig config = new XRayConfig();

		assertFalse(config.enabled, "X-Ray must start disabled");
		assertTrue(config.highlightingEnabled, "highlighting on by default");
		assertTrue(config.outlinesEnabled, "outlines on by default");
		assertTrue(config.fillEnabled, "fill on by default");
		assertTrue(config.hudEnabled, "HUD on by default");
		assertEquals(XRayConfig.DEFAULT_TOGGLE_KEY, config.toggleKey);
		assertEquals(XRayConfig.CURRENT_VERSION, config.configVersion);
	}

	@Test
	void validateClampsOpacity() {
		XRayConfig config = new XRayConfig();
		config.highlightOpacity = 5.0F;
		config.validate();
		assertEquals(XRayConfig.MAX_HIGHLIGHT_OPACITY, config.highlightOpacity);

		config.highlightOpacity = -2.0F;
		config.validate();
		assertEquals(XRayConfig.MIN_HIGHLIGHT_OPACITY, config.highlightOpacity);

		config.highlightOpacity = Float.NaN;
		config.validate();
		assertFalse(Float.isNaN(config.highlightOpacity), "NaN must be replaced");
	}

	@Test
	void validateClampsScanRadiusAndMaxHighlights() {
		XRayConfig config = new XRayConfig();
		config.scanRadius = 100000;
		config.maxHighlights = -5;
		config.validate();
		assertEquals(XRayConfig.MAX_SCAN_RADIUS, config.scanRadius);
		assertEquals(XRayConfig.MIN_MAX_HIGHLIGHTS, config.maxHighlights);
	}

	@Test
	void validateFixesBlankKey() {
		XRayConfig config = new XRayConfig();
		config.toggleKey = "   ";
		config.validate();
		assertEquals(XRayConfig.DEFAULT_TOGGLE_KEY, config.toggleKey);
	}

	@Test
	void unknownBlockIsVisibleByDefault() {
		XRayConfig config = new XRayConfig();
		assertTrue(config.isBlockEnabled("minecraft:diamond_ore"), "absent entries are visible");

		config.setBlockEnabled("minecraft:diamond_ore", false);
		assertFalse(config.isBlockEnabled("minecraft:diamond_ore"));

		config.setBlockEnabled("minecraft:diamond_ore", true);
		assertTrue(config.isBlockEnabled("minecraft:diamond_ore"));
	}

	@Test
	void fillAlphaIsScaledToEightBits() {
		XRayConfig config = new XRayConfig();
		config.highlightOpacity = 1.0F;
		config.validate();
		assertEquals(255, config.fillAlpha());

		config.highlightOpacity = 0.0F;
		config.validate();
		assertEquals(0, config.fillAlpha());

		config.highlightOpacity = 0.5F;
		config.validate();
		assertTrue(config.fillAlpha() >= 127 && config.fillAlpha() <= 128);
	}
}

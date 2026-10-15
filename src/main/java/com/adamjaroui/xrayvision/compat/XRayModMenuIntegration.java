package com.adamjaroui.xrayvision.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import com.adamjaroui.xrayvision.config.screen.XRayConfigScreen;

/**
 * ModMenu integration. ModMenu is an optional compile-only dependency: when it is present it calls
 * {@link #getModConfigScreenFactory()} and shows a "Config" button for X-Ray Vision; when it is absent
 * this class is simply never loaded and the mod runs without it.
 */
public final class XRayModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return XRayConfigScreen::new;
	}
}

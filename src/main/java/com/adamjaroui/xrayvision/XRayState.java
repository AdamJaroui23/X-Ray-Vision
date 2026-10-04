package com.adamjaroui.xrayvision;

import net.minecraft.client.Minecraft;

import com.adamjaroui.xrayvision.config.XRayConfigManager;
import com.adamjaroui.xrayvision.render.OreHighlightCache;

/**
 * Runtime state of the X-Ray feature.
 *
 * <p>Everything here happens on the client thread only. Turning the feature on or off never touches
 * world data, chunk data or block states: the only vanilla state that is modified is
 * {@link Minecraft#smartCull}, and the previous value is restored on disable and on shutdown.
 */
public final class XRayState {
	/** How long the HUD indicator stays highlighted after a toggle, in client ticks. */
	private static final int NOTIFICATION_TICKS = 40;

	private static boolean enabled;
	private static int notificationTicks;

	private static boolean smartCullOverridden;
	private static boolean smartCullBefore = true;

	private XRayState() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	/**
	 * @return {@code true} while the HUD indicator should be drawn in its "just toggled" style
	 */
	public static boolean isNotifying() {
		return notificationTicks > 0;
	}

	/**
	 * Counts the toggle notification down. Called once per client tick.
	 */
	public static void tick() {
		if (notificationTicks > 0) {
			notificationTicks--;
		}
	}

	public static void toggle(Minecraft minecraft) {
		setEnabled(minecraft, !enabled);
	}

	/**
	 * Enables or disables the X-Ray effect and rebuilds every loaded chunk section so the change is
	 * visible immediately, without a world reload.
	 */
	public static void setEnabled(Minecraft minecraft, boolean value) {
		if (enabled == value) {
			notificationTicks = NOTIFICATION_TICKS;
			return;
		}

		enabled = value;
		notificationTicks = NOTIFICATION_TICKS;

		if (value) {
			// Occlusion culling would otherwise stop the traversal at the first fully solid section,
			// which would hide everything beyond the player's own chunk section. Vanilla and Sodium
			// both read this field, so no mixin is needed.
			if (!smartCullOverridden) {
				smartCullBefore = minecraft.smartCull;
				smartCullOverridden = true;
			}

			minecraft.smartCull = false;
		} else {
			restoreSmartCull(minecraft);
			OreHighlightCache.INSTANCE.clear();
		}

		// Re-mesh every loaded section: the block -> render shape mapping just changed.
		if (minecraft.levelRenderer != null) {
			minecraft.levelRenderer.allChanged();
		}

		XRayConfigManager.get().enabled = value;
		XRayConfigManager.save();
	}

	/**
	 * Applies the persisted state once the client has started.
	 */
	public static void applyFromConfig(Minecraft minecraft) {
		if (XRayConfigManager.get().enabled) {
			setEnabled(minecraft, true);
		}
	}

	/**
	 * Restores {@link Minecraft#smartCull} to whatever the player had before the mod touched it.
	 */
	public static void restoreSmartCull(Minecraft minecraft) {
		if (smartCullOverridden) {
			minecraft.smartCull = smartCullBefore;
			smartCullOverridden = false;
		}
	}
}

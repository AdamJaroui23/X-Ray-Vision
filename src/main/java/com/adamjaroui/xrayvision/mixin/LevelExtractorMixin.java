package com.adamjaroui.xrayvision.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.core.BlockPos;

import com.adamjaroui.xrayvision.XRayState;
import com.adamjaroui.xrayvision.config.XRayConfigManager;
import com.adamjaroui.xrayvision.render.OreHighlightCache;

/**
 * Keeps the highlight cache honest.
 *
 * <p>In Minecraft 26.x the block change notification moved from {@code LevelRenderer.blockChanged} to
 * {@code LevelExtractor.blockChanged}, which {@code ClientLevel} calls for every block change the
 * client applies. That makes it the natural place to mark the affected chunk column stale. The column
 * is then rescanned lazily by {@link OreHighlightCache} within its normal per tick budget, so placing
 * or breaking a block never triggers an immediate rescan and never touches the render thread.
 */
@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin {
	@Inject(method = "blockChanged", at = @At("HEAD"))
	private void xrayvision$onBlockChanged(BlockPos pos, int updateFlags, CallbackInfo ci) {
		if (XRayState.isEnabled() && XRayConfigManager.get().highlightingEnabled) {
			OreHighlightCache.INSTANCE.invalidate(pos);
		}
	}
}

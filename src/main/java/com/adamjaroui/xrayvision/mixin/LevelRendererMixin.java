package com.adamjaroui.xrayvision.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

import com.adamjaroui.xrayvision.XRayState;
import com.adamjaroui.xrayvision.config.XRayConfigManager;
import com.adamjaroui.xrayvision.render.OreHighlightCache;

/**
 * Keeps the highlight cache honest.
 *
 * <p>{@code LevelRenderer.blockChanged} is called by {@code ClientLevel} for every block change the
 * client applies, which makes it the natural place to mark the affected chunk column stale. The
 * column is then rescanned lazily by {@link OreHighlightCache} within its normal per tick budget, so
 * placing or breaking a block never triggers an immediate rescan and never touches the render thread.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
	@Inject(method = "blockChanged", at = @At("HEAD"))
	private void xrayvision$onBlockChanged(BlockGetter level, BlockPos pos, BlockState oldState, BlockState newState, int updateFlags, CallbackInfo ci) {
		if (XRayState.isEnabled() && XRayConfigManager.get().highlightingEnabled) {
			OreHighlightCache.INSTANCE.invalidate(pos);
		}
	}
}

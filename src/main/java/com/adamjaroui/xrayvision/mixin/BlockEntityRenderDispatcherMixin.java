package com.adamjaroui.xrayvision.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.adamjaroui.xrayvision.XRayState;
import com.adamjaroui.xrayvision.render.XRayBlocks;

/**
 * Companion to {@link BlockBehaviourMixin}.
 *
 * <p>Blocks with a block entity (chests, furnaces, signs, banners, beds, ...) keep being drawn by
 * their {@code BlockEntityRenderer} even when their render shape is {@code INVISIBLE}, because the
 * block entity list is collected from {@code hasBlockEntity()} instead. Without this hook a hidden
 * furnace or sign would still float in view through the terrain.
 *
 * <p>{@code tryExtractRenderState} is the choke point used by both the vanilla
 * {@code LevelRenderer} and Sodium's {@code SodiumWorldRenderer}, so returning {@code null} suppresses
 * the block entity render in both. Blocks on the X-Ray list keep their render state and therefore
 * still show their chest lid, spawner cage or shulker shell.
 */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
	@ModifyReturnValue(method = "tryExtractRenderState", at = @At("RETURN"))
	private BlockEntityRenderState xrayvision$hideBlockEntities(BlockEntityRenderState original, BlockEntity blockEntity) {
		if (original == null || !XRayState.isEnabled() || !XRayBlocks.isBuilt()) {
			return original;
		}

		return XRayBlocks.isVisible(blockEntity.getBlockState().getBlock()) ? original : null;
	}
}

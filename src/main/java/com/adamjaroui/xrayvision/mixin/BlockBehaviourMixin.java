package com.adamjaroui.xrayvision.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import com.adamjaroui.xrayvision.XRayState;
import com.adamjaroui.xrayvision.render.XRayBlocks;

/**
 * The terrain hiding hook.
 *
 * <p>{@code getRenderShape} is the single point that decides whether a block contributes geometry to
 * a chunk section mesh. {@code BlockState#getRenderShape()} simply delegates here, and in Minecraft
 * 26.x both the vanilla section compiler ({@code SectionCompiler}) and Sodium
 * ({@code ChunkBuilderMeshingTask}) skip a block unless it returns {@link RenderShape#MODEL}, so
 * returning {@link RenderShape#INVISIBLE} removes the block from the chunk mesh in both renderers.
 *
 * <p>Nothing is written to the world: the block state, the chunk data and the block itself are
 * untouched. Only the client side render shape answer changes, and it changes back the moment the
 * feature is switched off.
 */
@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourMixin {
	@ModifyReturnValue(method = "getRenderShape", at = @At("RETURN"))
	private RenderShape xrayvision$hideTerrain(RenderShape original, BlockState state) {
		if (original != RenderShape.MODEL || !XRayState.isEnabled() || !XRayBlocks.isBuilt()) {
			return original;
		}

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null) {
			return original;
		}

		// Resource pack reloads read getRenderShape while grouping block models. Answering INVISIBLE
		// there would corrupt the model groups, so stay out of the way until the reload finishes.
		if (minecraft.gui.overlay() != null) {
			return original;
		}

		return XRayBlocks.isVisible(state.getBlock()) ? original : RenderShape.INVISIBLE;
	}
}

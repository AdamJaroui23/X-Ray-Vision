package com.adamjaroui.xrayvision.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

import com.adamjaroui.xrayvision.XRayState;
import com.adamjaroui.xrayvision.config.XRayConfig;
import com.adamjaroui.xrayvision.config.XRayConfigManager;
import com.adamjaroui.xrayvision.render.OreHighlightCache.ChunkHighlights;

/**
 * Draws the ESP style highlight over every cached X-Ray target block.
 *
 * <p>Minecraft 26.x replaced immediate mode rendering ({@code MultiBufferSource}) with a submit node
 * system, so the highlight is emitted through the {@link SubmitNodeCollector} from
 * {@code LevelRenderEvents.COLLECT_SUBMITS}: the wireframe uses vanilla's own
 * {@code submitShapeOutline} over {@link Shapes#block()}, and the
 * translucent fill is a small custom geometry submission. Both are batched and drawn by vanilla
 * together with the rest of the frame, and neither writes depth in a way that occludes the block it
 * surrounds.
 *
 * <p>Per frame this iterates the cached arrays only. Nothing is scanned, allocated or re-derived here.
 */
public final class XRayHighlightRenderer {
	/**
	 * Pulls the fill in by a fraction of a block so it does not z-fight with the block faces.
	 */
	private static final double INSET = 0.0015D;

	private XRayHighlightRenderer() {
	}

	public static void collect(LevelRenderContext context) {
		if (!XRayState.isEnabled()) {
			return;
		}

		XRayConfig config = XRayConfigManager.get();
		if (!config.highlightingEnabled || (!config.outlinesEnabled && !config.fillEnabled)) {
			return;
		}

		int maxHighlights = config.maxHighlights;
		if (maxHighlights <= 0) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.player == null || !XRayBlocks.isBuilt()) {
			return;
		}

		OreHighlightCache cache = OreHighlightCache.INSTANCE;
		if (cache.chunks().isEmpty()) {
			return;
		}

		SubmitNodeCollector collector = context.submitNodeCollector();
		if (collector == null) {
			return;
		}

		Vec3 camera = context.levelState().cameraRenderState.pos;
		double maxDistanceSq = (double) config.scanRadius * config.scanRadius;
		int fillAlpha = config.fillAlpha();
		float lineWidth = context.gameRenderer().gameRenderState().windowRenderState.appropriateLineWidth;

		PoseStack poseStack = context.poseStack();
		RenderType fillType = RenderTypes.debugFilledBox();
		// The same line render types vanilla uses for the targeted block outline.
		RenderType lineType = minecraft.gameRenderer.useImprovedTransparency()
				? RenderTypes.linesTranslucentNoDepthWrite()
				: RenderTypes.linesTranslucent();

		int drawn = 0;

		for (ChunkHighlights highlights : cache.chunks().values()) {
			int size = highlights.size();

			for (int i = 0; i < size; i++) {
				if (drawn >= maxHighlights) {
					return;
				}

				long packed = highlights.position(i);
				int x = BlockPos.getX(packed);
				int y = BlockPos.getY(packed);
				int z = BlockPos.getZ(packed);

				double dx = x + 0.5D - camera.x;
				double dy = y + 0.5D - camera.y;
				double dz = z + 0.5D - camera.z;
				if (dx * dx + dy * dy + dz * dz > maxDistanceSq) {
					continue;
				}

				int color = highlights.color(i);

				poseStack.pushPose();
				poseStack.translate(x - camera.x, y - camera.y, z - camera.z);

				if (config.fillEnabled) {
					int fillColor = ARGB.color(fillAlpha, ARGB.red(color), ARGB.green(color), ARGB.blue(color));
					collector.submitCustomGeometry(poseStack, fillType,
							(pose, consumer) -> drawFilledBox(pose, consumer, fillColor));
				}

				if (config.outlinesEnabled) {
					int lineColor = ARGB.color(255, ARGB.red(color), ARGB.green(color), ARGB.blue(color));
					collector.submitShapeOutline(poseStack, Shapes.block(), lineType, lineColor, lineWidth, false);
				}

				poseStack.popPose();
				drawn++;
			}
		}
	}

	/**
	 * Draws the six faces of the unit cube at the current pose into a
	 * {@link RenderTypes#debugFilledBox()} buffer. 24 vertices, no allocation.
	 */
	private static void drawFilledBox(PoseStack.Pose pose, VertexConsumer consumer, int color) {
		float x0 = (float) INSET;
		float y0 = (float) INSET;
		float z0 = (float) INSET;
		float x1 = (float) (1.0D - INSET);
		float y1 = (float) (1.0D - INSET);
		float z1 = (float) (1.0D - INSET);

		quad(pose, consumer, x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0, color);
		quad(pose, consumer, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, color);
		quad(pose, consumer, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, color);
		quad(pose, consumer, x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1, color);
		quad(pose, consumer, x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, color);
		quad(pose, consumer, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, color);
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer consumer, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz, int color) {
		consumer.addVertex(pose, ax, ay, az).setColor(color);
		consumer.addVertex(pose, bx, by, bz).setColor(color);
		consumer.addVertex(pose, cx, cy, cz).setColor(color);
		consumer.addVertex(pose, dx, dy, dz).setColor(color);
	}
}

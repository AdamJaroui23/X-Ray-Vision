package com.adamjaroui.xrayvision.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;

import com.adamjaroui.xrayvision.XRayState;
import com.adamjaroui.xrayvision.config.XRayConfig;
import com.adamjaroui.xrayvision.config.XRayConfigManager;
import com.adamjaroui.xrayvision.render.OreHighlightCache.ChunkHighlights;

/**
 * Draws the ESP style highlight over every cached X-Ray target block.
 *
 * <p>The overlay is emitted into the world renderer's own {@link MultiBufferSource} from
 * {@code WorldRenderEvents.BEFORE_TRANSLUCENT}, so it is batched and flushed by vanilla together with
 * the rest of the frame. Only vanilla render types are used: {@link RenderTypes#lines()} for the
 * wireframe and {@link RenderTypes#debugFilledBox()} for the translucent fill, which does not write
 * depth and therefore never occludes the block it surrounds.
 *
 * <p>Per frame this iterates the cached arrays only. Nothing is scanned, allocated or re-derived here.
 */
public final class XRayHighlightRenderer {
	/**
	 * Pulls the outline in by a fraction of a block so it does not z-fight with the block faces.
	 */
	private static final double INSET = 0.0015D;

	private XRayHighlightRenderer() {
	}

	public static void render(WorldRenderContext context) {
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

		Vec3 camera = context.worldState().cameraRenderState.pos;
		if (camera == null) {
			return;
		}

		double maxDistanceSq = (double) config.scanRadius * config.scanRadius;
		int fillAlpha = config.fillAlpha();
		float lineWidth = minecraft.getWindow().getAppropriateLineWidth();

		PoseStack poseStack = context.matrices();
		MultiBufferSource consumers = context.consumers();

		poseStack.pushPose();
		// Everything downstream is emitted in world coordinates, so shift by the camera position.
		poseStack.translate(-camera.x, -camera.y, -camera.z);
		PoseStack.Pose pose = poseStack.last();

		VertexConsumer lines = config.outlinesEnabled ? consumers.getBuffer(RenderTypes.lines()) : null;
		VertexConsumer fill = config.fillEnabled ? consumers.getBuffer(RenderTypes.debugFilledBox()) : null;

		int drawn = 0;

		for (ChunkHighlights highlights : cache.chunks().values()) {
			int size = highlights.size();

			for (int i = 0; i < size; i++) {
				if (drawn >= maxHighlights) {
					poseStack.popPose();
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
				if (fill != null) {
					drawFilledBox(pose, fill, x, y, z, ARGB.color(fillAlpha, ARGB.red(color), ARGB.green(color), ARGB.blue(color)));
				}

				if (lines != null) {
					drawBoxOutline(pose, lines, x, y, z, ARGB.color(255, ARGB.red(color), ARGB.green(color), ARGB.blue(color)), lineWidth);
				}

				drawn++;
			}
		}

		poseStack.popPose();
	}

	/**
	 * Draws the 12 edges of the unit cube at {@code (x, y, z)} into a {@link RenderTypes#lines()}
	 * buffer. 24 vertices, no allocation.
	 */
	private static void drawBoxOutline(PoseStack.Pose pose, VertexConsumer consumer, int x, int y, int z, int color, float lineWidth) {
		float x0 = (float) (x + INSET);
		float y0 = (float) (y + INSET);
		float z0 = (float) (z + INSET);
		float x1 = (float) (x + 1.0D - INSET);
		float y1 = (float) (y + 1.0D - INSET);
		float z1 = (float) (z + 1.0D - INSET);

		// Bottom face
		line(pose, consumer, x0, y0, z0, x1, y0, z0, color, 0.0F, 1.0F, 0.0F, lineWidth);
		line(pose, consumer, x1, y0, z0, x1, y0, z1, color, 0.0F, 1.0F, 0.0F, lineWidth);
		line(pose, consumer, x1, y0, z1, x0, y0, z1, color, 0.0F, 1.0F, 0.0F, lineWidth);
		line(pose, consumer, x0, y0, z1, x0, y0, z0, color, 0.0F, 1.0F, 0.0F, lineWidth);

		// Top face
		line(pose, consumer, x0, y1, z0, x1, y1, z0, color, 0.0F, -1.0F, 0.0F, lineWidth);
		line(pose, consumer, x1, y1, z0, x1, y1, z1, color, 0.0F, -1.0F, 0.0F, lineWidth);
		line(pose, consumer, x1, y1, z1, x0, y1, z1, color, 0.0F, -1.0F, 0.0F, lineWidth);
		line(pose, consumer, x0, y1, z1, x0, y1, z0, color, 0.0F, -1.0F, 0.0F, lineWidth);

		// Vertical edges
		line(pose, consumer, x0, y0, z0, x0, y1, z0, color, 1.0F, 0.0F, 0.0F, lineWidth);
		line(pose, consumer, x1, y0, z0, x1, y1, z0, color, 1.0F, 0.0F, 0.0F, lineWidth);
		line(pose, consumer, x1, y0, z1, x1, y1, z1, color, 1.0F, 0.0F, 0.0F, lineWidth);
		line(pose, consumer, x0, y0, z1, x0, y1, z1, color, 1.0F, 0.0F, 0.0F, lineWidth);
	}

	private static void line(PoseStack.Pose pose, VertexConsumer consumer, float x0, float y0, float z0, float x1, float y1, float z1, int color, float nx, float ny, float nz, float lineWidth) {
		consumer.addVertex(pose, x0, y0, z0).setColor(color).setNormal(nx, ny, nz).setLineWidth(lineWidth);
		consumer.addVertex(pose, x1, y1, z1).setColor(color).setNormal(nx, ny, nz).setLineWidth(lineWidth);
	}

	/**
	 * Draws the six faces of the unit cube at {@code (x, y, z)} into a
	 * {@link RenderTypes#debugFilledBox()} buffer. 24 vertices, no allocation.
	 */
	private static void drawFilledBox(PoseStack.Pose pose, VertexConsumer consumer, int x, int y, int z, int color) {
		float x0 = (float) (x + INSET);
		float y0 = (float) (y + INSET);
		float z0 = (float) (z + INSET);
		float x1 = (float) (x + 1.0D - INSET);
		float y1 = (float) (y + 1.0D - INSET);
		float z1 = (float) (z + 1.0D - INSET);

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

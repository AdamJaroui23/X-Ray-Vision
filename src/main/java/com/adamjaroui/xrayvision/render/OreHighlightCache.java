package com.adamjaroui.xrayvision.render;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import com.adamjaroui.xrayvision.config.XRayConfigManager;

/**
 * A cache of every X-Ray target block found in the chunk columns around the player.
 *
 * <p>The world is never scanned per frame. A chunk column is scanned at most once, at most
 * {@link #CHUNKS_SCANNED_PER_TICK} columns per tick so that no single tick stalls, and the result is
 * reused until the column leaves the scan radius, is unloaded, or a block inside it changes. Changing
 * dimension, disabling X-Ray or editing the block list drops the cache entirely.
 *
 * <p>All methods run on the client thread.
 */
public final class OreHighlightCache {
	public static final OreHighlightCache INSTANCE = new OreHighlightCache();

	private static final int CHUNKS_SCANNED_PER_TICK = 2;

	private final Long2ObjectOpenHashMap<ChunkHighlights> chunks = new Long2ObjectOpenHashMap<>();
	private ClientLevel trackedLevel;

	private OreHighlightCache() {
	}

	/**
	 * The highlights found in a single chunk column: packed block positions and their colours.
	 */
	public static final class ChunkHighlights {
		private final long[] positions;
		private final int[] colors;
		private boolean dirty;

		ChunkHighlights(long[] positions, int[] colors) {
			this.positions = positions;
			this.colors = colors;
		}

		public int size() {
			return this.positions.length;
		}

		public long position(int index) {
			return this.positions[index];
		}

		public int color(int index) {
			return this.colors[index];
		}
	}

	/**
	 * Drops every cached highlight, for example on a dimension change or when X-Ray is switched off.
	 */
	public void clear() {
		this.chunks.clear();
		this.trackedLevel = null;
	}

	/**
	 * Marks the column containing {@code pos} as stale so it is rescanned on a later tick.
	 */
	public void invalidate(BlockPos pos) {
		if (this.chunks.isEmpty()) {
			return;
		}

		ChunkHighlights highlights = this.chunks.get(ChunkPos.asLong(pos));
		if (highlights != null) {
			highlights.dirty = true;
		}
	}

	/**
	 * Prunes columns that are no longer relevant and scans a small number of new ones.
	 */
	public void tick(Minecraft minecraft) {
		ClientLevel level = minecraft.level;
		if (level == null) {
			this.clear();
			return;
		}

		// Dimension change (or a full world change): the old positions are meaningless.
		if (level != this.trackedLevel) {
			this.chunks.clear();
			this.trackedLevel = level;
		}

		LocalPlayer player = minecraft.player;
		if (player == null) {
			return;
		}

		int scanRadius = XRayConfigManager.get().scanRadius;
		int chunkRadius = (scanRadius + 15) >> 4;
		ChunkPos center = player.chunkPosition();
		int centerX = center.x;
		int centerZ = center.z;

		// 1. Drop columns that moved out of range or got unloaded.
		LongIterator iterator = this.chunks.keySet().iterator();
		while (iterator.hasNext()) {
			long key = iterator.nextLong();
			int cx = ChunkPos.getX(key);
			int cz = ChunkPos.getZ(key);
			if (Math.abs(cx - centerX) > chunkRadius || Math.abs(cz - centerZ) > chunkRadius
					|| !level.hasChunk(cx, cz)) {
				iterator.remove();
			}
		}

		// 2. Scan a bounded number of missing or stale columns.
		int budget = CHUNKS_SCANNED_PER_TICK;
		for (int dx = -chunkRadius; dx <= chunkRadius && budget > 0; dx++) {
			for (int dz = -chunkRadius; dz <= chunkRadius && budget > 0; dz++) {
				int cx = centerX + dx;
				int cz = centerZ + dz;
				long key = ChunkPos.asLong(cx, cz);
				ChunkHighlights existing = this.chunks.get(key);
				if (existing != null && !existing.dirty) {
					continue;
				}

				if (!level.hasChunk(cx, cz)) {
					continue;
				}

				this.chunks.put(key, scan(level.getChunk(cx, cz), cx, cz));
				budget--;
			}
		}
	}

	private static ChunkHighlights scan(LevelChunk chunk, int chunkX, int chunkZ) {
		LongList positions = new LongArrayList(32);
		IntList colors = new IntArrayList(32);

		LevelChunkSection[] sections = chunk.getSections();
		int baseX = chunkX << 4;
		int baseZ = chunkZ << 4;

		for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
			LevelChunkSection section = sections[sectionIndex];
			if (section == null || section.hasOnlyAir()) {
				continue;
			}

			int baseY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(sectionIndex));

			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					for (int x = 0; x < 16; x++) {
						BlockState state = section.getBlockState(x, y, z);
						int color = XRayBlocks.colorOf(state.getBlock());
						if (color != 0) {
							positions.add(BlockPos.asLong(baseX + x, baseY + y, baseZ + z));
							colors.add(color);
						}
					}
				}
			}
		}

		return new ChunkHighlights(positions.toLongArray(), colors.toIntArray());
	}

	/**
	 * @return the cached columns; only ever touched on the client thread
	 */
	public Long2ObjectOpenHashMap<ChunkHighlights> chunks() {
		return this.chunks;
	}

	/**
	 * @return the total number of cached highlight blocks
	 */
	public int totalHighlights() {
		int total = 0;
		for (ChunkHighlights highlights : this.chunks.values()) {
			total += highlights.size();
		}

		return total;
	}
}

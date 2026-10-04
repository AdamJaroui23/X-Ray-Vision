package com.adamjaroui.xrayvision.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adamjaroui.xrayvision.config.XRayConfig;
import com.adamjaroui.xrayvision.config.XRayConfigManager;

/**
 * The catalogue of blocks that X-Ray Vision keeps visible, together with the highlight colour of each
 * block.
 *
 * <p>Blocks are referenced through the {@link Blocks} constants rather than by string, so a typo can
 * never slip through: an unknown block is a compile error. The identifier used in the configuration
 * file is read back out of {@link BuiltInRegistries#BLOCK}, so it is always the real registry id.
 *
 * <p>The catalogue is built lazily from {@link #build()} on {@code CLIENT_STARTED}, well after the
 * game registries have been bootstrapped.
 */
public final class XRayBlocks {
	private static final Logger LOGGER = LoggerFactory.getLogger("X-Ray Vision/Blocks");

	// Highlight colours. Dark gray, orange, light gray, yellow, red, blue, cyan, green,
	// dark brown, white and golden yellow match the ore colours players already recognise.
	private static final int COAL = 0x3F3F3F;
	private static final int COPPER = 0xE07A46;
	private static final int IRON = 0xD8D8D8;
	private static final int GOLD = 0xFCEE4B;
	private static final int REDSTONE = 0xE01B24;
	private static final int LAPIS = 0x2C4EE0;
	private static final int DIAMOND = 0x4AEDD9;
	private static final int EMERALD = 0x17DD62;
	private static final int ANCIENT_DEBRIS = 0x5B3A2E;
	private static final int QUARTZ = 0xFFFFFF;
	private static final int NETHER_GOLD = 0xFAD64B;
	private static final int RAW_IRON = 0xC8C0B4;
	private static final int RAW_COPPER = 0xC1693C;
	private static final int CHEST = 0xB08A4A;
	private static final int ENDER_CHEST = 0x2E1B4E;
	private static final int SPAWNER = 0x2E4A6B;
	private static final int SHULKER = 0x8E60B0;

	private static final List<Entry> ENTRIES = new ArrayList<>();
	private static final List<Block> BLOCKS = new ArrayList<>();
	private static final Object2IntOpenHashMap<Block> COLORS = new Object2IntOpenHashMap<>();
	private static final Set<Block> VISIBLE = Collections.newSetFromMap(new IdentityHashMap<>());

	private static boolean built = false;

	static {
		COLORS.defaultReturnValue(0);
	}

	private XRayBlocks() {
	}

	/**
	 * An entry of the catalogue: a real block, its configuration id and its highlight colour.
	 */
	public record Entry(String id, int color) {
	}

	/**
	 * Populates the catalogue. Safe to call more than once; only the first call does any work.
	 */
	public static synchronized void build() {
		if (built) {
			return;
		}

		ENTRIES.clear();

		// Overworld ores
		register(Blocks.COAL_ORE, COAL);
		register(Blocks.DEEPSLATE_COAL_ORE, COAL);
		register(Blocks.COPPER_ORE, COPPER);
		register(Blocks.DEEPSLATE_COPPER_ORE, COPPER);
		register(Blocks.IRON_ORE, IRON);
		register(Blocks.DEEPSLATE_IRON_ORE, IRON);
		register(Blocks.GOLD_ORE, GOLD);
		register(Blocks.DEEPSLATE_GOLD_ORE, GOLD);
		register(Blocks.REDSTONE_ORE, REDSTONE);
		register(Blocks.DEEPSLATE_REDSTONE_ORE, REDSTONE);
		register(Blocks.LAPIS_ORE, LAPIS);
		register(Blocks.DEEPSLATE_LAPIS_ORE, LAPIS);
		register(Blocks.DIAMOND_ORE, DIAMOND);
		register(Blocks.DEEPSLATE_DIAMOND_ORE, DIAMOND);
		register(Blocks.EMERALD_ORE, EMERALD);
		register(Blocks.DEEPSLATE_EMERALD_ORE, EMERALD);

		// Nether ores
		register(Blocks.NETHER_QUARTZ_ORE, QUARTZ);
		register(Blocks.NETHER_GOLD_ORE, NETHER_GOLD);
		register(Blocks.ANCIENT_DEBRIS, ANCIENT_DEBRIS);

		// Storage / raw metal blocks
		register(Blocks.RAW_IRON_BLOCK, RAW_IRON);
		register(Blocks.RAW_COPPER_BLOCK, RAW_COPPER);
		register(Blocks.RAW_GOLD_BLOCK, NETHER_GOLD);

		// Chests
		register(Blocks.CHEST, CHEST);
		register(Blocks.TRAPPED_CHEST, CHEST);
		register(Blocks.COPPER_CHEST, CHEST);
		register(Blocks.EXPOSED_COPPER_CHEST, CHEST);
		register(Blocks.WEATHERED_COPPER_CHEST, CHEST);
		register(Blocks.OXIDIZED_COPPER_CHEST, CHEST);
		register(Blocks.WAXED_COPPER_CHEST, CHEST);
		register(Blocks.WAXED_EXPOSED_COPPER_CHEST, CHEST);
		register(Blocks.WAXED_WEATHERED_COPPER_CHEST, CHEST);
		register(Blocks.WAXED_OXIDIZED_COPPER_CHEST, CHEST);
		register(Blocks.ENDER_CHEST, ENDER_CHEST);

		// Spawners
		register(Blocks.SPAWNER, SPAWNER);
		register(Blocks.TRIAL_SPAWNER, SPAWNER);

		// Shulker boxes
		register(Blocks.SHULKER_BOX, SHULKER);
		register(Blocks.WHITE_SHULKER_BOX, SHULKER);
		register(Blocks.ORANGE_SHULKER_BOX, SHULKER);
		register(Blocks.MAGENTA_SHULKER_BOX, SHULKER);
		register(Blocks.LIGHT_BLUE_SHULKER_BOX, SHULKER);
		register(Blocks.YELLOW_SHULKER_BOX, SHULKER);
		register(Blocks.LIME_SHULKER_BOX, SHULKER);
		register(Blocks.PINK_SHULKER_BOX, SHULKER);
		register(Blocks.GRAY_SHULKER_BOX, SHULKER);
		register(Blocks.LIGHT_GRAY_SHULKER_BOX, SHULKER);
		register(Blocks.CYAN_SHULKER_BOX, SHULKER);
		register(Blocks.PURPLE_SHULKER_BOX, SHULKER);
		register(Blocks.BLUE_SHULKER_BOX, SHULKER);
		register(Blocks.BROWN_SHULKER_BOX, SHULKER);
		register(Blocks.GREEN_SHULKER_BOX, SHULKER);
		register(Blocks.RED_SHULKER_BOX, SHULKER);
		register(Blocks.BLACK_SHULKER_BOX, SHULKER);

		built = true;

		// Make every catalogue entry explicit in the config file so it is self documenting.
		XRayConfig config = XRayConfigManager.get();
		boolean added = false;
		for (int i = 0; i < ENTRIES.size(); i++) {
			Entry entry = ENTRIES.get(i);
			if (!config.blocks.containsKey(entry.id())) {
				config.setBlockEnabled(entry.id(), true);
				added = true;
			}
		}

		if (added) {
			XRayConfigManager.save();
		}

		refresh();
		LOGGER.info("Loaded {} X-Ray target blocks", Integer.valueOf(ENTRIES.size()));
	}

	private static void register(Block block, int color) {
		Identifier id = BuiltInRegistries.BLOCK.getKey(block);
		if (id == null) {
			LOGGER.warn("Skipping catalogue entry for a block that is not registered: {}", block);
			return;
		}

		BLOCKS.add(block);
		ENTRIES.add(new Entry(id.toString(), color));
	}

	/**
	 * Re-applies the per block configuration. Called after the catalogue is built and whenever the
	 * configuration changes. Also drops the highlight cache because stored colours may be stale.
	 */
	public static synchronized void refresh() {
		XRayConfig config = XRayConfigManager.get();
		COLORS.clear();
		VISIBLE.clear();

		for (int i = 0; i < ENTRIES.size(); i++) {
			Entry entry = ENTRIES.get(i);
			if (!config.isBlockEnabled(entry.id())) {
				continue;
			}

			Block block = BLOCKS.get(i);
			VISIBLE.add(block);
			COLORS.put(block, entry.color());
		}

		OreHighlightCache.INSTANCE.clear();
	}

	/**
	 * @return {@code true} when this block must stay visible (and keep its block entity renderer)
	 *     while X-Ray is active
	 */
	public static boolean isVisible(Block block) {
		return VISIBLE.contains(block);
	}

	/**
	 * @return the 0xRRGGBB highlight colour, or {@code 0} when the block is not highlighted
	 */
	public static int colorOf(Block block) {
		return COLORS.getInt(block);
	}

	/**
	 * @return {@code true} when this block should be highlighted, which is exactly the visible set
	 */
	public static boolean isHighlighted(Block block) {
		return COLORS.containsKey(block);
	}

	/**
	 * @return an immutable view of the catalogue, for config screens and diagnostics
	 */
	public static List<Entry> entries() {
		return Collections.unmodifiableList(ENTRIES);
	}

	/**
	 * @return {@code true} once {@link #build()} has run
	 */
	public static boolean isBuilt() {
		return built;
	}
}

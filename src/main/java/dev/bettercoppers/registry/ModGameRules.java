package dev.bettercoppers.registry;

import dev.bettercoppers.BetterCoppers;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;

/** The mod's settings (ADR 0010), in a "Better Coppers" category. Read them live on the server, through {@code MineLimits}. */
public final class ModGameRules {
	public static final GameRuleCategory CATEGORY = GameRuleCategory.register(BetterCoppers.id("better_coppers"));
	/** How far a mine grows from its middle beam, horizontal Chebyshev. */
	public static final GameRule<Integer> MINE_RANGE = integer("mine_range", 64, 16, 256);
	/** The lowest y a miner digs at, but never below the world's bottom + 5. */
	public static final GameRule<Integer> MINE_DEPTH_FLOOR = integer("mine_depth_floor", -59, -2032, 2031);
	/** How far from its home chest, in a straight line to the middle beam, a miner looks for a Mine Entrance. */
	public static final GameRule<Integer> MINE_ENTRANCE_SEARCH_RADIUS = integer("mine_entrance_search_radius", 32, 8, 64);

	private ModGameRules() {
	}

	private static GameRule<Integer> integer(String id, int value, int min, int max) {
		return GameRuleBuilder.forInteger(value).range(min, max).category(CATEGORY).buildAndRegister(BetterCoppers.id(id));
	}

	/** Loads the class, which registers the rules. */
	public static void init() {
	}
}

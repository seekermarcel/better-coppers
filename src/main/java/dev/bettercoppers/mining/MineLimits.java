package dev.bettercoppers.mining;

import dev.bettercoppers.registry.ModGameRules;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * The mine game rules as they are now. Read them again for every check, so a changed rule counts from the next one.
 * {@code depthFloor} is already raised to the world's bottom + 5.
 */
public record MineLimits(int range, int depthFloor, int entranceSearchRadius) {
	public static MineLimits of(ServerLevel level) {
		GameRules rules = level.getGameRules();
		return new MineLimits(rules.get(ModGameRules.MINE_RANGE), Math.max(rules.get(ModGameRules.MINE_DEPTH_FLOOR), level.getMinY() + 5),
			rules.get(ModGameRules.MINE_ENTRANCE_SEARCH_RADIUS));
	}

	/** Within the Mine Range of the middle beam {@code beam}, horizontal Chebyshev. */
	public boolean inRange(BlockPos beam, BlockPos pos) {
		return Math.max(Math.abs(pos.getX() - beam.getX()), Math.abs(pos.getZ() - beam.getZ())) <= this.range;
	}

	/** Not below the Depth Floor. */
	public boolean aboveFloor(BlockPos pos) {
		return pos.getY() >= this.depthFloor;
	}

	/** Every step of a section is in range, and none digs below the Depth Floor. */
	public boolean allows(BlockPos beam, List<MineLayout.Step> steps) {
		return steps.stream().allMatch(step -> this.inRange(beam, step.pos()) && (step.action() != MineLayout.Action.DIG || this.aboveFloor(step.pos())));
	}
}

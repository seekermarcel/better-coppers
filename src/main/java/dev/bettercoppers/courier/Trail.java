package dev.bettercoppers.courier;

import java.util.ArrayDeque;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

/**
 * Where a courier has walked since its last stop: a crumb every few blocks, the newest last. A stuck courier walks back
 * along it, further each time it gets stuck again, and looks for the way from there.
 */
final class Trail {
	/** Blocks between crumbs. */
	static final int SPACING = 4;
	/** Crumbs to go back per escape: about 32 blocks, then 64, then 96. */
	static final int CRUMBS_PER_ESCAPE = 8;
	private static final int MAX_CRUMBS = 3 * CRUMBS_PER_ESCAPE;

	private final ArrayDeque<BlockPos> crumbs = new ArrayDeque<>();

	void record(BlockPos pos) {
		BlockPos last = this.crumbs.peekLast();
		if (last == null || last.distSqr(pos) >= SPACING * SPACING) {
			this.crumbs.addLast(pos);
			if (this.crumbs.size() > MAX_CRUMBS) {
				this.crumbs.removeFirst();
			}
		}
	}

	/**
	 * The crumb to go back to for the {@code escape}th escape in a row (from 1), or null with no trail. The crumbs after
	 * it are dropped: the courier walks them again and lays them anew.
	 */
	@Nullable BlockPos backtrack(int escape) {
		BlockPos crumb = null;
		for (int i = 0; i < escape * CRUMBS_PER_ESCAPE && !this.crumbs.isEmpty(); i++) {
			crumb = this.crumbs.pollLast();
		}
		return crumb;
	}

	void clear() {
		this.crumbs.clear();
	}
}

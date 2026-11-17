package dev.bettercoppers.api;

import dev.bettercoppers.mining.MineLayout;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The shape of a level Mine Section, for {@link BetterCoppersApi#registerMineSection}. Cells are addressed from the Open
 * End the section grows from: {@code f} forward, {@code side} to the right (negative to the left). Slice f0 starts at the
 * Open End, and the golem stands in the parent's last slice (f-1) to dig it. Every column is dug 3 high with a plank floor
 * where one is missing; supports, torches and rails are vanilla mineshaft blocks. Build one with {@link #builder()}.
 */
public final class MineSectionTemplate {
	/** Which way an Open End faces, seen from the section's own Open End. */
	public enum Turn {
		AHEAD,
		LEFT,
		RIGHT
	}

	/** One column of the floor plan. */
	public record Cell(int f, int side) {
	}

	/** Two fence posts at {@code fromSide} and {@code toSide} of slice {@code f}, with a plank beam across their tops. */
	public record Support(int f, int fromSide, int toSide) {
	}

	/** Where a next section can start: the first rock cell past the edge, and the way it faces. */
	public record OpenEnd(int f, int side, Turn turn) {
	}

	private final Set<Cell> columns;
	private final List<Support> supports;
	private final List<Cell> pillars;
	private final List<Cell> torches;
	private final List<Cell> rails;
	private final List<OpenEnd> openEnds;
	private final boolean mirrorable;

	private MineSectionTemplate(Builder builder) {
		this.columns = Set.copyOf(builder.columns);
		this.supports = List.copyOf(builder.supports);
		this.pillars = List.copyOf(builder.pillars);
		this.torches = List.copyOf(builder.torches);
		this.rails = List.copyOf(builder.rails);
		this.openEnds = List.copyOf(builder.openEnds);
		this.mirrorable = builder.mirrorable;
	}

	public static Builder builder() {
		return new Builder();
	}

	public Set<Cell> columns() {
		return this.columns;
	}

	public List<Support> supports() {
		return this.supports;
	}

	public List<Cell> pillars() {
		return this.pillars;
	}

	public List<Cell> torches() {
		return this.torches;
	}

	public List<Cell> rails() {
		return this.rails;
	}

	public List<OpenEnd> openEnds() {
		return this.openEnds;
	}

	/** Whether the mirror image (left and right swapped) is rolled too, as a second candidate. */
	public boolean mirrorable() {
		return this.mirrorable;
	}

	public static final class Builder {
		private final Set<Cell> columns = new LinkedHashSet<>();
		private final List<Support> supports = new ArrayList<>();
		private final List<Cell> pillars = new ArrayList<>();
		private final List<Cell> torches = new ArrayList<>();
		private final List<Cell> rails = new ArrayList<>();
		private final List<OpenEnd> openEnds = new ArrayList<>();
		private boolean mirrorable;

		private Builder() {
		}

		/** Digs every column from {@code fromF} to {@code toF} and {@code fromSide} to {@code toSide}, inclusive. */
		public Builder columns(int fromF, int toF, int fromSide, int toSide) {
			for (int f = fromF; f <= toF; f++) {
				for (int side = fromSide; side <= toSide; side++) {
					this.columns.add(new Cell(f, side));
				}
			}
			return this;
		}

		public Builder column(int f, int side) {
			this.columns.add(new Cell(f, side));
			return this;
		}

		public Builder support(int f, int fromSide, int toSide) {
			this.supports.add(new Support(f, fromSide, toSide));
			return this;
		}

		/** A plank pillar 3 high on a dug column, like the inner corners of a vanilla crossing. It goes up once the next slice is open. */
		public Builder pillar(int f, int side) {
			this.pillars.add(new Cell(f, side));
			return this;
		}

		/**
		 * A wall torch in this column, skipped where it is already bright. It hangs at head height on a beam or pillar next
		 * to it, else one lower on the rock beside it.
		 */
		public Builder torch(int f, int side) {
			this.torches.add(new Cell(f, side));
			return this;
		}

		/** A rail, laid only if the miner carries rails. Rails go down last, from the far end back, and curve by themselves. */
		public Builder rail(int f, int side) {
			this.rails.add(new Cell(f, side));
			return this;
		}

		public Builder openEnd(int f, int side, Turn turn) {
			this.openEnds.add(new OpenEnd(f, side, turn));
			return this;
		}

		public Builder mirrorable() {
			this.mirrorable = true;
			return this;
		}

		/** Checks the template and builds it. Throws {@link IllegalArgumentException} saying what is wrong. */
		public MineSectionTemplate build() {
			for (int side = -1; side <= 1; side++) {
				require(this.columns.contains(new Cell(0, side)), "slice 0 must hold the opening at sides -1 to 1");
			}
			require(this.columns.stream().allMatch(cell -> cell.f() >= 0), "columns must not reach behind slice 0");
			require(connected(this.columns), "every column must be face-connected");
			for (OpenEnd end : this.openEnds) {
				Cell cell = new Cell(end.f(), end.side());
				Cell inside = switch (end.turn()) {
					case AHEAD -> new Cell(end.f() - 1, end.side());
					case LEFT -> new Cell(end.f(), end.side() + 1);
					case RIGHT -> new Cell(end.f(), end.side() - 1);
				};
				require(!this.columns.contains(cell) && this.columns.contains(inside), "Open End " + end + " must sit on the edge, facing out");
			}
			for (Support support : this.supports) {
				require(support.fromSide() < support.toSide(), "support " + support + " needs two posts");
				for (int side = support.fromSide(); side <= support.toSide(); side++) {
					require(this.columns.contains(new Cell(support.f(), side)), "support " + support + " must span dug columns");
				}
			}
			require(this.columns.containsAll(this.pillars), "pillars must stand on dug columns");
			require(this.columns.containsAll(this.torches), "torches must lie on dug columns");
			require(this.columns.containsAll(this.rails), "rails must lie on dug columns");
			MineSectionTemplate template = new MineSectionTemplate(this);
			// Lays it out once: every block in reach of a stand, and every torch on something.
			MineLayout.check(template);
			return template;
		}

		private static void require(boolean ok, String message) {
			if (!ok) {
				throw new IllegalArgumentException(message);
			}
		}

		private static boolean connected(Set<Cell> columns) {
			Set<Cell> seen = new HashSet<>();
			Deque<Cell> next = new ArrayDeque<>(List.of(new Cell(0, 0)));
			while (!next.isEmpty()) {
				Cell cell = next.pop();
				if (columns.contains(cell) && seen.add(cell)) {
					next.addAll(List.of(new Cell(cell.f() + 1, cell.side()), new Cell(cell.f() - 1, cell.side()),
						new Cell(cell.f(), cell.side() + 1), new Cell(cell.f(), cell.side() - 1)));
				}
			}
			return seen.size() == columns.size();
		}
	}
}

package dev.bettercoppers.mining;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.bettercoppers.BetterCoppers;
import dev.bettercoppers.home.Homes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/**
 * The mines of one dimension, saved as {@code bettercoppers:mines} ({@code data/bettercoppers/mines.dat}): server-only, never synced, not in {@code api}. A mine
 * is keyed by its entrance's middle beam and axis, so a frame rebuilt on the other axis is another mine.
 */
public final class Mines extends SavedData {
	/** The saved format. A newer one is logged and left alone; an older one goes through {@link #migrate}. */
	static final int FORMAT = 1;

	/** How often a miner without a mine looks again. */
	private static final int LOOK_INTERVAL = 100;

	/** Where a Mine Section can start, and the way it faces. */
	public record OpenEnd(BlockPos pos, Direction facing) {
		static final Codec<OpenEnd> CODEC = RecordCodecBuilder.create(i -> i.group(
			BlockPos.CODEC.fieldOf("pos").forGetter(OpenEnd::pos),
			Direction.CODEC.fieldOf("facing").forGetter(OpenEnd::facing)
		).apply(i, OpenEnd::new));
	}

	/**
	 * A Mine Section saved when it was rolled and fits, before its first block is dug. {@code parent} is the index of the
	 * section it grew from, -1 for the entrance. {@code box} is its footprint and floor layer.
	 */
	public record Section(Identifier kind, OpenEnd start, boolean mirrored, int parent, BoundingBox box, int progress, boolean done) {
		static final Codec<Section> CODEC = RecordCodecBuilder.create(i -> i.group(
			Identifier.CODEC.fieldOf("kind").forGetter(Section::kind),
			OpenEnd.CODEC.fieldOf("start").forGetter(Section::start),
			Codec.BOOL.optionalFieldOf("mirrored", false).forGetter(Section::mirrored),
			Codec.INT.fieldOf("parent").forGetter(Section::parent),
			BoundingBox.CODEC.fieldOf("box").forGetter(Section::box),
			Codec.INT.optionalFieldOf("progress", 0).forGetter(Section::progress),
			Codec.BOOL.optionalFieldOf("done", false).forGetter(Section::done)
		).apply(i, Section::new));

		/** A new section of {@code kind} at {@code start}, nothing dug yet. */
		static Section of(Identifier kind, OpenEnd start, boolean mirrored, int parent) {
			return new Section(kind, start, mirrored, parent, MineLayout.of(kind, start, mirrored).box(), 0, false);
		}

		public Section at(int progress, boolean done) {
			return new Section(this.kind, this.start, this.mirrored, this.parent, this.box, progress, done);
		}

		/** The Open Ends this section leaves. */
		public List<OpenEnd> openEnds() {
			return MineLayout.of(this.kind, this.start, this.mirrored).openEnds();
		}
	}

	/** One mine. {@code facing} stays empty until the first miner takes it. {@code ends} are its Open Ends nobody has grown from yet. */
	public record Mine(BlockPos entrance, Direction.Axis axis, Optional<Direction> facing, List<Section> sections, List<OpenEnd> ends) {
		static final Codec<Mine> CODEC = RecordCodecBuilder.create(i -> i.group(
			BlockPos.CODEC.fieldOf("entrance").forGetter(Mine::entrance),
			Direction.Axis.CODEC.fieldOf("axis").forGetter(Mine::axis),
			Direction.CODEC.optionalFieldOf("facing").forGetter(Mine::facing),
			Section.CODEC.listOf().optionalFieldOf("sections", List.of()).forGetter(Mine::sections),
			OpenEnd.CODEC.listOf().optionalFieldOf("ends", List.of()).forGetter(Mine::ends)
		).apply(i, Mine::new));

		/** No Open Ends left and every section done. A mine nobody has started is not finished. */
		public boolean isFinished() {
			return !this.sections.isEmpty() && this.ends.isEmpty() && this.sections.stream().allMatch(Section::done);
		}

		/** The index of the section {@code end} belongs to, or -1 for the entrance. */
		public int parentOf(OpenEnd end) {
			for (int i = 0; i < this.sections.size(); i++) {
				if (this.sections.get(i).openEnds().contains(end)) {
					return i;
				}
			}
			return -1;
		}

		private Mine with(List<Section> sections, List<OpenEnd> ends) {
			return new Mine(this.entrance, this.axis, this.facing, List.copyOf(sections), List.copyOf(ends));
		}

		/** The first section not done yet, or -1. */
		public int current() {
			for (int i = 0; i < this.sections.size(); i++) {
				if (!this.sections.get(i).done()) {
					return i;
				}
			}
			return -1;
		}
	}

	private record Stored(int format, List<Mine> mines) {
		static final Codec<Stored> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.fieldOf("format").forGetter(Stored::format),
			Mine.CODEC.listOf().fieldOf("mines").forGetter(Stored::mines)
		).apply(i, Stored::new));
	}

	public static final Codec<Mines> CODEC = Codec.PASSTHROUGH.xmap(Mines::read, Mines::write);
	// Vanilla needs a fix type. Nothing in random sequences' fixes touches our keys, so it is a no-op for this data.
	public static final SavedDataType<Mines> TYPE = new SavedDataType<>(BetterCoppers.id("mines"), Mines::new, CODEC, DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES);

	// shortcut: a list scanned linearly, fine for the few mines a world has; a spatial index if lookups ever show up.
	private final List<Mine> mines = new ArrayList<>();
	/** Data saved by a newer version, kept as read so saving doesn't destroy it. */
	private final Dynamic<?> newer;

	public Mines() {
		this.newer = null;
	}

	private Mines(List<Mine> mines, Dynamic<?> newer) {
		this.mines.addAll(mines);
		this.newer = newer;
	}

	public static Mines get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	public List<Mine> all() {
		return List.copyOf(mines);
	}

	public Optional<Mine> find(BlockPos entrance, Direction.Axis axis) {
		return mines.stream().filter(mine -> mine.entrance().equals(entrance) && mine.axis() == axis).findFirst();
	}

	/** Saves a mine for this frame unless there is one. False if it was already known, or the data is a newer format. */
	public boolean register(BlockPos entrance, Direction.Axis axis) {
		if (newer != null || find(entrance, axis).isPresent()) {
			return false;
		}
		mines.add(new Mine(entrance.immutable(), axis, Optional.empty(), List.of(), List.of()));
		setDirty();
		return true;
	}

	/** Deletes the mine of this frame. Only gametests use it, whose world and test positions outlive a run. */
	public void forget(BlockPos entrance, Direction.Axis axis) {
		if (mines.removeIf(mine -> mine.entrance().equals(entrance) && mine.axis() == axis)) {
			setDirty();
		}
	}

	/**
	 * The nearest registered, intact Mine Entrance whose mine is not finished, within the Entrance Search Radius of
	 * {@code home}; ties go to the lowest position. Entrances in unloaded chunks are left out.
	 */
	public Optional<Mine> nearestOpen(ServerLevel level, BlockPos home) {
		double radius = Mth.square(MineLimits.of(level).entranceSearchRadius());
		return mines.stream()
			.filter(mine -> !mine.isFinished() && mine.entrance().distSqr(home) <= radius && level.isLoaded(mine.entrance())
				&& MineEntrance.isIntact(level, mine.entrance(), mine.axis()))
			.min(Comparator.<Mine>comparingDouble(mine -> mine.entrance().distSqr(home)).thenComparing(Mine::entrance));
	}

	/**
	 * Saves the facing of a mine nobody has taken yet and its first section, a {@code kind} at the Open End in front of the
	 * opening, with that section's Open Ends. Returns the mine as saved now.
	 */
	public Mine start(Mine mine, Direction facing, Identifier kind) {
		Section first = Section.of(kind, firstEnd(mine, facing), false, -1);
		return replace(mine, new Mine(mine.entrance(), mine.axis(), Optional.of(facing), List.of(first), first.openEnds()));
	}

	/** The Open End of the first section: in front of the opening, along {@code facing}. */
	static OpenEnd firstEnd(Mine mine, Direction facing) {
		return new OpenEnd(mine.entrance().below(2).relative(facing), facing);
	}

	/** Saves how far section {@code index} of {@code mine} got. */
	public void setProgress(Mine mine, int index, int progress, boolean done) {
		Mine saved = find(mine.entrance(), mine.axis()).orElseThrow();
		List<Section> sections = new ArrayList<>(saved.sections());
		sections.set(index, sections.get(index).at(progress, done));
		replace(saved, saved.with(sections, saved.ends()));
	}

	/** Drops the Open Ends of section {@code index}: it was cut short, so nothing can grow from them. */
	public void dropEnds(Mine mine, int index) {
		Mine saved = find(mine.entrance(), mine.axis()).orElseThrow();
		List<OpenEnd> ends = saved.ends().stream().filter(end -> saved.parentOf(end) != index).toList();
		if (ends.size() != saved.ends().size()) {
			replace(saved, saved.with(saved.sections(), ends));
		}
	}

	/**
	 * Grows {@code mine} by one section at the Open End nearest {@code near} (ties go to the oldest) whose section is
	 * done and whose cells are loaded: it rolls that end and saves the first kind that fits, which takes the end's place
	 * and adds its own ends. An end no kind fits is dropped. Returns the mine as saved now; it has no section to dig if
	 * nothing could be grown.
	 */
	public Mine grow(ServerLevel level, Mine mine, BlockPos near) {
		Mine saved = find(mine.entrance(), mine.axis()).orElseThrow();
		while (true) {
			Mine current = saved;
			Optional<OpenEnd> pick = current.ends().stream()
				.filter(end -> level.isLoaded(end.pos()))
				.filter(end -> current.parentOf(end) < 0 || current.sections().get(current.parentOf(end)).done())
				.min(Comparator.comparingDouble(end -> end.pos().distSqr(near)));
			if (pick.isEmpty()) {
				return saved;
			}
			OpenEnd end = pick.get();
			List<OpenEnd> ends = new ArrayList<>(saved.ends());
			ends.remove(end);
			Section section = roll(level.getSeed(), MineLimits.of(level), saved, end);
			if (section == null) {
				// shortcut: an end no section fits is dropped; #42 closes it with a Barrier instead.
				saved = replace(saved, saved.with(saved.sections(), ends));
				continue;
			}
			List<Section> sections = new ArrayList<>(saved.sections());
			sections.add(section);
			ends.addAll(section.openEnds());
			return replace(saved, saved.with(sections, ends));
		}
	}

	/** How many rolled kinds an Open End tries before it gives up. */
	static final int TRIES = 3;

	/** The first of {@link #TRIES} rolled sections for {@code end} that fits within {@code limits}, or null. */
	@Nullable Section roll(long worldSeed, MineLimits limits, Mine mine, OpenEnd end) {
		int parent = mine.parentOf(end);
		List<SectionRoll.Candidate> candidates = SectionRoll.candidates(parent < 0 ? null : mine.sections().get(parent).kind(), mine.ends().size() == 1, end.pos().getY() > 0);
		List<SectionRoll.Candidate> order = SectionRoll.order(candidates, RandomSource.create(SectionRoll.seed(worldSeed, mine.entrance(), end)));
		for (SectionRoll.Candidate candidate : order.subList(0, Math.min(TRIES, order.size()))) {
			Section section = Section.of(candidate.kind(), end, candidate.mirrored(), parent);
			if (fits(section.box()) && limits.allows(mine.entrance(), MineLayout.build(section))) {
				return section;
			}
		}
		return null;
	}

	/** Clear of every saved section of every mine here. */
	// shortcut: only overlaps (and the limits) count until the blocked checks (#42) add lava, water, caves, the surface and the rest.
	private boolean fits(BoundingBox box) {
		return mines.stream().flatMap(mine -> mine.sections().stream()).noneMatch(section -> section.box().intersects(box));
	}

	private Mine replace(Mine old, Mine mine) {
		mines.replaceAll(saved -> saved.entrance().equals(old.entrance()) && saved.axis() == old.axis() ? mine : saved);
		setDirty();
		return mine;
	}

	/**
	 * The mine this miner works in. Without one (or once it is finished) it looks for the nearest open entrance: right
	 * away if {@code now}, otherwise every {@link #LOOK_INTERVAL} ticks. The first miner to take a mine sets its facing
	 * and its first section. Null if it has no home or no mine.
	 */
	public static @Nullable Mine of(ServerLevel level, CopperGolem golem, boolean now) {
		MinerData data = MinerData.get(golem);
		BlockPos home = Homes.get(golem);
		if (data == null || home == null) {
			return null;
		}
		Mines mines = get(level);
		if (data.mine() != null) {
			Optional<Mine> mine = mines.find(data.mine(), data.mineAxis());
			if (mine.isPresent() && !mine.get().isFinished()) {
				return mine.get();
			}
			data.clearMine();
		}
		if (!now && level.getGameTime() % LOOK_INTERVAL != 0) {
			return null;
		}
		Mine found = mines.nearestOpen(level, home).orElse(null);
		if (found == null) {
			return null;
		}
		if (found.sections().isEmpty()) {
			Direction facing = found.facing().orElse(facing(level, found.entrance(), found.axis(), home));
			// Not rolled: into a hill it is a Straight, on open ground Stairs Down.
			Identifier kind = isSolid(level, found.entrance().below().relative(facing)) ? MineLayout.STRAIGHT : MineLayout.STAIRS_DOWN;
			// shortcut: past the Mine Range or the Depth Floor it is just not dug and the miner keeps looking; #42 closes it with a Barrier.
			if (!MineLimits.of(level).allows(found.entrance(), MineLayout.build(kind, firstEnd(found, facing), false))) {
				return null;
			}
			found = mines.start(found, facing, kind);
		}
		data.setMine(found.entrance(), found.axis());
		return found;
	}

	/**
	 * The way a mine runs: into the hill, where the head-height cell in front of the opening is solid. With both sides
	 * solid or both open it runs away from {@code home}, and +X or +Z on a tie.
	 */
	static Direction facing(BlockGetter level, BlockPos beam, Direction.Axis axis, BlockPos home) {
		Direction ahead = Direction.fromAxisAndDirection(axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X, Direction.AxisDirection.POSITIVE);
		boolean front = isSolid(level, beam.below().relative(ahead));
		if (front != isSolid(level, beam.below().relative(ahead.getOpposite()))) {
			return front ? ahead : ahead.getOpposite();
		}
		int away = beam.subtract(home).get(ahead.getAxis());
		return away < 0 ? ahead.getOpposite() : ahead;
	}

	private static boolean isSolid(BlockGetter level, BlockPos pos) {
		return level.getBlockState(pos).isCollisionShapeFullBlock(level, pos);
	}

	private static Mines read(Dynamic<?> data) {
		int format = data.get("format").asInt(FORMAT);
		if (format > FORMAT) {
			BetterCoppers.LOGGER.warn("bettercoppers_mines has format {}, newer than this version knows ({}): mines are ignored and left as saved", format, FORMAT);
			return new Mines(List.of(), data);
		}
		return Stored.CODEC.parse(migrate(data, format))
			.resultOrPartial(error -> BetterCoppers.LOGGER.error("bettercoppers_mines: {}", error))
			.map(stored -> new Mines(stored.mines(), null))
			.orElseGet(Mines::new);
	}

	/** Brings older data up to {@link #FORMAT}, one format at a time. There is no older format yet. */
	private static Dynamic<?> migrate(Dynamic<?> data, int format) {
		return data;
	}

	private Dynamic<?> write() {
		if (newer != null) {
			return newer;
		}
		return new Dynamic<>(NbtOps.INSTANCE, Stored.CODEC.encodeStart(NbtOps.INSTANCE, new Stored(FORMAT, mines)).getOrThrow());
	}
}

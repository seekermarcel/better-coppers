package dev.bettercoppers.mining;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.bettercoppers.BetterCoppers;
import dev.bettercoppers.home.Homes;
import dev.bettercoppers.job.Jobs;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
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
	 * section it grew from, -1 for the entrance. {@code box} is its footprint and floor layer. Only a Barrier has a
	 * {@code reason}: what closed its end, for the sign.
	 */
	public record Section(Identifier kind, OpenEnd start, boolean mirrored, int parent, BoundingBox box, int progress, boolean done, Optional<BlockedReason> reason) {
		static final Codec<Section> CODEC = RecordCodecBuilder.create(i -> i.group(
			Identifier.CODEC.fieldOf("kind").forGetter(Section::kind),
			OpenEnd.CODEC.fieldOf("start").forGetter(Section::start),
			Codec.BOOL.optionalFieldOf("mirrored", false).forGetter(Section::mirrored),
			Codec.INT.fieldOf("parent").forGetter(Section::parent),
			BoundingBox.CODEC.fieldOf("box").forGetter(Section::box),
			Codec.INT.optionalFieldOf("progress", 0).forGetter(Section::progress),
			Codec.BOOL.optionalFieldOf("done", false).forGetter(Section::done),
			BlockedReason.CODEC.optionalFieldOf("reason").forGetter(Section::reason)
		).apply(i, Section::new));

		/** A new section of {@code kind} at {@code start}, nothing dug yet. */
		static Section of(Identifier kind, OpenEnd start, boolean mirrored, int parent) {
			return new Section(kind, start, mirrored, parent, MineLayout.of(kind, start, mirrored).box(), 0, false, Optional.empty());
		}

		/** A Barrier across {@code end}, closed for {@code reason}, not built yet. */
		static Section barrier(OpenEnd end, int parent, BlockedReason reason) {
			return new Section(MineLayout.BARRIER, end, false, parent, MineLayout.of(MineLayout.BARRIER, end, false).box(), 0, false, Optional.of(reason));
		}

		public Section at(int progress, boolean done) {
			return new Section(this.kind, this.start, this.mirrored, this.parent, this.box, progress, done, this.reason);
		}

		private Section withParent(int parent) {
			return new Section(this.kind, this.start, this.mirrored, parent, this.box, this.progress, this.done, this.reason);
		}

		/** The Open Ends this section leaves; none for a kind whose add-on was removed, whose saved ends still count. */
		public List<OpenEnd> openEnds() {
			return MineSections.isKnown(this.kind) ? MineLayout.of(this.kind, this.start, this.mirrored).openEnds() : List.of();
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
	}

	/** How long a claim holds without being renewed: a miner frozen in an unloaded chunk lets go after 5 minutes. */
	public static final int CLAIM_TICKS = 6000;

	/** A section of a mine, found by where it starts, so deleting a section doesn't move the claims of the others. */
	private record ClaimKey(BlockPos entrance, Direction.Axis axis, OpenEnd start) {
	}

	/** One miner digging one section, renewed by each tick of its {@link MineBehavior}. */
	private record Claim(UUID golem, long renewedAt) {
		/** The claimant is loaded, alive, a miner with a pickaxe in hand and still on {@code mine}, and renewed in time. */
		boolean isValid(ServerLevel level, Mine mine) {
			if (level.getGameTime() - this.renewedAt > CLAIM_TICKS || !(level.getEntity(this.golem) instanceof CopperGolem golem) || !golem.isAlive()
				|| Jobs.of(golem) != Jobs.JobKind.MINER || !MinerData.hasPickaxe(golem)) {
				return false;
			}
			MinerData data = MinerData.get(golem);
			return data != null && mine.entrance().equals(data.mine()) && mine.axis() == data.mineAxis();
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
	/** Who digs which section. Never saved: empty after a load. */
	private final Map<ClaimKey, Claim> claims = new HashMap<>();
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
		claims.keySet().removeIf(key -> key.entrance().equals(entrance) && key.axis() == axis);
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
		return start(mine, facing, Section.of(kind, firstEnd(mine, facing), false, -1));
	}

	private Mine start(Mine mine, Direction facing, Section first) {
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
		if (done) {
			claims.remove(new ClaimKey(saved.entrance(), saved.axis(), sections.get(index).start()));
		}
	}

	/**
	 * Cuts section {@code index} short at {@code step}, which met lava or water it can't plug: its Open Ends are dropped,
	 * and a Barrier for {@code reason} is saved at its last fully dug slice, the one the golem stands in to dig that step.
	 */
	public void cutShort(Mine mine, int index, MineLayout.Step step, BlockedReason reason) {
		Mine saved = find(mine.entrance(), mine.axis()).orElseThrow();
		OpenEnd start = saved.sections().get(index).start();
		Direction forward = start.facing();
		int slice = (step.pos().get(forward.getAxis()) - start.pos().get(forward.getAxis())) * forward.getAxisDirection().getStep();
		OpenEnd at = new OpenEnd(start.pos().relative(forward, Math.max(0, slice)).atY(step.stand().getY()), forward);
		List<Section> sections = new ArrayList<>(saved.sections());
		sections.add(Section.barrier(at, index, reason));
		replace(saved, saved.with(sections, saved.ends().stream().filter(end -> saved.parentOf(end) != index).toList()));
	}

	/** A player's fence on an Open End's middle Barrier cell closes that end. True if it closed one. */
	public boolean close(BlockPos fence) {
		for (Mine mine : mines) {
			for (OpenEnd end : mine.ends()) {
				if (MineLayout.middleFence(end).equals(fence)) {
					List<Section> sections = new ArrayList<>(mine.sections());
					sections.add(Section.barrier(end, mine.parentOf(end), BlockedReason.PLAYER).at(0, true));
					replace(mine, mine.with(sections, mine.ends().stream().filter(other -> !other.equals(end)).toList()));
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Breaking the middle fence of a Barrier deletes it and opens what it closed again: its end, or for a section that
	 * was cut short, that section, which digs on from where it stopped. True if it opened one.
	 */
	public boolean reopen(BlockPos fence) {
		for (Mine mine : mines) {
			for (int index = 0; index < mine.sections().size(); index++) {
				Section barrier = mine.sections().get(index);
				if (!barrier.kind().equals(MineLayout.BARRIER) || !MineLayout.middleFence(barrier.start()).equals(fence)) {
					continue;
				}
				int removed = index;
				List<Section> sections = new ArrayList<>(mine.sections().stream()
					.map(section -> section.parent() > removed ? section.withParent(section.parent() - 1) : section).toList());
				sections.remove(index);
				List<OpenEnd> ends = new ArrayList<>(mine.ends());
				int parent = barrier.parent();
				// A section of a removed add-on can't be dug on: its end opens again instead.
				if (parent < 0 || !MineSections.isKnown(sections.get(parent).kind()) || sections.get(parent).openEnds().contains(barrier.start())) {
					ends.add(barrier.start());
				} else {
					sections.set(parent, sections.get(parent).at(0, false));
					ends.addAll(sections.get(parent).openEnds());
				}
				claims.remove(new ClaimKey(mine.entrance(), mine.axis(), barrier.start()));
				replace(mine, mine.with(sections, ends));
				return true;
			}
		}
		return false;
	}

	/** After a player placed a block: a wooden fence may close an Open End. */
	public static void onPlaced(ServerLevel level, BlockPos pos, BlockState placed) {
		if (placed.is(BlockTags.WOODEN_FENCES)) {
			get(level).close(pos);
		}
	}

	/** After a player broke a block: a Barrier's middle fence opens its end again. */
	public static void onBroken(Level level, BlockPos pos, BlockState broken) {
		if (level instanceof ServerLevel server && broken.is(BlockTags.WOODEN_FENCES)) {
			get(server).reopen(pos);
		}
	}

	/**
	 * Finds work for {@code golem} in {@code mine} and claims it, all in this tick: the section it already holds, else
	 * the nearest unfinished section nobody else holds a valid claim on, else the nearest Open End whose section is done,
	 * rolled and saved as a new section, or as a Barrier if no kind fits. Ties go to the oldest, and work in unloaded
	 * chunks is left out, as is an end whose roll reads unloaded cells. Returns the index of the claimed section, or -1
	 * if nothing is free.
	 */
	public int pick(ServerLevel level, Mine mine, CopperGolem golem) {
		return pick(level, mine, golem, Set.of());
	}

	/** {@link #pick(ServerLevel, Mine, CopperGolem)}, leaving out work that starts at one of {@code skipped}. */
	public int pick(ServerLevel level, Mine mine, CopperGolem golem, Set<OpenEnd> skipped) {
		Mine saved = find(mine.entrance(), mine.axis()).orElseThrow();
		int held = claimed(saved, golem);
		if (held >= 0) {
			return held;
		}
		Mine known = saved;
		Optional<Integer> section = freeSections(level, known, skipped)
			.filter(i -> level.isLoaded(known.sections().get(i).start().pos()))
			.min(Comparator.comparingDouble(i -> known.sections().get(i).start().pos().distSqr(golem.blockPosition())));
		if (section.isPresent()) {
			return claim(level, saved, section.get(), golem);
		}
		Blocked.Site site = Blocked.Site.of(level);
		Set<OpenEnd> waiting = new HashSet<>();
		while (true) {
			Optional<OpenEnd> pick = freeEnds(saved, skipped).filter(end -> !waiting.contains(end) && level.isLoaded(end.pos()))
				.min(Comparator.comparingDouble(end -> end.pos().distSqr(golem.blockPosition())));
			if (pick.isEmpty()) {
				return -1;
			}
			OpenEnd end = pick.get();
			Section rolled = roll(level.getSeed(), MineLimits.of(level), site, saved, end);
			if (rolled == null) {
				// Some of its cells are not loaded: it waits for now.
				waiting.add(end);
				continue;
			}
			Mine grown = grow(saved, rolled);
			return claim(level, grown, grown.sections().size() - 1, golem);
		}
	}

	/** Saves a new section of {@code kind} at the Open End {@code end} of {@code mine}, with its Open Ends. Returns the mine as saved now. */
	public Mine grow(Mine mine, OpenEnd end, Identifier kind, boolean mirrored) {
		return grow(find(mine.entrance(), mine.axis()).orElseThrow(), Section.of(kind, end, mirrored, mine.parentOf(end)));
	}

	private Mine grow(Mine saved, Section section) {
		List<OpenEnd> ends = new ArrayList<>(saved.ends());
		ends.remove(section.start());
		ends.addAll(section.openEnds());
		List<Section> sections = new ArrayList<>(saved.sections());
		sections.add(section);
		return replace(saved, saved.with(sections, ends));
	}

	/**
	 * Where the nearest free work in unloaded chunks starts (an unfinished section or an Open End), for a miner with
	 * nothing free in loaded ones to walk towards without claiming it. Empty if there is none.
	 */
	public Optional<OpenEnd> unloadedWork(ServerLevel level, Mine mine, CopperGolem golem, Set<OpenEnd> skipped) {
		Mine saved = find(mine.entrance(), mine.axis()).orElse(mine);
		return Stream.concat(freeSections(level, saved, skipped).map(i -> saved.sections().get(i).start()), freeEnds(saved, skipped))
			.filter(start -> !level.isLoaded(start.pos()))
			.min(Comparator.comparingDouble(start -> start.pos().distSqr(golem.blockPosition())));
	}

	/** Whether {@code golem} holds no section of {@code mine} and has nothing free to pick there, loaded or not. */
	public boolean isWaiting(ServerLevel level, Mine mine, CopperGolem golem) {
		return !mine.isFinished() && claimed(mine, golem) < 0 && freeSections(level, mine, Set.of()).findAny().isEmpty() && freeEnds(mine, Set.of()).findAny().isEmpty();
	}

	/** Indices of the unfinished sections nobody holds a valid claim on, oldest first, leaving out those starting at {@code skipped}. */
	private Stream<Integer> freeSections(ServerLevel level, Mine mine, Set<OpenEnd> skipped) {
		return IntStream.range(0, mine.sections().size()).boxed()
			.filter(i -> !mine.sections().get(i).done() && !skipped.contains(mine.sections().get(i).start()))
			.filter(i -> {
				Claim claim = claims.get(new ClaimKey(mine.entrance(), mine.axis(), mine.sections().get(i).start()));
				return claim == null || !claim.isValid(level, mine);
			});
	}

	/** The Open Ends whose section (or the entrance) is done, oldest first, leaving out {@code skipped}. */
	private static Stream<OpenEnd> freeEnds(Mine mine, Set<OpenEnd> skipped) {
		return mine.ends().stream()
			.filter(end -> !skipped.contains(end))
			.filter(end -> mine.parentOf(end) < 0 || mine.sections().get(mine.parentOf(end)).done());
	}

	private int claim(ServerLevel level, Mine mine, int index, CopperGolem golem) {
		release(golem.getUUID());
		claims.put(new ClaimKey(mine.entrance(), mine.axis(), mine.sections().get(index).start()), new Claim(golem.getUUID(), level.getGameTime()));
		return index;
	}

	/** The index of the section {@code golem} holds a claim on in {@code mine}, lapsed or not, or -1. */
	public int claimed(Mine mine, CopperGolem golem) {
		Mine saved = find(mine.entrance(), mine.axis()).orElse(mine);
		for (int i = 0; i < saved.sections().size(); i++) {
			Claim claim = claims.get(new ClaimKey(saved.entrance(), saved.axis(), saved.sections().get(i).start()));
			if (claim != null && claim.golem().equals(golem.getUUID())) {
				return i;
			}
		}
		return -1;
	}

	/** Renews the claims of {@code golem}; one whose time ran out is dropped instead. */
	public void renew(ServerLevel level, CopperGolem golem) {
		long now = level.getGameTime();
		claims.replaceAll((key, claim) -> claim.golem().equals(golem.getUUID()) && now - claim.renewedAt() <= CLAIM_TICKS ? new Claim(claim.golem(), now) : claim);
		claims.values().removeIf(claim -> claim.golem().equals(golem.getUUID()) && now - claim.renewedAt() > CLAIM_TICKS);
	}

	/** Frees every claim {@code golem} holds: its job ended, it died, or its home changed. */
	public static void release(CopperGolem golem) {
		if (golem.level() instanceof ServerLevel level) {
			get(level).release(golem.getUUID());
		}
	}

	private void release(UUID golem) {
		claims.values().removeIf(claim -> claim.golem().equals(golem));
	}

	/** Frees every claim on {@code mine}: its frame is broken. */
	public void releaseAll(Mine mine) {
		claims.keySet().removeIf(key -> key.entrance().equals(mine.entrance()) && key.axis() == mine.axis());
	}

	/** How many rolled kinds an Open End tries before it gives up. */
	static final int TRIES = 3;

	/**
	 * The first of {@link #TRIES} rolled sections for {@code end} that isn't Blocked, or else a Barrier closing the end
	 * for the first one's reason. Null if a section's cells are not all loaded: no verdict yet.
	 */
	@Nullable Section roll(long worldSeed, MineLimits limits, Blocked.Site site, Mine mine, OpenEnd end) {
		int parent = mine.parentOf(end);
		List<SectionRoll.Candidate> candidates = SectionRoll.candidates(parent < 0 ? null : mine.sections().get(parent).kind(), mine.ends().size() == 1, end.pos().getY() > 0);
		List<SectionRoll.Candidate> order = SectionRoll.order(candidates, RandomSource.create(SectionRoll.seed(worldSeed, mine.entrance(), end)));
		BlockedReason reason = null;
		for (SectionRoll.Candidate candidate : order.subList(0, Math.min(TRIES, order.size()))) {
			Section section = Section.of(candidate.kind(), end, candidate.mirrored(), parent);
			if (!Blocked.loaded(site, section)) {
				return null;
			}
			BlockedReason blocked = Blocked.check(site, section, limits, mine.entrance(), this::taken);
			if (blocked == null) {
				return section;
			}
			reason = reason == null ? blocked : reason;
		}
		// A Straight always passes the follow rules, so there was at least one try.
		return Section.barrier(end, parent, reason);
	}

	/** Overlaps a saved section, or a Mine Entrance frame with its opening, of any mine here. */
	boolean taken(BoundingBox box) {
		return mines.stream().anyMatch(mine -> frame(mine).intersects(box) || mine.sections().stream().anyMatch(section -> section.box().intersects(box)));
	}

	private static BoundingBox frame(Mine mine) {
		Direction side = Direction.fromAxisAndDirection(mine.axis(), Direction.AxisDirection.POSITIVE);
		return BoundingBox.fromCorners(mine.entrance().relative(side, -1).below(2), mine.entrance().relative(side, 1));
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
			// Not rolled either, so nothing to retry: Blocked, it closes the end at once.
			Section first = Section.of(kind, firstEnd(found, facing), false, -1);
			Blocked.Site site = Blocked.Site.of(level);
			if (!Blocked.loaded(site, first)) {
				return null;
			}
			BlockedReason reason = Blocked.check(site, first, MineLimits.of(level), found.entrance(), mines::taken);
			found = mines.start(found, facing, reason == null ? first : Section.barrier(first.start(), -1, reason));
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
			.map(stored -> new Mines(stored.mines().stream().map(Mines::withoutUnknownKinds).toList(), null))
			.orElseGet(Mines::new);
	}

	/** A section of a kind nobody registered (its add-on was removed) is kept as saved, but done: nothing can dig it. */
	private static Mine withoutUnknownKinds(Mine mine) {
		return mine.with(mine.sections().stream().map(section -> MineSections.isKnown(section.kind()) ? section : section.at(section.progress(), true)).toList(), mine.ends());
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

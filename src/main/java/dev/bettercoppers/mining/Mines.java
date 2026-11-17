package dev.bettercoppers.mining;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.bettercoppers.BetterCoppers;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The mines of one dimension, saved as {@code bettercoppers:mines} ({@code data/bettercoppers/mines.dat}): server-only, never synced, not in {@code api}. A mine
 * is keyed by its entrance's middle beam and axis, so a frame rebuilt on the other axis is another mine.
 */
public final class Mines extends SavedData {
	/** The saved format. A newer one is logged and left alone; an older one goes through {@link #migrate}. */
	static final int FORMAT = 1;

	/** One mine. {@code facing} stays empty until the first miner takes it. */
	public record Mine(BlockPos entrance, Direction.Axis axis, Optional<Direction> facing) {
		static final Codec<Mine> CODEC = RecordCodecBuilder.create(i -> i.group(
			BlockPos.CODEC.fieldOf("entrance").forGetter(Mine::entrance),
			Direction.Axis.CODEC.fieldOf("axis").forGetter(Mine::axis),
			Direction.CODEC.optionalFieldOf("facing").forGetter(Mine::facing)
		).apply(i, Mine::new));
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
		mines.add(new Mine(entrance.immutable(), axis, Optional.empty()));
		setDirty();
		return true;
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

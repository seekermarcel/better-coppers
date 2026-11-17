package dev.bettercoppers.mining;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.bettercoppers.job.Load;
import dev.bettercoppers.registry.ModAttachments;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * Says a player made this golem a miner, and which mine it works in (its entrance's middle beam and axis, see
 * {@link Mines}). Stored on the golem as a Fabric attachment; what it carries is its {@link Load}. Fields of older
 * versions, such as a plan, are dropped on load.
 */
public final class MinerData {
	public static final Codec<MinerData> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
				BlockPos.CODEC.optionalFieldOf("mine").forGetter(data -> Optional.ofNullable(data.mine)),
				Direction.Axis.CODEC.optionalFieldOf("mine_axis").forGetter(data -> Optional.ofNullable(data.mineAxis))
			)
			.apply(instance, (mine, axis) -> {
				MinerData data = new MinerData();
				if (mine.isPresent() && axis.isPresent()) {
					data.setMine(mine.get(), axis.get());
				}
				return data;
			})
	);

	private @Nullable BlockPos mine;
	private Direction.@Nullable Axis mineAxis;
	/** Could not get out of the mine, by the Mine Path or straight. Not saved. */
	private boolean stuck;

	public static @Nullable MinerData get(CopperGolem golem) {
		return golem.getAttached(ModAttachments.MINER);
	}

	public static MinerData getOrCreate(CopperGolem golem) {
		return golem.getAttachedOrCreate(ModAttachments.MINER, MinerData::new);
	}

	public static boolean hasPickaxe(CopperGolem golem) {
		return golem.getMainHandItem().is(Items.COPPER_PICKAXE);
	}

	/** The middle beam of its mine's entrance, or null without a mine. */
	public @Nullable BlockPos mine() {
		return this.mine;
	}

	public Direction.@Nullable Axis mineAxis() {
		return this.mineAxis;
	}

	public void setMine(BlockPos entrance, Direction.Axis axis) {
		this.mine = entrance.immutable();
		this.mineAxis = axis;
	}

	public boolean isStuck() {
		return this.stuck;
	}

	public void setStuck(boolean stuck) {
		this.stuck = stuck;
	}

	public void clearMine() {
		this.mine = null;
		this.mineAxis = null;
	}
}

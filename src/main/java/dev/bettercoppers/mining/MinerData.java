package dev.bettercoppers.mining;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.bettercoppers.job.Load;
import dev.bettercoppers.job.Tools;
import dev.bettercoppers.registry.ModAttachments;
import java.util.Optional;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * Says a player made this golem a miner, and holds the plan it follows. Stored on the golem as a Fabric attachment;
 * what it carries is its {@link Load}.
 */
public final class MinerData {
	public static final Codec<MinerData> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
				MinePlan.CODEC.optionalFieldOf("plan").forGetter(data -> Optional.ofNullable(data.plan))
			)
			.apply(instance, plan -> {
				MinerData data = new MinerData();
				data.plan = plan.orElse(null);
				return data;
			})
	);

	private @Nullable MinePlan plan;

	public static @Nullable MinerData get(CopperGolem golem) {
		return golem.getAttached(ModAttachments.MINER);
	}

	public static MinerData getOrCreate(CopperGolem golem) {
		return golem.getAttachedOrCreate(ModAttachments.MINER, MinerData::new);
	}

	public static boolean hasPickaxe(CopperGolem golem) {
		return golem.getMainHandItem().is(Items.COPPER_PICKAXE);
	}

	/** A golem that was handed a pickaxe, or still has a load to bring home. Miners do not sort. */
	public static boolean isMiner(CopperGolem golem) {
		MinerData data = get(golem);
		return data != null && (hasPickaxe(golem) || Tools.hasLost(golem, Items.COPPER_PICKAXE) || !Load.of(golem).hasNothingToBringHome());
	}

	public @Nullable MinePlan plan() {
		return this.plan;
	}

	public void setPlan(@Nullable MinePlan plan) {
		this.plan = plan;
	}
}

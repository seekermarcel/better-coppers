package dev.bettercoppers.job;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.bettercoppers.registry.ModAttachments;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

/**
 * Says a player made this golem a farmer, forester, fisher, animal keeper or crafter, and holds a few remembered
 * places (the forester's tree stumps waiting for a sapling, and which sapling each one wants). Stored on the golem as
 * a Fabric attachment; what it carries is its {@link Load}.
 */
public final class WorkerData {
	private static final Codec<Pair<BlockPos, Item>> STUMP_SAPLING = Codec.pair(
		BlockPos.CODEC.fieldOf("pos").codec(),
		BuiltInRegistries.ITEM.byNameCodec().fieldOf("sapling").codec()
	);

	public static final Codec<WorkerData> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
				BlockPos.CODEC.listOf().optionalFieldOf("marks", List.of()).forGetter(data -> data.marks),
				STUMP_SAPLING.listOf().optionalFieldOf("stump_saplings", List.of()).forGetter(
					data -> data.stumpSaplings.entrySet().stream().map(entry -> Pair.of(entry.getKey(), entry.getValue())).toList()
				)
			)
			.apply(instance, (marks, stumpSaplings) -> {
				WorkerData data = new WorkerData();
				data.marks.addAll(marks);
				stumpSaplings.forEach(pair -> data.stumpSaplings.put(pair.getFirst(), pair.getSecond()));
				return data;
			})
	);

	private final List<BlockPos> marks = new ArrayList<>();
	private final Map<BlockPos, Item> stumpSaplings = new HashMap<>();

	public static @Nullable WorkerData get(CopperGolem golem) {
		return golem.getAttached(ModAttachments.WORKER);
	}

	public static WorkerData getOrCreate(CopperGolem golem) {
		return golem.getAttachedOrCreate(ModAttachments.WORKER, WorkerData::new);
	}

	/** The job of the tool in the golem's hand, if a player made it a worker. */
	public static @Nullable Job job(CopperGolem golem) {
		if (get(golem) == null) {
			return null;
		}
		Item lost = Tools.lostTool(golem);
		return lost != null ? Job.forTool(lost) : Job.forTool(golem.getMainHandItem());
	}

	public List<BlockPos> marks() {
		return this.marks;
	}

	/** The only sapling a marked stump takes: that of the tree that stood there. A mark without an entry takes any. */
	public Map<BlockPos, Item> stumpSaplings() {
		return this.stumpSaplings;
	}
}

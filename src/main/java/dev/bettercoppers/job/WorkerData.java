package dev.bettercoppers.job;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.bettercoppers.registry.ModAttachments;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

/**
 * Says a player made this golem a farmer, forester, fisher, animal keeper or crafter, and holds a few remembered
 * places (the forester's tree stumps waiting for a sapling). Stored on the golem as a Fabric attachment; what it
 * carries is its {@link Load}.
 */
public final class WorkerData {
	public static final Codec<WorkerData> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
				BlockPos.CODEC.listOf().optionalFieldOf("marks", List.of()).forGetter(data -> data.marks)
			)
			.apply(instance, marks -> {
				WorkerData data = new WorkerData();
				data.marks.addAll(marks);
				return data;
			})
	);

	private final List<BlockPos> marks = new ArrayList<>();

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
}

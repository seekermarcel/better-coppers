package dev.bettercoppers.job;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.bettercoppers.registry.ModAttachments;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Pouch and supplies of a farmer, forester, fisher, animal keeper or crafter golem, plus a few remembered places
 * (the forester's tree stumps waiting for a sapling). Stored on the golem as a Fabric attachment.
 */
public final class WorkerData {
	public static final Codec<WorkerData> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
				ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("pouch", List.of()).forGetter(data -> data.pouch),
				ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("supplies", List.of()).forGetter(data -> data.supplies),
				BlockPos.CODEC.listOf().optionalFieldOf("marks", List.of()).forGetter(data -> data.marks)
			)
			.apply(instance, (pouch, supplies, marks) -> {
				WorkerData data = new WorkerData();
				data.pouch.addAll(pouch.stream().filter(stack -> !stack.isEmpty()).toList());
				data.supplies.addAll(supplies.stream().filter(stack -> !stack.isEmpty()).toList());
				data.marks.addAll(marks);
				return data;
			})
	);

	private final List<ItemStack> pouch = new ArrayList<>();
	private final List<ItemStack> supplies = new ArrayList<>();
	private final List<BlockPos> marks = new ArrayList<>();

	public static @Nullable WorkerData get(CopperGolem golem) {
		return golem.getAttached(ModAttachments.WORKER);
	}

	public static WorkerData getOrCreate(CopperGolem golem) {
		return golem.getAttachedOrCreate(ModAttachments.WORKER, WorkerData::new);
	}

	/** The job of the tool in the golem's hand, if a player made it a worker. */
	public static @Nullable Job job(CopperGolem golem) {
		return get(golem) == null ? null : Job.forTool(golem.getMainHandItem());
	}

	/** Holds a job tool, or still has something in its pouch to bring home. Workers do not sort. */
	public static boolean isWorker(CopperGolem golem) {
		WorkerData data = get(golem);
		return data != null && (Job.forTool(golem.getMainHandItem()) != null || data.pouchCount() > 0);
	}

	public List<ItemStack> pouch() {
		return this.pouch;
	}

	public List<ItemStack> supplies() {
		return this.supplies;
	}

	public List<BlockPos> marks() {
		return this.marks;
	}

	public int pouchCount() {
		return this.pouch.stream().mapToInt(ItemStack::getCount).sum();
	}
}

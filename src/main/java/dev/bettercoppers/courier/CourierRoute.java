package dev.bettercoppers.courier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * A courier's route: everything in {@code from} goes to {@code to}, however far apart they are. On the way it walks
 * past each of the player's {@code stops} in turn, and back past them in reverse order.
 */
public record CourierRoute(BlockPos from, BlockPos to, List<BlockPos> stops) {
	/** The most stops one route takes. */
	public static final int MAX_STOPS = 32;

	public static final Codec<CourierRoute> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
				BlockPos.CODEC.fieldOf("from").forGetter(CourierRoute::from),
				BlockPos.CODEC.fieldOf("to").forGetter(CourierRoute::to),
				// Routes saved before stops existed have none.
				BlockPos.CODEC.listOf().optionalFieldOf("stops", List.of()).forGetter(CourierRoute::stops)
			)
			.apply(instance, CourierRoute::new)
	);

	public CourierRoute {
		stops = List.copyOf(stops);
	}

	public CourierRoute(BlockPos from, BlockPos to) {
		this(from, to, List.of());
	}

	/** The stops in the order a courier passes them on its way to {@code end}, one of the two route ends. */
	public List<BlockPos> stopsTowards(BlockPos end) {
		return end.equals(this.to) ? this.stops : this.stops.reversed();
	}
}

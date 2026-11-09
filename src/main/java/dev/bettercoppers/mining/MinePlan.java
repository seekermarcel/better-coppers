package dev.bettercoppers.mining;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a mineshaft plan describes. The entrance and heading are set by using the plan on a block; progress counts
 * finished steps so a plan handed back and forth resumes where it stopped. {@code givenUp} is the latest block where
 * a Mine Section was given up.
 */
public record MinePlan(MinePattern pattern, int length, Optional<BlockPos> entrance, Optional<Direction> heading, int progress, Optional<BlockPos> givenUp) {
	public static final int MIN_LENGTH = 8;
	/** Length of the plan the Survey Table offers first. */
	public static final int DEFAULT_LENGTH = 32;
	public static final int MAX_LENGTH = 128;

	public static final Codec<MinePlan> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
				MinePattern.CODEC.fieldOf("pattern").forGetter(MinePlan::pattern),
				Codec.intRange(MIN_LENGTH, MAX_LENGTH).fieldOf("length").forGetter(MinePlan::length),
				BlockPos.CODEC.optionalFieldOf("entrance").forGetter(MinePlan::entrance),
				Direction.CODEC.optionalFieldOf("heading").forGetter(MinePlan::heading),
				Codec.INT.optionalFieldOf("progress", 0).forGetter(MinePlan::progress),
				BlockPos.CODEC.optionalFieldOf("given_up").forGetter(MinePlan::givenUp)
			)
			.apply(instance, MinePlan::new)
	);
	public static final StreamCodec<ByteBuf, MinePlan> STREAM_CODEC = StreamCodec.composite(
		MinePattern.STREAM_CODEC, MinePlan::pattern,
		ByteBufCodecs.VAR_INT, MinePlan::length,
		ByteBufCodecs.optional(BlockPos.STREAM_CODEC), MinePlan::entrance,
		ByteBufCodecs.optional(Direction.STREAM_CODEC), MinePlan::heading,
		ByteBufCodecs.VAR_INT, MinePlan::progress,
		ByteBufCodecs.optional(BlockPos.STREAM_CODEC), MinePlan::givenUp,
		MinePlan::new
	);

	public static MinePlan blank(MinePattern pattern, int length) {
		return new MinePlan(pattern, length, Optional.empty(), Optional.empty(), 0, Optional.empty());
	}

	public boolean isMarked() {
		return this.entrance.isPresent() && this.heading.isPresent();
	}

	public MinePlan marked(BlockPos entrance, Direction heading) {
		return new MinePlan(this.pattern, this.length, Optional.of(entrance), Optional.of(heading), 0, Optional.empty());
	}

	public MinePlan withProgress(int progress) {
		return new MinePlan(this.pattern, this.length, this.entrance, this.heading, progress, this.givenUp);
	}

	/** Moves on to {@code progress} after giving up a Mine Section at {@code pos}. */
	public MinePlan givenUpAt(BlockPos pos, int progress) {
		return new MinePlan(this.pattern, this.length, this.entrance, this.heading, progress, Optional.of(pos));
	}

	/** The same mine, with nothing done yet. */
	public MinePlan unstarted() {
		return new MinePlan(this.pattern, this.length, this.entrance, this.heading, 0, Optional.empty());
	}
}

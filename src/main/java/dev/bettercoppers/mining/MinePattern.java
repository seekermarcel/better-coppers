package dev.bettercoppers.mining;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/**
 * The shapes of mine. Adding one takes a value here, a step generator in {@link MineLayout#build} (the switch there
 * will not compile without it) and the lang keys of its name and setting.
 */
public enum MinePattern implements StringRepresentable {
	/** Vanilla-style corridor with wooden supports and rails. */
	MINESHAFT("mineshaft", MinePlan.MAX_LENGTH, Setting.NONE),
	/** Walkable stairway going down from the entrance, its length being its depth. */
	STAIRCASE("staircase", MinePlan.MAX_LENGTH, Setting.NONE),
	/** A corridor with long, narrow branches to both sides, to expose as much ore as possible. */
	STRIP_MINE("strip_mine", MinePlan.MAX_LENGTH, new Setting("branch_length", 4, 32, 4, 16)),
	/** A wide 3-high hall with rock pillars left standing, and optional torches. */
	ROOM_AND_PILLAR("room_and_pillar", 64, new Setting("hall_width", 9, 17, 8, 9)),
	/** Mineshafts stacked straight down, joined by carved stairs at the entrance end. */
	MULTILEVEL_MINE("multilevel_mine", MinePlan.MAX_LENGTH, new Setting("levels", 2, 8, 1, 3));

	public static final Codec<MinePattern> CODEC = StringRepresentable.fromEnum(MinePattern::values);
	public static final StreamCodec<ByteBuf, MinePattern> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], MinePattern::ordinal);

	/** The one number a Mine Pattern lets the player choose besides length; the Survey Table moves it by {@code step}. */
	public record Setting(String name, int min, int max, int step, int defaultValue) {
		public static final Setting NONE = new Setting("none", 0, 0, 0, 0);

		public int moved(int value, int direction) {
			return Math.clamp(value + direction * this.step, this.min, this.max);
		}

		public Component displayName() {
			return Component.translatable("setting.bettercoppers." + this.name);
		}
	}

	private final String name;
	private final int maxLength;
	private final Setting setting;

	MinePattern(String name, int maxLength, Setting setting) {
		this.name = name;
		this.maxLength = maxLength;
		this.setting = setting;
	}

	public int maxLength() {
		return this.maxLength;
	}

	public Setting setting() {
		return this.setting;
	}

	public boolean hasSetting() {
		return this.setting != Setting.NONE;
	}

	/** The pattern after this one on the Survey Table, wrapping around. */
	public MinePattern next() {
		return values()[(this.ordinal() + 1) % values().length];
	}

	public Component displayName() {
		return Component.translatable("pattern.bettercoppers." + this.name);
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}
}

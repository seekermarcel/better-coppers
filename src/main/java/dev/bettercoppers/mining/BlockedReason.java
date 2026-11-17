package dev.bettercoppers.mining;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/** Why an Open End is a Closed End: what blocked its Mine Section, or a player's fence. Saved on the Barrier. */
public enum BlockedReason implements StringRepresentable {
	LAVA,
	WATER,
	CAVE,
	SURFACE,
	ANOTHER_TUNNEL,
	STRUCTURE,
	HARD_ROCK,
	TOO_FAR,
	TOO_DEEP,
	/** Closed by a player's fence: no sign. */
	PLAYER;

	public static final Codec<BlockedReason> CODEC = StringRepresentable.fromEnum(BlockedReason::values);

	@Override
	public String getSerializedName() {
		return this.name().toLowerCase(Locale.ROOT);
	}

	/** The reason as the Barrier's sign shows it. */
	public Component text() {
		return Component.translatable("mine.bettercoppers.blocked." + this.getSerializedName());
	}
}

package dev.bettercoppers.automatisation.registry;

import dev.bettercoppers.automatisation.Automatisation;
import dev.bettercoppers.automatisation.upgrade.GolemUpgrades;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

public final class AutoAttachments {
	/** The machine an operator golem works, linked by the player. */
	public static final AttachmentType<BlockPos> MACHINE = AttachmentRegistry.create(
		Automatisation.id("machine"), builder -> builder.persistent(BlockPos.CODEC)
	);

	/** Set when a player handed the golem its wrench; a wrench picked up by vanilla transport does not count. */
	public static final AttachmentType<Boolean> OPERATOR = AttachmentRegistry.create(
		Automatisation.id("operator"), builder -> builder.persistent(com.mojang.serialization.Codec.BOOL)
	);

	/** Plating, coating, gearwork and satchel a golem has been fitted with. */
	public static final AttachmentType<GolemUpgrades> UPGRADES = AttachmentRegistry.create(
		Automatisation.id("upgrades"), builder -> builder.persistent(GolemUpgrades.CODEC)
	);

	/** The spare part an operator fetched to repair its machine. */
	public static final AttachmentType<ItemStack> CARRIED_PART = AttachmentRegistry.create(
		Automatisation.id("carried_part"), builder -> builder.persistent(ItemStack.CODEC)
	);

	private AutoAttachments() {
	}

	public static void init() {
	}
}

package dev.bettercoppers.mixin;

import java.util.function.BiConsumer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Rolls vanilla's shearing loot, so a keeper can put the wool in its pouch instead of dropping it. */
@Mixin(LivingEntity.class)
public interface LivingEntityInvoker {
	@Invoker("dropFromShearingLootTable")
	void bettercoppers$dropFromShearingLootTable(ServerLevel level, ResourceKey<LootTable> table, ItemInstance tool, BiConsumer<ServerLevel, ItemStack> consumer);
}

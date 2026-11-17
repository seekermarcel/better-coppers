package dev.bettercoppers.mixin;

import dev.bettercoppers.mining.MineEntrance;
import dev.bettercoppers.mining.Mines;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A player placing a fence or plank may complete a Mine Entrance, and a fence may close an Open End. Blocks placed any other way never register one. */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
	@Inject(method = "place", at = @At("RETURN"))
	private void bettercoppers$registerMineEntrance(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> info) {
		if (info.getReturnValue().consumesAction() && context.getPlayer() instanceof ServerPlayer player && context.getLevel() instanceof ServerLevel level) {
			MineEntrance.onPlaced(player, level, context.getClickedPos(), level.getBlockState(context.getClickedPos()));
			Mines.onPlaced(level, context.getClickedPos(), level.getBlockState(context.getClickedPos()));
		}
	}
}

package dev.bettercoppers.upkeep;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.AABB;

/**
 * An axe in a dispenser scrapes the copper golem standing in front of it when the dispenser gets a redstone pulse:
 * one stage of rust off, or the wax. The axe stays in the dispenser and loses durability; with nothing to scrape it
 * dispenses like vanilla.
 */
public class AxeDispenseBehavior extends DefaultDispenseItemBehavior {
	private final DispenseItemBehavior fallback;
	private boolean scraped;

	private AxeDispenseBehavior(DispenseItemBehavior fallback) {
		this.fallback = fallback;
	}

	public static void register() {
		for (var axe : List.of(Items.WOODEN_AXE, Items.STONE_AXE, Items.COPPER_AXE, Items.IRON_AXE, Items.GOLDEN_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE)) {
			DispenseItemBehavior previous = DispenserBlock.DISPENSER_REGISTRY.get(axe);
			DispenserBlock.registerBehavior(axe, new AxeDispenseBehavior(previous != null ? previous : new DefaultDispenseItemBehavior()));
		}
	}

	@Override
	protected ItemStack execute(BlockSource source, ItemStack stack) {
		ServerLevel level = source.level();
		BlockPos front = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
		this.scraped = false;
		for (CopperGolem golem : level.getEntitiesOfClass(CopperGolem.class, new AABB(front))) {
			if (Upkeep.scrape(level, golem)) {
				stack.hurtAndBreak(1, level, null, item -> {});
				this.scraped = true;
				break;
			}
		}
		return this.scraped ? stack : this.fallback.dispense(source, stack);
	}

	/** The fallback has already made its own sound and animation. */
	@Override
	protected void playSound(BlockSource source) {
		if (this.scraped) {
			super.playSound(source);
		}
	}

	@Override
	protected void playAnimation(BlockSource source, Direction direction) {
		if (this.scraped) {
			super.playAnimation(source, direction);
		}
	}
}

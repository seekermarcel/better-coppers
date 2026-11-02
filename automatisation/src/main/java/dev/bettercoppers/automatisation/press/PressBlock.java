package dev.bettercoppers.automatisation.press;

import dev.bettercoppers.automatisation.machine.MachineBlock;
import dev.bettercoppers.automatisation.registry.AutoBlockEntities;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** A heavy press hanging over a belt: ingots become sheets, sugar cane becomes paper. */
public class PressBlock extends MachineBlock<PressBlockEntity> {
	public PressBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected BlockEntityType<PressBlockEntity> machineType() {
		return AutoBlockEntities.PRESS;
	}
}

package dev.bettercoppers.automatisation.belt;

import dev.bettercoppers.automatisation.machine.MachineBlock;
import dev.bettercoppers.automatisation.registry.AutoBlockEntities;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** A crank with a big gear: a golem turns it, and every belt line touching the drive runs. */
public class BeltDriveBlock extends MachineBlock<BeltDriveBlockEntity> {
	public BeltDriveBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected BlockEntityType<BeltDriveBlockEntity> machineType() {
		return AutoBlockEntities.BELT_DRIVE;
	}
}

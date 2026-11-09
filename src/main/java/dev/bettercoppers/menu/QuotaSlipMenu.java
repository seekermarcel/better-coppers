package dev.bettercoppers.menu;

import dev.bettercoppers.item.QuotaSlipItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * A Quota Slip's lines, on vanilla's double chest screen, so it needs no client code. The grid holds samples: an item
 * clicked onto it stays with the player. Every click is decided here and the result sent back to the client.
 */
public class QuotaSlipMenu extends ChestMenu {
	private static final int GRID_SIZE = 54;
	private static final int ROWS = 6;
	private static final int BIG_STEP = 16;

	private final ItemStack slip;

	public QuotaSlipMenu(int containerId, Inventory inventory, ItemStack slip) {
		super(MenuType.GENERIC_9x6, containerId, inventory, new SimpleContainer(GRID_SIZE), ROWS);
		this.slip = slip;
		NonNullList<ItemStack> lines = NonNullList.withSize(GRID_SIZE, ItemStack.EMPTY);
		slip.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(lines);
		for (int slot = 0; slot < GRID_SIZE; slot++) {
			this.getContainer().setItem(slot, lines.get(slot));
		}
	}

	@Override
	public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
		if (slotIndex >= 0 && slotIndex < GRID_SIZE) {
			this.clickLine(slotIndex, button, input);
		} else if (input == ContainerInput.QUICK_MOVE && slotIndex >= GRID_SIZE) {
			this.addSample(this.slots.get(slotIndex).getItem());
		} else if (input != ContainerInput.SWAP && !this.isTheSlip(slotIndex)) {
			super.clicked(slotIndex, button, input, player);
		}
		this.sendAllDataToRemote();
	}

	private boolean isTheSlip(int slotIndex) {
		return slotIndex >= GRID_SIZE && this.slots.get(slotIndex).getItem() == this.slip;
	}

	private void clickLine(int slot, int button, ContainerInput input) {
		ItemStack carried = this.getCarried();
		if (input == ContainerInput.PICKUP && !carried.isEmpty()) {
			this.getContainer().setItem(slot, QuotaSlipItem.line(carried.getItem(), button == 0 ? carried.getCount() : 1));
		} else if (input == ContainerInput.PICKUP) {
			this.adjust(slot, button == 0 ? 1 : -1);
		} else if (input == ContainerInput.QUICK_MOVE) {
			this.adjust(slot, button == 0 ? BIG_STEP : -BIG_STEP);
		}
	}

	private void adjust(int slot, int by) {
		ItemStack line = this.getContainer().getItem(slot);
		if (!line.isEmpty()) {
			this.getContainer().setItem(slot, QuotaSlipItem.line(line.getItem(), line.getCount() + by));
		}
	}

	private void addSample(ItemStack stack) {
		if (stack.isEmpty() || stack == this.slip) {
			return;
		}
		for (int slot = 0; slot < GRID_SIZE; slot++) {
			if (this.getContainer().getItem(slot).isEmpty()) {
				this.getContainer().setItem(slot, QuotaSlipItem.line(stack.getItem(), stack.getCount()));
				return;
			}
		}
	}

	/** The grid only holds samples, so nothing may be collected from it or dragged onto it. */
	@Override
	public boolean canTakeItemForPickAll(ItemStack carried, Slot slot) {
		return slot.container != this.getContainer() && super.canTakeItemForPickAll(carried, slot);
	}

	@Override
	public boolean canDragTo(Slot slot) {
		return slot.container != this.getContainer();
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return player.getMainHandItem() == this.slip || player.getOffhandItem() == this.slip;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		Container grid = this.getContainer();
		List<ItemStack> lines = new ArrayList<>();
		for (int slot = 0; slot < GRID_SIZE; slot++) {
			lines.add(grid.getItem(slot));
		}
		this.slip.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(lines));
	}
}

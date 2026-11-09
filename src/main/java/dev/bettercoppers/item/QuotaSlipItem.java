package dev.bettercoppers.item;

import dev.bettercoppers.home.Homes;
import dev.bettercoppers.menu.QuotaSlipMenu;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Lists how much of each item a chest should hold. Records a chest's contents on a sneak + right-click, is edited on a
 * right-click, and sets the quota of the chest it hangs on in an item frame. The lines are vanilla's container
 * component, which also gives the tooltip.
 */
public class QuotaSlipItem extends Item {
	public QuotaSlipItem(Properties properties) {
		super(properties);
	}

	/** One line of a slip: a plain item, between nothing and a full stack of it. */
	public static ItemStack line(Item item, int count) {
		ItemStack line = new ItemStack(item);
		int amount = Mth.clamp(count, 0, line.getMaxStackSize());
		return amount == 0 ? ItemStack.EMPTY : line.copyWithCount(amount);
	}

	/** The total listed per item, over all its lines. */
	public static Map<Item, Integer> listed(ItemStack slip) {
		Map<Item, Integer> listed = new HashMap<>();
		slip.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItemCopyStream()
			.forEach(line -> listed.merge(line.getItem(), line.getCount(), Integer::sum));
		return listed;
	}

	/** Makes the slip list exactly what the container holds. An empty container clears it. */
	public static void record(ItemStack slip, Container container) {
		List<ItemStack> lines = new ArrayList<>();
		for (ItemStack stack : container) {
			if (!stack.isEmpty()) {
				lines.add(line(stack.getItem(), stack.getCount()));
			}
		}
		slip.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(lines));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		Container container = Homes.containerAt(context.getLevel(), context.getClickedPos());
		if (player == null || !player.isSecondaryUseActive() || container == null) {
			return InteractionResult.PASS;
		}
		if (!context.getLevel().isClientSide()) {
			record(context.getItemInHand(), container);
			player.sendOverlayMessage(Component.translatable(container.isEmpty() ? "message.bettercoppers.quota_cleared" : "message.bettercoppers.quota_recorded"));
			context.getLevel().playSound(null, context.getClickedPos(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!level.isClientSide()) {
			ItemStack slip = player.getItemInHand(hand);
			player.openMenu(new SimpleMenuProvider((id, inventory, opener) -> new QuotaSlipMenu(id, inventory, slip), slip.getHoverName()));
		}
		return InteractionResult.SUCCESS;
	}
}

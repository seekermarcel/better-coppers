package dev.bettercoppers.home;

import dev.bettercoppers.api.GolemLinkTarget;
import dev.bettercoppers.api.GolemModifier;
import dev.bettercoppers.api.BetterCoppersApi;
import dev.bettercoppers.courier.CourierRoute;
import dev.bettercoppers.courier.Couriers;
import dev.bettercoppers.golem.Eyes;
import dev.bettercoppers.guide.Guide;
import dev.bettercoppers.job.JobHandover;
import dev.bettercoppers.job.Jobs;
import dev.bettercoppers.job.Tools;
import dev.bettercoppers.upkeep.GolemUpkeep;
import dev.bettercoppers.upkeep.Upkeep;
import com.mojang.datafixers.util.Pair;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Empty-hand clicks on a golem, and linking it to its home chest: sneak + right-click the golem with an empty hand (it
 * glows and shows its status), then right-click a copper chest within {@link #MAX_DISTANCE} blocks of it. The pick
 * expires after half a minute. A second press of sneak + empty hand on a golem with a job within {@link #END_JOB_TICKS}
 * ends the job. Clicks closer than {@link #SAME_PRESS_TICKS} are one press, as a held button repeats them, and the rest of
 * the press that ended a job does nothing.
 * <p>
 * Right-clicking a plain chest or barrel instead starts a courier route; the next chest or barrel clicked, at any
 * distance in the same dimension, ends it. There are {@link #ROUTE_TICKS ten minutes} to walk there.
 * <p>
 * A plain empty-hand click on a golem with a job only shows its status (ADR 0009).
 */
public final class HomeLinking {
	private static final int SELECTION_TICKS = 600;
	private static final int ROUTE_TICKS = 12000;
	private static final int END_JOB_TICKS = 100;
	/** A held button repeats a click every 4 ticks, and the server only sees the clicks. Closer than this is one press. */
	private static final int SAME_PRESS_TICKS = 5;
	private static final double MAX_DISTANCE = 32.0;
	/** A route this long earns the "Long Haul" guide step. */
	private static final int LONG_HAUL = 500;

	private record Selection(UUID golem, ResourceKey<Level> dimension, long until, @Nullable BlockPos routeStart, long endJobUntil, long lastClick) {
	}

	/** The press that ended a golem's job. */
	private record EndingPress(UUID golem, long lastClick) {
	}

	private static final Map<UUID, Selection> SELECTIONS = new HashMap<>();
	private static final Map<UUID, EndingPress> ENDING_PRESSES = new HashMap<>();

	private HomeLinking() {
	}

	/**
	 * Registered first, so it stops every other handler and vanilla. The tool that came back can land in the selected
	 * hand, and a held button would then hand it straight over again.
	 */
	public static InteractionResult onEndingPress(Player player, Level level, InteractionHand hand, Entity entity, @Nullable EntityHitResult hitResult) {
		EndingPress press = ENDING_PRESSES.get(player.getUUID());
		if (!(level instanceof ServerLevel) || press == null || !press.golem().equals(entity.getUUID())) {
			return InteractionResult.PASS;
		}
		if (level.getGameTime() - press.lastClick() > SAME_PRESS_TICKS) {
			ENDING_PRESSES.remove(player.getUUID());
			return InteractionResult.PASS;
		}
		ENDING_PRESSES.put(player.getUUID(), new EndingPress(press.golem(), level.getGameTime()));
		return InteractionResult.SUCCESS;
	}

	public static InteractionResult onUseEntity(Player player, Level level, InteractionHand hand, Entity entity, @Nullable EntityHitResult hitResult) {
		if (!(entity instanceof CopperGolem golem) || player.isSpectator() || !player.getItemInHand(hand).isEmpty()) {
			return InteractionResult.PASS;
		}
		if (hand == InteractionHand.MAIN_HAND && player.isShiftKeyDown()) {
			if (level instanceof ServerLevel serverLevel) {
				sneakClick(player, serverLevel, golem);
			}
			return InteractionResult.SUCCESS;
		}
		// The client does not know the job, so it ran vanilla's throw and the server has to answer. Vanilla throws for any
		// empty hand: also for the off hand, when the main hand's item did nothing.
		if (!(level instanceof ServerLevel) || !Jobs.has(golem)) {
			return InteractionResult.PASS;
		}
		if (hand == InteractionHand.MAIN_HAND) {
			player.sendOverlayMessage(status(golem));
		}
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.connection.send(new ClientboundSetEquipmentPacket(golem.getId(), List.of(Pair.of(EquipmentSlot.MAINHAND, golem.getMainHandItem().copy()))));
		}
		return InteractionResult.SUCCESS;
	}

	private static void sneakClick(Player player, ServerLevel level, CopperGolem golem) {
		long now = level.getGameTime();
		Selection previous = SELECTIONS.get(player.getUUID());
		boolean sameGolem = previous != null && previous.golem().equals(golem.getUUID());
		if (sameGolem && now - previous.lastClick() <= SAME_PRESS_TICKS) {
			SELECTIONS.put(player.getUUID(), new Selection(previous.golem(), previous.dimension(), previous.until(), previous.routeStart(), previous.endJobUntil(), now));
			return;
		}
		boolean hasJob = Jobs.has(golem);
		if (hasJob && sameGolem && now <= previous.endJobUntil()) {
			SELECTIONS.remove(player.getUUID());
			ENDING_PRESSES.put(player.getUUID(), new EndingPress(golem.getUUID(), now));
			golem.removeEffect(MobEffects.GLOWING);
			JobHandover.end(player, golem);
			golem.playSound(SoundEvents.COPPER_GOLEM_ITEM_NO_GET);
			player.sendOverlayMessage(Component.translatable("message.bettercoppers.job_ended"));
			return;
		}
		SELECTIONS.put(player.getUUID(), new Selection(golem.getUUID(), level.dimension(), now + SELECTION_TICKS, null, hasJob ? now + END_JOB_TICKS : 0L, now));
		golem.addEffect(new MobEffectInstance(MobEffects.GLOWING, SELECTION_TICKS, 0, false, false));
		golem.playSound(SoundEvents.COPPER_GOLEM_ITEM_GET, 0.6F, 1.4F);
		player.sendSystemMessage(Component.translatable(hasJob ? "message.bettercoppers.selected_job" : "message.bettercoppers.selected", status(golem)));
	}

	/** The golem got a job, which no first click warned about: the next sneak-click must not end it. */
	public static void closeEndJobWindow(CopperGolem golem) {
		SELECTIONS.replaceAll((player, s) -> s.golem().equals(golem.getUUID())
			? new Selection(s.golem(), s.dimension(), s.until(), s.routeStart(), 0L, s.lastClick()) : s);
	}

	/** Job, place, energy and rust, for the status message. */
	private static Component status(CopperGolem golem) {
		Component job = Jobs.name(golem);
		CourierRoute route = Couriers.route(golem);
		BlockPos place = Jobs.place(golem);
		Component placeText = route != null
			? Component.translatable("status.bettercoppers.route", route.from().toShortString(), route.to().toShortString())
			: place == null
				? Component.translatable("status.bettercoppers.no_home")
				: Component.translatable(place.equals(Homes.get(golem)) ? "status.bettercoppers.home" : "status.bettercoppers.workplace", place.getX(), place.getY(), place.getZ());
		if (!Jobs.isPaying(golem)) {
			return Component.translatable("message.bettercoppers.status_free", job, placeText);
		}
		int percent = Upkeep.get(golem).energy() * 100 / GolemUpkeep.MAX_ENERGY;
		String rust = Upkeep.isWaxed(golem) ? "waxed" : golem.getWeatherState().getSerializedName();
		MutableComponent status = Component.translatable(
			"message.bettercoppers.status", job, placeText, percent, Component.translatable("rust.bettercoppers." + rust)
		);
		Item lost = Tools.lostTool(golem);
		if (lost != null) {
			status.append(", ").append(Component.translatable("status.bettercoppers.lost_tool", new ItemStack(lost).getHoverName()));
		}
		if (Eyes.tier(golem) > 0) {
			status.append(", ").append(Component.translatable("status.bettercoppers.eyes", Math.round(Eyes.multiplier(golem) * 100)));
		}
		for (GolemModifier modifier : BetterCoppersApi.modifiers()) {
			Component extra = modifier.status(golem);
			if (extra != null) {
				status.append(", ").append(extra);
			}
		}
		return status;
	}

	public static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
		if (hand != InteractionHand.MAIN_HAND || !(level instanceof ServerLevel serverLevel)) {
			// The client cannot know about the pick; the server answer corrects it.
			return InteractionResult.PASS;
		}
		Selection selection = SELECTIONS.get(player.getUUID());
		BlockPos pos = hitResult.getBlockPos();
		if (selection == null) {
			return InteractionResult.PASS;
		}
		if (serverLevel.getGameTime() > selection.until() || serverLevel.dimension() != selection.dimension()) {
			SELECTIONS.remove(player.getUUID());
			return InteractionResult.PASS;
		}
		BlockState state = level.getBlockState(pos);
		if (selection.routeStart() == null && player instanceof ServerPlayer serverPlayer) {
			for (GolemLinkTarget target : BetterCoppersApi.linkTargets()) {
				if (target.accepts(state)) {
					SELECTIONS.remove(player.getUUID());
					if (!(serverLevel.getEntity(selection.golem()) instanceof CopperGolem golem)) {
						return InteractionResult.PASS;
					}
					golem.removeEffect(MobEffects.GLOWING);
					return target.link(serverPlayer, serverLevel, golem, pos);
				}
			}
		}
		if (selection.routeStart() != null) {
			return Homes.canEndRoute(state) ? endRoute(player, serverLevel, selection, pos) : InteractionResult.PASS;
		}
		if (Couriers.canStartRoute(state)) {
			if (refuseRoute(player, serverLevel, selection)) {
				return InteractionResult.FAIL;
			}
			SELECTIONS.put(player.getUUID(), new Selection(selection.golem(), selection.dimension(), serverLevel.getGameTime() + ROUTE_TICKS, pos, 0L, selection.lastClick()));
			serverLevel.playSound(null, pos, SoundEvents.COPPER_GOLEM_ITEM_GET, SoundSource.NEUTRAL, 1.0F, 1.2F);
			player.sendOverlayMessage(Component.translatable("message.bettercoppers.route_start"));
			return InteractionResult.SUCCESS;
		}
		if (!state.is(BlockTags.COPPER_CHESTS)) {
			return InteractionResult.PASS;
		}
		SELECTIONS.remove(player.getUUID());
		if (!(serverLevel.getEntity(selection.golem()) instanceof CopperGolem golem)) {
			return InteractionResult.PASS;
		}
		if (golem.position().distanceTo(Vec3.atCenterOf(pos)) > MAX_DISTANCE) {
			player.sendOverlayMessage(Component.translatable("message.bettercoppers.home_too_far"));
			return InteractionResult.FAIL;
		}
		Couriers.setRoute(golem, null);
		Homes.set(golem, pos);
		golem.removeEffect(MobEffects.GLOWING);
		serverLevel.playSound(null, pos, SoundEvents.COPPER_GOLEM_ITEM_DROP, SoundSource.NEUTRAL, 1.0F, 1.2F);
		HomeDisplay.drawLine(serverLevel, golem, pos, null);
		player.sendOverlayMessage(Component.translatable("message.bettercoppers.home_set", pos.getX(), pos.getY(), pos.getZ()));
		Guide.award(player, "home");
		return InteractionResult.SUCCESS;
	}

	/** A golem with a job takes no route. Forgets the pick, so the next click on a chest opens it again. */
	private static boolean refuseRoute(Player player, ServerLevel level, Selection selection) {
		if (!(level.getEntity(selection.golem()) instanceof CopperGolem golem) || !Jobs.has(golem)) {
			return false;
		}
		SELECTIONS.remove(player.getUUID());
		golem.removeEffect(MobEffects.GLOWING);
		player.sendOverlayMessage(Component.translatable("message.bettercoppers.route_has_job"));
		return true;
	}

	private static InteractionResult endRoute(Player player, ServerLevel level, Selection selection, BlockPos pos) {
		if (refuseRoute(player, level, selection)) {
			return InteractionResult.FAIL;
		}
		BlockPos start = selection.routeStart();
		if (Homes.sameContainer(level, start).contains(pos)) {
			player.sendOverlayMessage(Component.translatable("message.bettercoppers.route_same_chest"));
			return InteractionResult.FAIL;
		}
		SELECTIONS.remove(player.getUUID());
		Couriers.assign(level, selection.golem(), new CourierRoute(start, pos));
		if (level.getEntity(selection.golem()) instanceof CopperGolem golem) {
			golem.removeEffect(MobEffects.GLOWING);
		}
		level.playSound(null, pos, SoundEvents.COPPER_GOLEM_ITEM_DROP, SoundSource.NEUTRAL, 1.0F, 1.2F);
		player.sendOverlayMessage(Component.translatable("message.bettercoppers.route_set", (int) Math.sqrt(start.distSqr(pos))));
		Guide.job(player, "courier");
		if (start.distSqr(pos) >= LONG_HAUL * LONG_HAUL) {
			Guide.award(player, "long_haul");
		}
		return InteractionResult.SUCCESS;
	}
}

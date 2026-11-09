package dev.bettercoppers.api;

import dev.bettercoppers.entity.CopperChestMinecart;
import dev.bettercoppers.golem.Eyes;
import dev.bettercoppers.golem.Inventories;
import dev.bettercoppers.home.HomeDisplay;
import dev.bettercoppers.guide.Guide;
import dev.bettercoppers.job.JobHandover;
import dev.bettercoppers.job.Jobs;
import dev.bettercoppers.runningcosts.RunningCosts;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Hooks for add-ons. Register everything during mod initialisation, before any golem loads.
 * <ul>
 * <li>{@link #registerJob}: a new job (see {@link GolemJob}).</li>
 * <li>{@link #registerBehavior}: a behaviour added to every copper golem's brain, next to the built-in jobs. It
 * should only start for its own job and leave the golem alone otherwise.</li>
 * <li>{@link #registerLinkTarget}: blocks golems can be linked to.</li>
 * <li>{@link #registerUpgrade}: upgrades that change speed, rusting or how much a golem carries.</li>
 * <li>{@link #registerChargingSpot}: blocks golems recharge on.</li>
 * <li>{@link #registerHandbookChapter}: chapters for the Golem Handbook; {@link #awardGuide} unlocks guide
 * advancements.</li>
 * <li>{@link #CREATIVE_TAB}: the mod's creative tab. Add items with
 * {@code CreativeModeTabEvents.modifyOutputEvent(CREATIVE_TAB)}, which works whichever mod initialises first.</li>
 * </ul>
 */
public final class BetterCoppersApi {
	/** Key of the "Better Coppers" creative tab. */
	public static final ResourceKey<CreativeModeTab> CREATIVE_TAB = ResourceKey.create(
		Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("bettercoppers", "main")
	);
	private static final List<GolemJob> JOBS = new ArrayList<>();
	private static final List<Supplier<? extends Behavior<CopperGolem>>> BEHAVIORS = new ArrayList<>();
	private static final List<GolemLinkTarget> LINK_TARGETS = new ArrayList<>();
	private static final List<GolemUpgrade> UPGRADES = new ArrayList<>();
	private static final List<ChargingSpot> CHARGING_SPOTS = new ArrayList<>();
	private static final List<Identifier> HANDBOOK_CHAPTERS = new ArrayList<>();

	private BetterCoppersApi() {
	}

	/**
	 * A chapter for the Golem Handbook, after the built-in ones. Its title and text come from the translation keys
	 * {@code handbook.<namespace>.<path>.title} and {@code .text}. In the text, a line starting with "# " is a heading
	 * and a blank line starts a new paragraph; pages are laid out automatically.
	 */
	public static void registerHandbookChapter(Identifier chapter) {
		HANDBOOK_CHAPTERS.add(chapter);
	}

	public static List<Identifier> handbookChapters() {
		return HANDBOOK_CHAPTERS;
	}

	/**
	 * Unlocks a step of an add-on's guide advancements: an advancement with an "impossible" criterion named
	 * {@code done}. Does nothing on the client.
	 */
	public static void awardGuide(Player player, Identifier advancement) {
		Guide.award(player, advancement, Guide.CRITERION);
	}

	/** The same, for every player within sight of something a golem or machine did. */
	public static void awardGuideNearby(ServerLevel level, Vec3 pos, Identifier advancement) {
		Guide.awardNearby(level, pos, advancement);
	}

	public static void registerJob(GolemJob job) {
		JOBS.add(job);
	}

	/** A fresh behaviour for each golem: behaviours keep per-golem state. */
	public static void registerBehavior(Supplier<? extends Behavior<CopperGolem>> factory) {
		BEHAVIORS.add(factory);
	}

	public static void registerLinkTarget(GolemLinkTarget target) {
		LINK_TARGETS.add(target);
	}

	public static void registerUpgrade(GolemUpgrade upgrade) {
		UPGRADES.add(upgrade);
	}

	public static void registerChargingSpot(ChargingSpot spot) {
		CHARGING_SPOTS.add(spot);
	}

	public static List<GolemUpgrade> upgrades() {
		return UPGRADES;
	}

	/** The charging spot this block is, or null for an ordinary block. */
	public static @Nullable ChargingSpot chargingSpot(net.minecraft.world.level.block.state.BlockState state) {
		for (ChargingSpot spot : CHARGING_SPOTS) {
			if (spot.isSpot(state)) {
				return spot;
			}
		}
		return null;
	}

	/** A work radius for this golem, widened by its eye upgrade (1.5x iron, 2x gold, 3x diamond). */
	public static int radius(CopperGolem golem, int base) {
		return Eyes.radius(golem, base);
	}

	/** How many items this golem carries in its hand, upgrades included. */
	public static int carryLimit(CopperGolem golem, int base) {
		int limit = base;
		for (GolemUpgrade upgrade : UPGRADES) {
			limit = upgrade.carryLimit(golem, limit);
		}
		return limit;
	}

	public static List<GolemJob> jobs() {
		return JOBS;
	}

	public static List<Supplier<? extends Behavior<CopperGolem>>> behaviors() {
		return BEHAVIORS;
	}

	public static List<GolemLinkTarget> linkTargets() {
		return LINK_TARGETS;
	}

	/** The registered job this golem has, if any. */
	public static @Nullable GolemJob jobOf(CopperGolem golem) {
		for (GolemJob job : JOBS) {
			if (job.has(golem)) {
				return job;
			}
		}
		return null;
	}

	/**
	 * Puts one of {@code held} in the golem's hand, as the base mod's job items do: the player gets back what the golem
	 * carries for any other job, whose data goes. Then record the player's act for {@link GolemJob#has}.
	 */
	public static void giveTool(Player player, CopperGolem golem, ItemStack held) {
		JobHandover.giveTool(player, golem, held);
	}

	/** Whether the golem has energy and is not rusted shut or otherwise stopped: the check for starting work. */
	public static boolean canWork(ServerLevel level, CopperGolem golem) {
		return RunningCosts.canWork(level, golem);
	}

	/** How fast the golem moves and works, 1 being normal. */
	public static float workSpeed(CopperGolem golem) {
		return RunningCosts.speed(golem);
	}

	/** Whether the golem's job has running costs: not a sorter, not a courier, and not a golem without a job. */
	public static boolean paysRunningCosts(CopperGolem golem) {
		return Jobs.isPaying(golem);
	}

	public static void spendEnergy(CopperGolem golem, int energy) {
		RunningCosts.spend(golem, energy);
	}

	/** Draws the particle line from the golem to a linked block, as for a home. */
	public static void drawLinkLine(ServerLevel level, CopperGolem golem, BlockPos pos) {
		HomeDisplay.drawLine(level, golem, pos, null);
	}

	public static boolean hasRoom(Container container, ItemStack item) {
		return Inventories.hasRoom(container, item);
	}

	public static boolean contains(Container container, Predicate<ItemStack> filter) {
		return Inventories.contains(container, filter);
	}

	/** Whether this is a Copper Chest Minecart that is not yet a double chest. */
	public static boolean canExtendCart(Entity entity) {
		return entity instanceof CopperChestMinecart cart && !cart.isExtended();
	}

	/** Makes a Copper Chest Minecart a double chest. Only call after {@link #canExtendCart}. */
	public static void extendCart(Entity entity, ItemStack extension) {
		((CopperChestMinecart) entity).extend(extension);
	}
}

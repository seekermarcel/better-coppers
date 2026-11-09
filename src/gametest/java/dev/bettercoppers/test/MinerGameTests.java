package dev.bettercoppers.test;

import dev.bettercoppers.home.HomeLinking;
import dev.bettercoppers.home.Homes;
import dev.bettercoppers.job.Load;
import dev.bettercoppers.menu.SurveyTableMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import dev.bettercoppers.mining.MineLayout;
import dev.bettercoppers.mining.MinePattern;
import dev.bettercoppers.mining.MinePlan;
import dev.bettercoppers.mining.MinerData;
import dev.bettercoppers.registry.ModAttachments;
import dev.bettercoppers.registry.ModComponents;
import dev.bettercoppers.registry.ModItems;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Describes the miner's current behaviour: supplies, ore and fluids. The scene is a stone block with a shaft heading north. */
public class MinerGameTests {
	private static final Block COPPER_CHEST = Blocks.COPPER_CHEST.weathering().unaffected();
	private static final BlockPos HOME = new BlockPos(1, 1, 7);
	private static final BlockPos ENTRANCE = new BlockPos(4, 1, 5);
	/** Beside the shaft (its east wall is x = 5), in the second slice. */
	private static final BlockPos WALL = new BlockPos(6, 2, 4);
	private static final BlockPos START = new BlockPos(4, 1, 6);
	/** A slice is 9 digs and 3 floors, plus supports; every step counts as one progress. */
	private static final int TWO_SLICES = 24;

	private record Scene(CopperGolem golem, Container chest, MinePlan plan) {
	}

	/** Stone block, daytime, a miner with plan and pickaxe, and a home chest with supplies or empty. */
	private static Scene scene(GameTestHelper h, boolean supplies, BlockPos start) {
		ServerLevel level = h.getLevel();
		level.clockManager().setTotalTicks(level.dimensionType().defaultClock().orElseThrow(), 1000L);
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
		for (int x = 2; x <= 7; x++) {
			for (int y = 1; y <= 4; y++) {
				for (int z = 0; z <= 5; z++) {
					h.setBlock(new BlockPos(x, y, z), Blocks.STONE);
				}
			}
		}
		Container chest = GameTests.chest(h, HOME, COPPER_CHEST);
		if (supplies) {
			chest.setItem(0, new ItemStack(Items.RAIL, 16));
			chest.setItem(1, new ItemStack(Items.OAK_PLANKS, 32));
			chest.setItem(2, new ItemStack(Items.OAK_FENCE, 16));
			chest.setItem(3, new ItemStack(Items.TORCH, 16));
		}
		CopperGolem golem = GameTests.golem(h, start);
		GameTests.energize(golem);
		MinePlan plan = MinePlan.blank(MinePattern.MINESHAFT, 8).marked(h.absolutePos(ENTRANCE), Direction.NORTH);
		ItemStack planStack = new ItemStack(ModItems.MINESHAFT_PLAN);
		planStack.set(ModComponents.MINE_PLAN, plan);
		give(h, golem, new ItemStack(Items.COPPER_PICKAXE));
		give(h, golem, planStack);
		Homes.set(golem, h.absolutePos(HOME));
		return new Scene(golem, chest, plan);
	}

	private static void give(GameTestHelper h, CopperGolem golem, ItemStack stack) {
		Player player = h.makeMockPlayer(GameType.SURVIVAL);
		player.setShiftKeyDown(true);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		UseEntityCallback.EVENT.invoker().interact(player, h.getLevel(), InteractionHand.MAIN_HAND, golem, null);
	}

	/** Turns the first two slices to glass, which drops nothing, so the miner gets no filler from digging them. */
	private static void glassSlices(GameTestHelper h) {
		for (int x = 3; x <= 5; x++) {
			for (int y = 1; y <= 3; y++) {
				for (int z = 4; z <= 5; z++) {
					h.setBlock(new BlockPos(x, y, z), Blocks.GLASS);
				}
			}
		}
	}

	/** The screen sends these button ids to the menu on the server. */
	@GameTest
	public void survey_table_makes_the_plan_shown_on_its_panel(GameTestHelper h) {
		Player player = h.makeMockPlayer(GameType.SURVIVAL);
		SurveyTableMenu menu = new SurveyTableMenu(1, player.getInventory());
		menu.getSlot(0).set(new ItemStack(Items.PAPER));
		menu.clickMenuButton(player, SurveyTableMenu.BUTTON_NEXT_PATTERN);
		h.assertTrue(menu.getPattern() != MinePattern.MINESHAFT, Component.literal("the button moves on to another Mine Pattern"));
		for (int i = 1; i < MinePattern.values().length; i++) {
			menu.clickMenuButton(player, SurveyTableMenu.BUTTON_NEXT_PATTERN);
		}
		h.assertTrue(menu.getPattern() == MinePattern.MINESHAFT, Component.literal("a full round comes back to the Mineshaft"));
		for (int i = 0; i < 20; i++) {
			menu.clickMenuButton(player, SurveyTableMenu.BUTTON_LONGER);
		}
		menu.clickMenuButton(player, SurveyTableMenu.BUTTON_MORE);
		MinePlan plan = menu.getSlot(1).getItem().get(ModComponents.MINE_PLAN);
		h.assertTrue(plan != null && plan.pattern() == MinePattern.MINESHAFT, Component.literal("the result is a Mineshaft plan"));
		h.assertTrue(plan.length() == 128, Component.literal("length stops at the pattern's maximum: " + plan.length()));
		h.assertTrue(plan.setting() == 0, Component.literal("a pattern without a setting ignores its buttons: " + plan.setting()));
		h.succeed();
	}

	private static final int STAIR_DEPTH = 32;
	private static final int STAIR_TOP = STAIR_DEPTH + 2;
	private static final BlockPos STAIR_HOME = new BlockPos(2, STAIR_TOP, 0);

	/** The block a golem stands in on step {@code i} of the scene's stairs, which head south along x = 4 from z = 0. */
	private static BlockPos stairStep(int i) {
		return new BlockPos(4, STAIR_TOP - i, i);
	}

	/**
	 * Solid stone under a stone roof, entrance on the top edge. A hole in the floor of the middle step needs a patch,
	 * the torches have to be fetched from the chest, and the load goes all the way back up.
	 */
	@GameTest(maxTicks = 6000, padding = 48)
	public void miner_digs_a_long_staircase_to_the_end_and_carries_the_load_home(GameTestHelper h) {
		ServerLevel level = h.getLevel();
		level.clockManager().setTotalTicks(level.dimensionType().defaultClock().orElseThrow(), 1000L);
		for (int x = 2; x <= 6; x++) {
			for (int z = 0; z <= STAIR_DEPTH + 1; z++) {
				for (int y = 1; y <= STAIR_TOP + 2; y++) {
					boolean aboveTheLanding = z == 0 && y >= STAIR_TOP;
					h.setBlock(new BlockPos(x, y, z), aboveTheLanding ? Blocks.AIR : Blocks.STONE);
				}
			}
		}
		// Longer than the chunks a test keeps loaded, and entities freeze at their border.
		for (int z = 0; z <= STAIR_DEPTH + 1; z++) {
			for (int x : new int[] {2, 6}) {
				BlockPos pos = h.absolutePos(new BlockPos(x, 0, z));
				level.setChunkForced(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()), true);
			}
		}
		h.setBlock(stairStep(STAIR_DEPTH / 2).below(), Blocks.AIR);
		Container chest = GameTests.chest(h, STAIR_HOME, COPPER_CHEST);
		chest.setItem(0, new ItemStack(Items.TORCH, 16));
		CopperGolem golem = GameTests.golem(h, new BlockPos(3, STAIR_TOP, 0));
		GameTests.energize(golem);
		ItemStack planStack = new ItemStack(ModItems.MINESHAFT_PLAN);
		planStack.set(ModComponents.MINE_PLAN, MinePlan.blank(MinePattern.STAIRCASE, STAIR_DEPTH).marked(h.absolutePos(new BlockPos(4, STAIR_TOP, 1)), Direction.SOUTH));
		give(h, golem, new ItemStack(Items.COPPER_PICKAXE));
		give(h, golem, planStack);
		Homes.set(golem, h.absolutePos(STAIR_HOME));
		h.succeedWhen(() -> {
			String where = " golem at " + h.relativePos(golem.blockPosition()) + ", progress " + progress(golem);
			for (int i = 1; i <= STAIR_DEPTH; i++) {
				h.assertTrue(h.getBlockState(stairStep(i)).isAir(), "step " + i + " not dug;" + where);
				h.assertTrue(!h.getBlockState(stairStep(i).above()).isSolid(), "no headroom on step " + i + ";" + where);
				h.assertTrue(h.getBlockState(stairStep(i).below()).isSolid(), "no floor under step " + i + ";" + where);
			}
			h.assertTrue(h.getBlockState(stairStep(4).above()).is(Blocks.WALL_TORCH), "no torch on step 4;" + where);
			h.assertTrue(chest.countItem(Items.COBBLESTONE) > 0, "the load is not home;" + where);
			h.assertTrue(Load.of(golem).hasNothingToBringHome(), "still carrying its load;" + where);
			h.assertValueEqual(progress(golem), MineLayout.build(MinerData.get(golem).plan(), level.getMinY()).size(), "steps done");
		});
	}

	@GameTest
	public void survey_table_makes_a_strip_mine_plan_with_its_branch_length(GameTestHelper h) {
		Player player = h.makeMockPlayer(GameType.SURVIVAL);
		SurveyTableMenu menu = new SurveyTableMenu(1, player.getInventory());
		menu.getSlot(0).set(new ItemStack(Items.PAPER));
		while (menu.getPattern() != MinePattern.STRIP_MINE) {
			menu.clickMenuButton(player, SurveyTableMenu.BUTTON_NEXT_PATTERN);
		}
		h.assertValueEqual(menu.getSetting(), 16, "default branch length");
		menu.clickMenuButton(player, SurveyTableMenu.BUTTON_LESS);
		MinePlan plan = menu.getSlot(1).getItem().get(ModComponents.MINE_PLAN);
		h.assertTrue(plan != null && plan.pattern() == MinePattern.STRIP_MINE, Component.literal("the result is a Strip Mine plan"));
		h.assertValueEqual(plan.setting(), 12, "branch length");
		for (int i = 0; i < 20; i++) {
			menu.clickMenuButton(player, SurveyTableMenu.BUTTON_MORE);
		}
		h.assertValueEqual(menu.getSetting(), 32, "longest branch");
		h.succeed();
	}

	/**
	 * A rock block big enough for an 8 long Strip Mine with the shortest branches, heading north from (8, 1, 10). Runs
	 * past the test's own structure, so its chunks are kept loaded for the golem to walk in.
	 */
	@GameTest(maxTicks = 9600, padding = 48)
	public void miner_digs_a_strip_mine_to_the_end(GameTestHelper h) {
		ServerLevel level = h.getLevel();
		level.clockManager().setTotalTicks(level.dimensionType().defaultClock().orElseThrow(), 1000L);
		for (int cx = h.absolutePos(BlockPos.ZERO).getX() >> 4; cx <= h.absolutePos(new BlockPos(16, 0, 0)).getX() >> 4; cx++) {
			for (int cz = h.absolutePos(BlockPos.ZERO).getZ() >> 4; cz <= h.absolutePos(new BlockPos(0, 0, 16)).getZ() >> 4; cz++) {
				level.setChunkForced(cx, cz, true);
			}
		}
		for (BlockPos pos : BlockPos.betweenClosed(BlockPos.ZERO, new BlockPos(16, 0, 16))) {
			h.setBlock(pos, Blocks.STONE);
		}
		for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(1, 1, 1), new BlockPos(15, 4, 10))) {
			h.setBlock(pos, Blocks.STONE);
		}
		BlockPos home = new BlockPos(5, 1, 13);
		Container chest = GameTests.chest(h, home, COPPER_CHEST);
		chest.setItem(0, new ItemStack(Items.RAIL, 16));
		chest.setItem(1, new ItemStack(Items.OAK_PLANKS, 32));
		chest.setItem(2, new ItemStack(Items.OAK_FENCE, 16));
		chest.setItem(3, new ItemStack(Items.TORCH, 16));
		CopperGolem golem = GameTests.golem(h, new BlockPos(8, 1, 12));
		GameTests.energize(golem);
		MinePlan plan = MinePlan.blank(MinePattern.STRIP_MINE, 8, 4).marked(h.absolutePos(new BlockPos(8, 1, 10)), Direction.NORTH);
		ItemStack planStack = new ItemStack(ModItems.MINESHAFT_PLAN);
		planStack.set(ModComponents.MINE_PLAN, plan);
		give(h, golem, new ItemStack(Items.COPPER_PICKAXE));
		give(h, golem, planStack);
		Homes.set(golem, h.absolutePos(home));
		List<MineLayout.Step> steps = MineLayout.build(plan, level.getMinY());
		h.succeedWhen(() -> {
			h.assertValueEqual(progress(golem), steps.size(), "steps done");
			for (MineLayout.Step step : steps) {
				if (step.action() == MineLayout.Action.DIG) {
					h.assertTrue(level.getBlockState(step.pos()).isAir() || level.getBlockState(step.pos()).is(Blocks.WALL_TORCH), "not dug: " + step.pos());
				}
			}
			h.assertTrue(h.getBlockState(new BlockPos(13, 1, 7)).is(Blocks.STONE), "rock between the branches is gone");
			h.assertTrue(h.getBlockState(new BlockPos(13, 2, 8)).is(Blocks.WALL_TORCH), "no torch in the far end of a branch");
			h.assertValueEqual(chest.countItem(Items.RAIL), 16, "rails left in the chest");
			h.assertValueEqual(chest.countItem(Items.OAK_FENCE), 16, "fences left in the chest");
		});
	}

	private static boolean hasKey(Component component, String key) {
		return component.getContents() instanceof TranslatableContents contents && contents.getKey().equals(key)
			|| component.getSiblings().stream().anyMatch(sibling -> hasKey(sibling, key));
	}

	private static int progress(CopperGolem golem) {
		return MinerData.get(golem).plan().progress();
	}

	@GameTest(maxTicks = 600)
	public void miner_without_supplies_waits_at_home(GameTestHelper h) {
		// Starts 6 blocks from home, so a golem that never moves fails the "at home" check.
		Scene s = scene(h, false, new BlockPos(7, 1, 7));
		var firstSlice = MineLayout.build(s.plan(), h.getLevel().getMinY()).stream()
			.filter(step -> step.action() == MineLayout.Action.DIG)
			.limit(9)
			.toList();
		// The idle miner strolls around a bit, so check that it came to the chest at some point, not where it ends up.
		int[] closest = {Integer.MAX_VALUE};
		for (int t = 5; t < 500; t += 5) {
			h.runAfterDelay(t, () -> closest[0] = Math.min(closest[0], s.golem().blockPosition().distManhattan(h.absolutePos(HOME))));
		}
		h.runAfterDelay(500, () -> {
			h.assertTrue(closest[0] <= 3, "never came to the chest, closest distance " + closest[0]);
			for (var step : firstSlice) {
				h.assertTrue(!h.getLevel().getBlockState(step.pos()).isAir(), "dug without supplies: " + step.pos());
			}
			h.assertTrue(progress(s.golem()) == 0, "plan advanced without supplies");
			h.succeed();
		});
	}

	@GameTest(maxTicks = 1200)
	public void chest_broken_keeps_load_and_resumes(GameTestHelper h) {
		Scene s = scene(h, false, START);
		// A full pouch sends it home; the chest goes while it is on the way.
		Load.of(s.golem()).stash(new ItemStack(Items.COAL, 64));
		h.startSequence()
			.thenExecuteAfter(5, () -> h.setBlock(HOME, Blocks.AIR))
			.thenExecuteAfter(100, () -> {
				h.assertValueEqual(Load.of(s.golem()).count(stack -> stack.is(Items.COAL)), 64, "coal carried");
				h.assertValueEqual(GameTests.dropped(h, Items.COAL), 0, "coal dropped");
				h.setBlock(HOME, COPPER_CHEST);
			})
			.thenWaitUntil(() -> {
				Container chest = (Container) h.getBlockEntity(HOME, BlockEntity.class);
				h.assertValueEqual(chest.countItem(Items.COAL), 64, "coal in the new chest");
			})
			.thenSucceed();
	}

	@GameTest(maxTicks = 1800)
	public void chest_broken_stops_digging_and_resumes(GameTestHelper h) {
		Scene s = scene(h, true, START);
		// It carries all it needs, so only the missing chest can stop it.
		Load load = Load.of(s.golem());
		load.addSupplies(new ItemStack(Items.RAIL, 16));
		load.addSupplies(new ItemStack(Items.OAK_PLANKS, 32));
		load.addSupplies(new ItemStack(Items.OAK_FENCE, 16));
		h.setBlock(HOME, Blocks.AIR);
		h.startSequence()
			.thenExecuteAfter(300, () -> {
				h.assertValueEqual(progress(s.golem()), 0, "plan progress without a chest");
				h.setBlock(HOME, COPPER_CHEST);
			})
			.thenWaitUntil(() -> h.assertTrue(progress(s.golem()) > 0, "did not resume"))
			.thenSucceed();
	}

	/** Positive control for the test above: the same scene with supplies in the chest does make progress. */
	@GameTest(maxTicks = 1200)
	public void miner_with_supplies_makes_progress(GameTestHelper h) {
		Scene s = scene(h, true, new BlockPos(7, 1, 7));
		h.succeedWhen(() -> h.assertTrue(progress(s.golem()) > 0, "no progress yet"));
	}

	@GameTest(maxTicks = 3600)
	public void backed_up_miner_stops_digging_until_there_is_room(GameTestHelper h) {
		Scene s = scene(h, true, START);
		// Stacks big enough that restocking leaves no slot empty.
		for (int slot = 0; slot < 4; slot++) {
			s.chest().getItem(slot).setCount(64);
		}
		// Room for 30 of the 64 coal it carries: the visit leaves it holding 34.
		s.chest().setItem(4, new ItemStack(Items.COAL, 34));
		for (int slot = 5; slot < s.chest().getContainerSize(); slot++) {
			s.chest().setItem(slot, new ItemStack(Items.DIRT, 64));
		}
		Load.of(s.golem()).stash(new ItemStack(Items.COAL, 64));
		h.startSequence()
			.thenExecuteAfter(600, () -> {
				h.assertValueEqual(Load.of(s.golem()).count(stack -> stack.is(Items.COAL)), 34, "coal still carried");
				h.assertValueEqual(progress(s.golem()), 0, "plan progress");
				s.chest().setItem(5, ItemStack.EMPTY);
			})
			.thenWaitUntil(() -> {
				h.assertValueEqual(s.chest().countItem(Items.COAL), 98, "coal in the chest");
				h.assertTrue(progress(s.golem()) > 0, "plan did not carry on");
			})
			.thenSucceed();
	}

	@GameTest(maxTicks = 3600)
	public void miner_digs_ore_vein_and_carries_on(GameTestHelper h) {
		Scene s = scene(h, true, START);
		// A nearly full pouch, so the trip home does not wait on how fast it digs: 64 items fill it, and it returns on its own.
		Load.of(s.golem()).stash(new ItemStack(Items.COBBLESTONE, 56));
		h.setBlock(WALL, Blocks.COAL_ORE);
		h.succeedWhen(() -> {
			h.assertTrue(h.getBlockState(WALL).isAir(), "ore not dug yet");
			h.assertTrue(s.chest().countItem(Items.COAL) >= 1, "no coal in the chest yet");
			h.assertTrue(progress(s.golem()) > TWO_SLICES, "plan did not carry on: " + progress(s.golem()));
		});
	}

	/** Padded, like the one below: without it, these two made the miner and repair tests next to them fail now and then. */
	@GameTest(maxTicks = 3600, padding = 48)
	public void miner_finds_diamonds_and_hurries_home(GameTestHelper h) {
		Scene s = scene(h, true, START);
		h.setBlock(WALL, Blocks.DIAMOND_ORE);
		h.startSequence()
			.thenWaitUntil(() -> h.assertTrue(Boolean.TRUE.equals(s.golem().getAttached(ModAttachments.EXCITED)), "not excited yet"))
			.thenWaitUntil(() -> h.assertTrue(s.chest().countItem(Items.DIAMOND) >= 1, "no diamond in the chest yet"))
			.thenExecute(() -> h.assertTrue(h.getBlockState(WALL).isAir(), "diamond ore still in the wall"))
			.thenSucceed();
	}

	@GameTest(maxTicks = 3600, padding = 48)
	public void miner_digs_gold_redstone_and_emerald_ore(GameTestHelper h) {
		Scene s = scene(h, true, START);
		h.setBlock(WALL.below(), Blocks.GOLD_ORE);
		h.setBlock(WALL, Blocks.REDSTONE_ORE);
		h.setBlock(WALL.above(), Blocks.EMERALD_ORE);
		h.succeedWhen(() -> {
			h.assertTrue(h.getBlockState(WALL.below()).isAir(), "gold ore not dug yet");
			h.assertTrue(h.getBlockState(WALL).isAir(), "redstone ore not dug yet");
			h.assertTrue(h.getBlockState(WALL.above()).isAir(), "emerald ore not dug yet");
			for (var drop : List.of(Items.RAW_GOLD, Items.REDSTONE, Items.EMERALD)) {
				int found = s.chest().countItem(drop) + Load.of(s.golem()).count(stack -> stack.is(drop));
				h.assertTrue(found >= 1, "no " + drop + " carried or stored");
			}
		});
	}

	@GameTest(maxTicks = 3600)
	public void miner_seals_fluid_with_filler(GameTestHelper h) {
		Scene s = scene(h, true, START);
		h.setBlock(WALL, Blocks.WATER);
		h.succeedWhen(() -> {
			h.assertTrue(h.getBlockState(WALL).is(Blocks.COBBLESTONE) || h.getBlockState(WALL).is(Blocks.STONE), "water not plugged yet");
			h.assertTrue(progress(s.golem()) > TWO_SLICES, "plan did not carry on: " + progress(s.golem()));
		});
	}

	@GameTest(maxTicks = 3600)
	public void miner_without_filler_gives_up_the_tunnel_and_still_lays_its_rails(GameTestHelper h) {
		Scene s = scene(h, true, START);
		glassSlices(h);
		// Above the first block of the third slice, shut in by stone.
		h.setBlock(new BlockPos(4, 4, 3), Blocks.WATER);
		BlockPos gaveUpAt = h.absolutePos(new BlockPos(4, 3, 3));
		int allSteps = MineLayout.build(s.plan(), h.getLevel().getMinY()).size();
		h.succeedWhen(() -> {
			MinePlan plan = MinerData.get(s.golem()).plan();
			h.assertTrue(plan.givenUp().equals(Optional.of(gaveUpAt)), "gave up at " + plan.givenUp());
			h.assertTrue(h.getBlockState(new BlockPos(4, 1, 5)).is(Blocks.RAIL), "no rail in the first slice");
			h.assertTrue(h.getBlockState(new BlockPos(4, 1, 4)).is(Blocks.RAIL), "no rail in the second slice");
			h.assertTrue(h.getBlockState(new BlockPos(4, 3, 3)).is(Blocks.STONE), "dug on at the water");
			h.assertValueEqual(plan.progress(), allSteps, "steps done");
			h.assertTrue(hasKey(HomeLinking.status(s.golem()), "status.bettercoppers.given_up"), "not in the status line");
			ItemStack stack = new ItemStack(ModItems.MINESHAFT_PLAN);
			stack.set(ModComponents.MINE_PLAN, plan);
			List<Component> tooltip = new ArrayList<>();
			stack.getItem().appendHoverText(stack, Item.TooltipContext.EMPTY, TooltipDisplay.DEFAULT, tooltip::add, TooltipFlag.NORMAL);
			h.assertTrue(tooltip.stream().anyMatch(line -> hasKey(line, "tooltip.bettercoppers.given_up")), "not in the tooltip");
		});
	}

	@GameTest(maxTicks = 3600)
	public void miner_without_filler_skips_only_a_chased_ore_at_a_fluid(GameTestHelper h) {
		Scene s = scene(h, true, START);
		glassSlices(h);
		h.setBlock(WALL, Blocks.COAL_ORE);
		// Behind the ore, out of reach of the shaft. The stone keeps the water from running out of the scene.
		h.setBlock(WALL.east(), Blocks.WATER);
		h.setBlock(WALL.east(2), Blocks.STONE);
		h.succeedWhen(() -> {
			// Dug by the plan itself: a plan jumped to its end would never open the third slice.
			h.assertTrue(h.getBlockState(new BlockPos(4, 3, 3)).isAir(), "third slice not opened: progress " + progress(s.golem()));
			h.assertTrue(MinerData.get(s.golem()).plan().givenUp().isEmpty(), "gave up a section for an ore");
			h.assertTrue(h.getBlockState(WALL).is(Blocks.COAL_ORE), "dug the ore at the water");
		});
	}
}

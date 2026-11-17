package dev.bettercoppers.test;

import dev.bettercoppers.home.HomeLinking;
import dev.bettercoppers.home.Homes;
import dev.bettercoppers.job.Load;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import dev.bettercoppers.mining.BlockedReason;
import dev.bettercoppers.mining.MineLayout;
import dev.bettercoppers.mining.MinerData;
import dev.bettercoppers.mining.Mines;
import dev.bettercoppers.registry.ModAttachments;
import dev.bettercoppers.registry.ModGameRules;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.gamerules.GameRule;

/**
 * Describes the miner's current behaviour: finding its Mine Entrance, digging the first Mine Section, supplies, ore
 * and fluids. The usual scene is a stone block with a Mine Entrance in front of it, the mine heading north.
 */
public class MinerGameTests {
	static final Block COPPER_CHEST = Blocks.COPPER_CHEST.weathering().unaffected();
	private static final BlockPos HOME = new BlockPos(1, 1, 7);
	/** The middle beam of the scene's Mine Entrance; its opening is (4, 1..2, 6). */
	private static final BlockPos BEAM = new BlockPos(4, 3, 6);
	/** Beside the shaft (its east wall is x = 5), in the second slice. */
	private static final BlockPos WALL = new BlockPos(6, 2, 4);
	/** In the opening of the entrance. */
	private static final BlockPos START = new BlockPos(4, 1, 6);
	/** A slice is 9 digs and 3 floors, plus supports; every step counts as one progress. */
	private static final int TWO_SLICES = 24;

	private record Scene(CopperGolem golem, Container chest, BlockPos beam) {
	}

	/** Stone block, daytime, a Mine Entrance in front of it, a miner with home and pickaxe, and a home chest with supplies or empty. */
	private static Scene scene(GameTestHelper h, boolean supplies, BlockPos start) {
		ServerLevel level = h.getLevel();
		level.clockManager().setTotalTicks(level.dimensionType().defaultClock().orElseThrow(), 1000L);
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
		GameTests.hill(h);
		BlockPos beam = GameTests.mineEntrance(h, BEAM);
		Container chest = GameTests.chest(h, HOME, COPPER_CHEST);
		if (supplies) {
			chest.setItem(0, new ItemStack(Items.RAIL, 16));
			chest.setItem(1, new ItemStack(Items.OAK_PLANKS, 32));
			chest.setItem(2, new ItemStack(Items.OAK_FENCE, 16));
			chest.setItem(3, new ItemStack(Items.TORCH, 16));
		}
		return new Scene(miner(h, start, HOME), chest, beam);
	}

	/** Linked first, so the pickaxe makes it take the nearest mine right away. */
	static CopperGolem miner(GameTestHelper h, BlockPos start, BlockPos home) {
		CopperGolem golem = GameTests.golem(h, start);
		GameTests.energize(golem);
		Homes.set(golem, h.absolutePos(home));
		give(h, golem, new ItemStack(Items.COPPER_PICKAXE));
		return golem;
	}

	private static void give(GameTestHelper h, CopperGolem golem, ItemStack stack) {
		Player player = h.makeMockPlayer(GameType.SURVIVAL);
		player.setShiftKeyDown(true);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		UseEntityCallback.EVENT.invoker().interact(player, h.getLevel(), InteractionHand.MAIN_HAND, golem, null);
	}

	static void forceChunks(GameTestHelper h, BlockPos far) {
		BlockPos near = h.absolutePos(BlockPos.ZERO);
		BlockPos end = h.absolutePos(far);
		for (int cx = Math.min(near.getX(), end.getX()) >> 4; cx <= Math.max(near.getX(), end.getX()) >> 4; cx++) {
			for (int cz = Math.min(near.getZ(), end.getZ()) >> 4; cz <= Math.max(near.getZ(), end.getZ()) >> 4; cz++) {
				h.getLevel().setChunkForced(cx, cz, true);
			}
		}
	}

	private static Mines.Mine mine(GameTestHelper h, BlockPos beam) {
		return Mines.get(h.getLevel()).find(beam, Direction.Axis.X).orElseThrow();
	}

	private static Mines.Section firstSection(GameTestHelper h, BlockPos beam) {
		return mine(h, beam).sections().getFirst();
	}

	private static List<MineLayout.Step> steps(GameTestHelper h, BlockPos beam) {
		return MineLayout.build(firstSection(h, beam));
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

	/** Hill on the north side of the frame, open ground south. */
	@GameTest
	public void a_mine_runs_into_the_hill_and_starts_with_a_straight(GameTestHelper h) {
		Scene s = scene(h, false, new BlockPos(7, 1, 7));
		Mines.Mine mine = mine(h, s.beam());
		h.assertValueEqual(mine.facing(), Optional.of(Direction.NORTH), "facing");
		h.assertValueEqual(firstSection(h, s.beam()).kind(), MineLayout.STRAIGHT, "first section");
		h.assertValueEqual(firstSection(h, s.beam()).start(), new Mines.OpenEnd(h.absolutePos(new BlockPos(4, 1, 5)), Direction.NORTH), "Open End");
		h.assertValueEqual(MinerData.get(s.golem()).mine(), s.beam(), "the miner's mine");
		h.assertTrue(hasKey(HomeLinking.status(s.golem()), "status.bettercoppers.mine"), "no mine in the status line");
		h.succeed();
	}

	/** Open ground on both sides: the mine runs away from the home chest, and goes down stairs first. */
	@GameTest
	public void on_flat_ground_a_mine_runs_away_from_home_and_starts_with_stairs_down(GameTestHelper h) {
		JobGameTests.scene(h);
		GameTests.ground(h);
		BlockPos beam = GameTests.mineEntrance(h, new BlockPos(4, 3, 3));
		GameTests.chest(h, new BlockPos(4, 1, 7), COPPER_CHEST);
		CopperGolem golem = miner(h, new BlockPos(4, 1, 5), new BlockPos(4, 1, 7));
		h.assertValueEqual(mine(h, beam).facing(), Optional.of(Direction.NORTH), "facing");
		h.assertValueEqual(firstSection(h, beam).kind(), MineLayout.STAIRS_DOWN, "first section");
		h.assertValueEqual(MinerData.get(golem).mine(), beam, "the miner's mine");
		h.succeed();
	}

	/** Sets a game rule while {@code run} runs. Rules are world-wide, so a test changes one only for a single tick. */
	private static void withRule(GameTestHelper h, GameRule<Integer> rule, int value, Runnable run) {
		ServerLevel level = h.getLevel();
		int old = level.getGameRules().get(rule);
		level.getGameRules().set(rule, value, level.getServer());
		try {
			run.run();
		} finally {
			level.getGameRules().set(rule, old, level.getServer());
		}
	}

	/** The entrance is 8.7 blocks from the home chest: out of an Entrance Search Radius of 8, within the default 32. */
	@GameTest
	public void a_lowered_entrance_search_radius_misses_a_farther_entrance(GameTestHelper h) {
		JobGameTests.scene(h);
		BlockPos beam = GameTests.mineEntrance(h, new BlockPos(6, 3, 1));
		BlockPos home = new BlockPos(0, 1, 7);
		GameTests.chest(h, home, COPPER_CHEST);
		CopperGolem golem = GameTests.golem(h, new BlockPos(1, 1, 6));
		withRule(h, ModGameRules.MINE_ENTRANCE_SEARCH_RADIUS, 8, () -> {
			GameTests.energize(golem);
			Homes.set(golem, h.absolutePos(home));
			give(h, golem, new ItemStack(Items.COPPER_PICKAXE));
			h.assertFalse(beam.equals(MinerData.get(golem).mine()), "took an entrance out of the radius");
			h.assertTrue(mine(h, beam).sections().isEmpty(), "started a mine out of the radius");
		});
		h.assertValueEqual(Mines.of(h.getLevel(), golem, true).entrance(), beam, "mine with the default radius");
		h.succeed();
	}

	/**
	 * On flat ground the first section is Stairs Down, whose landing is dug 3 below the opening's floor block. Below the
	 * Depth Floor it is Blocked, and as it isn't rolled the end gets a Barrier at once.
	 */
	@GameTest
	public void a_raised_depth_floor_closes_the_entrance_with_a_barrier(GameTestHelper h) {
		JobGameTests.scene(h);
		GameTests.ground(h);
		BlockPos beam = GameTests.mineEntrance(h, new BlockPos(4, 3, 3));
		GameTests.chest(h, new BlockPos(4, 1, 7), COPPER_CHEST);
		withRule(h, ModGameRules.MINE_DEPTH_FLOOR, h.absolutePos(BlockPos.ZERO).getY() - 2, () -> {
			miner(h, new BlockPos(4, 1, 5), new BlockPos(4, 1, 7));
			Mines.Section first = firstSection(h, beam);
			h.assertValueEqual(first.kind(), MineLayout.BARRIER, "first section");
			h.assertValueEqual(first.reason(), Optional.of(BlockedReason.TOO_DEEP), "reason");
			h.assertTrue(mine(h, beam).ends().isEmpty(), "the end is still open");
		});
		Mines.get(h.getLevel()).forget(beam, Direction.Axis.X);
		Mines.get(h.getLevel()).register(beam, Direction.Axis.X);
		withRule(h, ModGameRules.MINE_DEPTH_FLOOR, h.absolutePos(BlockPos.ZERO).getY() - 3, () -> {
			CopperGolem golem = miner(h, new BlockPos(5, 1, 5), new BlockPos(4, 1, 7));
			h.assertValueEqual(firstSection(h, beam).kind(), MineLayout.STAIRS_DOWN, "first section right at the Depth Floor");
			h.assertValueEqual(MinerData.get(golem).mine(), beam, "the miner's mine");
		});
		h.succeed();
	}

	/**
	 * A whole section of {@code kind} as the mine's first, in a stone hill sealed from daylight, from an entrance at z = 11
	 * heading north. It is bigger than the test area, and entities freeze outside the chunks a test keeps loaded, so those
	 * are forced. Succeeds once the section is done and every step of it is in place, and then {@code after} holds.
	 */
	private static void digsToDone(GameTestHelper h, Identifier kind, Runnable after) {
		ServerLevel level = h.getLevel();
		BlockPos beam = hill(h, kind);
		CopperGolem golem = miner(h, new BlockPos(5, 6, 12), HILL_HOME);
		// Rails are laid only if it carries some, and a short section may never send it home to fetch them.
		Load load = Load.of(golem);
		load.addSupplies(new ItemStack(Items.RAIL, 16));
		load.addSupplies(new ItemStack(Items.OAK_PLANKS, 32));
		load.addSupplies(new ItemStack(Items.OAK_FENCE, 16));
		load.addSupplies(new ItemStack(Items.TORCH, 16));
		h.assertValueEqual(MinerData.get(golem).mine(), beam, "the miner's mine");
		h.startSequence()
			.thenWaitUntil(() -> {
				Mines.Section first = firstSection(h, beam);
				h.assertValueEqual(first.kind(), kind, "first section");
				h.assertTrue(first.done(), "section not done, progress " + first.progress());
				for (MineLayout.Step step : MineLayout.build(first)) {
					var state = level.getBlockState(step.pos());
					boolean placed = switch (step.action()) {
						case DIG -> !state.is(Blocks.STONE);
						case FLOOR -> state.isFaceSturdy(level, step.pos(), Direction.UP);
						case FENCE -> state.is(BlockTags.WOODEN_FENCES);
						case BEAM -> state.is(BlockTags.PLANKS);
						case RAIL -> state.is(BlockTags.RAILS);
						// Skipped where the opening already lights it.
						case TORCH, SIGN -> true;
					};
					h.assertTrue(placed, step.action() + " missing at " + h.relativePos(step.pos()));
				}
			})
			.thenWaitUntil(after)
			.thenSucceed();
	}

	private static final BlockPos HILL_HOME = new BlockPos(1, 6, 13);

	/**
	 * A stone hill sealed from daylight with a Mine Entrance at z = 11 heading north, its first section of {@code kind}
	 * saved but not dug, and a home chest with supplies. Returns the middle beam.
	 */
	private static BlockPos hill(GameTestHelper h, Identifier kind) {
		ServerLevel level = h.getLevel();
		level.clockManager().setTotalTicks(level.dimensionType().defaultClock().orElseThrow(), 1000L);
		forceChunks(h, new BlockPos(10, 0, 14));
		fill(h, new BlockPos(0, 0, 0), new BlockPos(10, 14, 10), Blocks.STONE);
		fill(h, new BlockPos(0, 0, 11), new BlockPos(10, 5, 14), Blocks.STONE);
		BlockPos beam = GameTests.mineEntrance(h, new BlockPos(5, 8, 11));
		Mines mines = Mines.get(level);
		mines.start(mines.find(beam, Direction.Axis.X).orElseThrow(), Direction.NORTH, kind);
		Container chest = GameTests.chest(h, HILL_HOME, COPPER_CHEST);
		chest.setItem(0, new ItemStack(Items.RAIL, 16));
		chest.setItem(1, new ItemStack(Items.OAK_PLANKS, 32));
		chest.setItem(2, new ItemStack(Items.OAK_FENCE, 16));
		chest.setItem(3, new ItemStack(Items.TORCH, 16));
		return beam;
	}

	/** A Crossing marked done leaves three free Open Ends: each miner rolls its own section at one of them. */
	@GameTest(maxTicks = 400, padding = 48)
	public void two_miners_on_one_mine_never_share_a_section(GameTestHelper h) {
		BlockPos beam = hill(h, MineLayout.CROSSING);
		Mines mines = Mines.get(h.getLevel());
		mines.setProgress(mine(h, beam), 0, steps(h, beam).size(), true);
		CopperGolem a = miner(h, new BlockPos(4, 6, 12), HILL_HOME);
		CopperGolem b = miner(h, new BlockPos(6, 6, 12), HILL_HOME);
		int first = mines.pick(h.getLevel(), mine(h, beam), a);
		int second = mines.pick(h.getLevel(), mine(h, beam), b);
		h.assertTrue(first > 0 && second > 0 && first != second, "sections picked: " + first + ", " + second);
		h.onEachTick(() -> {
			int ia = mines.claimed(mine(h, beam), a);
			int ib = mines.claimed(mine(h, beam), b);
			h.assertTrue(ia < 0 || ia != ib, "both miners hold section " + ia);
		});
		h.runAfterDelay(300, h::succeed);
	}

	/** One miner holds the only section; the others wait, and one takes it once the first lets go. */
	@GameTest(maxTicks = 200)
	public void a_third_miner_waits_when_every_tunnel_has_a_miner(GameTestHelper h) {
		Scene s = scene(h, true, START);
		Mines mines = Mines.get(h.getLevel());
		CopperGolem second = miner(h, new BlockPos(7, 1, 7), HOME);
		CopperGolem third = miner(h, new BlockPos(6, 1, 7), HOME);
		h.assertValueEqual(mines.pick(h.getLevel(), mine(h, s.beam()), s.golem()), 0, "first miner's section");
		h.assertValueEqual(mines.pick(h.getLevel(), mine(h, s.beam()), second), -1, "second miner's section");
		h.assertValueEqual(mines.pick(h.getLevel(), mine(h, s.beam()), third), -1, "third miner's section");
		h.assertTrue(hasKey(HomeLinking.status(third), "status.bettercoppers.mine_waiting"), "no waiting line in the status");
		Mines.release(s.golem());
		h.assertValueEqual(mines.pick(h.getLevel(), mine(h, s.beam()), third), 0, "section after the first miner let go");
		h.succeed();
	}

	/** A miner that stops ticking keeps its claim for 6000 ticks; then another miner takes the section. */
	@GameTest(maxTicks = 7000)
	public void a_claim_lapses_after_its_timeout(GameTestHelper h) {
		Scene s = scene(h, true, START);
		Mines mines = Mines.get(h.getLevel());
		s.golem().setNoAi(true);
		h.assertValueEqual(mines.pick(h.getLevel(), mine(h, s.beam()), s.golem()), 0, "frozen miner's section");
		CopperGolem other = miner(h, new BlockPos(7, 1, 7), HOME);
		h.startSequence()
			.thenExecuteAfter(Mines.CLAIM_TICKS - 100, () -> {
				h.assertValueEqual(mines.claimed(mine(h, s.beam()), s.golem()), 0, "frozen miner's claim before the timeout");
				h.assertValueEqual(mines.claimed(mine(h, s.beam()), other), -1, "other miner's claim before the timeout");
			})
			.thenWaitUntil(() -> h.assertValueEqual(mines.claimed(mine(h, s.beam()), other), 0, "other miner's claim"))
			.thenSucceed();
	}

	private static BlockPos hillBeam(GameTestHelper h) {
		return h.absolutePos(new BlockPos(5, 8, 11));
	}

	/** After a Straight the mine grows: the next section is rolled at its Open End. */
	@GameTest(maxTicks = 12000, padding = 48)
	public void miner_digs_a_straight_to_done_and_rolls_the_next_section(GameTestHelper h) {
		digsToDone(h, MineLayout.STRAIGHT, () -> h.assertTrue(mine(h, hillBeam(h)).sections().size() >= 2, "no next section rolled"));
	}

	@GameTest(maxTicks = 12000, padding = 48)
	public void miner_digs_a_corner_to_done(GameTestHelper h) {
		digsToDone(h, MineLayout.CORNER, () -> {});
	}

	@GameTest(maxTicks = 12000, padding = 48)
	public void miner_digs_a_crossing_to_done(GameTestHelper h) {
		digsToDone(h, MineLayout.CROSSING, () -> {});
	}

	@GameTest(maxTicks = 15000, padding = 48)
	public void miner_digs_a_small_hall_to_done(GameTestHelper h) {
		digsToDone(h, MineLayout.SMALL_HALL, () -> {});
	}

	@GameTest(maxTicks = 12000, padding = 48)
	public void miner_digs_stairs_down_to_done(GameTestHelper h) {
		digsToDone(h, MineLayout.STAIRS_DOWN, () -> {});
	}

	@GameTest(maxTicks = 12000, padding = 48)
	public void miner_digs_stairs_up_to_done(GameTestHelper h) {
		digsToDone(h, MineLayout.STAIRS_UP, () -> {});
	}

	/** A Dead End leaves no Open End: with it done, the mine is finished. */
	@GameTest(maxTicks = 12000, padding = 48)
	public void miner_digs_a_dead_end_to_done_and_finishes_the_mine(GameTestHelper h) {
		digsToDone(h, MineLayout.DEAD_END, () -> h.assertTrue(mine(h, hillBeam(h)).isFinished(), "mine not finished"));
	}

	/** A broken frame closes the mine: the miner forgets it and goes home. It may then take a neighbouring test's mine. */
	@GameTest(maxTicks = 1200)
	public void breaking_the_frame_closes_the_mine(GameTestHelper h) {
		Scene s = scene(h, true, START);
		h.setBlock(BEAM.east(), Blocks.AIR);
		h.succeedWhen(() -> {
			h.assertTrue(!s.beam().equals(MinerData.get(s.golem()).mine()), "still holds the broken mine");
			h.assertTrue(h.getBlockState(new BlockPos(4, 3, 5)).is(Blocks.STONE), "dug in a closed mine");
		});
	}
	static boolean hasKey(Component component, String key) {
		return component.getContents() instanceof TranslatableContents contents && contents.getKey().equals(key)
			|| component.getSiblings().stream().anyMatch(sibling -> hasKey(sibling, key));
	}

	private static int progress(GameTestHelper h, Scene s) {
		return firstSection(h, s.beam()).progress();
	}

	@GameTest(maxTicks = 600)
	public void miner_without_supplies_waits_at_home(GameTestHelper h) {
		// Starts 6 blocks from home, so a golem that never moves fails the "at home" check.
		Scene s = scene(h, false, new BlockPos(7, 1, 7));
		// The first support is in the third slice (z = 3): its fences have to come from somewhere.
		List<BlockPos> supportSlice = BlockPos.betweenClosedStream(new BlockPos(3, 1, 3), new BlockPos(5, 3, 3)).map(BlockPos::immutable).toList();
		// The idle miner strolls around a bit, so check that it came to the chest at some point, not where it ends up.
		int[] closest = {Integer.MAX_VALUE};
		for (int t = 5; t < 500; t += 5) {
			h.runAfterDelay(t, () -> closest[0] = Math.min(closest[0], s.golem().blockPosition().distManhattan(h.absolutePos(HOME))));
		}
		h.runAfterDelay(500, () -> {
			h.assertTrue(closest[0] <= 3, "never came to the chest, closest distance " + closest[0]);
			for (BlockPos pos : supportSlice) {
				h.assertTrue(!h.getBlockState(pos).isAir(), "dug a support slice without supplies: " + pos);
			}
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
				h.assertValueEqual(progress(h, s), 0, "plan progress without a chest");
				h.setBlock(HOME, COPPER_CHEST);
			})
			.thenWaitUntil(() -> h.assertTrue(progress(h, s) > 0, "did not resume"))
			.thenSucceed();
	}

	/** Positive control for the test above: the same scene with supplies in the chest does make progress. */
	@GameTest(maxTicks = 1200)
	public void miner_with_supplies_makes_progress(GameTestHelper h) {
		Scene s = scene(h, true, new BlockPos(7, 1, 7));
		h.succeedWhen(() -> h.assertTrue(progress(h, s) > 0, "no progress yet"));
	}

	static void fill(GameTestHelper h, BlockPos from, BlockPos to, Block block) {
		BlockPos.betweenClosed(from, to).forEach(pos -> h.setBlock(pos, block));
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
				h.assertValueEqual(progress(h, s), 0, "plan progress");
				s.chest().setItem(5, ItemStack.EMPTY);
			})
			.thenWaitUntil(() -> {
				h.assertValueEqual(s.chest().countItem(Items.COAL), 98, "coal in the chest");
				h.assertTrue(progress(h, s) > 0, "plan did not carry on");
			})
			.thenSucceed();
	}

	@GameTest(maxTicks = 3600)
	public void miner_digs_ore_vein_and_carries_on(GameTestHelper h) {
		Scene s = scene(h, true, START);
		h.setBlock(WALL, Blocks.COAL_ORE);
		h.startSequence()
			.thenWaitUntil(() -> h.assertTrue(h.getBlockState(WALL).isAir(), "ore not dug yet"))
			// A full pouch, so the trip home does not wait on how fast it digs.
			.thenExecute(() -> Load.of(s.golem()).stash(new ItemStack(Items.COBBLESTONE, 64)))
			.thenWaitUntil(() -> {
				h.assertTrue(s.chest().countItem(Items.COAL) >= 1, "no coal in the chest yet");
				h.assertTrue(progress(h, s) > TWO_SLICES, "plan did not carry on: " + progress(h, s));
			})
			.thenSucceed();
	}

	/** A flat coal slab of 72 ores beside the shaft, shut in by stone and all within reach: the miner digs 64 of it. */
	@GameTest(maxTicks = 11000, padding = 48)
	public void a_vein_bigger_than_the_cap_stops_at_64_ores(GameTestHelper h) {
		Scene s = scene(h, true, START);
		// The vein reaches past the test's own chunks, which would freeze the golem there.
		forceChunks(h, new BlockPos(14, 0, 7));
		forceChunks(h, new BlockPos(14, 0, -4));
		fill(h, new BlockPos(6, 0, -4), new BlockPos(14, 2, 6), Blocks.STONE);
		BlockPos from = new BlockPos(6, 1, -3);
		BlockPos to = new BlockPos(13, 1, 5);
		fill(h, from, to, Blocks.COAL_ORE);
		h.succeedWhen(() -> {
			h.assertTrue(firstSection(h, s.beam()).done(), "section not done: progress " + progress(h, s));
			int left = (int) BlockPos.betweenClosedStream(from, to).filter(pos -> h.getBlockState(pos).is(Blocks.COAL_ORE)).count();
			h.assertValueEqual(left, 72 - 64, "coal ore left");
		});
	}

	/** A line of coal running east from the shaft's wall (x = 5), shut in by stone: ore up to 8 blocks out is dug, the rest stays. */
	@GameTest(maxTicks = 11000, padding = 48)
	public void ore_beyond_the_vein_reach_stays(GameTestHelper h) {
		Scene s = scene(h, true, START);
		forceChunks(h, new BlockPos(17, 0, 7));
		fill(h, new BlockPos(6, 0, 2), new BlockPos(17, 2, 4), Blocks.STONE);
		fill(h, new BlockPos(6, 1, 3), new BlockPos(16, 1, 3), Blocks.COAL_ORE);
		h.succeedWhen(() -> {
			h.assertTrue(firstSection(h, s.beam()).done(), "section not done: progress " + progress(h, s));
			h.assertTrue(h.getBlockState(new BlockPos(13, 1, 3)).isAir(), "ore 8 blocks out not dug");
			for (int x = 14; x <= 16; x++) {
				h.assertTrue(h.getBlockState(new BlockPos(x, 1, 3)).is(Blocks.COAL_ORE), "dug ore beyond the reach at x " + x);
			}
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
			h.assertTrue(progress(h, s) > TWO_SLICES, "plan did not carry on: " + progress(h, s));
		});
	}

	@GameTest(maxTicks = 3600)
	public void miner_without_filler_cuts_the_section_short_and_still_lays_its_rails(GameTestHelper h) {
		Scene s = scene(h, true, START);
		glassSlices(h);
		// Above the first block of the third slice, shut in by stone.
		h.setBlock(new BlockPos(4, 4, 3), Blocks.WATER);
		h.succeedWhen(() -> {
			h.assertTrue(firstSection(h, s.beam()).done(), "section not done");
			h.assertTrue(h.getBlockState(new BlockPos(4, 1, 5)).is(Blocks.RAIL), "no rail in the first slice");
			// The Barrier goes in the second slice, the last one dug whole, and takes up its rail.
			h.assertTrue(h.getBlockState(new BlockPos(4, 1, 4)).is(BlockTags.WOODEN_FENCES), "no Barrier in the second slice");
			h.assertValueEqual(mine(h, s.beam()).sections().get(1).reason(), Optional.of(BlockedReason.WATER), "Barrier reason");
			h.assertTrue(h.getBlockState(new BlockPos(4, 3, 3)).is(Blocks.STONE), "dug on at the water");
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
			// A section cut short would never open the third slice.
			h.assertTrue(h.getBlockState(new BlockPos(4, 3, 3)).isAir(), "third slice not opened: progress " + progress(h, s));
			h.assertTrue(h.getBlockState(WALL).is(Blocks.COAL_ORE), "dug the ore at the water");
		});
	}

	/** Lava that turns up after the check (here: put next to the second slice once digging started) cuts the section short. */
	@GameTest(maxTicks = 3600)
	public void lava_met_mid_dig_cuts_the_section_short(GameTestHelper h) {
		Scene s = scene(h, true, START);
		h.startSequence()
			.thenWaitUntil(() -> h.assertTrue(progress(h, s) > 0, "not digging yet"))
			.thenExecute(() -> h.setBlock(WALL, Blocks.LAVA))
			.thenWaitUntil(() -> {
				Mines.Mine mine = mine(h, s.beam());
				h.assertTrue(mine.sections().getFirst().done(), "section not done");
				h.assertValueEqual(mine.sections().size(), 2, "sections");
				h.assertValueEqual(mine.sections().get(1).reason(), Optional.of(BlockedReason.LAVA), "Barrier reason");
				h.assertTrue(mine.ends().isEmpty(), "the cut section kept its Open End");
				// Digging the east column of the second slice met the lava, so the first slice is the last one dug whole.
				h.assertTrue(h.getBlockState(new BlockPos(4, 1, 5)).is(BlockTags.WOODEN_FENCES), "no Barrier in the first slice");
				h.assertTrue(h.getBlockState(new BlockPos(4, 2, 3)).is(Blocks.STONE), "dug on past the lava");
			})
			.thenSucceed();
	}

	/**
	 * A done Straight in the hill, and obsidian in the middle of the slice ahead of its Open End: every kind rolled there
	 * digs that cell, and a copper pickaxe can't. So the end gets a Barrier, with a sign saying why.
	 */
	@GameTest(maxTicks = 3600, padding = 48)
	public void a_blocked_roll_gets_a_barrier_with_its_sign(GameTestHelper h) {
		ServerLevel level = h.getLevel();
		level.clockManager().setTotalTicks(level.dimensionType().defaultClock().orElseThrow(), 1000L);
		forceChunks(h, new BlockPos(10, 0, 14));
		// Past the test area too, so whatever is rolled ahead stays in the hill.
		fill(h, new BlockPos(0, 0, -10), new BlockPos(10, 14, 10), Blocks.STONE);
		fill(h, new BlockPos(0, 0, 11), new BlockPos(10, 5, 14), Blocks.STONE);
		fill(h, new BlockPos(4, 6, 3), new BlockPos(6, 8, 10), Blocks.AIR);
		h.setBlock(new BlockPos(5, 7, 2), Blocks.OBSIDIAN);
		BlockPos beam = GameTests.mineEntrance(h, new BlockPos(5, 8, 11));
		Mines mines = Mines.get(level);
		Mines.Mine started = mines.start(mines.find(beam, Direction.Axis.X).orElseThrow(), Direction.NORTH, MineLayout.STRAIGHT);
		mines.setProgress(started, 0, MineLayout.build(started.sections().getFirst()).size(), true);
		BlockPos home = new BlockPos(1, 6, 13);
		GameTests.chest(h, home, COPPER_CHEST);
		CopperGolem golem = miner(h, new BlockPos(5, 6, 12), home);
		Load.of(golem).addSupplies(new ItemStack(Items.OAK_FENCE, 4));
		Load.of(golem).addSupplies(new ItemStack(Items.OAK_SIGN));
		h.startSequence()
			.thenWaitUntil(() -> {
				Mines.Mine mine = mine(h, beam);
				h.assertValueEqual(mine.sections().size(), 2, "sections");
				h.assertValueEqual(mine.sections().get(1).kind(), MineLayout.BARRIER, "second section");
				h.assertValueEqual(mine.sections().get(1).reason(), Optional.of(BlockedReason.HARD_ROCK), "reason");
				h.assertTrue(mine.sections().get(1).done(), "Barrier not built");
				for (int x = 4; x <= 6; x++) {
					h.assertTrue(h.getBlockState(new BlockPos(x, 6, 3)).is(BlockTags.WOODEN_FENCES), "no post at x " + x);
				}
				h.assertTrue(h.getBlockEntity(new BlockPos(5, 7, 3), BlockEntity.class) instanceof SignBlockEntity sign
					&& hasKey(sign.getText(SignTextSlot.FRONT).getMessages(false).get(2), "mine.bettercoppers.blocked.hard_rock"), "no sign saying Hard rock");
				h.assertTrue(mine.isFinished(), "a closed end is still open");
			})
			.thenSucceed();
	}

	/** A player's fence in the slice behind an Open End closes it; breaking that fence opens it again. */
	@GameTest
	public void a_player_closes_an_open_end_with_a_fence_and_breaking_it_opens_the_end(GameTestHelper h) {
		Scene s = scene(h, false, new BlockPos(7, 1, 7));
		Mines.OpenEnd end = mine(h, s.beam()).ends().getFirst();
		BlockPos fence = MineLayout.middleFence(end);
		ServerPlayer player = h.makeMockServerPlayerInLevel();
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_FENCE));
		h.getLevel().setBlock(fence.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
		h.getLevel().setBlock(fence, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(fence.below()).add(0, 0.5, 0), Direction.UP, fence.below(), false);
		player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
		h.assertTrue(h.getLevel().getBlockState(fence).is(BlockTags.WOODEN_FENCES), "fence not placed");
		h.assertTrue(mine(h, s.beam()).ends().isEmpty(), "the end is still open");
		h.assertValueEqual(mine(h, s.beam()).sections().getLast().reason(), Optional.of(BlockedReason.PLAYER), "Barrier reason");
		player.gameMode.destroyBlock(fence);
		h.assertTrue(h.getLevel().getBlockState(fence).isAir(), "fence not broken");
		h.assertValueEqual(mine(h, s.beam()).ends(), List.of(end), "ends after breaking the fence");
		h.assertValueEqual(mine(h, s.beam()).sections().size(), 1, "sections after breaking the fence");
		h.succeed();
	}
}

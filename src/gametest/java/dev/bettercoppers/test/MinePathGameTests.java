package dev.bettercoppers.test;

import dev.bettercoppers.home.HomeLinking;
import dev.bettercoppers.job.Load;
import dev.bettercoppers.mining.MineBehavior;
import dev.bettercoppers.mining.MineLayout;
import dev.bettercoppers.mining.Mines;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.block.Blocks;

/**
 * Getting to and from a deep mine: the Mine Path, being stuck in the mine, and the chunk ticket. The scene is a stone
 * ridge running south with a Mine Entrance at z = 2, three Straights dug and done, and a fourth one saved but not dug.
 */
public class MinePathGameTests {
	private static final BlockPos HOME = new BlockPos(1, 2, 0);
	private static final BlockPos BEAM = new BlockPos(5, 4, 2);
	private static final int DONE = 3;

	private record Scene(BlockPos beam, Container chest) {
	}

	private static Scene deepMine(GameTestHelper h) {
		ServerLevel level = h.getLevel();
		level.clockManager().setTotalTicks(level.dimensionType().defaultClock().orElseThrow(), 1000L);
		MinerGameTests.forceChunks(h, new BlockPos(10, 0, 44));
		MinerGameTests.fill(h, new BlockPos(0, 0, 0), new BlockPos(10, 1, 44), Blocks.STONE);
		MinerGameTests.fill(h, new BlockPos(0, 2, 3), new BlockPos(10, 6, 44), Blocks.STONE);
		BlockPos beam = GameTests.mineEntrance(h, BEAM);
		Mines mines = Mines.get(level);
		Mines.Mine mine = mines.start(mines.find(beam, Direction.Axis.X).orElseThrow(), Direction.SOUTH, MineLayout.STRAIGHT);
		for (int i = 0; i < DONE; i++) {
			mine = mines.grow(mine, mine.ends().getFirst(), MineLayout.STRAIGHT, false);
		}
		for (int i = 0; i < DONE; i++) {
			List<MineLayout.Step> steps = MineLayout.build(mine.sections().get(i));
			steps.stream().filter(step -> step.action() == MineLayout.Action.DIG).forEach(step -> level.setBlockAndUpdate(step.pos(), Blocks.AIR.defaultBlockState()));
			mines.setProgress(mine, i, steps.size(), true);
		}
		return new Scene(beam, GameTests.chest(h, HOME, MinerGameTests.COPPER_CHEST));
	}

	private static CopperGolem miner(GameTestHelper h, BlockPos start) {
		CopperGolem golem = MinerGameTests.miner(h, start, HOME);
		Load load = Load.of(golem);
		load.addSupplies(new ItemStack(Items.OAK_PLANKS, 32));
		load.addSupplies(new ItemStack(Items.OAK_FENCE, 16));
		load.addSupplies(new ItemStack(Items.TORCH, 16));
		return golem;
	}

	private static Mines.Section deepest(GameTestHelper h, BlockPos beam) {
		return Mines.get(h.getLevel()).find(beam, Direction.Axis.X).orElseThrow().sections().get(DONE);
	}

	/** From home along the three finished Straights to the fourth, digs there, and once its pouch is full brings it home. */
	@GameTest(maxTicks = 6000, padding = 48)
	public void a_miner_walks_to_a_section_three_deep_and_back(GameTestHelper h) {
		Scene s = deepMine(h);
		CopperGolem golem = miner(h, new BlockPos(2, 2, 1));
		Load.of(golem).stash(new ItemStack(Items.FLINT, 60));
		h.startSequence()
			.thenWaitUntil(() -> h.assertTrue(deepest(h, s.beam()).progress() > 0, "nothing dug in the fourth section"))
			.thenWaitUntil(() -> h.assertValueEqual(s.chest().countItem(Items.FLINT), 60, "flint brought home"))
			.thenSucceed();
	}

	/**
	 * The same walk with rails down the middle of the finished Straights, as a miner with rails lays them. Vanilla keeps a
	 * mob that stands off a rail from pathing onto one, which would leave the miner stuck at the entrance.
	 */
	@GameTest(maxTicks = 4000, padding = 48)
	public void a_miner_walks_into_a_mine_with_rails(GameTestHelper h) {
		Scene s = deepMine(h);
		for (int z = 3; z < 3 + 8 * DONE; z++) {
			h.setBlock(new BlockPos(BEAM.getX(), BEAM.getY() - 2, z), Blocks.RAIL);
		}
		miner(h, new BlockPos(2, 2, 1));
		h.succeedWhen(() -> h.assertTrue(deepest(h, s.beam()).progress() > 0, "nothing dug in the fourth section"));
	}

	/**
	 * Coal two deep under the walkway of the fourth section. The miner digs what it reaches from the walkway, never steps
	 * down into the hole (it could not climb out, and patching the walkway would wall it in), and finishes the section.
	 */
	@GameTest(maxTicks = 6000, padding = 48)
	public void a_miner_never_steps_down_into_a_vein_under_the_walkway(GameTestHelper h) {
		Scene s = deepMine(h);
		BlockPos top = new BlockPos(BEAM.getX(), 1, 3 + 8 * DONE + 3);
		h.setBlock(top, Blocks.COAL_ORE);
		h.setBlock(top.below(), Blocks.COAL_ORE);
		CopperGolem golem = miner(h, new BlockPos(2, 2, 1));
		int walkway = h.absolutePos(BlockPos.ZERO).getY() + 2;
		h.onEachTick(() -> {
			h.assertTrue(golem.getBlockY() >= walkway || !deepest(h, s.beam()).box().isInside(golem.blockPosition()), "stepped down into the vein at " + golem.blockPosition());
			h.assertFalse(h.getLevel().getBlockState(golem.blockPosition()).isCollisionShapeFullBlock(h.getLevel(), golem.blockPosition()), "walled in at " + golem.blockPosition());
		});
		h.succeedWhen(() -> {
			h.assertTrue(deepest(h, s.beam()).done(), "fourth section not done: progress " + deepest(h, s.beam()).progress());
			h.assertFalse(h.getBlockState(top).is(Blocks.COAL_ORE), "ore under the walkway not dug");
			h.assertTrue(h.getBlockState(top).isSolid(), "walkway over the vein not patched");
		});
	}

	/**
	 * A golem stands where the fourth section's first support post goes. The miner never puts the post into it, and once
	 * the golem is gone it finishes the section.
	 */
	@GameTest(maxTicks = 6000, padding = 48)
	public void a_miner_never_builds_into_a_golem(GameTestHelper h) {
		Scene s = deepMine(h);
		BlockPos post = new BlockPos(BEAM.getX() - 1, 2, 3 + 8 * DONE + 2);
		h.setBlock(post, Blocks.AIR);
		h.setBlock(post.above(), Blocks.AIR);
		CopperGolem bystander = GameTests.golem(h, post);
		bystander.setNoAi(true);
		miner(h, new BlockPos(2, 2, 1));
		h.onEachTick(() -> {
			if (bystander.isAlive()) {
				h.assertFalse(h.getLevel().getBlockState(bystander.blockPosition()).isSolid(), "built into the golem at " + bystander.blockPosition());
			}
		});
		h.startSequence()
			.thenWaitUntil(() -> h.assertTrue(deepest(h, s.beam()).progress() > 0, "nothing dug in the fourth section"))
			.thenExecuteAfter(1200, bystander::discard)
			.thenWaitUntil(() -> h.assertTrue(deepest(h, s.beam()).done(), "fourth section not done"))
			.thenSucceed();
	}

	/** Out of energy in the fourth section of a mine with rails, the miner walks out along its tunnels to the pad by its home. */
	@GameTest(maxTicks = 4000, padding = 48)
	public void an_empty_miner_walks_out_of_the_mine_to_its_pad(GameTestHelper h) {
		deepMine(h);
		for (int z = 3; z < 3 + 8 * DONE; z++) {
			h.setBlock(new BlockPos(BEAM.getX(), BEAM.getY() - 2, z), Blocks.RAIL);
		}
		BlockPos pad = new BlockPos(3, 1, 0);
		h.setBlock(pad, dev.bettercoppers.registry.ModBlocks.CHARGING_PAD);
		// Beside the rails, as a miner digging the tunnel's side stands.
		CopperGolem golem = miner(h, new BlockPos(BEAM.getX() - 1, 2, 3 + 8 * DONE - 2));
		dev.bettercoppers.runningcosts.RunningCosts.get(golem).setEnergy(0);
		h.succeedWhen(() -> h.assertValueEqual(golem.blockPosition(), h.absolutePos(pad.above()), "where the empty miner stands"));
	}

	/** Walled in at the far end, it tries the Mine Path, then straight for home, then says it is stuck. */
	@GameTest(maxTicks = 2400, padding = 48)
	public void a_walled_in_miner_is_stuck_in_the_mine(GameTestHelper h) {
		Scene s = deepMine(h);
		MinerGameTests.fill(h, new BlockPos(4, 2, 20), new BlockPos(6, 4, 20), Blocks.BEDROCK);
		CopperGolem golem = miner(h, new BlockPos(5, 2, 26));
		Load.of(golem).stash(new ItemStack(Items.FLINT, 64));
		h.succeedWhen(() -> h.assertTrue(MinerGameTests.hasKey(HomeLinking.status(golem), "status.bettercoppers.mine_stuck"), "not stuck in the mine"));
	}

	/** Working with no player in the world, the miner never loads chunks; a player near the entrance would let it. */
	@GameTest(maxTicks = 300, padding = 48)
	public void a_miner_holds_no_chunk_ticket_with_no_player_near(GameTestHelper h) {
		Scene s = deepMine(h);
		CopperGolem golem = miner(h, new BlockPos(2, 2, 1));
		ServerLevel level = h.getLevel();
		int range = 64;
		Player player = h.makeMockPlayer(GameType.SURVIVAL);
		player.setPos(golem.position());
		h.assertTrue(MineBehavior.isWatched(List.of(player), s.beam(), range), "a player beside the miner does not count");
		player.setPos(s.beam().getX() + range + 33, s.beam().getY(), s.beam().getZ());
		h.assertFalse(MineBehavior.isWatched(List.of(player), s.beam(), range), "a player 97 blocks away counts");
		TicketStorage tickets = level.getDataStorage().computeIfAbsent(TicketStorage.TYPE);
		// Other tests may put players into the world; their tickets last 40 ticks after they leave.
		long[] watchedAt = {-100};
		int[] checked = {0};
		h.onEachTick(() -> {
			if (MineBehavior.isWatched(level.players(), s.beam(), range)) {
				watchedAt[0] = h.getTick();
			} else if (h.getTick() - watchedAt[0] > 50) {
				checked[0]++;
				h.assertFalse(tickets.getTickets(ChunkPos.pack(golem.blockPosition())).stream().anyMatch(ticket -> ticket.getType() == MineBehavior.TICKET),
					"a miner ticket with no player near");
			}
		});
		h.runAfterDelay(250, () -> {
			h.assertTrue(checked[0] > 100, "a player was near the mine nearly all the time");
			h.succeed();
		});
	}
}

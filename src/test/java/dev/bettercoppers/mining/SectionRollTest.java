package dev.bettercoppers.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bettercoppers.api.BetterCoppersApi;
import dev.bettercoppers.api.MineSectionTemplate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SectionRollTest {
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	private static Map<String, Integer> weights(List<SectionRoll.Candidate> candidates) {
		return candidates.stream().collect(Collectors.toMap(c -> c.kind().getPath() + (c.mirrored() ? "_left" : ""), SectionRoll.Candidate::weight));
	}

	private static List<Identifier> kinds(List<SectionRoll.Candidate> candidates) {
		return candidates.stream().map(SectionRoll.Candidate::kind).toList();
	}

	@Test
	void builtInWeightsSumToOneHundred() {
		List<SectionRoll.Candidate> candidates = SectionRoll.candidates(MineLayout.STRAIGHT, false, false);
		assertEquals(Map.of("straight", 40, "corner", 8, "corner_left", 8, "stairs_down", 12, "stairs_up", 6, "crossing", 10, "small_hall", 5, "dead_end", 11), weights(candidates));
		assertEquals(100, candidates.stream().mapToInt(SectionRoll.Candidate::weight).sum());
	}

	@Test
	void aboveYZeroTheMineLeansDown() {
		Map<String, Integer> weights = weights(SectionRoll.candidates(MineLayout.STRAIGHT, false, true));
		assertEquals(24, weights.get("stairs_down"));
		assertEquals(3, weights.get("stairs_up"));
		assertEquals(40, weights.get("straight"));
	}

	@Test
	void followRulesLeaveOutKindsThatUndoTheParent() {
		assertFalse(kinds(SectionRoll.candidates(MineLayout.STAIRS_DOWN, false, false)).contains(MineLayout.STAIRS_UP));
		assertFalse(kinds(SectionRoll.candidates(MineLayout.STAIRS_UP, false, false)).contains(MineLayout.STAIRS_DOWN));
		assertFalse(kinds(SectionRoll.candidates(MineLayout.CORNER, false, false)).contains(MineLayout.CORNER));
		assertTrue(kinds(SectionRoll.candidates(MineLayout.STRAIGHT, false, false)).contains(MineLayout.DEAD_END));
		assertFalse(kinds(SectionRoll.candidates(MineLayout.STRAIGHT, true, false)).contains(MineLayout.DEAD_END), "the last Open End never gets a Dead End");
		assertTrue(kinds(SectionRoll.candidates(MineLayout.STRAIGHT, true, false)).contains(MineLayout.CROSSING));
	}

	private static Identifier testId(String path) {
		return Identifier.fromNamespaceAndPath("bettercoppers_test", path);
	}

	@Test
	void anAddOnKindIsACandidateAfterTheParentsItMayFollow() {
		Identifier lead = testId("roll_lead");
		Identifier room = testId("roll_room");
		BetterCoppersApi.registerMineSection(room, 7, MineSectionTemplate.builder().columns(0, 2, -1, 1).openEnd(3, 0, MineSectionTemplate.Turn.AHEAD).mirrorable().build(),
			parent -> parent.equals(lead));
		Map<String, Integer> weights = weights(SectionRoll.candidates(lead, false, true));
		assertEquals(7, weights.get("roll_room"), "no depth bias");
		assertEquals(7, weights.get("roll_room_left"), "the mirror image is a candidate of its own");
		assertEquals(40, weights.get("straight"));
		assertFalse(kinds(SectionRoll.candidates(MineLayout.STRAIGHT, false, false)).contains(room));
		assertFalse(kinds(SectionRoll.candidates(null, false, false)).contains(room), "not rolled at the entrance");
	}

	@Test
	void anAddOnKindWithoutOpenEndsNeverEndsTheMine() {
		Identifier lead = testId("closed_lead");
		Identifier closet = testId("closet");
		BetterCoppersApi.registerMineSection(closet, 3, MineSectionTemplate.builder().columns(0, 1, -1, 1).build(), parent -> parent.equals(lead));
		assertTrue(kinds(SectionRoll.candidates(lead, false, false)).contains(closet));
		assertFalse(kinds(SectionRoll.candidates(lead, true, false)).contains(closet));
	}

	@Test
	void aSeededRollRepeatsAndPicksEachCandidateOnce() {
		List<SectionRoll.Candidate> candidates = SectionRoll.candidates(MineLayout.STRAIGHT, false, false);
		long seed = SectionRoll.seed(1234L, new BlockPos(5, 70, 5), new Mines.OpenEnd(new BlockPos(5, 68, -3), Direction.NORTH));
		List<SectionRoll.Candidate> first = SectionRoll.order(candidates, RandomSource.create(seed));
		assertEquals(first, SectionRoll.order(candidates, RandomSource.create(seed)));
		assertEquals(new HashSet<>(candidates), new HashSet<>(first));
		assertEquals(candidates.size(), first.size());
	}

	@Test
	void theSeedDependsOnWorldEntranceAndOpenEnd() {
		BlockPos beam = new BlockPos(5, 70, 5);
		Mines.OpenEnd end = new Mines.OpenEnd(new BlockPos(5, 68, -3), Direction.NORTH);
		long seed = SectionRoll.seed(1L, beam, end);
		assertEquals(seed, SectionRoll.seed(1L, beam, end));
		assertNotEquals(seed, SectionRoll.seed(2L, beam, end));
		assertNotEquals(seed, SectionRoll.seed(1L, beam.east(), end));
		assertNotEquals(seed, SectionRoll.seed(1L, beam, new Mines.OpenEnd(end.pos(), Direction.EAST)));
	}

	@Test
	void heavierKindsComeFirstMoreOften() {
		List<SectionRoll.Candidate> candidates = SectionRoll.candidates(MineLayout.STRAIGHT, false, false);
		int straightFirst = 0;
		for (long seed = 0; seed < 2000; seed++) {
			if (SectionRoll.order(candidates, RandomSource.create(seed)).getFirst().kind().equals(MineLayout.STRAIGHT)) {
				straightFirst++;
			}
		}
		// 40 in 100: about 800 of 2000.
		assertTrue(straightFirst > 700 && straightFirst < 900, "straight first " + straightFirst + " times");
	}
}

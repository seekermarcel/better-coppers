package dev.bettercoppers.api;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.bettercoppers.api.MineSectionTemplate.Turn;
import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class MineSectionTemplateTest {
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	/** Three slices, 3 wide, open ahead. */
	private static MineSectionTemplate.Builder room() {
		return MineSectionTemplate.builder().columns(0, 2, -1, 1).openEnd(3, 0, Turn.AHEAD);
	}

	private static void rejects(Executable build) {
		assertThrows(IllegalArgumentException.class, build);
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath("bettercoppers_test", path);
	}

	@Test
	void aValidTemplateBuilds() {
		assertDoesNotThrow(() -> room().support(1, -1, 1).torch(2, 0).rail(0, 0).rail(1, 0).mirrorable().build());
	}

	@Test
	void sliceZeroMustHoldTheThreeWideOpening() {
		rejects(() -> MineSectionTemplate.builder().columns(0, 2, 0, 1).columns(1, 2, -1, -1).openEnd(3, 0, Turn.AHEAD).build());
	}

	@Test
	void columnsMustBeFaceConnected() {
		rejects(() -> room().column(1, 3).build());
	}

	@Test
	void columnsMustNotReachIntoTheParent() {
		rejects(() -> room().column(-1, 0).build());
	}

	@Test
	void openEndsSitOnTheEdgeFacingOut() {
		rejects(() -> room().openEnd(1, 0, Turn.AHEAD).build());
		rejects(() -> room().openEnd(4, 0, Turn.AHEAD).build());
		rejects(() -> room().openEnd(1, 2, Turn.LEFT).build());
		assertDoesNotThrow(() -> room().openEnd(1, -2, Turn.LEFT).openEnd(1, 2, Turn.RIGHT).build());
	}

	@Test
	void supportsSpanDugColumns() {
		rejects(() -> room().support(1, -2, 1).build());
		rejects(() -> room().support(1, 0, 0).build());
	}

	@Test
	void pillarsStandOnDugColumns() {
		rejects(() -> room().pillar(1, 2).build());
	}

	@Test
	void torchesLieOnDugColumns() {
		rejects(() -> room().torch(3, 0).build());
	}

	@Test
	void aTorchNeedsSomethingToHangOn() {
		rejects(() -> MineSectionTemplate.builder().columns(0, 4, -2, 2).torch(2, 0).build());
	}

	@Test
	void railsLieOnDugColumns() {
		rejects(() -> room().rail(1, 2).build());
	}

	@Test
	void everyBlockMustBeReachable() {
		// The far arm is only reached from slice 2, which the golem digs after slice 0.
		rejects(() -> MineSectionTemplate.builder().columns(0, 2, -1, 1).columns(0, 2, 3, 3).column(2, 2).build());
	}

	@Test
	void registrationRejectsTheModsNamespace() {
		rejects(() -> BetterCoppersApi.registerMineSection(Identifier.fromNamespaceAndPath("bettercoppers", "room"), 5, room().build()));
	}

	@Test
	void registrationRejectsADuplicate() {
		BetterCoppersApi.registerMineSection(id("duplicate"), 5, room().build(), parent -> false);
		rejects(() -> BetterCoppersApi.registerMineSection(id("duplicate"), 5, room().build()));
	}

	@Test
	void registrationRejectsAWeightBelowOne() {
		rejects(() -> BetterCoppersApi.registerMineSection(id("light"), 0, room().build()));
	}
}

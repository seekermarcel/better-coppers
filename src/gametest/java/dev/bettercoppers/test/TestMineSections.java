package dev.bettercoppers.test;

import dev.bettercoppers.api.BetterCoppersApi;
import dev.bettercoppers.api.MineSectionTemplate;
import dev.bettercoppers.api.MineSectionTemplate.Turn;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

/**
 * Mine Sections an add-on would register. They roll only after {@link #LEAD}, which is never rolled itself, so the
 * other tests' mines roll as without them.
 */
public class TestMineSections implements ModInitializer {
	/** Two slices, open ahead: a gametest starts a mine with it. */
	public static final Identifier LEAD = Identifier.fromNamespaceAndPath("bettercoppers_test", "lead");
	/** An L-shaped room: 5 slices, with an arm to the right, open at the arm's side. */
	public static final Identifier L_ROOM = Identifier.fromNamespaceAndPath("bettercoppers_test", "l_room");

	@Override
	public void onInitialize() {
		BetterCoppersApi.registerMineSection(LEAD, 1, MineSectionTemplate.builder().columns(0, 1, -1, 1).openEnd(2, 0, Turn.AHEAD).build(), parent -> false);
		// The support stands at the far end: at f1 its posts and the middle rails would shut the golem into the arm.
		// Far heavier than every built-in together, so it is all but sure to be rolled first.
		BetterCoppersApi.registerMineSection(L_ROOM, 1_000_000, MineSectionTemplate.builder()
			.columns(0, 4, -1, 1).columns(2, 4, 2, 4)
			.support(4, -1, 1).torch(4, 3).rail(0, 0).rail(1, 0).rail(2, 0).rail(3, 0).rail(3, 1).rail(3, 2).rail(3, 3)
			.openEnd(3, 5, Turn.RIGHT)
			.build(), parent -> parent.equals(LEAD));
	}
}

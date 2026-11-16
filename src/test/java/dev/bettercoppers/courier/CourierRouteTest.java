package dev.bettercoppers.courier;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class CourierRouteTest {
	private static final BlockPos FROM = new BlockPos(0, 64, 0);
	private static final BlockPos TO = new BlockPos(100, 64, 0);
	private static final BlockPos A = new BlockPos(30, 64, 20);
	private static final BlockPos B = new BlockPos(70, 64, 20);

	@Test
	void aRouteSavedWithoutStopsLoadsWithNone() {
		CourierRoute route = CourierRoute.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"from\":[0,64,0],\"to\":[100,64,0]}")).getOrThrow();
		assertEquals(new CourierRoute(FROM, TO), route);
	}

	@Test
	void stopsSurviveSaving() {
		CourierRoute route = new CourierRoute(FROM, TO, List.of(A, B));
		assertEquals(route, CourierRoute.CODEC.parse(JsonOps.INSTANCE, CourierRoute.CODEC.encodeStart(JsonOps.INSTANCE, route).getOrThrow()).getOrThrow());
	}

	@Test
	void stopsComeInReverseOnTheWayBack() {
		CourierRoute route = new CourierRoute(FROM, TO, List.of(A, B));
		assertEquals(List.of(A, B), route.stopsTowards(TO));
		assertEquals(List.of(B, A), route.stopsTowards(FROM));
	}
}

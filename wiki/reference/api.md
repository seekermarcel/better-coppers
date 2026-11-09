---
title: Add-on API
---

# Add-on API

The **add-on API** is the Java package `dev.bettercoppers.api`, through which an add-on adds jobs, golem behaviours, link targets, upgrades, charging blocks and Golem Handbook chapters to Better Coppers. It is written for add-on authors; this page names Java classes, unlike the rest of the wiki.

Add-ons import only from this package. The [Automatisation](/automatisation/) add-on is held to that by a Gradle check, and a missing hook is added to the package instead of importing internals (ADR 0004 in `docs/adr/`). The package is a seam for the project's own add-ons: no stability is promised to third parties yet, which is why Automatisation pins the exact base mod version.

Everything is registered during mod initialisation, before any golem loads.

```java
public class ExampleAddon implements ModInitializer {
	@Override
	public void onInitialize() {
		BetterCoppersApi.registerJob(ExampleJobs.GARDENER);
		BetterCoppersApi.registerBehavior(GardenBehavior::new);
		BetterCoppersApi.registerLinkTarget(ExampleJobs.GARDEN_BED);
		BetterCoppersApi.registerUpgrade(ExampleJobs.LUBRICANT);
		BetterCoppersApi.registerChargingSpot(ExampleJobs.SOLAR_MAT);
		BetterCoppersApi.registerHandbookChapter(Identifier.fromNamespaceAndPath("example", "gardening"));
	}
}
```

## Depending on the mod

In this repository, the add-on builds against the base mod as a Gradle project, in `automatisation/build.gradle`:

```groovy
implementation project(':')
```

Its `fabric.mod.json` declares the base mod under `depends`, on the same version:

```json
"bettercoppers": "${version}"
```

The build files define no Maven publication of the base mod, so no external coordinates exist yet.

## Registering hooks

| Method on `BetterCoppersApi` | What it does |
|---|---|
| `registerJob(GolemJob)` | Adds a [job](#golemjob). |
| `registerBehavior(Supplier<? extends Behavior<CopperGolem>>)` | Adds a [behaviour](#behaviours) to every copper golem's brain. |
| `registerLinkTarget(GolemLinkTarget)` | Lets players [link](#golemlinktarget) golems to a block. |
| `registerUpgrade(GolemUpgrade)` | Adds an [upgrade](#golemupgrade). |
| `registerChargingSpot(ChargingSpot)` | Adds a block golems [recharge](#chargingspot) on. |
| `registerHandbookChapter(Identifier)` | Adds a [Golem Handbook chapter](#handbook-chapters). |
| `CREATIVE_TAB` | Key of the "Better Coppers" [creative tab](#creative-tab). |

`jobs()`, `behaviors()`, `linkTargets()`, `upgrades()` and `handbookChapters()` return the registered lists.

## GolemJob

A job added by another mod. Registered jobs count as work everywhere in the mod: such golems do not sort, show up in the status message, pay the running costs and drop their job items when they die.

| Method | Required | What it does |
|---|---|---|
| `boolean has(CopperGolem)` | Yes | Whether the golem has this job now. An item check alone is not enough, since a golem can pick up any item by itself and only a player may give a job. Record the player's act, for example in an attachment, and require it. |
| `Component name()` | Yes | The job's name in the status message. |
| `BlockPos workplace(CopperGolem)` | No | Where the golem works, for the particle line while sneaking and the status message; null if nowhere yet. Default null. |
| `void dropItems(ServerLevel, CopperGolem)` | No | Drops what the golem carries for this job, on death or on turning into a statue. |
| `void onEnded(CopperGolem)` | No | A player ended or changed the golem's job. Called for every registered job, also on golems that never had this one. Clear what `has` relies on, or the golem can regain the job by picking up an item. What is in its hands has already gone back to the player. |

```java
public static final AttachmentType<Boolean> GARDENER_MARK = AttachmentRegistry.create(
	Identifier.fromNamespaceAndPath("example", "gardener"), builder -> builder.persistent(Codec.BOOL)
);

public static final GolemJob GARDENER = new GolemJob() {
	@Override
	public boolean has(CopperGolem golem) {
		return Boolean.TRUE.equals(golem.getAttached(GARDENER_MARK));
	}

	@Override
	public Component name() {
		return Component.translatable("job.example.gardener");
	}

	@Override
	public void onEnded(CopperGolem golem) {
		golem.removeAttached(GARDENER_MARK);
	}
};
```

`giveTool(Player, CopperGolem, ItemStack)` puts one of the held item in the golem's hand, as the base mod's job items do: the player gets back what the golem carries for any other job, whose data goes. Record the player's act for `has` after it.

```java
// In a UseEntityCallback, after checking for sneak and the job item.
BetterCoppersApi.giveTool(player, golem, held);
golem.setAttached(GARDENER_MARK, true);
```

`jobOf(CopperGolem)` returns the registered job the golem has, or null.

## Behaviours

`registerBehavior` takes a factory, since behaviours keep per-golem state and each golem needs a fresh one. The behaviour runs next to the built-in jobs, and it should only start for its own job and leave every other golem alone.

```java
public class GardenBehavior extends Behavior<CopperGolem> {
	public GardenBehavior() {
		super(ImmutableMap.of(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS, MemoryStatus.VALUE_ABSENT));
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel level, CopperGolem golem) {
		return ExampleJobs.GARDENER.has(golem) && BetterCoppersApi.canWork(level, golem);
	}
}
```

| Helper on `BetterCoppersApi` | What it does |
|---|---|
| `canWork(ServerLevel, CopperGolem)` | Whether the golem may start work: daytime, not on an errand, not recharging after running empty, not oxidized and not waxed. |
| `workSpeed(CopperGolem)` | How fast the golem moves and works, 1 being normal. Rust and upgrades both count. |
| `paysRunningCosts(CopperGolem)` | Whether the golem has a paying job: not a sorter, not a courier, and not without a job. |
| `spendEnergy(CopperGolem, int)` | Spends energy for a piece of work and adds rust progress. |
| `radius(CopperGolem, int)` | A work radius, widened by the golem's [eyes](/golem/eyes) (1.5x iron, 2x gold, 3x diamond). |
| `carryLimit(CopperGolem, int)` | How many items the golem carries in its hand, upgrades included. |
| `drawLinkLine(ServerLevel, CopperGolem, BlockPos)` | Draws the particle line from the golem to a linked block, as for a home. |
| `hasRoom(Container, ItemStack)` | Whether a slot could take the item: an empty slot that accepts it, or a matching stack that is not full. |
| `contains(Container, Predicate<ItemStack>)` | Whether a non-empty slot matches. |

```java
int range = BetterCoppersApi.radius(golem, 8);
float speed = 0.8F * BetterCoppersApi.workSpeed(golem);
BetterCoppersApi.spendEnergy(golem, 1);
```

### TravelWatch

`TravelWatch` tells a slow golem that is still getting somewhere apart from one that is stuck. Only ticks in which the golem did not get more than 0.5 blocks closer to its target than its best distance so far count as stalled.

| Method | What it does |
|---|---|
| `int tick(Entity, Vec3)` | Records one tick of walking towards the target and returns how many ticks in a row made no progress. |
| `void reset()` | Starts over, for a new target. |

```java
private final TravelWatch travel = new TravelWatch();

if (this.travel.tick(golem, Vec3.atCenterOf(target)) > 300) {
	// Stuck: give up on this target.
}
```

## GolemLinkTarget

A block players can link golems to: after sneak + right-clicking a golem with an empty hand, a right-click on such a block calls `link`. Link targets are checked before copper chests and courier routes, and not while a courier route is half finished. `link` is not called if the golem is not loaded, and the mod applies no distance limit of its own, so the target must check distance itself.

| Method | What it does |
|---|---|
| `boolean accepts(BlockState)` | Whether this block is a link target. |
| `InteractionResult link(ServerPlayer, ServerLevel, CopperGolem, BlockPos)` | Makes the link. Its result is the result of the click. |

```java
public static final GolemLinkTarget GARDEN_BED = new GolemLinkTarget() {
	@Override
	public boolean accepts(BlockState state) {
		return state.is(ExampleBlocks.GARDEN_BED);
	}

	@Override
	public InteractionResult link(ServerPlayer player, ServerLevel level, CopperGolem golem, BlockPos pos) {
		golem.setAttached(GARDEN_POS, pos.immutable());
		BetterCoppersApi.drawLinkLine(level, golem, pos);
		return InteractionResult.SUCCESS;
	}
};
```

## GolemUpgrade

Changes how a golem works. Every registered upgrade is asked about every golem, so each method returns its neutral value for golems it does not apply to. All methods have defaults.

| Method | What it does | Default |
|---|---|---|
| `float speed(CopperGolem)` | Multiplies how fast the golem walks and works. Multiple upgrades multiply. | 1 |
| `boolean preventsRust(CopperGolem)` | True while the golem should not rust from work. | false |
| `int carryLimit(CopperGolem, int base)` | Items carried in the hand. `base` starts at 16 and is passed through each upgrade in turn. | `base` |
| `void onSpend(CopperGolem, int energy)` | Called whenever the golem uses energy, for upgrades that wear off with use. | Nothing |
| `void dropItems(ServerLevel, CopperGolem)` | Drops what the upgrade gives back, on death or on turning into a statue. | Nothing |
| `Component status(CopperGolem)` | An extra part for the status message, or null. | null |

```java
public static final GolemUpgrade LUBRICANT = new GolemUpgrade() {
	@Override
	public float speed(CopperGolem golem) {
		return golem.getAttached(LUBRICATED) != null ? 1.1F : 1.0F;
	}
};
```

## ChargingSpot

A block golems recharge on every tick, besides the [Charging Pad](/items/charging_pad), which charges on redstone pulses. A golem looks for one the same way it looks for a Charging Pad, within 16 blocks of its place and 6 up or down, and charges while standing on it.

| Method | What it does |
|---|---|
| `boolean isSpot(BlockState)` | Whether this block is a charging spot. |
| `int chargePerTick()` | Energy per tick. Full is 1000, and a Charging Pad on the fastest clock gives about 2 per tick. |

```java
public static final ChargingSpot SOLAR_MAT = new ChargingSpot() {
	@Override
	public boolean isSpot(BlockState state) {
		return state.is(ExampleBlocks.SOLAR_MAT);
	}

	@Override
	public int chargePerTick() {
		return 1;
	}
};
```

`chargingSpot(BlockState)` returns the first spot matching the block, or null for an ordinary block.

## Handbook chapters

`registerHandbookChapter` adds a chapter after the built-in ones, in registration order. Its title and text come from the translation keys `handbook.<namespace>.<path>.title` and `handbook.<namespace>.<path>.text`. In the text, a line starting with `# ` is a heading and a blank line starts a new paragraph; pages are laid out automatically.

```json
{
	"handbook.example.gardening.title": "Gardening",
	"handbook.example.gardening.text": "# Getting started\nGive a golem a trowel.\n\nIt tends the beds around its home."
}
```

## Guide advancements

An add-on's guide steps are ordinary advancements with an `impossible` criterion named `done`.

```json
{
	"parent": "example:guide/root",
	"display": {
		"icon": { "id": "minecraft:wheat" },
		"title": { "translate": "advancements.example.guide.gardener.title" },
		"description": { "translate": "advancements.example.guide.gardener.description" }
	},
	"criteria": { "done": { "trigger": "minecraft:impossible" } },
	"requirements": [["done"]]
}
```

| Method | What it does |
|---|---|
| `awardGuide(Player, Identifier)` | Awards the step to one player. Does nothing on the client or for an unknown advancement. |
| `awardGuideNearby(ServerLevel, Vec3, Identifier)` | Awards it to every non-spectator player within 48 blocks of the position, for something a golem or machine did. |

```java
BetterCoppersApi.awardGuide(player, Identifier.fromNamespaceAndPath("example", "guide/gardener"));
```

## Creative tab

`CREATIVE_TAB` is the key of the mod's creative tab. Items are added with Fabric's event, which works whichever mod initialises first.

```java
CreativeModeTabEvents.modifyOutputEvent(BetterCoppersApi.CREATIVE_TAB).register(output -> output.accept(EXAMPLE_ITEM));
```

## Copper Chest Minecarts

| Method on `BetterCoppersApi` | What it does |
|---|---|
| `canExtendCart(Entity)` | Whether this is a [Copper Chest Minecart](/items/copper_chest_minecart) that is not yet a double chest. |
| `extendCart(Entity, ItemStack)` | Makes it a double chest. Only call after `canExtendCart`. |

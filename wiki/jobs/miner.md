---
title: Miner
infobox:
  icon: copper_pickaxe
  rows:
    Job item: Copper pickaxe and Mineshaft Plan
    Needs home: "Yes"
    Running costs: Energy, rust, tool wear
    Work radius: The Mineshaft Plan's tunnel, 8 to 128 blocks
    Mod: Better Coppers
---

# Miner

A **miner** is a [copper golem](/golem/copper-golem) that digs a mineshaft in the style of a vanilla [abandoned mineshaft](https://minecraft.wiki/w/Mineshaft), following a [Mineshaft Plan](/items/mineshaft_plan). It digs out the ores it comes across, keeps the tunnel safe from water and lava, and brings what it mines to its [home](/start/homes-and-status) chest.

## Setup

1. Craft a [Survey Table](/items/survey_table) and make a Mineshaft Plan at it.
2. [Mark](/items/mineshaft_plan#usage) the entrance by using the Mineshaft Plan on the ground or a wall. The tunnel heads the way the player is looking.
3. Sneak + right-click the golem with a copper pickaxe, then with the marked Mineshaft Plan. The order matters: a golem without a pickaxe refuses the Mineshaft Plan. An unmarked Mineshaft Plan is refused too.
4. Link the golem to a copper chest near the entrance: sneak + right-click it with an empty hand, then right-click the chest within 32 blocks of the golem.
5. Stock the chest with [supplies](#supplies).

<!-- MEDIA: jobs-miner-setup.png | screenshot | A marked entrance on the surface with a copper chest next to it holding rails, fences, planks and torches, and a copper golem holding a copper pickaxe -->

## Behaviour

### The tunnel

The tunnel is 3 blocks wide and 3 blocks high. The marked entrance is the middle block of the first slice at floor height. A **slice** is everything at one distance along the tunnel. Slices count from 0.

```
 P P P    plank beam, on supporting slices
 F . F    fence posts, two high
 F R F    rail in the middle column
 p p p    plank floor, only where there is none
```

| Part | Where | Made of |
|---|---|---|
| Opening | Every slice, 3 wide and 3 high | Dug out |
| Floor | Under the slice, where the block below is not solid | Any planks or [filler](#filler) |
| Support | Slices 2, 6, 10, and so on (every 4th) | 2 fence posts on each side, 2 high, and a 3 wide plank beam on top |
| Torch | Slices 3, 11, 19, and so on (every 8th) | Hung on the previous beam. Skipped where the light level is already 10 or more |
| Rail | Every slice, middle column | Laid last, from the far end back to the entrance |

The golem works one slice at a time: the middle column first, then the left and right columns, each from the top down. It then lays the floor, the support and the torch. It stands in the middle of the slice behind the one it works on. If there is no floor to stand on, as at a ledge or the edge of a cave, it lays one first from one block further back.

<!-- MEDIA: jobs-miner-shaft.png | screenshot | A finished 3x3 mineshaft from inside, with plank beams on fence posts, torches on the beams and rails down the middle -->

### Supplies

No rails, no digging. Before digging a slice, the golem checks that it and its chest together hold enough for that slice. Rails are checked for that slice and all before it, because rails go down last. If the chest has enough, it fetches them. If not, it goes home and waits, looking again every 7 seconds. It takes supplies only when it goes home to restock.

| Supply | Needed | Taken along per visit | Checked before digging |
|---|---|---|---|
| <Icon id="rail" /> Rails | 1 per slice | Up to the Mineshaft Plan's length | Yes |
| Wooden fences, any kind | 4 per support | Up to 16 | Yes, on support slices |
| <Icon id="oak_planks" /> Planks, any kind | 3 per support, plus floors | Up to 32 | Yes, on support slices |
| Torches | 1 every 8 slices | Up to 16 | No |

| Length | Rails | Fences | Planks for beams | Torches |
|---|---|---|---|---|
| 8 | 8 | 8 | 6 | 1 |
| 16 | 16 | 16 | 12 | 2 |
| 32 | 32 | 32 | 24 | 4 |
| 64 | 64 | 64 | 48 | 8 |
| 128 | 128 | 128 | 96 | 16 |

Floors add up to 3 more planks or filler blocks per slice. If a building material is missing from both the golem and its chest, that one step is skipped for good and the golem carries on.

A [sorter](/jobs/sorter) linked to the same chest leaves spare copper pickaxes alone. With a marked Mineshaft Plan it also leaves the torches, rails, planks and fences.

### Filler

**Filler** blocks are the `bettercoppers:filler` block tag: cobblestone, cobbled deepslate, stone, deepslate, andesite, diorite, granite, tuff, dirt, netherrack and blackstone. The golem uses them to plug water and lava and, in place of planks, for floors. It only gets filler from what it digs. It keeps up to 16 when it unloads at home, and does not take any from the chest.

### Digging

A block takes `hardness x 30 / tool speed x clumsiness` ticks, between 10 and 400 ticks. Clumsiness is 2 to 3.2, divided by the rust factor. A copper pickaxe has a tool speed of 5. A clean golem digs stone in 18 to 29 ticks, about 1 to 1.5 seconds. Placing a block takes 20 ticks plus a 10 tick pause.

Drops use the tool, so Fortune and Silk Touch work. The pickaxe loses 1 durability per block with a hardness above 0. Copper pickaxes have 190 durability.

### Ores

After each block it digs, the golem queues every ore touching that block, on all six sides, and digs those too. Ores are the `c:ores` block tag. Each vein has a budget of 16 ores, or 24, 32 or 48 with iron, gold or diamond [eyes](/golem/eyes). A new budget starts once the queue of ores is empty.

A miner can harvest diamond, gold, redstone and emerald ore with its copper pickaxe, although a player could not. Other blocks that need a better pickaxe stay in the wall, such as ancient debris and obsidian.

### Obstacles

| Situation | What the golem does |
|---|---|
| Water or lava in the block, or touching it | Plugs it with filler, 15 ticks each, then digs on. Skips fluids the golem itself stands in |
| No filler at a fluid | Abandons the Mineshaft Plan: it goes to the end of its steps, and the golem goes home |
| Gravel or sand fell into its last 32 digs | Clears it before anything else |
| Walking without getting closer | Every 5 seconds, digs out the nearest block that fell into the finished tunnel |
| 15 seconds without getting closer | Gives up on that step and moves on |
| Unbreakable block, or a block with a block entity such as a chest | Leaves it and moves on |
| Block its pickaxe cannot harvest | Leaves it and moves on |
| Chunk not loaded | Waits. A miner does not load chunks |

Before starting, a golem sometimes shakes its head for 1.5 to 3 seconds.

### Bringing it home

Mined items go into a chained [Copper Chest Minecart](/items/copper_chest_minecart) first, then the pouch. The golem walks home when the cart has no empty slot and the pouch holds 64 items, or when it needs supplies from the chest. It unloads, restocks and goes back to the spot it reached.

A miner far from home may ride a [minecart or boat](/golem/riding) for trips over 24 blocks, unless it is towing a cart.

When the last step is done, it brings the rest home and idles. The finished Mineshaft Plan stays with it until the job ends.

### Diamond ore

Digging a diamond ore block excites a miner: it plays a chime and sends out particles, the knob on its antenna blinks, it finishes the whole vein and hurries home at 1.3 times speed to show what it found. A happy puff of particles and a sound end the trip.

## Running costs

| Action | Energy |
|---|---|
| Digging a block | 2 |
| Placing a block (floor, support, rail, torch, plug) | 1 |

A full charge of 1000 energy is 500 digs. Miners stop at night. See [Running costs](/golem/running-costs).

When the pickaxe breaks, the golem keeps its job, brings its load home and looks for a new copper pickaxe. See [Tools](/golem/tools).

## Limits and edge cases

- Handing over a new marked Mineshaft Plan returns the old one, progress included.
- When a miner's job ends, the player gets the pickaxe and the Mineshaft Plan back with its progress.
- When a miner dies, the Mineshaft Plan drops with its progress.
- A miner needs a home. Without one, it does nothing.
- If the chest is broken or unloaded, the golem keeps its load and looks again every 10 seconds.
- If the chest is full, what does not fit stays with the golem and it tries again.
- Marking a Mineshaft Plan again resets its progress to 0.

## Advancements

| Advancement | How | ID |
|---|---|---|
| Surveyor | Have a Mineshaft Plan | `bettercoppers:guide/survey` |
| Down the Shaft | Give a golem a copper pickaxe and a Mineshaft Plan | `bettercoppers:guide/miner` |
| Shiny! | A miner digs diamond ore within 48 blocks of the player | `bettercoppers:guide/diamonds` |
| Full Employment | Give golems all eight jobs | `bettercoppers:guide/full_employment` |

## Data values

| Name | Value |
|---|---|
| Job item | `minecraft:copper_pickaxe`, plus `bettercoppers:mineshaft_plan` |
| Status name key | `job.bettercoppers.miner` |
| Filler tag | `bettercoppers:filler` (block) |
| Ore tag | `c:ores` (block) |
| Diamond ore tag | `c:ores/diamond` (block) |

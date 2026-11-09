---
title: Miner
infobox:
  icon: copper_pickaxe
  rows:
    Job item: Copper pickaxe and Mineshaft Plan
    Needs home: "Yes"
    Running costs: Energy, rust, tool wear
    Work radius: The Mineshaft Plan's mine, 8 to 128 blocks
    Mod: Better Coppers
---

# Miner

A **miner** is a [copper golem](/golem/copper-golem) that digs the mine a [Mineshaft Plan](/items/mineshaft_plan) describes: a [mineshaft](#the-tunnel) in the style of a vanilla [abandoned mineshaft](https://minecraft.wiki/w/Mineshaft), a [Staircase](#the-staircase), a [Strip Mine](#strip-mine), a [Room and Pillar](#room-and-pillar) hall or a [Multilevel Mine](#multilevel-mine). It digs out the ores it comes across, keeps the tunnel safe from water and lava, and brings what it mines to its [home](/start/homes-and-status) chest.

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
| Torch | Slices 3, 11, 19, and so on (every 8th) | Hung on the previous beam. Skipped where the light level is already above 10 |
| Rail | Every slice, middle column | Laid last, from the far end back to the entrance |

The golem works one slice at a time: the middle column first, then the left and right columns, each from the top down. It then lays the floor, the support and the torch. It stands in the middle of the slice behind the one it works on. If there is no floor to stand on, as at a ledge or the edge of a cave, it lays one first from one block further back.

<!-- MEDIA: jobs-miner-shaft.png | screenshot | A finished 3x3 mineshaft from inside, with plank beams on fence posts, torches on the beams and rails down the middle -->

### The Staircase

A **Staircase** is a walkable stairway going down from the entrance, one block down per block forward, the way the player looked when marking. It is 1 block wide with 2 clear blocks above each step. The Mineshaft Plan's length is its depth, so the lowest step is that many blocks below the entrance. It stops 5 layers above the bottom of the world, where bedrock begins, and is shorter if the entrance is too low.

| Part | Where | Made of |
|---|---|---|
| Opening | Every step, 2 high | Dug out |
| Floor | Under the step, where the block below is not solid | Any planks or [filler](#filler) |
| Torch | Steps 4, 12, 20, and so on (every 8th), on the right wall at head height, seen going down | Skipped where the light level is already above 10 |

The golem works from the step above, the upper block first. A Staircase needs no rails, fences or supports, so there is nothing to [check before digging](#supplies); the golem still takes floor patches and torches along when it visits its chest. Golems and chained [Copper Chest Minecarts](/items/copper_chest_minecart) walk the stairs, and every trip goes back to the one home chest.

### Strip Mine

A **Strip Mine** exposes as much ore as possible. It has a corridor as long as the Mineshaft Plan's length and branches to both sides. Slices count from 0, as in the tunnel.

| Part | Where | Made of |
|---|---|---|
| Corridor | Every slice, 3 wide and 3 high | Dug out |
| Branch | Both sides of slices 2, 5, 8, and so on, 1 wide and 2 high, leaving 2 blocks of rock between branches. As long as the Mineshaft Plan's branch length, 4 to 32 blocks | Dug out |
| Floor | Under the corridor and branches, where the block below is not solid | Any planks or [filler](#filler) |
| Torch | Slices 3, 12, 21, and so on, on the right wall, and every 9th block of a branch starting at the 4th | Skipped where the light level is already above 10 |

The golem digs a stretch of 3 slices of corridor, then that stretch's right branch, then its left branch, then the next stretch. There are no rails or supports, and a miner on a Strip Mine leaves fences in the chest. Torches are optional: without any it digs on in the dark.

### Room and Pillar

The **Room and Pillar** Mine Pattern digs a wide hall, 3 blocks high, instead of a tunnel. It starts right at the marked entrance, which is the middle of row 0, with no connecting corridor. To get a small door, mark the Mineshaft Plan from inside a short tunnel. Seen from above, with the golem digging upward and `#` a pillar:

```
row 7   .........
row 6   ..#...#..
row 5   .........
row 4   .........
row 3   .........
row 2   ..#...#..
row 1   .........
row 0   .........
            ^ entrance, centre column
```

| Part | Where | Made of |
|---|---|---|
| Hall | 9 or 17 wide (the Mine Pattern's setting), 8 to 64 long, 3 high | Dug out |
| Floor | Under each column, where the block below is not solid | Any planks or [filler](#filler) |
| Pillar | Rows 2, 6, 10, and so on, at 2 and, in a 17 wide hall, 6 blocks each side of the centre. 1×1, floor to ceiling | Rock left standing. Where a cave already removed it there is no pillar |
| Torch | On the entrance side of the pillars in rows 2, 10, 18, and so on. Up to 16 in a 9 wide hall, 32 in a 17 wide one | Skipped where the light level is already above 10 |

The golem finishes each row before the next: the centre column, then outward to one side, then the other, each column from the top down. It stands in the part of the row just dug, or in the row behind where a pillar is in the way.

A hall needs no [supplies](#supplies) except torches, which are optional, and planks or filler for floors. A miner on a hall leaves rails and fences in the chest. A plan edited by command to a length or hall width it cannot have makes the nearest hall it can: at most 64 long, and 17 wide if the width is 17 or more, otherwise 9.

<!-- MEDIA: jobs-miner-hall.png | screenshot | A finished Room and Pillar hall from inside, 9 wide, with rock pillars and torches on the pillars facing the entrance -->

### Multilevel Mine

A **Multilevel Mine** is a stack of [Mineshaft tunnels](#the-tunnel), each as long as the Mineshaft Plan's length, straight below the first. The floor of each level is 6 blocks below the last, which leaves 3 solid blocks between them.

| Part | Where | Made of |
|---|---|---|
| Levels | 2 to 8 (the Mine Pattern's setting), each a whole Mineshaft with its supports, torches and rails | As in a [Mineshaft](#the-tunnel) |
| Stairs | One set between each pair of levels, in the two rows behind the first slice, going off to the right of the entrance seen looking along the tunnel | Dug out, 1 wide and 2 high, with no rails or supports |
| Floor | Under the stairs, where the block below is not solid | Any planks or [filler](#filler) |
| Torch | One at the landing halfway down each set of stairs | Skipped where the light level is already above 10 |

Each set of stairs goes down 3 blocks to the right, one flat block further, one flat block back from the entrance, and 3 blocks down to the left, then two flat blocks to the next level's entrance. Keep the ground to the right of the entrance free: the stairs of the first level start in the block in front of the entrance. Levels that would have their floor in the 5 layers at the bottom of the world are dropped, along with the stairs to them.

The golem finishes a level, rails included, before digging the stairs below it, so it restocks for one level at a time. There are no chests on the lower levels: every trip is walked to the one home chest at the top. A golem may [ride](/golem/riding) a minecart along the rails of a level on a long trip, then walks the stairs. A plan edited by command to a length or number of levels it cannot have makes the nearest it can.

<!-- MEDIA: jobs-miner-multilevel.png | screenshot | The landing of the stairs between two levels of a Multilevel Mine, seen from the flight going down, with a torch on the wall -->

### Supplies

In a Mineshaft, no rails, no digging. Before digging a slice, the golem checks that it and its chest together hold enough for that slice. Rails are checked for that slice and all before it, because rails go down last. If the chest has enough, it fetches them. If not, it goes home and waits, looking again every 7 seconds. It takes supplies only when it goes home to restock.

| Supply | Needed | Taken along per visit | Checked before digging |
|---|---|---|---|
| <Icon id="rail" /> Rails | 1 per slice | Up to the Mineshaft Plan's length | Yes |
| Wooden fences, any kind | 4 per support | Up to 16, none for a Staircase, Strip Mine or Room and Pillar hall | Yes, on support slices |
| <Icon id="oak_planks" /> Planks, any kind | 3 per support, plus floors | Up to 32 | Yes, on support slices |
| Torches | 1 every 8 slices | Up to 16 | No |

| Length | Rails | Fences | Planks for beams | Torches |
|---|---|---|---|---|
| 8 | 8 | 8 | 6 | 1 |
| 16 | 16 | 16 | 12 | 2 |
| 32 | 32 | 32 | 24 | 4 |
| 64 | 64 | 64 | 48 | 8 |
| 128 | 128 | 128 | 96 | 16 |

The table is for one level: a Multilevel Mine takes that much for each of its levels. Floors add up to 3 more planks or filler blocks per slice. If a building material is missing from both the golem and its chest, that one step is skipped for good and the golem carries on.

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
| No filler at a fluid | Gives up the current Mine Section, which in a mineshaft is the rest of the tunnel and in a Staircase the rest of the stairs. In a Strip Mine it is the rest of one branch, or in the corridor the rest of the corridor and its later branches. In a Room and Pillar hall it is the outer part of that half-row, or the rest of the hall if it is in the centre column; the other half and later rows are still dug. In a Multilevel Mine it is the rest of that level's tunnel, and the levels below are still dug because the stairs start behind the first slice; on the stairs it is everything below them. If that leaves the row just in front of a pillar undug, the golem cannot get past that pillar in the next row and gives up the blocks there, one by one, after 15 seconds each. It does not go home for filler: it carries on at once and, in a mineshaft, lays rails in the part already dug. The Mineshaft Plan's tooltip and the golem's status show the block where it gave up (only the latest). Marking the Mineshaft Plan again is the retry: finished steps skip themselves |
| No filler at a fluid beside a chased ore or fallen block | Skips only that block. The Mineshaft Plan is untouched |
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
- If the chest is full, what does not fit stays with the golem. It waits at home and goes back to its plan once something it carries fits.
- Marking a Mineshaft Plan again resets its progress to 0.

## Advancements

| Advancement | How | ID |
|---|---|---|
| Surveyor | Have a Mineshaft Plan | `bettercoppers:guide/survey` |
| Down the Shaft | Give a golem a copper pickaxe and a Mineshaft Plan | `bettercoppers:guide/miner` |
| Shiny! | A miner digs diamond ore within 48 blocks of the player | `bettercoppers:guide/diamonds` |
| Full Employment | Give golems all nine jobs | `bettercoppers:guide/full_employment` |

## Data values

| Name | Value |
|---|---|
| Job item | `minecraft:copper_pickaxe`, plus `bettercoppers:mineshaft_plan` |
| Status name key | `job.bettercoppers.miner` |
| Filler tag | `bettercoppers:filler` (block) |
| Ore tag | `c:ores` (block) |
| Diamond ore tag | `c:ores/diamond` (block) |

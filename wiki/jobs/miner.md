---
title: Miner
infobox:
  icon: copper_pickaxe
  rows:
    Job item: Copper pickaxe
    Needs home: "Yes"
    Running costs: Energy, rust, tool wear
    Work radius: A Mine Entrance within 32 blocks of its home chest
    Mod: Better Coppers
---

# Miner

A **miner** is a [copper golem](/golem/copper-golem) that digs a mine from a [Mine Entrance](#mine-entrance) you build, in the style of a vanilla [abandoned mineshaft](https://minecraft.wiki/w/Mineshaft). It digs out the ores it comes across, keeps the tunnel safe from water and lava, and brings what it mines to its [home](/start/homes-and-status) chest.

## Setup

1. Build a [Mine Entrance](#mine-entrance) within 32 blocks of a copper chest, against a hillside if you can.
2. Sneak + right-click the golem with a copper pickaxe.
3. Link the golem to the copper chest: sneak + right-click it with an empty hand, then right-click the chest within 32 blocks of the golem. Steps 2 and 3 work in either order.
4. Stock the chest with [supplies](#supplies).

Handing over the pickaxe to a golem without a home shows "Now link it to a copper chest within 32 blocks of a Mine Entrance". With a home but no open entrance near it, it shows "No open Mine Entrance within 32 blocks of its home chest".

<!-- MEDIA: jobs-miner-setup.png | screenshot | A Mine Entrance in a hillside with a copper chest next to it holding rails, fences, planks and torches, and a copper golem holding a copper pickaxe -->

## Mine Entrance

A **Mine Entrance** is a frame like the supports in a vanilla mineshaft: two posts of any wooden fence, each 2 high and standing on a solid block, 1 block apart, with 3 planks of any wood across their tops. The opening between the posts must be free to walk through. The frame stands along the X or Z axis.

```
 P P P    3 planks
 F . F    fence posts, 2 high
 F . F    the opening between them stays free
```

Placing the frame's last block yourself shows "Mine Entrance built" and awards [Into the Hill](#advancements). Only blocks a player places count: frames from commands, structures or world generation are not entrances until a player rebuilds one of their blocks. Each dimension saves its entrances, so a frame rebuilt on the same spot along the same line is the same entrance again.

<!-- MEDIA: jobs-miner-entrance.png | screenshot | A Mine Entrance built into a hillside: two oak fence posts 2 high with 3 spruce planks across their tops, and the "Mine Entrance built" message above the hotbar -->

### Finding it

A miner with a home takes the nearest Mine Entrance within the [Entrance Search Radius](#settings) (32 blocks) of its home chest (straight-line distance to the middle plank) whose frame is whole and whose mine is not finished. It looks when it gets the pickaxe or a home, and every 5 seconds while it has none. It keeps that mine until the mine is finished, the frame is broken, it is linked to another chest or its job ends. A closer entrance built later does not take it away. A plain click on the miner shows "Waiting: every tunnel has a miner" (see [Several miners](#several-miners)), "Mine at x y z" (the middle plank), or "No open Mine Entrance within 32 blocks of its home chest".

### Which way it runs

The mine runs at right angles to the frame, into the hill: towards the side where the block at head height in front of the opening is solid. If both sides are solid or both are open, it runs away from the home chest of the first miner that takes it. The way is saved with the mine.

### A broken frame

The frame counts as whole while both posts and the planks stand; the opening may hold anything. A miner checks it when it starts a section and after unloading at home. If the frame is broken, the miner forgets that mine, goes home and looks for another entrance. Rebuilding the frame on the same spot along the same line opens the mine again.

## Behaviour

A mine grows one **Mine Section** at a time. Positions below count from the opening: forward along the mine, sides from the middle column, heights from the floor of the opening.

### First section

The first section is not left to chance. If the block at head height in front of the opening is solid, it is a [Straight](#straight). On open ground it is [Stairs Down](#stairs-down).

A first section that would reach past the [Mine Range or the Depth Floor](#settings) is not dug: the miner leaves that entrance alone and keeps looking.

### The next sections

Every section but a Dead End leaves one or more **Open Ends**: the block where the next section starts, and the way it faces. When its section is done, the miner takes the Open End nearest to it and rolls the next section there. The roll is random, but each Open End always rolls the same: it is seeded from the world seed, the entrance and the Open End.

| Section | Weight | Above y 0 | Open Ends |
|---|---|---|---|
| [Straight](#straight) | 40 | 40 | 1 ahead |
| [Corner](#corner), right or left | 8 + 8 | 8 + 8 | 1 to the side |
| [Stairs Down](#stairs-down) | 12 | 24 | 1 ahead, 4 lower |
| [Stairs Up](#stairs-up) | 6 | 3 | 1 ahead, 4 higher |
| [Crossing](#crossing) | 10 | 10 | 3: ahead, left and right |
| [Small Hall](#small-hall) | 5 | 5 | 3: ahead, left and right |
| [Dead End](#dead-end) | 11 | 11 | none |

Some kinds can't follow others: no Stairs Up right after Stairs Down or the other way round, no Corner right after a Corner, and never a Dead End on the mine's last Open End. The miner tries up to 3 of the rolled kinds, each picked by weight from those not tried yet. A section that would overlap a tunnel already saved, in this mine or another, or reach past the [Mine Range or the Depth Floor](#settings), is left out. If none of the 3 fits, that Open End is dropped.

A mine is finished when no Open End is left and every section is done. The miner then brings its load home and looks for the next open Mine Entrance.

### Several miners

Several miners can share a mine: every miner whose home chest has the same entrance nearest works in it. There is no limit to how many. A miner **claims** the section it digs, and no other miner digs a claimed section.

When a miner needs work, it takes, in one go:

1. a section that isn't done and that nobody holds a claim on, such as one another miner left half-dug, or else
2. an Open End whose section is done, where it rolls a new section.

The nearest one wins, and on a tie the oldest. Work in unloaded chunks is left out. With nothing free, the miner waits at home, strolls around its chest and looks again every 5 seconds. A plain click on it shows "Waiting: every tunnel has a miner".

A miner keeps its claim on trips home to unload or restock, while it shows off diamonds and while it digs out a vein. It lets go when the section is done, when its job ends, when it dies, when it is linked to another chest and when the frame is broken. A claim also runs out when the miner hasn't worked for 5 minutes, for example because its chunk isn't loaded, its pickaxe broke or it is busy elsewhere. Another miner may then take the section, and the first one looks for new work.

### Straight

8 slices, 3 blocks wide and 3 high. A **slice** is everything at one distance along the tunnel. Slices count from 0, starting just in front of the opening.

```
y+2     y+1     y+0     y-1
#####   #####   #####   #####
#####   #####   ##^##   #####
#...#   #...#   #.=.#   #___#
#PPP#   #F.F#   #F=F#   #___#
#...#   #...#   #.=.#   #___#
#...#   #...#   #.=.#   #___#
#.T.#   #...#   #.=.#   #___#
#PPP#   #F.F#   #F=F#   #___#
#...#   #...#   #.=.#   #___#
#...#   #...#   #.=.#   #___#
#ooo#   #ooo#   #ooo#   #####
```

Each block is one layer seen from above, the far end at the top: `o` is the opening, `.` dug out, `_` floor, `F` fence post, `P` planks, `T` torch, `=` rail and `^` where the next section would start.

| Part | Where | Made of |
|---|---|---|
| Opening | Every slice, 3 wide and 3 high | Dug out |
| Floor | Under the slice, where the block below is not solid | Any planks or [filler](#filler) |
| Support | Slices 2 and 6 | 2 fence posts on each side, 2 high, and a 3 wide plank beam on top |
| Torch | Slice 3, hung on the first beam | Skipped where the light level is already above 10 |
| Rail | Every slice, middle column | Laid last, from the far end back, only if the golem carries rails |

The golem works one slice at a time: the middle column first, then the left and right columns, each from the top down. It then lays the floor, the support and the torch. It stands in the middle of the slice behind the one it works on. If there is no floor to stand on, as at a ledge or the edge of a cave, it lays one first from one block further back.

<!-- MEDIA: jobs-miner-shaft.png | screenshot | A finished Straight from inside, with plank beams on fence posts, a torch on the first beam and rails down the middle -->

### Stairs Down

4 steps down, one per slice, each 4 blocks high so a player can walk back up, then a 3 high landing 4 blocks lower. It is 3 wide and has no supports. Seen from the side through the middle column:

```
y+3 #######
y+2 o.#####
y+1 o..####
y+0 o...###
y-1 #=...##
y-2 #_=...#
y-3 ##_=..#
y-4 ###_==>
y-5 ####__#
    -012345  <- slice
```

The golem digs each step from the step above it, top block first. A torch hangs on the left wall of slice 2, unless it is already bright, and rails become sloped rails by themselves.

<!-- MEDIA: jobs-miner-stairs-down.png | screenshot | A finished Stairs Down section seen from its top, 3 wide, with rails sloping down to the landing and a torch on the left wall -->

### Stairs Up

A flat slice, 4 steps up and a 3 high landing 4 blocks higher, 3 wide, no supports. Seen from the side through the middle column:

```
y+7 ########
y+6 ####...#
y+5 ###....#
y+4 ##...==>
y+3 #...=__#
y+2 o..=_###
y+1 o.=_####
y+0 o=_#####
y-1 #_######
    -0123456  <- slice
```

From the step behind, a golem reaches only the two lower blocks of a step, so it digs each step in two passes: the two lower blocks from behind, then, standing on the new step, its third block and the head room of the step behind. A torch hangs on the left wall of slice 3.

<!-- MEDIA: jobs-miner-stairs-up.png | screenshot | A finished Stairs Up section seen from its foot, with rails climbing to the landing and a torch on the left wall -->

### Corner

3 slices with the far wall left standing, open to the right or, mirrored, to the left. It has no support, because a support would block the turn. The rails curve by themselves.

```
y+2      y+1      y+0      y-1
######   ######   ######   ######
#...##   #...##   #...##   #___##
#...##   #...##   #.==>#   #___##
#...##   #...##   #.=.##   #___##
#ooo##   #ooo##   #ooo##   ######
```

### Crossing

5 by 5, shaped like a plus, with a plank pillar at each inner corner. It opens ahead, left and right. The rails run straight through, and a torch hangs on the right pillar of slice 1.

```
y+2         y+1         y+0         y-1
#########   #########   #########   #########
#########   #########   ####^####   #########
###...###   ###...###   ###.=.###   ###___###
##.P.P.##   ##.P.P.##   ##.P=P.##   ##_____##
##.....##   ##.....##   #<..=..>#   ##_____##
##.PTP.##   ##.P.P.##   ##.P=P.##   ##_____##
###...###   ###...###   ###.=.###   ###___###
###ooo###   ###ooo###   ###ooo###   #########
```

<!-- MEDIA: jobs-miner-crossing.png | screenshot | A finished Crossing from its entry, with four plank pillars, a torch on one and tunnels leading ahead, left and right -->

### Small Hall

5 wide, 7 long and 3 high, with wide supports at slices 1 and 5: a fence post on each side and a 5 wide plank beam. It opens ahead, left and right, between the supports. A torch hangs on each beam, facing into the hall.

```
y+2         y+1         y+0         y-1
#########   #########   #########   #########
#########   #########   ####^####   #########
##.....##   ##.....##   ##..=..##   ##_____##
##PPPPP##   ##F...F##   ##F.=.F##   ##_____##
##..T..##   ##.....##   ##..=..##   ##_____##
##.....##   ##.....##   #<..=..>#   ##_____##
##..T..##   ##.....##   ##..=..##   ##_____##
##PPPPP##   ##F...F##   ##F.=.F##   ##_____##
##.....##   ##.....##   ##..=..##   ##_____##
###ooo###   ###ooo###   ###ooo###   #########
```

<!-- MEDIA: jobs-miner-small-hall.png | screenshot | A finished Small Hall, 5 wide, with its two wide supports, two torches and side tunnels between the supports -->

### Dead End

4 slices with one support and a torch on the far face. It has no Open End and ends its branch on purpose, like the short dead ends of vanilla mineshafts.

```
y+2     y+1     y+0     y-1
#####   #####   #####   #####
#...#   #.T.#   #...#   #___#
#PPP#   #F.F#   #F=F#   #___#
#...#   #...#   #.=.#   #___#
#...#   #...#   #.=.#   #___#
#ooo#   #ooo#   #ooo#   #####
```

### Supplies

Before digging a support slice, the golem checks that it and its chest together hold its fences and planks. If the chest has enough, it fetches them. If not, it goes home and waits, looking again every 7 seconds. It takes supplies only when it goes home to restock. Rails are optional: with none in its supplies it skips them, and the section is still finished.

| Supply | Needed | Taken along per visit | Checked before digging |
|---|---|---|---|
| <Icon id="rail" /> Rails | 1 per slice | 1 per slice of the section | No, optional |
| Wooden fences, any kind | 4 per support | Up to 16, none for sections without supports | Yes, on support slices |
| <Icon id="oak_planks" /> Planks, any kind | 3 per support (5 in a Small Hall, 6 per pillar slice of a Crossing), plus floors | Up to 32 | Yes, on support and pillar slices |
| Torches | 1 every 8 slices | Up to 16 | No |

| Section | Rails | Fences | Planks for beams | Torches |
|---|---|---|---|---|
| Straight | 8 | 8 | 6 | 1 |
| Corner | 3 | 0 | 0 | 0 |
| Crossing | 5 | 0 | 12 | 1 |
| Small Hall | 7 | 8 | 10 | 2 |
| Stairs Down | 5 | 0 | 0 | 1 |
| Stairs Up | 6 | 0 | 0 | 1 |
| Dead End | 3 | 4 | 3 | 1 |

Floors add up to 3 more planks or filler blocks per slice. If a building material is missing from both the golem and its chest, that one step is skipped for good and the golem carries on.

A [sorter](/jobs/sorter) linked to the same chest leaves spare copper pickaxes alone. With a mine it also leaves the torches, rails, planks and fences.

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
| No filler at a fluid | Cuts the section short: what is dug stays, and the golem lays rails in the part already dug, then the section counts as done. Nothing grows on from its Open Ends. It does not go home for filler |
| No filler at a fluid beside a chased ore or fallen block | Skips only that block. The section carries on |
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

When a section is done, it rolls the next one without going home. When the mine is finished, it brings the rest home and looks for the next open Mine Entrance. Progress is saved with the mine, not on the golem, so a section half dug stays in the mine.

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

- A new copper pickaxe only swaps the tools: the miner keeps its mine.
- When a miner's job ends, the player gets the pickaxe and everything it carries. What it dug stays in the mine.
- When a miner dies, its load drops, and so does its pickaxe.
- Linking a miner to another chest makes it forget its mine and look again from there.
- A miner needs a home to work. Without one, it does nothing.
- If the chest is broken or unloaded, the golem keeps its load and looks again every 10 seconds.
- If the chest is full, what does not fit stays with the golem. It waits at home and goes back to its mine once something it carries fits.

## Settings

Three game rules in the Better Coppers category set each world's limits. Change them with `/gamerule`, on the world creation screen or in the game rules screen. They count from the next time a miner checks.

| Game rule | Default | Range | What it does |
|---|---|---|---|
| `bettercoppers:mine_entrance_search_radius` | 32 | 8 to 64 | How far from its home chest a miner looks for a Mine Entrance, in a straight line to the middle plank |
| `bettercoppers:mine_range` | 64 | 16 to 256 | How far a mine grows from the middle plank of its entrance, counted along x and z separately |
| `bettercoppers:mine_depth_floor` | -59 | -2032 to 2031 | The lowest height a miner digs at, but never lower than 5 above the bottom of the world |

## Advancements

| Advancement | How | ID |
|---|---|---|
| Into the Hill | Place the last block of a Mine Entrance | `bettercoppers:guide/mine_entrance` |
| Down the Shaft | Give a golem a copper pickaxe | `bettercoppers:guide/miner` |
| Shiny! | A miner digs diamond ore within 48 blocks of the player | `bettercoppers:guide/diamonds` |
| Full Employment | Give golems all nine jobs | `bettercoppers:guide/full_employment` |

## Data values

| Name | Value |
|---|---|
| Job item | `minecraft:copper_pickaxe` |
| Saved mines | `data/bettercoppers/mines.dat` in each dimension |
| Section kinds | `bettercoppers:straight`, `bettercoppers:corner`, `bettercoppers:crossing`, `bettercoppers:small_hall`, `bettercoppers:stairs_down`, `bettercoppers:stairs_up`, `bettercoppers:dead_end` |
| Status name key | `job.bettercoppers.miner` |
| Filler tag | `bettercoppers:filler` (block) |
| Ore tag | `c:ores` (block) |
| Diamond ore tag | `c:ores/diamond` (block) |

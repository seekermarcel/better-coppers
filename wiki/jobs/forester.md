---
title: Forester
infobox:
  icon: copper_axe
  rows:
    Job item: Copper axe
    Needs home: "Yes"
    Running costs: Energy, rust, tool wear
    Work radius: 12 blocks, 6 up and down
    Mod: Better Coppers
---

# Forester

A **forester** is a [copper golem](/golem/copper-golem) that fells natural trees around its [home](/start/homes-and-status) chest, collects what falls and replants the stumps. It never cuts logs a player placed.

## Setup

1. Sneak + right-click a clean golem with a copper axe. On a rusty or waxed golem the axe scrapes instead, so [scrape](/golem/running-costs) or unwax it first.
2. Link the golem to a copper chest within 32 blocks of the golem.
3. Optionally put saplings in the chest.

## Behaviour

The work area is a box around the home chest: 12 blocks to each side and 6 up and down, or 18, 24 or 36 blocks with iron, gold or diamond [eyes](/golem/eyes). The height stays 6.

A forester does these in order of priority:

1. **Pick up** saplings, apples, sticks and logs lying on the ground. Items on or in leaves are left until the leaves decay.
2. **Plant** a sapling on a stump waiting for one.
3. **Fell** the nearest natural tree.

### Felling

A log is a tree's base if there is no log under it and the block under it supports plants, and if the golem can stand next to it. A trunk is every log connected to the base, diagonals included, up to 200 logs. A tree is **natural** if a natural leaf block touches any log of its trunk. Trunks without natural leaves, such as a player's log walls, are left alone.

Chopping takes 30 ticks plus 8 ticks per log, longer with rust. A 5 log tree takes 70 ticks (3.5 seconds). The whole trunk then comes down at once, top first. The golem does not cut leaves, it waits for them to decay.

<!-- MEDIA: jobs-forester-felling.mp4 | clip | A forester chopping at an oak tree, the whole trunk disappearing at once, then the golem picking up the saplings and replanting the stump -->

### Replanting

After felling, the golem remembers every log of the trunk that stood on soil as a stump, if the log has a matching sapling, so a 2x2 tree gets four saplings back. Each stump only takes the species that grew there. Without that sapling it stays empty, and the golem carries on with other work until one turns up. Planting takes 20 ticks.

Saplings stay with the golem up to 8 of each kind when it unloads. At the chest it takes up to 8 of each species a stump is waiting for and it does not carry. When it has no saplings at all, it also takes up to 8 from the first stack of saplings in the chest.

### Bringing it home

Logs and drops go into a chained [Copper Chest Minecart](/items/copper_chest_minecart) first, then the pouch. The golem walks home at 64 items, or when it has nothing else to do.

## Running costs

| Action | Energy | Tool wear |
|---|---|---|
| Per log felled | 3 | 1 |
| Planting a sapling | 2 | None |
| Picking up an item | 0 | None |

A full charge of 1000 energy is about 333 logs. A copper axe has 190 durability. If the axe breaks mid-trunk, the rest of the trunk stays standing and the golem keeps its job. Foresters stop at night. See [Running costs](/golem/running-costs) and [Tools](/golem/tools).

## Limits and edge cases

- A tree with no open, solid spot beside its base is skipped.
- A stump is forgotten if the block is no longer air.
- A [sorter](/jobs/sorter) sharing the chest leaves 8 of every sapling and all spare copper axes.

## Advancements

| Advancement | How | ID |
|---|---|---|
| Odd Jobs | Give a golem a copper axe, a fishing rod, a brush or a crafting table | `bettercoppers:guide/odd_jobs` |
| Full Employment | Give golems all eight jobs | `bettercoppers:guide/full_employment` |

## Data values

| Name | Value |
|---|---|
| Job item | `minecraft:copper_axe` |
| Status name key | `job.bettercoppers.forester` |

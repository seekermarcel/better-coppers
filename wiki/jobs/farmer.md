---
title: Farmer
infobox:
  icon: copper_hoe
  rows:
    Job item: Copper hoe
    Needs home: "Yes"
    Running costs: Energy, rust, tool wear
    Work radius: 8 blocks, 3 up and down
    Mod: Better Coppers
---

# Farmer

A **farmer** is a [copper golem](/golem/copper-golem) that looks after every field around its [home](/start/homes-and-status) chest. It harvests ripe crops, replants them, and brings the harvest home. It never tills new land.

## Setup

1. Sneak + right-click a golem with a copper hoe.
2. Link the golem to a copper chest within 32 blocks of the golem.
3. Build the field: tilled farmland with water and seeds, within 8 blocks of the chest and no more than 3 blocks above or below it.
4. Optionally put seeds in the chest, or sneak + right-click the farmer with seeds to give it a whole stack.

<!-- MEDIA: jobs-farmer-field.png | screenshot | A farmer with a copper hoe in a wheat and carrot field next to its copper chest, with a row of sugar cane and a melon stem in view -->

## Behaviour

The field is a box around the home chest: 8 blocks to each side and 3 up and down, or 12, 16 or 24 blocks with iron, gold or diamond [eyes](/golem/eyes). The height stays 3. Only loaded chunks count. The golem always takes the nearest job in the box.

| Task | What the farmer does |
|---|---|
| Ripe crops: wheat, carrots, potatoes, beetroot, nether wart | Harvests and replants right away, using the seed from the drops |
| Pumpkin or melon attached to a stem | Picks it and leaves the stem |
| Sugar cane or bamboo, at least 3 high | Cuts everything above the bottom block |
| Empty farmland | Plants a crop from its supplies |
| Empty soul sand | Plants nether wart from its supplies |

Empty farmland gets what the neighbouring blocks grow if the farmer carries that seed, or else any crop seed it carries. It only plants on blocks with air above.

Each task takes 25 to 39 ticks (1.25 to 2 seconds), longer with rust. A short head shake sometimes comes first.

### Seeds and supplies

A farmer keeps 16 of every seed kind in its supplies. When unloading, seeds up to 16 of each kind stay with it. When restocking, it takes up to 16 of each seed kind found in the chest. If empty farmland is waiting and it carries no crop seeds while the chest holds some, it goes home to fetch them.

A **seed** is an item that plants a crop block or nether wart. Carrots and potatoes count. Pumpkin seeds, melon seeds and pitcher pods do not, so a farmer neither plants nor keeps them.

A farmer given seeds with sneak + right-click takes the whole held stack. A [sorter](/jobs/sorter) sharing the chest leaves 16 of every seed and all spare copper hoes.

### Bringing it home

The harvest goes into a chained [Copper Chest Minecart](/items/copper_chest_minecart) first, then the pouch. The golem walks home at 64 items, or when it has nothing else to do and carries something.

## Running costs

| Action | Energy | Tool wear |
|---|---|---|
| Harvesting a block | 3 | 1, only for pumpkins, melons and bamboo |
| Planting | 3 | None |

Ripe crops, nether wart and sugar cane break instantly, so they cost the hoe nothing. Harvesting and replanting a crop costs 6 energy. A full charge of 1000 energy is about 166 harvest-and-replant cycles. A copper hoe has 190 durability. Farmers stop at night. See [Running costs](/golem/running-costs) and [Tools](/golem/tools).

## Limits and edge cases

- A farmer never tills land, bone-meals crops or waters fields.
- A pumpkin or melon counts only if a stem attached to it faces it. Ones placed by hand are left alone.
- If it carries no seed for a harvested crop, no replant happens.
- Empty soul sand alone does not send the farmer home to fetch nether wart. It plants nether wart it already carries, or takes some on its next chest visit.
- A blocked crop is skipped for 30 seconds after 15 seconds without getting closer.

## Advancements

| Advancement | How | ID |
|---|---|---|
| Green Fingers | Give a golem a copper hoe | `bettercoppers:guide/farmer` |
| Full Employment | Give golems all eight jobs | `bettercoppers:guide/full_employment` |

## Data values

| Name | Value |
|---|---|
| Job item | `minecraft:copper_hoe` |
| Status name key | `job.bettercoppers.farmer` |

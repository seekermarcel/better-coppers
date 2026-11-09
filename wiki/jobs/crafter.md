---
title: Crafter
infobox:
  icon: crafting_table
  rows:
    Job item: Crafting table
    Needs home: "Yes"
    Running costs: Energy, rust
    Work radius: Item frames within 2 blocks of the home
    Mod: Better Coppers
---

# Crafter

A **crafter** is a [copper golem](/golem/copper-golem) that crafts whatever the item frames on or next to its [home](/start/homes-and-status) chest show, using only what is in that chest. It stops at a stack of the result.

## Setup

1. Sneak + right-click a golem with a crafting table.
2. Link the golem to a copper chest within 32 blocks of the golem.
3. Hang an item frame on or right next to the chest, showing what should be crafted.
4. Put the ingredients in the chest.

<!-- MEDIA: jobs-crafter-setup.png | screenshot | A crafter with a crafting table standing at its copper chest, with item frames on the chest showing sticks and torches and ingredients visible in the open chest -->

## Behaviour

The golem looks at every item frame with an item within 2 blocks of the home chest. For each, it crafts while the chest holds fewer than 64 of that item. The first frame it can craft for is done next. [Eyes](/golem/eyes) do not change this range.

It uses shaped and shapeless crafting recipes whose result is the framed item, and only ingredients from that chest. Recipes the game marks as special are not used. Furnace, smithing and stonecutter recipes are not used.

Each craft takes 80 ticks (4 seconds), longer with rust, and makes one recipe's output. The result and any leftover items, such as an empty bucket, go in the chest. If the chest is full, they stay in the golem's load and it stops crafting until there is room.

## Running costs

| Action | Energy | Tool wear |
|---|---|---|
| Per craft | 15 | None |

The crafting table never wears out. A full charge of 1000 energy is about 66 crafts. Crafters stop at night. See [Running costs](/golem/running-costs).

## Limits and edge cases

- If ingredients change while it crafts, the craft is cancelled and nothing is lost.
- A [sorter](/jobs/sorter) sharing the chest keeps only spare crafting tables, so it will carry the ingredients away. Give a crafter a chest of its own.
- With nothing to craft, the golem idles and looks again every 10 seconds.

## Advancements

| Advancement | How | ID |
|---|---|---|
| Odd Jobs | Give a golem a copper axe, a fishing rod, shears, a brush or a crafting table | `bettercoppers:guide/odd_jobs` |
| Full Employment | Give golems all nine jobs | `bettercoppers:guide/full_employment` |

## Data values

| Name | Value |
|---|---|
| Job item | `minecraft:crafting_table` |
| Status name key | `job.bettercoppers.crafter` |

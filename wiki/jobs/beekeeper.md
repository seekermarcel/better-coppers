---
title: Beekeeper
infobox:
  icon: shears
  rows:
    Job item: Shears
    Needs home: "Yes"
    Running costs: Energy, rust, tool wear
    Work radius: 10 blocks, 4 up and down
    Mod: Better Coppers
---

# Beekeeper

A **beekeeper** is a [copper golem](/golem/copper-golem) that takes the honey from full bee nests and beehives around its [home](/start/homes-and-status) chest and brings it back.

## Setup

1. Take off any flower or [trim](/golem/trims) first: sneak + right-click with shears removes those before it gives the job.
2. Sneak + right-click the golem with shears.
3. Link the golem to a copper chest within 32 blocks of the golem, close to the hives.
4. Light a [campfire](https://minecraft.wiki/w/Campfire) below each hive, at most 5 blocks under it.
5. Put glass bottles in the chest if you want honey bottles.

<!-- MEDIA: jobs-beekeeper-harvest.mp4 | clip | A beekeeper with shears standing at a beehive over a lit campfire, the hive emptying and the golem walking home to its copper chest -->

## Behaviour

The golem looks for hives in a box around the home chest: 10 blocks to each side and 4 up and down, or 15, 20 or 30 blocks with iron, gold or diamond [eyes](/golem/eyes). The height stays 4. It goes to the nearest hive that is full and smoked by a lit campfire.

| Rule | Value |
|---|---|
| With a glass bottle | 1 honey bottle per hive |
| Without a glass bottle | 3 honeycomb per hive |
| Harvest time | 25 ticks (1.25 seconds), longer with rust |

It uses glass bottles for as long as it has them. To get only honeycomb, keep bottles out of the chest. If it has none left and the chest holds some, it goes home to fetch them. On a chest visit it takes up to 16.

The bees stay inside and do not get angry. A hive with no smoke is left alone, wild nests included, until you light a campfire below it.

A [sorter](/jobs/sorter) sharing the chest leaves 16 glass bottles and all spare shears.

## Running costs

| Action | Energy | Tool wear |
|---|---|---|
| Honey bottle | 10 | 0 |
| Honeycomb | 10 | 1 |

A full charge of 1000 energy harvests 100 hives. Beekeepers stop at night. See [Running costs](/golem/running-costs) and [Tools](/golem/tools).

## Limits and edge cases

- The golem does not climb. A hive it cannot get within reach of, such as a nest high in a tree, is skipped for a while.
- Plain shears on a golem stay [vanilla](/start/vanilla-rule). Only sneak + right-click gives the job.

## Advancements

| Advancement | How | ID |
|---|---|---|
| Odd Jobs | Give a golem a copper axe, a fishing rod, shears, a brush or a crafting table | `bettercoppers:guide/odd_jobs` |
| Full Employment | Give golems all nine jobs | `bettercoppers:guide/full_employment` |

## Data values

| Name | Value |
|---|---|
| Job item | `minecraft:shears` |
| Status name key | `job.bettercoppers.beekeeper` |

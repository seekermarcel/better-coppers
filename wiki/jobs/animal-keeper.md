---
title: Animal keeper
infobox:
  icon: brush
  rows:
    Job item: Brush
    Needs home: "Yes"
    Running costs: Energy, rust, tool wear
    Work radius: 10 blocks, 4 up and down
    Mod: Better Coppers
---

# Animal keeper

An **animal keeper** is a [copper golem](/golem/copper-golem) that feeds the animals around its [home](/start/homes-and-status) chest, in pairs, so they breed. It does nothing else with them: it does not shear, milk, collect eggs or butcher.

## Setup

1. Sneak + right-click a golem with a brush.
2. Link the golem to a copper chest within 32 blocks of the golem, close to the animals.
3. Put food in the chest that the animals eat.

<!-- MEDIA: jobs-keeper-feeding.mp4 | clip | An animal keeper with a brush feeding two cows in a pen, hearts appearing, and a calf being born -->

## Behaviour

The golem counts every animal in a box around the home chest: 10 blocks to each side and 4 up and down, or 15, 20 or 30 blocks with iron, gold or diamond [eyes](/golem/eyes). The height stays 4. Animals are grouped by type.

| Rule | Value |
|---|---|
| Herd limit | No feeding once a herd has 16 or more animals of that type, babies included |
| Pairs | Needs 2 animals ready to be fed, or 1 ready and 1 already in love |
| Ready | Adult, not on breeding cooldown, not already in love |
| Feeding time | 25 ticks (1.25 seconds), longer with rust |

It feeds the nearest animal of a herd it has food for, then the breeding happens as in [vanilla](https://minecraft.wiki/w/Breeding). Any food the animal accepts works.

### Food

Food comes from the home chest and stays in the golem's supplies. On a chest visit it takes up to 16 of each kind of food that animals within its range plus 4 blocks will eat. A kind nothing around eats stays in the chest. If it runs out of food for a herd and the chest holds some, it goes home to fetch it.

A [sorter](/jobs/sorter) sharing the chest leaves 16 of every animal food and all spare brushes.

## Running costs

| Action | Energy | Tool wear |
|---|---|---|
| Per animal fed | 10 | 1 |

A full charge of 1000 energy feeds 100 animals. Animal keepers stop at night. See [Running costs](/golem/running-costs) and [Tools](/golem/tools).

## Limits and edge cases

- A lone animal is not fed. There is always a partner in love or a second ready animal.
- A keeper gathers nothing, so it has nothing to bring home.

## Advancements

| Advancement | How | ID |
|---|---|---|
| Odd Jobs | Give a golem a copper axe, a fishing rod, a brush or a crafting table | `bettercoppers:guide/odd_jobs` |
| Full Employment | Give golems all eight jobs | `bettercoppers:guide/full_employment` |

## Data values

| Name | Value |
|---|---|
| Job item | `minecraft:brush` |
| Status name key | `job.bettercoppers.keeper` |

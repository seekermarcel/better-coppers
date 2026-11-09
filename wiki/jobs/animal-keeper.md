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

An **animal keeper** is a [copper golem](/golem/copper-golem) that feeds the animals around its [home](/start/homes-and-status) chest, in pairs, so they breed. With the right items in that chest it also shears sheep and milks cows, goats and mooshrooms. It does not collect eggs or butcher.

## Setup

1. Sneak + right-click a golem with a brush.
2. Link the golem to a copper chest within 32 blocks of the golem, close to the animals.
3. Put food in the chest that the animals eat.
4. Optional: put shears, empty buckets or bowls in the chest for [shearing and milking](#shearing-and-milking).

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

A [sorter](/jobs/sorter) sharing the chest leaves 16 of every animal food, all spare brushes, all shears, 4 empty buckets and 4 bowls. Wool, milk and stew are sorted as usual.

### Shearing and milking

When no animal can be fed, the keeper shears sheep first and then milks. A sheared sheep can still be fed while its wool regrows. Babies are skipped.

| Animal | Needs | Gives |
|---|---|---|
| Sheep | Shears | Wool of the sheep's colour, as much as [vanilla](https://minecraft.wiki/w/Sheep) shearing |
| Cow or goat | Empty bucket | Milk bucket |
| Mooshroom | Bowl if it has one, otherwise empty bucket | Mushroom stew or milk bucket |

| Rule | Value |
|---|---|
| Time per action | 25 ticks (1.25 seconds), longer with rust |
| Milking limit | Each animal once every 5 minutes, milk and stew together |
| Carries | 1 pair of shears, 4 empty buckets, 4 bowls |

Everything comes from the home chest and is only taken when a matching animal is in range. If it runs out and an animal is waiting, it goes home for more. Without shears, buckets or bowls it just keeps breeding.

Wool goes straight into its pouch, so nothing lands on the ground. Stew is always plain mushroom stew, even from a brown mooshroom that was fed a flower. A mooshroom is never sheared, because that would turn it into a cow.

Shears lose 1 durability per sheep and are replaced from the home chest on its next visit. It does not search other chests for them.

## Running costs

| Action | Energy | Tool wear |
|---|---|---|
| Per animal fed | 10 | 1 |
| Per shear or milking | 10 | None on the brush |

A full charge of 1000 energy pays for 100 of these. Animal keepers stop at night. See [Running costs](/golem/running-costs) and [Tools](/golem/tools).

## Limits and edge cases

- A lone animal is not fed. There is always a partner in love or a second ready animal.
- Milk and stew do not stack, so each takes a chest slot. When the chest is full the keeper is [backed up](/golem/tools#full-or-missing-chest) and stops all its work, breeding too, until there is room.

## Advancements

| Advancement | How | ID |
|---|---|---|
| Odd Jobs | Give a golem a copper axe, a fishing rod, shears, a brush or a crafting table | `bettercoppers:guide/odd_jobs` |
| Full Employment | Give golems all nine jobs | `bettercoppers:guide/full_employment` |

## Data values

| Name | Value |
|---|---|
| Job item | `minecraft:brush` |
| Status name key | `job.bettercoppers.keeper` |

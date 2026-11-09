---
title: Fisher
infobox:
  icon: fishing_rod
  rows:
    Job item: Fishing rod
    Needs home: "Yes"
    Running costs: Energy, rust, tool wear
    Work radius: 10 blocks, 4 down and 2 up
    Mod: Better Coppers
---

# Fisher

A **fisher** is a [copper golem](/golem/copper-golem) that stands on the shore of open water near its [home](/start/homes-and-status) chest and fishes, slowly and patiently. It brings its catch home every few fish.

## Setup

1. Sneak + right-click a golem with a fishing rod.
2. Link the golem to a copper chest within 32 blocks of the golem, near open water.

<!-- MEDIA: jobs-fisher-shore.mp4 | clip | A fisher standing on a shore block casting its float, the float dipping under, and the catch arriving -->

## Behaviour

The golem looks for water in a box around the home chest: 10 blocks to each side, 4 down and 2 up, or 15, 20 or 30 blocks with iron, gold or diamond [eyes](/golem/eyes).

| Requirement | Detail |
|---|---|
| Open water | A water source block with air above it |
| Enough water | At least 6 water blocks in the 5x5 area around it, at the same level |
| Shore | A solid block next to the water with room for the golem, standing level with the water surface |

Of all spots, it picks the one closest to its home. It keeps that spot until it stops being valid.

The golem casts its **float** onto the water. It waits 400 to 999 ticks (20 to 50 seconds), longer with rust. The float dips under 20 ticks before the catch comes in, then the golem takes a 1 second pause.

The catch comes from the vanilla [fishing](https://minecraft.wiki/w/Fishing) table: fish and junk, never treasure. Lure and Luck of the Sea on the rod have no effect.

### Bringing it home

The catch goes into a chained [Copper Chest Minecart](/items/copper_chest_minecart) first, then the pouch. The golem walks home at 8 items, or when it has nothing else to do.

## Running costs

| Action | Energy | Tool wear |
|---|---|---|
| Per catch | 15 | 1 |

A full charge of 1000 energy is about 66 catches. Fishers stop at night. See [Running costs](/golem/running-costs) and [Tools](/golem/tools).

## Limits and edge cases

- With no valid spot, the golem brings home what it has and idles, looking again every 10 seconds.
- If a spot becomes invalid, the golem reels in and looks for a new one.
- A spot it gets no closer to for 15 seconds is skipped for 30 seconds.
- A [sorter](/jobs/sorter) sharing the chest leaves all spare fishing rods.

## Advancements

| Advancement | How | ID |
|---|---|---|
| Odd Jobs | Give a golem a copper axe, a fishing rod, shears, a brush or a crafting table | `bettercoppers:guide/odd_jobs` |
| Full Employment | Give golems all nine jobs | `bettercoppers:guide/full_employment` |

## Data values

| Name | Value |
|---|---|
| Job item | `minecraft:fishing_rod` |
| Status name key | `job.bettercoppers.fisher` |
| Float entity | `bettercoppers:golem_float`, not summonable and not saved |

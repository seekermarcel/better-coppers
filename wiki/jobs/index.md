---
title: Jobs
---

# Jobs

A **job** is work a player assigns to a [copper golem](/golem/copper-golem). Better Coppers adds nine jobs, and the [Automatisation add-on](/automatisation/) adds a tenth, the operator. Only a player can give or change a job. A golem without a job sorts exactly like in [vanilla](https://minecraft.wiki/w/Copper_Golem), even if it has a [home](/start/homes-and-status) (see [The vanilla rule](/start/vanilla-rule)).

## All jobs

| Job | Job item | Needs | Paying job | Work area | Mod |
|---|---|---|---|---|---|
| [Sorter](/jobs/sorter) | <Icon id="bundle" /> Any bundle | Home | No | Containers within 32 blocks of the golem, 8 up and down | Better Coppers |
| [Miner](/jobs/miner) | <Icon id="copper_pickaxe" /> Copper pickaxe, then <Icon id="mineshaft_plan" /> [Mineshaft Plan](/items/mineshaft_plan) | Home | Yes | The Mineshaft Plan's mine, 8 to 128 blocks long | Better Coppers |
| [Farmer](/jobs/farmer) | <Icon id="copper_hoe" /> Copper hoe | Home | Yes | 8 blocks around the home, 3 up and down | Better Coppers |
| [Forester](/jobs/forester) | <Icon id="copper_axe" /> Copper axe | Home | Yes | 12 blocks around the home, 6 up and down | Better Coppers |
| [Fisher](/jobs/fisher) | <Icon id="fishing_rod" /> Fishing rod | Home | Yes | 10 blocks around the home, 4 down and 2 up | Better Coppers |
| [Animal keeper](/jobs/animal-keeper) | <Icon id="brush" /> Brush | Home | Yes | 10 blocks around the home, 4 up and down | Better Coppers |
| [Beekeeper](/jobs/beekeeper) | <Icon id="shears" /> Shears | Home | Yes | 10 blocks around the home, 4 up and down | Better Coppers |
| [Crafter](/jobs/crafter) | <Icon id="crafting_table" /> Crafting table | Home | Yes | Item frames within 2 blocks of the home | Better Coppers |
| [Courier](/jobs/courier) | <Icon id="chest" /> None: a route between two chests | Route, no home | No | Anywhere in the same dimension | Better Coppers |
| [Operator](/jobs/operator) | <Icon id="copper_wrench" /> [Copper Wrench](/items/copper_wrench) | A machine as its workplace, no home | Yes | One machine, spare parts within 8 blocks of it | Automatisation add-on |

A **paying job** has [running costs](/golem/running-costs): energy, rust and tool wear. Sorters and couriers have none, so they also work at night. A golem with a paying job stops at night and when it is dark in a thunderstorm.

## Giving a job

Everything given to a golem takes sneak + right-click (see [Controls](/start/controls)).

1. Sneak + right-click the golem with the job item. A courier has no job item and takes a [route](/jobs/courier#setup) instead.
2. Link the golem to its home or workplace: sneak + right-click it with an empty hand, then right-click the copper chest or machine, within 32 blocks of the golem.

Pickaxes, axes and hoes must be copper. Others are refused with a message. An axe only becomes a job item on a clean, unwaxed golem. On a rusty or waxed golem any axe scrapes it instead. See [Tools](/golem/tools).

Giving a golem a different job item ends its old job first. The player gets back the old job item, the Mineshaft Plan, the pouch and the supplies, and what does not fit lands at the player's feet. A chained [Copper Chest Minecart](/items/copper_chest_minecart) stays chained. A new tool for the same job only swaps the tools.

A golem with a job takes no route, except a courier. Other jobs have to be ended first. If a golem somehow qualifies for two jobs, the first match wins, in this order: route, add-on job, job tool, bundle. A courier carrying a hoe is a courier.

## Ending a job

Sneak + right-click the golem twice with an empty hand, within 5 seconds. The player gets its tool or bundle, its Mineshaft Plan and everything it carries. A chained Copper Chest Minecart unchains and stays where it is, still loaded. Home, [eyes](/golem/eyes) and [trim](/golem/trims) stay with the golem, as do its energy and rust.

## What the paying jobs share

The farmer, forester, fisher, animal keeper, beekeeper and crafter follow one routine. The miner works the same way, with the differences on [its page](/jobs/miner).

| Thing | Value |
|---|---|
| Pouch size before walking home | 64 items, 8 for the fisher |
| With a chained Copper Chest Minecart | Goes home once the cart has no empty slot and the pouch is full |
| Chest visit | 60 ticks (3 seconds) |
| Pause after a task | 5 ticks (0.25 seconds) |
| Head shake | 8% chance per task, for 30 to 59 ticks (1.5 to 3 seconds) |
| Idle when there is no work | 10 seconds, then it looks again |
| Unreachable target | Given up after 15 seconds without getting closer, then skipped for 30 seconds |
| Walking speed | 0.8, the miner 0.9, times the rust factor |

Rust slows work and walking: exposed golems run at 85%, weathered ones at 70%. A fully oxidized or waxed golem does not work.

A full chest loses nothing: what does not fit stays in the load, and the golem is [backed up](/golem/tools#full-or-missing-chest). It stops working and waits at home until something it carries fits. A golem whose home chest is broken or unloaded stops working and waits with everything it carries. Items drop only when the golem dies or turns into a statue. A leashed golem does not work.

Every paying job looks for a new tool when its tool breaks, except that an operator's Copper Wrench and a crafter's crafting table never wear out. See [Tools](/golem/tools).

## Eyes and work radius

[Eyes](/golem/eyes) multiply the horizontal work radius. Heights stay the same. Couriers refuse eyes, and crafters have no radius to widen.

| Job | No eyes | Iron eyes (1.5x) | Gold eyes (2x) | Diamond eyes (3x) |
|---|---|---|---|---|
| Sorter | 32 | 48 | 64 | 96 |
| Farmer | 8 | 12 | 16 | 24 |
| Forester | 12 | 18 | 24 | 36 |
| Fisher | 10 | 15 | 20 | 30 |
| Animal keeper | 10 | 15 | 20 | 30 |
| Beekeeper | 10 | 15 | 20 | 30 |
| Miner (ores per vein) | 16 | 24 | 32 | 48 |
| Operator (spare parts) | 8 | 12 | 16 | 24 |

## Advancements

Each job step of the Better Coppers [guide](/reference/advancements) is earned by giving a golem that job. Giving golems all nine base jobs earns *Full Employment*.

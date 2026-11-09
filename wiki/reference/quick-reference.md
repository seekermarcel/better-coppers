---
title: Quick reference
---

# Quick reference

The **quick reference** is a one-page cheat sheet of the job items, controls and key numbers of Better Coppers and its Automatisation add-on. The explanation for each entry is on the linked pages.

## Job items

Each job item is handed over with sneak + right-click. A golem has one job at a time; a new job item ends the previous job and the player gets back what the golem carried for it.

| Job item | Job | Place | Running costs |
|---|---|---|---|
| <Icon id="bundle" /> Any bundle | [Sorter](/jobs/sorter) | Home | None |
| <Icon id="copper_pickaxe" /> Copper pickaxe and <Icon id="mineshaft_plan" /> marked [Mineshaft Plan](/items/mineshaft_plan) | [Miner](/jobs/miner) | Home | Yes |
| <Icon id="copper_hoe" /> Copper hoe | [Farmer](/jobs/farmer) | Home | Yes |
| <Icon id="copper_axe" /> Copper axe, on a golem with no rust and no wax | [Forester](/jobs/forester) | Home | Yes |
| <Icon id="fishing_rod" /> Fishing rod | [Fisher](/jobs/fisher) | Home | Yes |
| <Icon id="brush" /> Brush | [Animal keeper](/jobs/animal-keeper) | Home | Yes |
| <Icon id="shears" /> Shears, on a golem with no flower and no trim | [Beekeeper](/jobs/beekeeper) | Home | Yes |
| <Icon id="crafting_table" /> Crafting table | [Crafter](/jobs/crafter) | Home | Yes |
| <Icon id="chest" /> A route between two containers (no item) | [Courier](/jobs/courier) | Route | None |
| <Icon id="copper_wrench" /> [Copper Wrench](/items/copper_wrench) (add-on) | [Operator](/jobs/operator) | Workplace (a machine) | Yes |

Golems only take copper tools. A sneak + right-click with any other pickaxe, hoe or axe is refused, except that an axe still scrapes a rusty or waxed golem.

## Controls

"Golem" means a copper golem; see [Controls](/start/controls) and [The vanilla rule](/start/vanilla-rule) for the full rules.

### On a golem

| Action | Effect |
|---|---|
| Sneak + right-click with a job item | Gives the job |
| Sneak + right-click with seeds, on a farmer | Adds them to its supplies |
| Sneak + right-click with an iron, gold or diamond block | Gives [eyes](/golem/eyes); couriers refuse them |
| Sneak + right-click with a smithing template, then a trim material | Puts on an [armour trim](/golem/trims) |
| Sneak + right-click with shears | Takes the trim off; with no trim, gives the [beekeeper](/jobs/beekeeper) job |
| Sneak + right-click with an [upgrade](/automatisation/upgrades) (add-on) | Fits the upgrade |
| Sneak + right-click with an empty hand | Shows the status, makes the golem glow and starts a link |
| The same again within 5 seconds, on a golem with a job | Ends the job |
| The same again, on a golem without a job | Cancels the link |
| Right-click with an empty hand, on a golem with a job | Shows the status and cancels a link started on it |
| Right-click with an empty hand, on a golem without a job | Exactly [vanilla](https://minecraft.wiki/w/Copper_Golem) |
| Right-click with an empty hand while leading a chained [Copper Chest Minecart](/items/copper_chest_minecart) | Hands the cart to the golem |
| Hold sneak | Copper particle lines from golems within 24 blocks to their homes |

### After starting a link

| Action | Effect |
|---|---|
| Right-click a copper chest within 32 blocks of the golem | Sets the [home](/start/homes-and-status) |
| Right-click a chest or barrel, or sneak + right-click a copper chest | Starts a courier route (golem without a job, or a courier) |
| Then right-click any chest, copper chest or barrel | Ends the route; any distance in the same dimension |
| Right-click a machine within 32 blocks of the golem (add-on) | Sets an operator's workplace |

### On other things

| Action | Effect |
|---|---|
| Right-click a Copper Chest Minecart with an iron or copper chain | Chains it to the player |
| Sneak + right-click a chained Copper Chest Minecart | Takes the chain off |
| Right-click a Copper Chest Minecart with a [Cart Extension](/items/cart_extension) (add-on) | Makes it a double chest |
| Axe in a dispenser facing a golem | Scrapes it, even a golem no player touched |
| Right-click a [Saw](/items/saw) with an item (add-on) | Sets its filter; an empty hand clears it |

## Key numbers

### Time limits

| What | Time |
|---|---|
| Time to choose a block after starting a link | 30 seconds |
| Time to reach the route's second container | 10 minutes |
| Window to end a job with a second press | 5 seconds |

### Work radius and eyes

Radii are measured in blocks. [Eyes](/golem/eyes) multiply them; heights are not changed.

| Job | What the radius covers | Height | No eyes | Iron (1.5x) | Gold (2x) | Diamond (3x) |
|---|---|---|---|---|---|---|
| Sorter | Containers it knows | 8 up or down | 32 | 48 | 64 | 96 |
| Farmer | Field around the home | 3 up or down | 8 | 12 | 16 | 24 |
| Forester | Forest around the home | 6 up or down | 12 | 18 | 24 | 36 |
| Fisher | Water around the home | 4 down, 2 up | 10 | 15 | 20 | 30 |
| Animal keeper | Animals around the home | 4 up or down | 10 | 15 | 20 | 30 |
| Beekeeper | Hives around the home | 4 up or down | 10 | 15 | 20 | 30 |
| Miner | Ore blocks followed per vein (a count, not blocks of distance) | n/a | 16 | 24 | 32 | 48 |
| Operator | Chests holding spare parts, around the machine | n/a | 8 | 12 | 16 | 24 |

### Jobs

| What | Value |
|---|---|
| Items carried per trip by a sorter or courier | 16 ([Copper Satchel](/items/copper_satchel): 32) |
| Pouch size before walking home | 64 items (fisher: 8) |
| Mineshaft Plan length | 8 to 128 blocks, default 32 |
| Mineshaft support spacing | 4 blocks |
| Fisher: time per catch | 20 to 50 seconds |
| Animal keeper: herd limit | 16 per kind |
| Crafter: item frames counted | Within 2 blocks of the home |
| Idle stroll | Walks back when more than 6 blocks from home |

### Energy and rust

See [Running costs](/golem/running-costs). Sorters and couriers have none.

| What | Value |
|---|---|
| Full energy | 1000 |
| Search range for a charging block | 16 blocks around the place, 6 up or down |
| [Charging Pad](/items/charging_pad) | 8 energy per redstone pulse; pulses less than 3 ticks apart count once |
| Full charge on a Charging Pad | About 2 minutes with a 1-second clock, about 25 seconds with the fastest (4-tick) clock |
| [Charging Station](/items/charging_station) (add-on) | 4 energy per tick, no redstone clock |
| Energy per farming action | 3 |
| Energy per log felled | 3 |
| Energy per block dug by a miner / block placed | 2 / 1 |
| Energy per catch (fisher) | 15 |
| Energy per feed (animal keeper) | 10 |
| Energy per harvest (beekeeper) | 10 |
| Energy per craft (crafter) | 15 |
| Energy per second of cranking (operator) | 1 |
| Energy spent per rust stage | 3000 |
| Speed by rust stage | Unaffected 100%, exposed 85%, weathered 70%, oxidized: does not work |
| Waxed golem | Does not work |

### Guards and repairs

See [Guards and repairs](/golem/guards-and-repairs).

| What | Value |
|---|---|
| Iron golem defends copper golems with a job within | 16 blocks |
| Iron ingot heals an iron golem by | 25 health |
| Iron golem must be missing at least | 30 health |
| Ingots per repair trip | 4 at most |

A lost [Golem Handbook](/items/golem_handbook) can be crafted again from a book and a copper ingot.

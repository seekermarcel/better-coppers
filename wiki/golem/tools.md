---
title: Tools
---

# Tools

A golem's **job tool** is the [job item](/jobs/) in its hand that defines its [job](/jobs/). Golems work only with copper tools, and those take **tool wear** and eventually break. A bundle or crafting table is a job item but does not wear.

## Copper only

| Job item | Job |
|---|---|
| Copper pickaxe | [Miner](/jobs/miner) |
| Copper hoe | [Farmer](/jobs/farmer) |
| Copper axe | [Forester](/jobs/forester) |
| Fishing rod | [Fisher](/jobs/fisher) |
| Brush | [Animal keeper](/jobs/animal-keeper) |
| Crafting table | [Crafter](/jobs/crafter) |
| [Copper Wrench](/items/copper_wrench) | [Operator](/jobs/operator) (add-on) |

Any other pickaxe, axe or hoe is refused when given with sneak + right-click: the golem plays its "no" sound and the player reads "Copper golems only work with copper tools." The exception is an axe used on a rusty or waxed golem, which [scrapes](/golem/running-costs#scraping) it instead.

Fishing rods, brushes and crafting tables have no copper version and work as they are.

## Tool wear

Each use costs 1 point of [durability](https://minecraft.wiki/w/Item_durability).

| Job | Wears the tool when |
|---|---|
| Miner | Digging a block that is not instantly broken |
| Farmer | Harvesting a block that is not instantly broken: pumpkins, melons and bamboo. Instantly broken crops such as wheat and sugar cane cost no wear. |
| Forester | Felling a log |
| Fisher | Each catch |
| Animal keeper | Feeding an animal |
| Crafter | Never |
| Operator | Never |

Sorters and couriers have no tool. A sorter's bundle does not wear.

## Lost tools

A **lost tool** is a job tool that broke in a golem's hand. The golem keeps its job. It remembers which tool broke, and its status shows "looking for a new" that tool.

A golem that wants a replacement must:

- have a [home](/start/homes-and-status),
- be able to [work](/golem/running-costs) (daytime, energy, not oxidized or waxed),
- have nothing left to bring home, so the load goes home first.

Only miners, farmers, foresters, fishers and animal keepers fetch replacements. Sorters, couriers, crafters and operators have nothing that breaks.

### Fetching a replacement

| Number | Value |
|---|---|
| Searched first | The home chest |
| Then | The nearest chest or barrel within 16 blocks horizontally and 6 blocks vertically of home |
| Takes | One item of the same tool type |
| Walking speed | Rust slows it, as for all walking |
| Gives up walking after | 15 seconds (300 ticks) without getting closer |
| Retry when nothing is found | Every 30 seconds (600 ticks), with a head shake and smoke |

A locked chest is not searched. Giving the golem a new tool by hand also ends the search. To end its job instead, sneak + right-click it twice with an empty hand, within 5 seconds.

Keep spares nearby: a [sorter](/jobs/sorter) sharing a chest with a golem leaves [working stock](/jobs/sorter) behind, including spare tools of its job.

## Full or missing chest

A golem never loses items because of a chest:

| Situation | What happens |
|---|---|
| Chest full | The golem keeps what does not fit and tries again about every 10 seconds (200 ticks) |
| Chest broken or its chunk not loaded | The golem stops working and waits with everything it carries, until the chest is back or a new home is linked |

Its items only drop when it [dies or turns into a statue](/golem/monsters#death). The home must still be a copper chest; a chest of another kind does not count as a home.

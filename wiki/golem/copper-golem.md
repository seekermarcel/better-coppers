---
title: Copper Golem
infobox:
  icon: copper_golem_spawn_egg
  rows:
    Type: Mob
    Mod: Vanilla, extended by Better Coppers
    Health: 12 (6 hearts)
    Movement speed: 0.2
    ID: "minecraft:copper_golem"
---

# Copper Golem

A **copper golem** is a vanilla mob that carries items between chests. Better Coppers lets a player give it a [job](/jobs/), a home, eyes and a trim. A golem no player has touched stays exactly as described on the [Minecraft Wiki](https://minecraft.wiki/w/Copper_Golem); see [the vanilla rule](/start/vanilla-rule).

## Obtaining

### Building

A copper golem is built by placing a carved pumpkin on a copper block, as in vanilla. The block can be any weathering stage, waxed or not. The golem takes the weathering stage of the block, and the block turns into a copper chest of the same kind, which can serve as the golem's [home](/start/homes-and-status).

<!-- MEDIA: golem-build.png | screenshot | A freshly built copper golem standing beside the copper chest its copper block turned into, with a carved pumpkin in the player's hotbar -->

### Golem Assembler

With the [Automatisation](/automatisation/) add-on, an [operator](/jobs/operator) can build new golems in a [Golem Assembler](/items/golem_assembler). They start with no job.

## Usage

A golem is used by handing it a [job item](/start/first-golem) with sneak + right-click and [linking](/start/homes-and-status) it to a copper chest. Every click is listed under [Controls](/start/controls). The golem's work is described on the page of each [job](/jobs/).

## Behaviour

### Vanilla behaviour

A jobless golem takes items from copper chests and carries them to other chests, as in vanilla. This continues if the golem has a home. See [the vanilla rule](/start/vanilla-rule) for what the mod leaves alone.

A golem with a job does not do vanilla sorting. Its job decides what it does.

### Weathering

A copper golem weathers through four stages like a copper block, as in vanilla. Each step takes 504,000 to 552,000 ticks (7 to about 7.7 hours). Waxing with honeycomb stops weathering; an axe removes the wax or one stage. Lightning also removes a stage. All of this is vanilla.

| Stage | Status text | Speed of a [paying job](/golem/running-costs) | Can do paying work |
|---|---|---|---|
| Unaffected | `none` | 100 % | Yes |
| Exposed | `a little` | 85 % | Yes |
| Weathered | `a lot` | 70 % | Yes |
| Oxidized | `fully oxidized (cannot work)` | Does not work | No |
| Waxed, any stage | `waxed (cannot work)` | Does not work | No |

The mod adds **rust**: a golem with a paying job also goes one stage further for every 3,000 energy it spends, which is about three days of steady work. See [Running costs](/golem/running-costs). A golem that is scraped or waxed keeps its job. A waxed sorter or courier keeps working, as those jobs have no running costs.

### Statue

An oxidized golem that is not waxed turns into a copper golem statue, as in vanilla. Each tick it has a 0.58 % chance, which averages about 9 seconds, as long as it stands in an air block. A waxed golem never turns into a statue. A golem that reaches the oxidized stage through [rust](/golem/running-costs) from work is at the same risk.

Better Coppers drops the golem's job items just before this happens. The items that drop are the same as on death: see [Death](#death).

A statue keeps only the golem's name. A golem brought back from a statue is a new, jobless golem, with no home, route, eyes, trim, energy or rust progress. Scrape or wax a golem that is close to oxidizing to avoid this.

### Death

When a golem dies or becomes a statue, it drops:

| Item | Condition |
|---|---|
| Job tool or bundle | Held in its hands |
| [Mineshaft Plan](/items/mineshaft_plan) | Miner, progress included |
| Pouch and supplies | Paying job |
| [Eye block](/golem/eyes) | Has eyes |
| Trim template | Has a [trim](/golem/trims); the material is not returned |
| Iron ingots | Held for a [repair](/golem/guards-and-repairs) |
| [Copper Satchel](/items/copper_satchel) and spare part | Add-on only |

A chained [Copper Chest Minecart](/items/copper_chest_minecart) stays where it is and keeps its items. A golem's normal drops are vanilla.

### What the mod adds

| Addition | Page |
|---|---|
| Jobs: sorter, miner, farmer, forester, fisher, animal keeper, beekeeper, crafter, courier, operator | [Jobs](/jobs/) |
| A home and a status message | [Homes and status](/start/homes-and-status) |
| Energy, rust, tool wear and charging, for paying jobs | [Running costs](/golem/running-costs) |
| Copper-only tools and the search for a replacement | [Tools](/golem/tools) |
| Eyes that widen the work radius | [Eyes](/golem/eyes) |
| Armour trims, for looks | [Armour trims](/golem/trims) |
| Towing a Copper Chest Minecart on a chain | [Copper Chest Minecart](/items/copper_chest_minecart) |
| Riding minecarts and boats on long trips | [Minecarts and boats](/golem/riding) |
| Being hunted by monsters, when it has a job | [Monsters](/golem/monsters) |
| Guarding by iron golems, and repairing them | [Guards and repairs](/golem/guards-and-repairs) |
| Plating, coating, gearwork and a satchel, with the add-on | [Golem upgrades](/automatisation/upgrades) |

## Advancements

| Advancement | Requirement |
|---|---|
| A Little Helper | Build a copper golem: a carved pumpkin on a copper block |
| Home Sweet Chest | Link a golem to a copper chest |
| Good as New | Scrape a rusty golem with an axe in a dispenser |
| Eagle Eyes | Give a golem a block of iron, gold or diamond |
| Dressed to Impress | Give a golem an armour trim |
| Bodyguard | Watch an iron golem defend a copper golem |

See [Advancements](/reference/advancements) for the full list.

## Data values

| Name | Value |
|---|---|
| Entity ID | `minecraft:copper_golem` |
| Spawn egg ID | `minecraft:copper_golem_spawn_egg` |
| Translation key | `entity.minecraft.copper_golem` |

Better Coppers stores its per-golem data as Fabric attachments, in the entity's NBT under the key `fabric:attachments`.

### Saved attachments

An attachment that is absent means the golem does not have that thing.

| Attachment | Format | Meaning |
|---|---|---|
| `bettercoppers:home` | Int array `[x, y, z]` | Home copper chest |
| `bettercoppers:route` | Compound: `from`, `to`, each `[x, y, z]` | Courier route |
| `bettercoppers:worker` | Compound: optional `marks`, a list of `[x, y, z]` | Marks a farmer, forester, fisher, animal keeper, beekeeper or crafter. `marks` are a forester's stumps waiting for a sapling. |
| `bettercoppers:miner` | Compound: optional `plan` | Marks a miner. `plan` has `pattern` (`mineshaft`), `length` (8 to 128), optional `entrance` `[x, y, z]`, optional `heading` (a horizontal direction such as `north`) and `progress`. |
| `bettercoppers:load` | Compound: optional `pouch`, `supplies`, item stack lists | What it carries for a paying job |
| `bettercoppers:running_costs` | Compound: `energy` 0 to 1000 (default 1000), `rust_progress` (default 0), `recharging` boolean | Energy and progress towards the next rust stage, which resets at 3000 |
| `bettercoppers:lost_tool` | Item ID | A tool that broke, while the golem has no new one |
| `bettercoppers:eyes` | Integer: `1` iron, `2` gold, `3` diamond | Eyes. Synced to clients. |
| `bettercoppers:trim` | Compound: `material`, `pattern` | The trim worn. Synced to clients. |
| `bettercoppers:trim_template` | Item stack | Template returned when the trim comes off |
| `bettercoppers:carried_iron` | Integer | Iron ingots held for a repair |
| `bettercoppers:excited` | Boolean | A miner that found diamonds and is hurrying home with its antenna blinking. Synced to clients. |

The Automatisation add-on adds these:

| Attachment | Format | Meaning |
|---|---|---|
| `bettercoppers_automatisation:operator` | Boolean | Marks a player-made operator |
| `bettercoppers_automatisation:machine` | Int array `[x, y, z]` | Workplace machine |
| `bettercoppers_automatisation:upgrades` | Compound: `plating`, `coating`, `gearwork` integers (default 0), `satchel` boolean (default false) | Fitted upgrades, in damage or energy left |
| `bettercoppers_automatisation:carried_part` | Item stack | Spare part fetched for a repair |

These are not saved: a picked golem, a pending route, and whether a golem is on an errand. A server restart forgets them.

### How a job is stored

There is no job field. The job comes from what the golem holds and its attachments, and the first match wins.

| Priority | Job | Needs |
|---|---|---|
| 1 | Courier | `route` |
| 2 | Operator (add-on) | `operator` and a Copper Wrench in the main hand |
| 3 | Miner | `miner`, and a copper pickaxe in the main hand or as `lost_tool` |
| 4 | Farmer, forester, fisher, animal keeper, beekeeper, crafter | `worker`, and the job's tool in the main hand or as `lost_tool` |
| 5 | Sorter | A bundle in the off hand |

So a mapmaker summoning a golem with a job must set both the equipment and the attachment. A tool alone does not give a job, except the bundle, which needs nothing else.

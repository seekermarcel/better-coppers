---
title: Version history
---

# Version history

The **version history** lists the released versions of Better Coppers and its Automatisation add-on. Both mods share one version number. Better Coppers supports only the latest Minecraft version for now, so each release targets one game version (ADR 0005 in the repository's `docs/adr/` folder). Support for several versions at once is planned for later.

| Version | Date | Minecraft | Fabric Loader | Java |
|---|---|---|---|---|
| v0.2.0 | 2026-11-09 | 26.3 | 0.20.1 or newer | 25 or newer |
| v0.1.0 | 2026-11-08 | 26.3 | 0.20.1 or newer | 25 or newer |

## Unreleased

### Better Coppers

New:

- Item frames showing a grindstone, smithing table, enchanting table, armor stand or brewing stand reserve a container for all weapons, tools, enchanted books, armour or potions. An ender chest frame takes anything that does not stack. See [reserved chests](/jobs/sorter#reserved-chests).
- [Courier](/jobs/courier) routes take [stops](/jobs/courier#stops): sneak + right-click the ground with an empty hand while setting a route. The courier walks past them in turn, so it finds its way through tunnels, round mountains and out of walled bases.
- A [sorter](/jobs/sorter) without a cart carries up to 16 unstackables of the same kind in one trip, and a cart carries a whole group of them, such as all weapons.

Changed:

- A stuck [courier](/jobs/courier#getting-stuck) walks back along its way, further each time, instead of to a random spot nearby, and its status says where it keeps getting stuck.
- A sorter tops up a barrel that already holds the item before using other barrels.

Fixed:

- [Couriers](/jobs/courier) no longer get stuck where the way to the target first leads away from it, such as a tunnel that turns back.

## v0.2.0

Worlds from v0.1.0 load as they are. As before, the add-on needs the base mod of exactly the same version.

### Better Coppers

New:

- The [beekeeper](/jobs/beekeeper) job: shears make a golem take honey from smoked hives.
- [Animal keepers](/jobs/animal-keeper) shear sheep and milk cows, goats and mooshrooms, and get stew from mooshrooms, if their home chest holds shears, empty buckets or bowls.
- Four new Mine Patterns for the [miner](/jobs/miner), chosen at the [Survey Table](/items/survey_table): Staircase, Strip Mine, Room and Pillar, and Multilevel Mine.
- The [Quota Slip](/items/quota_slip) keeps a set amount of an item in a chest.
- [Farmers](/jobs/farmer) also harvest cocoa, sweet berries, glow berries, torchflowers, cactus and mushrooms, and take spent chorus plants and plant a new flower.

Changed:

- A golem whose home chest is full is [backed up](/golem/tools#full-or-missing-chest): it stops working, waits at home and goes back to work by itself once there is room. Its status message says why.
- A miner with no filler at water or lava gives up the rest of that Mine Section and carries on with the rails. The plan's tooltip and the status line show where it stopped.

Fixed:

- Farmers plant pumpkin seeds, melon seeds and pitcher pods.
- Foresters replant every stump with the species that grew there.
- Miners dig diamond, gold, redstone and emerald ore.

### Automatisation

- [Gold Gearwork](/items/gold_gearwork) makes an operator's machine do 1.3 times the work. Belt drives stay as they are.
- Sorters, couriers and golems without a job refuse Gold Gearwork, and the player keeps the item.

## v0.1.0

The first release. Both mods need [Fabric API](https://modrinth.com/mod/fabric-api). The add-on needs the base mod of exactly the same version. See [Installation](/start/installation).

### Better Coppers

- Jobs for copper golems: [sorter](/jobs/sorter), [miner](/jobs/miner), [farmer](/jobs/farmer), [forester](/jobs/forester), [fisher](/jobs/fisher), [animal keeper](/jobs/animal-keeper), [crafter](/jobs/crafter) and [courier](/jobs/courier).
- [Homes](/start/homes-and-status), courier routes and the status message.
- [Minecart and boat riding](/golem/riding) and the towed [Copper Chest Minecart](/items/copper_chest_minecart).
- [Monsters](/golem/monsters) that hunt golems with a job, iron golem [guards and repairs](/golem/guards-and-repairs).
- [Running costs](/golem/running-costs) and the [Charging Pad](/items/charging_pad), [tools](/golem/tools) that wear out and are fetched again.
- [Eyes](/golem/eyes) that widen the work radius, and [armour trims](/golem/trims).
- The [Survey Table](/items/survey_table) and [Mineshaft Plan](/items/mineshaft_plan).
- The [Golem Handbook](/items/golem_handbook) and the guide [advancements](/reference/advancements), in English and German.

### Automatisation

- The [operator](/jobs/operator) job and golem-[cranked machines](/automatisation/machines) with [wear and jams](/automatisation/wear-and-jams): [Belt Drive](/items/belt_drive), [Conveyor Belt](/items/conveyor_belt), [Funnel](/items/funnel), [Chute](/items/chute), [Press](/items/press), [Saw](/items/saw), [Grabber Arm](/items/grabber_arm) and [Golem Assembler](/items/golem_assembler).
- [Golem upgrades](/automatisation/upgrades): [Iron Plating](/items/iron_plating), [Copper Coating](/items/copper_coating), [Gold Gearwork](/items/gold_gearwork) and [Copper Satchel](/items/copper_satchel).
- The [Cart Extension](/items/cart_extension) and the [Charging Station](/items/charging_station).

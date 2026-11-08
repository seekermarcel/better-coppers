---
title: Automatisation
---

# Automatisation

**Better Coppers Automatisation** is an add-on for Better Coppers that adds a factory run by copper golems: [conveyor belts](/items/conveyor_belt), [funnels](/items/funnel), [chutes](/items/chute), [presses](/items/press), [saws](/items/saw), [grabber arms](/items/grabber_arm) and a [Golem Assembler](/items/golem_assembler). Its products go back into the golems as [upgrades](/automatisation/upgrades), job supplies and new golems.

<!-- MEDIA: automatisation-overview.png | screenshot | A working factory line from above at daytime: a chest, a pushing funnel, a row of conveyor belts with a press above one of them, a belt drive beside the line, two copper golems holding wrenches next to their machines -->

## Installation

The add-on is a separate jar. It needs the same version of [Better Coppers](/start/installation), Minecraft 26.3, Java 25 and Fabric API. Its mod ID is `bettercoppers_automatisation`.

The add-on adds seven chapters to the [Golem Handbook](/items/golem_handbook) and its own [advancement](/reference/advancements) tab. Its items appear in the Better Coppers creative tab.

## No power network

The add-on has no power network, no water wheels and no rotation to route. Every [machine](/automatisation/machines) has a crank, and a copper golem has to turn it. A machine only moves while an [operator](/jobs/operator) turns its crank, and stops within half a second when the operator walks off, runs out of [energy](/golem/running-costs) or rests at night.

Machines [wear out](/automatisation/wear-and-jams). A worn-out machine jams until its operator repairs it with a sheet from a container nearby, so every factory also has to feed itself.

## What the add-on adds

| Block | Needs an operator | What it does |
|---|---|---|
| <Icon id="belt_drive" /> [Belt Drive](/items/belt_drive) | Yes | Runs every belt line it touches, up to 64 belts |
| <Icon id="conveyor_belt" /> [Conveyor Belt](/items/conveyor_belt) | Through its Belt Drive | Carries items, mobs and players |
| <Icon id="funnel" /> [Funnel](/items/funnel) | No | Pulls items into the container behind it, or pushes stacks out |
| <Icon id="chute" /> [Chute](/items/chute) | No | Drops items straight down, a whole stack at a time |
| <Icon id="press" /> [Press](/items/press) | Yes | Ingots to sheets, sugar cane to paper |
| <Icon id="saw" /> [Saw](/items/saw) | Yes | Sheets to gears, logs to planks, and more with a filter |
| <Icon id="grabber_arm" /> [Grabber Arm](/items/grabber_arm) | Yes | Swings items between marked spots |
| <Icon id="golem_assembler" /> [Golem Assembler](/items/golem_assembler) | Yes | Builds new copper golems |
| <Icon id="charging_station" /> [Charging Station](/items/charging_station) | No | Recharges a golem with no redstone clock |

| Item | What it is for |
|---|---|
| <Icon id="copper_wrench" /> [Copper Wrench](/items/copper_wrench) | Job item of the [operator](/jobs/operator); sets up funnels and grabber arms |
| <Icon id="copper_sheet" /> [Copper](/items/copper_sheet), <Icon id="iron_sheet" /> [Iron](/items/iron_sheet) and <Icon id="gold_sheet" /> [Gold Sheet](/items/gold_sheet) | Made by the Press; recipes and repairs |
| <Icon id="copper_gear" /> [Copper](/items/copper_gear) and <Icon id="gold_gear" /> [Gold Gear](/items/gold_gear) | Made by the Saw; recipes and new golems |
| <Icon id="iron_plating" /> [Iron Plating](/items/iron_plating) | Upgrade: takes three quarters of each hit |
| <Icon id="copper_coating" /> [Copper Coating](/items/copper_coating) | Upgrade: no rust from work for a while |
| <Icon id="gold_gearwork" /> [Gold Gearwork](/items/gold_gearwork) | Upgrade: 30% faster for a while |
| <Icon id="copper_satchel" /> [Copper Satchel](/items/copper_satchel) | Upgrade: sorters and couriers carry twice as much |
| <Icon id="cart_extension" /> [Cart Extension](/items/cart_extension) | Makes a [Copper Chest Minecart](/items/copper_chest_minecart) a double chest |

## Where to start

Build a [Press](/items/press) first: most recipes in the add-on need sheets. [From ingot to upgrade](/automatisation/production) describes the production chain and how to lay out a line.

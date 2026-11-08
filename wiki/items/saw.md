---
title: Saw
infobox:
  icon: saw
  rows:
    Type: Block (machine)
    Mod: Automatisation
    Stack size: 64
    Tool: Pickaxe (required)
    Hardness: 3
    ID: "bettercoppers_automatisation:saw"
---

# Saw

A **Saw** is a [machine](/automatisation/machines) placed one block above a [Conveyor Belt](/items/conveyor_belt) that cuts whole stacks into gears, planks and cut blocks while an [operator](/jobs/operator) cranks it.

<!-- MEDIA: items-saw.png | screenshot | A saw placed one block above a conveyor belt, seen from a three-quarter view so its real shape and blade above the belt are visible -->

## Obtaining

<Recipe id="saw" />

## Usage

Place the Saw directly above a belt tile and link an operator to it. Set its filter by right-clicking it with an item other than a wrench; the item is not used up. Right-click with an empty hand to clear the filter.

| Input | Filter | Output per item |
|---|---|---|
| <Icon id="copper_sheet" /> [Copper Sheet](/items/copper_sheet) | Any or none | 1 [Copper Gear](/items/copper_gear) |
| <Icon id="gold_sheet" /> [Gold Sheet](/items/gold_sheet) | Any or none | 1 [Gold Gear](/items/gold_gear) |
| Planks | That wood's fence | 1 fence |
| Planks | That wood's stairs | 1 stairs |
| Planks | That wood's slab | 2 slabs |
| Any item a crafting table turns into planks, alone (logs, wood, stripped variants) | Any or none | Half again as many planks as crafting, rounded down: 6 per log |
| Anything a [stonecutter](https://minecraft.wiki/w/Stonecutter) cuts | The product wanted | The stonecutter recipe's output |

Woods match by name: `oak_planks` with `oak_fence`. An Iron Sheet is not cut. The rules are tried in the order above.

## Behaviour

A Saw catches any item it can work on in the block below, centres it and holds it, even while idle or [jammed](/automatisation/wear-and-jams). The operator then cranks for one cycle of 50 ticks (2.5 s). The stack changes at 40 ticks (2 s) and the Saw lets it go at the end. A result larger than one stack comes out as several stacks at the same spot.

The Saw wears 1 point per finished cycle and jams after 64 cycles. It is repaired with an [Iron Sheet](/items/iron_sheet).

## Advancements

| Advancement | Requirement |
|---|---|
| Cogs and Teeth | Have a Copper or Gold Gear in the inventory |

## Data values

| Name | Value |
|---|---|
| Registry ID | `bettercoppers_automatisation:saw` |
| Translation key | `block.bettercoppers_automatisation.saw` |
| Block tags | `minecraft:mineable/pickaxe` |
| Block states | `facing` (horizontal direction), `running` (boolean) |

---
title: Crafting recipes
---

# Crafting recipes

The **crafting recipes** of Better Coppers and its Automatisation add-on are shown below, sorted by product. Products of the [Press](/items/press) and the [Saw](/items/saw) are not crafted and are listed in their own tables further down.

## Crafting grid

<Recipe all />

## Press products

A [Press](/items/press) hangs one block above a belt and works on the stack lying underneath it. A cycle takes 2 seconds (40 ticks) of cranking; the whole stack is converted at the hit, 1 second (20 ticks) in, and the Press gains 1 wear per cycle.

| Input | Product | Per input item |
|---|---|---|
| <Icon id="copper_ingot" /> Copper Ingot | <Icon id="copper_sheet" /> [Copper Sheet](/items/copper_sheet) | 1 |
| <Icon id="iron_ingot" /> Iron Ingot | <Icon id="iron_sheet" /> [Iron Sheet](/items/iron_sheet) | 1 |
| Gold Ingot | <Icon id="gold_sheet" /> [Gold Sheet](/items/gold_sheet) | 1 |
| Sugar Cane | <Icon id="paper" /> Paper | 1 |

## Saw products

A [Saw](/items/saw) also hangs one block above a belt. A cycle takes 2.5 seconds (50 ticks) of cranking; the whole stack is converted at the hit, 2 seconds (40 ticks) in, and the Saw gains 1 wear per cycle. Results that do not fit in one stack come out as several stacks. Its filter is set by right-clicking it with an item and cleared with an empty hand. Rules are tried in this order, and the first that applies decides the product.

| Order | Input | Filter | Product | Per input item |
|---|---|---|---|---|
| 1 | <Icon id="copper_sheet" /> [Copper Sheet](/items/copper_sheet) | Any or none | <Icon id="copper_gear" /> [Copper Gear](/items/copper_gear) | 1 |
| 1 | <Icon id="gold_sheet" /> [Gold Sheet](/items/gold_sheet) | Any or none | <Icon id="gold_gear" /> [Gold Gear](/items/gold_gear) | 1 |
| 2 | Any planks | Fence of the same wood | That fence | 1 |
| 2 | Any planks | Stairs of the same wood | Those stairs | 1 |
| 2 | Any planks | Slab of the same wood | That slab | 2 |
| 3 | A log, wood block or other item whose one-slot crafting recipe makes planks | Any or none | Those planks | Half again the crafting amount, rounded down (a log gives 6) |
| 4 | Any item with a [stonecutter](https://minecraft.wiki/w/Stonecutter) recipe | The item that recipe makes | That item | As in the stonecutter |

Iron Sheets have no Saw product. The Press and Saw hold an item they can work on while their [operator](/jobs/operator) is away, and leave everything else alone.

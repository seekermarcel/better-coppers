---
title: From ingot to upgrade
---

# From ingot to upgrade

The **production chain** of [Automatisation](/automatisation/) turns ingots into sheets on a [Press](/items/press), sheets into gears on a [Saw](/items/saw), and sheets and gears into [upgrades](/automatisation/upgrades), machines and new golems at a crafting table or [Golem Assembler](/items/golem_assembler).

## The chain

| Step | Where | In | Out | Running time per stack |
|---|---|---|---|---|
| Sheets | [Press](/items/press) | 1 Copper, Iron or Gold Ingot | 1 [Copper](/items/copper_sheet), [Iron](/items/iron_sheet) or [Gold Sheet](/items/gold_sheet) | 2 s |
| Paper | [Press](/items/press) | 1 Sugar Cane | 1 Paper | 2 s |
| Gears | [Saw](/items/saw) | 1 Copper or Gold Sheet | 1 [Copper](/items/copper_gear) or [Gold Gear](/items/gold_gear) | 2.5 s |
| Planks | [Saw](/items/saw) | 1 log | 6 planks | 2.5 s |

Iron Sheets have no gear. The Press and Saw work on the whole stack at once, so the time above is the same for 1 item or 64.

## What the factory makes

| Product | Materials | Ingots needed | Use |
|---|---|---|---|
| <Icon id="iron_plating" /> [Iron Plating](/items/iron_plating) | 5 Iron Sheets | 5 iron | Upgrade |
| <Icon id="copper_coating" /> [Copper Coating](/items/copper_coating) | 3 Copper Sheets, Honeycomb | 3 copper | Upgrade |
| <Icon id="gold_gearwork" /> [Gold Gearwork](/items/gold_gearwork) | 2 Gold Gears, 1 Copper Gear, Redstone Dust | 2 gold, 1 copper | Upgrade |
| <Icon id="copper_satchel" /> [Copper Satchel](/items/copper_satchel) | 7 Leather, 1 Copper Sheet | 1 copper | Upgrade |
| <Icon id="cart_extension" /> [Cart Extension](/items/cart_extension) | 4 Copper Sheets, a wooden chest | 4 copper | [Copper Chest Minecart](/items/copper_chest_minecart) |
| New [copper golem](/golem/copper-golem) | 4 Copper Sheets, 2 Copper Gears, Carved Pumpkin, Redstone Dust | 6 copper | [Golem Assembler](/items/golem_assembler) |
| Rails | 4 Iron Sheets, a Stick | 4 iron | 16 rails, for a [miner](/jobs/miner) |
| Paper | Sugar Cane | None | [Mineshaft Plans](/items/mineshaft_plan) |
| Fences, slabs, stairs | Planks, with a [Saw](/items/saw) filter | None | [Miner](/jobs/miner) supports and building |

The recipe for rails:

<Recipe id="rail_from_iron_sheets" />

The [Saw](/items/saw) itself needs 5 Copper Sheets, so the first Press has to run before a Saw can be built. The Press is crafted from ingots, Pistons and a Block of Iron and needs no sheets. Spare parts for [repairs](/automatisation/wear-and-jams) are also sheets: a line that makes sheets can supply its own machines.

## Laying out a line

An iron sheet line needs these parts:

```
[chest] -> [funnel, pushing] -> belt -> belt -> belt -> belt -> [funnel, pulling] -> [chest]
                                 ^              ^
                           [Belt Drive]      [Press] one block above the belt
                           + operator        + operator
                           + chest with      + chest with
                             Copper Sheets     Iron Sheets
```

1. Place a chest, and a [Funnel](/items/funnel) on its side with the mouth facing the line. Right-click the Funnel with a [Copper Wrench](/items/copper_wrench) to make it push.
2. Place the [Conveyor Belts](/items/conveyor_belt) in the direction the line should run. A belt runs the way the player faces when placing it.
3. Place a [Press](/items/press) one block above a belt tile, with nothing between them.
4. Place a [Belt Drive](/items/belt_drive) directly beside any belt of the line, at the same height.
5. At the end, place a pulling Funnel on the side of a second chest, its mouth towards the last belt.
6. Make two [operators](/jobs/operator) and link one to the Press and one to the Belt Drive. Each machine needs its own operator.
7. Leave a chest of spare parts within 8 blocks of each machine: Iron Sheets for the Press, Copper Sheets for the Belt Drive.

Put iron ingots in the first chest. Iron Sheets arrive in the last chest for as long as both operators keep cranking.

<!-- MEDIA: automatisation-iron-line.png | screenshot | A complete iron sheet line from the side: chest with a pushing funnel, a row of belts, a press hanging one block above the middle belt, a belt drive beside the line, a pulling funnel into a chest at the end, a golem with a wrench standing beside the press and another beside the belt drive -->

### Practical limits

- Belts are flat. Use a [Chute](/items/chute) to go down and a [Grabber Arm](/items/grabber_arm) to go up.
- A pushing Funnel and a Grabber Arm move at most 16 items at a time. A [Press](/items/press) fed by them does at most 16 items per cycle, and [wears](/automatisation/wear-and-jams) one point per cycle however many items it handled.
- A held stack blocks the belt behind it, so items queue up and nothing slips past a Press or Saw.
- Operators work only in daytime. A line stands still at night.
- Copper golems walk across belts and are not carried by them, so an operator can stand on the line.

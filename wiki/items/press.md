---
title: Press
infobox:
  icon: press
  rows:
    Type: Block (machine)
    Mod: Automatisation
    Stack size: 64
    Tool: Pickaxe (required)
    Hardness: 3
    ID: "bettercoppers_automatisation:press"
---

# Press

A **Press** is a [machine](/automatisation/machines) placed one block above a [Conveyor Belt](/items/conveyor_belt) that flattens whole stacks of ingots into sheets while an [operator](/jobs/operator) cranks it.

<!-- MEDIA: items-press.png | screenshot | A press placed one block above a conveyor belt, seen from a three-quarter view so its real shape and the press head above the belt are visible -->

## Obtaining

<Recipe id="press" />

## Usage

Place the Press directly above a belt tile and link an operator to it. The Press works on items in the block below it, so a belt is not strictly needed.

| Input | Output |
|---|---|
| <Icon id="copper_ingot" /> Copper Ingot | <Icon id="copper_sheet" /> [Copper Sheet](/items/copper_sheet) |
| <Icon id="iron_ingot" /> Iron Ingot | <Icon id="iron_sheet" /> [Iron Sheet](/items/iron_sheet) |
| <Icon id="gold_ingot" /> Gold Ingot | <Icon id="gold_sheet" /> [Gold Sheet](/items/gold_sheet) |
| <Icon id="sugar_cane" /> Sugar Cane | <Icon id="paper" /> Paper |

Each item in the stack becomes one output.

## Behaviour

A Press catches any matching item in the block below, centres it and holds it, even while it is idle or [jammed](/automatisation/wear-and-jams). Belts leave a held item alone, and items queue up behind it. The operator then cranks for one cycle of 40 ticks (2 s). The stack changes at 20 ticks (1 s) and the Press lets it go at the end. A cycle takes the same time for 1 item or 64.

The Press wears 1 point per finished cycle and jams after 64 cycles. It is repaired with an [Iron Sheet](/items/iron_sheet). The Press has no filter.

## Data values

| Name | Value |
|---|---|
| Registry ID | `bettercoppers_automatisation:press` |
| Translation key | `block.bettercoppers_automatisation.press` |
| Block tags | `minecraft:mineable/pickaxe` |
| Block states | `facing` (horizontal direction), `running` (boolean) |

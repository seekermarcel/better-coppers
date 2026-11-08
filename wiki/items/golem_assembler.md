---
title: Golem Assembler
infobox:
  icon: golem_assembler
  rows:
    Type: Block (machine)
    Mod: Automatisation
    Stack size: 64
    Tool: Pickaxe (required)
    Hardness: 3
    ID: "bettercoppers_automatisation:golem_assembler"
---

# Golem Assembler

A **Golem Assembler** is a [machine](/automatisation/machines) with 9 slots that builds new copper golems while an [operator](/jobs/operator) cranks it.

<!-- MEDIA: items-golem-assembler.png | screenshot | The golem assembler's 9-slot menu filled with copper sheets, copper gears, a carved pumpkin and redstone, with a finished copper golem standing in front of the block visible behind the inventory if possible -->

## Obtaining

<Recipe id="golem_assembler" />

## Usage

Right-click the Assembler to open its slots. It can also be filled by [hoppers](https://minecraft.wiki/w/Hopper), [Funnels](/items/funnel) or a [Grabber Arm](/items/grabber_arm), which only put golem parts in.

One golem needs:

| Part | Amount |
|---|---|
| <Icon id="copper_sheet" /> [Copper Sheet](/items/copper_sheet) | 4 |
| <Icon id="copper_gear" /> [Copper Gear](/items/copper_gear) | 2 |
| <Icon id="carved_pumpkin" /> Carved Pumpkin | 1 |
| <Icon id="redstone" /> Redstone Dust | 1 |

The block in front of the Assembler (the side facing the player who placed it) and the block above that must have no collision, and no copper golem may stand in the block in front. With all parts inside and room in front, the operator cranks for 400 ticks (20 s). The parts are then used up and the golem steps out.

## Behaviour

The new golem has no job, like any other [copper golem](/golem/copper-golem). It is a jobless golem until a player gives it a job.

Progress counts ticks of cranking and is kept when the operator stops. The Assembler wears 1 point per finished golem, [jams](/automatisation/wear-and-jams) after 8 and is repaired with an [Iron Sheet](/items/iron_sheet). A broken Assembler drops its contents.

## Advancements

| Advancement | Requirement |
|---|---|
| Golem Factory | Be within 48 blocks when an Assembler builds a golem |

## Data values

| Name | Value |
|---|---|
| Registry ID | `bettercoppers_automatisation:golem_assembler` |
| Translation key | `block.bettercoppers_automatisation.golem_assembler` |
| Container title key | `container.bettercoppers_automatisation.golem_assembler` |
| Block tags | `minecraft:mineable/pickaxe` |
| Block states | `facing` (horizontal direction), `running` (boolean) |

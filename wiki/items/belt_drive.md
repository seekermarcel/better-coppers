---
title: Belt Drive
infobox:
  icon: belt_drive
  rows:
    Type: Block (machine)
    Mod: Automatisation
    Stack size: 64
    Tool: Pickaxe (required)
    Hardness: 3
    ID: "bettercoppers_automatisation:belt_drive"
---

# Belt Drive

A **Belt Drive** is a [machine](/automatisation/machines) with a crank and a big gear that makes [Conveyor Belts](/items/conveyor_belt) run while an [operator](/jobs/operator) turns it.

<!-- MEDIA: items-belt-drive.png | screenshot | A belt drive block placed next to a conveyor belt, seen from a three-quarter view so its real shape (body, big gear and crank) is visible -->

## Obtaining

<Recipe id="belt_drive" />

## Usage

Place a Belt Drive directly beside any belt of a line, at the same height, and link an operator to it. All belts of every line it touches then run.

A line is every belt reachable by following belts that run into each other, forwards and backwards. Up to 64 belts in total are driven. The count is checked between belts, so a line with branches can end a little over 64.

A belt on any of the four sides of the drive makes it a target for its operator, even if the line is empty. The operator cranks and the drive wears out all day.

## Behaviour

While running, the drive refreshes its belts every 5 ticks, so belts stop within half a second of the drive stopping. It wears 1 point per second of running and [jams](/automatisation/wear-and-jams) after 1200 points (20 minutes). It is repaired with a [Copper Sheet](/items/copper_sheet).

## Advancements

| Advancement | Requirement |
|---|---|
| Keep It Moving | Be within 48 blocks when a Belt Drive starts running |

## Data values

| Name | Value |
|---|---|
| Registry ID | `bettercoppers_automatisation:belt_drive` |
| Translation key | `block.bettercoppers_automatisation.belt_drive` |
| Block tags | `minecraft:mineable/pickaxe` |
| Block states | `facing` (horizontal direction), `running` (boolean) |

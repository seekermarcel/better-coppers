---
title: Conveyor Belt
infobox:
  icon: conveyor_belt
  rows:
    Type: Block
    Mod: Automatisation
    Stack size: 64
    Tool: None needed
    Hardness: 0.8
    ID: "bettercoppers_automatisation:conveyor_belt"
---

# Conveyor Belt

A **Conveyor Belt** is a flat block, 4 pixels tall, that carries items, mobs and players while a [Belt Drive](/items/belt_drive) next to its line is being cranked.

<!-- MEDIA: items-conveyor-belt.mp4 | clip | A short line of conveyor belts with a corner, running, carrying a dropped stack of items along -->

## Obtaining

<Recipe id="conveyor_belt" />

## Usage

A belt runs the way the player faces when placing it. Belts that run into each other form one line, so a corner is made by placing the next belt facing the new way. A line needs one [Belt Drive](/items/belt_drive) with an [operator](/jobs/operator); a belt that is not driven stands still.

Belts are flat. To go down, use a [Chute](/items/chute); to go up, use a [Grabber Arm](/items/grabber_arm).

## Behaviour

Running belts move what is on them at about 1.6 blocks per second and nudge it towards the middle of the belt.

| Entity | Carried |
|---|---|
| Dropped items | Yes |
| Mobs | Yes |
| Players | Yes, unless sneaking or flying |
| Copper golems | No |
| Passengers, spectators | No |
| Items a [Press](/items/press) or [Saw](/items/saw) is holding | No |

An item stops when another item lies still less than 0.55 blocks ahead of it on the same line. Items queue up behind a held stack, so nothing slips past a Press or Saw.

## Data values

| Name | Value |
|---|---|
| Registry ID | `bettercoppers_automatisation:conveyor_belt` |
| Translation key | `block.bettercoppers_automatisation.conveyor_belt` |
| Block states | `facing` (horizontal direction), `running` (boolean) |

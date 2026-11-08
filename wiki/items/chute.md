---
title: Chute
infobox:
  icon: chute
  rows:
    Type: Block
    Mod: Automatisation
    Stack size: 64
    Tool: Pickaxe
    Hardness: 2
    ID: "bettercoppers_automatisation:chute"
---

# Chute

A **Chute** is a hollow copper shaft that drops items straight down, a whole stack at a time and much faster than a [hopper](https://minecraft.wiki/w/Hopper).

<!-- MEDIA: items-chute.png | screenshot | A column of two chutes above a chest, seen from above at an angle so the hollow middle of the shaft is visible -->

## Obtaining

<Recipe id="chute" />

## Usage

Items that fall into the open top land in the shaft. Hoppers, [Funnels](/items/funnel) and [Grabber Arms](/items/grabber_arm) can fill it too, because the Chute acts as a one-slot container. Every 4 ticks (0.2 s), the stack goes:

- into the Chute below, if that one is empty;
- into the container below;
- out of the bottom, if the block below is not solid to its full height, for example onto a [Conveyor Belt](/items/conveyor_belt) or the floor.

If the block below is solid, the stack stays in the Chute until something changes. Only one stack is held at a time.

## Behaviour

A Chute needs no [operator](/jobs/operator). Only its walls are solid, so items drop into the 10-pixel-wide shaft. When broken it drops the stack it holds.

## Data values

| Name | Value |
|---|---|
| Registry ID | `bettercoppers_automatisation:chute` |
| Translation key | `block.bettercoppers_automatisation.chute` |
| Block tags | `minecraft:mineable/pickaxe` |

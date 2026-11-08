---
title: Funnel
infobox:
  icon: funnel
  rows:
    Type: Block
    Mod: Automatisation
    Stack size: 64
    Tool: Pickaxe
    Hardness: 2
    ID: "bettercoppers_automatisation:funnel"
---

# Funnel

A **Funnel** is a block placed against a container that pulls items into it or pushes stacks out of it, with no [operator](/jobs/operator) needed.

<!-- MEDIA: items-funnel.png | screenshot | A funnel placed against the side of a chest, seen from a three-quarter view so its back plate and mouth rim are visible -->

## Obtaining

<Recipe id="funnel" />

## Usage

Place a Funnel against a container. Its mouth points away from the block it was placed on, in any of the six directions. With no container directly behind it, the Funnel does nothing.

| Mode | Set with | What it does |
|---|---|---|
| Pulling (default) | Right-click with a [Copper Wrench](/items/copper_wrench) to change | Swallows every item that enters its block, from a belt or thrown in |
| Pushing | Right-click with a Copper Wrench to change | Every 10 ticks (0.5 s), pushes up to 16 items of one stack out of the container, in front of its mouth |

Pushing only happens when no item lies in the block in front of the mouth. The Funnel takes the first stack in the container that passes its filter and puts it out on top of whatever is in front, for example a [Conveyor Belt](/items/conveyor_belt).

**Filter:** right-click a Funnel with any item other than a wrench to set its filter; the item is not used up. Items that are the same item pass, whatever their components. Right-click with an empty hand to clear it. Sneak to place blocks against a Funnel.

## Behaviour

A Funnel needs no power and works on any container [hoppers](https://minecraft.wiki/w/Hopper) can use, including double chests. A Funnel pulling into a full container keeps what does not fit lying in its mouth.

## Data values

| Name | Value |
|---|---|
| Registry ID | `bettercoppers_automatisation:funnel` |
| Translation key | `block.bettercoppers_automatisation.funnel` |
| Block tags | `minecraft:mineable/pickaxe` |
| Block states | `facing` (all directions), `pushing` (boolean) |

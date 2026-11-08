---
title: Charging Pad
infobox:
  icon: charging_pad
  rows:
    Type: Block
    Mod: Better Coppers
    Stack size: 64
    Tool: Pickaxe
    Hardness: 3
    Blast resistance: 6
    ID: "bettercoppers:charging_pad"
---

# Charging Pad

A **Charging Pad** is the base mod's block where a [copper golem](/golem/copper-golem) with a paying [job](/jobs/) recharges its [energy](/golem/running-costs). It is powered by redstone pulses, so it needs a redstone clock. The add-on's [Charging Station](/items/charging_station) does the same without a clock, and faster.

## Obtaining

<Recipe id="charging_pad" />

It drops as an item when mined with any tool, though a pickaxe is fastest, and it survives explosions.

## Usage

Place the Charging Pad and build a [redstone clock](https://minecraft.wiki/w/Redstone_circuits) beside it. Keep the space above it free: a golem stands on top and needs two open blocks there.

Golems find a Charging Pad within 16 blocks horizontally and 6 blocks vertically of their home or workplace. Each Charging Pad fits one golem, so build one per golem.

<!-- MEDIA: charging-pad-clock.png | screenshot | A Charging Pad with a compact redstone clock next to it and a working copper golem standing on top of the pad -->

## Behaviour

A steady redstone signal does nothing. Each time the signal switches on, every paying golem standing on it gains **8 energy**, up to the maximum of 1000. A pulse that arrives within 3 ticks of the last one that counted is ignored. Placing it next to a lit source is not a pulse.

| Clock | Pulses to full | Time from empty to full |
|---|---|---|
| Every 4 ticks (fastest useful) | 125 | 25 seconds |
| Every second (20 ticks) | 125 | about 2 minutes |

For a golem at full energy, a pulse does nothing. Sorters, couriers and jobless golems are not charged. During the cooldown after a pulse it gives off light level 9.

When the players sleep through the night, every paying golem with a free Charging Pad near its place wakes up fully charged, with or without a clock, as described in [Running costs](/golem/running-costs#charging). Rules for when golems come to a Charging Pad and when they leave are there too.

## Advancements

| Advancement | How |
|---|---|
| Recharged | Be within 48 blocks when a golem charges to full on a Charging Pad |

## Data values

| | |
|---|---|
| Block ID | `bettercoppers:charging_pad` |
| Item ID | `bettercoppers:charging_pad` |
| Translation key | `block.bettercoppers.charging_pad` |
| Block tag | `minecraft:mineable/pickaxe` |
| Block states | `powered` (true/false), `cooling` (true/false) |

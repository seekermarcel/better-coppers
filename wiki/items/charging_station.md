---
title: Charging Station
infobox:
  icon: charging_station
  rows:
    Type: Block
    Mod: Automatisation
    Stack size: 64
    Tool: Pickaxe (required)
    Hardness: 3
    Light: 7
    ID: "bettercoppers_automatisation:charging_station"
---

# Charging Station

A **Charging Station** is a gilded block where a golem with a [paying job](/golem/running-costs) recharges with no redstone clock, faster than on a [Charging Pad](/items/charging_pad).

## Obtaining

<Recipe id="charging_station" />

## Usage

Place the Station so a golem can stand on it, with two free blocks above it. A golem recharges on the nearest free Charging Pad or Charging Station within 16 blocks horizontally and 6 blocks vertically of its [machine](/jobs/operator), or of its home if it has no machine. Each Station holds one golem at a time.

## Behaviour

A golem standing on the Station gains 4 energy every tick. A full charge from empty (1000 energy) takes 250 ticks (12.5 s), twice as fast as a Charging Pad on the fastest clock.

A golem goes to a Station at night and when it has run out of energy. A golem that ran empty stays until it is full. When the players sleep through the night, every paying job golem with a free Charging Pad or Station near its workplace wakes up fully charged.

## Advancements

| Advancement | Requirement |
|---|---|
| Fast Charger | Have a Charging Station in the inventory |

## Data values

| Name | Value |
|---|---|
| Registry ID | `bettercoppers_automatisation:charging_station` |
| Translation key | `block.bettercoppers_automatisation.charging_station` |
| Block tags | `minecraft:mineable/pickaxe` |

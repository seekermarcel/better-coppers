---
title: Scraper
infobox:
  icon: scraper
  rows:
    Type: Block
    Mod: Better Coppers
    Stack size: 64
    Tool: Pickaxe
    Hardness: 3.5
    Blast resistance: 3.5
    ID: "bettercoppers:scraper"
---

# Scraper

A **Scraper** is the base mod's block that [scrapes](/golem/running-costs#scraping) the [copper golem](/golem/copper-golem) standing in front of it, on its own and without wearing out.

## Obtaining

<Recipe id="scraper" />

It is crafted like a [dispenser](https://minecraft.wiki/w/Dispenser), with any axe in place of the bow. It needs a pickaxe to drop as an item.

## Usage

Place it like a dispenser: its front, with the axe, faces you, and it can face up or down too. Stand on the spot where the golem will be when you place it.

The usual build is a Scraper beside a [Charging Pad](/items/charging_pad) or [Charging Station](/items/charging_station), facing the golem's spot, so a golem is scraped clean every time it charges.

<!-- MEDIA: scraper-pad.png | screenshot | A Scraper facing a Charging Pad with a rusty copper golem standing on the pad -->

## Behaviour

Once a second, it scrapes the first copper golem in the block in front of it that can be scraped: the wax first, otherwise one stage of rust. It keeps going until the golem is clean, then does nothing. It needs no redstone and has no inventory.

It scrapes every copper golem, with or without a job. This is the one exception to [the vanilla rule](/start/vanilla-rule#the-scraper), because the player built it on purpose.

## Advancements

| Advancement | How |
|---|---|
| Good as New | Be within 48 blocks when a Scraper scrapes a golem |

## Data values

| | |
|---|---|
| Block ID | `bettercoppers:scraper` |
| Item ID | `bettercoppers:scraper` |
| Translation key | `block.bettercoppers.scraper` |
| Block tag | `minecraft:mineable/pickaxe` |
| Block states | `facing` (north/east/south/west/up/down) |

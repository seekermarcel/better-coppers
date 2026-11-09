---
title: Survey Table
infobox:
  icon: survey_table
  rows:
    Type: Block
    Mod: Better Coppers
    Stack size: 64
    Tool: Axe
    Hardness: 2.5
    Blast resistance: 2.5
    ID: "bettercoppers:survey_table"
---

# Survey Table

A **Survey Table** is a workstation that turns paper into [Mineshaft Plans](/items/mineshaft_plan) for the [miner](/jobs/miner) job.

## Obtaining

### Crafting

<Recipe id="survey_table" />

### Breaking

The block drops itself when broken with any tool or by hand, unless it is destroyed by an explosion that does not leave drops. An axe is the fastest tool.

## Usage

Right-clicking a Survey Table opens its screen. The screen looks like a [stonecutter](https://minecraft.wiki/w/Stonecutter)'s, with a panel in the middle instead of a recipe list.

<!-- MEDIA: items-survey-table-screen.png | screenshot | The Survey Table screen with paper in the left slot, a Mineshaft Plan in the right slot, and the panel in the middle showing the Mine Pattern button and the length 32 with its minus and plus buttons -->

| Part | Function |
|---|---|
| Left slot | Takes paper, and only paper |
| Middle panel | A button with the name of the Mine Pattern, then "Length" and the chosen length, with a **-** and a **+** button |
| Right slot | The finished Mineshaft Plan. Taking it uses up one paper |

| Length setting | Value |
|---|---|
| Shortest | 8 blocks |
| Starting value | 32 blocks |
| Longest | 128 blocks, shown with "max" |
| Step | 8 blocks per click |

The length resets to 32 when the screen is opened. There are 16 possible lengths. Clicking the Mine Pattern button moves on to the next Mine Pattern: Mineshaft, Staircase or Strip Mine. A Mine Pattern with a setting besides length adds a second row of **-** and **+** buttons for it. Strip Mine has one: its branch length, 4 to 32 blocks in steps of 4, starting at 16. The result is a blank Mineshaft Plan with no entrance marked. Paper left in the slot returns to the player when the screen closes.

## Advancements

| Advancement | How | ID |
|---|---|---|
| Surveyor | Make a Mineshaft Plan | `bettercoppers:guide/survey` |

## Data values

| Name | Value |
|---|---|
| Block and item ID | `bettercoppers:survey_table` |
| Translation key | `block.bettercoppers.survey_table` |
| Screen title key | `container.bettercoppers.survey_table` |
| Tag | `minecraft:mineable/axe` |

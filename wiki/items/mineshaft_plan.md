---
title: Mineshaft Plan
infobox:
  icon: mineshaft_plan
  rows:
    Type: Item
    Mod: Better Coppers
    Stack size: 1
    ID: "bettercoppers:mineshaft_plan"
---

# Mineshaft Plan

A **Mineshaft Plan** is an item that holds a mine's entrance, direction, length and progress. A [miner](/jobs/miner) works through one step by step.

## Obtaining

A Mineshaft Plan is made at a [Survey Table](/items/survey_table) from one paper. It has no crafting recipe. The mod's creative tab has a blank one of length 32.

## Usage

### Marking

To **mark** a Mineshaft Plan, use it on a block where the mine should start.

| Clicked | Entrance |
|---|---|
| Top of a block | The block above it |
| Any other side of a block | The clicked block itself |

The entrance is the middle block of the first slice at floor height. The tunnel heads in the horizontal direction the player is facing. A message shows the entrance coordinates and heading.

Marking a Mineshaft Plan again moves the entrance and resets its progress to 0. A Mineshaft Plan cannot be marked while a golem holds it.

### Giving it to a golem

Sneak + right-click a golem that holds a copper pickaxe with a marked Mineshaft Plan. The golem keeps it and starts digging.

| Situation | Result |
|---|---|
| Not marked | Refused. The message asks to mark the entrance first |
| Golem has no copper pickaxe | Refused. The message asks for the pickaxe first |
| Golem already has one | The old one goes back to the player, progress included |

When the golem's job ends, the Mineshaft Plan comes back to the player with its progress. If the golem dies, it drops.

### Tooltip

| Line | Shown |
|---|---|
| Mineshaft | Always, in gold |
| Length: *N* blocks | Always |
| Use on the ground at the mine entrance | While unmarked, in yellow |
| Entrance: *x y z*, heading *direction* | Once marked |
| In progress (*N* steps done) | Once progress is above 0 |

<!-- MEDIA: items-mineshaft-plan-tooltip.png | screenshot | The tooltip of a marked Mineshaft Plan with its length, entrance coordinates and heading, and a progress line -->

### Progress

Progress counts finished steps of the [tunnel layout](/jobs/miner#the-tunnel), including steps the golem had to skip. The golem resumes where it stopped.

| Length | Dig and floor steps | Support and torch steps | Rail steps | Total steps |
|---|---|---|---|---|
| 8 | 96 | 15 | 8 | 119 |
| 16 | 192 | 30 | 16 | 238 |
| 32 | 384 | 60 | 32 | 476 |
| 64 | 768 | 120 | 64 | 952 |
| 128 | 1536 | 240 | 128 | 1904 |

Each slice has 12 dig and floor steps. Each support adds 7 steps. Each torch adds 1 step.

## Advancements

| Advancement | How | ID |
|---|---|---|
| Surveyor | Have a Mineshaft Plan in the inventory | `bettercoppers:guide/survey` |
| Down the Shaft | Give a golem a copper pickaxe and a Mineshaft Plan | `bettercoppers:guide/miner` |

## Data values

| Name | Value |
|---|---|
| Item ID | `bettercoppers:mineshaft_plan` |
| Translation key | `item.bettercoppers.mineshaft_plan` |
| Item component | `bettercoppers:mineshaft_plan` |

The item component has these fields:

| Field | Type | Meaning |
|---|---|---|
| `pattern` | String | Always `mineshaft` |
| `length` | Integer, 8 to 128 | Tunnel length in blocks |
| `entrance` | Block position, optional | Set by marking |
| `heading` | Direction, optional | Set by marking |
| `progress` | Integer, default 0 | Finished steps |

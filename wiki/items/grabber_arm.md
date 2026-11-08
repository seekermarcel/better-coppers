---
title: Grabber Arm
infobox:
  icon: grabber_arm
  rows:
    Type: Block (machine)
    Mod: Automatisation
    Stack size: 64
    Tool: Pickaxe (required)
    Hardness: 3
    ID: "bettercoppers_automatisation:grabber_arm"
---

# Grabber Arm

A **Grabber Arm** is a [machine](/automatisation/machines): a copper arm on a turntable that swings up to 16 items at a time from its input spots to its output spots while an [operator](/jobs/operator) cranks it.

<!-- MEDIA: items-grabber-arm.mp4 | clip | A grabber arm with its real shape visible (turntable base, arm segments, claw), swinging items from a chest onto a conveyor belt while a golem with a wrench stands beside it -->

## Obtaining

<Recipe id="grabber_arm" />

## Usage

Mark the arm's spots with a [Copper Wrench](/items/copper_wrench):

1. Right-click the arm with the wrench. Marking starts, and the arm's spots show as coloured dust.
2. Right-click a block with the wrench: once marks an input (blue dust), twice an output (orange dust), three times removes it.
3. Right-click the arm again to finish. The overlay shows how many inputs and outputs were set.

A spot must be within 4 blocks horizontally (X and Z each) and 3 vertically of the arm. Marking is kept for the player until the server restarts; right-clicking another arm starts marking that one instead. The spots themselves are saved with the arm.

A spot is a container or any other block. On other blocks the arm picks up loose items lying in the spot, or puts stacks down on top of the block, for example on a [Conveyor Belt](/items/conveyor_belt).

## Behaviour

Each swing takes 20 ticks (1 s) of cranking. A grab and a drop are one swing each, so moving a load takes 2 s. The arm takes up to 16 items:

- **Inputs** are used in turn. An input only counts when its first stack has an output with room.
- **Outputs** are checked in the order they were marked; the first with room gets the load.
- An arm that holds something it cannot put down keeps it and its operator keeps cranking.

From a container, the arm takes the first non-empty slot. A non-container spot accepts drops only when the block is not solid to its full height and holds fewer than 3 loose item stacks. The arm wears 1 point per swing, and [jams](/automatisation/wear-and-jams) after 256 swings. It is repaired with an [Iron Sheet](/items/iron_sheet). A broken arm drops what it holds.

## Advancements

| Advancement | Requirement |
|---|---|
| Pick and Place | Finish marking an arm with at least one input and one output |

## Data values

| Name | Value |
|---|---|
| Registry ID | `bettercoppers_automatisation:grabber_arm` |
| Translation key | `block.bettercoppers_automatisation.grabber_arm` |
| Block tags | `minecraft:mineable/pickaxe` |
| Block states | `facing` (horizontal direction), `running` (boolean) |

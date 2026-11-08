---
title: Wear and jams
---

# Wear and jams

**Wear** is the damage a [machine](/automatisation/machines) takes from use. When a machine has taken its limit it **jams**: it stops, smokes and plays an anvil break sound. It stays jammed until its [operator](/jobs/operator) repairs it with a spare part from a container nearby.

## Wear numbers

| Machine | Wear per | Jams after | Equals | Repair item |
|---|---|---|---|---|
| [Belt Drive](/items/belt_drive) | Second of running | 1200 | 20 minutes of running | [Copper Sheet](/items/copper_sheet) |
| [Press](/items/press) | Finished cycle | 64 | 64 stacks | [Iron Sheet](/items/iron_sheet) |
| [Saw](/items/saw) | Finished cycle | 64 | 64 stacks | [Iron Sheet](/items/iron_sheet) |
| [Grabber Arm](/items/grabber_arm) | Swing (each grab and each drop) | 256 | 128 grab-and-drop trips | [Iron Sheet](/items/iron_sheet) |
| [Golem Assembler](/items/golem_assembler) | Finished golem | 8 | 8 golems | [Iron Sheet](/items/iron_sheet) |

A Press or Saw takes one wear per cycle however many items are in the stack, so full stacks cost the least wear per item. Sneak + right-click a machine with a [Copper Wrench](/items/copper_wrench) to see its wear in percent and what it is repaired with.

## Repairs

When its machine has jammed, an operator:

1. Looks for the repair item in any container within 8 blocks of the machine horizontally and 3 blocks vertically. [Eyes](/golem/eyes) widen the horizontal distance to 12 (iron), 16 (gold) or 24 (diamond) blocks. The operator picks the container nearest to the machine.
2. Walks to the container and takes one item.
3. Walks back to within reach of the machine: 2.2 blocks horizontally and 2.5 vertically.
4. Works for 40 ticks (2 s), with anvil sounds, and uses up the part. The machine's wear goes back to 0.

If no part is found, the operator waits 100 ticks (5 s) and looks again. A repair costs no [energy](/golem/running-costs), but the operator has to be able to work: daylight, not recharging, not fully oxidized and not waxed. If the operator cannot get anywhere for 15 s, it rests for 10 s.

A part fetched for a different machine is dropped on the ground when the operator's current machine needs another item.

<!-- MEDIA: automatisation-repair.mp4 | clip | A jammed press smoking; a copper golem with a wrench walks to a nearby chest, takes an iron sheet, walks back to the press, and the smoke stops -->

## Edge cases

- A jammed [Press](/items/press) or [Saw](/items/saw) still catches items in the block below and holds them, so the belt behind it queues up.
- A jammed machine does not respond to its crank, and its operator does not use energy until the repair is finished.
- Breaking a machine and placing it again resets its wear. The dropped item stores nothing.
- The [Golem Assembler](/items/golem_assembler) drops its contents when broken, and a [Grabber Arm](/items/grabber_arm) drops what it holds.

## Advancements

| Advancement | How to get it |
|---|---|
| Maintenance Crew | Be within 48 blocks when an operator finishes repairing a machine |

## Data values

Wear is stored in the machine's block entity as the integer `wear`.

---
title: Machines and cranks
---

# Machines and cranks

A **machine** in [Automatisation](/automatisation/) is a block that only moves while an [operator](/jobs/operator) turns its **crank**. The machines are the [Belt Drive](/items/belt_drive), [Press](/items/press), [Saw](/items/saw), [Grabber Arm](/items/grabber_arm) and [Golem Assembler](/items/golem_assembler). [Conveyor Belts](/items/conveyor_belt), [Funnels](/items/funnel), [Chutes](/items/chute) and [Charging Stations](/items/charging_station) are not machines and take no link.

## How an operator drives a machine

Each machine stays running for 10 ticks (0.5 s) after a turn of its crank. An operator in reach turns the crank every tick, so the machine runs continuously for as long as the operator stays. If the operator leaves, runs out of [energy](/golem/running-costs), stops at night or is panicking, the machine stops within 0.5 s.

An operator is in reach when it is within 2.2 blocks of the machine's centre horizontally and within 2.5 blocks vertically. An operator only goes to its machine when the machine has work. With no work, the operator stays within 5 blocks and potters about.

Every 40 ticks the operator reaches out with its arm and the crank clanks; this is animation and sound only.

<!-- MEDIA: automatisation-cranking.mp4 | clip | A copper golem holding a wrench standing next to a press over a running belt, reaching out while the press head comes down on a stack of ingots and turns them into sheets -->

## Speeds

The time a machine takes counts running ticks only: a machine that stops keeps its progress.

| Machine | One piece of work | Time | Happens at |
|---|---|---|---|
| [Press](/items/press) | One stack | 40 ticks (2 s) | The stack changes at 20 ticks (1 s) |
| [Saw](/items/saw) | One stack | 50 ticks (2.5 s) | The stack changes at 40 ticks (2 s) |
| [Grabber Arm](/items/grabber_arm) | One swing (a grab or a drop) | 20 ticks (1 s) | End of the swing |
| [Golem Assembler](/items/golem_assembler) | One golem | 400 ticks (20 s) | End of the build |
| [Belt Drive](/items/belt_drive) | Keeps belts running | Continuous | Belts move about 1.6 blocks per second |

Machine speed is fixed. [Gold Gearwork](/items/gold_gearwork) makes an operator walk faster but does not make its machine run faster. A rusty operator misses some of its turns: each tick its turn succeeds with a chance equal to its [speed](/golem/running-costs) (85% when exposed, 70% when weathered). A single successful turn keeps the machine going for 10 ticks, so this hardly slows the machine; the operator's walk is slowed as usual.

## What machines share

- **Link:** a machine is the [workplace](/jobs/operator) of the operators linked to it, within 32 blocks of the golem. Nothing stops two operators being linked to the same machine, and each runs it.
- **Placement:** a machine faces the player who placed it. The [Golem Assembler](/items/golem_assembler) puts its golems out of that side.
- **Tool:** a machine needs a pickaxe to drop as an item. Any pickaxe works.
- **Wear:** each machine jams after a set amount of work and is repaired by its operator. See [Wear and jams](/automatisation/wear-and-jams).
- **Wear check:** sneak + right-click a machine with a [Copper Wrench](/items/copper_wrench) to see its wear in percent and its repair material.
- **State:** the block state `running` shows whether a machine is running. Clients use it to animate the machine.

## What counts as work

| Machine | The operator cranks when |
|---|---|
| [Belt Drive](/items/belt_drive) | At least one Conveyor Belt touches it on a side, even with nothing on the belts |
| [Press](/items/press), [Saw](/items/saw) | It holds an item, or a matching item is in the block below |
| [Grabber Arm](/items/grabber_arm) | It holds something, is mid-swing, or an input has an item some output can take |
| [Golem Assembler](/items/golem_assembler) | All parts are inside and there is room in front |

A Belt Drive touching a belt therefore costs its operator energy and wears out all day, even on an empty line.

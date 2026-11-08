---
title: Operator
infobox:
  icon: copper_wrench
  rows:
    Job item: Copper Wrench
    Needs home: No (linked to a machine)
    Running costs: Energy and rust
    Work radius: Reach 2.2 blocks; parts within 8 blocks of the machine
    Mod: Automatisation
---

# Operator

An **operator** is a copper golem with a [Copper Wrench](/items/copper_wrench) that cranks and repairs one [machine](/automatisation/machines) from the [Automatisation](/automatisation/) add-on. It is a [paying job](/golem/running-costs) and its workplace is the machine instead of a home.

## Setup

1. Sneak + right-click a copper golem with a [Copper Wrench](/items/copper_wrench). The golem takes it and any other job ends; the player gets back what the golem carried for that job.
2. Sneak + right-click the golem with an empty hand, then right-click a machine within 32 blocks of the golem.

A plain right-click with the wrench gives nothing. A golem that picks up a wrench through vanilla transport does not become an operator. Linking before handing over the wrench fails with a message. The machines are the [Belt Drive](/items/belt_drive), [Press](/items/press), [Saw](/items/saw), [Grabber Arm](/items/grabber_arm) and [Golem Assembler](/items/golem_assembler).

To end the job, sneak + right-click the golem with an empty hand twice within 5 seconds. The wrench comes back to the player and the machine link is forgotten.

<!-- MEDIA: jobs-operator-link.png | screenshot | The particle line from a copper golem holding a wrench to the press it was just linked to -->

## Behaviour

The operator walks to its machine whenever the machine [has work](/automatisation/machines), at 0.8 times walking speed scaled by its [speed](/golem/running-costs). In reach (2.2 blocks horizontally, 2.5 vertically) it stops, looks at the machine and cranks. With no work it stays within 5 blocks and idles.

A jammed machine is [repaired](/automatisation/wear-and-jams): the operator fetches the repair item from the nearest container within 8 blocks horizontally and 3 vertically of the machine, and spends 2 s repairing it. [Eyes](/golem/eyes) widen the horizontal search.

The operator walks over [Conveyor Belts](/items/conveyor_belt) and is not carried by them.

## Running costs

- **Energy:** 1 point per second of cranking. A repair or walk costs no energy. A full charge of 1000 energy lasts 1000 s (16 minutes 40 seconds) of constant cranking.
- **Charging:** the operator recharges on a [Charging Pad](/items/charging_pad) or [Charging Station](/items/charging_station) within 16 blocks horizontally and 6 blocks vertically of its machine, even if it also has a home. Without a machine, the same applies around its home.
- **Rest:** it does not work at night.
- **Rust:** every 3000 energy spent adds one rust stage. A fully oxidized or waxed golem does not work. See [Running costs](/golem/running-costs).

## Limits and edge cases

- An operator with no machine link waits. If the linked machine is gone or not loaded, it waits 10 s and checks again; the job stays.
- If it cannot reach the machine for 15 s, it waits 10 s and tries again.
- A leashed operator does not work, and a panicking one stops working.
- An operator needs the wrench in its main hand. The job is lost with it.
- A [Belt Drive](/items/belt_drive) with a belt beside it always counts as having work, so its operator cranks all day.
- A part fetched for a previous machine is dropped if the current machine needs another item.

## Advancements

| Advancement | Requirement |
|---|---|
| Grease Monkey | Have a Copper Wrench |
| Turn the Crank | Link an operator to a machine |
| Keep It Moving | Be within 48 blocks when a Belt Drive starts running |
| Pick and Place | Finish marking a Grabber Arm with at least one input and one output |
| Maintenance Crew | Be within 48 blocks when an operator finishes a repair |

## Data values

| Name | Value |
|---|---|
| Job name key | `job.bettercoppers_automatisation.operator` |
| Attachment | `bettercoppers_automatisation:operator` (boolean), set when a player hands over a wrench |
| Attachment | `bettercoppers_automatisation:machine` (block position), the linked machine |
| Attachment | `bettercoppers_automatisation:carried_part` (item), the spare part being carried |

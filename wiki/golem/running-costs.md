---
title: Running costs
---

# Running costs

**Running costs** are what a paying job uses up: [energy](#energy), [rust](#rust) and [tool wear](/golem/tools). A paying job is every [job](/jobs/) except [sorter](/jobs/sorter) and [courier](/jobs/courier), which are free. Those two, and jobless golems, have no energy to spend and gain no rust from work. All golems still weather over time as in [vanilla](https://minecraft.wiki/w/Copper_Golem).

## Energy

A golem has a maximum of **1000** energy and starts full. The status message shows it as a percentage. It only spends energy while doing the work in this table. Walking costs nothing.

| Job | Action | Energy |
|---|---|---|
| [Miner](/jobs/miner) | Digging a block | 2 |
| [Miner](/jobs/miner) | Placing a block | 1 |
| [Farmer](/jobs/farmer) | Harvesting a block | 3 |
| [Farmer](/jobs/farmer) | Planting a seed | 3 |
| [Forester](/jobs/forester) | Felling a log | 3 |
| [Forester](/jobs/forester) | Planting a sapling | 2 |
| [Fisher](/jobs/fisher) | Each catch | 15 |
| [Animal keeper](/jobs/animal-keeper) | Feeding an animal | 10 |
| [Crafter](/jobs/crafter) | Each craft | 15 |
| [Operator](/jobs/operator) | Every 1 second (20 ticks) of cranking | 1 |

### Charging

Golems rest at a [Charging Pad](/items/charging_pad) (powered by redstone pulses) or, with the add-on, a [Charging Station](/items/charging_station). A golem heads for one when:

- it is dark outside (night, or a thunderstorm), even with full energy, or
- it ran out of energy.

Golems with a paying job do not work while it is dark outside. A golem looks within 16 blocks horizontally and 6 blocks vertically of its [home](/start/homes-and-status), or of its [workplace](/jobs/operator) for an operator. The nearest free one wins. A free spot has two blocks of open space above it, with no other golem standing on it. Each one fits one golem, so build one per golem. If none is free or none is reachable, the golem looks again every 10 seconds (200 ticks) and waits.

| | Charging Pad | Charging Station |
|---|---|---|
| Energy | 8 per redstone pulse | 4 per tick |
| Fastest full charge | 25 seconds, on a 4 tick clock | 12.5 seconds |
| Needs a clock | Yes | No |

**After running empty**, a golem stays on the spot until it is full, and does not work until then. If the energy has not changed for 10 seconds (200 ticks), it goes back to work with whatever it has; with none at all, it keeps waiting. A golem that only came for the night leaves in the morning.

**Sleeping through the night** charges every loaded paying golem in the dimension to full if a free Charging Pad or Charging Station lies within reach of its home or workplace. Each spot charges one golem. Golems already full do not take a spot, and no redstone clock is needed.

A golem on an errand, or leashed, does not go to charge.

## Rust

Rust is a golem's vanilla weathering stage. It advances in two ways:

- As in [vanilla](https://minecraft.wiki/w/Copper_Golem), over time, for every golem.
- Through work, for golems with a paying job: every **3000 energy** spent adds one stage. That is three full charges of work. With the add-on, [Copper Coating](/items/copper_coating) stops this for its next 3000 energy.

| Stage | Works at | Status |
|---|---|---|
| Unaffected | 100% | Can work |
| Exposed | 85% | Can work |
| Weathered | 70% | Can work |
| Oxidized | none | Does not work |
| Waxed (any stage) | none | Does not work |

The percentage scales walking speed and how long each piece of work takes. With [Gold Gearwork](/items/gold_gearwork) from the add-on, it is multiplied by 1.3 for its duration. An oxidized golem still walks errands at 50% speed.

This table is for paying jobs. A sorter or courier does its job at full speed at every stage, waxed or not.

A golem that does not work also gains no rust from work. An oxidized golem can [turn into a statue](https://minecraft.wiki/w/Copper_Golem) as in vanilla, dropping what it carries.

### Waxing

Waxing with [honeycomb](https://minecraft.wiki/w/Honeycomb) is vanilla. A waxed golem with a paying job stops working until the wax is removed.

## Scraping

**Scraping** removes one stage of rust with an axe. The wax comes off first on a waxed golem.

**By hand.** Any axe on a rusty or waxed golem scrapes it, exactly as in vanilla, with or without sneaking. On a clean golem, sneak + right-click with a copper axe makes it a [forester](/jobs/forester), and any other axe is refused. A plain right-click on a golem with a job never throws the tool out, as described under [The vanilla rule](/start/vanilla-rule).

**By axe dispenser.** A [dispenser](https://minecraft.wiki/w/Dispenser) holding any axe (wooden, stone, copper, iron, golden, diamond or netherite) scrapes the golem in the block in front of it on every redstone pulse, one stage per pulse. This is the one automatic thing that happens to every golem, including ones no player touched, because the player built the dispenser on purpose.

- The axe stays in the dispenser and takes 1 durability per scrape.
- A waxed golem loses only its wax on that pulse.
- Scraping off a stage this way resets the golem's progress towards its next stage of rust to zero. A hand scrape does not.
- With nothing to scrape, the dispenser dispenses the axe as usual.

<!-- MEDIA: running-costs-axe-dispenser.png | screenshot | A dispenser facing a one-block spot where a working copper golem stands, with a lever or redstone line next to the dispenser -->

## Tools

Job tools take [tool wear](/golem/tools) and break. See [Tools](/golem/tools).

## Advancements

| Advancement | How |
|---|---|
| Recharged | Be within 48 blocks when a golem is charged to full on a Charging Pad |
| Good as New | Be within 48 blocks when a golem is scraped by an axe in a dispenser |

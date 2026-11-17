# Better Coppers

The language of a mod in which copper golems take on jobs for the player. Docs, handbook and new code use these terms.

## Golems

**Job**:
Any work a player assigned to a golem: sorter, miner, farmer, forester, fisher, animal keeper, beekeeper, crafter, courier, or an add-on job such as operator. Only a player can change a golem's job.
_Avoid_: Occupation, role

**Jobless golem**:
A golem with no job. It sorts like vanilla, even if it has a home.
_Avoid_: Vanilla golem

**Untouched golem**:
A golem no player has acted on. Every untouched golem is jobless; a jobless golem with a home is not untouched.
_Avoid_: Unsettled golem

**Paying job**:
A job with running costs, which is every job except sorter and courier.
_Avoid_: Worker job, working golem

**Job item**:
The item a player hands a golem to give it a job, such as a bundle, copper hoe or fishing rod.
_Avoid_: Tool, when the item is a bundle or crafting table

**Lost tool**:
A job tool that broke in a golem's hand. The golem keeps its job and looks for a replacement.

**Load**:
Everything a golem with a paying job carries for that job: its pouch, its supplies and the contents of its chained Copper Chest Minecart.

**Pouch**:
What a golem has gathered and is bringing home.
_Avoid_: Loot

**Supplies**:
Materials a golem holds to use in its job, such as a miner's rails or a farmer's seeds.

**Backed up**:
A golem with a paying job that has stopped working because its home chest cannot take what it carries.

**Eyes**:
A golem's upgrade from a block of iron, gold or diamond, which widens its work radius.
_Avoid_: Upgrade

**Trim**:
An armour trim worn by a golem, purely for looks.

## Sorting

**Reserved chest**:
A chest or barrel with item frames on it, which only takes the items its frames show. A frame showing a bundle stands for what is in the bundle; one showing a furnace, smoker or blast furnace stands for what that block can cook.

**Working stock**:
What a sorter leaves in a shared home chest so the other golem's job can carry on, including spare job tools.
_Avoid_: Reserve

**Quota**:
How much of an item a chest should hold. Golems deliver only up to it, couriers refill up to it, and golems take only what is above it.
_Avoid_: Stock-keeping, stock level

**Quota Slip**:
The item that lists the quotas of a chest, by hanging in an item frame on it.

**Decoy**:
A chest a sorter looks into before making its delivery.

## Places and links

**Link**:
The player's act of tying a golem to a place, started by sneak + right-clicking the golem with an empty hand.

**Home**:
The copper chest a golem is linked to.
_Avoid_: Anchor, source, base

**Workplace**:
The place an add-on job works at instead of a home, such as an operator's machine.
_Avoid_: Home

**Route**:
A courier's pair of containers: pickup and destination, and its stops. A golem with a route has no home.
_Avoid_: Errand, path

**Stop**:
A spot on a route that the player set, which a courier walks past on its way, in order, and back in reverse.
_Avoid_: Waypoint, checkpoint

**Errand**:
Temporary work that pauses whatever the golem was doing, such as repairing an iron golem.
_Avoid_: Route, job

**Ride**:
A golem's trip by empty minecart or boat towards a far-off target.

**Chain**:
The iron or copper chain that makes a golem tow a Copper Chest Minecart.
_Avoid_: Tow rope, leash

**Copper Chest Minecart**:
A minecart with a copper chest, towed by a golem to carry more.
_Avoid_: Cart chest, Minecart with Copper Chest

## Running costs

**Running costs**:
What a paying job uses up: energy, rust and tool wear.
_Avoid_: Upkeep

**Energy**:
A golem's charge for working, restored at a Charging Pad or Charging Station.

**Rust**:
A golem's vanilla weathering stage, which work advances and which slows or stops it.
_Avoid_: Oxidation, wear

**Tool wear**:
Damage to a job tool from use, until it breaks.
_Avoid_: Wear, on its own

**Charging Pad**:
The base mod's block where a golem recharges, powered by redstone pulses.
_Avoid_: Charge pad, pad

**Scraping**:
Removing one rust stage with an axe, by hand or from a dispenser.

## Mining

**Mine Entrance**:
The frame a player builds near a miner's home: two wooden fence posts, 2 high, with a beam of 3 planks across their tops, exactly like a mineshaft support. A mine grows from it, into the hill.
_Avoid_: Entry, mine door, portal, frame (on its own)

**Mine**:
Everything miners have dug from one Mine Entrance, shared by all of them and kept with the world.
_Avoid_: Mineshaft (that is vanilla's structure), dig site

**Mine Section**:
One piece a miner digs at a time, such as a crossing, a straight, a small hall, a corner, stairs down or up, or a dead end. A mine grows section by section.

**Section Kind**:
One sort of Mine Section, with its own Section Weight: Straight, Corner, Crossing, Small Hall, Stairs Down, Stairs Up, Dead End, or one an add-on registered.
_Avoid_: Section type, variant, piece; junction, corridor, room, ramp (for the built-in kinds)

**Section Template**:
The cells, supports, torches, rails and Open Ends of a Section Kind, relative to the Open End it grows from.
_Avoid_: Schematic, blueprint, structure (that is vanilla's)

**Section Weight**:
How likely a Section Kind is when a miner rolls the next section for an Open End.
_Avoid_: Chance, rarity, frequency

**Open End**:
The spot at the edge of a Mine Section where the next section can start, with the way it faces.
_Avoid_: Exit, socket, connector

**Dead End**:
A Mine Section with no Open End, which ends a branch on purpose.
_Avoid_: Stub, cap

**Blocked**:
Of a Mine Section: it can't be dug where it was rolled, because of lava, water, a cave, the surface, another tunnel, a structure, hard rock or the mine's limits.
_Avoid_: Obstructed, invalid

**Closed End**:
An Open End no miner will grow from again, marked by a Barrier. A miner closes it when no Mine Section fits, a player closes it with a fence.
_Avoid_: Blocked end, sealed end, cap

**Barrier**:
The fence posts, and the sign if the miner has one, set across a Closed End. Breaking its middle fence opens the end again.
_Avoid_: Wall, blocker

**Claim**:
A miner's hold on the one Mine Section it is digging, so that no other miner digs it. It lapses when the miner leaves, quits or stops working for a while.
_Avoid_: Lock, reservation, assignment

**Mine Path**:
The way a miner walks through its mine's finished sections, from the Mine Entrance to the section it works on and back.
_Avoid_: Route, trail (both are courier terms), path (on its own)

**Ore Vein**:
Ore blocks touching face to face, which a miner digs out once it has dug next to one of them.
_Avoid_: Cluster, deposit, ore body

**Mine Range**:
How far, horizontally, from its Mine Entrance a mine may grow; a game rule.
_Avoid_: Reach (that is the Ore Vein's), radius, max distance, mine size

**Depth Floor**:
The lowest height a miner digs to; a game rule.
_Avoid_: Min depth, bottom, depth limit

**Entrance Search Radius**:
How far from its home chest a miner looks for a Mine Entrance; a game rule.
_Avoid_: Search range, entrance range

## Danger and protection

**Hunted**:
What happens to a golem with a job when monsters target it. Jobless golems are not hunted.

**Guard**:
An iron golem that stays near golems with a job and attacks what hunts them.

**Repair**:
A copper golem healing an iron golem with iron ingots, done as an errand.

## Fishing

**Float**:
The bobber a fisher casts.
_Avoid_: Bobber

## Guidance for players

**Golem Handbook**:
The in-game book that explains the mod chapter by chapter.
_Avoid_: Guide

**Guide**:
The advancement tab that leads the player from step to step.
_Avoid_: Handbook, quest log

## Automatisation add-on

**Operator**:
A golem with a Copper Wrench whose job is to crank and repair one machine.
_Avoid_: Mechanic

**Machine**:
An add-on block that only moves while an operator cranks it, such as a press, saw or belt drive.

**Crank**:
The handle on a machine that an operator turns. It takes the place of a power network.

**Jam**:
A machine's stopped state from wear, cleared by an operator's repair.

**Charging Station**:
The add-on's block where a golem recharges with no redstone clock, faster than a Charging Pad.
_Avoid_: Charge pad, Charging Pad

**Upgrade**:
An add-on item that improves a golem, such as Iron Plating or Gold Gearwork. Eyes are not upgrades.
_Avoid_: Modifier

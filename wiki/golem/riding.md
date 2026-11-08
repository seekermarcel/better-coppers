---
title: Minecarts and boats
---

# Minecarts and boats

A **ride** is a trip by empty minecart or boat that a [copper golem](/golem/copper-golem) with a [job](/jobs/) takes on its own when the place it is walking to is far away. No player has to tell it to: the golem looks for a suitable vehicle whenever it has a long walk ahead. A golem towing a [Copper Chest Minecart](/items/copper_chest_minecart) does not ride; it always walks.

Golems without a job walk, as in [vanilla](/start/vanilla-rule).

<!-- MEDIA: golem-riding-minecart.mp4 | clip | A copper golem with a job climbing into an empty minecart on a long straight rail line, pushing it along and getting off shortly before its destination -->

## When a golem rides

A golem looks for a ride once every second (20 ticks). It starts a ride only if all of these are true:

| Condition | Detail |
|---|---|
| Has a job | Any job, including [sorter](/jobs/sorter) and [courier](/jobs/courier) |
| Is walking somewhere | The ride is planned from the golem's current walk target |
| The target is far | At least 24 blocks away, counted horizontally |
| Not leashed | A leashed golem never rides |
| Not towing | A golem with a chained cart stays on foot, because the cart would not keep up |
| Not panicking | A panicking golem does not start a ride |
| A suitable vehicle is near | See below |

## Suitable vehicles

The golem searches a box reaching 8 blocks out from itself in every direction, and picks the closest vehicle that qualifies. The vehicle must be empty.

| | Minecart | Boat |
|---|---|---|
| Which | Plain minecarts only. Chest, hopper, furnace, TNT and command block minecarts, and the Copper Chest Minecart, are not used. | Any boat or raft, with or without a chest |
| Where | On a straight rail, flat or sloped. A cart on a curve is not used. | In water, or directly above water |
| Direction | The rail must run roughly towards the goal, within about 53 degrees of it either way | At least 3 of the 4 points 3, 6, 9 and 12 blocks ahead towards the goal must have water at that block or the one below it |

The golem walks up to the vehicle and climbs in once within 1.8 blocks. It has 10 seconds (200 ticks) to get there. If time runs out, or another passenger takes the vehicle first, that vehicle counts as a dead end.

**Dead ends.** A vehicle that got the golem nowhere is not tried again for 60 seconds (1200 ticks) for any goal within 16 blocks of the same goal. A ride counts as a dead end if the golem came less than 8 blocks closer before it stalled.

## Riding

The golem keeps following its job's walk target during the ride, so the destination updates as the target changes.

**Minecart.** While standing, the golem pushes the cart along the rail in the direction closer to the goal. Once the cart moves, it keeps pushing in the direction of travel, adding 0.03 blocks per tick each tick until the cart reaches 0.35 blocks per tick (7 blocks per second). Because it pushes along the current motion, a moving cart follows curves. A cart that comes to rest on a curve gets no push, as a curve has no straight direction to push along.

**Boat.** The golem paddles straight at its goal, turning at most 5 degrees per tick, and adds 0.04 blocks per tick of speed only while facing within 45 degrees of the goal. It paddles until the boat runs ashore, so it gets out onto land.

## Getting off

| Vehicle | Gets off when |
|---|---|
| Minecart | Within 10 blocks of the goal, measured horizontally |
| Boat | Within 1.5 blocks of the goal |
| Either | The vehicle is destroyed, the golem is leashed or it loses its job |
| Either | The distance to the goal has not shrunk by at least 0.25 blocks for more than 3 seconds (60 ticks), which is the end of the track, the shore, or the wrong way |

When a golem gets off a cart it brakes the cart to a stop. If a rolling cart scoops up the same golem within 5 seconds (100 ticks), the golem steps out again.

A working golem that ends up alone in a minecart or boat by bumping into it uses it if the goal is at least 24 blocks away and the vehicle leads that way. Otherwise it climbs out and walks. A jobless golem may stay parked in a boat, as in vanilla.

## Towing a cart

A [Copper Chest Minecart](/items/copper_chest_minecart) is pulled along behind a golem on a [chain](/items/copper_chest_minecart#usage). It is not a ride: the golem walks, and the cart follows its footsteps, on or off rails. See the cart's article for chaining, capacity and when a chain snaps.

## Advancements

| Advancement | How |
|---|---|
| All Aboard | Be within 48 blocks of a golem as it climbs into a minecart or boat |

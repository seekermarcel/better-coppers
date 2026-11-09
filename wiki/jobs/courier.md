---
title: Courier
infobox:
  icon: barrel
  rows:
    Job item: None, a route between two chests
    Needs home: "No"
    Running costs: None
    Work radius: Any distance in the same dimension
    Mod: Better Coppers
---

# Courier

A **courier** is a [copper golem](/golem/copper-golem) that carries everything from one chest to another, however far apart they are. On the road it keeps the chunks around it loaded, so a route keeps working while no player is near. It has a route instead of a [home](/start/homes-and-status), and it pays no [running costs](/golem/running-costs).

## Setup

1. Sneak + right-click the golem with an empty hand. It glows.
2. Within 30 seconds, right-click a plain chest, trapped chest or barrel, or sneak + right-click a copper chest. The golem picks things up here. A plain click on a copper chest would make it the golem's home instead.
3. Within 10 minutes, right-click the destination: any chest, copper chest or barrel, in the same dimension.

There is no distance limit, but both ends must be in the same dimension and must not be the same container. The golem may be far away and unloaded when the route is set; it takes the route as soon as it loads, unless it has since been given another job. A route set this way is lost if the server restarts before the golem loads.

A golem with a job takes no route, except a courier. A courier given a new route keeps what it carries and delivers it on the new route.

<!-- MEDIA: jobs-courier-route.png | screenshot | A courier with items in its hand and a chained Copper Chest Minecart walking along a path, a barrel far behind it and a chest in the distance -->

## Behaviour

The courier walks to the pickup chest, takes what it can carry, walks to the destination, puts it all in, and walks back. It never gives up. It works at night, and rust does not slow it down.

| Stage | Detail |
|---|---|
| Pickup | Up to 16 items in hand from the first stack it may take, or 32 with a [Copper Satchel](/items/copper_satchel). 60 ticks (3 seconds) at the chest |
| Nothing to take | Waits 10 seconds and looks again |
| Delivery | Puts in everything it carries. 60 ticks at the chest |
| Destination full | Waits 10 seconds and tries again, keeping the rest |
| Chest gone or locked | Waits, looking again every 10 seconds, until the route is fixed |
| Stuck for 10 seconds | Shakes its head, walks to a random spot up to 10 blocks away for at most 5 seconds, then tries again |

### Frames on the pickup chest

Item frames on the pickup chest limit what is taken, with the same rules as for a [reserved chest](/jobs/sorter#reserved-chests). Without any, everything is. Other frames on the destination change nothing.

### Refilling a chest

A [Quota Slip](/items/quota_slip) on the destination changes the courier's round. It takes effect once the courier has been at the destination.

| Stage | Detail |
|---|---|
| Waiting | Stands at the destination and looks again every 10 seconds, until something on the slip is below its amount. It loads no chunks while it waits |
| Fetching | Walks to the pickup chest and takes only what was missing |
| Delivery | Puts in only what the slip allows |
| More than fits | Takes the rest back to the pickup chest, and waits there if that is full |
| Nothing to fetch | Walks back to the destination and looks again |

A Quota Slip on the pickup chest makes the courier leave that amount behind, so two couriers running in opposite directions cannot pass the same items back and forth.

A courier on such a route no longer empties the pickup chest. If that chest is another golem's home, that golem can end up [backed up](/golem/running-costs).

### Towing a cart

With a chained [Copper Chest Minecart](/items/copper_chest_minecart), the courier also loads everything it may take into the cart until no slot is empty, and unloads all of it at the destination. A golem towing a cart always walks. Without one, it may ride a [minecart or boat](/golem/riding) for trips over 24 blocks.

### Chunk loading

A courier keeps its chunks loaded while it carries anything, while it is fetching for a quota, or while it is more than 12 blocks from the pickup chest. Standing at the pickup with nothing to carry, it does not load chunks.

| Property | Value |
|---|---|
| Ticket radius | 3 chunks around the golem's chunk |
| Refreshed | Every 10 ticks |
| Expires | 40 ticks (2 seconds) after the last refresh |

The golem's own chunk and the chunks next to it tick entities, so it does not walk into a frozen chunk. As with a thrown ender pearl, the ticket keeps the dimension active.

## How a route ends

| Action | Result |
|---|---|
| Linking the golem to a home copper chest | The route ends. Cargo in its hand stays with it, for vanilla sorting to deliver |
| Ending the job (sneak + right-click twice with an empty hand within 5 seconds) | The route ends. The player gets what it holds, and a chained cart unchains and stays where it is, still loaded |

## Running costs

None. A courier uses no energy, its work does not rust it, and it wears no tool. [Eyes](/golem/eyes) are refused, since a courier has no radius.

## Limits and edge cases

- Both ends must be chests, trapped chests, copper chests or barrels. Ender chests and shulker boxes do not count.
- Linking the same container at both ends, including two halves of a double chest, is refused.
- A courier carrying anything counts as carrying cargo, so a hoe in its hand is delivered and does not make it a farmer.
- To [repair](/golem/guards-and-repairs) an iron golem, a courier finishes its delivery first.
- A leashed or panicking courier does not work.

## Advancements

| Advancement | How | ID |
|---|---|---|
| Special Delivery | Set up a courier route | `bettercoppers:guide/courier` |
| Long Haul | Set up a route of 500 blocks or more | `bettercoppers:guide/long_haul` |
| Full Employment | Give golems all nine jobs | `bettercoppers:guide/full_employment` |

## Data values

| Name | Value |
|---|---|
| Status name key | `job.bettercoppers.courier` |
| Chunk ticket type | `bettercoppers:courier` |

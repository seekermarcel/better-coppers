---
title: Guards and repairs
---

# Guards and repairs

A **guard** is an [iron golem](https://minecraft.wiki/w/Iron_Golem) that stays near [copper golems](/golem/copper-golem) with a [job](/jobs/) and attacks what [hunts](/golem/monsters) them. A **repair** is the reverse: a copper golem heals an iron golem with iron ingots, as an [errand](#repairs) that pauses its work.

Every iron golem guards, including ones in villages. The behaviour only shows around copper golems with a job.

<!-- MEDIA: guards-defending.mp4 | clip | An iron golem walking over to a working copper golem and attacking a zombie that is chasing it -->

## Guards

### Staying close

| Number | Value |
|---|---|
| Search radius for copper golems with a job | 32 blocks |
| Walks over if the nearest one is further than | 10 blocks |
| Stops when within | 5 blocks |

An iron golem with a target does not stroll over; fighting comes first. If it has no target, it checks for a golem to stay near at random moments.

### Defending

An iron golem goes for any monster within 16 blocks of itself that either:

- is targeting a copper golem with a job, or
- hit a copper golem with a job in the last 5 seconds (100 ticks).

It picks the nearest such monster. Only hostile monsters count. Creepers are excepted, as they are for iron golems in vanilla.

## Repairs

A repair is an **errand**: temporary work that pauses a golem's [job](/jobs/). A golem on an errand does no work, spends no [energy](/golem/running-costs) and causes no [tool wear](/golem/tools).

### Who repairs

A copper golem with a [home](/start/homes-and-status) or a [route](/jobs/courier), including a jobless golem with only a home. A golem with neither does not repair.

It does not start if it is leashed or riding. A [sorter](/jobs/sorter), a courier or a home-only golem carrying something in its hand finishes the delivery first; iron ingots in a home-only golem's hand count as supply instead.

Each iron golem gets one copper golem at a time. Another golem can take over only if the first is gone or no longer on the errand.

### When

A golem looks every 2 seconds (40 ticks) for an iron golem within 16 blocks that is missing at least 30 health. The iron golem has 100 health as in vanilla. It picks the nearest one that is not taken. Repairs continue until less than 12.5 health is missing.

### Iron supply

| Order | Source |
|---|---|
| 1 | Iron ingots it already carries from an earlier errand |
| 2 | Iron ingots in its hand (golem with only a home) |
| 3 | Its home chest |
| 4 | A courier's pickup chest |
| 5 | The nearest chest or barrel with iron within 16 blocks horizontally and 6 blocks vertically of the copper golem |

Without iron anywhere, no repair starts. A locked chest is not used.

The golem takes up to 4 ingots per trip, enough to cover what the iron golem is missing, rounded to the nearest ingot.

### The repair

It walks to within 2.5 blocks of the iron golem. Each ingot takes 1.25 seconds (25 ticks), heals 25 health and plays the vanilla repair sound. If the iron golem still needs more once the ingots are gone, the golem fetches more.

Walking is slowed by [rust](/golem/running-costs#rust). If the golem makes no headway for 15 seconds (300 ticks), it gives up on that iron golem.

### Leftover iron

| Golem | What happens |
|---|---|
| With a job | Keeps it for the next repair |
| Without a job | Back into its hand, or into the home chest if the hand is full |
| Job ends | Handed to the player |
| Dies or turns into a statue | Dropped |

## Advancements

| Advancement | How |
|---|---|
| Bodyguard | Be within 48 blocks when an iron golem goes for a monster that is after a copper golem |
| Field Medic | Be within 48 blocks when a copper golem repairs an iron golem |

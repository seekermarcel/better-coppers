---
title: Homes and status
---

# Homes and status

A **home** is the copper chest a [copper golem](/golem/copper-golem) is linked to. A golem with a job brings what it makes there and fetches its supplies from there. The **status message** reports a golem's job, place and condition. A golem has at most one home. A [courier](/jobs/courier) has a [route](/jobs/courier) instead, and an [operator](/jobs/operator) has a machine as its **workplace**.

## Linking

A **link** ties a golem to a place. It starts with **sneak + right-clicking the golem with an empty hand**, which picks the golem.

1. The golem glows for 30 seconds and the chat shows "Golem picked" with its status.
2. **Right-click a copper chest.** The chest becomes the golem's home, a dotted line of copper particles appears for everyone nearby, and a message above the hotbar shows the chest's coordinates.

To move a golem to another chest, link it again. The old home is replaced.

Every player has one pick at a time. Picking another golem drops the previous pick. A pick is lost when the server restarts.

### Link range

| Rule | Value |
|---|---|
| Distance from the **golem** to the chest | 32 blocks at most, measured in a straight line from the golem to the centre of the chest |
| Distance from the player to the chest | Only normal reach; the player can be far from the golem |
| Time to pick the chest | 600 ticks (30 seconds) |
| Dimension | The chest must be in the golem's dimension. A click in another dimension drops the pick. |
| Golem loaded | The golem must be loaded. If it is not, the pick is dropped and the click acts as normal. |

A chest further away shows "That chest is too far from the golem (more than 32 blocks)." The pick is dropped and the chest does not open.

### Cancelling

| Action | Result |
|---|---|
| Right-click the picked golem with an empty hand, if it has a job | Cancels the pick |
| Sneak + right-click the picked golem with an empty hand, if it is jobless | Cancels the pick and shows "Link cancelled." |
| Pick another golem | Drops the first pick |
| Wait 30 seconds | The pick runs out |

The golem stops glowing when the pick ends. See [Controls](/start/controls) for every click.

### What a click on a block does

| Clicked block while a golem is picked | Jobless golem or courier | Golem with any other job |
|---|---|---|
| Copper chest | Sets the home | Sets the home |
| Copper chest, while sneaking | Starts a route | Sets the home |
| Chest, trapped chest or barrel | Starts a route | Refused, with a message; the pick is dropped |
| Machine (add-on) | Links an operator; other golems get a message | Links an operator; other golems get a message |
| Anything else | Acts as normal; the pick stays | Acts as normal; the pick stays |

Starting a route sets a new time limit of 12,000 ticks (10 minutes) to reach the other end. See [Courier](/jobs/courier). Setting a home on a courier ends its route, and whatever it carries stays in its hand. Any item may be held while clicking the block.

## Status message

A golem's status appears in two places: in the chat when it is picked, and above the hotbar on a plain right-click with an empty hand on a golem with a job. A plain right-click on a jobless golem shows nothing, because that click stays [vanilla](/start/vanilla-rule).

The form depends on the job:

| Golem | Message |
|---|---|
| Paying job | `Job, place, energy N%, rust: state` and any extras |
| Sorter or courier | `Job, place` |
| Jobless | `No job, place` |

Extras are only shown on [paying jobs](/golem/running-costs). Eyes or upgrades on a sorter, courier or jobless golem do not appear in its status.

| Part | Values |
|---|---|
| Job | The job's name, or `No job` |
| Place | `home X Y Z`, or `workplace X Y Z` for an operator's machine, or `route X, Y, Z -> X, Y, Z` for a courier, or `no home` |
| Energy | Energy as a percentage of 1000, rounded down |
| Rust | See below |

| Rust text | Meaning |
|---|---|
| `none` | Unaffected. Full speed. |
| `a little` | Exposed. 85 % speed. |
| `a lot` | Weathered. 70 % speed. |
| `fully oxidized (cannot work)` | Oxidized. Does no paying work and can turn into a statue. |
| `waxed (cannot work)` | Waxed, at any stage. Does no paying work. |

| Extra | When | Shown as |
|---|---|---|
| Lost tool | The job tool broke and no new one was found | `looking for a new <tool>` |
| [Eyes](/golem/eyes) | The golem has eyes | `eyes 150%`, `eyes 200%` or `eyes 300%` |
| [Iron Plating](/items/iron_plating) (add-on) | Fitted | `plating N%` |
| [Copper Coating](/items/copper_coating) (add-on) | Fitted | `coating N%` |
| [Gold Gearwork](/items/gold_gearwork) (add-on) | Fitted | `gearwork N%` |
| [Copper Satchel](/items/copper_satchel) (add-on) | Fitted | `satchel` |

<!-- MEDIA: start-status-message.png | screenshot | The chat after sneak + right-clicking a farmer with an empty hand, showing the full "Golem picked" line with job, home coordinates, energy and rust, with the glowing golem in view -->

## Seeing homes

While a player sneaks, particles show where nearby golems belong. Only that player sees them. They are refreshed every 10 ticks (0.5 seconds) and cover golems within 24 blocks of the player.

| Golem | What is shown |
|---|---|
| Has a home or workplace | A dotted line of copper dust from the golem to the chest or machine |
| Courier | A line to each end of its route that is within 40 blocks of the golem |
| Has a job but no home, workplace or route | A puff of smoke over its head |
| Jobless, no home | Nothing |

<!-- MEDIA: start-home-lines.png | screenshot | A sneaking player's view of several golems with copper particle lines running to their home chests, and one golem with a job and no home puffing smoke -->

## Home rules

- The home must be a **copper chest**, at any weathering stage, waxed or not, single or double. A plain chest, trapped chest or barrel cannot be a home.
- A home is a position. Any copper chest standing at that position counts as the golem's home, including a new one placed after the old one was broken.
- Several golems can share a home. A [sorter](/jobs/sorter) leaves [working stock](/jobs/sorter) for the other jobs at a shared chest.
- A golem with a job needs a home to work, except a courier and an operator.
- A jobless golem can have a home. It then still [sorts like vanilla](/start/vanilla-rule).
- Linking a home to a golem with a route ends the route. Setting a route removes the home.
- With nothing to do, a golem with a home or workplace walks back once it is more than 6 blocks away horizontally. This does not apply to a courier, or to a golem being led on a lead, riding, charging on a pad or on an errand.
- Ending a job keeps the home.

A sorter or a paying job other than operator does no work without a home. The golem keeps what it holds.

## When the home is lost

The link is a remembered position, and it is not removed when the chest goes. It stays in the golem's status. A home can stop working for two reasons: the chest was broken or replaced by something else, or its chunk is not loaded.

| Effect | Result |
|---|---|
| Sorter and paying jobs other than operator | Stop working and wait |
| What the golem carries | Keeps everything |
| Rechecks | A paying job looks again every 200 ticks (10 seconds) |
| Strolling | Still walks back to the old position |
| Charging | Still looks for a [Charging Pad](/items/charging_pad) around the old position |

Work resumes as soon as a copper chest stands at the position and is loaded, or when the golem is linked to another chest. A golem's items only drop if it dies or turns into a [statue](/golem/copper-golem#statue).

An operator works at its machine and is not affected by a lost home. A **full** chest is not a lost home. The golem keeps what does not fit and tries again until there is room.

## Advancements

| Advancement | Requirement |
|---|---|
| Home Sweet Chest | Link a golem to a copper chest |

See [Advancements](/reference/advancements).

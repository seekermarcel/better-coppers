---
title: Copper Chest Minecart
infobox:
  icon: copper_chest_minecart
  rows:
    Type: Item and entity
    Mod: Better Coppers
    Stack size: 1
    Slots: 27 (54 with Cart Extension)
    ID: "bettercoppers:copper_chest_minecart"
---

# Copper Chest Minecart

A **Copper Chest Minecart**, called "Minecart with Copper Chest" in the game, is a minecart with a copper chest that a [copper golem](/golem/copper-golem) tows on a chain to carry much more. It can also be towed by a player.

## Obtaining

<Recipe id="copper_chest_minecart" />

Any copper chest works, of any age, waxed or not.

## Usage

Placed on a rail, it sits on it like a normal minecart. It can also be placed on any block, since it is towed off rails. A plain right-click opens it like a chest.

### Chaining

| Action | Result |
|---|---|
| Right-click the cart with an iron or copper chain | The cart follows you. A chain of any copper age counts, waxed or not. The chain is used up. |
| Right-click a golem with an empty hand, while a cart is chained to you within 16 blocks | The golem takes over the chain. This is the one plain click that changes a golem with a job. |
| Right-click a chained cart with another chain | The chain moves to you. No new chain is used up. |
| Sneak + right-click a chained cart | The chain comes off and drops |

Only a golem not already towing a cart can take one. Any golem can, even one without a job, but only golems with a job make use of what is inside.

<!-- MEDIA: copper-chest-minecart-towing.mp4 | clip | A copper golem walking along a path with a copper chest minecart following behind on a chain, taking a corner -->

## Behaviour

### Towing

| Number | Value |
|---|---|
| Distance behind a golem | 1.6 blocks |
| Distance behind a player | 2.5 blocks |
| Catches up by teleporting when | Further than 10 blocks away |
| Chain snaps and drops when | Further than 24 blocks horizontally, or in another dimension |

The cart follows the holder's footsteps, so it takes the same corners and narrow entrances. It steps up single blocks and stairs while chained. It moves only with the holder and never rolls away on its own; a cart stuck for more than a second jumps along the footsteps. A holder that is unloaded or offline leaves the cart waiting in place, still chained. If the holder dies, the chain drops and the cart stays where it is.

A towing golem always walks; it never takes a [ride](/golem/riding). Other golems and players can shove the cart, but its holder cannot.

### Carrying

The cart holds 27 stacks. A [Cart Extension](/items/cart_extension) from the add-on makes it a double cart with 54, and keeps what is inside.

| Job | Use |
|---|---|
| [Sorter](/jobs/sorter) | Loads everything related to the first item it picks up and delivers it in one trip |
| [Courier](/jobs/courier) | Carries a cart load per trip instead of 16 items |
| Other paying jobs | Fill the cart first, then their pouch |

When a job ends, the chain comes off and drops, and the cart stays where it is with its contents. Changing jobs keeps the cart chained.

Breaking the cart drops the cart, its contents, its chain if chained, and the Cart Extension if fitted.

## Advancements

| Advancement | How |
|---|---|
| Heavy Load | Hand a cart to a golem with an iron or copper chain |

## Data values

| | |
|---|---|
| Item ID | `bettercoppers:copper_chest_minecart` |
| Entity ID | `bettercoppers:copper_chest_minecart` |
| Translation keys | `item.bettercoppers.copper_chest_minecart`, `entity.bettercoppers.copper_chest_minecart` |
| Saved fields | `chain_holder` (holder UUID), `chain` (the chain item), `extension` (the fitted extension item) |

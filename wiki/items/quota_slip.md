---
title: Quota Slip
infobox:
  icon: quota_slip
  rows:
    Type: Item
    Mod: Better Coppers
    Stack size: 1
    ID: "bettercoppers:quota_slip"
---

# Quota Slip

A **Quota Slip** lists how much of each item a chest should hold. Its amounts are the chest's **quota**.

## Obtaining

<Recipe id="quota_slip" />

## Usage

| Action | Result |
|---|---|
| Sneak + right-click a chest or barrel | The slip lists exactly what the container holds, replacing what it listed. An empty container clears it |
| Right-click anywhere else | Opens the slip's grid |
| Hang it in an item frame on a chest or barrel | Sets that container's quota |

### The grid

The grid has 54 slots. Items placed on it are samples and stay with you.

| Click on a slot | Result |
|---|---|
| With an item on the cursor, left | Sets the slot to that item and count |
| With an item on the cursor, right | Sets the slot to one of that item |
| Empty cursor, left or right | One more or one fewer |
| Shift + left or right | 16 more or 16 fewer |
| Shift-click an item in your inventory | Adds it to the first free slot |

A slot goes up to the item's stack size. For more of an item, use more slots: the amounts add up.

## What a quota does

- Only sorters, couriers and [crafters](/jobs/crafter#quotas) read it. [Jobless golems](/golem/copper-golem) ignore it.
- A chest with a slip counts as a [reserved chest](/jobs/sorter#reserved-chests) for the items listed. A blank slip reserves nothing.
- Quotas are per item type and ignore enchantments and other item data.
- Golems deliver only up to the amount, and take only what is above it.
- A [courier](/jobs/courier#refilling-a-chest) whose destination has a slip keeps that chest filled.
- A [crafter](/jobs/crafter#quotas) crafts what its frames show up to the amount on a slip on its home chest, instead of a stack.
- If two slips on one chest list the same item, the larger amount counts.

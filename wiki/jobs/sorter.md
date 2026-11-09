---
title: Sorter
infobox:
  icon: bundle
  rows:
    Job item: Any bundle
    Needs home: "Yes"
    Running costs: None
    Work radius: 32 blocks, 8 up and down
    Mod: Better Coppers
---

# Sorter

A **sorter** is a [copper golem](/golem/copper-golem) that empties its [home](/start/homes-and-status) copper chest and puts every item where it belongs. It knows where each item goes instead of opening every chest in turn as a [vanilla](https://minecraft.wiki/w/Copper_Golem) golem does. Sorting is free: a sorter pays no [running costs](/golem/running-costs).

## Setup

1. Sneak + right-click a golem with any bundle. The golem keeps the bundle in its off hand. The bundle ends any other job first.
2. Link the golem to a copper chest: sneak + right-click it with an empty hand, then right-click the chest within 32 blocks of the golem.
3. Put the destination containers within range: chests, trapped chests and barrels.
4. Optionally hang item frames on containers to [reserve them](#reserved-chests).

A golem with a bundle and no home does nothing. A sorter already holding a bundle does not take a second one.

<!-- MEDIA: jobs-sorter-storage.png | screenshot | A sorter's storage room: one copper chest as home, several barrels and plain chests, some of them with item frames showing cobblestone, a furnace and a bundle -->

## Behaviour

A sorter empties only its own home chest, both halves of a double chest included. It takes up to 16 items from the first stack it may move, walks to where they belong, puts them in and comes back. A [Copper Satchel](/items/copper_satchel) doubles this to 32. Every chest visit, real or [decoy](#decoys), takes 60 ticks (3 seconds).

A sorter walks at full speed and works by day and by night. With nothing left to sort, or nowhere to put what it holds, it waits 7 seconds and looks again.

### Where items go

The first rule with a matching container wins. Among matches, the nearest wins.

| Order | Destination | Condition |
|---|---|---|
| 1 | [Reserved chest](#reserved-chests) | One of its frames allows the item, and it has room |
| 2 | Chest without frames | Already holds that item, and has room |
| 3 | Barrel without frames | Has room |
| 4 | Chest without frames | Is empty |

Items match by item type only, so a named or enchanted item counts as the plain item.

### Range

A sorter knows every chest, trapped chest and barrel in a box around itself, 32 blocks to each side and 8 up and down, in loaded chunks. [Eyes](/golem/eyes) widen the horizontal range to 48, 64 or 96 blocks. The vertical range stays 8. The box follows the golem, not its home chest.

Copper chests are never destinations. Locked containers are ignored.

### Reserved chests

A **reserved chest** is a chest or barrel with item frames on it. It takes only what its frames show, and those items go there before anywhere else. A frame counts for the block behind it. A frame on either half of a double chest counts for the whole chest. An empty frame reserves nothing.

| Frame shows | Allows |
|---|---|
| Any item | That item |
| <Icon id="bundle" /> A bundle | Everything inside the bundle (one of each item is enough) |
| Empty bundle | Nothing, but the container stays reserved |
| A furnace | Everything a furnace can smelt, including cobblestone, sand and logs |
| A blast furnace | Everything a blast furnace can smelt, such as ores and raw metals |
| A smoker | Everything a smoker can cook, such as raw food |

To reserve a container for furnaces or bundles themselves, put one in a bundle and frame the bundle.

A [Quota Slip](/items/quota_slip) in a frame sets how much of each listed item the container should hold. The sorter delivers only up to that amount and sends the rest to the next destination. On its home chest, it leaves that amount behind. If working stock also applies, the larger amount stays. A sorter never fetches items to fill a quota: that takes a [courier](/jobs/courier#refilling-a-chest).

### Decoys

A **decoy** is a chest a sorter looks into before making its delivery, for character. It opens 0 to 3 of them per trip, shakes its head and carries on. It picks them at random from the 6 containers closest to the real destination that do not hold the item. A decoy is never the destination or its other half. With a chained cart, the trip's decoys are spread over its deliveries.

### Working stock

A sorter leaves what another golem sharing its chest needs to keep working. This is the **working stock**. It also applies when the golems are linked to the two halves of a double chest.

| Golem sharing the chest | Left in the chest |
|---|---|
| Miner | All copper pickaxes. With a marked [Mineshaft Plan](/items/mineshaft_plan), also all torches, rails, planks and wooden fences |
| Farmer | All copper hoes, 16 of every seed |
| Forester | All copper axes, 8 of every sapling |
| Fisher | All fishing rods |
| Animal keeper | All brushes, 16 of every animal food |
| Beekeeper | All shears, 16 glass bottles |
| Crafter | All crafting tables |

Amounts count per item type across the whole chest. A sorter only takes what is above them. If several golems share the chest, the largest amount counts. A second sorter, a courier or an operator adds nothing.

### Towing a cart

A sorter with a chained [Copper Chest Minecart](/items/copper_chest_minecart) carries more per trip. At the home chest it loads, besides the stack in its hand, every stack of items **related** to the first one. It then delivers everything before going back.

Two items are related if they are the same item or their IDs share a material word. Each ID is split at its underscores.

| Rule | Words | Example |
|---|---|---|
| Shape and variant words are ignored | slab, stairs, wall, fence, gate, button, pressure, plate, door, trapdoor, sign, hanging, block, bricks, brick, tiles, tile, pillar, pane, polished, smooth, chiseled, cracked, mossy, cut, cobbled, stripped, waxed, exposed, weathered, oxidized, infested, raw, stained, glazed, ore, of, on, the, and | `stone`, `stone_slab` and `stone_bricks` are related |
| Colours are ignored | white, orange, magenta, light, blue, yellow, lime, pink, gray, cyan, purple, brown, green, red, black | All wool colours are related |
| Binding words count together with the next word | nether, end, dark, pale, sea, glow | `nether_bricks` is not related to `nether_wart` |
| Any other shared word counts | | `oak_planks` and `oak_log` share "oak"; `oak_log` and `birch_log` share "log" |

Items with durability (tools, weapons, armour) only travel with exactly the same item.

At the destination, the sorter unloads from the cart every stack of the delivered item. Into a matching chest it also unloads anything the chest already holds. Into a reserved chest it unloads everything the frames allow. When its hand is empty and the cart is not, it takes up to 16 items from the cart and finds them a destination. Working stock is respected: only items above it are loaded.

## Running costs

None. A sorter uses no energy, its work does not rust it, and it wears no tool. It keeps working at night.

## Limits and edge cases

- A sorter stops while its home chest is broken or unloaded, and waits with what it holds.
- If its destination is full when it arrives, it keeps the rest and skips that container for 30 seconds.
- If someone else has a container open, a sorter queues within 3 blocks for up to 30 seconds, then skips it for 30 seconds.
- A container it cannot path to is skipped for 30 seconds. It tries up to the 4 nearest per target.
- A leashed or panicking sorter does not work.
- To [repair](/golem/guards-and-repairs) an iron golem, a sorter finishes its delivery first.

## Advancements

| Advancement | How | ID |
|---|---|---|
| Tidy Up | Give a golem a bundle | `bettercoppers:guide/sorter` |
| Heavy Load | Hand a Copper Chest Minecart to a golem | `bettercoppers:guide/cart` |
| Full Employment | Give golems all nine jobs | `bettercoppers:guide/full_employment` |

## Data values

| Name | Value |
|---|---|
| Job item tag | `#minecraft:bundles` |
| Status name key | `job.bettercoppers.sorter` |
| Destination blocks | `minecraft:chest`, `minecraft:trapped_chest`, `minecraft:barrel` |
| Home blocks | `#minecraft:copper_chests` |

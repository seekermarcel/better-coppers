---
title: Monsters
---

# Monsters

**Monsters** in Better Coppers hunt every [copper golem](/golem/copper-golem) that has a [job](/jobs/). A golem being targeted this way is **hunted**. Jobless golems are not hunted and are treated exactly as in [vanilla](https://minecraft.wiki/w/Copper_Golem), even when they have a home.

A copper golem has 12 health (6 hearts), the vanilla value. The mod does not change it.

<!-- MEDIA: monsters-hunted-golem.mp4 | clip | A zombie walking up to and attacking a copper golem holding a copper hoe, while an unrelated copper golem without a job nearby is ignored -->

## Which monsters hunt

Any mob that hunts villagers or [iron golems](https://minecraft.wiki/w/Iron_Golem) with a normal targeting goal also hunts copper golems with a job. In the vanilla game, that is:

| Monster | Follow range (blocks) |
|---|---|
| Zombie, Husk, Drowned, Zombie Villager | 35 |
| Skeleton, Stray, Bogged, Parched, Wither Skeleton | 16 |
| Spider, Cave Spider | 16 |
| Slime, Magma Cube | 16 |
| Pillager | 32 |
| Ravager | 32 |
| Illusioner | 18 |
| Vindicator | 12 |
| Evoker | 12 |

Mobs from other mods that use the same kind of targeting goal against villagers or iron golems hunt too.

Not on the list: creepers, witches, endermen, phantoms, zombified piglins, piglins and hoglins. They do not hunt copper golems.

## How hunting works

- A monster picks its target from within its follow range, which is its vanilla [follow range](https://minecraft.wiki/w/Attribute#Attributes), and it must have line of sight.
- The golem must be alive and have a job. A sorter and a courier count; a golem with only a home does not.
- The hunt has the same priority as the monster's vanilla goal against villagers or iron golems, whichever is highest for that mob.
- There is no daylight check of the mod's own. Spiders, which vanilla leaves neutral in bright light, can hunt a golem with a job by day.
- Damage is the monster's normal vanilla attack. The mod does not change it.

A golem that panics, as vanilla golems do, pauses its work and errands until it calms down.

With the [Automatisation](/automatisation/upgrades) add-on, [Iron Plating](/items/iron_plating) takes three quarters of every hit, up to 40 damage in total.

## Protection

An [iron golem](/golem/guards-and-repairs) nearby acts as a **guard**: it stays close to golems with a job and attacks what hunts or hits them. Build one for every work site.

## Death

When a golem dies, the following land on the ground at its position:

| What | Note |
|---|---|
| Its job tool or bundle | Guaranteed vanilla equipment drop, with its remaining durability |
| Its pouch and supplies | Everything it was carrying for its job |
| Its [Mineshaft Plan](/items/mineshaft_plan) | Miners only |
| Its [eyes](/golem/eyes) block | The iron, gold or diamond block |
| Iron ingots it carried for a [repair](/golem/guards-and-repairs) | |
| Its [trim](/golem/trims) template | The material is lost |
| Add-on upgrades and job items | [Automatisation](/automatisation/) only |

A chained [Copper Chest Minecart](/items/copper_chest_minecart) stays where it is and keeps its items. Its chain drops as an item.

The same drops happen when a golem [turns into a statue](https://minecraft.wiki/w/Copper_Golem).

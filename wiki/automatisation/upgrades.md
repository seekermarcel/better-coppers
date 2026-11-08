---
title: Golem upgrades
---

# Golem upgrades

An **upgrade** is an [Automatisation](/automatisation/) item that improves one copper golem. The four upgrades are <Icon id="iron_plating" /> [Iron Plating](/items/iron_plating), <Icon id="copper_coating" /> [Copper Coating](/items/copper_coating), <Icon id="gold_gearwork" /> [Gold Gearwork](/items/gold_gearwork) and <Icon id="copper_satchel" /> [Copper Satchel](/items/copper_satchel). [Eyes](/golem/eyes) are a separate thing and not upgrades.

## Comparison

| Upgrade | Effect | Amount | Used up by | Can be stacked |
|---|---|---|---|---|
| Iron Plating | Takes 75% of every hit | 40 damage | Hits | While below 40, adding 40 |
| Copper Coating | No rust from work | 3000 energy | Energy spent | While below 3000, adding 3000 |
| Gold Gearwork | Walks and works 1.3 times as fast | 2000 energy | Energy spent | While below 2000, adding 2000 |
| Copper Satchel | Sorters and couriers carry 32 items per trip instead of 16 | Permanent | Nothing | No, one per golem |

## Fitting

Sneak + right-click a copper golem with the upgrade in the main hand. One item is used up (none in Creative mode). Any golem can be fitted; no job is needed. A plain right-click fits nothing.

An upgrade that cannot be added (already at or above its full amount, or a satchel already worn) is not used up and the click does nothing.

Sneak + right-click a golem with an empty hand to see its status, which lists fitted upgrades in percent of the full amount. A plain click on a golem with a job shows the same status. Percentages can exceed 100 after stacking.

<!-- MEDIA: automatisation-fitting.png | screenshot | The status overlay text above the hotbar after sneak + right-clicking a copper golem with an empty hand, showing plating, coating, gearwork and satchel values -->

## Iron Plating

Each hit a copper golem takes loses 75% to the plating, up to what is left, and the rest goes through. The plating's amount drops by that share rounded up, so a 1-damage hit costs 1 plating although only 0.75 was absorbed. Damage types that bypass armour are not reduced and do not use up plating. The golem plays a shield block sound while plating is left and an item break sound on the hit that uses it up.

## Copper Coating

While any coating is left, spending energy does not advance [rust](/golem/running-costs) progress. Each point of energy spent uses up 1 coating, so a coating lasts 3000 energy. For an [operator](/jobs/operator), that is 3000 s (50 minutes) of cranking. Coating is only used up by energy spending, so it never runs out on a golem with no [paying job](/golem/running-costs).

## Gold Gearwork

While any gearwork is left, the golem's [speed](/golem/running-costs) is multiplied by 1.3. Speed affects walking and the timed work of paying jobs. A [machine](/automatisation/machines) does not run faster for it, so an operator only gets the walking benefit. Gearwork is used up by energy, 1 per point spent: 2000 s (33 minutes 20 seconds) of cranking for an operator. Sorters and couriers neither use speed nor spend energy, so it has no effect on them and does not run out.

Coating and gearwork are used up together when both are fitted.

## Copper Satchel

Doubles the golem's carry limit per trip from 16 to 32 for a [sorter](/jobs/sorter) or [courier](/jobs/courier). It does nothing for other jobs. It is permanent.

## How upgrades are lost

| Event | Iron Plating, Copper Coating, Gold Gearwork | Copper Satchel |
|---|---|---|
| Used up | Removed when the amount reaches 0 | Never used up |
| Golem dies or turns into a statue | Lost | Drops as an item |
| Job ends | Kept | Kept |

Only the satchel can be recovered.

## Advancements

| Advancement | How to get it |
|---|---|
| Kitted Out | Fit a golem with any upgrade |
| Fully Loaded | Have one golem with all four upgrades at once |

## Data values

Upgrades are stored in the golem's attachment `bettercoppers_automatisation:upgrades`, with the fields `plating`, `coating`, `gearwork` (integers) and `satchel` (boolean).

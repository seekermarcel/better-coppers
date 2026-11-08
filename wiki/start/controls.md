---
title: Controls
---

# Controls

The **controls** of Better Coppers are right-clicks on a [copper golem](/golem/copper-golem), with or without sneaking. The rule is simple: **a plain click is safe, and giving a golem anything takes sneak + right-click**. The one exception is handing over a chained [Copper Chest Minecart](/items/copper_chest_minecart).

"Golem with a job" means any [job](/jobs/), sorter and courier included. A **jobless golem** has none, even if it has a home. Spectators cannot interact with golems.

## Plain right-click

| Held in main hand | Jobless golem | Golem with a job |
|---|---|---|
| Nothing, while leading a chained cart (golem tows no cart yet) | Hands the cart over | Hands the cart over |
| Nothing | [Vanilla](https://minecraft.wiki/w/Copper_Golem): the golem throws what it holds to the player | Shows its [status](/start/homes-and-status#status-message) above the hotbar, takes nothing, and cancels this player's pick on it |
| Shears | Vanilla: takes the flower off its head, if it has one | The same |
| Honeycomb | Vanilla: waxes it | The same. A waxed golem with a [paying job](/golem/running-costs) stops working. |
| Axe, golem rusty or waxed | Vanilla: scrapes one stage, or the wax | The same |
| Axe, golem clean | Nothing | Nothing |
| Any other item | Vanilla | Nothing from the mod; vanilla items such as leads and name tags still work |

**The empty off hand.** When a main hand item does nothing, vanilla tries again with the off hand, and an empty off hand counts as an empty hand. On a jobless golem this throws its held item to the player, as in vanilla. On a golem with a job the mod stops that throw, so the tool cannot be lost this way.

A plain click with an empty hand on a golem the player picked cancels the pick only if the golem has a job. On a jobless golem the click stays vanilla and the pick stays.

## Sneak + right-click

All of these use the main hand. An item that is handed over is used up from the stack unless noted.

| Held in main hand | Golem | Result |
|---|---|---|
| Nothing | Any | Starts a link: see [below](#sneak-right-click-with-an-empty-hand). It never takes the golem's held item. |
| <Icon id="bundle" /> Bundle | Not a sorter | Becomes a [sorter](/jobs/sorter). Ends any other job. |
| <Icon id="bundle" /> Bundle | Already a sorter | Nothing |
| <Icon id="copper_pickaxe" /> Copper pickaxe | Any | Becomes a [miner](/jobs/miner), or swaps the pickaxe of a miner. |
| <Icon id="mineshaft_plan" /> Marked [Mineshaft Plan](/items/mineshaft_plan) | Miner holding its pickaxe | Gives it the plan. An old plan comes back. |
| <Icon id="mineshaft_plan" /> Marked Mineshaft Plan | Any other | Message: give it a copper pickaxe first |
| <Icon id="mineshaft_plan" /> Unmarked Mineshaft Plan | Any | Message: mark the entrance first |
| <Icon id="copper_hoe" /> Copper hoe | Any | Becomes a [farmer](/jobs/farmer), or swaps the hoe. |
| <Icon id="copper_axe" /> Copper axe | Clean, not waxed | Becomes a [forester](/jobs/forester), or swaps the axe. |
| <Icon id="copper_axe" /> Copper axe | Rusty or waxed | Scrapes it, as in vanilla |
| <Icon id="fishing_rod" /> Fishing rod | Any | Becomes a [fisher](/jobs/fisher), or swaps the rod. |
| <Icon id="brush" /> Brush | Any | Becomes an [animal keeper](/jobs/animal-keeper), or swaps the brush. |
| <Icon id="crafting_table" /> Crafting table | Any | Becomes a [crafter](/jobs/crafter), or swaps the table. |
| Seeds | Farmer | All seeds in the stack go into its supplies. |
| Non-copper pickaxe or hoe | Any | Refused: "Copper golems only work with copper tools." |
| Non-copper axe | Clean, not waxed | Refused with the same message |
| Non-copper axe | Rusty or waxed | Scrapes it, as in vanilla |
| <Icon id="iron_block" /> Iron, <Icon id="gold_block" /> gold or <Icon id="diamond_block" /> diamond block | Any but a courier | Gives or replaces its [eyes](/golem/eyes). The old block comes back. The block it already has does nothing. |
| Iron, gold or diamond block | Courier | Refused with a message |
| Armour trim smithing template | Any | Puts the template on: see [Armour trims](/golem/trims). |
| Trim material | Golem with a template | Applies or changes the trim. Without a template, nothing. |
| <Icon id="shears" /> Shears | Has a flower | Vanilla: takes the flower off |
| <Icon id="shears" /> Shears | No flower, has a trim | Takes the trim off and gives the template back |
| Honeycomb, lead, name tag and other items | Any | The same as a plain click |

With the [Automatisation](/automatisation/) add-on:

| Held in main hand | Golem | Result |
|---|---|---|
| <Icon id="copper_wrench" /> [Copper Wrench](/items/copper_wrench) | Not an operator | Becomes an [operator](/jobs/operator). |
| <Icon id="iron_plating" /> [Iron Plating](/items/iron_plating), <Icon id="copper_coating" /> [Copper Coating](/items/copper_coating) or <Icon id="gold_gearwork" /> [Gold Gearwork](/items/gold_gearwork) | Below the full amount | Fits or tops it up: see [Golem upgrades](/automatisation/upgrades). |
| <Icon id="copper_satchel" /> [Copper Satchel](/items/copper_satchel) | No satchel yet | Fits it |

The handover of a job item keeps the golem's [home](/start/homes-and-status), eyes, trim, energy and rust. Giving a different job item ends the old job and returns what the golem carried for it. The same job with a new tool only swaps the tools. A chained cart stays chained.

## Sneak + right-click with an empty hand

| State of the golem | Result |
|---|---|
| Not picked by this player | Picks it: it glows for 30 seconds and the chat shows its status. Any earlier pick by this player is dropped. |
| Picked, has a job, within 100 ticks (5 seconds) of the first press | **Ends the job** |
| Picked, has a job, more than 5 seconds later | Picks it again and restarts the timers |
| Picked, jobless | Cancels the pick: "Link cancelled." |

Ending the job needs a second **press**. The server only sees clicks, and a held button repeats them every 4 ticks, so clicks 5 ticks (0.25 seconds) or less apart count as one press. Giving a golem a job closes the 5-second window, so the next sneak-click cannot end the job just given. Starting a route on a courier also closes it.

The rest of a press that gave a job, ended a job or cancelled a pick does nothing to that golem. Holding the button cannot hand the returned tool back or repeat the action.

## Ending a job

Sneak + right-click the golem twice with an empty hand, within 5 seconds.

| Goes back to the player | Stays with the golem |
|---|---|
| Tool or bundle | Home |
| Everything in its main and off hand, including a courier's cargo | [Eyes](/golem/eyes) |
| Its Mineshaft Plan, progress included | [Trim](/golem/trims) |
| Pouch and supplies | Energy and rust |
| Iron ingots held for a [repair](/golem/guards-and-repairs) | Add-on [upgrades](/automatisation/upgrades) |

What does not fit in the player's inventory lands at the player's feet. A chained cart is unchained, stays where it is and keeps its items, and the chain drops. The route of a courier or the machine link of an operator ends. The golem is now jobless, sorts like vanilla, and walks back to a home if it has one.

<!-- MEDIA: start-end-job.mp4 | clip | A player sneak + right-clicking a farmer twice with an empty hand, the hoe coming back to the inventory and the message "The golem gives up its job." appearing above the hotbar -->

## Clicking blocks after picking a golem

| Block | Sneaking | Result |
|---|---|---|
| Copper chest | No | Sets the home |
| Copper chest | Yes, jobless golem or courier | Starts a route |
| Copper chest | Yes, any other job | Sets the home |
| Chest, trapped chest or barrel | Either | Starts a route, or refuses if the golem has another job |
| Machine (add-on) | Either | Links an operator to it |
| Any other block | Either | Acts as normal and keeps the pick |

After a route is started, the next right-click on any chest or barrel ends it, at any distance in the same dimension, within 10 minutes. Clicking the chest the route started at is refused and the pick stays. See [Homes and status](/start/homes-and-status) and [Courier](/jobs/courier).

## Other controls

| Action | Result |
|---|---|
| Hold sneak | Shows particle lines from golems within 24 blocks to their [homes](/start/homes-and-status#seeing-homes) |
| Right-click a [Copper Chest Minecart](/items/copper_chest_minecart) with an iron or copper chain | Chains it to the player |
| Sneak + right-click a chained Copper Chest Minecart | Unchains it |
| Axe in a dispenser, redstone pulse | [Scrapes](/start/vanilla-rule#the-axe-dispenser) the golem in front |
| Use a [Mineshaft Plan](/items/mineshaft_plan) on a block | Marks the entrance |

## See also

- [The vanilla rule](/start/vanilla-rule)
- [Quick reference](/reference/quick-reference)

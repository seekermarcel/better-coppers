---
title: The vanilla rule
---

# The vanilla rule

**The vanilla rule** is the design principle of Better Coppers: anything that happens automatically to a [copper golem](/golem/copper-golem) no player touched is exactly [vanilla](https://minecraft.wiki/w/Copper_Golem). Mod behaviour starts only after a deliberate player action. Golems and redstone builds in an existing world keep working when the mod is installed.

## Terms

| Term | Meaning |
|---|---|
| **Untouched golem** | A golem no player has acted on. Every untouched golem is jobless. |
| **Jobless golem** | A golem with no [job](/jobs/). It sorts like vanilla, even if it has a home. |
| **Golem with a job** | Sorter, miner, farmer, forester, fisher, animal keeper, beekeeper, crafter, courier or operator. |

A golem with only a home, with no job item and no route, is jobless but not untouched.

## What stays vanilla

For a jobless golem:

| Thing | Behaviour |
|---|---|
| Sorting | Carries items from copper chests to other chests, as in vanilla |
| Weathering, waxing, scraping | As in vanilla, including [turning into a statue](/golem/copper-golem#statue) |
| Plain right-click with an empty hand | Throws what it holds to the player |
| Shears, honeycomb, axes | As in vanilla |
| Monsters | Do not hunt it |
| Iron golems | Do not guard it |
| Running costs | None |

For every golem, with or without a job, shears and honeycomb stay as in vanilla, and an axe still scrapes a rusty or waxed golem. Leads and name tags work as in vanilla too.

## What starts the mod's behaviour

Each of these is a deliberate player action, and each opts the golem into only what is listed.

| Player action | What changes |
|---|---|
| Handing over a [job item](/start/controls) | The golem has that job. It stops vanilla sorting, [monsters hunt it](/golem/monsters) and [iron golems guard it](/golem/guards-and-repairs). |
| Setting a [route](/jobs/courier) | The golem is a courier: as above, with no home. |
| [Linking a home](/start/homes-and-status) | The golem walks back when it strays more than 6 blocks, can [repair iron golems](/golem/guards-and-repairs), and shows a particle line. It stays jobless and still sorts like vanilla. |
| Handing over a chained [cart](/items/copper_chest_minecart) | The golem tows the cart. |
| Giving [eyes](/golem/eyes) | The eyes change colour, and any job it has or gets works further from home. |
| Giving a [trim](/golem/trims) | The golem wears the trim. |
| Fitting an [add-on upgrade](/automatisation/upgrades) | The upgrade applies. |

Ending a job makes a golem jobless again: a former farmer that picks up a hoe through vanilla sorting does not become a farmer again. A golem only gains a job through a player, never by holding the right item.

A plain click is safe for the same reason. See [Controls](/start/controls).

## The axe dispenser

The one exception to the rule is an **axe in a dispenser**. It scrapes any copper golem in front of it, whether or not a player has touched that golem, because the player built the dispenser on purpose.

| Property | Value |
|---|---|
| Trigger | A redstone pulse on the dispenser |
| Target | The first copper golem in the block directly in front of the dispenser that can be scraped |
| Result | Removes the wax, or else one stage of rust |
| Axes | Any axe, from wood to netherite |
| Cost | The axe stays in the dispenser and loses 1 durability |
| Nothing to scrape | The dispenser behaves as in vanilla and ejects the axe |

Scraping one stage also clears the golem's progress towards its next [rust](/golem/running-costs) stage.

<!-- MEDIA: start-axe-dispenser.png | screenshot | A dispenser holding an axe, wired to a button, facing a rusty copper golem standing in the block in front of it -->

## See also

- [Copper Golem](/golem/copper-golem)
- [Running costs](/golem/running-costs)

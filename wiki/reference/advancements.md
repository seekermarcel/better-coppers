---
title: Advancements
---

# Advancements

**Guide advancements** are two advancement tabs that lead a player through the mods step by step: one tab for Better Coppers and one for the Automatisation add-on. Together with the [Golem Handbook](/items/golem_handbook) they are the in-game guide. The tabs are opened with the normal advancements key.

<!-- MEDIA: reference-advancement-tab.png | screenshot | The Better Coppers tab of the advancements screen, showing the tree from A Little Helper down through Home Sweet Chest and its branches, with some steps earned -->

## How steps are earned

Steps that describe something a golem or machine does on its own ("Watch ...") go to every player, except spectators, within 48 blocks of it at that moment, in the same dimension. All other steps go to the player who did the action. No step is hidden.

"Type" is the advancement frame: Task, Goal or Challenge.

## Better Coppers tab

The tab's IDs have the namespace `bettercoppers`.

| Advancement | In-game description | Parent | Type | How it is earned |
|---|---|---|---|---|
| **Better Coppers**<br>`guide/root` | Copper golems that work for you. Read the Golem Handbook! | None | Task | Automatically on entering a world, with no chat message. Gives one [Golem Handbook](/items/golem_handbook). |
| <Icon id="carved_pumpkin" /> **A Little Helper**<br>`guide/build_golem` | Build a copper golem: a carved pumpkin on a copper block | Better Coppers | Task | Building a [copper golem](https://minecraft.wiki/w/Copper_Golem). |
| <Icon id="copper_chest" /> **Home Sweet Chest**<br>`guide/home` | Link a golem to a copper chest: sneak + right-click it, then the chest | A Little Helper | Task | Linking a golem to a copper chest as its [home](/start/homes-and-status). |
| <Icon id="bundle" /> **Tidy Up**<br>`guide/sorter` | Sneak + right-click a golem with a bundle to make it a sorter | Home Sweet Chest | Task | Giving a golem a bundle, making it a [sorter](/jobs/sorter). |
| <Icon id="copper_chest_minecart" /> **Heavy Load**<br>`guide/cart` | Hand a copper chest minecart to a golem with an iron or copper chain | Tidy Up | Task | Handing a chained [Copper Chest Minecart](/items/copper_chest_minecart) to a golem. |
| <Icon id="copper_hoe" /> **Green Fingers**<br>`guide/farmer` | Sneak + right-click a golem with a copper hoe to make it a farmer | Home Sweet Chest | Task | Making a golem a [farmer](/jobs/farmer). |
| <Icon id="copper_axe" /> **Odd Jobs**<br>`guide/odd_jobs` | Sneak + right-click a golem with a copper axe, a fishing rod, a brush or a crafting table | Home Sweet Chest | Task | Making a golem a [forester](/jobs/forester), [fisher](/jobs/fisher), [animal keeper](/jobs/animal-keeper) or [crafter](/jobs/crafter); one of the four is enough. |
| **Full Employment**<br>`guide/full_employment` | Give golems every job: sorter, miner, farmer, forester, fisher, animal keeper, crafter and courier | Odd Jobs | Challenge | Having given all eight jobs, each at least once, on any golems. |
| <Icon id="survey_table" /> **Surveyor**<br>`guide/survey` | Make a mineshaft plan at a Survey Table | Home Sweet Chest | Task | Having a [Mineshaft Plan](/items/mineshaft_plan) in the inventory, however it was obtained. |
| <Icon id="copper_pickaxe" /> **Down the Shaft**<br>`guide/miner` | Sneak + right-click a golem with a copper pickaxe, then with a mineshaft plan | Surveyor | Task | A golem holding a copper pickaxe also gets a marked Mineshaft Plan, in either order, making it a [miner](/jobs/miner). |
| **Shiny!**<br>`guide/diamonds` | Watch a miner find diamonds and hurry home with its antenna blinking | Down the Shaft | Goal | Watch: a miner digs a diamond ore and starts blinking. |
| <Icon id="barrel" /> **Special Delivery**<br>`guide/courier` | Set up a courier route between two chests | Home Sweet Chest | Task | Completing a [courier](/jobs/courier) route. |
| **Long Haul**<br>`guide/long_haul` | Set up a courier route over 500 blocks long | Special Delivery | Goal | Completing a route whose two ends are at least 500 blocks apart in a straight line. |
| <Icon id="minecart" /> **All Aboard**<br>`guide/ride` | Watch a golem ride a minecart or a boat | Home Sweet Chest | Task | Watch: a golem boards a [minecart or boat](/golem/riding). |
| <Icon id="charging_pad" /> **Recharged**<br>`guide/recharge` | Charge a working golem to full on a Charging Pad driven by a redstone clock | Home Sweet Chest | Task | Watch: a golem reaches full energy on a [Charging Pad](/items/charging_pad), or on a [Charging Station](/items/charging_station) with the add-on. |
| <Icon id="dispenser" /> **Good as New**<br>`guide/scrape` | Scrape a rusty golem with an axe in a dispenser | Recharged | Task | Watch: a dispenser with an axe scrapes a golem. Removing wax this way also counts. |
| <Icon id="gold_block" /> **Eagle Eyes**<br>`guide/eyes` | Sneak + right-click a golem with a block of iron, gold or diamond to widen its work radius | Home Sweet Chest | Task | Giving a golem [eyes](/golem/eyes). |
| **Bodyguard**<br>`guide/guard` | Watch an iron golem defend a copper golem | A Little Helper | Task | Watch: an iron golem starts attacking something that hit or is hunting a copper golem with a job. See [Guards and repairs](/golem/guards-and-repairs). |
| <Icon id="iron_ingot" /> **Field Medic**<br>`guide/repair` | Watch a copper golem repair an iron golem with iron | Bodyguard | Task | Watch: a copper golem heals an iron golem with an iron ingot. |
| **Dressed to Impress**<br>`guide/trim` | Sneak + right-click a golem with a template, then with a material, for an armour trim | A Little Helper | Task | Completing or changing a golem's [armour trim](/golem/trims). |

## Automatisation tab

The tab appears with the add-on. Its IDs have the namespace `bettercoppers_automatisation`.

| Advancement | In-game description | Parent | Type | How it is earned |
|---|---|---|---|---|
| <Icon id="belt_drive" /> **Better Coppers Automatisation**<br>`guide/root` | A golem-run factory: no power network, golems turn every crank | None | Task | Automatically on entering a world, with no chat message. |
| <Icon id="copper_wrench" /> **Grease Monkey**<br>`guide/wrench` | Craft a Copper Wrench | Better Coppers Automatisation | Task | Having a [Copper Wrench](/items/copper_wrench) in the inventory. |
| <Icon id="press" /> **Turn the Crank**<br>`guide/operator` | Sneak + right-click a golem with a wrench and link it to a machine | Grease Monkey | Task | Linking an [operator](/jobs/operator) to a machine. |
| <Icon id="conveyor_belt" /> **Keep It Moving**<br>`guide/belt` | Get a belt line running with a Belt Drive | Turn the Crank | Task | Watch: a [Belt Drive](/items/belt_drive) starts running under its operator's crank. |
| <Icon id="grabber_arm" /> **Pick and Place**<br>`guide/arm` | Set up a Grabber Arm's inputs and outputs with the wrench | Turn the Crank | Task | Finishing a [Grabber Arm](/items/grabber_arm) setup with at least one input and one output. |
| **Maintenance Crew**<br>`guide/repair` | Watch an operator repair a jammed machine | Turn the Crank | Task | Watch: an operator clears a [jam](/automatisation/wear-and-jams). |
| <Icon id="copper_sheet" /> **Flat Out**<br>`guide/sheets` | Press an ingot into a sheet | Turn the Crank | Task | Having a Copper, Iron or Gold Sheet in the inventory; any one is enough. |
| <Icon id="copper_gear" /> **Cogs and Teeth**<br>`guide/gears` | Saw a sheet into a gear | Flat Out | Task | Having a Copper or Gold Gear in the inventory; either is enough. |
| <Icon id="golem_assembler" /> **Golem Factory**<br>`guide/assembler` | Watch a Golem Assembler build a new copper golem | Cogs and Teeth | Goal | Watch: a [Golem Assembler](/items/golem_assembler) finishes a golem. |
| <Icon id="iron_plating" /> **Kitted Out**<br>`guide/upgrade` | Fit a golem with plating, coating, gearwork or a satchel | Flat Out | Task | Fitting any [upgrade](/automatisation/upgrades) to a golem. |
| <Icon id="gold_gearwork" /> **Fully Loaded**<br>`guide/fully_loaded` | Give one golem all four upgrades at once | Kitted Out | Challenge | Fitting an upgrade so that one golem has all four active. |
| <Icon id="cart_extension" /> **Double Decker**<br>`guide/cart_extension` | Turn a copper chest minecart into a double chest | Flat Out | Task | Using a [Cart Extension](/items/cart_extension) on a Copper Chest Minecart. |
| <Icon id="charging_station" /> **Fast Charger**<br>`guide/charging_station` | Build a Charging Station | Flat Out | Task | Having a [Charging Station](/items/charging_station) in the inventory. |

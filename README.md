# Better Coppers

[![build](https://github.com/seekermarcel/better-coppers/actions/workflows/build.yml/badge.svg)](https://github.com/seekermarcel/better-coppers/actions/workflows/build.yml)
[![release](https://img.shields.io/github/v/release/seekermarcel/better-coppers)](https://github.com/seekermarcel/better-coppers/releases/latest)
[![license](https://img.shields.io/badge/license-LGPL--3.0-blue)](COPYING.LESSER)

**Copper Golems that do real work.** A Fabric mod for **Minecraft 26.3** that turns the copper golem from a chest shuffler into a small, slightly clumsy worker. It sorts your storage, digs mineshafts, farms and fishes, carries goods across the world, rides minecarts and boats, and gets hunted by monsters. It also needs iron golems to keep it safe, and it patches those iron golems up with your iron.

Everything is meant to feel vanilla: golems are slow, sometimes shake their head and try again, and every job costs something.

**[The wiki](https://seekermarcel.github.io/better-coppers/)** has every job, item and number.

---

## At a glance

| Give the golem… | It becomes a… | What it does |
|---|---|---|
| a **bundle** | Sorter | Empties its copper chest and puts every item where it belongs |
| a **copper pickaxe** + **mineshaft plan** | Miner | Digs a vanilla-style mineshaft, follows ores, brings the loot home |
| a **copper hoe** | Farmer | Harvests and replants the fields around its chest |
| a **copper axe** | Forester | Fells natural trees and replants them |
| a **fishing rod** | Fisher | Casts its line from the shore and brings the catch home |
| a **brush** | Animal keeper | Feeds animals so they breed |
| a **crafting table** | Crafter | Crafts whatever the item frames on its chest show |
| a **route** (two chests) | Courier | Carries everything from one chest to another, however far apart |

Golems only work with **copper** tools. A golem without any of these sorts its copper chest exactly like in vanilla. A golem with a home walks back when it has nothing to do and has strayed more than 6 blocks from it.

On top of the jobs:
- **Copper Chest Minecart:** a golem can tow one on an iron or copper chain to carry much more.
- **Eyes:** a block of iron, gold or diamond gives a golem glowing eyes and 1.5×, 2× or 3× its work radius.
- **Broken tools:** a golem whose tool breaks keeps its job and fetches a new copper tool from the chests around its home.
- **Minecarts and boats:** golems ride them on long trips.
- **Monsters and guards:** monsters hunt copper golems that have a job, iron golems guard them, and copper golems repair iron golems.
- **Armour trims:** golems can wear them, just for looks.
- **Running costs:** working golems recharge on Charging Pads driven by a redstone clock, rust from work, and wear out their tools.

---

## Installation

1. Install **Minecraft 26.3** with **Fabric Loader** 0.20.1 or newer.
2. Add [Fabric API](https://modrinth.com/mod/fabric-api).
3. Download the jar from the [latest release](https://github.com/seekermarcel/better-coppers/releases/latest) into your `mods/` folder.

---

## Getting started

**New to the mod?** You get a **Golem Handbook** the first time you join a world (lost it? book + copper ingot). It explains everything, chapter by chapter, in English and German. The **Better Coppers advancement tab** (press L) is the quest line: each step tells you what to try next, from building your first golem to "Full Employment" with a golem in every job. The add-on brings its own handbook chapters and its own tab.

1. **Give a golem its job item.** Sneak + right-click the golem with it (see the table above).
2. **Give it a home.** Sneak + right-click the golem with an empty hand. It glows and tells you its job and home, and for every job except sorter and courier its energy and rust. Then right-click the **copper chest** it should belong to (within 32 blocks).
3. **Check on it.** While you sneak, copper particles run from every golem near you to its home chest. A golem that has a job but no home puffs smoke.

To move a golem to another chest, link it again.

---

## Jobs

### Sorter
- Sneak + right-click it with a **bundle**, which it keeps in its off hand. It only empties its own home chest.
- It knows every container within 32 blocks (8 up/down) and where each item belongs:
  1. a chest or barrel with an **item frame** showing that item (a framed container only takes what its frames show),
  2. otherwise a chest that already holds that item,
  3. otherwise any barrel with room,
  4. otherwise an empty chest.
- For character, it first opens 0–3 wrong chests on each trip and shakes its head.
- It carries 16 items at a time, as in vanilla, or a whole cart load with a chained cart.
- To take the bundle back, end its job: sneak + right-click it twice with an empty hand, within 5 seconds.

### Miner
1. Make a **Survey Table** (2 paper / 2 copper ingots / 2 planks, stacked in that order).
2. Put paper in, choose a length (8–128) and take out the **mineshaft plan**.
3. Mark the entrance by using the plan on the ground (or on a wall). The tunnel heads the way you look.
4. Sneak + right-click the golem with a **copper pickaxe**, then with the plan, then link it to a copper chest near the entrance.

The golem digs a **3×3 vanilla-style mineshaft**: plank floor over gaps, a support every 4 blocks (fence posts and a plank beam), torches on the beams, and rails laid at the end.

- **No supplies, no digging.** It must carry the rails, fences and planks for what it digs. It fetches them from its chest, or waits there until you restock. Torches come from the chest too, but without them it digs on and leaves them out.
- **Ores:** it digs out ores touching what it digs and follows each vein for up to 16 ores, diamond, gold, redstone and emerald included.
- **Diamonds:** when it finds diamonds, it digs out the whole vein and hurries home with its antenna blinking.
- **Safety:** it plugs water and lava with filler it dug itself (cobblestone, stone, dirt and the like) and clears gravel and sand that falls in.
- **Slow and clumsy:** it digs two to three times slower than a player and skips blocks it can't reach.
- **Loot:** it carries 64 items (more with a cart), then walks home. Link a sorter to the same chest and it sorts the loot, leaving the mining supplies alone.
- **Getting things back:** end its job to get the pickaxe and the plan back. The plan keeps its progress. It takes the pickaxe first, then the plan.

### Farmer
- Sneak + right-click it with a **copper hoe**. Its field is everything within 8 blocks of its home chest.
- It harvests ripe wheat, carrots, potatoes, beetroot, nether wart and pitcher crops and replants them right away. Empty farmland gets what the neighbouring rows grow.
- It picks pumpkins and melons and leaves the stems, and cuts sugar cane and bamboo once they are 3 high.
- It keeps 16 of every seed it can plant and never tills new land. You build the field. A stem needs a free block next to it for its fruit, so leave those spots as plain dirt.

### Forester, fisher, animal keeper, crafter

| Tool | Job | What it does |
|---|---|---|
| Copper axe, on a clean golem | Forester | Fells natural trees within 12 blocks (never logs you placed), collects saplings, apples, sticks and logs, replants every stump (four for a 2x2 tree) with the same species only, leaving it empty until it has that sapling |
| Fishing rod | Fisher | Casts a float into open water within 10 blocks and reels in when it dips, a catch every 20–50 seconds (fish and junk, no treasure) |
| Brush | Animal keeper | Feeds pairs of animals with food from its chest so they breed, up to 16 per kind |
| Crafting table | Crafter | Crafts what item frames on or next to its chest show, from ingredients in that chest |

### Courier
A courier carries everything from one chest to another, **however far apart they are**:
1. Sneak + right-click the golem with an empty hand.
2. Right-click a plain **chest or barrel**, or sneak + right-click a **copper chest**: it picks things up here.
3. Walk to the destination (you have 10 minutes) and right-click any chest, copper chest or barrel there.

The courier takes 16 items at a time, or a whole cart load with a chained cart. While travelling it keeps the chunks around itself loaded, like a thrown ender pearl, so the route keeps working when you're far away. When it gets stuck, it shakes its head, wanders off a few blocks and tries again. If the destination is full, it waits there. Linking a home copper chest ends the route.

---

## Getting around

### Copper Chest Minecart on a chain
- **Recipe:** minecart + any copper chest.
- **Hand it over:** right-click the cart with an **iron or copper chain** (any age, waxed or not) so it follows you, then right-click a golem with an empty hand.
- **Unchain it:** sneak + right-click the cart.
- **Sorters** load everything *related* to the first item they pick up: stone also brings stone slabs, stairs and bricks, but not oak logs. They then deliver it all in one trip.
- **Other jobs** fill the cart before their pouch.

### Minecarts and boats
A golem heading somewhere more than 24 blocks away looks for a ride nearby:
- **Minecart:** an empty minecart on straight rails that run the right way. The golem pushes it along at about 4× walking speed (faster on powered rails) and gets off near its goal.
- **Boat:** an empty boat on water that stretches the right way. The golem paddles straight at its goal and gets out when it runs ashore.

A golem towing a chained cart always walks.

---

## Danger and protection

- **Monsters hunt working copper golems.** Every monster that goes after villagers or iron golems now goes after copper golems that have a job too: zombies, skeletons, spiders, illagers and more. Jobless golems are left alone, as in vanilla. A copper golem has only 12 health.
- **Iron golems guard them.** An iron golem stays near the copper golems with a job that live around it and attacks any monster that hunts or hits one. Creepers are excepted, as in vanilla.
- **Copper golems repair iron golems.** When an iron golem nearby is badly hurt, a copper golem with a home or route pauses what it is doing (a sorter or courier finishes its delivery first), takes **iron ingots** from its home chest (or the nearest chest with iron) and repairs it, 25 health per ingot.

So keep some iron in your chests, and build an iron golem for every work site.

---

## Tools and eyes

**Copper tools only.** Golems work with copper pickaxes, axes and hoes. Other pickaxes, axes and hoes are refused (any axe still scrapes a rusty golem). Fishing rods, brushes and crafting tables have no copper version and work as before.

**Broken tools.** When a tool breaks, the golem keeps its job. It brings home what it carries, then looks for the same tool in its home chest and then in every chest and barrel within 16 blocks of home, and takes it. With none anywhere it shakes its head and looks again every half minute. To end its job instead, sneak + right-click it twice with an empty hand, within 5 seconds.

**Eyes.** Sneak + right-click a golem with a block of iron, gold or diamond:

| Block | Eyes | Radius | Farm | Forest | Fishing | Sorter |
|---|---|---|---|---|---|---|
| none | warm yellow | 1× | 8 | 12 | 10 | 32 |
| Iron block | white | 1.5× | 12 | 18 | 15 | 48 |
| Gold block | gold | 2× | 16 | 24 | 20 | 64 |
| Diamond block | cyan | 3× | 24 | 36 | 30 | 96 |

It also widens animal pens, how far a miner follows a vein and where an add-on operator looks for spare parts. Couriers don't take eyes. A new block replaces the old one (which you get back), and the block drops when the golem dies.

---

## Armour trims
Sneak + right-click a golem with an **armour trim smithing template**, then with a **trim material** (iron, gold, copper, emerald, amethyst…). The trim shows in the material's colour, exactly like on armour, and all 18 vanilla patterns are supported.
- The material is used up. The template stays with the golem.
- A new template or material changes the trim.
- **Sneak + right-click with shears** takes the trim off and gives the template back. A flower comes off first.

Trims are only for looks.

---

## Running costs
Sorters and couriers are free. Every other job costs something:

- **Energy:** work uses energy. At night, or when empty, the golem walks to a free **Charging Pad** near its home (3 copper ingots, a lightning rod and redstone: `CLC` / `CRC`) and stands on it. A steady redstone signal does nothing: the pad charges on **redstone pulses**, so give it a **redstone clock**. Each pulse gives 8 energy (of 1000). Pulses less than 3 ticks apart count once, so the fastest clock fills an empty golem in about 25 seconds and a once-a-second clock in about two minutes. Build one pad per golem.
- **Sleeping:** when the players sleep through the night, every working golem with a free pad near its home wakes up fully charged, as if it had spent the night on the pad.
- **Rust:** steady work makes golems rust one stage every few days. Exposed golems work at 85 % speed and weathered ones at 70 %. Fully oxidized or waxed golems don't work at all, except as sorters and couriers.
- **Scraping:** use an axe by hand, or put an **axe in a dispenser**: each redstone pulse scrapes the golem in front of it by one stage.
- **Tools** wear out and break. A golem whose tool breaks keeps its job and fetches a new one (see [Tools and eyes](#tools-and-eyes)). When you end its job, it goes back to vanilla sorting.

---

## Quick reference

| You do | What happens |
|---|---|
| Sneak + right-click golem with a job item | Gives it that job, or switches to it |
| Sneak + right-click golem (empty hand) | Shows its status and starts linking |
| …then right-click a copper chest | Sets its home |
| …then right-click a chest/barrel, then another | Sets a courier route |
| …then sneak + right-click a copper chest, then right-click another chest/barrel | Sets a courier route from the copper chest |
| …the same click again within 5 seconds | Ends its job: you get its tool or bundle and everything it carries, a chained cart stays where it is. Without a job, at any time: cancels linking |
| Right-click golem (empty hand) | With a job: shows its status and cancels linking. Without: hands over what it holds, as in vanilla |
| Sneak + right-click golem with an iron, gold or diamond block | Eye upgrade: bigger work radius |
| Sneak + right-click golem with template, then material | Gives it an armour trim |
| Sneak + right-click golem with shears | Removes the trim |
| Right-click cart with an iron or copper chain, then a golem with an empty hand | Hands the cart over |
| Sneak (just hold it) | Shows lines from golems to their homes |

---

## Add-on: Better Coppers Automatisation

The [`automatisation/`](automatisation/README.md) folder holds an add-on with **conveyor belts, funnels, chutes, presses, saws, grabber arms and a golem assembler**, in the spirit of Create but more mechanical. There is no power network: every machine has a crank that a copper golem with a **Copper Wrench** has to turn, and repairs when it jams. What the factory makes goes back into the colony: armour plating, rust coating and faster gearwork for golems, satchels and double carts for bigger loads, cheap rails and fences for miners, a faster charging station, and new golems. See its [README](automatisation/README.md).

---

## Building from source

Run `nix develop` (or use any JDK 25), then `./gradlew build`. The jars land in `build/libs/` and `automatisation/build/libs/`. `./gradlew runClient` starts the game with the mod.

On Windows without Nix:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot"
.\gradlew.bat runClient   # start the game with the mod
.\gradlew.bat build       # mod jar in build/libs/ (and the add-on's in automatisation/build/libs/)
.\gradlew.bat :automatisation:runClient   # the game with the mod and the add-on
```

## Project structure

```
src/main/java/dev/bettercoppers/
  golem/     GolemBrain, Golems, Sorter, SortItemsBehavior (sorting AI), RideBehavior (minecarts and boats), ItemFamilies (what counts as related), CartChaining, Inventories, Errands
  courier/   CourierBehavior, Couriers (routes, chunk tickets), CourierRoute
  combat/    GolemGuards (monsters hunt copper golems with a job, iron golems guard them) and the iron golem goals
  repair/    RepairBehavior (iron golem repairs)
  trim/      GolemTrims (trim templates and materials)
  mining/    MineBehavior (mining AI), MineLayout, MinePlan, MinerData, MinerInteractions
  job/       JobBehavior (shared home-chest routine), Farm-/Forest-/Fish-/Keeper-/CraftBehavior, WorkerData
  runningcosts/    RunningCosts (energy, rust, scraping), ChargeBehavior, AxeDispenseBehavior
  home/      Homes, HomeLinking (linking homes and routes), HomeDisplay (particle lines)
  block/, menu/  Survey Table and its menu
  entity/, item/  Copper Chest Minecart
  api/       hooks for add-ons: jobs, brain behaviours, link targets, upgrades, charging spots, handbook
  mixin/     adds the mod's behaviours in front of vanilla's golem AI (vanilla AI kept), goal accessors
src/client/java/dev/bettercoppers/client/
  render/    cart renderer with its chain, GolemTrimLayer, GolemAntennaLayer, GolemFloatRenderer
automatisation/  the Automatisation add-on (its own Gradle subproject)
tools/       TrimGen.java (golem trim textures from the vanilla armour trims), SurveyGen.java (Survey Table textures),
             PadGen.java (Charging Pad), HandbookGen.java (handbook icon), GolemEyesGen.java (eye and antenna glow),
             gen_guide.py (advancements and handbook texts of both mods)
```

## License

[LGPL-3.0](COPYING.LESSER), which builds on the [GPL-3.0](COPYING).

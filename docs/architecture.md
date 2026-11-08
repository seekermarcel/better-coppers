# Architecture

The flows and traps that no single file shows. Vocabulary is in [`GLOSSARY.md`](../GLOSSARY.md), reasons are in [`adr/`](adr/).

## How a golem decides what to do

`CopperGolemAiMixin` puts `GolemBrain.modBehaviours()` in front of vanilla's idle activity. Vanilla's behaviours stay in the list, and `TransportItemsBetweenContainersMixin` blocks vanilla transport for a golem with a job or on an errand.

Each behaviour decides for itself whether it applies, in its start conditions, which ask `Jobs.of`. Order in `modBehaviours()` matters:
- `RideBehavior` is last among priority 0, so it sees the walk target the job behaviours just set.
- `HomeStroll` is priority 1, so it runs after every job and stops vanilla's stroll from starting.
- Add-on behaviours from the API come after the base jobs and before `RideBehavior`.

## How a player action reaches a golem

Right-clicks run through the `UseEntityCallback` handlers, and the first result other than `PASS` wins. The base mod's handlers run in the order `BetterCoppers.onInitialize` registers them, and `Tools` comes early, so non-copper tools are refused before a job takes them. Mod load order varies, so an add-on's handlers can run before or after those. They must not depend on that: `Operators` and `Upgrades` only react to the add-on's own items, which no base handler takes. A handler that must beat every other one needs an event phase, as `HomeLinking.onHandoverPress` has. Handing a golem anything takes sneak + right-click (ADR 0009).

`HomeLinking` owns empty-hand clicks: sneak starts the link, and a plain click on a golem with a job shows its status and returns `SUCCESS`, which stops vanilla's item throw. A selection and its glow end together (`HomeLinking.drop`). A plain click on the selected golem with a job cancels it, and so does a second sneak-click on a selected jobless golem. `CartChaining` runs before it, for a player leading a cart. Vanilla throws for any empty hand, so the same check covers the off hand when the main hand's item did nothing.

Client and server both run these handlers, but job attachments are not synced, so the client cannot call `Jobs.has`. Decide on the server. Vanilla's throw also runs on the client, so `HomeLinking` resends the golem's main hand after blocking a click.

The server only sees clicks, and a held button repeats one every 4 ticks. So `HomeLinking` counts clicks within 5 ticks as one press, and only a later press within 100 ticks ends a job. Giving a job closes that window. The rest of the press that gave or ended a job or cancelled a selection does nothing (`HomeLinking.onHandoverPress`, in a phase before all other handlers): the tool that came back can land in the selected hand, and a repeat would hand it over again (or end the job just given).

## What a job is in code

A job is derived from what the golem holds and which data it has, with no stored job field. Miners and the `Job` enum's jobs need both the tool and a player-created attachment, which is what stops an untouched golem from gaining a job by picking up a tool. A sorter holds its bundle in the off hand, which vanilla transport leaves alone, and a courier is defined by its route.

`Jobs` answers from those facts. When a golem seems to have two jobs the first match wins: route, add-on job, job tool in hand or lost, bundle. So a courier carrying a hoe is a courier, and data without its tool means nothing.

`JobHandover` is how a job starts over or ends, with one exception: linking a courier to a home ends its route in `HomeLinking`, and the cargo in its hand stays for vanilla to deliver. `JobHandover` gives the player the held items, plan, pouch, supplies and repair iron, removes the job's data and calls `GolemJob.onEnded`, so no job data outlives its job: an ex-worker that picks up a hoe through vanilla transport stays jobless. `giveTool` and `clear` keep a chained cart for the next job, and `end` unchains it. A broken tool is not a job end: the job stays and `ToolFetchBehavior` restocks it. `Jobs.has` is the vanilla rule's predicate and runs for every golem, so it must stay free of entity searches for an untouched golem. `Jobs.place` is the workplace, then the home, which stays linked underneath.

`WorkerData.job` is narrower than it sounds: it names the job of the tool, given worker data. Ask `Jobs.of` what the golem's job is.

The `Job` enum holds only the five jobs that share `JobBehavior`. Miner, sorter and courier have their own code, and add-on jobs are `GolemJob`s.

`ChargingSpot` is an API hook for add-on charging blocks such as the Charging Station. The Charging Pad block does not use it.

## A golem with only a home

It sorts like vanilla, and the link also opts it into two things: `HomeStroll` walks it back when it strays, and `RepairBehavior` lets it repair iron golems. Sorters and couriers finish a delivery before starting a repair, and so does a home-only golem holding anything but iron ingots. Iron ingots in its main hand (vanilla lifts them from its copper chest) count as supply: the repair moves them into `CARRIED_IRON` without visiting a chest, and leftover iron goes back to the hand, or to the home chest if the hand is full, when the repair ends.

## State

Per-golem state lives in Fabric attachments (`ModAttachments`), and most of it is saved. `Load` holds pouch and supplies in `LOAD`; `WORKER` and `MINER` stay the signal that a player gave the job, and `LOAD` never is. `Load.hasNothingToBringHome` ignores supplies, which stay with the golem. These are lost on restart: the `ERRAND` attachment and the static maps `Couriers.PENDING`, `HomeLinking.SELECTIONS`, `HomeLinking.HANDOVER_PRESSES` and `RepairBehavior.CLAIMS`.

Behaviours keep per-golem fields, so every golem needs its own instance. The API takes factories for this reason.

There are no save migrations because no version has been released. That ends with the first release: from then on, changing a saved format needs a migration.

## Generated files

- `tools/gen_guide.py` writes advancements, the handbook and all guide texts for both mods. Change guide content there and rerun it: it overwrites everything it generates and merges its keys into the lang files.
- `tools/gen_automatisation_resources.py` writes the add-on's blockstates, models, item definitions, loot tables, recipes and tags.
- `tools/*.java` generate textures, as listed in `tools/README.md`.
- `tools/gen_wiki_icons.py` writes `wiki/public/icons/`. Crafting grids in the wiki are built from the recipe JSON, so a recipe change reaches the wiki without an edit, unless it uses a new tag (`recipes.data.mts`) or item with no icon yet.

## Tests

Gametests build their scene with `setBlock` and hand items over through `UseEntityCallback` with a mock player, so they exercise the real player path. `VanillaGameTests` guards the vanilla rule. The add-on has gametests of its own (`automatisation/src/gametest`). Playtesting in the real game is done by hand.

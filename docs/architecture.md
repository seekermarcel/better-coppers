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

`JobHandover` is how a job starts over or ends, with one exception: linking a courier to a home ends its route in `HomeLinking`, and the cargo in its hand stays for vanilla to deliver. `JobHandover` gives the player the held items, pouch, supplies and repair iron, removes the job's data and calls `GolemJob.onEnded`, so no job data outlives its job: an ex-worker that picks up a hoe through vanilla transport stays jobless. `giveTool` and `clear` keep a chained cart for the next job, and `end` unchains it. A broken tool is not a job end: the job stays and `ToolFetchBehavior` restocks it. `Jobs.has` is the vanilla rule's predicate and runs for every golem, so it must stay free of entity searches for an untouched golem. `Jobs.place` is the workplace, then the home, which stays linked underneath.

`WorkerData.job` is narrower than it sounds: it names the job of the tool, given worker data. Ask `Jobs.of` what the golem's job is.

The `Job` enum holds only the six jobs that share `JobBehavior`. Miner, sorter and courier have their own code, and add-on jobs are `GolemJob`s.

Only paying jobs ask `RunningCosts.canWork`. Sorters and couriers skip it on purpose, so they keep working when waxed or oxidized: rust never affects these free jobs, a waxed jobless golem still sorts like vanilla, and wax is what protects a golem from turning into a statue.

`ChargingSpot` is an API hook for add-on charging blocks such as the Charging Station. The Charging Pad block does not use it.

## A golem with only a home

It sorts like vanilla, and the link also opts it into two things: `HomeStroll` walks it back when it strays, and `RepairBehavior` lets it repair iron golems. Sorters and couriers finish a delivery before starting a repair, and so does a home-only golem holding anything but iron ingots. Iron ingots in its main hand (vanilla lifts them from its copper chest) count as supply: the repair moves them into `CARRIED_IRON` without visiting a chest, and leftover iron goes back to the hand, or to the home chest if the hand is full, when the repair ends.

## State

Per-golem state lives in Fabric attachments (`ModAttachments`), and most of it is saved. `Load` holds pouch and supplies in `LOAD`; `WORKER` and `MINER` stay the signal that a player gave the job, and `LOAD` never is. `Load.hasNothingToBringHome` ignores supplies, which stay with the golem. Mines live in a per-dimension `SavedData`, `bettercoppers:mines` (`Mines`, the spec's `bettercoppers_mines`), keyed by middle beam and axis. Each mine saves its facing, its Mine Sections, each with its kind, its Open End, mirroring, `parent`, `box` (footprint and floor layer) and `progress`, and the Open Ends nobody has grown from yet; a section's steps and Open Ends are rebuilt from kind, Open End and mirroring (`MineLayout.of`), and steps already done skip themselves. An Open End's parent section is found by rebuilding the sections' Open Ends (`Mine.parentOf`). `MinerData` holds only which mine the miner works in (`mine` + `mine_axis`), which `Mines.of` looks up again when it is missing or finished. A mine is finished when it has no Open End and every section is done. It is server-only and has a `format` field: older formats are migrated step by step on the raw `Dynamic` before decoding, and a newer one is logged, ignored and written back untouched. A Mine Entrance is registered only from `BlockItemMixin`, when a player places its last block, and nothing watches its blocks afterwards: `MineEntrance.isIntact` is checked when needed. These are lost on restart: the `ERRAND` attachment, the claims in `Mines` (section → golem UUID and last renewal, keyed by mine and the section's Open End so deleting a section moves no claim) and the static maps `Couriers.PENDING`, `HomeLinking.SELECTIONS`, `HomeLinking.HANDOVER_PRESSES` and `RepairBehavior.CLAIMS`.

Behaviours keep per-golem fields, so every golem needs its own instance. The API takes factories for this reason.

A courier cannot see a far destination's Quota Slip from the pickup: item frames are entities, and that chunk may not be loaded. So it reads the quota while it stands at the destination and saves what is missing in `SHORTAGE`. Null there means no quota and today's round, empty means nothing is missing and it stays.

There are no save migrations because no version has been released. That ends with the first release: from then on, changing a saved format needs a migration.

### Mine Sections

`MineLayout` addresses cells from a section's Open End: `forward` along its facing, `side` to the right, `up` above the parent's floor. Slice -1 is the parent's last slice (for the first section, the entrance's opening), where the golem stands to dig slice 0. The dig order is per slice: middle column top down, then left, then right, then the floors, supports and torch; rails come last, from the far end back. Every step's block is in reach of its stand (`Golems.isInReach`). A slice's columns go middle first, then outwards, left before right, and each is worked from the free column (dug, no post or pillar at feet height) of the slice behind that is next to it and nearest the middle. `Mines.pick` finds a miner work and claims it in one call: the section it already holds, else the nearest unfinished section without a valid claim (ties to the oldest), else the nearest Open End whose section is done, which it rolls (`SectionRoll`): candidates that pass the follow rules, weighted on a scale where the built-ins sum to 100, picked without replacement from a `RandomSource` seeded by world seed, entrance and Open End. The first of 3 picks that overlaps no saved section box is saved with its Open Ends, and an end none fits is dropped. A claim is valid while its golem is loaded, alive, a miner with a pickaxe in hand and on that mine, and renewed (every `MineBehavior` tick) within `Mines.CLAIM_TICKS`; others check it lazily when they pick. Claims are freed when the section is done, by `Mines.release` (job end, death or statue, home relink) and by `releaseAll` when the frame is found broken. `MineBehavior` digs the section it holds a claim on, drops its in-memory step if the claim lapsed, waits at home when `pick` finds nothing, saving progress after each step, checks the frame when a section starts and after unloading at home, and cuts a section short (on to its rails) when it meets a fluid without filler. It walks out and home along the Mine Path (`MinePath`: the opening, then each ancestor's start, merged into straight legs of at most 32 blocks) with `setRequiredPathLength(64)`. Stuck on the way out it skips that work for the trip (`pick`'s `skipped`); stuck on the way home it walks straight once, then sets `MinerData`'s unsaved stuck flag, which the status reads. With nothing free in loaded chunks it walks towards `Mines.unloadedWork` without a claim. It refreshes a `bettercoppers:miner` chunk ticket only while a non-spectator player is within Mine Range + 32 of the entrance (`MineBehavior.isWatched`, ADR 0011).

## Generated files

- `tools/gen_guide.py` writes advancements, the handbook and all guide texts for both mods. Change guide content there and rerun it: it overwrites everything it generates and merges its keys into the lang files.
- `tools/gen_automatisation_resources.py` writes the add-on's blockstates, models, item definitions, loot tables, recipes and tags.
- `tools/*.java` generate textures, as listed in `tools/README.md`.
- `tools/gen_wiki_icons.py` writes `wiki/public/icons/`. Crafting grids in the wiki are built from the recipe JSON, so a recipe change reaches the wiki without an edit, unless it uses a new tag (`recipes.data.mts`) or item with no icon yet.

## Tests

Gametests build their scene with `setBlock` and hand items over through `UseEntityCallback` with a mock player, so they exercise the real player path. `VanillaGameTests` guards the vanilla rule. The add-on has gametests of its own (`automatisation/src/gametest`). Playtesting in the real game is done by hand.

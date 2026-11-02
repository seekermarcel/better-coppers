# Architecture

The flows and traps that no single file shows. Vocabulary is in [`GLOSSARY.md`](../GLOSSARY.md), reasons are in [`adr/`](adr/).

## How a golem decides what to do

`CopperGolemAiMixin` puts `GolemBrain.modBehaviours()` in front of vanilla's idle activity. Vanilla's behaviours stay in the list, and `TransportItemsBetweenContainersMixin` blocks vanilla transport for a golem with a job or on an errand.

Each behaviour decides for itself whether it applies, in its start conditions, and order in `modBehaviours()` matters:
- `RideBehavior` is last among priority 0, so it sees the walk target the job behaviours just set.
- `HomeStroll` is priority 1, so it runs after every job and stops vanilla's stroll from starting.
- Add-on behaviours from the API come after the base jobs and before `RideBehavior`.

## How a player action reaches a golem

Right-clicks run through the `UseEntityCallback` handlers in the order `BetterCoppers.onInitialize` registers them, and the first result other than `PASS` wins. `Tools` is first, so non-copper tools are refused before a job takes them. `HomeLinking` comes before the job handlers, so it gets sneak + empty hand first.

## What a job is in code

A job is derived from what the golem holds and which data it has, with no stored job field. Miners and the `Job` enum's jobs need both the tool and a player-created attachment, which is what stops an untouched golem from gaining a job by picking up a tool. A sorter holds its bundle in the off hand, which vanilla transport leaves alone, and a courier is defined by its route.

Several method names are narrower or older than they sound:

| Method | Actually means |
|---|---|
| `Upkeep.hasJob` | Has any job. This is the vanilla rule's predicate. |
| `Upkeep.isJobGolem` | Has a paying job, so not sorter or courier. |
| `WorkerData.isWorker` | Has one of the `Job` enum's jobs, or worker data and a load to bring home, so not miner. |
| `GolemGuards.isSettled` | Alive and has a job. It says nothing about a home. |
| `Upkeep.homeOf` | Home, else workplace, else the golem's current position. |

The `Job` enum holds only the five jobs that share `JobBehavior`. Miner, sorter and courier have their own code, and add-on jobs are `GolemJob`s.

Other names that differ from `GLOSSARY.md`: `Upkeep` is running costs, `GolemModifier` is the hook behind upgrades, `ChargePad` is an API hook for add-on charging blocks and is unrelated to the Charging Pad block, and `GolemBobberRenderer` draws the float.

## A golem with only a home

It sorts like vanilla, and the link also opts it into two things: `HomeStroll` walks it back when it strays, and `RepairBehavior` lets it repair iron golems. Sorters and couriers finish a delivery before starting a repair.

## State

Per-golem state lives in Fabric attachments (`ModAttachments`), and most of it is saved. `Load` holds pouch and supplies in `LOAD`; `WORKER` and `MINER` stay the signal that a player gave the job, and `LOAD` never is. `Load.hasNothingToBringHome` ignores supplies, which stay with the golem. These are lost on restart: the `ERRAND` attachment and the static maps `Couriers.PENDING`, `HomeLinking.SELECTIONS` and `RepairBehavior.CLAIMS`.

Behaviours keep per-golem fields, so every golem needs its own instance. The API takes factories for this reason.

There are no save migrations because no version has been released. That ends with the first release: from then on, changing a saved format needs a migration.

## Generated files

- `tools/gen_guide.py` writes advancements, the handbook and all guide texts for both mods. Change guide content there and rerun it: it overwrites everything it generates and merges its keys into the lang files.
- `tools/gen_automatisation_resources.py` writes the add-on's blockstates, models, item definitions, loot tables, recipes and tags.
- `tools/*.java` generate textures, as listed in `tools/README.md`.

## Tests

Gametests build their scene with `setBlock` and hand items over through `UseEntityCallback` with a mock player, so they exercise the real player path. `VanillaGameTests` guards the vanilla rule. The add-on has no tests. Playtesting in the real game is done by hand.

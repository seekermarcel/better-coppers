# Better Coppers

Fabric mod for Minecraft 26.3 (Java 25) that gives copper golems jobs: sorting, mining, farming, forestry, fishing, animal keeping, crafting, couriering. Also minecart/boat riding, monsters hunting golems, iron golem guards and repairs, armour trims. License LGPL-3.0.

## Commands
Everything runs in the flake dev shell:
- `nix develop -c ./gradlew build` runs unit tests (`src/test/java`) and gametests (`src/gametest/java`), and makes the jar in `build/libs/`.
- `nix develop -c ./gradlew test` or `runGameTest` runs one of them.
- `nix develop -c ./gradlew runClient` starts the game.

## Package map (`src/main/java/dev/bettercoppers/`)
- `api`: BetterCoppersApi, TravelWatch, hooks for add-ons (jobs, behaviours, link targets, upgrades, charging spots, handbook)
- `golem`: GolemBrain (registers behaviours), Golems, Inventories, ItemFamilies, Sorter, SortingDestinations, SortItemsBehavior, WorkingStock, ChestVisit, Errands, RideBehavior, CartChaining, Eyes, HomeStroll
- `job`: Jobs (what a golem's job is and where it belongs), Load (what it carries), Job enum, JobHandover (starting and ending jobs), JobBehavior, WorkerData, WorkerInteractions, Tools, ToolFetchBehavior, Farm/Forest/Fish/Keeper/CraftBehavior
- `guide`: Golem Handbook, guide advancements
- `mining`: miner job, mine layout and plan
- `courier`: routes and chunk tickets
- `home`: home linking and particle lines
- `runningcosts`: energy, rust, scraping, charging, axe dispenser
- `combat`, `repair`: monsters, iron golem guards, repairs
- `trim`: armour trims
- `block`, `menu`, `entity`, `item`, `registry`: Survey Table, Copper Chest Minecart, registrations
- `mixin`: adds the mod's behaviours in front of vanilla's golem AI, and gates vanilla transport
- Client code is in `src/client/java`. `tools/` holds texture and resource generators. `automatisation/` is the add-on subproject (its own jar), imports only `api` (ADR 0004).

## Docs
- `GLOSSARY.md`: canonical terms. Use them when naming things or writing player-facing text.
- `docs/architecture.md`: read before changing golem AI, player interactions, saved state or generated files. It also lists code names that mislead.
- `docs/adr/`: why things are as they are. Read before reversing a design choice, and add one for a decision that is hard to reverse, surprising and a real trade-off.
- `CONTRIBUTING.md`: the steps for adding a job and for releases.

## The vanilla rule
Anything that happens automatically to a golem no player touched is exactly vanilla. Mod behaviour starts only after a deliberate player action (job item, route, home link, chain, trim, eyes). "Without a job" is `!Jobs.has(golem)`, even if the golem has a home: a golem with only a home (no job item, no route) still sorts like vanilla. The one exception is the axe dispenser, which scrapes any golem in front of it, because the player built it on purpose.

A plain click on a golem is safe (ADR 0009): with an empty hand it is exactly vanilla for a golem without a job and only shows the status of one with a job (and cancels its pending link), and shears, honeycomb and axes stay vanilla for every golem, except that on a golem with a job a plain axe or inert item cannot throw the tool out through vanilla's fall-through to the empty off hand; scraping still works. Giving a golem anything, or taking its trim off with shears, takes sneak + right-click.

## Conventions
Tabs, short Javadocs, English. Conventional commit messages. Never commit `docs/superpowers/` (gitignored). Only the latest Minecraft version is supported (ADR 0005).

Any change players can notice updates the Golem Handbook in the same commit, in English and German, through `tools/gen_guide.py`.

The add-on imports from the base mod only through `dev.bettercoppers.api`. If it needs something else, add a hook to `api`. Do not import the internal class (ADR 0004).

## Agent skills

### Issue tracker

GitHub Issues on origin (`seekermarcel/better-coppers`), via `gh`. See `docs/agents/issue-tracker.md`.

### Triage labels

The five default labels, unrenamed. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: `GLOSSARY.md` and `docs/adr/` at the root. See `docs/agents/domain.md`.

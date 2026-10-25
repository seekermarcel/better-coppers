# Better Coppers

Fabric mod for Minecraft 26.3 (Java 25) that gives copper golems jobs: sorting, mining, farming, forestry, fishing, animal keeping, crafting, couriering. Also minecart/boat riding, monsters hunting golems, iron golem guards and repairs, armour trims. License LGPL-3.0.

## Commands
Everything runs in the flake dev shell:
- `nix develop -c ./gradlew build` runs unit tests (`src/test/java`) and gametests (`src/gametest/java`), and makes the jar in `build/libs/`.
- `nix develop -c ./gradlew test` or `runGameTest` runs one of them.
- `nix develop -c ./gradlew runClient` starts the game.

## Package map (`src/main/java/dev/bettercoppers/`)
- `golem`: GolemBrain (registers behaviours), Golems, Inventories, ItemFamilies, Sorter, SortItemsBehavior, Errands, RideBehavior, CartChaining, TravelWatch
- `job`: Job enum, JobBehavior, WorkerData, WorkerInteractions, Farm/Forest/Fish/Keeper/CraftBehavior
- `mining`: miner job, mine layout and plan
- `courier`: routes and chunk tickets
- `home`: home linking and particle lines
- `upkeep`: energy, rust, scraping, charging, axe dispenser
- `combat`, `repair`: monsters, iron golem guards, repairs
- `trim`: armour trims
- `block`, `menu`, `entity`, `item`, `registry`: Survey Table, Copper Chest Minecart, registrations
- `mixin`: swaps in the golem AI
- Client code is in `src/client/java`. `tools/` holds texture generators.

Adding a job: one `Job` entry, one `JobBehavior` subclass registered in `GolemBrain.modBehaviours()`, lang keys in en_us and de_de (key-identical), one gametest.

## The vanilla rule
Anything that happens automatically to a golem no player touched is exactly vanilla. Mod behaviour starts only after a deliberate player action (job item, route, home link, chain, trim). "Without a job" is `!Upkeep.hasJob(golem)`.

## Conventions
Tabs, short Javadocs, English. Conventional commit messages. Never commit `docs/superpowers/` (gitignored).

## Multi-version
Only the latest Minecraft version is supported for now. When more are needed, use [Stonecutter](https://stonecutter.kikugie.dev/): it builds one jar per MC version from one source tree using comment-based version conditionals. CI would then need a matrix per version.

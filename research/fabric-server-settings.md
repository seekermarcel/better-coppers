# How can a Fabric mod on Minecraft 26.3 expose server settings?

Research for [#33](https://github.com/seekermarcel/better-coppers/issues/33) (map [#25](https://github.com/seekermarcel/better-coppers/issues/25)). Researched 2026-11-17.

The mod has no config system or game rules today. Its tuning numbers are `static final int` constants, for example `MinePlan.MAX_LENGTH = 128` and `MineBehavior.ORE_BUDGET = 16` in `src/main/java/dev/bettercoppers/mining/`. The question is how to make a few server-side numbers configurable: the walking-distance limit, the depth floor, the search radius, the vein cap and maybe the section weights.

## Versions in this repo

From `gradle.properties`:

- `minecraft_version=26.3`, `loader_version=0.20.1`, `loom_version=1.19.1`
- `fabric_api_version=0.164.0+26.3`. This bundles `fabric-game-rule-api-v1` 4.0.10 and `fabric-resource-loader-v1` 3.1.0. To confirm, list `META-INF/jars/` in `fabric-api-0.164.0+26.3.jar` in the Gradle cache.

`fabric.mod.json` already depends on `fabric-api: *`, so the game rule API needs no new dependency.

## Option 1: custom game rules (vanilla 26.3 plus Fabric API)

**Registration.** The Fabric API in this repo has `net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder` (checked with `javap` on the jar):

- `forBoolean(boolean)`, `forInteger(int)`, `forDouble(double)` and `forEnum(E)`
- `IntegerRuleBuilder.minValue(Integer)` and `range(Integer, Integer)`
- `category(GameRuleCategory)` and `buildAndRegister(Identifier)`, which returns `net.minecraft.world.level.gamerules.GameRule<T>`

`buildAndRegister` puts the rule into `BuiltInRegistries.GAME_RULE`, which is a real registry in 26.3. Vanilla `GameRules` registers its own rules the same way, through private `registerInteger(name, category, default, min[, max])` helpers.

The vanilla categories are `PLAYER`, `MOBS`, `SPAWNING`, `DROPS`, `UPDATES`, `CHAT` and `MISC`. `GameRuleCategory.register(Identifier)` adds a custom one, for example a "Better Coppers" group. `GameRuleEvents.changeCallback(rule)` fires `(value, server)` when a rule changes, in case a cached value has to be refreshed.

Source: the Fabric docs page [Game Rules](https://docs.fabricmc.net/develop/game-rules), which targets 26.2 and matches the 26.3 jar signatures above.

```java
public static final GameRule<Integer> MINE_DEPTH_FLOOR = GameRuleBuilder.forInteger(-59)
		.range(-64, 320).category(BETTER_COPPERS).buildAndRegister(BetterCoppers.id("mine_depth_floor"));
// read:
int floor = level.getGameRules().get(MINE_DEPTH_FLOOR);
```

**Types.** Vanilla `GameRuleType` has only `INT` and `BOOL`. Fabric adds double and enum through `FabricGameRuleTypeVisitor`, plus its own GUI entries (`impl.gamerule.entry.DoubleRuleEntry` and `EnumRuleEntry`). All the numbers we want are ints, so the vanilla types are enough.

**Per world or global.** The values are per save. `ServerLevel.getGameRules()` delegates to `MinecraftServer.getGameRules()`, so the overworld and the Nether share one set (`getGlobalGameRules()` returns the overworld's). The server loads that set from the saved data `GameRuleMap.TYPE`, id `game_rules`, in the world's `data/` folder. A new world starts at the defaults set in code.

**Client sync.** None by default, and none is needed for server logic. Vanilla sends `ClientboundGameRuleValuesPacket` only when a player with `Permissions.COMMANDS_GAMEMASTER` asks for the values (`ServerGamePacketListenerImpl.sendGameRuleValues`). The reverse packet `ServerboundSetGameRulePacket` checks the same permission. Our rules are read only on the server, which is where golem AI runs.

**Editing for players.**

- `/gamerule bettercoppers:mine_depth_floor -40`, with tab completion and range checks.
- The Edit Game Rules screen when a world is created (`WorldCreationGameRulesScreen`).
- The in-world game rules screen for operators (`InWorldGameRulesScreen`, which sends `ServerboundSetGameRulePacket`).
- The dedicated server's JSON-RPC management API (`server.jsonrpc.methods.GameRulesService`). Fabric patches it for its own types in `GameRulesServiceGameRuleUpdateMixin`.

Every rule needs translation keys for its name and description (`gamerule.<namespace>.<path>`, see the Fabric docs page). This repo needs them in English and German.

**Code size.** About 2 lines per rule, one category, about 4 lang lines per rule per language, and a `get` at each place the value is used. There is no file I/O, no parsing and no new dependency.

**Limits.**

- One flat value per rule. A list or map, such as section weights, would need one int rule per weight.
- Each server operator has to set the values in each world. There is no server-wide default file.

## Option 2: a plain config file

**Registration.** None. Read and write JSON with Gson, which ships with Minecraft, at `FabricLoader.getInstance().getConfigDir().resolve("bettercoppers.json")`. Load it in `onInitialize`, and write the defaults if the file is missing.

**Per world or global.** Global for the instance or server (`config/`). It is not per world, and a singleplayer player with several worlds gets the same values in each.

**Client sync.** None. A singleplayer client reads its own file, and a dedicated server reads the server's.

**Editing.** Edit the text file, then restart, or add a reload command ourselves. There is no in-game UI unless we build one or add a library.

**Code size.** About 40 to 80 lines: a record with defaults, load and save, and range clamping. Hand-written validation has to clamp bad values, because the file is untrusted input at a trust boundary.

## Option 3: config libraries

Fabric builds for 26.3, from the Modrinth API (`/v2/project/<slug>/version?loaders=["fabric"]&game_versions=["26.3"]`, queried 2026-11-17):

| Library | 26.3 Fabric build | Notes |
|---|---|---|
| Cloth Config | 26.3.159+fabric (2026-09-23) | Annotated config class (AutoConfig) and a generated screen. Global file. |
| YACL | 3.9.7+26.3-fabric (2026-09-20) | Config handler and a screen builder. Global file. |
| MidnightLib | 1.10.0+26.3-fabric (2026-10-14) | Small annotated config with a screen. Global file. |
| Forge Config API Port | 26.3.1 (2026-09-21) | NeoForge-style TOML specs, with per-world "server" configs and sync. |
| owo-lib (owo-config) | **none**: the latest is 0.13.1+26.2 (2026-08-19) | Not usable on 26.3 today. |
| Mod Menu | 21.0.1 (2026-11-12) | Only adds the button that opens a library's screen. |

All of them add a runtime dependency that players and servers must install, or that we must jar-in-jar. They lag behind each Minecraft release (owo-lib shows this right now), and ADR 0005 commits us to following the latest version. They are client-UI oriented: the generated screens edit a local file, so on a dedicated server you still edit a file by hand. Forge Config API Port is the only one with per-world server configs that are built in and synced. Code size is about 20 to 40 lines of annotated class, plus a build dependency and a `fabric.mod.json` dependency.

## Option 4: data packs

**Registration.** Either `DataResourceLoader.get().registerReloadListener(id, provider -> listener)`, in `fabric-resource-loader-v1` 3.1.0 (shipped in this Fabric API, checked with `javap`), or a dynamic registry through Fabric's dynamic registry API. Read `data/bettercoppers/<something>.json` and parse it with a Codec.

**Per world or global.** Per world: data packs are enabled for each world. Datapack values can also be shipped as a built-in pack (`ResourceLoader.registerBuiltinPack`).

**Client sync.** Not by default for a reload listener. Dynamic registries can opt into sync.

**Editing.** Players have to write a data pack (a folder or zip with `pack.mcmeta` and JSON) and run `/reload`. This is the heaviest UX of the four for a single number. It suits structured data such as section weights or block lists, which is what data packs are for.

**Code size.** About 50 to 100 lines: a codec, a reload listener, a holder for the current values, and default data files.

## Comparison

| | Game rules | Plain file | Library | Data pack |
|---|---|---|---|---|
| New dependency | no (Fabric API) | no | yes | no |
| Scope | per save | per instance | per instance (FCAP: per world too) | per world |
| In-game edit | `/gamerule` and vanilla screens | no | client screen | `/reload` after editing files |
| Dedicated server edit | command, RPC API | text file | text file | files and `/reload` |
| Client sync | not needed | not needed | depends | not needed |
| Structured values | no (flat int/bool/double/enum) | yes | yes | yes |
| Rough code | ~2 lines per rule + lang | ~60 lines | ~30 lines + dependency | ~80 lines |

## Recommendation (not a decision; the grilling ticket decides)

Use **custom integer game rules** through Fabric's `GameRuleBuilder` for the scalar mining settings: the walking-distance limit, the depth floor, the search radius and the vein cap.

- They need no new dependency or file format.
- `range(min, max)` validates the values.
- Operators can already reach them through `/gamerule`, the world-creation screen and the in-world screen.
- They are per save, which fits "this server's mine rules".
- The code is the smallest of the four options.

If section weights do become configurable, they are structured data. Choose then between one int game rule per section and a data pack JSON, depending on how many sections there are. Do not add a config library for this: owo-lib has no 26.3 build, and a library would tie our releases to its update schedule.

The open point for the grilling ticket is the vanilla rule. Game rules only change behaviour for golems a player has already given a job, such as the miner, so they do not affect untouched golems. Each rule also needs a Handbook and wiki mention, because players can notice it.

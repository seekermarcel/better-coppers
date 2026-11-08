# Contributing

## Setup
`nix develop` gives you JDK 25 (see `flake.nix`). Without Nix, install any JDK 25. Use `./gradlew` for everything.

`./gradlew runClient` starts the game with the mod.

## Tests
- `./gradlew test`: JUnit tests in `src/test/java`.
- `./gradlew runGameTest`: Fabric gametests on a headless server, in `src/gametest/java`.
- `./gradlew build` runs both, so run it before you push.

## The vanilla rule
See [The vanilla rule](CLAUDE.md#the-vanilla-rule) and [why it works the way it does](docs/adr/0002-vanilla-rule-rests-on-one-predicate.md).

## Adding a job
These steps are for a job that shares the home-chest routine of `JobBehavior`. Miner, sorter and courier have their own code. An add-on job uses `BetterCoppersApi.registerJob` and `registerBehavior` instead of steps 1 to 3.

1. One entry in the `Job` enum.
2. One class extending `JobBehavior`.
3. Register it in `GolemBrain.modBehaviours()`.
4. Lang keys in both `en_us.json` and `de_de.json` (keep them key-identical).
5. Add the job to `tools/gen_guide.py` (advancements and handbook text) and rerun it. Then check the `Guide.job` switch: a job with no matching advancement is skipped silently.
6. One gametest in `JobGameTests`.
7. One article in `wiki/jobs/`, listed in the sidebar in `wiki/.vitepress/config.mts`.

## Code style
Tabs, English. Short Javadoc on anything non-obvious.

## Releases
Bump `version` in `gradle.properties`, tag `vX.Y.Z` and push the tag. CI builds, runs all tests and attaches the jar to a GitHub release.

The same run publishes both jars to Modrinth once these are set in the GitHub repository settings. The projects must already exist on Modrinth, and without the two variables the step is skipped:
- Secret `MODRINTH_TOKEN`: a Modrinth personal access token with the "Create versions" scope.
- Variables `MODRINTH_PROJECT_ID` and `MODRINTH_ADDON_PROJECT_ID`: the IDs of the base mod's and the add-on's projects.

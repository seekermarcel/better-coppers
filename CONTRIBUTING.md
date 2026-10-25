# Contributing

## Setup
`nix develop` gives you JDK 25 (see `flake.nix`). Without Nix, install any JDK 25. Use `./gradlew` for everything.

`./gradlew runClient` starts the game with the mod.

## Tests
- `./gradlew test`: JUnit tests in `src/test/java`.
- `./gradlew runGameTest`: Fabric gametests on a headless server, in `src/gametest/java`.
- `./gradlew build` runs both, so run it before you push.

## The vanilla rule
Anything that happens automatically to a golem no player touched is exactly vanilla. Mod behaviour starts only after a deliberate player action (job item, route, home link, chain, trim). "Without a job" means `!Upkeep.hasJob(golem)`, even if the golem has a home.

## Adding a job
1. One entry in the `Job` enum.
2. One class extending `JobBehavior`.
3. Register it in `GolemBrain.modBehaviours()`.
4. Lang keys in both `en_us.json` and `de_de.json` (keep them key-identical).
5. One gametest.

## Code style
Tabs, English. Short Javadoc on anything non-obvious.

## Releases
Bump `version` in `gradle.properties`, tag `vX.Y.Z` and push the tag. CI builds, runs all tests and attaches the jar to a GitHub release.

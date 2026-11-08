---
title: Installation
---

# Installation

**Better Coppers** is a Fabric mod for Minecraft: Java Edition. **Better Coppers Automatisation** is its optional add-on and comes as a separate jar. Both are installed like any other Fabric mod, by placing the jars in the `mods` folder.

## Requirements

| Requirement | Version |
|---|---|
| Minecraft | 26.3 (any 26.3 release) |
| Java | 25 or newer |
| Fabric Loader | 0.20.1 or newer |
| Fabric API | Required, any version made for Minecraft 26.3. The mods are built against 0.164.0+26.3. |

Only the latest Minecraft version is supported. There are no builds for older versions.

## The two jars

| Mod | Mod ID | Jar | Requires |
|---|---|---|---|
| Better Coppers | `bettercoppers` | `bettercoppers-<version>.jar` | Fabric Loader, Fabric API |
| Better Coppers Automatisation | `bettercoppers_automatisation` | `bettercoppers-automatisation-<version>.jar` | Fabric Loader, Fabric API, and Better Coppers of exactly the same version |

The add-on does nothing without the base mod and the game refuses to start if the versions differ. The base mod works without the add-on. Both jars are published together, with one version number. The add-on adds the [machines](/automatisation/machines), the [operator](/jobs/operator) job and the [golem upgrades](/automatisation/upgrades); see [Automatisation](/automatisation/).

Both jars are attached to each release on the [GitHub Releases page](https://github.com/seekermarcel/better-coppers/releases).

## Installing

1. Install Minecraft 26.3 with Fabric Loader 0.20.1 or newer.
2. Put Fabric API in the `mods` folder.
3. Put `bettercoppers-<version>.jar` in the `mods` folder.
4. To use the add-on, also put `bettercoppers-automatisation-<version>.jar` in the `mods` folder.

## Clients and servers

Both mods have code for both sides and must be installed on both sides.

| Setup | Where the jars go |
|---|---|
| Singleplayer | The client's `mods` folder. |
| Dedicated server | The server's `mods` folder, and the `mods` folder of every player who joins. |

The mods add blocks, items and an entity, so every player needs the same jars as the server. All golem behaviour runs on the server.

Neither mod has a configuration file, commands or game rules of its own.

## First start

A player gets a [Golem Handbook](/items/golem_handbook) on first joining a world, as the reward of the root advancement of the Better Coppers tab. A lost copy can be crafted from a book and a copper ingot. The handbook and all mod text exist in English and German.

The next step is [your first golem](/start/first-golem).

## Building from source

The repository builds with JDK 25. `./gradlew build` writes the base mod to `build/libs/` and the add-on to `automatisation/build/libs/`. The `-sources` jar in `build/libs/` is not a mod and does not go in the `mods` folder.

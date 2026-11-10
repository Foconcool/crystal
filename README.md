# Crystal Route Cleaner (Fabric 26.2)

A client-side **source project** for Minecraft Java 26.2. Works independently of Polar's internal code. It imports Polar-style gemstone route JSON, interprets each `VEIN` position as a cobblestone Etherwarp landing platform, approximates obstructions on sight lines between consecutive platforms, and offers a four-tab in-game GUI.

**This is source code, not a tested release JAR.** The environment in which this project was generated had no Java 25 / Gradle / Minecraft artifacts or network access, so the complete Fabric project could **not** be compiled or tested in Minecraft here. The standalone analyzer self-test is provided and has been run with Java 21 (9 checks). You may need to make version-specific adjustments in an IDE after resolving Fabric dependencies.

## Features

- Native in-game GUI (Dashboard, Routes, Scanner, Settings).
- K opens the GUI; J stops local auto mining.
- Import a copied Polar-style JSON route from the clipboard, or put `.json` files in `config/routecleaner/routes/` and click Reload Folder.
- Includes **Floating Sand Ruby** example from the supplied 36-waypoint JSON.
- Cobblestone at each VEIN coordinate is treated as a protected landing block.
- Samples multiple Etherwarp line-of-sight candidates, with headroom checks.
- Classifies **Stone** and **Cyan Terracotta** as hardstone; optional ores via vanilla `_ore` block IDs.
- Never selects stained glass, stained glass panes, other glass, cobblestone platforms, or unlisted blocks for mining.
- Ranks reachable targets by nearby planned obstruction blocks; actual Mining Spread is server-controlled, not simulated or forced.
- Marks missing landing platforms, unloaded chunks, protected obstructions and out-of-range connections.
- Optional re-scanning while playing.
- **Automatic mining only in an integrated single-player world** for testing. On multiplayer networks including Hypixel, the mod is a preview / route-inspection assistant. No anti-cheat evasion or ban guarantees.

## Setup

1. Install **Java 25** and **Gradle 9.5.1 or newer compatible 9.x release**. Fabric 26.2 uses non-obfuscated Minecraft names and Fabric Loom 1.17.
2. Open this folder as a Gradle project in IntelliJ IDEA. Let Gradle download Fabric Loom, Fabric API and Minecraft 26.2. An Internet connection is required.
3. Run `gradle build` or, if you prefer a wrapper, generate one using `gradle wrapper --gradle-version 9.5.1` then run `./gradlew build` (or `gradlew.bat build` on Windows).
4. If it builds, copy the normal (non-sources) JAR from `build/libs/` into a compatible Fabric 26.2 `mods` folder. Fabric API is required.
5. Launch a game, open your favorite instance, copy `example-routes/floating-sand-ruby.json` into `config/routecleaner/routes/`, press **K**, then choose **Reload folder**.
6. Go near a waypoint in the Crystal Hollows or a test world, and use **Scan route**. Some connections will be `UNLOADED` until those chunks are loaded; missing platforms are shown separately.

## How scans work

A sample ray runs from the player's approximate eye position above waypoint A to nine points near the top of the cobblestone block at waypoint B. For each ray the planner counts permitted and protected obstacles. It picks the lowest-cost unobstructed (or cheapest removable) sight line, then checks the two headroom blocks above both landing platforms. This is intentionally an **approximation**: it is not a byte-for-byte reproduction of Hypixel's Etherwarp checks.

Only blocks already loaded on the client are inspected. No automatic movement or Etherwarp is performed. Missing cobblestone does not get placed automatically. On multiplayer, no blocks are mined by the mod. In single-player, the optional auto mine targets line-of-sight obstructions within 2.5–4.5 blocks through ordinary `MultiPlayerGameMode` calls; if no target is reachable it stops. The automatic feature is **disabled by default**.

**Important limits:** The server can choose spread-affected neighbors, not the mod. The GUI's hardstone / ore whitelist applies to direct planned targets, not to server-side secondary breaks. The world scan does not verify whether every glass block is a gemstone; it conservatively excludes all glass. Hypixel treats automation as disallowed — do not assume the preview feature is approved by Hypixel either.

## Standalone core self-test

With Java 21 or newer (Java 25 is needed for the full Minecraft target):

```sh
mkdir -p /tmp/routecleaner-test
javac -d /tmp/routecleaner-test src/main/java/dev/routecleaner/core/*.java src/test/java/dev/routecleaner/core/AnalyzerSelfTest.java
java -cp /tmp/routecleaner-test dev.routecleaner.core.AnalyzerSelfTest
```

## Source layout

- `core/` — Pure Java route geometry, classification, spread ranking and scan results.
- `storage/` — Route JSON import and persisted settings.
- `client/WorldGrid` — Minecraft block adapter.
- `client/CleanerController` — Local test-world block breaking, never on remote servers.
- `client/RouteCleanerScreen` — Four-tab native Minecraft GUI.
- `client/RouteCleanerClient` — Fabric entry point and keybindings.

No Polar reverse engineering is needed and no third-party macro code is included.

## Links

- [Fabric documentation](https://docs.fabricmc.net/develop/)
- [Fabric's 26.2 update notes](https://fabricmc.net/2026/06/15/262.html)

## Continuous build (recommended when you do not have Gradle set up locally)

The repository contains `.github/workflows/build.yml`. After uploading the source directory to a GitHub repository, open **Actions → Build Fabric 26.2 mod → Run workflow**. It installs Temurin 25 and Gradle 9.5.1, runs `gradle clean build` plus the core checks, and if compilation succeeds uploads `crystal-route-cleaner-fabric-26.2` as an artifact containing the non-sources JAR. GitHub Actions requires a runner with access to the Fabric, Minecraft and Maven repositories.

The presence of this workflow does **not** mean the full mod has already been compiled: the source package is only validated with offline core tests in this environment, and real dependency/API build errors may still occur.

For a quick dependency-free check on Linux/macOS with Java 21+, run `./scripts/check-core.sh`.

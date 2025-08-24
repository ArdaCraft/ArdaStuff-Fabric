# ArdaStuff (Fabric)

ArdaStuff is a collection of quality-of-life tweaks and utilities for a Fabric-based Minecraft server used by the ArdaCraft community. It registers server-side commands, gameplay adjustments via mixins, and small protections to keep the server experience smooth.

This repository targets modern Fabric/Minecraft versions (see gradle.properties or fabric.mod.json for exact version constraints).

## Features

- Server utility commands:
  - /guide – gives a Patchouli guide book for players
  - /cwaterspread – toggles water spread behavior per-player (permission-gated)
  - /nightvision and /nv – toggle permanent Night Vision for the executing player
  - /sauronsays <message> – broadcast a styled server message (permission-gated)
  - /mount – spawn and mount a horse
  - /boat – spawn and mount a boat
- Event-based gameplay protections and tweaks (via Stimuli and mixins), e.g.:
  - Disable fire tick, ice melt, wither spawning, snow falling, etc., as configured in ArdaStuff
  - Safe handling for projectiles hitting fragile entities like paintings and item frames
  - Auto-teleport players falling into the void in the Overworld back to spawn
- Scheduled auto-saving with graceful shutdown handling
- LuckPerms integration for permission checks

## Requirements

- Java 17+
- Fabric Loader compatible with the targeted Minecraft version
- Fabric API
- LuckPerms (for permissions)
- Stimuli (see libs or build.gradle)
- Any optional mods referenced by features you use (e.g., Patchouli, Create)

Refer to build.gradle and the libs/ directory for exact dependencies and versions.

## Building from Source

1. Ensure Java 17+ is installed and available on PATH.
2. Clone the repository.
3. Run the Gradle build:
   - On Windows: `gradlew.bat build`
   - On Unix-like systems: `./gradlew build`
4. The compiled jar will be in `build/libs/`.

## Installing

- Place the built jar into your server's `mods/` directory along with Fabric API and other required mods.
- Ensure config and permissions are set appropriately for your environment (LuckPerms nodes are used by several commands and features).

## Usage Overview

- Use `/guide` to receive the ArdaCraft Patchouli guide book (requires Patchouli and the configured book id).
- Moderation and utility commands:
  - `/cwaterspread` toggles a water-spread behavior for the executing player and requires the `metatweaks.cwaterspread` permission.
  - `/nightvision` or `/nv` toggles Night Vision effect on the executing player.
  - `/sauronsays <message>` broadcasts a server-wide message; requires elevated permission level (2+).
  - `/mount` and `/boat` spawn and mount respective entities for quick traversal/testing.

## Development Notes

- Main entrypoint: `com.ardacraft.ardastuff.ArdaStuff` (implements `ModInitializer`).
- Command registration: `com.ardacraft.ardastuff.ArdaStuffCommandHandler#ArdaStuffCommands`.
- Mixins: `src/main/java/com/ardacraft/ardastuff/mixin/` and configured via `src/main/resources/ardastuff.mixins.json`.
- Permissions are checked with LuckPerms, typically via `LuckPermsProvider` and custom nodes like `metatweaks.protection`.

## Contributing

Pull requests are welcome. Please:
- Keep changes focused and well-documented.
- Follow existing code style and conventions.
- Add or update JavaDoc/comments for public classes and methods.

## License

This project is licensed under the terms specified in the `LICENSE` file.

---
Generated/updated on: 2025-08-23 19:07 (local time)

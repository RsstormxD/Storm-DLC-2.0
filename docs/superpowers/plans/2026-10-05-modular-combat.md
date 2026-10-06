# Storm DLC 2.0 Modular System Implementation Plan

> Implementation is performed in this session, respecting the user's request not to run tests.

**Goal:** Integrate friends, lifecycle cleanup, silent combat rotations,
prediction ESP and block automation into the existing Fabric ClickGUI.

**Architecture:** Shared friends, targeting, rotation, prediction and placement
services; centrally dispatched module callbacks; a dedicated friends editor.

**Tech Stack:** Java 21, Fabric Minecraft 1.21.4, Mojmap, Loom 1.13.6, Gradle 8.14.

**Spec:** ../specs/2026-10-05-modular-combat-design.md

## Global Constraints

- Keep Storm DLC 2.0 and mod version 2.0.0+1.21.4.
- Do not add or run tests, launch Minecraft or introduce a singleplayer gate.
- Preserve existing rendering features and persisted configurations.

## Review Focus

- Disconnect or replacement of player/world must release target and input state.
- Adding a selected target as a friend must invalidate all combat actions immediately.
- Camera angles must remain unchanged through model rendering and packet submission.
- Placement failure must restore both selected hotbar slot and the server slot.
- Normal and rapid toggles must close subscriptions exactly once.

## Tasks

- [x] Back up sources; migrate Java and Mixin references to Mojmap; update Gradle.
- [x] Implement FriendsManager and editor; wire FRIENDS and COMBAT into ClickGUI.
- [x] Implement client-thread lifecycle and managed resources, world reset and rendering dispatch.
- [x] Implement common target selection, enum settings, prediction, silent rotations and render hook.
- [x] Replace KillAura with the requested filters, cooldown, packet rotation and ESP logic.
- [x] Implement AutoTrap/AutoWeb using common placement validation and slot restoration.
- [x] Adapt remaining target consumers, config compatibility and documentation.
- [x] Assemble with tests excluded, package the JAR and document remaining publication requirement.

Validation: `assemble -x test -x compileTestJava -x processTestResources` completed successfully. No tests or Minecraft runtime were launched. Release metadata and the visual-rotation refmap were read from the packaged JAR. GitHub publication remains pending the repository address.

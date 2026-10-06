# Storm DLC 2.0 — modular combat and friends

Implement the user's Fabric 1.21.4 specification inside the existing ClickGUI.
Keep Storm DLC 2.0 branding, Java 21, existing visual modules, configuration,
animated Discord RPC and the prepared GitHub release integration. Migrate the
whole project from Yarn to official Mojang mappings, including mixins.

Use one persistent, normalized, reactive friends service with immutable
snapshots and closeable subscriptions. Keep the old FriendManager API as a
compatibility facade. Provide a dedicated Friends category and editor screen.

Share target validation and sorting between KillAura, AutoTrap and AutoWeb.
Every selection and action must revalidate name/UUID friend membership, world,
health, entity identity, line of sight and range. Combat belongs to COMBAT.

Module lifecycle transitions run on the client thread. Modules own closeable
resources and release them even if disable cleanup fails. Disconnect, respawn,
world replacement, death and missing interaction manager invalidate live state.
Callbacks are centrally registered once and dispatch only to enabled modules.

Silent rotations maintain independent yaw/pitch state. Only an actual attack
sends a movement rotation packet before the attack packet and restores the
camera rotation afterward. Never write the local player's camera yaw/pitch.
Visual rotations temporarily apply body/head/previous angles around model-state
extraction, restore them immediately and reset on disable. Presets: GCD
quantization, eased interpolation and visible AABB sampling with raycasting.

Elytra prediction integrates 3D velocity, bounded drag and gravity in substeps
and includes measured connection latency. The projected AABB is shown by True
Position ESP. Current server interaction limits, visibility and ray alignment
are checked before attacking; prediction cannot guarantee server acceptance.

Block automation uses a bounded placement grid and per-tick rate limit. It
requires air, a loaded chunk, a valid adjacent support face, reachable visible
hit point, allowed hotbar block and the block's normal placement rules. Slot
and network slot are restored in finally. Pending placements prevent packet
spam without changing the world speculatively. AutoWeb respects cobweb's
entity-collision behavior. No singleplayer-only condition is introduced.

User constraint: do not add or run tests. Validate by source review and assemble
without test tasks; do not launch Minecraft. Publication still needs the
GitHub repository address requested in the preceding task.

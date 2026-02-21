# CLAUDE.md — Diamond Bank Project Constitution

This file defines the rules and constraints for this project. When working with Claude on this codebase, treat these as immutable unless you explicitly decide to change them and update this file.

---

## Project Identity

- **Mod ID**: `diamondbank`
- **Maven group**: `com.jeffhutting.diamondbank`
- **Artifact**: `diamond-bank`
- **Version**: `1.0.0` (bump on releases)

---

## Tech Stack (Do Not Change Without Updating This File)

| Concern | Value |
|---|---|
| Minecraft version | 1.21.11 |
| Fabric Loader | 0.18.4 |
| Fabric API | 0.139.4+1.21.11 |
| Loom | 1.15-SNAPSHOT |
| Mappings | **Mojang official** (`loom.officialMojangMappings()`) |
| JDK | 21+ |

**Do not use Yarn mappings.** Yarn was discontinued after 1.21.11. All class, method, and field names must use Mojang official names.

---

## Mojang Mapping Cheat Sheet

Common names that differ from Yarn — use the Mojang column:

| Concept | Mojang (use this) | Yarn (don't use) |
|---|---|---|
| Command source | `CommandSourceStack` | `ServerCommandSource` |
| Player (server) | `ServerPlayer` | `ServerPlayerEntity` |
| Save data base class | `SavedData` | `PersistentState` |
| Save data type | `SavedDataType` | `PersistentStateType` |
| Save data storage | `DimensionDataStorage` | `PersistentStateManager` |
| Send success message | `source.sendSuccess(...)` | `source.sendFeedback(...)` |
| Send error message | `source.sendFailure(...)` | `source.sendError(...)` |
| Player UUID | `player.getUUID()` | `player.getUuid()` |
| Get inventory stack | `player.getInventory().getItem(i)` | `...getStack(i)` |
| Shrink item stack | `stack.shrink(n)` | `stack.decrement(n)` |
| Add to inventory | `player.getInventory().add(stack)` | `...insertStack(stack)` |
| Get overworld | `server.overworld()` | `server.getWorld(World.OVERWORLD)` |
| Load/create state | `storage.computeIfAbsent(TYPE)` | `manager.getOrCreate(TYPE)` |
| Mark data dirty | `setDirty()` | `markDirty()` |
| Text/chat | `Component` | `Text` |
| Command registration | `Commands` | `CommandManager` |

---

## Permission System

Minecraft 1.21.11 uses **semantic permission constants**, not integer op levels. Import from `net.minecraft.server.permissions.Permissions`.

| Constant | Equivalent op level |
|---|---|
| `Permissions.COMMANDS_MODERATOR` | 2 |
| `Permissions.COMMANDS_GAMEMASTER` | 3 |
| `Permissions.COMMANDS_ADMIN` | 4 |
| `Permissions.COMMANDS_OWNER` | 4+ |

Usage:
```java
.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
```

---

## Project Structure

```
src/main/java/com/jeffhutting/diamondbank/
  DiamondBank.java          # ModInitializer entrypoint
  DiamondBankServer.java    # (reserved for server-specific init if needed)
  commands/
    BankCommands.java       # All /bank command registration and handlers
  data/
    BankState.java          # SavedData subclass, Codec serialization, balance API
```

---

## Architecture Rules

- **Server-side only.** No `client` entrypoint. No `splitEnvironmentSourceSets()` in build.gradle.
- **One save file.** All player balances live in `world/data/diamondbank.dat` via `BankState`.
- **Access pattern.** Any class needing balances calls `BankState.getServerState(server)` — never hold a static reference to the state object.
- **Persistence.** Call `setDirty()` after every balance mutation. Never call save manually.
- **Commands.** Registered via `CommandRegistrationCallback.EVENT` in `BankCommands.register()`, called from `onInitialize()`.

---

## Known Gotchas

- `EntityArgument.getPlayer()` only resolves **currently online** players. Offline player pay is not supported without a different approach.
- `giveDiamonds()` loops in stacks of 64. If inventory is full, excess diamonds are dropped — this is Minecraft's default `inventory.add()` behavior.
- In singleplayer (`./gradlew runClient`), the player automatically has op level 4, so admin commands always pass.

---

## Dev Log

See `DEVLOG.md` for session notes, discoveries, and next steps.

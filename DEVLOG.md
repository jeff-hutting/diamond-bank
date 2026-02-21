# DEVLOG — Diamond Bank

Session notes, discoveries, and open questions. Most recent first.

---

## 2026-02-21 — Initial build, mappings migration, in-game test

### What was built
Full working economy mod: `/bank balance`, `deposit`, `withdraw`, `pay`, and admin commands (`give`, `set`). Data persists via `SavedData` + Codec to `world/data/diamondbank.dat`.

### Key discoveries

**Yarn mappings are dead.** Fabric discontinued Yarn after 1.21.11. The entire project was migrated to Mojang official mappings mid-session. This was the biggest learning moment — the Fabric docs had already switched, but the template still referenced Yarn. Lesson: always check the docs date and the mappings block in `build.gradle`.

**1.21.11 has a new permission system.** Integer op levels (`hasPermissionLevel(2)`) were replaced with semantic constants (`Permissions.COMMANDS_MODERATOR`). This isn't in most tutorials yet. Source of truth: decompile the actual `minecraft-merged.jar` from the Loom cache.

**`splitEnvironmentSourceSets()` in build.gradle will break a server-side-only mod.** The Fabric template adds this by default expecting a client source set. Removing it fixed a class-not-found compile error that looked completely unrelated.

**How to find the truth when docs fail:** Copy the jar from `.gradle/loom-cache/minecraftMaven/`, extract the `.class` file you care about, and read the bytecode. You'll see exact method signatures. This is faster than searching for outdated Stack Overflow answers.

### Commands tested in-game
- ✅ `/bank balance`
- ✅ `/bank deposit <amount>`
- ✅ `/bank withdraw <amount>`
- ✅ `/bank admin give <player> <amount>`
- ✅ `/bank admin set <player> <amount>`
- ⚠️ `/bank pay <player> <amount>` — untested (requires second online player)

### Open questions
- Does pay work correctly in multiplayer? (test on real server)
- What happens if inventory is full on withdraw? (should drop to ground — verify)
- Should deposit/withdraw have confirmation messages with updated balance? (already done, confirm UX feels right)

---

## Next Session Ideas

- Test `/bank pay` in actual multiplayer
- `/bank top` — leaderboard of top balances
- Interest system — passive balance growth over time
- Phase 2 planning: RPG class system

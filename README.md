# 💎 Diamond Bank

A server-side economy mod for Minecraft 1.21.11 built with Fabric. Players deposit and withdraw diamonds from a shared bank, pay other players, and carry virtual balances that persist between sessions.

No client mod required — works with any vanilla-compatible client.

---

## Features

- Virtual diamond balance stored per player (survives restarts)
- Deposit physical diamonds from inventory into the bank
- Withdraw diamonds back to inventory at any time
- Pay other online players directly from your balance
- Admin commands to grant or set balances (requires operator level 2+)

---

## Commands

| Command | Description |
|---|---|
| `/bank balance` | Check your current balance |
| `/bank deposit <amount>` | Deposit diamonds from your inventory |
| `/bank withdraw <amount>` | Withdraw diamonds to your inventory |
| `/bank pay <player> <amount>` | Transfer balance to another online player |
| `/bank admin give <player> <amount>` | *(Op)* Add to a player's balance |
| `/bank admin set <player> <amount>` | *(Op)* Set a player's balance to an exact amount |

Admin commands require operator permission level 2 or higher (`Permissions.COMMANDS_MODERATOR`).

---

## Installation

**Requirements:**
- Minecraft Java Edition 1.21.11
- [Fabric Loader](https://fabricmc.net/) 0.18.4+
- [Fabric API](https://modrinth.com/mod/fabric-api) 0.139.4+1.21.11

**Steps:**
1. Download the latest `diamond-bank-x.x.x.jar` from [Releases](../../releases)
2. Place it in your server's `mods/` folder alongside `fabric-api-*.jar`
3. Start the server — no configuration needed

Players do **not** need this mod installed on their client.

---

## Data Storage

Balances are saved to `world/data/diamondbank.dat` using Minecraft's native `SavedData` system with Codec-based NBT serialization. Data is written on every normal world save and on server shutdown.

---

## Building from Source

```bash
git clone https://github.com/jeffhutting/diamond-bank.git
cd diamond-bank
./gradlew build
# Output: build/libs/diamond-bank-1.0.0.jar
```

Requires JDK 21+. Uses Mojang official mappings (not Yarn).

---

## Technical Notes

- **Mappings**: Mojang official (Yarn was [discontinued after 1.21.11](https://fabricmc.net/))
- **Permission system**: Uses `Permissions.COMMANDS_MODERATOR` (semantic permission constants introduced in 1.21.11, replacing integer op levels)
- **Persistence**: `SavedData` + `SavedDataType` + Codec — the modern Mojang-mapped equivalents of the older Yarn `PersistentState` API
- **Server-side only**: Registered via `main` entrypoint only, no `client` entrypoint

---

## License

MIT — do whatever you want with it.

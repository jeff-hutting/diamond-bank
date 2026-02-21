package com.jeffhutting.diamondbank.data;

import com.jeffhutting.diamondbank.DiamondBank;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BankState extends PersistentState {

    // This is our ledger — every player's UUID mapped to their diamond balance
    // UUID is Minecraft's unique identifier for each player account
    private final Map<UUID, Long> balances;

    // Private constructor used by the codec when loading from disk
    private BankState(Map<UUID, Long> balances) {
        this.balances = new HashMap<>(balances);
    }

    // Public no-arg constructor for creating a fresh state (no save file yet)
    public BankState() {
        this.balances = new HashMap<>();
    }

    // -----------------------------------------------------------------------
    // CODEC — how the data is serialized to and from disk in 1.21.11+
    // -----------------------------------------------------------------------
    //
    // A Codec is Minecraft's modern serialization system. It describes how to
    // convert an object to a storable format (like NBT or JSON) and back again.
    //
    // We can't use UUID as a map key directly with Codec, so we store balances
    // as a Map<String, Long> on disk, converting UUIDs to/from strings.
    //
    // RecordCodecBuilder builds a codec for a class by describing each field.
    // Codec.unboundedMap defines a map codec.
    // Codec.STRING is the codec for String keys.
    // Codec.LONG is the codec for Long values.

    private static final Codec<Map<UUID, Long>> BALANCES_CODEC =
        Codec.unboundedMap(Codec.STRING, Codec.LONG)
            .xmap(
                // When LOADING: convert String keys back to UUIDs
                stringMap -> {
                    Map<UUID, Long> uuidMap = new HashMap<>();
                    stringMap.forEach((key, value) -> uuidMap.put(UUID.fromString(key), value));
                    return uuidMap;
                },
                // When SAVING: convert UUID keys to Strings
                uuidMap -> {
                    Map<String, Long> stringMap = new HashMap<>();
                    uuidMap.forEach((key, value) -> stringMap.put(key.toString(), value));
                    return stringMap;
                }
            );

    private static final Codec<BankState> CODEC =
        RecordCodecBuilder.create(instance -> instance.group(
            BALANCES_CODEC.fieldOf("balances").forGetter(state -> state.balances)
        ).apply(instance, BankState::new));

    // -----------------------------------------------------------------------
    // PERSISTENT STATE TYPE — registers this class with Minecraft's save system
    // -----------------------------------------------------------------------

    public static final PersistentStateType<BankState> TYPE = new PersistentStateType<>(
        DiamondBank.MOD_ID,   // Save file name: world/data/diamondbank.dat
        BankState::new,       // How to create a fresh state
        CODEC,                // How to save and load
        null                  // DataFixTypes — null is fine for a new mod
    );

    // -----------------------------------------------------------------------
    // PUBLIC API — how other classes interact with balances
    // -----------------------------------------------------------------------

    // Get a player's balance. Returns 0 if we've never seen them before.
    public long getBalance(UUID playerUuid) {
        return balances.getOrDefault(playerUuid, 0L);
    }

    // Set a player's balance directly.
    // markDirty() tells Minecraft "data changed, save it on the next world save."
    public void setBalance(UUID playerUuid, long amount) {
        balances.put(playerUuid, amount);
        markDirty();
    }

    // Remove diamonds from a player's balance.
    // Returns false if they can't afford it, so the caller knows the transaction failed.
    public boolean withdraw(UUID playerUuid, long amount) {
        long current = getBalance(playerUuid);
        if (current < amount) return false;
        setBalance(playerUuid, current - amount);
        return true;
    }

    // Add diamonds to a player's balance.
    public void deposit(UUID playerUuid, long amount) {
        setBalance(playerUuid, getBalance(playerUuid) + amount);
    }

    // -----------------------------------------------------------------------
    // HOW OTHER CLASSES GET ACCESS TO THIS STATE
    // -----------------------------------------------------------------------

    // Any class that needs to read or write balances calls this method.
    // It asks the overworld's PersistentStateManager to either load the
    // existing diamondbank.dat file or create a fresh one if none exists yet.
    public static BankState getServerState(MinecraftServer server) {
        PersistentStateManager manager = server
            .getWorld(World.OVERWORLD)
            .getPersistentStateManager();

        return manager.getOrCreate(TYPE);
    }
}

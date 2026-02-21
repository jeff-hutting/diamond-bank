package com.jeffhutting.diamondbank.data;

import com.jeffhutting.diamondbank.DiamondBank;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BankState extends SavedData {

    // Our ledger — every player's UUID mapped to their diamond balance.
    private final Map<UUID, Long> balances;

    // Private constructor used by the codec when loading from disk.
    private BankState(Map<UUID, Long> balances) {
        this.balances = new HashMap<>(balances);
    }

    // Public no-arg constructor for creating a fresh state (no save file yet).
    public BankState() {
        this.balances = new HashMap<>();
    }

    // -----------------------------------------------------------------------
    // CODEC — how the data is serialized to and from disk
    // -----------------------------------------------------------------------
    //
    // A Codec describes how to convert an object to a storable format (NBT/JSON)
    // and back. We store balances as Map<String, Long> on disk because UUID
    // isn't directly supported as a map key by Codec.
    //
    private static final Codec<Map<UUID, Long>> BALANCES_CODEC =
        Codec.unboundedMap(Codec.STRING, Codec.LONG)
            .xmap(
                // LOADING: convert String keys back to UUIDs
                stringMap -> {
                    Map<UUID, Long> uuidMap = new HashMap<>();
                    stringMap.forEach((key, value) -> uuidMap.put(UUID.fromString(key), value));
                    return uuidMap;
                },
                // SAVING: convert UUID keys to Strings
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
    // SAVED DATA TYPE — registers this class with Minecraft's save system
    //
    // In Mojang mappings, PersistentState -> SavedData
    //                      PersistentStateType -> SavedDataType
    //                      PersistentStateManager -> DimensionDataStorage
    // -----------------------------------------------------------------------
    public static final SavedDataType<BankState> TYPE = new SavedDataType<>(
        DiamondBank.MOD_ID,   // Save file name: world/data/diamondbank.dat
        BankState::new,       // How to create a fresh state
        CODEC,                // How to save and load
        null                  // DataFixTypes — null is fine for a new mod
    );

    // -----------------------------------------------------------------------
    // PUBLIC API
    // -----------------------------------------------------------------------

    public long getBalance(UUID playerUuid) {
        return balances.getOrDefault(playerUuid, 0L);
    }

    // setDirty() tells Minecraft "data changed, save on next world save."
    public void setBalance(UUID playerUuid, long amount) {
        balances.put(playerUuid, amount);
        setDirty();
    }

    // Returns false if the player can't afford it.
    public boolean withdraw(UUID playerUuid, long amount) {
        long current = getBalance(playerUuid);
        if (current < amount) return false;
        setBalance(playerUuid, current - amount);
        return true;
    }

    public void deposit(UUID playerUuid, long amount) {
        setBalance(playerUuid, getBalance(playerUuid) + amount);
    }

    // -----------------------------------------------------------------------
    // HOW OTHER CLASSES GET ACCESS TO THIS STATE
    // -----------------------------------------------------------------------

    // Any class that needs to read/write balances calls this static method.
    // It asks the overworld's DimensionDataStorage to load or create our save file.
    public static BankState getServerState(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(TYPE);
    }
}

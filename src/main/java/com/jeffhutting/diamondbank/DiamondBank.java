package com.jeffhutting.diamondbank;

import com.jeffhutting.diamondbank.commands.BankCommands;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiamondBank implements ModInitializer {

    // The mod ID must match the "id" field in fabric.mod.json exactly
    public static final String MOD_ID = "diamondbank";

    // Logger prints messages to the server console at startup
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Diamond Bank loaded! Ready to accept deposits.");

        // Register all /bank commands
        BankCommands.register();
    }
}

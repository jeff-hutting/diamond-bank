package com.jeffhutting.diamondbank;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiamondBank implements ModInitializer {

  // The mod ID must match the "id" field in fabric.mod.json exactly
  public static final String MOD_ID = "diamondbank";
  
  // A logger lets us print messages to the server console
  // You'll see these when the server starts up
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

  @Override
  public void onInitialize() {
    LOGGER.info("Diamond Bank loaded! Ready to accept deposits.");
  }
}
package net.caramel.ayd;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.caramel.ayd.worldgen.RateScalePlacementModifier;
import net.caramel.ayd.command.AydCommands;
import net.caramel.ayd.config.OreDensityConfig;
import net.caramel.ayd.config.OreDensityConfigFile;
import net.caramel.ayd.worldgen.WorldgenState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AreYouDense implements ModInitializer {
	public static final String MOD_ID = "are-you-dense";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		Registry.register(Registries.PLACEMENT_MODIFIER_TYPE, id("rate_scale"), RateScalePlacementModifier.TYPE);
		var configFile = new OreDensityConfigFile(FabricLoader.getInstance().getConfigDir().resolve("are-you-dense.toml"));
		try {
			var result = configFile.load();
			result.issues().forEach(issue -> LOGGER.warn("config issue for {}: {}", issue.key(), issue.message()));
			WorldgenState.initialize(result.config());
		} catch (java.io.IOException exception) {
			LOGGER.error("could not read ore density config, using defaults", exception);
			WorldgenState.initialize(OreDensityConfig.empty());
		}
		AydCommands.setConfigFile(configFile);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> AydCommands.register(dispatcher));
	}

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}
}

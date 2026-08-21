package net.caramel.ayd.worldgen;

import net.caramel.ayd.config.OreDensityConfig;
import net.minecraft.util.Identifier;

import java.util.*;

public final class WorldgenState {
    private static OreDensityConfig startupConfig = OreDensityConfig.empty();
    private static OreDensityConfig savedConfig = OreDensityConfig.empty();
    private static OreFamilyCatalog catalog = OreFamilyCatalog.empty();
    private static Map<OreFamily, Double> activeMultipliers = Map.of();

    private WorldgenState() {
    }

    public static synchronized void initialize(OreDensityConfig config) {
        startupConfig = config;
        savedConfig = config;
    }

    public static synchronized OreDensityConfig startupConfig() { return startupConfig; }
    public static synchronized OreDensityConfig savedConfig() { return savedConfig; }
    public static synchronized OreFamilyCatalog catalog() { return catalog; }
    public static synchronized Map<OreFamily, Double> activeMultipliers() { return activeMultipliers; }

    public static synchronized void publishCatalog(OreFamilyCatalog newCatalog) {
        catalog = newCatalog;
        Map<OreFamily, Double> resolved = new HashMap<>();
        for (OreFamily family : newCatalog.families()) {
            Double value = null;
            boolean conflict = false;
            for (Identifier blockId : family.blockIds()) {
                Double candidate = startupConfig.overrides().get(blockId);
                if (candidate == null) continue;
                if (value == null) value = candidate;
                else if (Double.compare(value, candidate) != 0) conflict = true;
            }
            resolved.put(family, conflict || value == null ? 1.0 : value);
        }
        activeMultipliers = Map.copyOf(resolved);
    }

    public static synchronized void publishSavedConfig(OreDensityConfig config) {
        savedConfig = config;
    }

    public static synchronized double activeMultiplier(OreFamily family) {
        return activeMultipliers.getOrDefault(family, 1.0);
    }
}

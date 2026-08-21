package net.caramel.ayd.worldgen;

import net.minecraft.util.Identifier;

import java.util.List;

public record OreFamily(
        List<Identifier> blockIds,
        List<Identifier> configuredFeatureIds,
        List<Identifier> placedFeatureIds
) {
    public OreFamily {
        blockIds = List.copyOf(blockIds);
        configuredFeatureIds = List.copyOf(configuredFeatureIds);
        placedFeatureIds = List.copyOf(placedFeatureIds);
    }
}

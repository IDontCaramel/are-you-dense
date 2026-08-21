package net.caramel.ayd.worldgen;

import net.minecraft.util.Identifier;

import java.util.*;

public final class OreFamilyCatalog {
    private final List<OreFamily> families;
    private final Map<Identifier, OreFamily> byBlock;

    public OreFamilyCatalog(Collection<OreFamily> families) {
        this.families = List.copyOf(families);
        Map<Identifier, OreFamily> index = new HashMap<>();
        for (OreFamily family : this.families) {
            for (Identifier blockId : family.blockIds()) {
                index.put(blockId, family);
            }
        }
        byBlock = Map.copyOf(index);
    }

    public static OreFamilyCatalog empty() {
        return new OreFamilyCatalog(List.of());
    }

    public List<OreFamily> families() {
        return families;
    }

    public Optional<OreFamily> familyFor(Identifier blockId) {
        return Optional.ofNullable(byBlock.get(blockId));
    }

    public boolean containsPlacedFeature(Identifier placedFeatureId) {
        return families.stream().anyMatch(family -> family.placedFeatureIds().contains(placedFeatureId));
    }
}

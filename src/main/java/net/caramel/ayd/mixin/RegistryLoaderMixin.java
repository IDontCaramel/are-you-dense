package net.caramel.ayd.mixin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.caramel.ayd.AreYouDense;
import net.caramel.ayd.worldgen.*;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryLoader;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.io.IOException;
import java.util.*;

@Mixin(RegistryLoader.class)
public abstract class RegistryLoaderMixin {
    @ModifyVariable(method = "load", at = @At("HEAD"), argsOnly = true)
    private static ResourceManager areYouDense$installOverlay(ResourceManager manager) {
        if (manager instanceof OreOverlayResourceManager) return manager;

        try {
            Map<Identifier, JsonObject> configured = readJson(manager, "worldgen/configured_feature");
            Map<Identifier, JsonObject> placed = readJson(manager, "worldgen/placed_feature");
            OreFamilyDiscovery.Result discovered = new OreFamilyDiscovery().discover(
                    configured,
                    placed,
                    Registries.BLOCK::containsId
            );
            WorldgenState.publishCatalog(discovered.catalog());
            Map<Identifier, Double> active = new HashMap<>();
            discovered.catalog().families().forEach(family -> {
                double multiplier = WorldgenState.activeMultiplier(family);
                if (multiplier != 1.0) family.blockIds().forEach(block -> active.put(block, multiplier));
            });
            OreOverlayBuilder.Result overlay = new OreOverlayBuilder().build(placed, discovered.catalog(), active);
            AreYouDense.LOGGER.info("discovered {} ore families and generated {} placed feature overrides", discovered.catalog().families().size(), overlay.resources().size());
            return new OreOverlayResourceManager(manager, overlay.resources());
        } catch (RuntimeException | IOException exception) {
            AreYouDense.LOGGER.error("ore density worldgen overlay failed, using vanilla resources", exception);
            WorldgenState.publishCatalog(OreFamilyCatalog.empty());
            return manager;
        }
    }

    private static Map<Identifier, JsonObject> readJson(ResourceManager manager, String prefix) throws IOException {
        Map<Identifier, JsonObject> result = new HashMap<>();
        for (var entry : manager.findResources(prefix, id -> id.getPath().endsWith(".json")).entrySet()) {
            Identifier id = WorldgenResourceIds.registryId(entry.getKey(), prefix);
            Resource resource = entry.getValue();
            try (var reader = resource.getReader()) {
                result.put(id, JsonParser.parseReader(reader).getAsJsonObject());
            }
        }
        return result;
    }
}

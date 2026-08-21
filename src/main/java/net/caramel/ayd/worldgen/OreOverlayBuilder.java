package net.caramel.ayd.worldgen;

import com.google.gson.*;
import net.minecraft.util.Identifier;

import java.util.*;

public final class OreOverlayBuilder {
    public record Result(Map<Identifier, String> resources, int skipped) {}

    public Result build(Map<Identifier, JsonObject> placedFeatures,
                        OreFamilyCatalog catalog,
                        Map<Identifier, Double> activeMultipliers) {
        Map<Identifier, String> generated = new TreeMap<>(Comparator.comparing(Identifier::toString));
        int skipped = 0;
        for (OreFamily family : catalog.families()) {
            double multiplier = family.blockIds().stream().map(activeMultipliers::get).filter(Objects::nonNull).findFirst().orElse(1.0);
            if (Double.compare(multiplier, 1.0) == 0) continue;
            for (Identifier placedId : family.placedFeatureIds()) {
                JsonObject source = placedFeatures.get(placedId);
                if (source == null) { skipped++; continue; }
                try {
                    JsonObject copy = source.deepCopy();
                    JsonArray placement = copy.has("placement") && copy.get("placement").isJsonArray()
                            ? copy.getAsJsonArray("placement") : null;
                    if (placement == null) { skipped++; continue; }
                    JsonArray replacement = new JsonArray();
                    JsonObject modifier = new JsonObject();
                    modifier.addProperty("type", "are-you-dense:rate_scale");
                    modifier.addProperty("multiplier", multiplier);
                    replacement.add(modifier);
                    placement.forEach(replacement::add);
                    copy.add("placement", replacement);
                    Identifier resourceId = WorldgenResourceIds.resourceId(placedId, "worldgen/placed_feature");
                    generated.put(resourceId, new Gson().toJson(copy));
                } catch (RuntimeException ignored) {
                    skipped++;
                }
            }
        }
        return new Result(Map.copyOf(generated), skipped);
    }
}

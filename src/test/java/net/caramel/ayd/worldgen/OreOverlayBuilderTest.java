package net.caramel.ayd.worldgen;

import com.google.gson.JsonParser;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OreOverlayBuilderTest {
    @Test
    void writesOverrideAtThePlacedFeatureResourcePath() {
        Identifier block = new Identifier("minecraft", "diamond_ore");
        Identifier configured = new Identifier("minecraft", "ore_diamond_small");
        Identifier placed = new Identifier("minecraft", "ore_diamond");
        OreFamily family = new OreFamily(List.of(block), List.of(configured), List.of(placed));
        var source = JsonParser.parseString("{\"feature\":\"minecraft:ore_diamond_small\",\"placement\":[{\"type\":\"minecraft:count\",\"count\":7}]}").getAsJsonObject();

        var result = new OreOverlayBuilder().build(
                Map.of(placed, source),
                new OreFamilyCatalog(List.of(family)),
                Map.of(block, 0.1)
        );

        Identifier resource = new Identifier("minecraft", "worldgen/placed_feature/ore_diamond.json");
        assertEquals(1, result.resources().size());
        var generated = JsonParser.parseString(result.resources().get(resource)).getAsJsonObject();
        assertNotNull(generated);
        assertEquals("are-you-dense:rate_scale", generated.getAsJsonArray("placement").get(0).getAsJsonObject().get("type").getAsString());
        assertEquals(0.1, generated.getAsJsonArray("placement").get(0).getAsJsonObject().get("multiplier").getAsDouble());
    }

    @Test
    void convertsDataPackPathsToRegistryIds() {
        Identifier resource = new Identifier("example", "worldgen/configured_feature/nested/ore.json");
        Identifier registry = new Identifier("example", "nested/ore");

        assertEquals(registry, WorldgenResourceIds.registryId(resource, "worldgen/configured_feature"));
        assertEquals(resource, WorldgenResourceIds.resourceId(registry, "worldgen/configured_feature"));
    }
}

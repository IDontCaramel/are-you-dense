package net.caramel.ayd.worldgen;

import com.google.gson.JsonParser;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OreFamilyDiscoveryTest {
    @Test
    void joinsConfiguredFeaturesThatShareAnOutputBlock() {
        Identifier first = new Identifier("minecraft", "first");
        Identifier second = new Identifier("example", "second");
        var configured = Map.of(
                first, json("minecraft:ore", "minecraft:diamond_ore"),
                second, json("minecraft:scattered_ore", "minecraft:deepslate_diamond_ore", "minecraft:diamond_ore")
        );
        var placed = Map.of(new Identifier("minecraft", "diamond"), JsonParser.parseString("{\"feature\":\"minecraft:first\",\"placement\":[]}").getAsJsonObject());

        var result = new OreFamilyDiscovery().discover(configured, placed, id -> id.getNamespace().equals("minecraft"));

        assertEquals(1, result.catalog().families().size());
        assertEquals(2, result.catalog().families().get(0).blockIds().size());
        assertTrue(result.catalog().familyFor(new Identifier("minecraft", "diamond_ore")).isPresent());
    }

    private static com.google.gson.JsonObject json(String type, String... blocks) {
        StringBuilder targets = new StringBuilder("[");
        for (int i = 0; i < blocks.length; i++) {
            if (i > 0) targets.append(',');
            targets.append("{\"state\":{\"Name\":\"").append(blocks[i]).append("\"}}");
        }
        targets.append(']');
        return JsonParser.parseString("{\"type\":\"" + type + "\",\"config\":{\"targets\":" + targets + "}}").getAsJsonObject();
    }
}

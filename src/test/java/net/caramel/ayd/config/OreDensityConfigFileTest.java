package net.caramel.ayd.config;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OreDensityConfigFileTest {
    @Test
    void parsesAndWritesSparseDecimalValues() throws Exception {
        Path directory = Files.createTempDirectory("ayd-config");
        Path file = directory.resolve("are-you-dense.toml");
        Files.writeString(file, "[ores]\n\"minecraft:zinc_ore\" = 0.5\n\"minecraft:stone\" = 1\n");

        OreDensityConfigFile configFile = new OreDensityConfigFile(file);
        var parsed = configFile.load();

        assertEquals(Map.of(new Identifier("minecraft", "zinc_ore"), 0.5), parsed.config().overrides());
        assertTrue(parsed.issues().isEmpty());

        configFile.save(parsed.config());
        assertEquals("[ores]\n\"minecraft:zinc_ore\" = 0.5\n", Files.readString(file));
    }

    @Test
    void rejectsValuesOutsideTheSupportedRange() throws Exception {
        Path file = Files.createTempFile("ayd-config", ".toml");
        Files.writeString(file, "[ores]\n\"minecraft:a\" = -1\n\"minecraft:b\" = 11\n\"minecraft:c\" = \"fast\"\n");

        var result = new OreDensityConfigFile(file).load();

        assertTrue(result.config().overrides().isEmpty());
        assertEquals(3, result.issues().size());
    }
}

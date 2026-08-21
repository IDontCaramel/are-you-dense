package net.caramel.ayd.config;

import net.minecraft.util.Identifier;
import org.tomlj.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class OreDensityConfigFile {
    public record ParseResult(OreDensityConfig config, List<ConfigIssue> issues) {}
    @FunctionalInterface interface Mover { void move(Path source, Path target) throws IOException; }
    private final Path path;
    private final Mover mover;
    public OreDensityConfigFile(Path path) { this(path, (a, b) -> Files.move(a, b, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)); }
    OreDensityConfigFile(Path path, Mover mover) { this.path = path; this.mover = mover; }
    public ParseResult load() throws IOException {
        if (!Files.exists(path)) return new ParseResult(OreDensityConfig.empty(), List.of());
        TomlParseResult table = Toml.parse(Files.readString(path, StandardCharsets.UTF_8));
        var values = new HashMap<Identifier, Double>(); var issues = new ArrayList<ConfigIssue>();
        TomlTable ores = table.getTable("ores");
        if (ores == null) return new ParseResult(OreDensityConfig.empty(), List.of());
        for (var entry : ores.toMap().entrySet()) {
            String key = entry.getKey();
            Object raw = entry.getValue();
            String shown = String.valueOf(raw);
            Identifier id;
            try { id = Identifier.tryParse(key); } catch (RuntimeException e) { id = null; }
            if (id == null) { issues.add(new ConfigIssue(key, shown, ConfigIssue.Kind.MALFORMED_IDENTIFIER, "Malformed block identifier")); continue; }
            if (!(raw instanceof Number number)) { issues.add(new ConfigIssue(key, shown, ConfigIssue.Kind.NOT_A_NUMBER, "Multiplier must be a number")); continue; }
            double value = number.doubleValue();
            if (!Double.isFinite(value)) { issues.add(new ConfigIssue(key, shown, ConfigIssue.Kind.NON_FINITE, "Multiplier must be finite")); continue; }
            if (value < OreDensityConfig.MIN) { issues.add(new ConfigIssue(key, shown, ConfigIssue.Kind.BELOW_MINIMUM, "Multiplier cannot be below 0")); continue; }
            if (value > OreDensityConfig.MAX) { issues.add(new ConfigIssue(key, shown, ConfigIssue.Kind.ABOVE_MAXIMUM, "Multiplier cannot exceed 10")); continue; }
            if (value != 1.0) values.put(id, value);
        }
        return new ParseResult(new OreDensityConfig(values), List.copyOf(issues));
    }
    public void save(OreDensityConfig config) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        StringBuilder out = new StringBuilder("[ores]\n");
        config.overrides().forEach((id, value) -> out.append(tomlKey(id.toString())).append(" = ").append(Double.toString(value)).append('\n'));
        Path stagedConfig = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(stagedConfig, out.toString(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        try { mover.move(stagedConfig, path); } catch (IOException e) { try { Files.deleteIfExists(stagedConfig); } catch (IOException ignored) {} throw e; }
    }
    private static String tomlKey(String key) { return key.matches("[A-Za-z0-9_-]+") ? key : '"' + key.replace("\\", "\\\\").replace("\"", "\\\"") + '"'; }
}

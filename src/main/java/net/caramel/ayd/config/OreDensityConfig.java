package net.caramel.ayd.config;

import net.minecraft.util.Identifier;
import java.util.*;
import java.util.function.Predicate;

public final class OreDensityConfig {
    public static final double MIN = 0.0, MAX = 10.0;
    private final SortedMap<Identifier, Double> overrides;

    public OreDensityConfig(Map<Identifier, Double> values) {
        TreeMap<Identifier, Double> copy = new TreeMap<>(Comparator.comparing(Identifier::toString));
        values.forEach((id, value) -> { if (value != null && Double.isFinite(value) && value >= MIN && value <= MAX && value != 1.0) copy.put(id, value); });
        overrides = Collections.unmodifiableSortedMap(copy);
    }
    public static OreDensityConfig empty() { return new OreDensityConfig(Map.of()); }
    public SortedMap<Identifier, Double> overrides() { return overrides; }
    public OreDensityConfig withFamily(Collection<Identifier> ids, double value) { var m = new TreeMap<>(overrides); ids.forEach(id -> { if (value == 1.0) m.remove(id); else m.put(id, value); }); return new OreDensityConfig(m); }
    public OreDensityConfig withoutFamily(Collection<Identifier> ids) { var m = new TreeMap<>(overrides); ids.forEach(m::remove); return new OreDensityConfig(m); }
    public OreDensityConfig clear() { return empty(); }
    public OreDensityConfig filtered(Predicate<Identifier> keep) { var m = new TreeMap<Identifier, Double>(); overrides.forEach((id, v) -> { if (keep.test(id)) m.put(id, v); }); return new OreDensityConfig(m); }
}

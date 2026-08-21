package net.caramel.ayd.worldgen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.function.Predicate;

public final class OreFamilyDiscovery {
    public enum Rejection { UNSUPPORTED_FEATURE, INVALID_CONFIGURED_FEATURE, INVALID_TARGET, UNKNOWN_BLOCK, INLINE_PLACED_FEATURE, UNKNOWN_FEATURE }
    public record RejectionReason(Identifier id, Rejection reason, String detail) {}
    public record Result(OreFamilyCatalog catalog, List<RejectionReason> rejections) {}

    public Result discover(Map<Identifier, JsonObject> configuredFeatures,
                           Map<Identifier, JsonObject> placedFeatures,
                           Predicate<Identifier> registeredBlock) {
        Map<Identifier, Set<Identifier>> outputs = new HashMap<>();
        List<RejectionReason> rejections = new ArrayList<>();

        for (var entry : configuredFeatures.entrySet()) {
            Identifier id = entry.getKey();
            JsonObject json = entry.getValue();
            String type = string(json, "type");
            if (!"minecraft:ore".equals(type) && !"minecraft:scattered_ore".equals(type)) {
                rejections.add(new RejectionReason(id, Rejection.UNSUPPORTED_FEATURE, "unsupported feature type"));
                continue;
            }
            JsonArray targets = json.has("config") && json.get("config").isJsonObject()
                    ? json.getAsJsonObject("config").getAsJsonArray("targets") : null;
            if (targets == null) {
                rejections.add(new RejectionReason(id, Rejection.INVALID_CONFIGURED_FEATURE, "missing targets"));
                continue;
            }
            Set<Identifier> blocks = new TreeSet<>(Comparator.comparing(Identifier::toString));
            boolean valid = true;
            for (JsonElement target : targets) {
                if (!target.isJsonObject()) { valid = false; break; }
                JsonObject state = target.getAsJsonObject().getAsJsonObject("state");
                String blockName = state == null ? null : string(state, "Name");
                Identifier blockId = blockName == null ? null : Identifier.tryParse(blockName);
                if (blockId == null) {
                    rejections.add(new RejectionReason(id, Rejection.INVALID_TARGET, "target state needs a literal Name"));
                    valid = false;
                    break;
                }
                if (!registeredBlock.test(blockId)) {
                    rejections.add(new RejectionReason(id, Rejection.UNKNOWN_BLOCK, blockId.toString()));
                    valid = false;
                    break;
                }
                blocks.add(blockId);
            }
            if (valid && !blocks.isEmpty()) outputs.put(id, blocks);
        }

        UnionFind union = new UnionFind(outputs.keySet());
        Map<Identifier, Identifier> firstByBlock = new HashMap<>();
        for (var entry : outputs.entrySet()) {
            for (Identifier block : entry.getValue()) {
                Identifier first = firstByBlock.putIfAbsent(block, entry.getKey());
                if (first != null) union.join(first, entry.getKey());
            }
        }

        Map<Identifier, Set<Identifier>> configuredByComponent = new HashMap<>();
        for (Identifier id : outputs.keySet()) configuredByComponent.computeIfAbsent(union.root(id), ignored -> new TreeSet<>(Comparator.comparing(Identifier::toString))).add(id);
        Map<Identifier, Set<Identifier>> placedByComponent = new HashMap<>();
        for (var entry : placedFeatures.entrySet()) {
            String reference = string(entry.getValue(), "feature");
            Identifier configuredId = reference == null ? null : Identifier.tryParse(reference);
            if (configuredId == null) {
                rejections.add(new RejectionReason(entry.getKey(), Rejection.INLINE_PLACED_FEATURE, "placed feature is not a direct reference"));
                continue;
            }
            if (!outputs.containsKey(configuredId)) {
                rejections.add(new RejectionReason(entry.getKey(), Rejection.UNKNOWN_FEATURE, configuredId.toString()));
                continue;
            }
            placedByComponent.computeIfAbsent(union.root(configuredId), ignored -> new TreeSet<>(Comparator.comparing(Identifier::toString))).add(entry.getKey());
        }

        List<OreFamily> families = new ArrayList<>();
        for (var entry : configuredByComponent.entrySet()) {
            Set<Identifier> blocks = new TreeSet<>(Comparator.comparing(Identifier::toString));
            entry.getValue().forEach(id -> blocks.addAll(outputs.get(id)));
            families.add(new OreFamily(List.copyOf(blocks), List.copyOf(entry.getValue()), List.copyOf(placedByComponent.getOrDefault(entry.getKey(), Set.of()))));
        }
        families.sort(Comparator.comparing(family -> family.blockIds().get(0).toString()));
        return new Result(new OreFamilyCatalog(families), List.copyOf(rejections));
    }

    private static String string(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() && object.get(key).getAsJsonPrimitive().isString()
                ? object.get(key).getAsString() : null;
    }

    private static final class UnionFind {
        private final Map<Identifier, Identifier> parents = new HashMap<>();
        UnionFind(Collection<Identifier> ids) { ids.forEach(id -> parents.put(id, id)); }
        Identifier root(Identifier id) { Identifier parent = parents.get(id); if (!parent.equals(id)) { parent = root(parent); parents.put(id, parent); } return parent; }
        void join(Identifier left, Identifier right) { Identifier a = root(left), b = root(right); if (!a.equals(b)) parents.put(b, a); }
    }
}

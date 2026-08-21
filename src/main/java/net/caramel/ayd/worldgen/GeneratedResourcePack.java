package net.caramel.ayd.worldgen;

import net.minecraft.resource.*;
import net.minecraft.resource.metadata.ResourceMetadataReader;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class GeneratedResourcePack implements ResourcePack {
    private final Map<Identifier, byte[]> resources;

    public GeneratedResourcePack(Map<Identifier, String> resources) {
        Map<Identifier, byte[]> bytes = new HashMap<>();
        resources.forEach((id, json) -> bytes.put(id, json.getBytes(StandardCharsets.UTF_8)));
        this.resources = Map.copyOf(bytes);
    }

    @Override
    public InputSupplier<InputStream> openRoot(String... path) {
        return null;
    }

    @Override
    public InputSupplier<InputStream> open(ResourceType type, Identifier id) {
        if (type != ResourceType.SERVER_DATA) return null;
        byte[] bytes = resources.get(id);
        return bytes == null ? null : () -> new ByteArrayInputStream(bytes);
    }

    @Override
    public void findResources(ResourceType type, String namespace, String prefix, ResultConsumer consumer) {
        if (type != ResourceType.SERVER_DATA) return;
        resources.forEach((id, bytes) -> {
            if (id.getNamespace().equals(namespace) && id.getPath().startsWith(prefix)) {
                consumer.accept(id, () -> new ByteArrayInputStream(bytes));
            }
        });
    }

    @Override
    public Set<String> getNamespaces(ResourceType type) {
        if (type != ResourceType.SERVER_DATA) return Set.of();
        Set<String> namespaces = new HashSet<>();
        resources.keySet().forEach(id -> namespaces.add(id.getNamespace()));
        return Set.copyOf(namespaces);
    }

    @Override
    public <T> T parseMetadata(ResourceMetadataReader<T> reader) {
        return null;
    }

    @Override
    public String getName() {
        return "Are You Dense generated worldgen";
    }

    @Override
    public void close() {
    }
}

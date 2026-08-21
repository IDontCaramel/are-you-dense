package net.caramel.ayd.worldgen;

import net.minecraft.resource.*;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class OreOverlayResourceManager implements ResourceManager {
    private final ResourceManager delegate;
    private final GeneratedResourcePack generatedPack;
    private final Map<Identifier, Resource> generatedResources;

    public OreOverlayResourceManager(ResourceManager delegate, Map<Identifier, String> generated) {
        this.delegate = delegate;
        this.generatedPack = new GeneratedResourcePack(generated);
        Map<Identifier, Resource> resources = new HashMap<>();
        generated.forEach((id, ignored) -> resources.put(id, new Resource(generatedPack, generatedPack.open(ResourceType.SERVER_DATA, id))));
        generatedResources = Map.copyOf(resources);
    }

    public ResourceManager delegate() {
        return delegate;
    }

    @Override
    public Set<String> getAllNamespaces() {
        Set<String> namespaces = new HashSet<>(delegate.getAllNamespaces());
        namespaces.addAll(generatedPack.getNamespaces(ResourceType.SERVER_DATA));
        return Set.copyOf(namespaces);
    }

    @Override
    public Optional<Resource> getResource(Identifier id) {
        return Optional.ofNullable(generatedResources.get(id)).or(() -> delegate.getResource(id));
    }

    @Override
    public List<Resource> getAllResources(Identifier id) {
        List<Resource> resources = new ArrayList<>(delegate.getAllResources(id));
        Resource generated = generatedResources.get(id);
        if (generated != null) resources.add(generated);
        return List.copyOf(resources);
    }

    @Override
    public Map<Identifier, Resource> findResources(String prefix, Predicate<Identifier> predicate) {
        Map<Identifier, Resource> resources = new HashMap<>(delegate.findResources(prefix, predicate));
        generatedResources.forEach((id, resource) -> { if (id.getPath().startsWith(prefix) && predicate.test(id)) resources.put(id, resource); });
        return Map.copyOf(resources);
    }

    @Override
    public Map<Identifier, List<Resource>> findAllResources(String prefix, Predicate<Identifier> predicate) {
        Map<Identifier, List<Resource>> resources = new HashMap<>();
        delegate.findAllResources(prefix, predicate).forEach((id, values) -> resources.put(id, new ArrayList<>(values)));
        generatedResources.forEach((id, resource) -> {
            if (id.getPath().startsWith(prefix) && predicate.test(id)) resources.computeIfAbsent(id, ignored -> new ArrayList<>()).add(resource);
        });
        resources.replaceAll((id, values) -> List.copyOf(values));
        return Map.copyOf(resources);
    }

    @Override
    public Stream<ResourcePack> streamResourcePacks() {
        return Stream.concat(delegate.streamResourcePacks(), Stream.of(generatedPack));
    }
}

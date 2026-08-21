package net.caramel.ayd.worldgen;

import net.minecraft.util.Identifier;

/** Converts between data-pack resource paths and dynamic-registry identifiers. */
public final class WorldgenResourceIds {
    private WorldgenResourceIds() {
    }

    public static Identifier registryId(Identifier resourceId, String directory) {
        String prefix = directory + "/";
        String path = resourceId.getPath();
        if (!path.startsWith(prefix) || !path.endsWith(".json")) {
            throw new IllegalArgumentException("resource is not JSON in " + directory + ": " + resourceId);
        }
        return new Identifier(resourceId.getNamespace(), path.substring(prefix.length(), path.length() - 5));
    }

    public static Identifier resourceId(Identifier registryId, String directory) {
        return new Identifier(registryId.getNamespace(), directory + "/" + registryId.getPath() + ".json");
    }
}

package net.caramel.ayd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.caramel.ayd.config.OreDensityConfig;
import net.caramel.ayd.config.OreDensityConfigFile;
import net.caramel.ayd.worldgen.OreFamily;
import net.caramel.ayd.worldgen.WorldgenState;

import java.io.IOException;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class AydCommands {
    private static OreDensityConfigFile configFile;

    private AydCommands() {
    }

    public static void setConfigFile(OreDensityConfigFile file) {
        configFile = file;
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("ayd")
                .requires(source -> source.hasPermissionLevel(2))
                .then(literal("set").then(argument("block", IdentifierArgumentType.identifier())
                        .suggests(AydCommands::suggestOreBlocks)
                        .then(argument("multiplier", StringArgumentType.word()).executes(context -> set(
                                context.getSource(),
                                IdentifierArgumentType.getIdentifier(context, "block"),
                                StringArgumentType.getString(context, "multiplier")
                        )))))
                .then(literal("reset")
                        .then(literal("all").executes(context -> resetAll(context.getSource())))
                        .then(argument("block", IdentifierArgumentType.identifier())
                                .suggests(AydCommands::suggestOreBlocks)
                                .executes(context -> reset(context.getSource(), IdentifierArgumentType.getIdentifier(context, "block")))))
                .then(literal("list").executes(context -> list(context.getSource()))));
    }

    private static int set(ServerCommandSource source, Identifier blockId, String rawMultiplier) {
        OreFamily family = familyFor(source, blockId);
        if (family == null) return 0;

        final double multiplier;
        try {
            multiplier = Double.parseDouble(rawMultiplier);
        } catch (NumberFormatException exception) {
            source.sendError(Text.literal("multiplier must be a finite number from 0 to 10"));
            return 0;
        }
        if (!Double.isFinite(multiplier) || multiplier < 0.0 || multiplier > 10.0) {
            source.sendError(Text.literal("multiplier must be a finite number from 0 to 10"));
            return 0;
        }

        OreDensityConfig replacement = WorldgenState.savedConfig().withFamily(family.blockIds(), multiplier);
        if (!save(source, replacement)) return 0;
        source.sendFeedback(() -> Text.literal("saved " + family.blockIds() + "\nConfiguration saved. Restart the server to apply world-generation changes."), false);
        return 1;
    }

    private static int reset(ServerCommandSource source, Identifier blockId) {
        OreFamily family = familyFor(source, blockId);
        if (family == null) return 0;
        OreDensityConfig replacement = WorldgenState.savedConfig().withoutFamily(family.blockIds());
        if (!save(source, replacement)) return 0;
        source.sendFeedback(() -> Text.literal("reset " + family.blockIds() + "\nConfiguration saved. Restart the server to apply world-generation changes."), false);
        return 1;
    }

    private static int resetAll(ServerCommandSource source) {
        if (!save(source, OreDensityConfig.empty())) return 0;
        source.sendFeedback(() -> Text.literal("reset all ore overrides\nConfiguration saved. Restart the server to apply world-generation changes."), false);
        return 1;
    }

    private static int list(ServerCommandSource source) {
        boolean found = false;
        for (OreFamily family : WorldgenState.catalog().families()) {
            double active = WorldgenState.activeMultiplier(family);
            double pending = family.blockIds().stream()
                    .map(WorldgenState.savedConfig().overrides()::get)
                    .filter(value -> value != null)
                    .findFirst()
                    .orElse(1.0);
            if (active != 1.0 || pending != 1.0) {
                found = true;
                String value = Double.compare(active, pending) == 0
                        ? Double.toString(active)
                        : Double.toString(active) + " -> " + Double.toString(pending);
                for (Identifier blockId : family.blockIds()) {
                    source.sendFeedback(() -> Text.literal(blockId.getPath() + " = " + value), false);
                }
            }
        }
        if (!found) source.sendFeedback(() -> Text.literal("no ore density overrides are configured"), false);
        return 1;
    }

    /**
     * Offers every member of every discovered ore family. Selecting any member
     * changes the whole family, matching the behavior of {@link #familyFor}.
     */
    private static CompletableFuture<Suggestions> suggestOreBlocks(
            CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        return CommandSource.suggestIdentifiers(
                WorldgenState.catalog().families().stream()
                        .flatMap(family -> family.blockIds().stream())
                        .distinct()
                        .sorted(Comparator.comparing(Identifier::toString)),
                builder
        );
    }

    private static OreFamily familyFor(ServerCommandSource source, Identifier blockId) {
        if (!Registries.BLOCK.containsId(blockId)) {
            source.sendError(Text.literal("unregistered block: " + blockId));
            return null;
        }
        var family = WorldgenState.catalog().familyFor(blockId);
        if (family.isEmpty()) source.sendError(Text.literal("block is not generated by a supported ore feature: " + blockId));
        return family.orElse(null);
    }

    private static boolean save(ServerCommandSource source, OreDensityConfig replacement) {
        if (configFile == null) {
            source.sendError(Text.literal("ore density config is not available"));
            return false;
        }
        try {
            configFile.save(replacement);
            WorldgenState.publishSavedConfig(replacement);
            return true;
        } catch (IOException exception) {
            source.sendError(Text.literal("could not save ore density config: " + exception.getMessage()));
            return false;
        }
    }
}

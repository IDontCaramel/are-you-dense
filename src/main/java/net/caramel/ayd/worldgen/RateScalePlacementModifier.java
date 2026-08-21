package net.caramel.ayd.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.feature.FeaturePlacementContext;
import net.minecraft.world.gen.placementmodifier.PlacementModifier;
import net.minecraft.world.gen.placementmodifier.PlacementModifierType;
import java.util.stream.Stream;

public final class RateScalePlacementModifier extends PlacementModifier {
    public static final Codec<RateScalePlacementModifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(0.0, 10.0).fieldOf("multiplier").forGetter(RateScalePlacementModifier::multiplier)
    ).apply(instance, RateScalePlacementModifier::new));
    public static final PlacementModifierType<RateScalePlacementModifier> TYPE = new PlacementModifierType<>() {
        @Override
        public Codec<RateScalePlacementModifier> codec() {
            return CODEC;
        }
    };
    private final double multiplier;

    public RateScalePlacementModifier(double multiplier) {
        this.multiplier = multiplier;
    }

    public double multiplier() {
        return multiplier;
    }

    @Override public Stream<BlockPos> getPositions(FeaturePlacementContext context, Random random, BlockPos pos) {
        int whole = (int) Math.floor(multiplier); double fraction = multiplier - whole;
        if (whole == 0 && fraction == 0) return Stream.empty();
        Stream<BlockPos> result = Stream.generate(() -> pos).limit(whole);
        if (fraction > 0.0 && random.nextFloat() < fraction) result = Stream.concat(result, Stream.of(pos));
        return result;
    }
    @Override public PlacementModifierType<?> getType() { return TYPE; }
}

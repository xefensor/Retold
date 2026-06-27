package cz.xefensor.retold.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public record RetoldCentralEndIslandMaskDensityFunction(
        DensityFunction argument,
        int radius,
        double outsideValue
) implements DensityFunction {
    public static final MapCodec<RetoldCentralEndIslandMaskDensityFunction> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DensityFunction.CODEC
                            .fieldOf("argument")
                            .forGetter(RetoldCentralEndIslandMaskDensityFunction::argument),

                    Codec.INT
                            .optionalFieldOf("radius", 512)
                            .forGetter(RetoldCentralEndIslandMaskDensityFunction::radius),

                    Codec.DOUBLE
                            .optionalFieldOf("outside_value", -1000.0)
                            .forGetter(RetoldCentralEndIslandMaskDensityFunction::outsideValue)
            ).apply(instance, RetoldCentralEndIslandMaskDensityFunction::new));

    public static final KeyDispatchDataCodec<RetoldCentralEndIslandMaskDensityFunction> CODEC_HOLDER =
            KeyDispatchDataCodec.of(CODEC);

    @Override
    public double compute(FunctionContext context) {
        int x = context.blockX();
        int z = context.blockZ();

        long distanceSquared = (long) x * x + (long) z * z;
        long radiusSquared = (long) radius * radius;

        if (distanceSquared > radiusSquared) {
            return outsideValue;
        }

        return argument.compute(context);
    }

    @Override
    public void fillArray(double[] densities, ContextProvider contextProvider) {
        for (int i = 0; i < densities.length; i++) {
            densities[i] = compute(contextProvider.forIndex(i));
        }
    }

    @Override
    public DensityFunction mapChildren(Visitor visitor) {
        return new RetoldCentralEndIslandMaskDensityFunction(
                argument.mapAll(visitor),
                radius,
                outsideValue
        );
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(mapChildren(visitor));
    }

    @Override
    public double minValue() {
        return Math.min(outsideValue, argument.minValue());
    }

    @Override
    public double maxValue() {
        return Math.max(outsideValue, argument.maxValue());
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC_HOLDER;
    }
}
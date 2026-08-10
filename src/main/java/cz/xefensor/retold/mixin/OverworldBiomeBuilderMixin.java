package cz.xefensor.retold.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(OverworldBiomeBuilder.class)
public abstract class OverworldBiomeBuilderMixin {
    @Inject(
            method = "addBottomBiome",
            at = @At("HEAD"),
            cancellable = true
    )
    private void retold$excludeDeepDarkFromOverworldPreset(
            Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes,
            Climate.Parameter temperature,
            Climate.Parameter humidity,
            Climate.Parameter continentalness,
            Climate.Parameter erosion,
            Climate.Parameter weirdness,
            float offset,
            ResourceKey<Biome> biome,
            CallbackInfo ci
    ) {
        if (Biomes.DEEP_DARK.equals(biome)) {
            ci.cancel();
        }
    }
}

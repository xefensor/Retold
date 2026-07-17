package cz.xefensor.retold.behavior.profiles;

import cz.xefensor.retold.Retold;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

public final class RetoldMobProfileReloadListener
        extends SimpleJsonResourceReloadListener<RetoldMobProfileDefinition> {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(Retold.MODID, "mob_profiles");

    public RetoldMobProfileReloadListener() {
        super(
                RetoldMobProfileDefinition.CODEC,
                FileToIdConverter.json("mob_profiles")
        );
    }

    @Override
    protected void apply(
            Map<Identifier, RetoldMobProfileDefinition> loadedDefinitions,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        try {
            int loadedCount = RetoldMobProfiles.replace(loadedDefinitions);
            Retold.LOGGER.info("Loaded {} mob profiles", loadedCount);
        } catch (IllegalArgumentException exception) {
            Retold.LOGGER.error(
                    "Rejected mob profile reload; keeping the previous valid snapshot: {}",
                    exception.getMessage()
            );
        }
    }
}

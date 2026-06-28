package cz.xefensor.retold.villager;

import cz.xefensor.retold.Retold;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class RetoldVillagerTeachingReloadListener
        extends SimpleJsonResourceReloadListener<RetoldVillagerTeachingEntry> {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(Retold.MODID, "villager_teaching");

    private static Map<Identifier, RetoldVillagerTeachingEntry> entriesByProfession = Map.of();

    public RetoldVillagerTeachingReloadListener() {
        super(
                RetoldVillagerTeachingEntry.CODEC,
                FileToIdConverter.json("villager_teaching")
        );
    }

    @Override
    protected void apply(
            Map<Identifier, RetoldVillagerTeachingEntry> loadedEntries,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        Map<Identifier, RetoldVillagerTeachingEntry> byProfession = new HashMap<>();

        for (RetoldVillagerTeachingEntry entry : loadedEntries.values()) {
            byProfession.put(entry.profession(), entry);
        }

        entriesByProfession = Map.copyOf(byProfession);

        Retold.LOGGER.info("Loaded {} villager teaching entries", entriesByProfession.size());
    }

    public static Optional<RetoldVillagerTeachingEntry> get(Identifier professionId) {
        return Optional.ofNullable(entriesByProfession.get(professionId));
    }
}
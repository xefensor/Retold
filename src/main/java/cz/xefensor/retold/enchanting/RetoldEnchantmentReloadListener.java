package cz.xefensor.retold.enchanting;

import cz.xefensor.retold.Retold;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

public final class RetoldEnchantmentReloadListener
        extends SimpleJsonResourceReloadListener<RetoldEnchantmentSpellDefinition> {
    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(Retold.MODID, "enchantment_spells");

    public RetoldEnchantmentReloadListener() {
        super(
                RetoldEnchantmentSpellCodecs.DEFINITION,
                FileToIdConverter.json("enchantment_spells")
        );
    }

    @Override
    protected void apply(
            Map<Identifier, RetoldEnchantmentSpellDefinition> loadedDefinitions,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        try {
            int loadedCount = RetoldEnchantmentCatalog.replace(loadedDefinitions);
            Retold.LOGGER.info("Loaded {} enchantment spell definitions", loadedCount);
        } catch (IllegalArgumentException exception) {
            Retold.LOGGER.error(
                    "Rejected enchantment spell reload; keeping the previous valid snapshot: {}",
                    exception.getMessage()
            );
        }
    }
}

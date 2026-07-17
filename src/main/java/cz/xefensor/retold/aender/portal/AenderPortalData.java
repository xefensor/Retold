package cz.xefensor.retold.aender.portal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cz.xefensor.retold.Retold;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class AenderPortalData extends SavedData {
    public static final SavedDataType<AenderPortalData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Retold.MODID, "aender_portals"),
            AenderPortalData::new,
            PortalEntry.CODEC.listOf().xmap(AenderPortalData::new, AenderPortalData::encode)
    );

    private final Set<PortalEntry> portals = new HashSet<>();

    public AenderPortalData() {
    }

    private AenderPortalData(List<PortalEntry> portals) {
        this.portals.addAll(portals);
    }

    public static AenderPortalData get(ServerLevel level) {
        return level.getServer().getDataStorage().computeIfAbsent(TYPE);
    }

    public void register(ServerLevel level, AenderPortalShape shape) {
        if (portals.add(PortalEntry.from(level, shape))) {
            setDirty();
        }
    }

    public void remove(ServerLevel level, AenderPortalShape shape) {
        if (portals.remove(PortalEntry.from(level, shape))) {
            setDirty();
        }
    }

    public List<AenderPortalShape> findNear(ServerLevel level, BlockPos target, int radius) {
        Identifier dimension = level.dimension().identifier();
        double maxDistanceSqr = (double) radius * radius;
        List<AenderPortalShape> result = new ArrayList<>();

        for (PortalEntry entry : portals) {
            if (!entry.dimension().equals(dimension)) {
                continue;
            }

            AenderPortalShape shape = entry.toShape();

            if (horizontalDistanceSqr(shape.centerBlock(), target) <= maxDistanceSqr) {
                result.add(shape);
            }
        }

        result.sort(Comparator.comparingDouble(shape -> horizontalDistanceSqr(shape.centerBlock(), target)));
        return result;
    }

    private List<PortalEntry> encode() {
        return List.copyOf(portals);
    }

    private static double horizontalDistanceSqr(BlockPos first, BlockPos second) {
        double dx = first.getX() - second.getX();
        double dz = first.getZ() - second.getZ();
        return dx * dx + dz * dz;
    }

    private record PortalEntry(Identifier dimension, BlockPos minCorner, int width, int depth) {
        private static final Codec<PortalEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Identifier.CODEC.fieldOf("dimension").forGetter(PortalEntry::dimension),
                BlockPos.CODEC.fieldOf("min_corner").forGetter(PortalEntry::minCorner),
                Codec.INT.fieldOf("width").forGetter(PortalEntry::width),
                Codec.INT.fieldOf("depth").forGetter(PortalEntry::depth)
        ).apply(instance, PortalEntry::new));

        private static PortalEntry from(ServerLevel level, AenderPortalShape shape) {
            return new PortalEntry(
                    level.dimension().identifier(),
                    shape.minCorner(),
                    shape.width(),
                    shape.depth()
            );
        }

        private AenderPortalShape toShape() {
            return new AenderPortalShape(minCorner, width, depth);
        }
    }
}

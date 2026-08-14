package cz.xefensor.retold.api.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/** Immutable description of a Retold-owned world mutation before it occurs. */
public record RetoldWorldMutationContext(
        ServerLevel level,
        BlockPos pos,
        RetoldWorldMutationBounds bounds,
        RetoldWorldMutationType type,
        @Nullable Entity actor,
        @Nullable Identifier subjectId
) {
    public RetoldWorldMutationContext {
        Objects.requireNonNull(level, "level");
        pos = Objects.requireNonNull(pos, "pos").immutable();
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(type, "type");

        if (!bounds.contains(pos)) {
            throw new IllegalArgumentException(
                    "Representative mutation position must be inside its bounds"
            );
        }
    }
}

package cz.xefensor.retold.api.world;

import cz.xefensor.retold.Retold;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Stable, default-allow permission layer for Retold-owned world mutations.
 *
 * <p>Integrations register one rule under their own identifier. A mutation is
 * allowed only when every installed rule allows it. No registered rules means
 * the exact standalone Retold behavior is preserved.</p>
 */
public final class RetoldWorldProtection {
    private static final Object RULE_LOCK = new Object();
    private static final Map<Identifier, RetoldWorldProtectionRule> RULES =
            new LinkedHashMap<>();
    private static volatile List<RegisteredRule> ruleSnapshot = List.of();

    private RetoldWorldProtection() {
    }

    public static Registration register(
            Identifier integrationId,
            RetoldWorldProtectionRule rule
    ) {
        Objects.requireNonNull(integrationId, "integrationId");
        Objects.requireNonNull(rule, "rule");

        synchronized (RULE_LOCK) {
            if (RULES.putIfAbsent(integrationId, rule) != null) {
                throw new IllegalStateException(
                        "A Retold world-protection rule is already registered as "
                                + integrationId
                );
            }

            refreshSnapshot();
        }

        return new RuleRegistration(integrationId, rule);
    }

    public static boolean canModify(RetoldWorldMutationContext context) {
        Objects.requireNonNull(context, "context");

        for (RegisteredRule registered : ruleSnapshot) {
            try {
                if (!registered.rule().allows(context)) {
                    return false;
                }
            } catch (RuntimeException exception) {
                Retold.LOGGER.error(
                        "World-protection rule {} failed while checking {} at {} in {}. Denying the mutation.",
                        registered.id(),
                        context.type(),
                        context.pos(),
                        context.level().dimension().identifier(),
                        exception
                );
                return false;
            }
        }

        return true;
    }

    public static boolean canMobBreak(
            ServerLevel level,
            BlockPos pos,
            Entity actor
    ) {
        return canModify(new RetoldWorldMutationContext(
                level,
                pos,
                RetoldWorldMutationBounds.single(pos),
                RetoldWorldMutationType.MOB_BREAK,
                actor,
                null
        ));
    }

    public static boolean canMobPlace(
            ServerLevel level,
            BlockPos pos,
            Entity actor
    ) {
        return canModify(new RetoldWorldMutationContext(
                level,
                pos,
                RetoldWorldMutationBounds.single(pos),
                RetoldWorldMutationType.MOB_PLACE,
                actor,
                null
        ));
    }

    public static boolean canEntityBreak(
            ServerLevel level,
            BlockPos pos,
            Entity actor
    ) {
        return canModify(new RetoldWorldMutationContext(
                level,
                pos,
                RetoldWorldMutationBounds.single(pos),
                RetoldWorldMutationType.ENTITY_BREAK,
                actor,
                null
        ));
    }

    public static boolean canCreatePortal(ServerLevel level, BlockPos pos) {
        return canCreatePortal(level, RetoldWorldMutationBounds.single(pos));
    }

    public static boolean canCreatePortal(
            ServerLevel level,
            RetoldWorldMutationBounds bounds
    ) {
        BlockPos pos = center(bounds);
        return canModify(new RetoldWorldMutationContext(
                level,
                pos,
                bounds,
                RetoldWorldMutationType.PORTAL_CREATE,
                null,
                Identifier.fromNamespaceAndPath(Retold.MODID, "aender_portal")
        ));
    }

    public static boolean canRetrogenStructure(
            ServerLevel level,
            ChunkPos chunkPos,
            Identifier structureId
    ) {
        RetoldWorldMutationBounds bounds = chunkBounds(level, chunkPos);
        return canModify(new RetoldWorldMutationContext(
                level,
                center(bounds),
                bounds,
                RetoldWorldMutationType.STRUCTURE_RETROGEN,
                null,
                structureId
        ));
    }

    public static boolean canRegenerateAenderChunk(
            ServerLevel level,
            ChunkPos chunkPos
    ) {
        RetoldWorldMutationBounds bounds = chunkBounds(level, chunkPos);
        return canModify(new RetoldWorldMutationContext(
                level,
                center(bounds),
                bounds,
                RetoldWorldMutationType.AENDER_CHUNK_REGENERATE,
                null,
                null
        ));
    }

    public static boolean canWorldModify(
            ServerLevel level,
            BlockPos pos,
            @Nullable Identifier subjectId
    ) {
        return canModify(new RetoldWorldMutationContext(
                level,
                pos,
                RetoldWorldMutationBounds.single(pos),
                RetoldWorldMutationType.WORLD_MODIFY,
                null,
                subjectId
        ));
    }

    private static RetoldWorldMutationBounds chunkBounds(
            ServerLevel level,
            ChunkPos chunkPos
    ) {
        return new RetoldWorldMutationBounds(
                chunkPos.getMinBlockX(),
                level.getMinY(),
                chunkPos.getMinBlockZ(),
                chunkPos.getMaxBlockX(),
                level.getMaxY() - 1,
                chunkPos.getMaxBlockZ()
        );
    }

    private static BlockPos center(RetoldWorldMutationBounds bounds) {
        return new BlockPos(
                bounds.minX() + (bounds.maxX() - bounds.minX()) / 2,
                bounds.minY() + (bounds.maxY() - bounds.minY()) / 2,
                bounds.minZ() + (bounds.maxZ() - bounds.minZ()) / 2
        );
    }

    private static void refreshSnapshot() {
        List<RegisteredRule> snapshot = new ArrayList<>();

        for (Map.Entry<Identifier, RetoldWorldProtectionRule> entry : RULES.entrySet()) {
            snapshot.add(new RegisteredRule(entry.getKey(), entry.getValue()));
        }

        ruleSnapshot = List.copyOf(snapshot);
    }

    private static void unregister(
            Identifier integrationId,
            RetoldWorldProtectionRule rule
    ) {
        synchronized (RULE_LOCK) {
            if (RULES.get(integrationId) == rule) {
                RULES.remove(integrationId);
                refreshSnapshot();
            }
        }
    }

    public interface Registration extends AutoCloseable {
        Identifier integrationId();

        @Override
        void close();
    }

    private record RegisteredRule(
            Identifier id,
            RetoldWorldProtectionRule rule
    ) {
    }

    private static final class RuleRegistration implements Registration {
        private final Identifier integrationId;
        private final RetoldWorldProtectionRule rule;
        private final AtomicBoolean closed = new AtomicBoolean();

        private RuleRegistration(
                Identifier integrationId,
                RetoldWorldProtectionRule rule
        ) {
            this.integrationId = integrationId;
            this.rule = rule;
        }

        @Override
        public Identifier integrationId() {
            return integrationId;
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                unregister(integrationId, rule);
            }
        }
    }
}

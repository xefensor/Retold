package cz.xefensor.retold.api.world;

/**
 * A deny-capable rule installed by a claims or world-protection integration.
 * Every registered rule must allow a mutation before Retold performs it.
 */
@FunctionalInterface
public interface RetoldWorldProtectionRule {
    boolean allows(RetoldWorldMutationContext context);
}

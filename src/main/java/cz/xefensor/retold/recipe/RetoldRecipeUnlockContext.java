package cz.xefensor.retold.recipe;

public final class RetoldRecipeUnlockContext {
    private static final ThreadLocal<Integer> INTERNAL_UNLOCK_DEPTH =
            ThreadLocal.withInitial(() -> 0);

    private RetoldRecipeUnlockContext() {
    }

    public static void beginInternalUnlock() {
        INTERNAL_UNLOCK_DEPTH.set(INTERNAL_UNLOCK_DEPTH.get() + 1);
    }

    public static void endInternalUnlock() {
        int depth = INTERNAL_UNLOCK_DEPTH.get() - 1;

        if (depth <= 0) {
            INTERNAL_UNLOCK_DEPTH.remove();
            return;
        }

        INTERNAL_UNLOCK_DEPTH.set(depth);
    }

    public static boolean isInternalUnlock() {
        return INTERNAL_UNLOCK_DEPTH.get() > 0;
    }
}
package cz.xefensor.retold.client.render;

public final class RetoldEndermanParticleColor {
    private static final ThreadLocal<Boolean> GREEN_ENDERMAN_PORTAL =
            ThreadLocal.withInitial(() -> false);

    private RetoldEndermanParticleColor() {
    }

    public static void beginGreenEndermanPortal() {
        GREEN_ENDERMAN_PORTAL.set(true);
    }

    public static void endGreenEndermanPortal() {
        GREEN_ENDERMAN_PORTAL.set(false);
    }

    public static boolean isGreenEndermanPortal() {
        return GREEN_ENDERMAN_PORTAL.get();
    }
}

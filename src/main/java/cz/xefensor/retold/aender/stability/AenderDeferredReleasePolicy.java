package cz.xefensor.retold.aender.stability;

/** Pure watcher/stability gate for released stabilizer chunks. */
final class AenderDeferredReleasePolicy {
    private AenderDeferredReleasePolicy() {
    }

    static boolean shouldRelease(boolean stable, boolean watched, boolean deferred) {
        return deferred && !stable && !watched;
    }
}

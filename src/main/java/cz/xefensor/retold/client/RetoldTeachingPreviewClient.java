package cz.xefensor.retold.client;

public final class RetoldTeachingPreviewClient {
    private RetoldTeachingPreviewClient() {
    }

    private static boolean active = false;
    private static String label = "Learn";
    private static String tooltip = "Place an item into the teaching slot, then click Learn.";
    private static Runnable refreshCallback;

    public static void set(boolean newActive, String newLabel, String newTooltip) {
        active = newActive;
        label = newLabel;
        tooltip = newTooltip;

        if (refreshCallback != null) {
            refreshCallback.run();
        }
    }

    public static boolean active() {
        return active;
    }

    public static String label() {
        return label;
    }

    public static String tooltip() {
        return tooltip;
    }

    public static void setRefreshCallback(Runnable newRefreshCallback) {
        refreshCallback = newRefreshCallback;
    }

    public static void clearRefreshCallback() {
        refreshCallback = null;
    }

    public static void reset() {
        active = false;
        label = "Learn";
        tooltip = "Place an item into the teaching slot, then click Learn.";
    }
}
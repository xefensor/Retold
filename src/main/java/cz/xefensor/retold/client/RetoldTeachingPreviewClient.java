package cz.xefensor.retold.client;

public final class RetoldTeachingPreviewClient {
    private RetoldTeachingPreviewClient() {
    }

    private static boolean active = false;
    private static String buttonLabel = "Learn";
    private static String status = "Status: Place item into slot";
    private static String cost = "Cost: -";
    private static String tooltip = "Place an item into the teaching slot.";
    private static Runnable refreshCallback;

    public static void set(
            boolean newActive,
            String newButtonLabel,
            String newStatus,
            String newCost,
            String newTooltip
    ) {
        active = newActive;
        buttonLabel = newButtonLabel;
        status = newStatus;
        cost = newCost;
        tooltip = newTooltip;

        if (refreshCallback != null) {
            refreshCallback.run();
        }
    }

    public static boolean active() {
        return active;
    }

    public static String buttonLabel() {
        return buttonLabel;
    }

    public static String status() {
        return status;
    }

    public static String cost() {
        return cost;
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
        buttonLabel = "Learn";
        status = "Status: Place item into slot";
        cost = "Cost: -";
        tooltip = "Place an item into the teaching slot.";
    }
}
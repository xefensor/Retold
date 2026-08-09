package cz.xefensor.retold.client;

import cz.xefensor.retold.network.RetoldTeachingPreviewPayload;
import net.minecraft.network.chat.Component;

public final class RetoldTeachingPreviewClient {
    private static boolean active = false;
    private static Component buttonLabel = Component.translatable(
            "container.retold.teaching.learn"
    );
    private static Component status = Component.translatable(
            "container.retold.teaching.status.place_item"
    );
    private static Component cost = Component.translatable(
            "container.retold.teaching.cost.none"
    );
    private static Component tooltip = Component.translatable(
            "container.retold.teaching.tooltip.place_item"
    );
    private static RetoldTeachingPreviewPayload.Feedback feedback =
            RetoldTeachingPreviewPayload.Feedback.NONE;
    private static Runnable refreshCallback;
    private RetoldTeachingPreviewClient() {
    }

    public static void set(
            boolean newActive,
            Component newButtonLabel,
            Component newStatus,
            Component newCost,
            Component newTooltip,
            RetoldTeachingPreviewPayload.Feedback newFeedback
    ) {
        active = newActive;
        buttonLabel = newButtonLabel;
        status = newStatus;
        cost = newCost;
        tooltip = newTooltip;
        feedback = newFeedback;

        if (refreshCallback != null) {
            refreshCallback.run();
        }
    }

    public static boolean active() {
        return active;
    }

    public static Component buttonLabel() {
        return buttonLabel;
    }

    public static Component status() {
        return status;
    }

    public static Component cost() {
        return cost;
    }

    public static Component tooltip() {
        return tooltip;
    }

    public static RetoldTeachingPreviewPayload.Feedback takeFeedback() {
        RetoldTeachingPreviewPayload.Feedback current = feedback;
        feedback = RetoldTeachingPreviewPayload.Feedback.NONE;
        return current;
    }

    public static void setRefreshCallback(Runnable newRefreshCallback) {
        refreshCallback = newRefreshCallback;
    }

    public static void clearRefreshCallback() {
        refreshCallback = null;
    }

    public static void reset() {
        active = false;
        buttonLabel = Component.translatable("container.retold.teaching.learn");
        status = Component.translatable("container.retold.teaching.status.place_item");
        cost = Component.translatable("container.retold.teaching.cost.none");
        tooltip = Component.translatable("container.retold.teaching.tooltip.place_item");
        feedback = RetoldTeachingPreviewPayload.Feedback.NONE;
    }
}

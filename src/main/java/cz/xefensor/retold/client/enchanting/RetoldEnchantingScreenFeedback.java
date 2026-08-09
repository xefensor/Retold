package cz.xefensor.retold.client.enchanting;

/** Receives server-authoritative enchanting feedback on the open screen. */
public interface RetoldEnchantingScreenFeedback {
    void retold$castFinished(int containerId, boolean success);
}

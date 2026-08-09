package cz.xefensor.retold.enchanting;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Identifies book enchantments that actually changed the completed anvil output. */
final class RetoldTransferredEnchantments {
    private RetoldTransferredEnchantments() {
    }

    static Set<String> find(
            Map<String, Integer> leftEnchantments,
            Map<String, Integer> bookEnchantments,
            Map<String, Integer> outputEnchantments
    ) {
        Set<String> transferred = new HashSet<>();

        for (Map.Entry<String, Integer> entry : bookEnchantments.entrySet()) {
            String enchantment = entry.getKey();
            int bookLevel = entry.getValue();
            int leftLevel = leftEnchantments.getOrDefault(enchantment, 0);
            int outputLevel = outputEnchantments.getOrDefault(enchantment, 0);

            if (bookLevel > 0 && outputLevel > leftLevel) {
                transferred.add(enchantment);
            }
        }

        return Set.copyOf(transferred);
    }
}

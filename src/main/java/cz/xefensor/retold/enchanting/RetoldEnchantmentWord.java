package cz.xefensor.retold.enchanting;

import java.util.Objects;

/** Semantic spell word; final rendered SGA glyphs are assigned separately. */
public record RetoldEnchantmentWord(
        String domain,
        String effect,
        String modifier
) {
    public RetoldEnchantmentWord {
        Objects.requireNonNull(domain, "domain");
        Objects.requireNonNull(effect, "effect");
        Objects.requireNonNull(modifier, "modifier");

        if (domain.isBlank() || effect.isBlank() || modifier.isBlank()) {
            throw new IllegalArgumentException("Semantic spell concepts must not be blank");
        }
    }
}

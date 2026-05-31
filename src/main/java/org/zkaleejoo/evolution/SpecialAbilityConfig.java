package org.zkaleejoo.evolution;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.bukkit.Material;

public record SpecialAbilityConfig(
        String id,
        AbilityType type,
        boolean enabled,
        double chance,
        int amount,
        double maxMultiplier,
        int maxStacks,
        long stackWindowMs,
        int perStackAmplifier,
        int cooldownSeconds,
        boolean compatibleWithMending,
        Set<Material> materialWhitelist,
        AbilityTrigger trigger,
        double distanceBlocks,
        boolean requireMainHand
) {

    public SpecialAbilityConfig {
        materialWhitelist = materialWhitelist == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new LinkedHashSet<>(materialWhitelist));
        trigger = trigger == null ? AbilityTrigger.BLOCK_BREAK : trigger;
        distanceBlocks = Math.max(1.0D, distanceBlocks);
    }

    public SpecialAbilityConfig(String id, AbilityType type, boolean enabled, double chance, int amount,
            double maxMultiplier, int maxStacks, long stackWindowMs, int perStackAmplifier, int cooldownSeconds,
            boolean compatibleWithMending, Set<Material> materialWhitelist) {
        this(id, type, enabled, chance, amount, maxMultiplier, maxStacks, stackWindowMs, perStackAmplifier,
                cooldownSeconds, compatibleWithMending, materialWhitelist, AbilityTrigger.BLOCK_BREAK, 80.0D, true);
    }

    public boolean hasMaterialWhitelist() {
        return !materialWhitelist.isEmpty();
    }
}

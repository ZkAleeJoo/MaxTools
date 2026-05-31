package org.zkaleejoo.evolution;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class EvolutionSyncPlanner {

    private EvolutionSyncPlanner() {
    }

    public static EvolutionSyncPlan plan(int usage, Set<String> unlockedAbilities,
            List<EvolutionMilestone> milestones, Set<String> availableAbilities) {
        int safeUsage = Math.max(0, usage);
        Set<String> normalizedUnlocked = normalize(unlockedAbilities);
        Set<String> normalizedAvailable = normalize(availableAbilities);
        List<EvolutionMilestone> milestonesToApply = new ArrayList<>();
        Set<String> abilitiesToAdd = new LinkedHashSet<>();
        int lastReachedMilestone = 0;

        for (EvolutionMilestone milestone : milestones == null ? List.<EvolutionMilestone>of() : milestones) {
            if (milestone == null || safeUsage < milestone.blocksRequired()) {
                continue;
            }

            lastReachedMilestone = Math.max(lastReachedMilestone, milestone.blocksRequired());
            boolean hasMissingAbility = false;
            for (String abilityId : milestone.unlockAbilities()) {
                String normalized = normalizeAbility(abilityId);
                if (normalized.isBlank() || !normalizedAvailable.contains(normalized)
                        || normalizedUnlocked.contains(normalized)) {
                    continue;
                }
                abilitiesToAdd.add(normalized);
                hasMissingAbility = true;
            }

            boolean hasEnchantment = milestone.enchantment() != null && !milestone.enchantment().isBlank();
            if (hasEnchantment || hasMissingAbility) {
                milestonesToApply.add(milestone);
            }
        }

        return new EvolutionSyncPlan(List.copyOf(milestonesToApply), Set.copyOf(abilitiesToAdd), lastReachedMilestone);
    }

    private static Set<String> normalize(Set<String> values) {
        Set<String> normalized = new LinkedHashSet<>();
        if (values == null) {
            return normalized;
        }
        for (String value : values) {
            String ability = normalizeAbility(value);
            if (!ability.isBlank()) {
                normalized.add(ability);
            }
        }
        return normalized;
    }

    private static String normalizeAbility(String abilityId) {
        return abilityId == null ? "" : abilityId.trim().toLowerCase(Locale.ROOT);
    }
}

package org.zkaleejoo.evolution;

public record AbilityStatus(
        SpecialAbilityConfig ability,
        boolean unlocked,
        long remainingCooldownSeconds,
        int requiredBlocks) {

    public AbilityState state() {
        if (!unlocked) {
            return AbilityState.BLOCKED;
        }
        return remainingCooldownSeconds > 0 ? AbilityState.COOLDOWN : AbilityState.ACTIVE;
    }

    public enum AbilityState {
        ACTIVE,
        COOLDOWN,
        BLOCKED
    }
}
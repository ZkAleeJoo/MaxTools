package org.zkaleejoo.evolution;

public record EvolutionSyncResult(
        boolean changed,
        int milestonesApplied,
        int abilitiesAdded,
        int lastAppliedMilestone
) {
}

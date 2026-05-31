package org.zkaleejoo.evolution;

import java.util.List;
import java.util.Set;

public record EvolutionSyncPlan(
        List<EvolutionMilestone> milestonesToApply,
        Set<String> abilitiesToAdd,
        int lastAppliedMilestone
) {

    public boolean isChanged() {
        return !milestonesToApply.isEmpty() || lastAppliedMilestone > 0;
    }
}

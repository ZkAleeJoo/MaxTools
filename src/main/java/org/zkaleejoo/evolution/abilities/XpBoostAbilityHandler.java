package org.zkaleejoo.evolution.abilities;

import org.bukkit.inventory.meta.ItemMeta;
import org.zkaleejoo.evolution.SpecialAbilityConfig;
import org.zkaleejoo.evolution.ToolEvolutionManager;

public class XpBoostAbilityHandler implements AbilityHandler {

    @Override
    public boolean canTrigger(ItemMeta meta, SpecialAbilityConfig ability, ToolEvolutionManager evolutionManager) {
        return meta != null && evolutionManager.canProcAbility(meta, ability);
    }

    @Override
    public void onBlockBreak(BlockBreakAbilityContext context, SpecialAbilityConfig ability) {
        if (!context.rollProc(ability)) {
            return;
        }

        if (ability.hasMaterialWhitelist()
                && !ability.materialWhitelist().contains(context.event().getBlock().getType())) {
            return;
        }

        int baseXp = context.event().getExpToDrop();
        if (baseXp <= 0) {
            return;
        }

        int multiplier = Math.max(1, ability.amount());
        context.event().setExpToDrop(baseXp * multiplier);
        context.evolutionManager().applyCooldown(context.meta(), ability);
        context.evolutionManager().incrementAbilityActivation(context.meta(), ability.id());
    }
}

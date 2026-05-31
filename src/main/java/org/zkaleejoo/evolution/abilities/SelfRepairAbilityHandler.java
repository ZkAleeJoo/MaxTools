package org.zkaleejoo.evolution.abilities;

import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.zkaleejoo.evolution.SpecialAbilityConfig;
import org.zkaleejoo.evolution.ToolEvolutionManager;

public class SelfRepairAbilityHandler implements AbilityHandler {

    @Override
    public boolean canTrigger(ItemMeta meta, SpecialAbilityConfig ability, ToolEvolutionManager evolutionManager) {
        return meta != null && evolutionManager.canProcAbility(meta, ability);
    }

    @Override
    public boolean onApply(ItemMeta meta, SpecialAbilityConfig ability, ToolEvolutionManager evolutionManager) {
        if (!(meta instanceof Damageable damageable)) {
            return false;
        }

        int damage = damageable.getDamage();
        if (damage <= 0) {
            return false;
        }

        int repairedDamage = Math.max(0, damage - Math.max(1, ability.amount()));
        damageable.setDamage(repairedDamage);
        evolutionManager.applyCooldown(meta, ability);
        evolutionManager.incrementAbilityActivation(meta, ability.id());
        return true;
    }
}

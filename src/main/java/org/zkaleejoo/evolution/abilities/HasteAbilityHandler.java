package org.zkaleejoo.evolution.abilities;

import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.zkaleejoo.evolution.SpecialAbilityConfig;
import org.zkaleejoo.evolution.ToolEvolutionManager;

public class HasteAbilityHandler implements AbilityHandler {

    @Override
    public boolean canTrigger(ItemMeta meta, SpecialAbilityConfig ability, ToolEvolutionManager evolutionManager) {
        if (meta == null || ability == null || !ability.enabled()) {
            return false;
        }

        if (!ability.compatibleWithMending() && meta.hasEnchant(Enchantment.MENDING)) {
            return false;
        }

        if (isPassiveMode(ability)) {
            return true;
        }

        return evolutionManager.canProcAbility(meta, ability);
    }

    @Override
    public void onTick(TickAbilityContext context, SpecialAbilityConfig ability) {
        int amplifier = Math.max(0, ability.amount() - 1);
        PotionEffect effect = new PotionEffect(PotionEffectType.HASTE, 60, amplifier, true, false, true);
        context.player().addPotionEffect(effect);

        if (!isPassiveMode(ability)) {
            context.evolutionManager().applyCooldown(context.meta(), ability);
            context.evolutionManager().incrementAbilityActivation(context.meta(), ability.id());
        }
    }

    private boolean isPassiveMode(SpecialAbilityConfig ability) {
        return ability.cooldownSeconds() <= 0 && ability.chance() >= 1.0D;
    }
}

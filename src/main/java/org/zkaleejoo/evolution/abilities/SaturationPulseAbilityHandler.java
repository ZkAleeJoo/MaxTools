package org.zkaleejoo.evolution.abilities;

import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.zkaleejoo.evolution.SpecialAbilityConfig;
import org.zkaleejoo.evolution.ToolEvolutionManager;

public class SaturationPulseAbilityHandler implements AbilityHandler {

    private static final int MAX_FOOD_LEVEL = 20;
    private static final int SATURATION_DURATION_TICKS = 40;

    @Override
    public boolean canTrigger(ItemMeta meta, SpecialAbilityConfig ability, ToolEvolutionManager evolutionManager) {
        return meta != null && evolutionManager.canProcAbility(meta, ability);
    }

    @Override
    public void onBlockBreak(BlockBreakAbilityContext context, SpecialAbilityConfig ability) {
        Player player = context.player();
        int currentFoodLevel = player.getFoodLevel();
        if (currentFoodLevel >= MAX_FOOD_LEVEL) {
            return;
        }

        if (!context.rollProc(ability)) {
            return;
        }

        int restoredFood = Math.max(1, ability.amount());
        int updatedFoodLevel = Math.min(MAX_FOOD_LEVEL, currentFoodLevel + restoredFood);
        player.setFoodLevel(updatedFoodLevel);

        if (updatedFoodLevel >= MAX_FOOD_LEVEL) {
            int saturationAmplifier = Math.max(0, Math.min(2, restoredFood - 1));
            PotionEffect saturationEffect = new PotionEffect(
                    PotionEffectType.SATURATION,
                    SATURATION_DURATION_TICKS,
                    saturationAmplifier,
                    true,
                    false,
                    true);
            player.addPotionEffect(saturationEffect);
        }

        context.evolutionManager().applyCooldown(context.meta(), ability);
        context.evolutionManager().incrementAbilityActivation(context.meta(), ability.id());
    }
}

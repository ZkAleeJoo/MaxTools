package org.zkaleejoo.evolution.abilities;

import java.util.List;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.zkaleejoo.evolution.SpecialAbilityConfig;
import org.zkaleejoo.evolution.ToolEvolutionManager;

public class TelepathyAbilityHandler implements AbilityHandler {

    @Override
    public boolean canTrigger(ItemMeta meta, SpecialAbilityConfig ability, ToolEvolutionManager evolutionManager) {
        return meta != null && evolutionManager.canProcAbility(meta, ability);
    }

    @Override
    public void onBlockBreak(BlockBreakAbilityContext context, SpecialAbilityConfig ability) {
        if (!context.rollProc(ability)) {
            return;
        }

        List<ItemStack> drops = context.getDrops();
        if (drops.isEmpty()) {
            return;
        }

        context.event().setDropItems(false);
        context.evolutionManager().applyCooldown(context.meta(), ability);
        context.evolutionManager().incrementAbilityActivation(context.meta(), ability.id());

        for (ItemStack drop : drops) {
            context.player().getInventory().addItem(drop).values()
                    .forEach(leftover -> context.player().getWorld()
                            .dropItemNaturally(context.player().getLocation(), leftover));
        }
        context.markDropsDispatched();
    }
}

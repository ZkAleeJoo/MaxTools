package org.zkaleejoo.evolution.abilities;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.zkaleejoo.evolution.SpecialAbilityConfig;
import org.zkaleejoo.evolution.ToolEvolutionManager;

public record TickAbilityContext(Player player, ItemStack tool, ItemMeta meta, ToolEvolutionManager evolutionManager) {

    public boolean rollProc(SpecialAbilityConfig ability) {
        return evolutionManager.canProcAbility(meta, ability);
    }
}

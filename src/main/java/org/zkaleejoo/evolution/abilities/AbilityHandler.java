package org.zkaleejoo.evolution.abilities;

import java.util.UUID;
import org.bukkit.inventory.meta.ItemMeta;
import org.zkaleejoo.evolution.SpecialAbilityConfig;
import org.zkaleejoo.evolution.ToolEvolutionManager;

public interface AbilityHandler {

    boolean canTrigger(ItemMeta meta, SpecialAbilityConfig ability, ToolEvolutionManager evolutionManager);

    default void onBlockBreak(BlockBreakAbilityContext context, SpecialAbilityConfig ability) {
    }

    default void onTick(TickAbilityContext context, SpecialAbilityConfig ability) {
    }

    default boolean onApply(ItemMeta meta, SpecialAbilityConfig ability, ToolEvolutionManager evolutionManager) {
        return false;
    }

    default void onPlayerDisconnect(UUID playerId) {
    }

    default void onPluginDisable() {
    }
}

package org.zkaleejoo.evolution;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.zkaleejoo.MaxTools;
import org.zkaleejoo.evolution.abilities.TickAbilityContext;

public class AbilityTaskManager {

    private final MaxTools plugin;
    private final ToolEvolutionManager evolutionManager;
    private BukkitTask hasteTask;

    public AbilityTaskManager(MaxTools plugin, ToolEvolutionManager evolutionManager) {
        this.plugin = plugin;
        this.evolutionManager = evolutionManager;
    }

    public void start() {
        if (hasteTask != null) {
            return;
        }
        hasteTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player == null) continue;
                ItemStack tool = player.getInventory().getItem(EquipmentSlot.HAND);
                if (!evolutionManager.isTrackedTool(tool)) {
                    continue;
                }

                ItemMeta meta = tool.getItemMeta();
                if (meta == null) {
                    continue;
                }

                Set<String> unlocked = new LinkedHashSet<>(
                        evolutionManager.getUnlockedAbilities(meta.getPersistentDataContainer()));

                TickAbilityContext context = new TickAbilityContext(player, tool, meta, evolutionManager);
                unlocked.stream()
                        .map(abilityId -> evolutionManager.getSpecialAbilityConfig(abilityId, tool.getType()))
                        .filter(ability -> ability != null && ability.enabled())
                        .sorted(Comparator.comparingInt(config -> config.type().ordinal()))
                        .forEach(ability -> evolutionManager.getAbilityHandlerRegistry().find(ability.type())
                                .ifPresent(handler -> {
                                    if (handler.canTrigger(meta, ability, evolutionManager)) {
                                        handler.onTick(context, ability);
                                    }
                                }));
                tool.setItemMeta(meta);
            }
        }, 20L, 20L);
    }

    public void stop() {
        if (hasteTask == null) {
            return;
        }
        hasteTask.cancel();
        hasteTask = null;
        evolutionManager.getAbilityHandlerRegistry().asMap().values()
                .forEach(handler -> handler.onPluginDisable());
    }
}

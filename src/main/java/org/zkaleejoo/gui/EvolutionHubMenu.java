package org.zkaleejoo.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.zkaleejoo.MaxTools;
import org.zkaleejoo.evolution.EvolutionMilestone;
import org.zkaleejoo.evolution.SpecialAbilityConfig;
import org.zkaleejoo.evolution.ToolEvolutionManager;
import org.zkaleejoo.utils.MessageUtils;
import org.zkaleejoo.utils.MetKeys;

public class EvolutionHubMenu {

    private final MaxTools plugin;
    private final ToolEvolutionManager evolutionManager;

    public EvolutionHubMenu(MaxTools plugin, ToolEvolutionManager evolutionManager) {
        this.plugin = plugin;
        this.evolutionManager = evolutionManager;
    }

    public Inventory build(Player player, ItemStack tool) {
        int size = plugin.getConfigManager().getGuiInventorySize("menu-layouts.evolution-hub.size", 27);
        String title = plugin.getConfigManager().getGuiString("menu-layouts.evolution-hub.title",
                plugin.getConfigManager().getMenuTitle());
        Component titleComponent = MessageUtils.getColoredComponent(title);
        MaxEvolutionMenuHolder holder = new MaxEvolutionMenuHolder(MaxEvolutionMenuHolder.MenuKind.TREE);
        Inventory inventory = Bukkit.createInventory(holder, size, titleComponent);
        holder.bindInventory(inventory);
        int toolInfoSlot = plugin.getConfigManager().getGuiInt("menu-layouts.evolution-hub.tool-info-slot", 13);
        if (toolInfoSlot >= 0 && toolInfoSlot < size) {
            inventory.setItem(toolInfoSlot, createToolInfoItem(tool));
        }
        return inventory;
    }

    private ItemStack createToolInfoItem(ItemStack tool) {
        ItemStack display = tool.clone();
        ItemMeta meta = display.getItemMeta();
        if (meta == null) {
            return display;
        }

        int usage = evolutionManager.getUsage(tool);
        int target = evolutionManager.getCurrentTarget(tool.getType(), usage);

        List<EvolutionMilestone> milestones = evolutionManager.getMilestones(tool.getType());
        int unlockedMilestones = evolutionManager.getReachedMilestones(tool.getType(), usage).size();
        int totalMilestones = milestones.size();
        int currentLevel = unlockedMilestones;
        int nextLevel = totalMilestones == 0 ? currentLevel : Math.min(totalMilestones, currentLevel + 1);

        Map<String, SpecialAbilityConfig> abilities = evolutionManager.getSpecialAbilities(tool.getType());
        Set<String> unlockedAbilityIds = evolutionManager.getUnlockedAbilities(tool);
        int unlockedAbilities = unlockedAbilityIds.stream()
                .filter(abilities::containsKey)
                .collect(Collectors.toSet())
                .size();
        int totalAbilities = (int) abilities.values().stream().filter(SpecialAbilityConfig::enabled).count();

        String displayName = plugin.getConfigManager().getMenuToolItemTitle();
        displayName = replacePlaceholders(displayName, display.getType(), currentLevel, nextLevel, usage, target,
                unlockedMilestones, totalMilestones, unlockedAbilities, totalAbilities);
        meta.displayName(MessageUtils.getColoredComponent(displayName));

        List<Component> lore = new ArrayList<>();
        for (String rawLine : plugin.getConfigManager().getMenuToolItemLore()) {
            String line = replacePlaceholders(rawLine, display.getType(), currentLevel, nextLevel, usage, target,
                    unlockedMilestones, totalMilestones, unlockedAbilities, totalAbilities);
            lore.add(MessageUtils.getColoredComponent(line));
        }
        meta.lore(lore);

        meta.getPersistentDataContainer().set(MetKeys.key(plugin, MetKeys.MENU_NON_MOVABLE), PersistentDataType.BYTE,
                (byte) 1);
        display.setItemMeta(meta);
        return display;
    }

    private String replacePlaceholders(String input, Material toolType, int currentLevel, int nextLevel, int usage,
            int target, int unlockedMilestones, int totalMilestones, int unlockedAbilities, int totalAbilities) {
        if (input == null) {
            return "";
        }
        return input
                .replace("{tool_name}", plugin.getConfigManager().getToolName(toolType))
                .replace("{current_level}", String.valueOf(currentLevel))
                .replace("{next_level}", String.valueOf(nextLevel))
                .replace("{usage}", String.valueOf(usage))
                .replace("{target}", String.valueOf(target))
                .replace("{unlocked_milestones}", String.valueOf(unlockedMilestones))
                .replace("{total_milestones}", String.valueOf(totalMilestones))
                .replace("{unlocked_abilities}", String.valueOf(unlockedAbilities))
                .replace("{total_abilities}", String.valueOf(totalAbilities));
    }
}

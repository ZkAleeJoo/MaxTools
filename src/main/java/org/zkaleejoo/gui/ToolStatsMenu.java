package org.zkaleejoo.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataType;
import org.zkaleejoo.MaxTools;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.evolution.ToolEvolutionManager;
import org.zkaleejoo.evolution.ToolEvolutionManager.ToolDerivedStats;
import org.zkaleejoo.utils.MessageUtils;
import org.zkaleejoo.utils.MetKeys;

public class ToolStatsMenu {

    private static final String BASE_PATH = "menu-layouts.tool-stats";

    private final MaxTools plugin;
    private final ToolEvolutionManager evolutionManager;

    public ToolStatsMenu(MaxTools plugin, ToolEvolutionManager evolutionManager) {
        this.plugin = plugin;
        this.evolutionManager = evolutionManager;
    }

    public Inventory build(Player player, ItemStack tool) {
        MainConfigManager config = plugin.getConfigManager();
        int size = config.getGuiInventorySize(BASE_PATH + ".size", 54);
        String rawTitle = config.getGuiString(BASE_PATH + ".title", "&8Tool Stats");
        MaxEvolutionMenuHolder holder = new MaxEvolutionMenuHolder(MaxEvolutionMenuHolder.MenuKind.STATS);

        ItemMeta meta = tool == null ? null : tool.getItemMeta();
        int usage = evolutionManager.getUsage(tool);
        ToolDerivedStats stats = evolutionManager.getDerivedStats(meta, tool == null ? null : tool.getType(), usage);
        String title = applyStatsPlaceholders(rawTitle, tool == null ? null : tool.getType(), stats, "", 0, 0);
        Inventory inventory = Bukkit.createInventory(holder, size, MessageUtils.getColoredComponent(title));
        holder.bindInventory(inventory);

        setSummaryItem(inventory, config, BASE_PATH + ".materials.summary", tool == null ? null : tool.getType(),
                stats);
        setAbilityItems(inventory, config, BASE_PATH + ".ability-slots", tool == null ? null : tool.getType(), stats);
        setStaticItem(inventory, config, BASE_PATH + ".materials.filler", tool == null ? null : tool.getType(), stats);
        setStaticItem(inventory, config, BASE_PATH + ".materials.back", tool == null ? null : tool.getType(), stats);
        return inventory;
    }

    private void setSummaryItem(Inventory inventory, MainConfigManager config, String path, Material toolType,
            ToolDerivedStats stats) {
        List<Integer> slots = config.getGuiSlots(path + ".slots");
        if (slots.isEmpty()) {
            return;
        }

        String title = config.getGuiString(path + ".title", "&bTool Overview");
        List<String> loreLines = config.getGuiStringList(path + ".lore");
        String state = resolveProgressState(stats.usage(), stats.currentTarget());
        int percent = stats.milestoneCompletionPercent();
        String tier = evolutionManager.getTierNameForUsage(toolType, stats.usage());
        ItemVisual visual = resolveItemVisual(config, path, state, tier, percent, Material.BOOK, 0);

        for (int slot : slots) {
            if (slot < 0 || slot >= inventory.getSize()) {
                continue;
            }
            ItemStack item = new ItemStack(visual.material());
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                continue;
            }

            meta.displayName(MessageUtils.getColoredComponent(applyStatsPlaceholders(title, toolType, stats,
                    "", 0, 0)));
            if (visual.customModelData() > 0) {
                CustomModelDataComponent cmdComponent = meta.getCustomModelDataComponent();
                cmdComponent.setFloats(List.of((float) visual.customModelData()));
                meta.setCustomModelDataComponent(cmdComponent);
            }

            if (!loreLines.isEmpty()) {
                List<Component> lore = new ArrayList<>();
                for (String line : loreLines) {
                    lore.add(MessageUtils.getColoredComponent(applyStatsPlaceholders(line, toolType, stats, "", 0, 0)));
                }
                meta.lore(lore);
            }

            meta.getPersistentDataContainer().set(MetKeys.key(plugin, MetKeys.MENU_NON_MOVABLE),
                    PersistentDataType.BYTE,
                    (byte) 1);
            item.setItemMeta(meta);
            inventory.setItem(slot, item);
        }
    }

    private void setAbilityItems(Inventory inventory, MainConfigManager config, String path, Material toolType,
            ToolDerivedStats stats) {
        List<Integer> slots = config.getGuiSlots(path);
        if (slots.isEmpty()) {
            return;
        }

        String title = config.getGuiString(BASE_PATH + ".materials.ability.title", "&e{ability}");
        List<String> loreLines = config.getGuiStringList(BASE_PATH + ".materials.ability.lore");
        String state = resolveProgressState(stats.usage(), stats.currentTarget());
        String tier = evolutionManager.getTierNameForUsage(toolType, stats.usage());

        int index = 0;
        for (Map.Entry<String, Integer> entry : stats.abilityActivations().entrySet()) {
            if (index >= slots.size()) {
                break;
            }
            int slot = slots.get(index++);
            if (slot < 0 || slot >= inventory.getSize()) {
                continue;
            }

            String abilityName = evolutionManager.getDisplayAbilityName(entry.getKey());
            int activationCount = entry.getValue();
            int abilityPercent = stats.totalAbilityActivations() <= 0
                    ? 0
                    : (int) Math.round((activationCount * 100.0D) / stats.totalAbilityActivations());
            ItemVisual visual = resolveItemVisual(config, BASE_PATH + ".materials.ability", state, tier, abilityPercent,
                    Material.BLAZE_POWDER, 0);
            ItemStack item = new ItemStack(visual.material());
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                continue;
            }

            meta.displayName(MessageUtils.getColoredComponent(applyStatsPlaceholders(title, toolType, stats,
                    abilityName, activationCount, abilityPercent)));
            if (visual.customModelData() > 0) {
                CustomModelDataComponent cmdComponent = meta.getCustomModelDataComponent();
                cmdComponent.setFloats(List.of((float) visual.customModelData()));
                meta.setCustomModelDataComponent(cmdComponent);
            }

            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(MessageUtils.getColoredComponent(applyStatsPlaceholders(line, toolType, stats,
                        abilityName, activationCount, abilityPercent)));
            }
            meta.lore(lore.isEmpty() ? null : lore);
            meta.getPersistentDataContainer().set(MetKeys.key(plugin, MetKeys.MENU_NON_MOVABLE),
                    PersistentDataType.BYTE,
                    (byte) 1);
            item.setItemMeta(meta);
            inventory.setItem(slot, item);
        }
    }

    private void setStaticItem(Inventory inventory, MainConfigManager config, String path, Material toolType,
            ToolDerivedStats stats) {
        List<Integer> slots = config.getGuiSlots(path + ".slots");
        if (slots.isEmpty()) {
            return;
        }

        Material material = config.getGuiMaterial(path + ".material", Material.BLACK_STAINED_GLASS_PANE);
        String title = config.getGuiString(path + ".title", " ");
        List<String> loreLines = config.getGuiStringList(path + ".lore");

        for (int slot : slots) {
            if (slot < 0 || slot >= inventory.getSize()) {
                continue;
            }
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                continue;
            }
            meta.displayName(MessageUtils.getColoredComponent(applyStatsPlaceholders(title, toolType, stats,
                    "", 0, 0)));
            if (!loreLines.isEmpty()) {
                List<Component> lore = new ArrayList<>();
                for (String line : loreLines) {
                    lore.add(MessageUtils.getColoredComponent(applyStatsPlaceholders(line, toolType, stats,
                            "", 0, 0)));
                }
                meta.lore(lore);
            }
            meta.getPersistentDataContainer().set(MetKeys.key(plugin, MetKeys.MENU_NON_MOVABLE),
                    PersistentDataType.BYTE,
                    (byte) 1);
            item.setItemMeta(meta);
            inventory.setItem(slot, item);
        }
    }

    private String applyStatsPlaceholders(String input, Material toolType, ToolDerivedStats stats, String abilityName,
            int abilityActivations, int abilityPercent) {
        String milestoneBar = evolutionManager.buildProgressBar(stats.milestoneCompletionPercent(),
                plugin.getConfigManager().getToolStatsMilestoneBarLength(),
                plugin.getConfigManager().getToolStatsMilestoneFilledChar(),
                plugin.getConfigManager().getToolStatsMilestoneEmptyChar());
        String abilityBar = evolutionManager.buildProgressBar(stats.abilityCompletionPercent(),
                plugin.getConfigManager().getToolStatsAbilityBarLength(),
                plugin.getConfigManager().getToolStatsAbilityFilledChar(),
                plugin.getConfigManager().getToolStatsAbilityEmptyChar());
        String tier = evolutionManager.getTierNameForUsage(toolType, stats.usage());
        int remainingBlocks = Math.max(0, stats.currentTarget() - stats.usage());
        String nextReward = resolveNextReward(toolType, stats.usage());
        return input
                .replace("{usage}", String.valueOf(stats.usage()))
                .replace("{target}", String.valueOf(stats.currentTarget()))
                .replace("{milestones_unlocked}", String.valueOf(stats.unlockedMilestones()))
                .replace("{milestones_total}", String.valueOf(stats.totalMilestones()))
                .replace("{milestones_percent}", String.valueOf(stats.milestoneCompletionPercent()))
                .replace("{milestones_bar}", milestoneBar)
                .replace("{abilities_unlocked}", String.valueOf(stats.unlockedAbilities()))
                .replace("{abilities_total}", String.valueOf(stats.totalAbilities()))
                .replace("{abilities_percent}", String.valueOf(stats.abilityCompletionPercent()))
                .replace("{abilities_bar}", abilityBar)
                .replace("{activations_total}", String.valueOf(stats.totalAbilityActivations()))
                .replace("{ability}", abilityName)
                .replace("{ability_activations}", String.valueOf(abilityActivations))
                .replace("{ability_percent}", String.valueOf(abilityPercent))
                .replace("{next_reward}", nextReward)
                .replace("{remaining_blocks}", String.valueOf(remainingBlocks))
                .replace("{tier}", tier);
    }

    private String resolveProgressState(int usage, int target) {
        if (usage <= 0) {
            return "locked";
        }
        if (target > 0 && usage >= target) {
            return "unlocked";
        }
        return "current";
    }

    private String resolveNextReward(Material toolType, int usage) {
        return evolutionManager.getMilestones(toolType).stream()
                .filter(milestone -> usage < milestone.blocksRequired())
                .findFirst()
                .map(milestone -> {
                    String enchantName = evolutionManager.getDisplayEnchantmentName(milestone.enchantment());
                    return (enchantName == null || enchantName.isBlank()) ? "-" : enchantName + " " + milestone.level();
                })
                .orElse("-");
    }

    private ItemVisual resolveItemVisual(MainConfigManager config, String path, String state, String tier, int percent,
            Material fallbackMaterial, int fallbackCmd) {
        String tierKey = tier == null ? "" : tier.toLowerCase(Locale.ROOT).replace(" ", "_");
        String statePath = path + ".state-variants." + state;
        String tierPath = path + ".rarity-variants." + tierKey;
        String progressPath = path + ".progress-variants";

        Material material = config.getGuiMaterial(statePath + ".material", null);
        int customModelData = config.getGuiInt(statePath + ".custom-model-data", 0);
        if (material == null) {
            material = config.getGuiMaterial(tierPath + ".material", null);
        }
        customModelData = config.getGuiInt(tierPath + ".custom-model-data", customModelData);

        if (config.getMainConfig().getConfigurationSection(progressPath) != null) {
            for (String key : config.getMainConfig().getConfigurationSection(progressPath).getKeys(false)) {
                String variantPath = progressPath + "." + key;
                int min = config.getGuiInt(variantPath + ".min-percent", 0);
                int max = config.getGuiInt(variantPath + ".max-percent", 100);
                if (percent < min || percent > max) {
                    continue;
                }
                Material progressMaterial = config.getGuiMaterial(variantPath + ".material", null);
                if (progressMaterial != null) {
                    material = progressMaterial;
                }
                customModelData = config.getGuiInt(variantPath + ".custom-model-data", customModelData);
                break;
            }
        }

        if (material == null) {
            material = config.getGuiMaterial(path + ".material", fallbackMaterial);
        }
        if (customModelData <= 0) {
            customModelData = config.getGuiInt(path + ".custom-model-data", fallbackCmd);
        }
        return new ItemVisual(material, Math.max(0, customModelData));
    }

    private record ItemVisual(Material material, int customModelData) {
    }
}

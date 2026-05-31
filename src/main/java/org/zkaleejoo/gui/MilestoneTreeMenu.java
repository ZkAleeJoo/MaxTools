package org.zkaleejoo.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
import org.zkaleejoo.MaxTools;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.evolution.EvolutionMilestone;
import org.zkaleejoo.evolution.ToolEvolutionManager;
import org.zkaleejoo.utils.MessageUtils;
import org.zkaleejoo.utils.MetKeys;

public class MilestoneTreeMenu {

    private static final String BASE_PATH = "menu-layouts.milestone-tree";

    private final MaxTools plugin;
    private final ToolEvolutionManager evolutionManager;

    public MilestoneTreeMenu(MaxTools plugin, ToolEvolutionManager evolutionManager) {
        this.plugin = plugin;
        this.evolutionManager = evolutionManager;
    }

    public Inventory build(Player player, MenuSession session) {
        MainConfigManager config = plugin.getConfigManager();
        String title = MessageUtils
                .getColoredMessage(config.getGuiString(BASE_PATH + ".title", "&8Milestones ({page}/{pages})"));
        List<Integer> milestoneSlots = config.getGuiSlots(BASE_PATH + ".slots");
        int size = config.getGuiInventorySize(BASE_PATH + ".size", 54);

        List<EvolutionMilestone> milestones = evolutionManager.getMilestones(session.getToolType()).stream()
                .sorted(Comparator.comparingInt(EvolutionMilestone::blocksRequired))
                .toList();

        int totalPages = Math.max(1, (int) Math.ceil(milestones.size() / (double) Math.max(1, milestoneSlots.size())));
        int page = Math.min(Math.max(0, session.getPage()), totalPages - 1);
        session.setPage(page);
        String tier = evolutionManager.getTierNameForUsage(session.getToolType(), session.getUsage());
        String nextReward = resolveNextReward(session.getToolType(), session.getUsage());
        int remainingBlocks = Math.max(0,
                evolutionManager.getCurrentTarget(session.getToolType(), session.getUsage()) - session.getUsage());

        Component titleComponent = MessageUtils.getColoredComponent(title
                .replace("{page}", String.valueOf(page + 1))
                .replace("{pages}", String.valueOf(totalPages))
                .replace("{next_reward}", nextReward)
                .replace("{remaining_blocks}", String.valueOf(remainingBlocks))
                .replace("{tier}", tier));
        MaxEvolutionMenuHolder holder = new MaxEvolutionMenuHolder(MaxEvolutionMenuHolder.MenuKind.TREE);
        Inventory inventory = Bukkit.createInventory(holder, size, titleComponent);
        holder.bindInventory(inventory);

        Map<Integer, Integer> slotMapping = new HashMap<>();
        int startIndex = page * Math.max(1, milestoneSlots.size());
        for (int i = 0; i < milestoneSlots.size(); i++) {
            int milestoneIndex = startIndex + i;
            if (milestoneIndex >= milestones.size()) {
                break;
            }
            EvolutionMilestone milestone = milestones.get(milestoneIndex);
            int slot = milestoneSlots.get(i);
            inventory.setItem(slot,
                    buildMilestoneItem(config, milestone, session.getToolType(), session.getUsage(), tier));
            slotMapping.put(slot, milestone.blocksRequired());
        }
        session.setSlotMilestoneIds(slotMapping);

        placeStaticItems(inventory, config, page, totalPages);
        return inventory;
    }

    private ItemStack buildMilestoneItem(MainConfigManager config, EvolutionMilestone milestone,
            Material sessionToolType, int usage, String currentTier) {
        String state = usage >= milestone.blocksRequired() ? "unlocked"
                : (milestone.blocksRequired() == evolutionManager.getCurrentTarget(sessionToolType, usage) ? "current"
                        : "locked");
        int progressPercent = resolveProgressPercent(usage, milestone.blocksRequired());
        String milestoneTier = evolutionManager.getTierNameForUsage(sessionToolType, milestone.blocksRequired());

        String itemPath = BASE_PATH + ".items." + state;
        ItemVisual visual = resolveItemVisual(config, itemPath, state, milestoneTier, progressPercent, Material.PAPER,
                0);
        ItemStack item = new ItemStack(visual.material());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        String enchantName = evolutionManager.getDisplayEnchantmentName(milestone.enchantment());
        String title = config.getGuiString(itemPath + ".title", "&eMilestone &7- &f{blocks}");
        meta.displayName(MessageUtils.getColoredComponent(
                applyPlaceholders(title, sessionToolType, milestone, enchantName, state, usage, currentTier)));

        int cmd = visual.customModelData();
        if (cmd > 0) {
            CustomModelDataComponent cmdComponent = meta.getCustomModelDataComponent();
            cmdComponent.setFloats(List.of((float) cmd));
            meta.setCustomModelDataComponent(cmdComponent);
        }

        List<String> loreLines = config.getGuiStringList(itemPath + ".lore");
        if (loreLines.isEmpty()) {
            loreLines = List.of("&7Blocks: &f{blocks}", "&7Reward: &f{reward}", "&eClick to view details");
        }
        List<Component> lore = new ArrayList<>();
        for (String loreLine : loreLines) {
            lore.add(MessageUtils.getColoredComponent(
                    applyPlaceholders(loreLine, sessionToolType, milestone, enchantName, state, usage, currentTier)));
        }
        meta.lore(lore);
        meta.getPersistentDataContainer().set(MetKeys.key(plugin, MetKeys.MENU_NON_MOVABLE),
                org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private void placeStaticItems(Inventory inventory, MainConfigManager config, int page, int totalPages) {
        setStaticItem(inventory, config, BASE_PATH + ".materials.filler", -1, page, totalPages);
        setStaticItem(inventory, config, BASE_PATH + ".materials.previous-page", page > 0 ? page - 1 : -1, page,
                totalPages);
        setStaticItem(inventory, config, BASE_PATH + ".materials.next-page", page + 1 < totalPages ? page + 1 : -1,
                page, totalPages);
        setAbilitiesMenuItem(inventory, config, BASE_PATH + ".materials.abilities-menu", page, totalPages);
        setStatsMenuItem(inventory, config, BASE_PATH + ".materials.stats-menu", page, totalPages);
    }

    private void setAbilitiesMenuItem(Inventory inventory, MainConfigManager config, String path, int page,
            int totalPages) {
        List<Integer> slots = config.getGuiSlots(path + ".slots");
        if (slots.isEmpty()) {
            return;
        }
        setStaticItem(inventory, config, path, -1, page, totalPages);
        for (int slot : slots) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getItemMeta() == null) {
                continue;
            }
            ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(MetKeys.key(plugin, MetKeys.MENU_OPEN_ABILITIES),
                    org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
            inventory.setItem(slot, item);
        }
    }

    private void setStatsMenuItem(Inventory inventory, MainConfigManager config, String path, int page,
            int totalPages) {
        List<Integer> slots = config.getGuiSlots(path + ".slots");
        if (slots.isEmpty()) {
            return;
        }
        setStaticItem(inventory, config, path, -1, page, totalPages);
        for (int slot : slots) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getItemMeta() == null) {
                continue;
            }
            ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(MetKeys.key(plugin, MetKeys.MENU_OPEN_STATS),
                    org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
            inventory.setItem(slot, item);
        }
    }

    private void setStaticItem(Inventory inventory, MainConfigManager config, String path, int targetPage, int page,
            int totalPages) {
        List<Integer> slots = config.getGuiSlots(path + ".slots");
        if (slots.isEmpty()) {
            return;
        }

        Material material = config.getGuiMaterial(path + ".material", Material.BLACK_STAINED_GLASS_PANE);
        String title = config.getGuiString(path + ".title", " ");
        List<String> loreLines = config.getGuiStringList(path + ".lore");
        int cmd = config.getGuiInt(path + ".custom-model-data", 0);

        for (int slot : slots) {
            if (slot < 0 || slot >= inventory.getSize()) {
                continue;
            }
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                continue;
            }
            meta.displayName(MessageUtils.getColoredComponent(title
                    .replace("{page}", String.valueOf(page + 1))
                    .replace("{pages}", String.valueOf(totalPages))));
            if (cmd > 0) {
                CustomModelDataComponent cmdComponent = meta.getCustomModelDataComponent();
                cmdComponent.setFloats(List.of((float) cmd));
                meta.setCustomModelDataComponent(cmdComponent);
            }
            if (!loreLines.isEmpty()) {
                List<Component> lore = new ArrayList<>();
                for (String line : loreLines) {
                    lore.add(MessageUtils.getColoredComponent(line
                            .replace("{page}", String.valueOf(page + 1))
                            .replace("{pages}", String.valueOf(totalPages))));
                }
                meta.lore(lore);
            }
            meta.getPersistentDataContainer().set(MetKeys.key(plugin, MetKeys.MENU_NON_MOVABLE),
                    org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
            if (targetPage >= 0) {
                meta.getPersistentDataContainer().set(MetKeys.key(plugin, MetKeys.MENU_TARGET_PAGE),
                        org.bukkit.persistence.PersistentDataType.INTEGER,
                        targetPage);
            }
            item.setItemMeta(meta);
            inventory.setItem(slot, item);
        }
    }

    private String applyPlaceholders(String input, Material toolType, EvolutionMilestone milestone, String enchantName,
            String state,
            int usage, String tier) {
        String reward = enchantName == null || enchantName.isBlank()
                ? "-"
                : enchantName + " " + milestone.level();
        int remainingBlocks = Math.max(0, milestone.blocksRequired() - usage);
        String nextReward = resolveNextReward(toolType, usage);
        return input
                .replace("{blocks}", String.valueOf(milestone.blocksRequired()))
                .replace("{reward}", reward)
                .replace("{state}", state)
                .replace("{usage}", String.valueOf(usage))
                .replace("{next_reward}", nextReward)
                .replace("{remaining_blocks}", String.valueOf(remainingBlocks))
                .replace("{tier}", tier);
    }

    private String resolveNextReward(Material toolType, int usage) {
        EvolutionMilestone targetMilestone = evolutionManager.getMilestones(toolType).stream()
                .filter(milestone -> usage < milestone.blocksRequired())
                .min(Comparator.comparingInt(EvolutionMilestone::blocksRequired))
                .orElse(null);
        if (targetMilestone == null) {
            return "-";
        }
        String enchantName = evolutionManager.getDisplayEnchantmentName(targetMilestone.enchantment());
        if (enchantName == null || enchantName.isBlank()) {
            return "-";
        }
        return enchantName + " " + targetMilestone.level();
    }

    private int resolveProgressPercent(int usage, int target) {
        if (target <= 0) {
            return 100;
        }
        return Math.max(0, Math.min(100, (int) Math.round((usage * 100.0D) / target)));
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

        List<String> progressKeys = config.getMainConfig().getConfigurationSection(progressPath) == null
                ? List.of()
                : new ArrayList<>(config.getMainConfig().getConfigurationSection(progressPath).getKeys(false));
        for (String key : progressKeys) {
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

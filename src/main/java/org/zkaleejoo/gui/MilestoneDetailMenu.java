package org.zkaleejoo.gui;

import java.util.ArrayList;
import java.util.List;
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
import org.zkaleejoo.evolution.EvolutionMilestone;
import org.zkaleejoo.evolution.ToolEvolutionManager;
import org.zkaleejoo.utils.MessageUtils;
import org.zkaleejoo.utils.MetKeys;

public class MilestoneDetailMenu {

    private static final String BASE_PATH = "menu-layouts.milestone-detail";
    private final MaxTools plugin;
    private final ToolEvolutionManager evolutionManager;

    public MilestoneDetailMenu(MaxTools plugin, ToolEvolutionManager evolutionManager) {
        this.plugin = plugin;
        this.evolutionManager = evolutionManager;
    }

    public Inventory build(Player player, MenuSession session, EvolutionMilestone milestone) {
        MainConfigManager config = plugin.getConfigManager();
        int size = config.getGuiInventorySize(BASE_PATH + ".size", 27);
        String enchantName = evolutionManager.getDisplayEnchantmentName(milestone.enchantment());
        String title = config.getGuiString(BASE_PATH + ".title", "&8Milestone {blocks}");
        Component titleComponent = MessageUtils.getColoredComponent(applyPlaceholders(title, milestone, enchantName));
        MaxEvolutionMenuHolder holder = new MaxEvolutionMenuHolder(MaxEvolutionMenuHolder.MenuKind.DETAIL);
        Inventory inventory = Bukkit.createInventory(holder, size, titleComponent);
        holder.bindInventory(inventory);

        int infoSlot = config.getGuiInt(BASE_PATH + ".items.info.slot", 13);
        if (infoSlot >= 0 && infoSlot < size) {
            inventory.setItem(infoSlot, buildInfoItem(config, milestone, enchantName));
        }

        setStaticItem(inventory, config, BASE_PATH + ".materials.back");
        setStaticItem(inventory, config, BASE_PATH + ".materials.filler");
        return inventory;
    }

    private ItemStack buildInfoItem(MainConfigManager config, EvolutionMilestone milestone, String enchantName) {
        String path = BASE_PATH + ".items.info";
        Material material = config.getGuiMaterial(path + ".material", Material.BOOK);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        String title = config.getGuiString(path + ".title", "&eMilestone &f{blocks}");
        meta.displayName(MessageUtils.getColoredComponent(applyPlaceholders(title, milestone, enchantName)));

        int cmd = config.getGuiInt(path + ".custom-model-data", 0);
        if (cmd > 0) {
            CustomModelDataComponent component = meta.getCustomModelDataComponent();
            component.getFloats().add((float) cmd);
            meta.setCustomModelDataComponent(component);
        }

        List<String> loreLines = config.getGuiStringList(path + ".lore");
        if (loreLines.isEmpty()) {
            loreLines = List.of("&7Required blocks: &f{blocks}", "&7Reward: &f{reward}", "&7Abilities: &f{abilities}");
        }
        List<Component> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(MessageUtils.getColoredComponent(applyPlaceholders(line, milestone, enchantName)));
        }
        meta.lore(lore);
        meta.getPersistentDataContainer().set(MetKeys.key(plugin, MetKeys.MENU_NON_MOVABLE), PersistentDataType.BYTE,
                (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private void setStaticItem(Inventory inventory, MainConfigManager config, String path) {
        List<Integer> slots = config.getGuiSlots(path + ".slots");
        if (slots.isEmpty()) {
            return;
        }
        Material material = config.getGuiMaterial(path + ".material", Material.BLACK_STAINED_GLASS_PANE);
        String title = config.getGuiString(path + ".title", " ");
        List<String> lore = config.getGuiStringList(path + ".lore");

        for (Integer slot : slots) {
            if (slot == null || slot < 0 || slot >= inventory.getSize()) {
                continue;
            }
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                continue;
            }
            meta.displayName(MessageUtils.getColoredComponent(title));
            if (!lore.isEmpty()) {
                List<Component> components = new ArrayList<>();
                for (String line : lore) {
                    components.add(MessageUtils.getColoredComponent(line));
                }
                meta.lore(components);
            }
            meta.getPersistentDataContainer().set(MetKeys.key(plugin, MetKeys.MENU_NON_MOVABLE),
                    PersistentDataType.BYTE,
                    (byte) 1);
            item.setItemMeta(meta);
            inventory.setItem(slot, item);
        }
    }

    private String applyPlaceholders(String input, EvolutionMilestone milestone, String enchantName) {
        String reward = (enchantName == null || enchantName.isBlank()) ? "-" : enchantName + " " + milestone.level();
        String abilities = milestone.unlockAbilities().isEmpty() ? "-" : String.join(", ", milestone.unlockAbilities());
        return input
                .replace("{blocks}", String.valueOf(milestone.blocksRequired()))
                .replace("{reward}", reward)
                .replace("{abilities}", abilities)
                .replace("{enchantment}", enchantName == null ? "-" : enchantName)
                .replace("{level}", String.valueOf(milestone.level()));
    }
}

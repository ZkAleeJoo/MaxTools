package org.zkaleejoo.gui;

import java.util.ArrayList;
import java.util.List;
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
import org.zkaleejoo.evolution.AbilityStatus;
import org.zkaleejoo.evolution.ToolEvolutionManager;
import org.zkaleejoo.utils.MessageUtils;
import org.zkaleejoo.utils.MetKeys;

public class AbilitiesMenu {
    private static final String BASE_PATH = "menu-layouts.abilities";

    private final MaxTools plugin;
    private final ToolEvolutionManager evolutionManager;

    public AbilitiesMenu(MaxTools plugin, ToolEvolutionManager evolutionManager) {
        this.plugin = plugin;
        this.evolutionManager = evolutionManager;
    }

    public Inventory build(Player player, ItemStack tool) {
        MainConfigManager config = plugin.getConfigManager();
        int size = config.getGuiInventorySize(BASE_PATH + ".size", 54);
        String title = config.getGuiString(BASE_PATH + ".title", "&8Abilities");
        MaxEvolutionMenuHolder holder = new MaxEvolutionMenuHolder(MaxEvolutionMenuHolder.MenuKind.ABILITIES);
        Inventory inventory = Bukkit.createInventory(holder, size, MessageUtils.getColoredComponent(title));
        holder.bindInventory(inventory);

        List<Integer> abilitySlots = config.getGuiSlots(BASE_PATH + ".slots");
        List<AbilityStatus> statuses = tool == null || tool.getItemMeta() == null
                ? List.of()
                : evolutionManager.getAbilityStatuses(tool.getItemMeta(), tool.getType(),
                        config.getGuiBoolean(BASE_PATH + ".show-locked", true));

        for (int i = 0; i < Math.min(abilitySlots.size(), statuses.size()); i++) {
            inventory.setItem(abilitySlots.get(i), buildAbilityItem(config, statuses.get(i)));
        }

        setStaticItem(inventory, config, BASE_PATH + ".materials.filler");
        setStaticItem(inventory, config, BASE_PATH + ".materials.back");
        return inventory;
    }

    private ItemStack buildAbilityItem(MainConfigManager config, AbilityStatus abilityStatus) {
        String stateKey = switch (abilityStatus.state()) {
            case ACTIVE -> "active";
            case COOLDOWN -> "cooldown";
            case BLOCKED -> "blocked";
        };
        String itemPath = BASE_PATH + ".items." + stateKey;
        Material material = config.getGuiMaterial(itemPath + ".material", Material.BOOK);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        String abilityName = evolutionManager.getDisplayAbilityName(abilityStatus.ability().id());
        String displayState = config.getGuiString(BASE_PATH + ".states." + stateKey, stateKey.toUpperCase(Locale.ROOT));
        String cooldownText = abilityStatus.state() == AbilityStatus.AbilityState.COOLDOWN
                ? formatCooldown(abilityStatus.remainingCooldownSeconds(),
                        config.getGuiString(BASE_PATH + ".cooldown-time-format", "seconds"))
                : "-";

        String title = config.getGuiString(itemPath + ".title", "&b{ability}");
        meta.displayName(MessageUtils.getColoredComponent(applyPlaceholders(title, abilityName,
                displayState, cooldownText, abilityStatus.requiredBlocks(), abilityStatus.unlocked())));

        int cmd = config.getGuiInt(itemPath + ".custom-model-data", 0);
        if (cmd > 0) {
            CustomModelDataComponent cmdComponent = meta.getCustomModelDataComponent();
            cmdComponent.setFloats(List.of((float) cmd));
            meta.setCustomModelDataComponent(cmdComponent);
        }

        List<String> loreLines = config.getGuiStringList(itemPath + ".lore");
        List<Component> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(MessageUtils.getColoredComponent(applyPlaceholders(line, abilityName, displayState,
                    cooldownText, abilityStatus.requiredBlocks(), abilityStatus.unlocked())));
        }
        meta.lore(lore.isEmpty() ? null : lore);
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
            meta.displayName(MessageUtils.getColoredComponent(title));
            if (cmd > 0) {
                CustomModelDataComponent cmdComponent = meta.getCustomModelDataComponent();
                cmdComponent.setFloats(List.of((float) cmd));
                meta.setCustomModelDataComponent(cmdComponent);
            }
            if (!loreLines.isEmpty()) {
                List<Component> lore = new ArrayList<>();
                for (String line : loreLines) {
                    lore.add(MessageUtils.getColoredComponent(line));
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

    private String applyPlaceholders(String input, String abilityName, String state, String cooldown,
            int requiredBlocks,
            boolean unlocked) {
        return input
                .replace("{ability}", abilityName)
                .replace("{state}", state)
                .replace("{cooldown}", cooldown)
                .replace("{required_blocks}", String.valueOf(requiredBlocks))
                .replace("{unlocked}", unlocked ? "true" : "false");
    }

    private String formatCooldown(long seconds, String format) {
        if ("mm:ss".equalsIgnoreCase(format)) {
            long minutes = seconds / 60;
            long remainingSeconds = seconds % 60;
            return String.format(Locale.ROOT, "%02d:%02d", minutes, remainingSeconds);
        }
        return String.valueOf(seconds);
    }
}

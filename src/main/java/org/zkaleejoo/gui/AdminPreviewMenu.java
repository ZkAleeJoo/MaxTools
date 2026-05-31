package org.zkaleejoo.gui;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.zkaleejoo.MaxTools;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.evolution.ToolEvolutionManager;
import org.zkaleejoo.utils.MessageUtils;
import org.zkaleejoo.utils.MetKeys;

public class AdminPreviewMenu {

    private static final String BASE_PATH = "menu-layouts.admin-preview";
    private final MaxTools plugin;
    private final ToolEvolutionManager evolutionManager;

    public AdminPreviewMenu(MaxTools plugin, ToolEvolutionManager evolutionManager) {
        this.plugin = plugin;
        this.evolutionManager = evolutionManager;
    }

    public Inventory build(Player player) {
        int size = plugin.getConfigManager().getGuiInventorySize(BASE_PATH + ".size", 54);
        String title = plugin.getConfigManager().getGuiString(BASE_PATH + ".title", "&8Admin Preview &7(Temp Demo)");
        MaxEvolutionMenuHolder holder = new MaxEvolutionMenuHolder(MaxEvolutionMenuHolder.MenuKind.ADMIN_PREVIEW);
        Inventory inventory = Bukkit.createInventory(holder, size, MessageUtils.getColoredComponent(title));
        holder.bindInventory(inventory);

        setIfValid(inventory, plugin.getConfigManager().getGuiInt(BASE_PATH + ".slots.localized-tools", 10),
                buildLocalizedToolsItem());
        setIfValid(inventory, plugin.getConfigManager().getGuiInt(BASE_PATH + ".slots.localized-enchantments", 12),
                buildLocalizedEnchantmentsItem());
        setIfValid(inventory, plugin.getConfigManager().getGuiInt(BASE_PATH + ".slots.progress-bars", 14),
                buildProgressBarItem());
        setIfValid(inventory, plugin.getConfigManager().getGuiInt(BASE_PATH + ".slots.ability-states", 16),
                buildAbilityStatesItem());
        setIfValid(inventory, plugin.getConfigManager().getGuiInt(BASE_PATH + ".slots.evolution-lore", 31),
                buildEvolutionLorePreviewItem());

        fillBorders(inventory);
        return inventory;
    }

    private ItemStack buildLocalizedToolsItem() {
        MainConfigManager config = plugin.getConfigManager();
        String itemPath = BASE_PATH + ".items.localized-tools";
        ItemStack item = new ItemStack(config.getGuiMaterial(itemPath + ".material", Material.NETHERITE_PICKAXE));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(
                MessageUtils.getColoredComponent(config.getGuiString(itemPath + ".title", "&bLocalized Tool Names")));
        List<Component> lore = config.getGuiStringList(itemPath + ".lore", List.of(
                "&7Current tool locale mapping:",
                "&8- &fWOODEN_PICKAXE: &a{wooden_pickaxe}",
                "&8- &fDIAMOND_AXE: &a{diamond_axe}",
                "&8- &fNETHERITE_SHOVEL: &a{netherite_shovel}",
                "&7Source: &emessages_*.yml -> tool-names"))
                .stream()
                .map(line -> line
                        .replace("{wooden_pickaxe}", config.getToolName(Material.WOODEN_PICKAXE))
                        .replace("{diamond_axe}", config.getToolName(Material.DIAMOND_AXE))
                        .replace("{netherite_shovel}", config.getToolName(Material.NETHERITE_SHOVEL)))
                .map(this::line)
                .toList();
        meta.lore(lore);
        meta.getPersistentDataContainer().set(MetKeys.key(plugin, MetKeys.MENU_NON_MOVABLE), PersistentDataType.BYTE,
                (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildLocalizedEnchantmentsItem() {
        MainConfigManager config = plugin.getConfigManager();
        String itemPath = BASE_PATH + ".items.localized-enchantments";
        ItemStack item = new ItemStack(config.getGuiMaterial(itemPath + ".material", Material.ENCHANTED_BOOK));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(MessageUtils
                .getColoredComponent(config.getGuiString(itemPath + ".title", "&dLocalized Enchant Names")));
        List<Component> lore = config.getGuiStringList(itemPath + ".lore", List.of(
                "&7Names read from language file:",
                "&8- &fEFFICIENCY: &a{efficiency}",
                "&8- &fUNBREAKING: &a{unbreaking}",
                "&8- &fFORTUNE: &a{fortune}",
                "&7Source: &emessages_*.yml -> enchantments"))
                .stream()
                .map(line -> line
                        .replace("{efficiency}", evolutionManager.getDisplayEnchantmentName("EFFICIENCY"))
                        .replace("{unbreaking}", evolutionManager.getDisplayEnchantmentName("UNBREAKING"))
                        .replace("{fortune}", evolutionManager.getDisplayEnchantmentName("FORTUNE")))
                .map(this::line)
                .toList();
        meta.lore(lore);
        meta.getPersistentDataContainer().set(MetKeys.key(plugin, MetKeys.MENU_NON_MOVABLE), PersistentDataType.BYTE,
                (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildProgressBarItem() {
        MainConfigManager config = plugin.getConfigManager();
        String itemPath = BASE_PATH + ".items.progress-bars";
        ItemStack item = new ItemStack(config.getGuiMaterial(itemPath + ".material", Material.COMPASS));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        int barLength = config.getToolStatsMilestoneBarLength();
        String filled = config.getToolStatsMilestoneFilledChar();
        String empty = config.getToolStatsMilestoneEmptyChar();
        List<Integer> samples = config.getGuiIntegerList(itemPath + ".samples", List.of(25, 60, 95)).stream()
                .map(value -> Math.max(0, Math.min(100, value)))
                .toList();
        int sample1 = samples.size() > 0 ? samples.get(0) : 25;
        int sample2 = samples.size() > 1 ? samples.get(1) : 60;
        int sample3 = samples.size() > 2 ? samples.get(2) : 95;
        String bar1 = evolutionManager.buildProgressBar(sample1, barLength, filled, empty);
        String bar2 = evolutionManager.buildProgressBar(sample2, barLength, filled, empty);
        String bar3 = evolutionManager.buildProgressBar(sample3, barLength, filled, empty);

        meta.displayName(
                MessageUtils.getColoredComponent(config.getGuiString(itemPath + ".title", "&6Progress Bar Preview")));
        List<Component> lore = config.getGuiStringList(itemPath + ".lore", List.of(
                "&7{sample_1_percent}% &8» {sample_1_bar}",
                "&7{sample_2_percent}% &8» {sample_2_bar}",
                "&7{sample_3_percent}% &8» {sample_3_bar}",
                "&7Unit: &f{unit}"))
                .stream()
                .map(line -> line
                        .replace("{sample_1_percent}", String.valueOf(sample1))
                        .replace("{sample_1_bar}", bar1)
                        .replace("{sample_2_percent}", String.valueOf(sample2))
                        .replace("{sample_2_bar}", bar2)
                        .replace("{sample_3_percent}", String.valueOf(sample3))
                        .replace("{sample_3_bar}", bar3)
                        .replace("{bar_25}", bar1)
                        .replace("{bar_60}", bar2)
                        .replace("{bar_95}", bar3)
                        .replace("{unit}", config.getProgressUnit()))
                .map(this::line)
                .toList();
        meta.lore(lore);
        meta.getPersistentDataContainer().set(MetKeys.key(plugin, MetKeys.MENU_NON_MOVABLE), PersistentDataType.BYTE,
                (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildAbilityStatesItem() {
        MainConfigManager config = plugin.getConfigManager();
        String itemPath = BASE_PATH + ".items.ability-states";
        ItemStack item = new ItemStack(config.getGuiMaterial(itemPath + ".material", Material.BLAZE_POWDER));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        String active = config.getEvolutionLoreStateLabel("active", "&aACTIVE");
        String cooldown = config.getEvolutionLoreStateLabel("cooldown", "&eON COOLDOWN");
        String blocked = config.getEvolutionLoreStateLabel("blocked", "&cBLOCKED");
        int cooldownDemoSeconds = Math.max(0, config.getGuiInt(itemPath + ".cooldown-demo-seconds", 12));

        meta.displayName(
                MessageUtils.getColoredComponent(config.getGuiString(itemPath + ".title", "&eAbility States Preview")));
        List<Component> lore = config.getGuiStringList(itemPath + ".lore", List.of(
                "&8- &f{self_repair} &8» {active}",
                "&8- &f{auto_smelt} &8» {cooldown} &7({cooldown_seconds}s)",
                "&8- &f{telepathy} &8» {blocked}"))
                .stream()
                .map(line -> line
                        .replace("{self_repair}", evolutionManager.getDisplayAbilityName("self-repair"))
                        .replace("{auto_smelt}", evolutionManager.getDisplayAbilityName("auto-smelt"))
                        .replace("{telepathy}", evolutionManager.getDisplayAbilityName("telepathy"))
                        .replace("{active}", MessageUtils.getColoredMessage(active))
                        .replace("{cooldown}", MessageUtils.getColoredMessage(cooldown))
                        .replace("{blocked}", MessageUtils.getColoredMessage(blocked))
                        .replace("{cooldown_seconds}", String.valueOf(cooldownDemoSeconds)))
                .map(this::line)
                .toList();
        meta.lore(lore);
        meta.getPersistentDataContainer().set(MetKeys.key(plugin, MetKeys.MENU_NON_MOVABLE), PersistentDataType.BYTE,
                (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildEvolutionLorePreviewItem() {
        MainConfigManager config = plugin.getConfigManager();
        String itemPath = BASE_PATH + ".items.evolution-lore";
        ItemStack item = new ItemStack(config.getGuiMaterial(itemPath + ".material", Material.BOOK));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        String progressFormat = config.getEvolutionLoreProgressFormat(
                "&7Progress: &f{bar} &8(&f{percent}%&8) &7[{current}/{target}]");
        int barLength = config.getEvolutionLoreProgressBarLength(10);
        String filledChar = config.getEvolutionLoreFilledChar("█");
        String emptyChar = config.getEvolutionLoreEmptyChar("░");
        String bar = evolutionManager.buildProgressBar(72, barLength, filledChar, emptyChar);

        String previewLine = progressFormat
                .replace("{bar}", bar)
                .replace("{percent}", "72")
                .replace("{current}", "720")
                .replace("{target}", "1000");

        String enchantLine = MessageUtils.getColoredMessage(config.getEvolutionLoreLineFormat()
                .replace("{enchant}", evolutionManager.getDisplayEnchantmentName("EFFICIENCY"))
                .replace("{level}", "3"));
        meta.displayName(
                MessageUtils.getColoredComponent(config.getGuiString(itemPath + ".title", "&aEvolution Lore Preview")));
        List<Component> lore = config.getGuiStringList(itemPath + ".lore", List.of(
                "&7Format from config:",
                "{progress_line}",
                "&8 ",
                "&7Enchant example: {enchant_line}",
                "&7State examples and labels are localized"))
                .stream()
                .map(line -> line
                        .replace("{progress_line}", previewLine)
                        .replace("{enchant_line}", enchantLine))
                .map(this::line)
                .toList();
        meta.lore(lore);
        meta.getPersistentDataContainer().set(MetKeys.key(plugin, MetKeys.MENU_NON_MOVABLE), PersistentDataType.BYTE,
                (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private void fillBorders(Inventory inventory) {
        ItemStack filler = new ItemStack(
                plugin.getConfigManager().getGuiMaterial(BASE_PATH + ".border.material",
                        Material.BLACK_STAINED_GLASS_PANE));
        ItemMeta meta = filler.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.displayName(MessageUtils
                .getColoredComponent(plugin.getConfigManager().getGuiString(BASE_PATH + ".border.title", " ")));
        meta.getPersistentDataContainer().set(MetKeys.key(plugin, MetKeys.MENU_NON_MOVABLE), PersistentDataType.BYTE,
                (byte) 1);
        filler.setItemMeta(meta);

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) != null) {
                continue;
            }
            int row = slot / 9;
            int col = slot % 9;
            if (row == 0 || row == 5 || col == 0 || col == 8) {
                inventory.setItem(slot, filler);
            }
        }
    }

    private Component line(String text) {
        return MessageUtils.getColoredComponent(text);
    }

    private void setIfValid(Inventory inventory, int slot, ItemStack item) {
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        inventory.setItem(slot, item);
    }
}

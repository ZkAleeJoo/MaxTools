package org.zkaleejoo.commands.subcommands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.zkaleejoo.MaxTools;
import org.zkaleejoo.evolution.EvolutionMilestone;
import org.zkaleejoo.evolution.SpecialAbilityConfig;
import org.zkaleejoo.evolution.ToolEvolutionManager;
import org.zkaleejoo.utils.MessageUtils;
import org.zkaleejoo.utils.MetKeys;

public class TestToolSubCommand implements SubCommand {

    private static final Set<String> SUPPORTED_SUFFIXES = Set.of("_PICKAXE", "_AXE", "_SHOVEL");

    private final MaxTools plugin;

    public TestToolSubCommand(MaxTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "testtool";
    }

    @Override
    public List<String> getAliases() {
        return List.of("testtools");
    }

    @Override
    public String getPermission() {
        return "maxtools.admin.testtool";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgOnlyPlayers()));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgTestToolUsage()));
            return true;
        }

        ToolEvolutionManager evolutionManager = plugin.getToolEvolutionManager();
        Material material = parseTrackedMaterial(args[0], evolutionManager);
        if (material == null) {
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix()
                            + plugin.getConfigManager().getMsgTestToolInvalidMaterial()));
            return true;
        }

        String abilityArg = "";
        String usageArg = null;
        if (args.length >= 2) {
            if (isInteger(args[1])) {
                usageArg = args[1];
            } else {
                abilityArg = args[1].trim();
                if (args.length >= 3) {
                    usageArg = args[2];
                }
            }
        }

        int usage = parseUsageArg(usageArg, player);
        if (usage < 0) {
            return true;
        }

        Set<String> unlockedAbilities = resolveAbilities(abilityArg, material, evolutionManager, player);
        if (unlockedAbilities == null) {
            return true;
        }

        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgTestToolMetadainvalid()));
            return true;
        }

        String testToolId = plugin.getTestToolRegistry().registerTool(
                material,
                usage,
                unlockedAbilities,
                player.getUniqueId(),
                player.getName());

        initializePdc(meta.getPersistentDataContainer(), material, usage, unlockedAbilities, evolutionManager,
                testToolId);
        evolutionManager.registerCustomTool(meta, material, player.getUniqueId(), player.getName(), true);

        itemStack.setItemMeta(meta);
        evolutionManager.updateProgressDisplay(itemStack, usage, player);
        evolutionManager.updateProgressLore(itemStack, usage);

        player.getInventory().addItem(itemStack);

        String abilitiesSummary = unlockedAbilities.isEmpty()
                ? plugin.getConfigManager().getMsgTestToolNoAbilities()
                : String.join(", ", unlockedAbilities);
        String createdMessage = plugin.getConfigManager().getMsgTestToolCreated()
                .replace("{id}", testToolId)
                .replace("{material}", material.name())
                .replace("{usage}", String.valueOf(usage))
                .replace("{abilities}", abilitiesSummary);
        player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + createdMessage));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        ToolEvolutionManager evolutionManager = plugin.getToolEvolutionManager();
        if (args.length == 1) {
            String input = args[0].toLowerCase(Locale.ROOT);
            return getTrackedToolMaterials(evolutionManager).stream()
                    .map(Material::name)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(input))
                    .toList();
        }

        if (args.length == 2) {
            String input = args[1].toLowerCase(Locale.ROOT);
            List<String> completions = new ArrayList<>();
            completions.add("all");
            completions.addAll(evolutionManager.getSpecialAbilities().values().stream()
                    .filter(SpecialAbilityConfig::enabled)
                    .map(SpecialAbilityConfig::id)
                    .sorted()
                    .toList());
            return completions.stream().filter(option -> option.startsWith(input)).toList();
        }

        if (args.length == 3) {
            return List.of("0", "100", "500", "1000");
        }

        return Collections.emptyList();
    }

    private Material parseTrackedMaterial(String input, ToolEvolutionManager evolutionManager) {
        if (input == null || input.isBlank()) {
            return null;
        }

        try {
            Material material = Material.valueOf(input.trim().toUpperCase(Locale.ROOT));
            if (!supportsToolCategory(material)) {
                return null;
            }
            return evolutionManager.isTrackedTool(new ItemStack(material)) ? material : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean supportsToolCategory(Material material) {
        String name = material.name();
        for (String suffix : SUPPORTED_SUFFIXES) {
            if (name.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("null")
    private List<Material> getTrackedToolMaterials(ToolEvolutionManager evolutionManager) {
        return Arrays.stream(Material.values())
                .filter(this::supportsToolCategory)
                .filter(material -> evolutionManager.isTrackedTool(new ItemStack(material)))
                .toList();
    }

    private boolean isInteger(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private int parseUsageArg(String usageArg, Player player) {
        if (usageArg == null || usageArg.isBlank()) {
            return 0;
        }

        try {
            int usage = Integer.parseInt(usageArg);
            if (usage < 0) {
                player.sendMessage(MessageUtils.getColoredMessage(
                        plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgTestToolLevelinvaid()));
                return -1;
            }
            return usage;
        } catch (NumberFormatException exception) {
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix()
                            + plugin.getConfigManager().getMsgTestToolLevelnumberinvalid()));
            return -1;
        }
    }

    private Set<String> resolveAbilities(String abilityArg, Material material, ToolEvolutionManager evolutionManager,
            Player player) {
        if (abilityArg == null || abilityArg.isBlank()) {
            return Collections.emptySet();
        }

        if ("all".equalsIgnoreCase(abilityArg)) {
            return evolutionManager.getSpecialAbilities(material).values().stream()
                    .filter(SpecialAbilityConfig::enabled)
                    .map(SpecialAbilityConfig::id)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        String normalized = abilityArg.toLowerCase(Locale.ROOT);
        SpecialAbilityConfig selected = evolutionManager.getSpecialAbilityConfig(normalized, material);
        if (selected == null || !selected.enabled()) {
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getMainConfigManager().getMsgTestToolAbilityinvalid()
                            + abilityArg));
            return null;
        }

        return Set.of(selected.id());
    }

    private void initializePdc(PersistentDataContainer container, Material material, int usage,
            Set<String> unlockedAbilities,
            ToolEvolutionManager evolutionManager, String testToolId) {
        NamespacedKey blocksMinedKey = MetKeys.key(plugin, MetKeys.BLOCKS_MINED);
        NamespacedKey specialUnlockedKey = MetKeys.key(plugin, MetKeys.SPECIAL_UNLOCKED);
        NamespacedKey unlockedAbilitiesKey = MetKeys.key(plugin, MetKeys.UNLOCKED_ABILITIES);
        NamespacedKey lastAppliedMilestoneKey = MetKeys.key(plugin, MetKeys.LAST_APPLIED_MILESTONE);
        NamespacedKey totalAbilityActivationsKey = MetKeys.key(plugin, MetKeys.ABILITY_ACTIVATIONS_TOTAL);
        NamespacedKey lastLoreUsageKey = MetKeys.key(plugin, MetKeys.LAST_LORE_PROGRESS_USAGE);
        NamespacedKey lastLorePercentKey = MetKeys.key(plugin, MetKeys.LAST_LORE_PROGRESS_PERCENT);
        NamespacedKey lastLoreTargetKey = MetKeys.key(plugin, MetKeys.LAST_LORE_PROGRESS_TARGET);
        NamespacedKey baseDisplayNameKey = MetKeys.key(plugin, MetKeys.BASE_DISPLAY_NAME);
        NamespacedKey testModeKey = MetKeys.key(plugin, MetKeys.TEST_MODE);
        NamespacedKey testToolIdKey = MetKeys.key(plugin, MetKeys.TEST_TOOL_ID);

        int safeUsage = Math.max(0, usage);
        container.set(blocksMinedKey, PersistentDataType.INTEGER, safeUsage);

        Set<String> normalizedUnlocked = unlockedAbilities.stream()
                .map(id -> id.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        container.set(unlockedAbilitiesKey, PersistentDataType.STRING, String.join(",", normalizedUnlocked));
        container.set(specialUnlockedKey, PersistentDataType.BYTE,
                normalizedUnlocked.contains("self-repair") ? (byte) 1 : (byte) 0);

        int lastAppliedMilestone = 0;
        for (EvolutionMilestone milestone : evolutionManager.getReachedMilestones(safeUsage)) {
            lastAppliedMilestone = Math.max(lastAppliedMilestone, milestone.blocksRequired());
        }
        container.set(lastAppliedMilestoneKey, PersistentDataType.INTEGER, lastAppliedMilestone);
        container.set(totalAbilityActivationsKey, PersistentDataType.INTEGER, 0);
        container.set(lastLoreUsageKey, PersistentDataType.INTEGER, -1);
        container.set(lastLorePercentKey, PersistentDataType.INTEGER, -1);
        container.set(lastLoreTargetKey, PersistentDataType.INTEGER, -1);
        container.set(testModeKey, PersistentDataType.INTEGER, 1);
        container.set(testToolIdKey, PersistentDataType.STRING, testToolId);
        container.set(baseDisplayNameKey, PersistentDataType.STRING,
                "&c[TEST TOOL #" + testToolId + "]&r " + plugin.getConfigManager().getToolName(material));
    }
}

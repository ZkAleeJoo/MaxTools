package org.zkaleejoo.evolution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.zkaleejoo.MaxTools;
import org.zkaleejoo.utils.MessageUtils;
import org.zkaleejoo.utils.MetKeys;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import org.zkaleejoo.evolution.abilities.AbilityHandlerRegistry;
import org.zkaleejoo.evolution.abilities.AreaBreakAbilityHandler;
import org.zkaleejoo.evolution.abilities.AutoSmeltAbilityHandler;
import org.zkaleejoo.evolution.abilities.HasteAbilityHandler;
import org.zkaleejoo.evolution.abilities.LuckSurgeAbilityHandler;
import org.zkaleejoo.evolution.abilities.MomentumAbilityHandler;
import org.zkaleejoo.evolution.abilities.SaturationPulseAbilityHandler;
import org.zkaleejoo.evolution.abilities.SelfRepairAbilityHandler;
import org.zkaleejoo.evolution.abilities.TelepathyAbilityHandler;
import org.zkaleejoo.evolution.abilities.VeinMinerAbilityHandler;
import org.zkaleejoo.evolution.abilities.XpBoostAbilityHandler;

@SuppressWarnings("null")
public class ToolEvolutionManager {

    private static final Map<String, String> ENCHANTMENT_ALIASES = new HashMap<>();

    static {
        ENCHANTMENT_ALIASES.put("DURABILITY", "UNBREAKING");
        ENCHANTMENT_ALIASES.put("DIG_SPEED", "EFFICIENCY");
        ENCHANTMENT_ALIASES.put("LOOT_BONUS_BLOCKS", "FORTUNE");
    }

    private final MaxTools plugin;

    private final NamespacedKey blocksMinedKey;
    private final NamespacedKey specialUnlockedKey;
    private final NamespacedKey unlockedAbilitiesKey;
    private final NamespacedKey managedLoreLinesKey;
    private final NamespacedKey baseDisplayNameKey;
    private final NamespacedKey lastProgressDisplayNameKey;
    private final NamespacedKey lastAppliedMilestoneKey;
    private final NamespacedKey lastLoreProgressUsageKey;
    private final NamespacedKey lastLoreProgressPercentKey;
    private final NamespacedKey lastLoreProgressTargetKey;
    private final NamespacedKey totalAbilityActivationsKey;
    private final NamespacedKey customToolIdKey;
    private final NamespacedKey testModeKey;
    private final AbilityHandlerRegistry abilityHandlerRegistry;
    private final PlayerPlacedBlockTracker playerPlacedBlockTracker;

    private Set<Material> trackedTools = Collections.emptySet();
    private List<EvolutionMilestone> milestones = new ArrayList<>();
    private Map<String, SpecialAbilityConfig> specialAbilities = new LinkedHashMap<>();
    private Set<Material> countingBlacklist = Collections.emptySet();
    private Set<Material> countingWhitelist = Collections.emptySet();
    private boolean countRequirePreferredTool = true;
    private boolean strictToolCategoryMatch = true;
    private boolean progressDisplayEnabled = true;
    private String progressDisplayFormat = "&7[{current}/{target} {unit}]";
    private String progressDisplayCompletedFormat = "&7[{current}/{target} {unit}]";
    private boolean progressDisplayRefreshBaseNameOnRename = false;
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();
    private final SelfRepairProgressTracker selfRepairProgressTracker = new SelfRepairProgressTracker();

    public ToolEvolutionManager(MaxTools plugin, PlayerPlacedBlockTracker playerPlacedBlockTracker) {
        this.plugin = plugin;
        this.playerPlacedBlockTracker = playerPlacedBlockTracker;
        this.blocksMinedKey = MetKeys.key(plugin, MetKeys.BLOCKS_MINED);
        this.specialUnlockedKey = MetKeys.key(plugin, MetKeys.SPECIAL_UNLOCKED);
        this.unlockedAbilitiesKey = MetKeys.key(plugin, MetKeys.UNLOCKED_ABILITIES);
        this.managedLoreLinesKey = MetKeys.key(plugin, MetKeys.MANAGED_LORE_LINES);
        this.baseDisplayNameKey = MetKeys.key(plugin, MetKeys.BASE_DISPLAY_NAME);
        this.lastProgressDisplayNameKey = MetKeys.key(plugin, MetKeys.LAST_PROGRESS_DISPLAY_NAME);
        this.lastAppliedMilestoneKey = MetKeys.key(plugin, MetKeys.LAST_APPLIED_MILESTONE);
        this.lastLoreProgressUsageKey = MetKeys.key(plugin, MetKeys.LAST_LORE_PROGRESS_USAGE);
        this.lastLoreProgressPercentKey = MetKeys.key(plugin, MetKeys.LAST_LORE_PROGRESS_PERCENT);
        this.lastLoreProgressTargetKey = MetKeys.key(plugin, MetKeys.LAST_LORE_PROGRESS_TARGET);
        this.totalAbilityActivationsKey = MetKeys.key(plugin, MetKeys.ABILITY_ACTIVATIONS_TOTAL);
        this.customToolIdKey = MetKeys.key(plugin, MetKeys.CUSTOM_TOOL_ID);
        this.testModeKey = MetKeys.key(plugin, MetKeys.TEST_MODE);
        this.abilityHandlerRegistry = new AbilityHandlerRegistry()
                .register(AbilityType.SELF_REPAIR, new SelfRepairAbilityHandler())
                .register(AbilityType.AUTO_SMELT, new AutoSmeltAbilityHandler())
                .register(AbilityType.TELEPATHY, new TelepathyAbilityHandler())
                .register(AbilityType.DRILL, new AreaBreakAbilityHandler(AbilityType.DRILL))
                .register(AbilityType.HAMMER, new AreaBreakAbilityHandler(AbilityType.HAMMER))
                .register(AbilityType.VEIN_MINER, new VeinMinerAbilityHandler())
                .register(AbilityType.XP_BOOST, new XpBoostAbilityHandler())
                .register(AbilityType.HASTE, new HasteAbilityHandler())
                .register(AbilityType.MOMENTUM, new MomentumAbilityHandler())
                .register(AbilityType.LUCK_SURGE, new LuckSurgeAbilityHandler())
                .register(AbilityType.SATURATION_PULSE, new SaturationPulseAbilityHandler());
    }

    public void reload() {
        FileConfiguration config = plugin.getConfigManager().getEvolutionConfig();
        trackedTools = parseTrackedTools(config);
        milestones = MilestoneConfigParser.parse(config, "milestones", plugin.getLogger()::warning);
        specialAbilities = parseSpecialAbilities(config, "special-abilities", true);
        parseCountingSettings(config);
        parseProgressDisplaySettings(config);
    }

    public boolean isTrackedTool(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return false;
        }
        return trackedTools.contains(itemStack.getType());
    }

    public boolean isEvolutionEligibleTool(ItemStack itemStack) {
        if (!isTrackedTool(itemStack)) {
            return false;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return false;
        }

        if (hasMaxToolsData(meta)) {
            return !isCustomToolRevoked(meta);
        }

        return isCleanNewTool(meta);
    }

    public boolean isCustomTool(ItemStack itemStack) {
        if (!isTrackedTool(itemStack)) {
            return false;
        }

        ItemMeta meta = itemStack.getItemMeta();
        return meta != null && hasMaxToolsData(meta);
    }

    public boolean isCustomToolRevoked(ItemMeta meta) {
        if (meta == null || plugin.getCustomToolDatabase() == null
                || !plugin.getCustomToolDatabase().isAvailable()) {
            return false;
        }

        String toolId = getCustomToolId(meta);
        if (!toolId.isBlank()) {
            return !plugin.getCustomToolDatabase().containsTool(toolId);
        }

        return hasMaxToolsData(meta) && plugin.getCustomToolDatabase().getLastPurgeAt() > 0L;
    }

    public boolean ensureCustomToolRegistered(ItemStack itemStack, Player owner, boolean testTool) {
        if (!isTrackedTool(itemStack)) {
            return false;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return false;
        }

        if (isCustomToolRevoked(meta)) {
            return false;
        }

        if (!hasMaxToolsData(meta) && !isCleanNewTool(meta)) {
            return false;
        }

        if (getCustomToolId(meta).isBlank()) {
            registerCustomTool(meta, itemStack.getType(), owner == null ? null : owner.getUniqueId(),
                    owner == null ? null : owner.getName(), testTool);
            itemStack.setItemMeta(meta);
        }
        return true;
    }

    public String registerCustomTool(ItemMeta meta, Material material, UUID ownerId, String ownerName,
            boolean testTool) {
        if (meta == null || material == null || material == Material.AIR) {
            return "";
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        String toolId = getCustomToolId(meta);
        if (toolId.isBlank()) {
            toolId = UUID.randomUUID().toString();
            container.set(customToolIdKey, PersistentDataType.STRING, toolId);
        }

        if (plugin.getCustomToolDatabase() != null) {
            plugin.getCustomToolDatabase().registerTool(toolId, material, ownerId, ownerName, testTool);
        }
        return toolId;
    }

    public String getCustomToolId(ItemMeta meta) {
        if (meta == null) {
            return "";
        }
        return meta.getPersistentDataContainer().getOrDefault(customToolIdKey, PersistentDataType.STRING, "");
    }

    private boolean hasMaxToolsData(ItemMeta meta) {
        if (meta == null) {
            return false;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(customToolIdKey, PersistentDataType.STRING)
                || container.has(blocksMinedKey, PersistentDataType.INTEGER)
                || container.has(specialUnlockedKey, PersistentDataType.BYTE)
                || container.has(unlockedAbilitiesKey, PersistentDataType.STRING)
                || container.has(lastAppliedMilestoneKey, PersistentDataType.INTEGER)
                || container.has(testModeKey, PersistentDataType.INTEGER);
    }

    private boolean isCleanNewTool(ItemMeta meta) {
        if (meta == null) {
            return false;
        }

        if (!meta.getPersistentDataContainer().getKeys().isEmpty()) {
            return false;
        }
        if (meta.hasEnchants() || meta.hasLore() || meta.hasDisplayName() || !meta.getCustomModelDataComponent().getFloats().isEmpty()) {
            return false;
        }
        if (!meta.getItemFlags().isEmpty() || meta.isUnbreakable()) {
            return false;
        }
        if (meta.getAttributeModifiers() != null && !meta.getAttributeModifiers().isEmpty()) {
            return false;
        }

        return !(meta instanceof Damageable damageable) || damageable.getDamage() <= 0;
    }

    public int incrementUsage(ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return 0;
        }

        int updated = incrementUsage(itemMeta);
        itemStack.setItemMeta(itemMeta);
        return updated;
    }

    public int incrementUsage(ItemMeta itemMeta) {
        PersistentDataContainer container = itemMeta.getPersistentDataContainer();
        int current = container.getOrDefault(blocksMinedKey, PersistentDataType.INTEGER, 0);
        int maxUsage = milestones.isEmpty() ? Integer.MAX_VALUE
                : milestones.get(milestones.size() - 1).blocksRequired();
        if (current >= maxUsage) {
            return current;
        }

        int updated = Math.min(maxUsage, current + 1);
        container.set(blocksMinedKey, PersistentDataType.INTEGER, updated);
        return updated;
    }

    public int getUsage(ItemStack itemStack) {
        if (itemStack == null) {
            return 0;
        }
        ItemMeta meta = itemStack.getItemMeta();
        return getUsage(meta);
    }

    public int getUsage(ItemMeta meta) {
        if (meta == null) {
            return 0;
        }
        return meta.getPersistentDataContainer().getOrDefault(blocksMinedKey, PersistentDataType.INTEGER, 0);
    }

    public boolean isTestTool(ItemMeta meta) {
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().getOrDefault(testModeKey, PersistentDataType.INTEGER, 0) == 1;
    }

    public void updateProgressDisplay(ItemStack itemStack, int usage, Player player) {
        if (!progressDisplayEnabled || itemStack == null || itemStack.getType() == Material.AIR) {
            return;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }

        updateProgressDisplay(meta, itemStack.getType(), usage);
        itemStack.setItemMeta(meta);
    }

    public void updateProgressDisplay(ItemMeta meta, Material itemType, int usage) {
        if (!progressDisplayEnabled || meta == null || itemType == Material.AIR) {
            return;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        String baseName = container.get(baseDisplayNameKey, PersistentDataType.STRING);
        String previouslyGeneratedDisplayName = container.get(lastProgressDisplayNameKey, PersistentDataType.STRING);
        Component currentDisplayNameComponent = meta.displayName();
        String serializedCurrentDisplayName = currentDisplayNameComponent == null
                ? null
                : LEGACY_SERIALIZER.serialize(currentDisplayNameComponent);
        if (progressDisplayRefreshBaseNameOnRename
                && meta.hasDisplayName()
                && previouslyGeneratedDisplayName != null
                && serializedCurrentDisplayName != null
                && !previouslyGeneratedDisplayName.equals(serializedCurrentDisplayName)) {
            baseName = serializedCurrentDisplayName;
            container.set(baseDisplayNameKey, PersistentDataType.STRING, baseName);
        }
        if (baseName == null || baseName.isBlank()) {
            baseName = (meta.hasDisplayName() && serializedCurrentDisplayName != null)
                    ? serializedCurrentDisplayName
                    : plugin.getConfigManager().getToolName(itemType);
            container.set(baseDisplayNameKey, PersistentDataType.STRING, baseName);
        }

        int target = resolveCurrentTarget(usage);
        int displayedUsage = Math.min(usage, target);
        String renderedProgress = renderProgress(usage, displayedUsage, target);
        String updatedDisplayName = MessageUtils.getColoredMessage(baseName + " " + renderedProgress);
        if (updatedDisplayName.equals(serializedCurrentDisplayName)) {
            return;
        }
        meta.displayName(LEGACY_SERIALIZER.deserialize(updatedDisplayName));
        container.set(lastProgressDisplayNameKey, PersistentDataType.STRING, updatedDisplayName);
    }

    public void updateProgressLore(ItemStack itemStack, int usage) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return;
        }

        if (!plugin.getConfigManager().isEvolutionLoreEnabled()) {
            return;
        }

        FileConfiguration mainConfig = plugin.getConfigManager().getMainConfig();
        String lorePath = plugin.getConfigManager().getEvolutionLoreBasePath();
        if (!isSectionEnabled(mainConfig, lorePath, "progress", false)) {
            return;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }

        if (updateProgressLore(meta, usage)) {
            itemStack.setItemMeta(meta);
        }
    }

    public boolean updateProgressLore(ItemMeta meta, int usage) {
        if (meta == null) {
            return false;
        }

        if (!plugin.getConfigManager().isEvolutionLoreEnabled()) {
            return false;
        }
        FileConfiguration mainConfig = plugin.getConfigManager().getMainConfig();
        String lorePath = plugin.getConfigManager().getEvolutionLoreBasePath();
        if (!isSectionEnabled(mainConfig, lorePath, "progress", false)) {
            return false;
        }

        int interval = Math.max(1, mainConfig.getInt(lorePath + ".sections.progress.update-interval-blocks", 5));
        int target = resolveCurrentTarget(usage);
        int percent = target <= 0 ? 100 : Math.min(100, Math.max(0, (int) ((usage * 100.0D) / target)));

        PersistentDataContainer container = meta.getPersistentDataContainer();
        int lastUsage = container.getOrDefault(lastLoreProgressUsageKey, PersistentDataType.INTEGER, -1);
        int lastPercent = container.getOrDefault(lastLoreProgressPercentKey, PersistentDataType.INTEGER, -1);
        int lastTarget = container.getOrDefault(lastLoreProgressTargetKey, PersistentDataType.INTEGER, -1);

        boolean shouldRefresh = lastUsage < 0
                || target != lastTarget
                || percent != lastPercent
                || (usage - lastUsage) >= interval;

        if (!shouldRefresh) {
            return false;
        }

        updateManagedLore(meta);
        container.set(lastLoreProgressUsageKey, PersistentDataType.INTEGER, usage);
        container.set(lastLoreProgressPercentKey, PersistentDataType.INTEGER, percent);
        container.set(lastLoreProgressTargetKey, PersistentDataType.INTEGER, target);
        return true;
    }

    private int resolveCurrentTarget(int usage) {
        return resolveCurrentTarget(null, usage);
    }

    private int resolveCurrentTarget(Material toolType, int usage) {
        if (milestones.isEmpty()) {
            return Math.max(1, usage);
        }

        for (EvolutionMilestone milestone : milestones) {
            if (usage < milestone.blocksRequired()) {
                return milestone.blocksRequired();
            }
        }

        return milestones.get(milestones.size() - 1).blocksRequired();
    }

    public int getCurrentTarget(int usage) {
        return resolveCurrentTarget(Math.max(0, usage));
    }

    public int getCurrentTarget(Material toolType, int usage) {
        return resolveCurrentTarget(toolType, Math.max(0, usage));
    }

    public String getTierNameForUsage(int usage) {
        return getTierNameForUsage(null, usage);
    }

    public String getTierNameForUsage(Material toolType, int usage) {
        FileConfiguration mainConfig = plugin.getConfigManager().getMainConfig();
        String lorePath = plugin.getConfigManager().getEvolutionLoreBasePath();
        int safeUsage = Math.max(0, usage);
        int target = resolveCurrentTarget(toolType, safeUsage);
        int percent = target <= 0 ? 100 : Math.min(100, Math.max(0, (int) ((safeUsage * 100.0D) / target)));
        int reachedMilestones = resolveTierProgress(mainConfig, lorePath, toolType, safeUsage, percent);
        return resolveTierName(mainConfig.getStringList(lorePath + ".tiers"), reachedMilestones);
    }

    public List<EvolutionMilestone> getMilestones() {
        return List.copyOf(milestones);
    }

    public List<EvolutionMilestone> getMilestones(Material toolType) {
        return List.copyOf(milestones);
    }

    public Map<String, SpecialAbilityConfig> getSpecialAbilities() {
        return Map.copyOf(specialAbilities);
    }

    public Map<String, SpecialAbilityConfig> getSpecialAbilities(Material toolType) {
        return Map.copyOf(specialAbilities);
    }

    public AbilityHandlerRegistry getAbilityHandlerRegistry() {
        return abilityHandlerRegistry;
    }

    public String getDisplayAbilityName(String abilityId) {
        return formatAbilityName(abilityId);
    }

    public List<AbilityStatus> getAbilityStatuses(ItemMeta meta, boolean includeLocked) {
        return getAbilityStatuses(meta, null, includeLocked);
    }

    public List<AbilityStatus> getAbilityStatuses(ItemMeta meta, Material toolType, boolean includeLocked) {
        if (meta == null) {
            return Collections.emptyList();
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        Set<String> unlocked = getUnlockedAbilities(container);
        Map<String, Integer> requiredBlocksByAbility = getRequiredBlocksByAbility(toolType);
        List<AbilityStatus> statuses = new ArrayList<>();

        for (SpecialAbilityConfig ability : specialAbilities.values()) {
            if (!ability.enabled()) {
                continue;
            }
            boolean isUnlocked = unlocked.contains(ability.id());
            if (!isUnlocked && !includeLocked) {
                continue;
            }
            long remainingSeconds = isUnlocked ? getRemainingCooldownSeconds(meta, ability.id()) : 0L;
            int requiredBlocks = requiredBlocksByAbility.getOrDefault(ability.id(), 0);
            statuses.add(new AbilityStatus(ability, isUnlocked, remainingSeconds, requiredBlocks));
        }
        return statuses;
    }

    public ToolDerivedStats getDerivedStats(ItemMeta meta, int usage) {
        return getDerivedStats(meta, null, usage);
    }

    public ToolDerivedStats getDerivedStats(ItemMeta meta, Material toolType, int usage) {
        int safeUsage = Math.max(0, usage);
        int unlockedMilestones = getReachedMilestonesCount(toolType, safeUsage);
        int totalMilestones = milestones.size();
        int milestonePercent = totalMilestones <= 0
                ? 100
                : (int) Math.round((unlockedMilestones * 100.0D) / totalMilestones);

        int totalAbilities = (int) specialAbilities.values().stream().filter(SpecialAbilityConfig::enabled).count();
        int unlockedAbilities = meta == null ? 0
                : (int) getUnlockedAbilities(meta.getPersistentDataContainer()).stream()
                        .filter(id -> {
                            SpecialAbilityConfig ability = specialAbilities.get(id);
                            return ability != null && ability.enabled();
                        })
                        .count();
        int abilityPercent = totalAbilities <= 0
                ? 100
                : (int) Math.round((unlockedAbilities * 100.0D) / totalAbilities);

        int target = getCurrentTarget(toolType, safeUsage);
        Map<String, Integer> activationStats = getAbilityActivationStats(meta, toolType, true);
        int totalActivations = activationStats.values().stream().mapToInt(Integer::intValue).sum();

        return new ToolDerivedStats(
                safeUsage,
                target,
                unlockedMilestones,
                totalMilestones,
                Math.min(100, Math.max(0, milestonePercent)),
                unlockedAbilities,
                totalAbilities,
                Math.min(100, Math.max(0, abilityPercent)),
                totalActivations,
                activationStats);
    }

    public Map<String, Integer> getAbilityActivationStats(ItemMeta meta, boolean includeZeroValues) {
        return getAbilityActivationStats(meta, null, includeZeroValues);
    }

    public Map<String, Integer> getAbilityActivationStats(ItemMeta meta, Material toolType, boolean includeZeroValues) {
        Map<String, Integer> stats = new LinkedHashMap<>();
        if (meta == null) {
            return stats;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        for (SpecialAbilityConfig ability : specialAbilities.values()) {
            if (!ability.enabled()) {
                continue;
            }
            int count = getAbilityActivationCount(container, ability.id());
            if (!includeZeroValues && count <= 0) {
                continue;
            }
            stats.put(ability.id(), count);
        }
        return stats;
    }

    public int incrementAbilityActivation(ItemMeta meta, String abilityId) {
        if (meta == null || abilityId == null || abilityId.isBlank()) {
            return 0;
        }
        String normalized = abilityId.trim().toLowerCase(Locale.ROOT);
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey abilityKey = getAbilityActivationKey(normalized);
        int currentAbilityCount = container.getOrDefault(abilityKey, PersistentDataType.INTEGER, 0);
        int updatedAbilityCount = currentAbilityCount + 1;
        container.set(abilityKey, PersistentDataType.INTEGER, updatedAbilityCount);

        int total = container.getOrDefault(totalAbilityActivationsKey, PersistentDataType.INTEGER, 0) + 1;
        container.set(totalAbilityActivationsKey, PersistentDataType.INTEGER, total);
        return updatedAbilityCount;
    }

    private int getAbilityActivationCount(PersistentDataContainer container, String abilityId) {
        if (container == null || abilityId == null || abilityId.isBlank()) {
            return 0;
        }
        return container.getOrDefault(getAbilityActivationKey(abilityId), PersistentDataType.INTEGER, 0);
    }

    private NamespacedKey getAbilityActivationKey(String abilityId) {
        return MetKeys.abilityActivationKey(plugin, abilityId);
    }

    private String renderProgress(int usage, int displayedUsage, int target) {
        String format = usage >= target ? progressDisplayCompletedFormat : progressDisplayFormat;
        String progressUnit = plugin.getConfigManager().getProgressUnit();
        return MessageUtils.getColoredMessage(format
                .replace("{current}", String.valueOf(displayedUsage))
                .replace("{target}", String.valueOf(target))
                .replace("{unit}", progressUnit));
    }

    public boolean shouldCountBlock(Block block, ItemStack tool) {
        if (block == null || tool == null || tool.getType() == Material.AIR) {
            return false;
        }

        Material type = block.getType();
        if (type == Material.AIR) {
            return false;
        }

        if (playerPlacedBlockTracker != null && playerPlacedBlockTracker.isPlayerPlacedBlock(block)) {
            return false;
        }

        if (!countingWhitelist.isEmpty() && !countingWhitelist.contains(type)) {
            return false;
        }

        if (countingBlacklist.contains(type)) {
            return false;
        }

        if (strictToolCategoryMatch && !matchesToolCategory(block, tool.getType())) {
            return false;
        }

        return !countRequirePreferredTool || block.isPreferredTool(tool);
    }

    private boolean matchesToolCategory(Block block, Material toolType) {
        String name = toolType.name();

        if (name.endsWith("_PICKAXE")) {
            return Tag.MINEABLE_PICKAXE.isTagged(block.getType());
        }

        if (name.endsWith("_AXE")) {
            return Tag.MINEABLE_AXE.isTagged(block.getType());
        }

        if (name.endsWith("_SHOVEL")) {
            return Tag.MINEABLE_SHOVEL.isTagged(block.getType());
        }

        return true;
    }

    public boolean isSpecialUnlocked(ItemStack itemStack) {
        return !getUnlockedAbilities(itemStack).isEmpty();
    }

    public boolean hasUnlockedAbility(ItemStack itemStack, String abilityId) {
        if (abilityId == null || abilityId.isBlank()) {
            return false;
        }
        return getUnlockedAbilities(itemStack).contains(abilityId.trim().toLowerCase(Locale.ROOT));
    }

    public SpecialAbilityConfig getSpecialAbilityConfig(String abilityId) {
        return getSpecialAbilityConfig(abilityId, null);
    }

    public SpecialAbilityConfig getSpecialAbilityConfig(String abilityId, Material toolType) {
        if (abilityId == null || abilityId.isBlank()) {
            return null;
        }
        return specialAbilities.get(abilityId.trim().toLowerCase(Locale.ROOT));
    }

    public EvolutionSyncResult syncEvolution(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR || !isTrackedTool(itemStack)) {
            return new EvolutionSyncResult(false, 0, 0, 0);
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return new EvolutionSyncResult(false, 0, 0, 0);
        }
        if (!hasMaxToolsData(meta)) {
            return new EvolutionSyncResult(false, 0, 0, 0);
        }

        EvolutionSyncResult result = syncEvolution(meta, itemStack.getType(), getUsage(meta));
        itemStack.setItemMeta(meta);
        return result;
    }

    public EvolutionSyncResult syncEvolution(ItemMeta meta, Material toolType, int usage) {
        if (meta == null) {
            return new EvolutionSyncResult(false, 0, 0, 0);
        }
        if (!hasMaxToolsData(meta)) {
            return new EvolutionSyncResult(false, 0, 0, 0);
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        int currentLastApplied = container.getOrDefault(lastAppliedMilestoneKey, PersistentDataType.INTEGER, 0);
        EvolutionSyncPlan plan = EvolutionSyncPlanner.plan(
                usage,
                getUnlockedAbilitiesInternal(container),
                milestones,
                specialAbilities.keySet());

        boolean changed = false;
        int appliedMilestones = 0;
        for (EvolutionMilestone milestone : plan.milestonesToApply()) {
            if (applyMilestone(meta, toolType, milestone)) {
                appliedMilestones++;
                changed = true;
            }
        }

        if (plan.lastAppliedMilestone() > currentLastApplied) {
            container.set(lastAppliedMilestoneKey, PersistentDataType.INTEGER, plan.lastAppliedMilestone());
            changed = true;
        }

        updateProgressDisplay(meta, toolType, usage);
        updateProgressLore(meta, usage);
        updateManagedLore(meta);

        return new EvolutionSyncResult(changed, appliedMilestones, plan.abilitiesToAdd().size(),
                Math.max(currentLastApplied, plan.lastAppliedMilestone()));
    }

    public Set<String> getUnlockedAbilities(ItemStack itemStack) {
        if (itemStack == null || itemStack.getItemMeta() == null) {
            return Collections.emptySet();
        }

        PersistentDataContainer container = itemStack.getItemMeta().getPersistentDataContainer();
        return getUnlockedAbilities(container);
    }

    public Set<String> getUnlockedAbilities(PersistentDataContainer container) {
        return getUnlockedAbilitiesInternal(container);
    }

    private Set<String> getUnlockedAbilitiesInternal(PersistentDataContainer container) {
        String raw = container.getOrDefault(unlockedAbilitiesKey, PersistentDataType.STRING, "");
        Set<String> unlocked = new LinkedHashSet<>();

        if (raw != null && !raw.isBlank()) {
            for (String token : raw.split(",")) {
                String normalized = token.trim().toLowerCase(Locale.ROOT);
                if (!normalized.isBlank()) {
                    unlocked.add(normalized);
                }
            }
        }

        if (container.getOrDefault(specialUnlockedKey, PersistentDataType.BYTE, (byte) 0) == (byte) 1) {
            unlocked.add("self-repair");
        }

        return unlocked;
    }

    public List<EvolutionMilestone> getReachedMilestones(int usage) {
        return getReachedMilestones(null, usage);
    }

    public List<EvolutionMilestone> getReachedMilestones(Material toolType, int usage) {
        List<EvolutionMilestone> reached = new ArrayList<>();
        for (EvolutionMilestone milestone : milestones) {
            if (usage >= milestone.blocksRequired()) {
                reached.add(milestone);
            }
        }
        return reached;
    }

    public List<EvolutionMilestone> getNewlyReachedMilestones(ItemStack itemStack, int usage) {
        if (itemStack == null || usage <= 0) {
            return Collections.emptyList();
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return Collections.emptyList();
        }

        List<EvolutionMilestone> newlyReached = getNewlyReachedMilestones(meta, itemStack.getType(), usage);
        return newlyReached;
    }

    public List<EvolutionMilestone> getNewlyReachedMilestones(ItemMeta meta, int usage) {
        return getNewlyReachedMilestones(meta, null, usage);
    }

    public List<EvolutionMilestone> getNewlyReachedMilestones(ItemMeta meta, Material toolType, int usage) {
        if (meta == null || usage <= 0) {
            return Collections.emptyList();
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        int lastApplied = container.getOrDefault(lastAppliedMilestoneKey, PersistentDataType.INTEGER, 0);
        if (usage <= lastApplied) {
            return Collections.emptyList();
        }

        List<EvolutionMilestone> newlyReached = new ArrayList<>();
        int newLastApplied = lastApplied;
        for (EvolutionMilestone milestone : milestones) {
            int required = milestone.blocksRequired();
            if (required <= lastApplied) {
                continue;
            }
            if (required > usage) {
                break;
            }
            newlyReached.add(milestone);
            newLastApplied = required;
        }

        if (newLastApplied > lastApplied) {
            container.set(lastAppliedMilestoneKey, PersistentDataType.INTEGER, newLastApplied);
        }

        return newlyReached;
    }

    public boolean applyMilestone(ItemStack itemStack, EvolutionMilestone milestone) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return false;
        }

        boolean changed = applyMilestone(meta, itemStack.getType(), milestone);
        if (changed) {
            itemStack.setItemMeta(meta);
        }
        return changed;
    }

    public boolean applyMilestone(ItemMeta meta, EvolutionMilestone milestone) {
        return applyMilestone(meta, null, milestone);
    }

    public boolean applyMilestone(ItemMeta meta, Material toolType, EvolutionMilestone milestone) {
        if (meta == null) {
            return false;
        }
        boolean changed = false;

        if (milestone.enchantment() != null && !milestone.enchantment().isBlank()) {
            Enchantment enchantment = resolveEnchantment(milestone.enchantment());
            if (enchantment != null) {
                int currentLevel = meta.getEnchantLevel(enchantment);
                if (currentLevel < milestone.level()) {
                    meta.removeEnchant(enchantment);
                    meta.addEnchant(enchantment, milestone.level(), true);
                    changed = true;
                }
            }
        }

        if (!milestone.unlockAbilities().isEmpty()) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            Set<String> unlocked = getUnlockedAbilitiesInternal(container);
            boolean unlockedChanged = false;
            for (String abilityId : milestone.unlockAbilities()) {
                String normalized = abilityId.toLowerCase(Locale.ROOT);
                if (specialAbilities.containsKey(normalized) && unlocked.add(normalized)) {
                    unlockedChanged = true;
                }
            }

            if (unlockedChanged) {
                container.set(unlockedAbilitiesKey, PersistentDataType.STRING, String.join(",", unlocked));
                if (unlocked.contains("self-repair")) {
                    container.set(specialUnlockedKey, PersistentDataType.BYTE, (byte) 1);
                }
                changed = true;
            }
        }

        if (changed) {
            updateManagedLore(meta);
        }

        return changed;
    }

    public List<String> getAbilitiesToNotify(EvolutionMilestone milestone) {
        return getAbilitiesToNotify(milestone, null);
    }

    public List<String> getAbilitiesToNotify(EvolutionMilestone milestone, Material toolType) {
        Map<String, SpecialAbilityConfig> abilities = specialAbilities;
        return milestone.unlockAbilities().stream()
                .map(s -> s.toLowerCase(Locale.ROOT))
                .filter(abilities::containsKey)
                .toList();
    }

    public void processSpecialAbilities(ItemStack tool) {
        Set<String> unlocked = getUnlockedAbilities(tool);
        if (unlocked.isEmpty()) {
            return;
        }

        ItemMeta meta = tool.getItemMeta();
        if (meta == null) {
            return;
        }

        boolean changed = false;
        for (String abilityId : unlocked) {
            SpecialAbilityConfig ability = specialAbilities.get(abilityId);
            if (ability == null || ability.type() != AbilityType.SELF_REPAIR
                    || ability.trigger() != AbilityTrigger.BLOCK_BREAK) {
                continue;
            }

            var handler = abilityHandlerRegistry.find(ability.type()).orElse(null);
            if (handler == null || !handler.canTrigger(meta, ability, this)) {
                continue;
            }

            if (handler.onApply(meta, ability, this)) {
                changed = true;
            }
        }

        if (changed) {
            tool.setItemMeta(meta);
        }
    }

    public boolean processSpecialAbilities(ItemMeta meta, Set<String> unlockedAbilities) {
        return processSpecialAbilities(meta, null, unlockedAbilities);
    }

    public boolean processSpecialAbilities(ItemMeta meta, Material toolType, Set<String> unlockedAbilities) {
        if (meta == null || unlockedAbilities == null || unlockedAbilities.isEmpty()) {
            return false;
        }

        Map<String, SpecialAbilityConfig> abilities = specialAbilities;
        boolean changed = false;
        for (String abilityId : unlockedAbilities) {
            SpecialAbilityConfig ability = abilities.get(abilityId);
            if (ability == null || ability.type() != AbilityType.SELF_REPAIR
                    || ability.trigger() != AbilityTrigger.BLOCK_BREAK) {
                continue;
            }

            var handler = abilityHandlerRegistry.find(ability.type()).orElse(null);
            if (handler == null || !handler.canTrigger(meta, ability, this)) {
                continue;
            }

            if (handler.onApply(meta, ability, this)) {
                changed = true;
            }
        }
        return changed;
    }

    public boolean processWalkDistanceAbility(ItemMeta meta, SpecialAbilityConfig ability, double distance) {
        return processWalkDistanceAbility(meta, ability, distance, getCustomToolId(meta));
    }

    public boolean processWalkDistanceAbility(ItemMeta meta, SpecialAbilityConfig ability, double distance,
            String progressKey) {
        if (meta == null || ability == null || ability.type() != AbilityType.SELF_REPAIR
                || ability.trigger() != AbilityTrigger.WALK_DISTANCE) {
            return false;
        }

        String counterKey = buildSelfRepairProgressKey(progressKey, ability);
        SelfRepairProgress.Result progress = selfRepairProgressTracker.record(counterKey, ability.distanceBlocks(),
                distance);
        if (progress.ignored()) {
            return false;
        }

        double remainingDistance = progress.remainingDistance();
        boolean changed = false;
        int activations = progress.activations();
        if (activations > 0) {
            var handler = abilityHandlerRegistry.find(ability.type()).orElse(null);
            for (int i = 0; i < activations; i++) {
                if (handler == null || !isAbilityReady(meta, ability)) {
                    selfRepairProgressTracker.deferActivation(counterKey, ability.distanceBlocks(), remainingDistance);
                    break;
                }

                if (Math.random() > ability.chance()) {
                    continue;
                }

                if (handler.onApply(meta, ability, this)) {
                    changed = true;
                    continue;
                }

                selfRepairProgressTracker.reset(counterKey);
                break;
            }
        }

        return changed;
    }

    public void clearWalkDistanceProgress(UUID playerId) {
        if (playerId == null) {
            return;
        }
        selfRepairProgressTracker.clearKeysStartingWith(playerId + ":");
    }

    private String buildSelfRepairProgressKey(String progressKey, SpecialAbilityConfig ability) {
        String baseKey = progressKey == null || progressKey.isBlank() ? "unknown" : progressKey;
        String abilityId = ability == null ? "self-repair" : ability.id();
        return baseKey + ":" + abilityId;
    }

    public boolean canProcAbility(ItemMeta meta, SpecialAbilityConfig ability) {
        if (!isAbilityReady(meta, ability)) {
            return false;
        }

        return Math.random() <= ability.chance();
    }

    public boolean isAbilityReady(ItemMeta meta, SpecialAbilityConfig ability) {
        if (meta == null || ability == null || !ability.enabled()) {
            return false;
        }

        if (!ability.compatibleWithMending() && meta.hasEnchant(Enchantment.MENDING)) {
            return false;
        }

        return isOffCooldown(meta, ability.id());
    }

    public void applyCooldown(ItemMeta meta, SpecialAbilityConfig ability) {
        if (meta == null || ability == null) {
            return;
        }
        markCooldown(meta, ability.id(), ability.cooldownSeconds());
    }

    public boolean isOffCooldown(ItemMeta meta, String abilityId) {
        NamespacedKey cooldownKey = MetKeys.abilityCooldownKey(plugin, abilityId);
        long now = System.currentTimeMillis();
        long nextUse = meta.getPersistentDataContainer().getOrDefault(cooldownKey, PersistentDataType.LONG, 0L);
        return now >= nextUse;
    }

    private void markCooldown(ItemMeta meta, String abilityId, int cooldownSeconds) {
        NamespacedKey cooldownKey = MetKeys.abilityCooldownKey(plugin, abilityId);
        long nextUse = System.currentTimeMillis() + Math.max(0, cooldownSeconds) * 1000L;
        meta.getPersistentDataContainer().set(cooldownKey, PersistentDataType.LONG, nextUse);
    }

    @SuppressWarnings("deprecation")
    private void updateManagedLore(ItemMeta meta) {
        if (!plugin.getConfigManager().isEvolutionLoreEnabled()) {
            return;
        }

        List<String> lore = new ArrayList<>();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        FileConfiguration mainConfig = plugin.getConfigManager().getMainConfig();
        String lorePath = plugin.getConfigManager().getEvolutionLoreBasePath();
        int usage = container.getOrDefault(blocksMinedKey, PersistentDataType.INTEGER, 0);
        int target = resolveCurrentTarget(usage);
        int percent = target <= 0 ? 100 : Math.min(100, Math.max(0, (int) ((usage * 100.0D) / target)));

        if (isSectionEnabled(mainConfig, lorePath, "header", false)) {
            appendLoreLine(lore,
                    mainConfig.getString(lorePath + ".sections.header.top", "&8&m--------------------"));
        }

        if (isSectionEnabled(mainConfig, lorePath, "tier", false)) {
            int reachedMilestones = resolveTierProgress(mainConfig, lorePath, null, usage, percent);
            List<String> tiers = mainConfig.getStringList(lorePath + ".tiers");
            String tier = resolveTierName(tiers, reachedMilestones);
            String tierFormat = mainConfig.getString(lorePath + ".sections.tier.format", "&7Tier: &f{tier}");
            appendLoreLine(lore, tierFormat.replace("{tier}", tier));
        }

        if (isSectionEnabled(mainConfig, lorePath, "progress", false)) {
            String progressFormat = mainConfig.getString(
                    lorePath + ".sections.progress.format",
                    "&7Progreso: &f{bar} &8(&f{percent}%&8) &7[{current}/{target}]");
            int barLength = Math.max(4, mainConfig.getInt(lorePath + ".sections.progress.bar-length", 10));
            String filledChar = mainConfig.getString(lorePath + ".sections.progress.filled-char", "█");
            String emptyChar = mainConfig.getString(lorePath + ".sections.progress.empty-char", "░");
            String bar = buildRawProgressBar(percent, barLength, filledChar, emptyChar);
            String rendered = progressFormat
                    .replace("{bar}", bar)
                    .replace("{percent}", String.valueOf(percent))
                    .replace("{current}", String.valueOf(usage))
                    .replace("{target}", String.valueOf(target));
            appendLoreLine(lore, rendered);
        }

        if (isSectionEnabled(mainConfig, lorePath, "abilities", false)) {
            appendLoreLine(lore,
                    mainConfig.getString(lorePath + ".sections.abilities.title", "&8[&bSkills&8]"));
            String abilityFormat = mainConfig.getString(
                    lorePath + ".sections.abilities.line-format",
                    "&8- &b{ability} &8» {ability_state} {cooldown}");
            String activeState = mainConfig.getString(lorePath + ".states.active", "&aACTIVE");
            String cooldownState = mainConfig.getString(lorePath + ".states.cooldown", "&eCOOLDOWN");
            String blockedState = mainConfig.getString(lorePath + ".states.blocked", "&cBLOCKED");
            String cooldownFormat = mainConfig.getString(lorePath + ".sections.abilities.cooldown-format",
                    "&7({cooldown}s)");
            String cooldownTimeFormat = mainConfig
                    .getString(lorePath + ".sections.abilities.cooldown-time-format", "seconds");
            boolean showLockedAbilities = mainConfig.getBoolean(lorePath + ".sections.abilities.show-locked",
                    true);

            for (AbilityStatus abilityStatus : getAbilityStatuses(meta, showLockedAbilities)) {
                String abilityName = formatAbilityName(abilityStatus.ability().id());
                String state = switch (abilityStatus.state()) {
                    case BLOCKED -> blockedState;
                    case COOLDOWN -> cooldownState;
                    case ACTIVE -> activeState;
                };
                String cooldownValue = abilityStatus.state() == AbilityStatus.AbilityState.COOLDOWN
                        ? cooldownFormat.replace("{cooldown}",
                                formatCooldown(abilityStatus.remainingCooldownSeconds(), cooldownTimeFormat))
                        : "";

                String rendered = abilityFormat
                        .replace("{ability}", abilityName)
                        .replace("{ability_state}", state)
                        .replace("{cooldown}", cooldownValue);
                appendLoreLine(lore, rendered);
            }
        }

        List<String> managedLore = new ArrayList<>();
        if (isSectionEnabled(mainConfig, lorePath, "enchantments", true)) {
            appendLoreLine(lore,
                    mainConfig.getString(lorePath + ".sections.enchantments.title", "&8[&3Enchants&8]"));
            String enchantmentLineFormat = mainConfig.getString(
                    lorePath + ".sections.enchantments.line-format",
                    plugin.getConfigManager().getEvolutionLoreLineFormat());
            for (Map.Entry<Enchantment, Integer> enchant : meta.getEnchants().entrySet()) {
                String rendered = enchantmentLineFormat
                        .replace("{enchant}", getDisplayEnchantmentName(enchant.getKey()))
                        .replace("{key}", enchant.getKey().getKey().getKey())
                        .replace("{level}", String.valueOf(enchant.getValue()));
                String colored = MessageUtils.getColoredMessage(rendered);
                managedLore.add(colored);
                lore.add(colored);
            }
        }

        if (isSectionEnabled(mainConfig, lorePath, "header", false)) {
            appendLoreLine(lore,
                    mainConfig.getString(lorePath + ".sections.header.bottom", "&8&m--------------------"));
        }

        if (!managedLore.isEmpty()) {
            container.set(managedLoreLinesKey, PersistentDataType.STRING, String.join("\n", managedLore));
        } else {
            container.remove(managedLoreLinesKey);
        }

        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);

        meta.setLore(lore.isEmpty() ? null : lore);
    }

    private void appendLoreLine(List<String> lore, String line) {
        if (line == null || line.isBlank()) {
            return;
        }
        lore.add(MessageUtils.getColoredMessage(line));
    }

    private boolean isSectionEnabled(FileConfiguration mainConfig, String lorePath, String section,
            boolean defaultEnabled) {
        return mainConfig.getBoolean(lorePath + ".sections." + section + ".enabled", defaultEnabled);
    }

    private int getReachedMilestonesCount(Material toolType, int usage) {
        int reached = 0;
        for (EvolutionMilestone milestone : milestones) {
            if (usage >= milestone.blocksRequired()) {
                reached++;
            }
        }
        return reached;
    }

    private int resolveTierProgress(FileConfiguration mainConfig, String lorePath, Material toolType, int usage,
            int percent) {
        String tierMode = mainConfig.getString(lorePath + ".sections.tier.mode", "milestones")
                .trim()
                .toLowerCase(Locale.ROOT);
        if ("percent".equals(tierMode)) {
            List<String> tiers = mainConfig.getStringList(lorePath + ".tiers");
            int tierCount = tiers == null || tiers.isEmpty() ? 5 : tiers.size();
            if (tierCount <= 1) {
                return 0;
            }
            int boundedPercent = Math.max(0, Math.min(100, percent));
            return Math.min(tierCount - 1, (int) Math.floor((boundedPercent / 100.0D) * (tierCount - 1)));
        }
        return getReachedMilestonesCount(toolType, usage);
    }

    private String resolveTierName(List<String> tiers, int reachedMilestones) {
        List<String> defaults = List.of("Common", "Rare", "Epic", "Mythic", "Legendary");
        List<String> availableTiers = tiers == null || tiers.isEmpty() ? defaults : tiers;
        int index = Math.min(Math.max(0, reachedMilestones), availableTiers.size() - 1);
        return availableTiers.get(index);
    }

    private String buildRawProgressBar(int percent, int length, String filledChar, String emptyChar) {
        int filled = (int) Math.round((Math.max(0, Math.min(100, percent)) / 100.0D) * length);
        String safeFilled = (filledChar == null || filledChar.isBlank()) ? "█" : filledChar;
        String safeEmpty = (emptyChar == null || emptyChar.isBlank()) ? "░" : emptyChar;
        return safeFilled.repeat(filled) + safeEmpty.repeat(Math.max(0, length - filled));
    }

    public String buildProgressBar(int percent, int length, String filledChar, String emptyChar) {
        return MessageUtils.getColoredMessage(buildRawProgressBar(percent, length, filledChar, emptyChar));
    }

    private String formatAbilityName(String abilityId) {
        if (abilityId == null || abilityId.isBlank()) {
            return "Unknown";
        }

        String[] parts = abilityId.split("[-_]");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT))
                    .append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return builder.toString();
    }

    private long getRemainingCooldownSeconds(ItemMeta meta, String abilityId) {
        NamespacedKey cooldownKey = MetKeys.abilityCooldownKey(plugin, abilityId);
        long nextUse = meta.getPersistentDataContainer().getOrDefault(cooldownKey, PersistentDataType.LONG, 0L);
        long remainingMillis = nextUse - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            return 0L;
        }
        return Math.max(1L, TimeUnit.MILLISECONDS.toSeconds(remainingMillis));
    }

    private String formatCooldown(long seconds, String timeFormat) {
        if (!"mm:ss".equalsIgnoreCase(timeFormat)) {
            return String.valueOf(seconds);
        }

        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return String.format(Locale.ROOT, "%02d:%02d", minutes, remainingSeconds);
    }

    private Map<String, Integer> getRequiredBlocksByAbility(Material toolType) {
        Map<String, Integer> required = new HashMap<>();
        for (EvolutionMilestone milestone : milestones) {
            int blocksRequired = milestone.blocksRequired();
            for (String abilityId : milestone.unlockAbilities()) {
                String normalized = abilityId.toLowerCase(Locale.ROOT);
                if (!specialAbilities.containsKey(normalized)) {
                    continue;
                }
                required.merge(normalized, blocksRequired, (a, b) -> Math.min(a, b));
            }
        }
        return required;
    }

    public String getDisplayEnchantmentName(String configuredEnchantment) {
        Enchantment enchantment = resolveEnchantment(configuredEnchantment);
        if (enchantment == null) {
            return configuredEnchantment;
        }
        return getDisplayEnchantmentName(enchantment);
    }

    private String getDisplayEnchantmentName(Enchantment enchantment) {
        String key = enchantment.getKey().getKey();
        String translated = plugin.getConfigManager().getEnchantmentName(key);
        if (!translated.isBlank()) {
            return translated;
        }

        String[] parts = key.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT))
                    .append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return builder.toString();
    }

    private Enchantment resolveEnchantment(String configuredEnchantment) {
        if (configuredEnchantment == null || configuredEnchantment.isBlank()) {
            return null;
        }

        String normalized = configuredEnchantment.trim();
        Enchantment enchantment = resolveEnchantmentKey(normalized);
        if (enchantment != null) {
            return enchantment;
        }

        String alias = ENCHANTMENT_ALIASES.get(normalized.toUpperCase(Locale.ROOT));
        if (alias != null) {
            enchantment = resolveEnchantmentKey(alias);
            if (enchantment != null) {
                return enchantment;
            }
        }

        plugin.getLogger().warning("Invalid enchantment configured in milestone: " + configuredEnchantment);
        return null;
    }

    private Enchantment resolveEnchantmentKey(String configuredKey) {
        var enchantmentRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
        String normalized = configuredKey.toLowerCase(Locale.ROOT);
        if (!normalized.contains(":")) {
            return enchantmentRegistry.get(NamespacedKey.minecraft(normalized));
        }

        NamespacedKey namespaced = NamespacedKey.fromString(normalized);
        if (namespaced == null) {
            return null;
        }
        return enchantmentRegistry.get(namespaced);
    }

    private Set<Material> parseTrackedTools(FileConfiguration config) {
        Set<Material> parsed = new LinkedHashSet<>();
        for (String raw : config.getStringList("tracked-tools")) {
            try {
                Material material = Material.valueOf(raw.toUpperCase(Locale.ROOT));
                parsed.add(material);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger()
                        .warning("Invalid material in tracked-tools: " + raw + " (use exact Material enum names)");
            }
        }
        return parsed.isEmpty() ? Collections.emptySet() : parsed;
    }

    private Map<String, SpecialAbilityConfig> parseSpecialAbilities(FileConfiguration config, String path,
            boolean allowLegacyFallback) {
        ConfigurationSection section = config.getConfigurationSection(path);
        Map<String, SpecialAbilityConfig> parsed = new LinkedHashMap<>();

        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection abilitySection = section.getConfigurationSection(key);
                if (abilitySection == null) {
                    continue;
                }

                SpecialAbilityConfig ability = parseSingleAbility(key, abilitySection);
                if (ability != null) {
                    parsed.put(key.toLowerCase(Locale.ROOT), ability);
                }
            }
        }

        if (allowLegacyFallback && parsed.isEmpty()) {
            double legacyChance = config.getDouble("special-ability.repair-chance", 0.15D);
            parsed.put("self-repair", new SpecialAbilityConfig(
                    "self-repair",
                    AbilityType.SELF_REPAIR,
                    true,
                    clampChance(legacyChance, "special-ability.repair-chance"),
                    1,
                    1.0D,
                    1,
                    2500L,
                    1,
                    0,
                    true,
                    Collections.emptySet(),
                    AbilityTrigger.WALK_DISTANCE,
                    80.0D,
                    true));
        }

        return parsed;
    }

    private SpecialAbilityConfig parseSingleAbility(String id, ConfigurationSection section) {
        String typeRaw = section.getString("type", id).toUpperCase(Locale.ROOT).replace('-', '_');
        AbilityType type;
        try {
            type = AbilityType.valueOf(typeRaw);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Invalid ability type for " + id + ": " + typeRaw);
            return null;
        }

        boolean enabled = section.getBoolean("enabled", true);
        double chance = clampChance(section.getDouble("chance", 0.15D), "special-abilities." + id + ".chance");
        int amount = Math.max(1, section.getInt("amount", 1));
        double maxMultiplier = Math.max(1.0D, section.getDouble("max-multiplier", amount));
        int maxStacks = Math.max(1, section.getInt("max-stacks", amount));
        long stackWindowMs = Math.max(1L, section.getLong("stack-window-ms", 2500L));
        int perStackAmplifier = Math.max(1, section.getInt("per-stack-amplifier", 1));
        int cooldownSeconds = Math.max(0, section.getInt("cooldown-seconds", 0));
        boolean compatibleWithMending = section.getBoolean("compatible-with-mending", true);
        AbilityTrigger trigger = parseAbilityTrigger(section.getString("trigger", defaultTrigger(type).name()), id);
        double distanceBlocks = Math.max(1.0D, section.getDouble("distance-blocks", 80.0D));
        boolean requireMainHand = section.getBoolean("require-main-hand", true);

        Set<Material> materialWhitelist = parseMaterialWhitelist(section, id);

        return new SpecialAbilityConfig(id.toLowerCase(Locale.ROOT), type, enabled, chance, amount, maxMultiplier,
                maxStacks, stackWindowMs, perStackAmplifier, cooldownSeconds, compatibleWithMending, materialWhitelist,
                trigger, distanceBlocks, requireMainHand);
    }

    private AbilityTrigger defaultTrigger(AbilityType type) {
        if (type == AbilityType.SELF_REPAIR) {
            return AbilityTrigger.WALK_DISTANCE;
        }
        if (type == AbilityType.HASTE) {
            return AbilityTrigger.TICK;
        }
        return AbilityTrigger.BLOCK_BREAK;
    }

    private AbilityTrigger parseAbilityTrigger(String raw, String abilityId) {
        if (raw == null || raw.isBlank()) {
            return AbilityTrigger.BLOCK_BREAK;
        }

        try {
            return AbilityTrigger.valueOf(raw.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Invalid trigger for " + abilityId + ": " + raw);
            return AbilityTrigger.BLOCK_BREAK;
        }
    }

    private Set<Material> parseMaterialWhitelist(ConfigurationSection section, String abilityId) {
        List<String> configured = section.getStringList("material-whitelist");
        return parseMaterialList(configured, "special-abilities." + abilityId + ".material-whitelist");
    }

    private Set<Material> parseMaterialList(List<String> configured, String logPath) {
        if (configured.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Material> parsed = new LinkedHashSet<>();
        for (String raw : configured) {
            if (raw == null || raw.isBlank()) {
                continue;
            }

            String normalized = raw.trim().toUpperCase(Locale.ROOT);
            try {
                parsed.add(Material.valueOf(normalized));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Invalid material in " + logPath + ": " + raw
                        + " (use exact Material enum names)");
            }
        }

        return parsed.isEmpty() ? Collections.emptySet() : parsed;
    }

    private double clampChance(double raw, String path) {
        if (raw < 0.0D) {
            plugin.getLogger().warning("Configured chance is below 0.0 at " + path + ". Clamping to 0.0");
            return 0.0D;
        }
        if (raw > 1.0D) {
            plugin.getLogger().warning("Configured chance is above 1.0 at " + path + ". Clamping to 1.0");
            return 1.0D;
        }
        return raw;
    }

    private void parseCountingSettings(FileConfiguration config) {
        countRequirePreferredTool = config.getBoolean("counting.require-preferred-tool", true);
        strictToolCategoryMatch = config.getBoolean("counting.strict-tool-category-match", true);
        countingWhitelist = parseMaterialSet(config, "counting.whitelist-materials");

        if (config.contains("counting.blacklist-materials")) {
            Set<Material> configuredBlacklist = parseMaterialSet(config, "counting.blacklist-materials");
            countingBlacklist = configuredBlacklist.isEmpty() ? buildDefaultCountingBlacklist() : configuredBlacklist;
            return;
        }

        countingBlacklist = buildDefaultCountingBlacklist();
    }

    private void parseProgressDisplaySettings(FileConfiguration config) {
        progressDisplayEnabled = config.getBoolean("progress-display.enabled", true);
        progressDisplayFormat = config.getString("progress-display.format", "&7[{current}/{target} {unit}]");
        if (progressDisplayFormat == null || progressDisplayFormat.isBlank()) {
            progressDisplayFormat = "&7[{current}/{target} {unit}]";
        }
        progressDisplayCompletedFormat = config.getString("progress-display.completed-format", progressDisplayFormat);
        if (progressDisplayCompletedFormat == null || progressDisplayCompletedFormat.isBlank()) {
            progressDisplayCompletedFormat = progressDisplayFormat;
        }
        progressDisplayRefreshBaseNameOnRename = config.getBoolean("progress-display.refresh-base-name-on-rename",
                false);
    }

    private Set<Material> parseMaterialSet(FileConfiguration config, String path) {
        List<String> configured = config.getStringList(path);
        if (configured.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Material> parsed = new LinkedHashSet<>();
        for (String raw : configured) {
            if (raw == null || raw.isBlank()) {
                continue;
            }

            String normalized = raw.trim().toUpperCase(Locale.ROOT);
            try {
                parsed.add(Material.valueOf(normalized));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger()
                        .warning("Invalid material in " + path + ": " + raw + " (use exact Material enum names)");
            }
        }

        return parsed.isEmpty() ? Collections.emptySet() : parsed;
    }

    private Set<Material> buildDefaultCountingBlacklist() {
        Set<Material> defaults = new LinkedHashSet<>();
        for (Material material : Material.values()) {
            if (!material.isBlock()) {
                continue;
            }

            String name = material.name();
            if (Tag.LEAVES.isTagged(material)
                    || Tag.FLOWERS.isTagged(material)
                    || Tag.CROPS.isTagged(material)
                    || Tag.SAPLINGS.isTagged(material)
                    || Tag.SMALL_FLOWERS.isTagged(material)
                    || name.endsWith("_GRASS")
                    || name.endsWith("_FERN")
                    || name.endsWith("_BUSH")
                    || name.endsWith("_VINE")) {
                defaults.add(material);
            }
        }
        return defaults;
    }

    public record ToolDerivedStats(
            int usage,
            int currentTarget,
            int unlockedMilestones,
            int totalMilestones,
            int milestoneCompletionPercent,
            int unlockedAbilities,
            int totalAbilities,
            int abilityCompletionPercent,
            int totalAbilityActivations,
            Map<String, Integer> abilityActivations) {
    }
}

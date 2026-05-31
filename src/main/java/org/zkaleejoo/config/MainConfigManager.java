package org.zkaleejoo.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.zkaleejoo.MaxTools;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.Sound;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class MainConfigManager {

    private final CustomConfig configFile;
    private CustomConfig langFile;
    private final CustomConfig evolutionFile;
    private final CustomConfig menusFile;
    private final MaxTools plugin;
    private String selectedLanguage;
    private boolean languageProfileSyncEnabled;
    private static final String LANGUAGE_PROFILES_FOLDER = "language_profiles";

    // VARIABLES CONFIG
    private String prefix;
    private boolean evolutionLoreEnabled;
    private String evolutionLoreLineFormat;
    private Map<String, String> enchantmentNames = new HashMap<>();
    private Map<String, String> fallbackEnchantmentNames = new HashMap<>();
    private String evolutionLoreBasePath = "evolution-lore";
    private boolean updateCheckEnabled;
    private boolean bStatsEnabled;
    private String msgUpdateAvailable;
    private String msgUpdateCurrent;
    private String msgUpdateDownload;
    // VARIABLES MENSAJES
    private String msgNoPermission;
    private String msgPluginReload;
    private String msgMilestoneReached;
    private String msgSpecialUnlocked;
    private String msgToolInfo;
    private String msgOnlyPlayers;
    private String msgInvalidTool;
    private String msgEnabledWord;
    private String msgDisabledWord;
    private String msgCommandUsage;
    private String msgDiscordNotifierNotInitialized;
    private String msgDiscordTestQueued;
    private String msgDiscordTestFailed;
    private String msgDiscordTestConsoleToolName;
    private String msgDiscordTestNoToolName;
    private String msgClearTestToolUsage;
    private String msgClearTestToolNotTest;
    private String msgClearTestToolSuccess;
    private String msgClearTestToolDestroyed;
    private String msgClearTestToolIdNotFound;
    private String msgClearTestToolIdRemoved;
    private String msgCustomToolRevoked;
    private String msgAdminToolsRemoveUsage;
    private String msgAdminToolsRemoveStarted;
    private String msgAdminToolsRemoveSuccess;
    private String msgAdminToolsRemoveDatabaseUnavailable;
    private String msgLanguageChanged;
    private String msgLanguageAlreadySelected;
    private String msgSyncNoChanges;
    private String msgSyncSuccess;
    private String fallbackProgressUnit = "Blocks";
    private Map<String, String> toolNames = new HashMap<>();
    private Map<String, String> fallbackToolNames = new HashMap<>();
    private String menuTitle;
    private String menuToolItemTitle;
    private List<String> menuToolItemLore = new ArrayList<>();
    private boolean toolInfoGuiDefault;
    private int toolStatsMilestoneBarLength;
    private int toolStatsAbilityBarLength;
    private String toolStatsMilestoneFilledChar;
    private String toolStatsMilestoneEmptyChar;
    private String toolStatsAbilityFilledChar;
    private String toolStatsAbilityEmptyChar;
    private boolean discordEnabled;
    private String discordWebhookUrl;
    private String discordServerName;
    private boolean discordMilestoneEnabled;
    private boolean discordAbilityEnabled;
    private String discordMilestoneTemplate;
    private String discordAbilityTemplate;
    private String discordTestTemplate;
    private int discordQueueMaxPending;
    private String msgTestToolUsage;
    private String msgTestToolInvalidMaterial;
    private String msgTestToolMetadainvalid;
    private String msgTestToolLevelinvaid;
    private String msgTestToolLevelnumberinvalid;
    private String msgTestToolAbilityinvalid;
    private String msgTestToolCreated;
    private String msgTestToolNoAbilities;

    public MainConfigManager(MaxTools plugin) {
        this.plugin = plugin;
        configFile = new CustomConfig("config.yml", null, plugin, false);
        evolutionFile = new CustomConfig("evolution.yml", null, plugin, false);
        menusFile = new CustomConfig("menus.yml", null, plugin, false);
        configFile.registerConfig();
        evolutionFile.registerConfig();
        menusFile.registerConfig();
        ensureDefaultLanguageProfiles();
        loadConfig();
    }

    public void loadConfig() {
        FileConfiguration config = configFile.getConfig();

        selectedLanguage = config.getString("general.language", "en");
        languageProfileSyncEnabled = config.getBoolean("general.language-profile-sync", true);
        String langPath = "messages_" + selectedLanguage + ".yml";
        langFile = new CustomConfig(langPath, "lang", plugin, false);
        langFile.registerConfig();
        FileConfiguration lang = langFile.getConfig();

        // CONFIG
        prefix = config.getString("general.prefix", "&#222DF5&lMaxTools &8» ");
        evolutionLoreBasePath = config.isConfigurationSection("evolution-lore")
                ? "evolution-lore"
                : "general.evolution-lore";
        evolutionLoreEnabled = config.getBoolean(evolutionLoreBasePath + ".enabled", true);
        evolutionLoreLineFormat = getLocalizedConfigString(evolutionLoreBasePath + ".line-format",
                config.getString(evolutionLoreBasePath + ".line-format", "&7✦ &b{enchant} {level}"));
        updateCheckEnabled = config.getBoolean("general.update-check", true);
        bStatsEnabled = config.getBoolean("general.bstats", true);

        // MENSAJES
        msgNoPermission = lang.getString("messages.no-permission", "&cYou do not have permission.");
        msgPluginReload = lang.getString("messages.plugin-reload", "&aConfiguration successfully reloaded.");
        msgMilestoneReached = lang.getString("messages.milestone-reached",
                "&aYour tool reached &e%blocks%&a blocks and gained &e%enchant% %level%&a.");
        msgSpecialUnlocked = lang.getString("messages.special-unlocked",
                "&6Your tool unlocked a special ability: &e%ability%&6.");
        msgToolInfo = lang.getString("messages.tool-info",
                "&7Tool blocks mined: &e%usage% &8| &7Special: &e%special% &8| &7Abilities: &e%abilities%");
        msgOnlyPlayers = lang.getString("messages.only-players", "&cOnly players can use this command.");
        msgInvalidTool = lang.getString("messages.invalid-tool", "&cHold a valid tool in your hand.");
        msgEnabledWord = lang.getString("messages.enabled-word", "Enabled");
        msgDisabledWord = lang.getString("messages.disabled-word", "Disabled");
        msgCommandUsage = lang.getString("messages.command-usage",
                "&cUsage: /maxtools <reload|toolinfo [gui|text]|menu|preview|discordtest|dctest|testtool|cleartesttool|admintoolsremove|lang>");
        msgUpdateAvailable = lang.getString("messages.update-available",
                "&f&lNEW VERSION: &7{version}");
        msgUpdateCurrent = lang.getString("messages.update-current",
                "&7Your current version: &c{version}");
        msgUpdateDownload = lang.getString("messages.update-download",
                "&eDownload it to get improvements and fixes.");
        msgDiscordNotifierNotInitialized = lang.getString("messages.discord-notifier-not-initialized",
                "&cDiscord notifier is not initialized.");
        msgDiscordTestQueued = lang.getString("messages.discord-test-queued", "&aDiscord test message queued.");
        msgDiscordTestFailed = lang.getString("messages.discord-test-failed",
                "&cCould not queue Discord test message. Check discord.enabled and webhook-url.");
        msgDiscordTestConsoleToolName = lang.getString("messages.discord-test-console-tool-name", "Console");
        msgDiscordTestNoToolName = lang.getString("messages.discord-test-no-tool-name", "No Tool");
        msgClearTestToolUsage = lang.getString("messages.clear-testtool-usage",
                "&cUsage: /met cleartesttool [id]");
        msgClearTestToolNotTest = lang.getString("messages.clear-testtool-not-test",
                "&eThe held tool is not a test tool (test_mode). Nothing was modified.");
        msgClearTestToolSuccess = lang.getString("messages.clear-testtool-success",
                "&aTest mode removed from the held tool.");
        msgClearTestToolDestroyed = lang.getString("messages.clear-testtool-destroyed",
                "&aTest tool {id} removed from your main hand.");
        msgClearTestToolIdNotFound = lang.getString("messages.clear-testtool-id-not-found",
                "&cNo registered test tool exists with ID &e{id}&c.");
        msgClearTestToolIdRemoved = lang.getString("messages.clear-testtool-id-removed",
                "&aTest tool ID &e{id}&a removed from the registry.");
        msgCustomToolRevoked = lang.getString("messages.custom-tool-revoked",
                "&cThis MaxTools tool is no longer registered and was removed.");
        msgAdminToolsRemoveUsage = lang.getString("messages.admintoolsremove-usage",
                "&cUse: /met admintoolsremove confirm");
        msgAdminToolsRemoveStarted = lang.getString("messages.admintoolsremove-started",
                "&eScanning loaded inventories and clearing the custom tool registry...");
        msgAdminToolsRemoveSuccess = lang.getString("messages.admintoolsremove-success",
                "&aCustom tool purge completed. Removed {removed} loaded item(s) and cleared {database} database record(s).");
        msgAdminToolsRemoveDatabaseUnavailable = lang.getString("messages.admintoolsremove-database-unavailable",
                "&cThe custom tool database is not available. Physical loaded items were still scanned.");
        msgLanguageChanged = lang.getString("messages.language-changed", "&aLanguage updated successfully.");
        msgLanguageAlreadySelected = lang.getString("messages.language-already-selected",
                "&eThat language is already active.");
        msgSyncNoChanges = lang.getString("messages.sync-no-changes",
                "&aHeld tool is already synchronized with evolution.yml.");
        msgSyncSuccess = lang.getString("messages.sync-success",
                "&aHeld tool synchronized. Milestones applied: {milestones}. Abilities added: {abilities}.");
        menuTitle = getMenuString("menu-texts.evolution-hub.title",
                lang.getString("messages.menu-title", "&8Tool Evolution Hub"));
        menuToolItemTitle = getMenuString("menu-texts.evolution-hub.tool-item.title",
                lang.getString("messages.menu-tool-item-title", "&b{tool_name}"));
        menuToolItemLore = getMenuStringList("menu-texts.evolution-hub.tool-item.lore",
                lang.getStringList("messages.menu-tool-item-lore"));
        toolInfoGuiDefault = config.getBoolean("general.toolinfo-gui-default", false);
        toolStatsMilestoneBarLength = Math.max(4,
                getMenuInt("menu-layouts.tool-stats.progress-bars.milestones.length",
                        config.getInt("menu-layouts.tool-stats.progress-bars.milestones.length", 12)));
        toolStatsMilestoneFilledChar = getMenuString("menu-layouts.tool-stats.progress-bars.milestones.filled-char",
                "&a█");
        toolStatsMilestoneEmptyChar = getMenuString("menu-layouts.tool-stats.progress-bars.milestones.empty-char",
                "&7░");
        toolStatsAbilityBarLength = Math.max(4,
                getMenuInt("menu-layouts.tool-stats.progress-bars.abilities.length",
                        config.getInt("menu-layouts.tool-stats.progress-bars.abilities.length", 12)));
        toolStatsAbilityFilledChar = getMenuString("menu-layouts.tool-stats.progress-bars.abilities.filled-char",
                "&b█");
        toolStatsAbilityEmptyChar = getMenuString("menu-layouts.tool-stats.progress-bars.abilities.empty-char",
                "&7░");
        discordEnabled = config.getBoolean("discord.enabled", false);
        discordWebhookUrl = config.getString("discord.webhook-url", "");
        discordServerName = config.getString("discord.server-name", plugin.getName());
        discordMilestoneEnabled = config.getBoolean("discord.events.milestone-unlocked", true);
        discordAbilityEnabled = config.getBoolean("discord.events.ability-unlocked", true);
        discordMilestoneTemplate = getLocalizedConfigString("discord.templates.milestone-unlocked",
                ":pick: **{player}** unlocked a milestone with **{tool}** at **{blocks}** blocks (`{timestamp}`) — reward: **{ability}**");
        discordAbilityTemplate = getLocalizedConfigString("discord.templates.ability-unlocked",
                ":sparkles: **{player}** unlocked ability **{ability}** on **{tool}** at **{blocks}** blocks (`{timestamp}`)");
        discordTestTemplate = getLocalizedConfigString("discord.templates.test-message",
                ":satellite: Test message from **{player}** with **{tool}** at **{blocks}** blocks (`{timestamp}`)");
        discordQueueMaxPending = Math.max(16, config.getInt("discord.queue.max-pending", 256));
        if (menuToolItemLore == null || menuToolItemLore.isEmpty()) {
            menuToolItemLore = List.of(
                    "&7Current level: &f{current_level}",
                    "&7Next level: &f{next_level}",
                    "&7Usage: &f{usage}",
                    "&7Target: &f{target}",
                    "&7Milestones: &f{unlocked_milestones}&7/&f{total_milestones}",
                    "&7Abilities: &f{unlocked_abilities}&7/&f{total_abilities}");
        }
        fallbackProgressUnit = lang.getString("messages.progress-unit", "Blocks");
        toolNames = loadToolNames(lang);
        if (!"en".equalsIgnoreCase(selectedLanguage)) {
            fallbackToolNames = loadToolNames(loadLangConfig("en"));
        } else {
            fallbackToolNames = Map.of();
        }
        enchantmentNames = new HashMap<>();
        fallbackEnchantmentNames = new HashMap<>();
        loadEnchantmentNames(lang, enchantmentNames);
        if (!"en".equalsIgnoreCase(selectedLanguage)) {
            loadEnchantmentNames(loadLangConfig("en"), fallbackEnchantmentNames);
        }
        validateConfigPlaceholders();

        msgTestToolUsage = lang.getString("messages.testtool-usage",
                "&cUse: /met testtool <material> [ability|all] [level]");
        msgTestToolInvalidMaterial = lang.getString("messages.testtool-invalid-material",
                "&cInvalid material or not compatible. Use picks/axes/shovels from tracked-tools.");
        msgTestToolMetadainvalid = lang.getString("messages.testtool-metadainvalid",
                "&cCould not create metadata for that material.");
        msgTestToolLevelinvaid = lang.getString("messages.testtool-level", "&c[level] must be a number >= 0.");
        msgTestToolLevelnumberinvalid = lang.getString("messages.testtool-levelnumber",
                "&c[level] must be numeric.");
        msgTestToolAbilityinvalid = lang.getString("messages.testtool-invalid-ability",
                "&cInvalid or disabled ability: &e");
        msgTestToolCreated = lang.getString("messages.testtool-created",
                "&aTest tool created: &e{material}&a | blocks: &e{usage}&a | abilities: &e{abilities}");
        msgTestToolNoAbilities = lang.getString("messages.testtool-no-abilities", "none");

    }

    public void reloadConfig() {
        configFile.reloadConfig();
        configFile.updateConfig();
        evolutionFile.reloadConfig();
        evolutionFile.updateConfig();
        menusFile.reloadConfig();
        menusFile.updateConfig();
        if (langFile != null) {
            langFile.reloadConfig();
            langFile.updateConfig();
        }
        loadConfig();
    }

    // GETTERS
    public String getPrefix() {
        return prefix;
    }

    public String getMsgTestToolUsage() {
        return msgTestToolUsage;
    }

    public String getMsgTestToolInvalidMaterial() {
        return msgTestToolInvalidMaterial;
    }

    public String getMsgTestToolMetadainvalid() {
        return msgTestToolMetadainvalid;
    }

    public String getMsgTestToolLevelinvaid() {
        return msgTestToolLevelinvaid;
    }

    public String getMsgTestToolLevelnumberinvalid() {
        return msgTestToolLevelnumberinvalid;
    }

    public String getMsgTestToolAbilityinvalid() {
        return msgTestToolAbilityinvalid;
    }

    public String getMsgTestToolCreated() {
        return msgTestToolCreated;
    }

    public String getMsgTestToolNoAbilities() {
        return msgTestToolNoAbilities;
    }

    public boolean isUpdateCheckEnabled() {
        return updateCheckEnabled;
    }

    public boolean isBStatsEnabled() {
        return bStatsEnabled;
    }

    public String getMsgNoPermission() {
        return msgNoPermission;
    }

    public String getMsgPluginReload() {
        return msgPluginReload;
    }

    public String getMsgMilestoneReached() {
        return msgMilestoneReached;
    }

    public String getMsgSpecialUnlocked() {
        return msgSpecialUnlocked;
    }

    public String getMsgToolInfo() {
        return msgToolInfo;
    }

    public String getMsgOnlyPlayers() {
        return msgOnlyPlayers;
    }

    public String getMsgInvalidTool() {
        return msgInvalidTool;
    }

    public String getMsgEnabledWord() {
        return msgEnabledWord;
    }

    public String getMsgDisabledWord() {
        return msgDisabledWord;
    }

    public String getMsgCommandUsage() {
        return msgCommandUsage;
    }

    public String getMsgUpdateAvailable() {
        return msgUpdateAvailable;
    }

    public String getMsgUpdateCurrent() {
        return msgUpdateCurrent;
    }

    public String getMsgUpdateDownload() {
        return msgUpdateDownload;
    }

    public String getMsgDiscordNotifierNotInitialized() {
        return msgDiscordNotifierNotInitialized;
    }

    public String getMsgDiscordTestQueued() {
        return msgDiscordTestQueued;
    }

    public String getMsgDiscordTestFailed() {
        return msgDiscordTestFailed;
    }

    public String getMsgDiscordTestConsoleToolName() {
        return msgDiscordTestConsoleToolName;
    }

    public String getMsgDiscordTestNoToolName() {
        return msgDiscordTestNoToolName;
    }

    public String getMsgClearTestToolUsage() {
        return msgClearTestToolUsage;
    }

    public String getMsgClearTestToolNotTest() {
        return msgClearTestToolNotTest;
    }

    public String getMsgClearTestToolSuccess() {
        return msgClearTestToolSuccess;
    }

    public String getMsgClearTestToolDestroyed() {
        return msgClearTestToolDestroyed;
    }

    public String getMsgClearTestToolIdNotFound() {
        return msgClearTestToolIdNotFound;
    }

    public String getMsgClearTestToolIdRemoved() {
        return msgClearTestToolIdRemoved;
    }

    public String getMsgCustomToolRevoked() {
        return msgCustomToolRevoked;
    }

    public String getMsgAdminToolsRemoveUsage() {
        return msgAdminToolsRemoveUsage;
    }

    public String getMsgAdminToolsRemoveStarted() {
        return msgAdminToolsRemoveStarted;
    }

    public String getMsgAdminToolsRemoveSuccess() {
        return msgAdminToolsRemoveSuccess;
    }

    public String getMsgAdminToolsRemoveDatabaseUnavailable() {
        return msgAdminToolsRemoveDatabaseUnavailable;
    }

    public String getMsgLanguageChanged() {
        return msgLanguageChanged;
    }

    public String getMsgLanguageAlreadySelected() {
        return msgLanguageAlreadySelected;
    }

    public String getMsgSyncNoChanges() {
        return msgSyncNoChanges;
    }

    public String getMsgSyncSuccess() {
        return msgSyncSuccess;
    }

    public String getMenuTitle() {
        return menuTitle;
    }

    public String getMenuToolItemTitle() {
        return menuToolItemTitle;
    }

    public List<String> getMenuToolItemLore() {
        return List.copyOf(menuToolItemLore);
    }

    public boolean isToolInfoGuiDefault() {
        return toolInfoGuiDefault;
    }

    public int getToolStatsMilestoneBarLength() {
        return toolStatsMilestoneBarLength;
    }

    public String getToolStatsMilestoneFilledChar() {
        return toolStatsMilestoneFilledChar;
    }

    public String getToolStatsMilestoneEmptyChar() {
        return toolStatsMilestoneEmptyChar;
    }

    public int getToolStatsAbilityBarLength() {
        return toolStatsAbilityBarLength;
    }

    public String getToolStatsAbilityFilledChar() {
        return toolStatsAbilityFilledChar;
    }

    public String getToolStatsAbilityEmptyChar() {
        return toolStatsAbilityEmptyChar;
    }

    public boolean isDiscordEnabled() {
        return discordEnabled;
    }

    public String getDiscordWebhookUrl() {
        return discordWebhookUrl == null ? "" : discordWebhookUrl;
    }

    public String getDiscordServerName() {
        return discordServerName == null || discordServerName.isBlank() ? plugin.getName() : discordServerName;
    }

    public boolean isDiscordMilestoneEnabled() {
        return discordMilestoneEnabled;
    }

    public boolean isDiscordAbilityEnabled() {
        return discordAbilityEnabled;
    }

    public String getDiscordMilestoneTemplate() {
        return discordMilestoneTemplate;
    }

    public String getDiscordAbilityTemplate() {
        return discordAbilityTemplate;
    }

    public String getDiscordTestTemplate() {
        return discordTestTemplate;
    }

    public int getDiscordQueueMaxPending() {
        return discordQueueMaxPending;
    }

    public boolean isEvolutionLoreEnabled() {
        return evolutionLoreEnabled;
    }

    public String getEvolutionLoreLineFormat() {
        return evolutionLoreLineFormat;
    }

    public FileConfiguration getEvolutionConfig() {
        return evolutionFile.getConfig();
    }

    public String getEnchantmentName(String enchantmentKey) {
        if (enchantmentKey == null) {
            return "";
        }
        String key = enchantmentKey.toLowerCase(Locale.ROOT);
        String localized = enchantmentNames.get(key);
        if (localized != null && !localized.isBlank()) {
            return localized;
        }

        String fallback = fallbackEnchantmentNames.get(key);
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }

        return formatConfigKey(enchantmentKey);
    }

    public String getToolName(Material material) {
        if (material == null) {
            return "";
        }
        String materialKey = material.name().toLowerCase(Locale.ROOT);

        String localized = toolNames.get(materialKey);
        if (localized != null && !localized.isBlank()) {
            return localized;
        }

        String fallbackName = fallbackToolNames.get(materialKey);
        if (fallbackName != null && !fallbackName.isBlank()) {
            return fallbackName;
        }

        return formatMaterialName(material);
    }

    public String getProgressUnit() {
        return fallbackProgressUnit;
    }

    public FileConfiguration getMainConfig() {
        return configFile.getConfig();
    }

    public boolean setLanguage(String languageCode) {
        String normalized = languageCode == null ? "" : languageCode.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank() || (!"en".equals(normalized) && !"es".equals(normalized))) {
            return false;
        }

        if (normalized.equalsIgnoreCase(selectedLanguage)) {
            return false;
        }

        if (languageProfileSyncEnabled && !applyLanguageProfile(normalized)) {
            plugin.getLogger().warning("Could not apply language profile '" + normalized
                    + "'. Keeping current config files untouched.");
            return false;
        }

        configFile.getConfig().set("general.language", normalized);
        configFile.saveConfig();
        loadConfig();
        return true;
    }

    public boolean isSpanishLanguage() {
        return "es".equalsIgnoreCase(selectedLanguage);
    }

    public String getEvolutionLoreBasePath() {
        return evolutionLoreBasePath;
    }

    public String getGuiString(String path, String fallback) {
        return getMenuString(path, configFile.getConfig().getString(path, fallback));
    }

    public String getGuiString(String path) {
        String menuValue = getLocalizedMenusConfig().getString(path);
        if (menuValue != null) {
            return menuValue;
        }
        return configFile.getConfig().getString(path);
    }

    public int getGuiInt(String path, int fallback) {
        return getMenuInt(path, configFile.getConfig().getInt(path, fallback));
    }

    public int getGuiInventorySize(String path, int fallback) {
        int configured = getGuiInt(path, fallback);
        int size = Math.max(9, Math.min(54, configured));
        return size % 9 == 0 ? size : ((size / 9) + 1) * 9;
    }

    public boolean getGuiBoolean(String path, boolean fallback) {
        FileConfiguration menuConfig = getLocalizedMenusConfig();
        if (menuConfig.contains(path)) {
            return menuConfig.getBoolean(path, fallback);
        }
        return configFile.getConfig().getBoolean(path, fallback);
    }

    public List<String> getGuiStringList(String path) {
        List<String> lines = getMenuStringList(path, configFile.getConfig().getStringList(path));
        return lines == null ? List.of() : List.copyOf(lines);
    }

    public List<String> getGuiStringList(String path, List<String> fallback) {
        List<String> lines = getMenuStringList(path, configFile.getConfig().getStringList(path));
        if (lines == null || lines.isEmpty()) {
            return fallback == null ? List.of() : List.copyOf(fallback);
        }
        return List.copyOf(lines);
    }

    @SuppressWarnings("null")
    public List<Integer> getGuiSlots(String path) {
        List<Integer> slots = getMenuIntegerList(path, configFile.getConfig().getIntegerList(path));
        if (slots == null) {
            return List.of();
        }
        return slots.stream()
                .filter(Objects::nonNull)
                .filter(slot -> slot >= 0)
                .distinct()
                .sorted()
                .toList();
    }

    public List<Integer> getGuiIntegerList(String path, List<Integer> fallback) {
        List<Integer> values = getMenuIntegerList(path, configFile.getConfig().getIntegerList(path));
        if (values == null || values.isEmpty()) {
            return fallback == null ? List.of() : List.copyOf(fallback);
        }
        return List.copyOf(values);
    }

    public boolean isSlotConfigured(String path, int slot, int fallbackSlot) {
        int singleSlot = getGuiInt(path + ".slot", fallbackSlot);
        return slot == singleSlot || getGuiSlots(path + ".slots").contains(slot);
    }

    public Material getGuiMaterial(String path, Material fallback) {
        String raw = getGuiString(path, "");
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    @SuppressWarnings("removal")
    public Sound getGuiSound(String path, Sound fallback) {
        String raw = getGuiString(path, "");
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Sound.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    public float getGuiFloat(String path, float fallback) {
        FileConfiguration menuConfig = getLocalizedMenusConfig();
        if (menuConfig.contains(path)) {
            return (float) menuConfig.getDouble(path, fallback);
        }
        return (float) configFile.getConfig().getDouble(path, fallback);
    }

    public String getEvolutionLoreStateLabel(String state, String fallback) {
        return getLocalizedConfigString(evolutionLoreBasePath + ".states." + state,
                getGuiString(evolutionLoreBasePath + ".states." + state, fallback));
    }

    public String getEvolutionLoreProgressFormat(String fallback) {
        return getLocalizedConfigString(evolutionLoreBasePath + ".sections.progress.format",
                getGuiString(evolutionLoreBasePath + ".sections.progress.format", fallback));
    }

    public int getEvolutionLoreProgressBarLength(int fallback) {
        return Math.max(4, getGuiInt(evolutionLoreBasePath + ".sections.progress.bar-length", fallback));
    }

    public String getEvolutionLoreFilledChar(String fallback) {
        return getLocalizedConfigString(evolutionLoreBasePath + ".sections.progress.filled-char",
                getGuiString(evolutionLoreBasePath + ".sections.progress.filled-char", fallback));
    }

    public String getEvolutionLoreEmptyChar(String fallback) {
        return getLocalizedConfigString(evolutionLoreBasePath + ".sections.progress.empty-char",
                getGuiString(evolutionLoreBasePath + ".sections.progress.empty-char", fallback));
    }

    private FileConfiguration loadLangConfig(String localeCode) {
        String fileName = "messages_" + localeCode.toLowerCase(Locale.ROOT).trim() + ".yml";
        String resourcePath = "lang/" + fileName;
        if (plugin.getResource(resourcePath) == null) {
            return null;
        }

        CustomConfig customConfig = new CustomConfig(fileName, "lang", plugin, false);
        customConfig.registerConfig();
        return customConfig.getConfig();
    }

    private Map<String, String> loadToolNames(FileConfiguration config) {
        if (config == null) {
            return Map.of();
        }

        Map<String, String> localizedNames = new LinkedHashMap<>();
        if (config.isConfigurationSection("tool-names")) {
            for (String key : config.getConfigurationSection("tool-names").getKeys(false)) {
                String value = config.getString("tool-names." + key, "").trim();
                if (!value.isBlank()) {
                    localizedNames.put(key.toLowerCase(Locale.ROOT), value);
                }
            }
        }
        return localizedNames;
    }

    private void loadEnchantmentNames(FileConfiguration config, Map<String, String> target) {
        if (config == null || target == null) {
            return;
        }

        if (config.isConfigurationSection("enchantments")) {
            for (String key : config.getConfigurationSection("enchantments").getKeys(false)) {
                String value = config.getString("enchantments." + key, "").trim();
                if (!value.isBlank()) {
                    target.put(key.toLowerCase(Locale.ROOT), value);
                }
            }
        }
    }

    private String formatConfigKey(String key) {
        StringBuilder builder = new StringBuilder();
        for (String part : key.split("[_\\-\\s]+")) {
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

    private String formatMaterialName(Material material) {
        StringBuilder builder = new StringBuilder();
        for (String part : material.name().split("_")) {
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

    private void validateConfigPlaceholders() {
        FileConfiguration menuConfig = getLocalizedMenusConfig();
        validateLorePlaceholders(menuConfig,
                "menu-layouts.admin-preview.items.progress-bars.lore",
                Set.of("{unit}"),
                Set.of("{sample_1_bar}", "{bar_25}"),
                Set.of("{sample_2_bar}", "{bar_60}"),
                Set.of("{sample_3_bar}", "{bar_95}"));
        validateLorePlaceholders(menuConfig,
                "menu-layouts.admin-preview.items.ability-states.lore",
                Set.of("{self_repair}", "{auto_smelt}", "{telepathy}", "{active}", "{cooldown}", "{blocked}",
                        "{cooldown_seconds}"));
        validateLorePlaceholders(menuConfig,
                "menu-layouts.admin-preview.items.evolution-lore.lore",
                Set.of("{progress_line}", "{enchant_line}"));
    }

    @SafeVarargs
    private void validateLorePlaceholders(FileConfiguration config, String path, Set<String>... requiredGroups) {
        List<String> lines = config.getStringList(path);
        if (lines == null || lines.isEmpty()) {
            plugin.getLogger().warning("Config path '" + path
                    + "' is empty. Preview item may render without dynamic values.");
            return;
        }

        String joined = String.join("\n", lines);
        for (Set<String> group : requiredGroups) {
            boolean found = group.stream().anyMatch(joined::contains);
            if (!found) {
                plugin.getLogger().warning("Config path '" + path
                        + "' is missing required placeholder(s): " + String.join(" or ", group)
                        + ". Please update your lore template.");
            }
        }
    }

    private String getMenuString(String path, String fallback) {
        return getLocalizedMenusConfig().getString(path, fallback);
    }

    private int getMenuInt(String path, int fallback) {
        return getLocalizedMenusConfig().getInt(path, fallback);
    }

    private List<String> getMenuStringList(String path, List<String> fallback) {
        List<String> menuValues = getLocalizedMenusConfig().getStringList(path);
        if (menuValues != null && !menuValues.isEmpty()) {
            return menuValues;
        }
        return fallback;
    }

    private List<Integer> getMenuIntegerList(String path, List<Integer> fallback) {
        List<Integer> menuValues = getLocalizedMenusConfig().getIntegerList(path);
        if (menuValues != null && !menuValues.isEmpty()) {
            return menuValues;
        }
        return fallback;
    }

    private FileConfiguration getLocalizedMenusConfig() {
        return menusFile.getConfig();
    }

    private String getLocalizedConfigString(String path, String fallback) {
        String value = configFile.getConfig().getString(path);
        return value == null ? fallback : value;
    }

    private void ensureDefaultLanguageProfiles() {
        try {
            Path dataFolder = plugin.getDataFolder().toPath();
            boolean created = false;

            created |= copyFileIfAbsent(dataFolder.resolve("config.yml"), getProfilePath("config", "en"));
            created |= copyFileIfAbsent(dataFolder.resolve("menus.yml"), getProfilePath("menus", "en"));
            created |= copyResourceToFileIfAbsent("extra_lang/config_es.yml", getProfilePath("config", "es"));
            created |= copyResourceToFileIfAbsent("extra_lang/menus_es.yml", getProfilePath("menus", "es"));

            if (created) {
                plugin.getLogger().info("Initial language profiles generated in '"
                        + LANGUAGE_PROFILES_FOLDER
                        + "'. Existing language profile files were not overwritten.");
            }
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed generating initial language profiles: " + ex.getMessage());
        }
    }

    private boolean applyLanguageProfile(String languageCode) {
        String normalized = languageCode.toLowerCase(Locale.ROOT);

        try {
            snapshotCurrentLanguageProfile();
            applyTemplateOrProfile(normalized, "config");
            applyTemplateOrProfile(normalized, "menus");
            refreshRuntimeDefaults();
            return true;
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed applying language profile '" + normalized + "': " + ex.getMessage());
            return false;
        }
    }

    private void snapshotCurrentLanguageProfile() throws IOException {
        if (selectedLanguage == null || selectedLanguage.isBlank()) {
            return;
        }
        String current = selectedLanguage.toLowerCase(Locale.ROOT).trim();
        copyFile(plugin.getDataFolder().toPath().resolve("config.yml"), getProfilePath("config", current));
        copyFile(plugin.getDataFolder().toPath().resolve("menus.yml"), getProfilePath("menus", current));
    }

    private void applyTemplateOrProfile(String languageCode, String baseName) throws IOException {
        Path profilePath = getProfilePath(baseName, languageCode);
        Path activePath = plugin.getDataFolder().toPath().resolve(baseName + ".yml");

        if (!Files.exists(profilePath)) {
            copyResourceToFile(getLanguageTemplateResourcePath(languageCode, baseName), profilePath);
        }

        copyFile(profilePath, activePath);
    }

    private String getLanguageTemplateResourcePath(String languageCode, String baseName) throws IOException {
        return switch (baseName) {
            case "config" -> "en".equals(languageCode) ? "config.yml" : "extra_lang/config_es.yml";
            case "menus" -> "en".equals(languageCode) ? "menus.yml" : "extra_lang/menus_es.yml";
            default -> throw new IOException("Unsupported language profile base: " + baseName);
        };
    }

    private Path getProfilePath(String baseName, String languageCode) {
        return plugin.getDataFolder().toPath()
                .resolve(LANGUAGE_PROFILES_FOLDER)
                .resolve(baseName + "_" + languageCode + ".yml");
    }

    private boolean copyResourceToFileIfAbsent(String resourcePath, Path targetPath) throws IOException {
        if (Files.exists(targetPath)) {
            return false;
        }

        InputStream resourceStream = plugin.getResource(Objects.requireNonNull(resourcePath));
        if (resourceStream == null) {
            throw new IOException("Missing resource: " + resourcePath);
        }

        Files.createDirectories(targetPath.getParent());
        try (InputStream in = resourceStream) {
            Files.copy(in, targetPath);
        }
        return true;
    }

    private boolean copyFileIfAbsent(Path sourcePath, Path targetPath) throws IOException {
        if (Files.exists(targetPath)) {
            return false;
        }
        if (!Files.exists(sourcePath)) {
            throw new IOException("Missing source file: " + sourcePath.getFileName());
        }

        Files.createDirectories(targetPath.getParent());
        Files.copy(sourcePath, targetPath);
        return true;
    }

    private void copyResourceToFile(String resourcePath, Path targetPath) throws IOException {
        InputStream resourceStream = plugin.getResource(Objects.requireNonNull(resourcePath));
        if (resourceStream == null) {
            throw new IOException("Missing resource: " + resourcePath);
        }
        Files.createDirectories(targetPath.getParent());
        try (InputStream in = resourceStream) {
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void copyFile(Path sourcePath, Path targetPath) throws IOException {
        if (!Files.exists(sourcePath)) {
            throw new IOException("Missing source file: " + sourcePath.getFileName());
        }
        Files.createDirectories(targetPath.getParent());
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private void refreshRuntimeDefaults() {
        configFile.reloadConfig();
        configFile.updateConfig();
        menusFile.reloadConfig();
        menusFile.updateConfig();
    }
}

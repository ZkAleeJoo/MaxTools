package org.zkaleejoo;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bstats.bukkit.Metrics;
import org.zkaleejoo.commands.MainCommand;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.evolution.AbilityTaskManager;
import org.zkaleejoo.evolution.PlayerPlacedBlockTracker;
import org.zkaleejoo.evolution.ToolEvolutionManager;
import org.zkaleejoo.listeners.PlayerJoinListener;
import org.zkaleejoo.listeners.ToolEvolutionListener;
import org.zkaleejoo.utils.DiscordWebhookNotifier;
import org.zkaleejoo.utils.MessageUtils;
import org.zkaleejoo.utils.TestToolRegistry;
import org.zkaleejoo.utils.UpdateChecker;
import org.zkaleejoo.gui.EvolutionMenuService;
import org.zkaleejoo.gui.LanguageMenuService;
import org.zkaleejoo.storage.CustomToolDatabase;

public final class MaxTools extends JavaPlugin {

    private static final int BSTATS_PLUGIN_ID = 31238;
    private static final long UPDATE_CHECK_INTERVAL_TICKS = 20L * 60L * 60L * 5L;
    private static final String IMPORTANT_FILE_NAME = "IMPORTANT.txt";
    public static final String UPDATE_DOWNLOAD_URL = "https://modrinth.com/plugin/maxtools";

    private MainConfigManager mainConfigManager;
    private ToolEvolutionManager toolEvolutionManager;
    private String latestVersion;
    private EvolutionMenuService evolutionMenuService;
    private AbilityTaskManager abilityTaskManager;
    private DiscordWebhookNotifier discordWebhookNotifier;
    private LanguageMenuService languageMenuService;
    private TestToolRegistry testToolRegistry;
    private PlayerPlacedBlockTracker playerPlacedBlockTracker;
    private CustomToolDatabase customToolDatabase;
    private BukkitTask updateCheckTask;
    private BukkitTask placedBlockSaveTask;
    private Metrics metrics;

    @Override
    public void onEnable() {
        saveImportantFile();

        mainConfigManager = new MainConfigManager(this);
        syncMetricsState();
        customToolDatabase = new CustomToolDatabase(this);
        customToolDatabase.initialize();
        playerPlacedBlockTracker = new PlayerPlacedBlockTracker(this);
        toolEvolutionManager = new ToolEvolutionManager(this, playerPlacedBlockTracker);
        toolEvolutionManager.reload();
        testToolRegistry = new TestToolRegistry(this);
        abilityTaskManager = new AbilityTaskManager(this, toolEvolutionManager);
        evolutionMenuService = new EvolutionMenuService(this);
        languageMenuService = new LanguageMenuService(this);
        recreateDiscordWebhookNotifier();

        MainCommand mainCommand = new MainCommand(this);
        PluginCommand maxToolsCommand = getCommand("maxtools");
        if (maxToolsCommand != null) {
            maxToolsCommand.setExecutor(mainCommand);
            maxToolsCommand.setTabCompleter(mainCommand);
        } else {
            getLogger().severe("Command maxtools is not defined in plugin.yml");
        }

        getServer().getPluginManager()
                .registerEvents(new ToolEvolutionListener(this, toolEvolutionManager, playerPlacedBlockTracker), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(evolutionMenuService, this);
        getServer().getPluginManager().registerEvents(languageMenuService, this);
        abilityTaskManager.start();
        startPlacedBlockAutosave();

        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(
                "&9&lMaxTools &8» &9   _____                ___________           .__          "));
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(
                "&9&lMaxTools &8» &9  /     \\ _____  ___  __\\__    ___/___   ____ |  |   ______"));
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(
                "&9&lMaxTools &8» &9 /  \\ /  \\\\__  \\ \\  \\/  / |    | /  _ \\ /  _ \\|  |  /  ___/"));
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(
                "&9&lMaxTools &8» &9/    Y    \\/ __ \\_>    <  |    |(  <_> |  <_> )  |__\\___ \\ "));
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(
                "&9&lMaxTools &8» &9\\____|__  (____  /__/\\_ \\ |____| \\____/ \\____/|____/____  >"));
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(
                "&9&lMaxTools &8» &9        \\/     \\/      \\/                               \\/ "));

        Bukkit.getConsoleSender()
                .sendMessage(
                        MessageUtils.getColoredMessage("&9&lMaxTools &8» &9The plugin has been enabled!"));

        startUpdateChecks();
    }

    @Override
    public void onDisable() {
        if (abilityTaskManager != null) {
            abilityTaskManager.stop();
        }
        if (evolutionMenuService != null) {
            evolutionMenuService.resetActiveMenuSessions();
        }
        if (discordWebhookNotifier != null) {
            discordWebhookNotifier.shutdown();
        }
        if (updateCheckTask != null) {
            updateCheckTask.cancel();
            updateCheckTask = null;
        }
        if (metrics != null) {
            metrics.shutdown();
            metrics = null;
        }
        if (placedBlockSaveTask != null) {
            placedBlockSaveTask.cancel();
            placedBlockSaveTask = null;
        }
        if (playerPlacedBlockTracker != null) {
            playerPlacedBlockTracker.saveIfDirty();
        }
        if (customToolDatabase != null) {
            customToolDatabase.close();
        }
    }

    public MainConfigManager getConfigManager() {
        return mainConfigManager;
    }

    public ToolEvolutionManager getToolEvolutionManager() {
        return toolEvolutionManager;
    }

    public EvolutionMenuService getEvolutionMenuService() {
        return evolutionMenuService;
    }

    public DiscordWebhookNotifier getDiscordWebhookNotifier() {
        return discordWebhookNotifier;
    }

    public LanguageMenuService getLanguageMenuService() {
        return languageMenuService;
    }

    public TestToolRegistry getTestToolRegistry() {
        return testToolRegistry;
    }

    public CustomToolDatabase getCustomToolDatabase() {
        return customToolDatabase;
    }

    public void recreateDiscordWebhookNotifier() {
        if (discordWebhookNotifier != null) {
            discordWebhookNotifier.shutdown();
        }
        discordWebhookNotifier = new DiscordWebhookNotifier(this, mainConfigManager.getDiscordQueueMaxPending());
    }

    private void saveImportantFile() {
        File importantFile = new File(getDataFolder(), IMPORTANT_FILE_NAME);
        if (importantFile.exists()) {
            return;
        }

        saveResource(IMPORTANT_FILE_NAME, false);
    }

    private void checkUpdates() {
        if (!getMainConfigManager().isUpdateCheckEnabled())
            return;

        new UpdateChecker(this).getVersion(version -> {
            if (this.getPluginMeta().getVersion().equalsIgnoreCase(version)) {
                this.latestVersion = null;
                Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(
                        "&9&lMaxTools &8» &aA check for updates was performed and nothing was found."));
            } else {
                this.latestVersion = version;

                Bukkit.getConsoleSender()
                        .sendMessage(MessageUtils
                                .getColoredMessage("&9&lMaxTools &8» &f&lNEW VERSION &7" + version));
                Bukkit.getConsoleSender().sendMessage(
                        MessageUtils
                                .getColoredMessage(
                                        "&9&lMaxTools &8» &fDownload it now at the following link: &7"
                                                + UPDATE_DOWNLOAD_URL));
            }
        });
    }

    private void startUpdateChecks() {
        if (updateCheckTask != null) {
            updateCheckTask.cancel();
            updateCheckTask = null;
        }

        if (!getMainConfigManager().isUpdateCheckEnabled()) {
            latestVersion = null;
            return;
        }

        checkUpdates();
        updateCheckTask = Bukkit.getScheduler().runTaskTimer(this, this::checkUpdates,
                UPDATE_CHECK_INTERVAL_TICKS, UPDATE_CHECK_INTERVAL_TICKS);
    }

    private void startPlacedBlockAutosave() {
        if (placedBlockSaveTask != null) {
            placedBlockSaveTask.cancel();
            placedBlockSaveTask = null;
        }

        placedBlockSaveTask = Bukkit.getScheduler().runTaskTimer(this,
                () -> playerPlacedBlockTracker.saveIfDirty(),
                20L * 60L * 5L,
                20L * 60L * 5L);
    }

    private void syncMetricsState() {
        if (getMainConfigManager().isBStatsEnabled()) {
            if (metrics == null) {
                metrics = new Metrics(this, BSTATS_PLUGIN_ID);
            }
            return;
        }

        if (metrics != null) {
            metrics.shutdown();
            metrics = null;
        }
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public MainConfigManager getMainConfigManager() {
        return mainConfigManager;
    }

    public void reloadRuntimeState() {
        syncMetricsState();
        startUpdateChecks();
    }
}

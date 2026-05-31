package org.zkaleejoo.gui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.zkaleejoo.MaxTools;
import org.zkaleejoo.evolution.EvolutionMilestone;
import org.zkaleejoo.utils.MetKeys;

@SuppressWarnings("null")
public class EvolutionMenuService implements Listener {

    private static final long PAGE_STATE_TTL_MILLIS = 60_000L;

    private final MaxTools plugin;
    private final MilestoneTreeMenu milestoneTreeMenu;
    private final MilestoneDetailMenu milestoneDetailMenu;
    private final Map<UUID, MenuSession> sessions = new HashMap<>();
    private final AbilitiesMenu abilitiesMenu;
    private final ToolStatsMenu toolStatsMenu;
    private final Map<UUID, BukkitTask> refreshTasks = new HashMap<>();
    private final Map<UUID, PageStateSnapshot> pageStateSnapshots = new HashMap<>();
    private final Set<UUID> previewViewers = new HashSet<>();
    private final AdminPreviewMenu adminPreviewMenu;

    public EvolutionMenuService(MaxTools plugin) {
        this.plugin = plugin;
        this.milestoneTreeMenu = new MilestoneTreeMenu(plugin, plugin.getToolEvolutionManager());
        this.milestoneDetailMenu = new MilestoneDetailMenu(plugin, plugin.getToolEvolutionManager());
        this.abilitiesMenu = new AbilitiesMenu(plugin, plugin.getToolEvolutionManager());
        this.toolStatsMenu = new ToolStatsMenu(plugin, plugin.getToolEvolutionManager());
        this.adminPreviewMenu = new AdminPreviewMenu(plugin, plugin.getToolEvolutionManager());
    }

    public boolean openHub(Player player) {
        ItemStack tool = player.getInventory().getItem(EquipmentSlot.HAND);
        if (!plugin.getToolEvolutionManager().isTrackedTool(tool)) {
            return false;
        }

        int usage = plugin.getToolEvolutionManager().getUsage(tool);
        MenuSession session = new MenuSession(player.getUniqueId(), tool.getType(), usage);
        restorePageState(player.getUniqueId(), tool.getType(), session);
        session.setViewType(MenuSession.ViewType.TREE);
        sessions.put(player.getUniqueId(), session);
        player.openInventory(milestoneTreeMenu.build(player, session));
        playMenuSound(player, MenuSoundEvent.OPEN);
        return true;
    }

    public void openAdminPreview(Player player) {
        previewViewers.add(player.getUniqueId());
        player.openInventory(adminPreviewMenu.build(player));
        playMenuSound(player, MenuSoundEvent.OPEN);
    }

    public void playMenuError(Player player) {
        playMenuSound(player, MenuSoundEvent.ERROR);
    }

    public void resetActiveMenuSessions() {
        for (UUID viewerId : new HashSet<>(previewViewers)) {
            Player player = Bukkit.getPlayer(viewerId);
            if (player != null && player.isOnline() && isPluginMenu(player.getOpenInventory().getTopInventory())) {
                player.closeInventory();
            }
        }

        for (UUID playerId : new HashSet<>(sessions.keySet())) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline() && isPluginMenu(player.getOpenInventory().getTopInventory())) {
                player.closeInventory();
            }
            stopRefreshTask(playerId);
        }

        previewViewers.clear();
        sessions.clear();
        refreshTasks.clear();
        pageStateSnapshots.clear();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory topInventory = event.getView().getTopInventory();
        if (!isPluginMenu(topInventory)) {
            return;
        }

        if (!isAllowedInteraction(event)) {
            event.setCancelled(true);
        }

        if (previewViewers.contains(player.getUniqueId())) {
            return;
        }

        MenuSession session = sessions.get(player.getUniqueId());
        if (session == null || !topInventory.equals(event.getClickedInventory())) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (!isInteractiveSlot(session, event.getSlot(), clicked)) {
            return;
        }

        if (clicked == null) {
            return;
        }

        Integer targetPage = clicked.getItemMeta() == null
                ? null
                : clicked.getItemMeta().getPersistentDataContainer().get(
                        MetKeys.key(plugin, MetKeys.MENU_TARGET_PAGE),
                        PersistentDataType.INTEGER);
        Byte openAbilities = clicked.getItemMeta() == null
                ? null
                : clicked.getItemMeta().getPersistentDataContainer().get(
                        MetKeys.key(plugin, MetKeys.MENU_OPEN_ABILITIES),
                        PersistentDataType.BYTE);
        Byte openStats = clicked.getItemMeta() == null
                ? null
                : clicked.getItemMeta().getPersistentDataContainer().get(
                        MetKeys.key(plugin, MetKeys.MENU_OPEN_STATS),
                        PersistentDataType.BYTE);

        if (targetPage != null && session.getViewType() == MenuSession.ViewType.TREE) {
            int previousPage = session.getPage();
            session.setPage(targetPage);
            player.openInventory(milestoneTreeMenu.build(player, session));
            if (targetPage > previousPage) {
                playMenuSound(player, MenuSoundEvent.PAGE_NEXT);
            } else if (targetPage < previousPage) {
                playMenuSound(player, MenuSoundEvent.PAGE_PREV);
            }
            return;
        }

        if (openAbilities != null && openAbilities == (byte) 1 && session.getViewType() == MenuSession.ViewType.TREE) {
            session.setViewType(MenuSession.ViewType.ABILITIES);
            player.openInventory(abilitiesMenu.build(player, player.getInventory().getItem(EquipmentSlot.HAND)));
            startRefreshTask(player.getUniqueId());
            playMenuSound(player, MenuSoundEvent.OPEN_ABILITIES);
            return;
        }

        if (openStats != null && openStats == (byte) 1 && session.getViewType() == MenuSession.ViewType.TREE) {
            session.setViewType(MenuSession.ViewType.STATS);
            player.openInventory(toolStatsMenu.build(player, player.getInventory().getItem(EquipmentSlot.HAND)));
            playMenuSound(player, MenuSoundEvent.OPEN_STATS);
            return;
        }

        if (session.getViewType() == MenuSession.ViewType.TREE) {
            Integer milestoneBlocks = session.getSlotMilestoneIds().get(event.getSlot());
            if (milestoneBlocks == null) {
                return;
            }
            Optional<EvolutionMilestone> selected = plugin.getToolEvolutionManager()
                    .getMilestones(session.getToolType()).stream()
                    .filter(m -> m.blocksRequired() == milestoneBlocks)
                    .findFirst();
            if (selected.isEmpty()) {
                return;
            }
            session.setSelectedMilestoneBlocks(milestoneBlocks);
            session.setViewType(MenuSession.ViewType.DETAIL);
            player.openInventory(milestoneDetailMenu.build(player, session, selected.get()));
            playMenuSound(player, MenuSoundEvent.OPEN);
            return;
        }

        if (session.getViewType() == MenuSession.ViewType.DETAIL) {
            if (isBackSlot("menu-layouts.milestone-detail.materials.back", event.getSlot(), 18)) {
                session.setViewType(MenuSession.ViewType.TREE);
                player.openInventory(milestoneTreeMenu.build(player, session));
                playMenuSound(player, MenuSoundEvent.BACK);
            }
            return;
        }

        if (session.getViewType() == MenuSession.ViewType.ABILITIES) {
            if (isBackSlot("menu-layouts.abilities.materials.back", event.getSlot(), 49)) {
                stopRefreshTask(player.getUniqueId());
                session.setViewType(MenuSession.ViewType.TREE);
                player.openInventory(milestoneTreeMenu.build(player, session));
                playMenuSound(player, MenuSoundEvent.BACK);
            }
            return;
        }

        if (session.getViewType() == MenuSession.ViewType.STATS
                && isBackSlot("menu-layouts.tool-stats.materials.back", event.getSlot(), 49)) {
            session.setViewType(MenuSession.ViewType.TREE);
            player.openInventory(milestoneTreeMenu.build(player, session));
            playMenuSound(player, MenuSoundEvent.BACK);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        UUID playerId = player.getUniqueId();
        previewViewers.remove(playerId);
        if (!isPluginMenu(event.getInventory())) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> handleMenuClose(player, playerId));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        previewViewers.remove(playerId);
        stopRefreshTask(playerId);
        sessions.remove(playerId);
        pageStateSnapshots.remove(playerId);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!isPluginMenu(topInventory)) {
            return;
        }

        if (!isAllowedInteraction(event)) {
            event.setCancelled(true);
        }
    }

    private boolean isPluginMenu(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof MaxEvolutionMenuHolder;
    }

    private boolean isAllowedInteraction(InventoryClickEvent event) {
        return false;
    }

    private boolean isAllowedInteraction(InventoryDragEvent event) {
        return false;
    }

    private boolean isInteractiveSlot(MenuSession session, int slot, ItemStack clicked) {
        if (slot < 0) {
            return false;
        }

        if (session.getViewType() == MenuSession.ViewType.TREE) {
            if (session.getSlotMilestoneIds().containsKey(slot)) {
                return true;
            }
            if (clicked == null || clicked.getItemMeta() == null) {
                return false;
            }
            return clicked.getItemMeta().getPersistentDataContainer()
                    .has(MetKeys.key(plugin, MetKeys.MENU_TARGET_PAGE), PersistentDataType.INTEGER)
                    || clicked.getItemMeta().getPersistentDataContainer()
                            .has(MetKeys.key(plugin, MetKeys.MENU_OPEN_ABILITIES), PersistentDataType.BYTE)
                    || clicked.getItemMeta().getPersistentDataContainer()
                            .has(MetKeys.key(plugin, MetKeys.MENU_OPEN_STATS), PersistentDataType.BYTE)
                    || clicked.getItemMeta().getPersistentDataContainer()
                            .has(MetKeys.key(plugin, MetKeys.MENU_NON_MOVABLE), PersistentDataType.BYTE);
        }

        if (clicked != null && clicked.getItemMeta() != null && clicked.getItemMeta().getPersistentDataContainer()
                .has(MetKeys.key(plugin, MetKeys.MENU_NON_MOVABLE), PersistentDataType.BYTE)) {
            return true;
        }

        if (session.getViewType() == MenuSession.ViewType.DETAIL) {
            return isBackSlot("menu-layouts.milestone-detail.materials.back", slot, 18);
        }

        if (session.getViewType() == MenuSession.ViewType.ABILITIES) {
            return isBackSlot("menu-layouts.abilities.materials.back", slot, 49);
        }

        if (session.getViewType() == MenuSession.ViewType.STATS) {
            return isBackSlot("menu-layouts.tool-stats.materials.back", slot, 49);
        }

        return false;
    }

    private boolean isBackSlot(String path, int slot, int fallback) {
        return plugin.getConfigManager().isSlotConfigured(path, slot, fallback);
    }

    private void startRefreshTask(UUID playerId) {
        stopRefreshTask(playerId);
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Player player = Bukkit.getPlayer(playerId);
            MenuSession session = sessions.get(playerId);
            if (player == null || !player.isOnline() || session == null
                    || session.getViewType() != MenuSession.ViewType.ABILITIES) {
                stopRefreshTask(playerId);
                return;
            }

            Inventory top = player.getOpenInventory().getTopInventory();
            if (!isPluginMenu(top)) {
                stopRefreshTask(playerId);
                return;
            }

            ItemStack currentTool = player.getInventory().getItem(EquipmentSlot.HAND);
            Inventory updated = abilitiesMenu.build(player, currentTool);
            top.setContents(updated.getContents());
        }, 20L, 20L);
        refreshTasks.put(playerId, task);
    }

    private void stopRefreshTask(UUID playerId) {
        BukkitTask task = refreshTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    private void handleMenuClose(Player player, UUID playerId) {
        if (player == null || !player.isOnline()) {
            stopRefreshTask(playerId);
            sessions.remove(playerId);
            pageStateSnapshots.remove(playerId);
            return;
        }

        Inventory currentTop = player.getOpenInventory().getTopInventory();
        if (isPluginMenu(currentTop)) {
            return;
        }

        stopRefreshTask(playerId);

        MenuSession session = sessions.remove(playerId);
        if (session != null) {
            savePageState(playerId, session);
        }
    }

    private void savePageState(UUID playerId, MenuSession session) {
        pageStateSnapshots.put(playerId,
                new PageStateSnapshot(session.getToolType(), session.getPage(),
                        System.currentTimeMillis() + PAGE_STATE_TTL_MILLIS));
    }

    private void restorePageState(UUID playerId, Material toolType, MenuSession session) {
        PageStateSnapshot snapshot = pageStateSnapshots.remove(playerId);
        if (snapshot == null || snapshot.expiresAtMillis < System.currentTimeMillis()) {
            return;
        }
        if (snapshot.toolType != toolType) {
            return;
        }
        session.setPage(snapshot.page);
    }

    private void playMenuSound(Player player, MenuSoundEvent event) {
        if (player == null || !player.isOnline()) {
            return;
        }

        if (!plugin.getConfigManager().getGuiBoolean("menu-sounds.enabled", true)) {
            return;
        }

        MenuSoundConfig config = getMenuSoundConfig(event);
        player.playSound(player.getLocation(), config.sound(), config.volume(), config.pitch());
    }

    private MenuSoundConfig getMenuSoundConfig(MenuSoundEvent event) {
        String basePath = "menu-sounds." + event.configKey();
        Sound sound = plugin.getConfigManager().getGuiSound(basePath + ".sound", event.defaultSound());
        float volume = Math.max(0.0F, plugin.getConfigManager().getGuiFloat(basePath + ".volume", 1.0F));
        float pitch = Math.max(0.01F, plugin.getConfigManager().getGuiFloat(basePath + ".pitch", 1.0F));
        return new MenuSoundConfig(sound, volume, pitch);
    }

    private record MenuSoundConfig(Sound sound, float volume, float pitch) {
    }

    private enum MenuSoundEvent {
        OPEN("open", Sound.UI_BUTTON_CLICK),
        PAGE_NEXT("page-next", Sound.ITEM_BOOK_PAGE_TURN),
        PAGE_PREV("page-prev", Sound.ITEM_BOOK_PAGE_TURN),
        OPEN_ABILITIES("open-abilities", Sound.BLOCK_ENCHANTMENT_TABLE_USE),
        OPEN_STATS("open-stats", Sound.BLOCK_COMPARATOR_CLICK),
        BACK("back", Sound.UI_BUTTON_CLICK),
        ERROR("error", Sound.ENTITY_VILLAGER_NO);

        private final String configKey;
        private final Sound defaultSound;

        MenuSoundEvent(String configKey, Sound defaultSound) {
            this.configKey = configKey;
            this.defaultSound = defaultSound;
        }

        public String configKey() {
            return configKey;
        }

        public Sound defaultSound() {
            return defaultSound;
        }
    }

    private static final class PageStateSnapshot {
        private final Material toolType;
        private final int page;
        private final long expiresAtMillis;

        private PageStateSnapshot(Material toolType, int page, long expiresAtMillis) {
            this.toolType = toolType;
            this.page = page;
            this.expiresAtMillis = expiresAtMillis;
        }
    }
}

package org.zkaleejoo.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.zkaleejoo.MaxTools;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.utils.MessageUtils;
import org.zkaleejoo.utils.UpdateNotificationFormatter;

public class PlayerJoinListener implements Listener {

    private final MaxTools plugin;

    public PlayerJoinListener(MaxTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("maxtools.admin")) {
            return;
        }

        MainConfigManager config = plugin.getMainConfigManager();
        for (String line : UpdateNotificationFormatter.format(
                config.getPrefix(),
                config.getMsgUpdateAvailable(),
                config.getMsgUpdateCurrent(),
                config.getMsgUpdateDownload(),
                plugin.getPluginMeta().getVersion(),
                plugin.getLatestVersion(),
                MaxTools.UPDATE_DOWNLOAD_URL)) {
            player.sendMessage(MessageUtils.getColoredMessage(line));
        }
    }
}

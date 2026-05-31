package org.zkaleejoo.commands.subcommands;

import java.util.Collections;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.zkaleejoo.MaxTools;
import org.zkaleejoo.utils.MessageUtils;

public class ReloadSubCommand implements SubCommand {

    private final MaxTools plugin;

    public ReloadSubCommand(MaxTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getPermission() {
        return "maxtools.admin.reload";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (plugin.getEvolutionMenuService() != null) {
            plugin.getEvolutionMenuService().resetActiveMenuSessions();
        }
        plugin.getConfigManager().reloadConfig();
        plugin.getToolEvolutionManager().reload();
        plugin.recreateDiscordWebhookNotifier();
        plugin.reloadRuntimeState();
        sender.sendMessage(MessageUtils.getColoredMessage(
                plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgPluginReload()));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}

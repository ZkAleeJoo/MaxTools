package org.zkaleejoo.commands.subcommands;

import java.util.Collections;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.zkaleejoo.MaxTools;
import org.zkaleejoo.utils.MessageUtils;

public class MenuSubCommand implements SubCommand {

    private final MaxTools plugin;

    public MenuSubCommand(MaxTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "menu";
    }

    @Override
    public List<String> getAliases() {
        return List.of("hub", "gui");
    }

    @Override
    public String getPermission() {
        return "maxtools.menu";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgOnlyPlayers()));
            return true;
        }

        if (!plugin.getEvolutionMenuService().openHub(player)) {
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgInvalidTool()));
            plugin.getEvolutionMenuService().playMenuError(player);
            return true;
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}

package org.zkaleejoo.commands.subcommands;

import java.util.Collections;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.zkaleejoo.MaxTools;
import org.zkaleejoo.utils.MessageUtils;

public class PreviewSubCommand implements SubCommand {

    private final MaxTools plugin;

    public PreviewSubCommand(MaxTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "preview";
    }

    @Override
    public List<String> getAliases() {
        return List.of("adminpreview");
    }

    @Override
    public String getPermission() {
        return "maxtools.admin.preview";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgOnlyPlayers()));
            return true;
        }
        plugin.getEvolutionMenuService().openAdminPreview(player);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}

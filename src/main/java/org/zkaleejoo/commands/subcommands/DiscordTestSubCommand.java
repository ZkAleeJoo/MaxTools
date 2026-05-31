package org.zkaleejoo.commands.subcommands;

import java.util.Collections;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.zkaleejoo.MaxTools;
import org.zkaleejoo.utils.MessageUtils;

public class DiscordTestSubCommand implements SubCommand {

    private final MaxTools plugin;

    public DiscordTestSubCommand(MaxTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "discordtest";
    }

    @Override
    public List<String> getAliases() {
        return List.of("dctest");
    }

    @Override
    public String getPermission() {
        return "maxtools.admin.discordtest";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (plugin.getDiscordWebhookNotifier() == null) {
            sender.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().getMsgDiscordNotifierNotInitialized()));
            return true;
        }

        String actor = sender.getName();
        String toolName = plugin.getConfigManager().getMsgDiscordTestConsoleToolName();
        if (sender instanceof Player player) {
            ItemStack heldItem = player.getInventory().getItem(EquipmentSlot.HAND);
            Material heldType = heldItem == null ? Material.AIR : heldItem.getType();
            toolName = heldType == Material.AIR
                    ? plugin.getConfigManager().getMsgDiscordTestNoToolName()
                    : plugin.getConfigManager().getToolName(heldType);
        }

        boolean queued = plugin.getDiscordWebhookNotifier().sendTestMessage(actor, toolName, 0);
        if (queued) {
            sender.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().getMsgDiscordTestQueued()));
        } else {
            sender.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().getMsgDiscordTestFailed()));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}

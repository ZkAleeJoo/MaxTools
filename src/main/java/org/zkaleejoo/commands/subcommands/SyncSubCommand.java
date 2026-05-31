package org.zkaleejoo.commands.subcommands;

import java.util.Collections;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.zkaleejoo.MaxTools;
import org.zkaleejoo.evolution.EvolutionSyncResult;
import org.zkaleejoo.utils.MessageUtils;

public class SyncSubCommand implements SubCommand {

    private final MaxTools plugin;

    public SyncSubCommand(MaxTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "sync";
    }

    @Override
    public String getPermission() {
        return "maxtools.admin.sync";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgOnlyPlayers()));
            return true;
        }

        ItemStack tool = player.getInventory().getItem(EquipmentSlot.HAND);
        if (!plugin.getToolEvolutionManager().isTrackedTool(tool)) {
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgInvalidTool()));
            return true;
        }

        EvolutionSyncResult result = plugin.getToolEvolutionManager().syncEvolution(tool);
        String message = result.changed()
                ? plugin.getConfigManager().getMsgSyncSuccess()
                        .replace("{milestones}", String.valueOf(result.milestonesApplied()))
                        .replace("{abilities}", String.valueOf(result.abilitiesAdded()))
                : plugin.getConfigManager().getMsgSyncNoChanges();

        player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + message));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}

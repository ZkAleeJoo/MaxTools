package org.zkaleejoo.commands.subcommands;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.zkaleejoo.MaxTools;
import org.zkaleejoo.utils.MessageUtils;
import java.util.Set;

public class ToolInfoSubCommand implements SubCommand {

    private final MaxTools plugin;

    public ToolInfoSubCommand(MaxTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "toolinfo";
    }

    @Override
    public String getPermission() {
        return "maxtools.toolinfo";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgOnlyPlayers()));
            return true;
        }

        boolean useGui = plugin.getConfigManager().isToolInfoGuiDefault();
        if (args.length > 0) {
            if ("gui".equalsIgnoreCase(args[0])) {
                useGui = true;
            } else if ("text".equalsIgnoreCase(args[0]) || "legacy".equalsIgnoreCase(args[0])) {
                useGui = false;
            }
        }

        if (useGui) {
            try {
                if (plugin.getEvolutionMenuService().openHub(player)) {
                    return true;
                }
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("Failed to open toolinfo GUI for " + player.getName()
                        + ". Falling back to legacy text mode: " + exception.getMessage());
            }
        }

        return sendLegacyTextInfo(player);
    }

    private boolean sendLegacyTextInfo(Player player) {
        ItemStack item = player.getInventory().getItem(EquipmentSlot.HAND);
        if (!plugin.getToolEvolutionManager().isTrackedTool(item)) {
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgInvalidTool()));
            return true;
        }

        int usage = plugin.getToolEvolutionManager().getUsage(item);
        Set<String> abilities = plugin.getToolEvolutionManager().getUnlockedAbilities(item);
        boolean special = !abilities.isEmpty();

        String message = plugin.getConfigManager().getMsgToolInfo()
                .replace("%usage%", String.valueOf(usage))
                .replace("%special%",
                        special ? plugin.getConfigManager().getMsgEnabledWord()
                                : plugin.getConfigManager().getMsgDisabledWord())
                .replace("%abilities%", abilities.isEmpty() ? "-" : String.join(", ", abilities));

        player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + message));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase(Locale.ROOT);
            return List.of("gui", "text", "legacy").stream()
                    .filter(option -> option.startsWith(input))
                    .toList();
        }
        return Collections.emptyList();
    }
}

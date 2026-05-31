package org.zkaleejoo.commands.subcommands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.zkaleejoo.MaxTools;
import org.zkaleejoo.utils.MessageUtils;
import org.zkaleejoo.utils.MetKeys;
import org.zkaleejoo.utils.TestToolRegistry;

public class ClearTestToolSubCommand implements SubCommand {

    private final MaxTools plugin;

    public ClearTestToolSubCommand(MaxTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "cleartesttool";
    }

    @Override
    public List<String> getAliases() {
        return List.of("cleartest", "untesttool", "cleartestool", "cleartestools", "cleartesttool");
    }

    @Override
    public String getPermission() {
        return "maxtools.admin.cleartesttool";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgOnlyPlayers()));
            return true;
        }

        if (args.length > 1) {
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgClearTestToolUsage()));
            return true;
        }

        if (args.length == 1) {
            return clearRegisteredTestTool(player, args[0]);
        }

        return clearHeldTestTool(player);
    }

    private boolean clearHeldTestTool(Player player) {
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.getType() == Material.AIR || !heldItem.hasItemMeta()) {
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgInvalidTool()));
            return true;
        }

        ItemMeta meta = heldItem.getItemMeta();
        if (!plugin.getToolEvolutionManager().isTestTool(meta)) {
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgClearTestToolNotTest()));
            return true;
        }

        String testToolId = getTestToolId(meta);
        if (!testToolId.isBlank()) {
            plugin.getTestToolRegistry().removeTool(testToolId);
        }

        removeMainHandItem(player);
        player.sendMessage(MessageUtils.getColoredMessage(
                plugin.getConfigManager().getPrefix()
                        + plugin.getConfigManager().getMsgClearTestToolDestroyed()
                                .replace("{id}", getDisplayId(testToolId))));
        return true;
    }

    private boolean clearRegisteredTestTool(Player player, String rawId) {
        TestToolRegistry registry = plugin.getTestToolRegistry();
        String testToolId = registry.normalizeId(rawId);
        if (testToolId.isBlank()) {
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgClearTestToolUsage()));
            return true;
        }

        if (!registry.containsTool(testToolId)) {
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix()
                            + plugin.getConfigManager().getMsgClearTestToolIdNotFound()
                                    .replace("{id}", testToolId)));
            return true;
        }

        boolean removedHeldItem = isHeldTestTool(player, testToolId);
        if (removedHeldItem) {
            removeMainHandItem(player);
        }
        registry.removeTool(testToolId);

        String message = removedHeldItem
                ? plugin.getConfigManager().getMsgClearTestToolDestroyed()
                : plugin.getConfigManager().getMsgClearTestToolIdRemoved();
        player.sendMessage(MessageUtils.getColoredMessage(
                plugin.getConfigManager().getPrefix() + message.replace("{id}", testToolId)));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterByInput(plugin.getTestToolRegistry().getToolIds(), args[0]);
        }

        return Collections.emptyList();
    }

    private boolean isHeldTestTool(Player player, String testToolId) {
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.getType() == Material.AIR || !heldItem.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = heldItem.getItemMeta();
        return plugin.getToolEvolutionManager().isTestTool(meta) && testToolId.equals(getTestToolId(meta));
    }

    private String getTestToolId(ItemMeta meta) {
        if (meta == null) {
            return "";
        }
        String id = meta.getPersistentDataContainer()
                .getOrDefault(Objects.requireNonNull(MetKeys.key(plugin, MetKeys.TEST_TOOL_ID)), Objects.requireNonNull(PersistentDataType.STRING), "");
        return plugin.getTestToolRegistry().normalizeId(id);
    }

    private void removeMainHandItem(Player player) {
        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        player.updateInventory();
    }

    private String getDisplayId(String testToolId) {
        return testToolId == null || testToolId.isBlank() ? "N/A" : testToolId;
    }

    private List<String> filterByInput(List<String> values, String input) {
        String normalizedInput = input == null ? "" : input.toLowerCase(Locale.ROOT);
        List<String> filtered = new ArrayList<>();
        for (String value : values) {
            if (value.startsWith(normalizedInput)) {
                filtered.add(value);
            }
        }
        return filtered;
    }
}

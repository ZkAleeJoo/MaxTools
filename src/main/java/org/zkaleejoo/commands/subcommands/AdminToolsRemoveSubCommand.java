package org.zkaleejoo.commands.subcommands;

import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.zkaleejoo.MaxTools;
import org.zkaleejoo.utils.MessageUtils;

public class AdminToolsRemoveSubCommand implements SubCommand {

    private final MaxTools plugin;

    public AdminToolsRemoveSubCommand(MaxTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "admintoolsremove";
    }

    @Override
    public List<String> getAliases() {
        return List.of("adminremove", "purgetools", "toolspurge");
    }

    @Override
    public String getPermission() {
        return "maxtools.admin.admintoolsremove";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length != 1 || !"confirm".equalsIgnoreCase(args[0])) {
            sender.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgAdminToolsRemoveUsage()));
            return true;
        }

        sender.sendMessage(MessageUtils.getColoredMessage(
                plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgAdminToolsRemoveStarted()));

        int removedLoadedItems = purgeLoadedCustomTools();
        int databaseRecords = 0;
        if (plugin.getCustomToolDatabase() != null && plugin.getCustomToolDatabase().isAvailable()) {
            databaseRecords = plugin.getCustomToolDatabase().deleteAllTools();
        } else {
            sender.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().getMsgAdminToolsRemoveDatabaseUnavailable()));
        }

        String message = plugin.getConfigManager().getMsgAdminToolsRemoveSuccess()
                .replace("{removed}", String.valueOf(removedLoadedItems))
                .replace("{database}", String.valueOf(databaseRecords));
        sender.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + message));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && "confirm".startsWith(args[0].toLowerCase())) {
            return List.of("confirm");
        }
        return Collections.emptyList();
    }

    private int purgeLoadedCustomTools() {
        int removed = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player == null) {
                continue;
            }
            removed += purgeInventory(player.getInventory());
            removed += purgeInventory(player.getEnderChest());
            removed += purgeInventory(player.getOpenInventory().getTopInventory());
            ItemStack cursorItem = player.getItemOnCursor();
            if (plugin.getToolEvolutionManager().isCustomTool(cursorItem)) {
                removed += Math.max(1, cursorItem.getAmount());
                player.setItemOnCursor(new ItemStack(Material.AIR));
            } else {
                int nestedCursorRemoved = purgeNestedContainer(cursorItem);
                if (nestedCursorRemoved > 0) {
                    removed += nestedCursorRemoved;
                    player.setItemOnCursor(cursorItem);
                }
            }
            player.updateInventory();
        }

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                removed += purgeEntity(entity);
            }

            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState state : chunk.getTileEntities()) {
                    if (!(state instanceof Container container)) {
                        continue;
                    }
                    int removedFromContainer = purgeInventory(container.getInventory());
                    if (removedFromContainer > 0) {
                        removed += removedFromContainer;
                        state.update(true, false);
                    }
                }
            }
        }

        return removed;
    }

    private int purgeEntity(Entity entity) {
        if (entity instanceof Player) {
            return 0;
        }

        if (entity instanceof Item itemEntity) {
            ItemStack stack = itemEntity.getItemStack();
            if (plugin.getToolEvolutionManager().isCustomTool(stack)) {
                int amount = Math.max(1, stack.getAmount());
                itemEntity.remove();
                return amount;
            }

            int nestedRemoved = purgeNestedContainer(stack);
            if (nestedRemoved > 0) {
                itemEntity.setItemStack(stack);
            }
            return nestedRemoved;
        }

        if (entity instanceof ItemFrame itemFrame) {
            ItemStack framed = itemFrame.getItem();
            if (plugin.getToolEvolutionManager().isCustomTool(framed)) {
                itemFrame.setItem(new ItemStack(Material.AIR));
                return Math.max(1, framed.getAmount());
            }

            int nestedRemoved = purgeNestedContainer(framed);
            if (nestedRemoved > 0) {
                itemFrame.setItem(framed);
            }
            return nestedRemoved;
        }

        if (entity instanceof InventoryHolder holder) {
            return purgeInventory(holder.getInventory());
        }

        return 0;
    }

    private int purgeInventory(Inventory inventory) {
        if (inventory == null) {
            return 0;
        }

        ItemStack[] contents = inventory.getContents();
        int removed = 0;
        boolean changed = false;
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null || item.getType().isAir()) {
                continue;
            }

            if (plugin.getToolEvolutionManager().isCustomTool(item)) {
                removed += Math.max(1, item.getAmount());
                contents[slot] = null;
                changed = true;
                continue;
            }

            int nestedRemoved = purgeNestedContainer(item);
            if (nestedRemoved > 0) {
                removed += nestedRemoved;
                contents[slot] = item;
                changed = true;
            }
        }

        if (changed) {
            inventory.setContents(contents);
        }
        return removed;
    }

    private int purgeNestedContainer(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return 0;
        }

        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof BlockStateMeta blockStateMeta)
                || !(blockStateMeta.getBlockState() instanceof Container container)) {
            return 0;
        }

        int removed = purgeInventory(container.getInventory());
        if (removed > 0) {
            blockStateMeta.setBlockState(container);
            item.setItemMeta(blockStateMeta);
        }
        return removed;
    }
}

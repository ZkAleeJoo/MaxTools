package org.zkaleejoo.evolution.abilities;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.zkaleejoo.evolution.SpecialAbilityConfig;
import org.zkaleejoo.evolution.ToolEvolutionManager;

public class VeinMinerAbilityHandler implements AbilityHandler {

    private static final List<int[]> CARDINAL_DIRECTIONS = List.of(
            new int[] {1, 0, 0},
            new int[] {-1, 0, 0},
            new int[] {0, 1, 0},
            new int[] {0, -1, 0},
            new int[] {0, 0, 1},
            new int[] {0, 0, -1}
    );

    private final Set<UUID> processingPlayers = new HashSet<>();

    @Override
    public boolean canTrigger(ItemMeta meta, SpecialAbilityConfig ability, ToolEvolutionManager evolutionManager) {
        return meta != null && evolutionManager.canProcAbility(meta, ability);
    }

    @Override
    public void onBlockBreak(BlockBreakAbilityContext context, SpecialAbilityConfig ability) {
        Player player = context.player();
        ItemStack tool = context.tool();
        UUID playerId = player.getUniqueId();
        Block origin = context.event().getBlock();
        Material originType = origin.getType();

        if (processingPlayers.contains(playerId)) {
            return;
        }

        if (!tool.getType().name().endsWith("_PICKAXE")) {
            return;
        }

        if (!context.rollProc(ability)) {
            return;
        }

        if (ability.hasMaterialWhitelist() && !ability.materialWhitelist().contains(originType)) {
            return;
        }

        int maxExtraBlocks = Math.max(0, ability.amount());
        if (maxExtraBlocks <= 0) {
            return;
        }

        List<Block> targets = findConnectedBlocks(origin, originType, maxExtraBlocks);
        if (targets.isEmpty()) {
            return;
        }

        context.evolutionManager().applyCooldown(context.meta(), ability);
        context.evolutionManager().incrementAbilityActivation(context.meta(), ability.id());

        processingPlayers.add(playerId);
        try {
            for (Block target : targets) {
                if (target.getType() == Material.AIR || target.equals(origin)) {
                    continue;
                }

                if (!context.evolutionManager().shouldCountBlock(target, tool)) {
                    continue;
                }

                breakTargetBlockWithEvent(context, target);
            }
        } finally {
            processingPlayers.remove(playerId);
        }
    }

    private List<Block> findConnectedBlocks(Block origin, Material targetType, int maxExtraBlocks) {
        List<Block> result = new java.util.ArrayList<>();
        Queue<Block> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();

        queue.add(origin);
        visited.add(locationKey(origin));

        while (!queue.isEmpty() && result.size() < maxExtraBlocks) {
            Block current = queue.poll();
            for (int[] direction : CARDINAL_DIRECTIONS) {
                Block next = current.getRelative(direction[0], direction[1], direction[2]);
                String nextKey = locationKey(next);
                if (!visited.add(nextKey)) {
                    continue;
                }
                if (next.getType() != targetType) {
                    continue;
                }

                result.add(next);
                if (result.size() >= maxExtraBlocks) {
                    break;
                }
                queue.add(next);
            }
        }

        return result;
    }

    private void breakTargetBlockWithEvent(BlockBreakAbilityContext context, Block target) {
        BlockBreakEvent targetBreakEvent = new BlockBreakEvent(target, context.player());
        context.plugin().getServer().getPluginManager().callEvent(targetBreakEvent);
        if (targetBreakEvent.isCancelled()) {
            return;
        }
        applyBreakResult(target, context.player(), context.tool(), targetBreakEvent);
    }

    private void applyBreakResult(Block target, Player player, ItemStack tool, BlockBreakEvent event) {
        if (target.getType() == Material.AIR) {
            return;
        }
        World world = target.getWorld();
        if (event.isDropItems()) {
            target.breakNaturally(tool);
        } else {
            target.setType(Material.AIR, false);
        }

        int expToDrop = event.getExpToDrop();
        if (expToDrop > 0) {
            world.spawn(target.getLocation(), ExperienceOrb.class, orb -> orb.setExperience(expToDrop));
        }
    }

    private String locationKey(Block block) {
        return block.getWorld().getName()
                + ":" + block.getX()
                + ":" + block.getY()
                + ":" + block.getZ();
    }
}

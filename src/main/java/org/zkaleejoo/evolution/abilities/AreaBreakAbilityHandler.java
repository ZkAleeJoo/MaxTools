package org.zkaleejoo.evolution.abilities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.zkaleejoo.evolution.AbilityType;
import org.zkaleejoo.evolution.SpecialAbilityConfig;
import org.zkaleejoo.evolution.ToolEvolutionManager;

public class AreaBreakAbilityHandler implements AbilityHandler {

    private final AbilityType abilityType;
    private final Set<UUID> processingPlayers = new HashSet<>();

    public AreaBreakAbilityHandler(AbilityType abilityType) {
        this.abilityType = abilityType;
    }

    @Override
    public boolean canTrigger(ItemMeta meta, SpecialAbilityConfig ability, ToolEvolutionManager evolutionManager) {
        return meta != null && evolutionManager.canProcAbility(meta, ability);
    }

    @Override
    public void onBlockBreak(BlockBreakAbilityContext context, SpecialAbilityConfig ability) {
        Player player = context.player();
        ItemStack tool = context.tool();
        UUID playerId = player.getUniqueId();

        if (processingPlayers.contains(playerId)) {
            return;
        }

        if (!isCompatibleTool(tool.getType())) {
            return;
        }

        if (!context.rollProc(ability)) {
            return;
        }

        List<Block> targets = getThreeByThreeTargets(context.event().getBlock(), player, Math.max(1, ability.amount()));
        if (targets.isEmpty()) {
            return;
        }

        context.evolutionManager().applyCooldown(context.meta(), ability);
        context.evolutionManager().incrementAbilityActivation(context.meta(), ability.id());

        processingPlayers.add(playerId);
        try {
            for (Block target : targets) {
                if (target.equals(context.event().getBlock()) || target.getType() == Material.AIR) {
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

    private boolean isCompatibleTool(Material material) {
        if (!isHighTierTool(material)) {
            return false;
        }

        return switch (abilityType) {
            case DRILL -> material.name().endsWith("_PICKAXE") || material.name().endsWith("_SHOVEL");
            case HAMMER -> material.name().endsWith("_SHOVEL");
            default -> false;
        };
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

        List<ItemStack> drops = event.isDropItems()
                ? target.getDrops(tool, player).stream().map(ItemStack::clone).toList()
                : List.of();

        target.setType(Material.AIR, false);

        if (event.isDropItems()) {
            World world = target.getWorld();
            for (ItemStack drop : drops) {
                world.dropItemNaturally(target.getLocation(), drop);
            }
        }

        int expToDrop = event.getExpToDrop();
        if (expToDrop > 0) {
            target.getWorld().spawn(target.getLocation(), ExperienceOrb.class, orb -> orb.setExperience(expToDrop));
        }
    }

    private List<Block> getThreeByThreeTargets(Block center, Player player, int radius) {
        List<Block> blocks = new ArrayList<>();
        BlockFace facing = player.getFacing();
        Location playerLocation = player.getLocation();
        boolean vertical = playerLocation != null && Math.abs(playerLocation.getPitch()) > 60;

        for (int first = -radius; first <= radius; first++) {
            for (int second = -radius; second <= radius; second++) {
                Block target;
                if (vertical) {
                    target = center.getRelative(first, 0, second);
                } else if (facing == BlockFace.EAST || facing == BlockFace.WEST) {
                    target = center.getRelative(0, first, second);
                } else {
                    target = center.getRelative(first, second, 0);
                }
                blocks.add(target);
            }
        }
        return blocks;
    }

    private boolean isHighTierTool(Material material) {
        return material.name().startsWith("DIAMOND_") || material.name().startsWith("NETHERITE_");
    }
}

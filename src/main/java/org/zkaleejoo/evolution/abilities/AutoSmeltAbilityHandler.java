package org.zkaleejoo.evolution.abilities;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.zkaleejoo.evolution.SpecialAbilityConfig;
import org.zkaleejoo.evolution.ToolEvolutionManager;

public class AutoSmeltAbilityHandler implements AbilityHandler {

    @Override
    public boolean canTrigger(ItemMeta meta, SpecialAbilityConfig ability, ToolEvolutionManager evolutionManager) {
        return meta != null && evolutionManager.canProcAbility(meta, ability);
    }

    @Override
    public void onBlockBreak(BlockBreakAbilityContext context, SpecialAbilityConfig ability) {
        if (!context.rollProc(ability)) {
            return;
        }

        List<ItemStack> drops = context.getDrops();
        if (drops.isEmpty()) {
            return;
        }

        if (!hasSmeltableDrops(drops)) {
            return;
        }

        context.event().setDropItems(false);
        context.setDrops(autoSmeltDrops(drops));
        context.evolutionManager().applyCooldown(context.meta(), ability);
        context.evolutionManager().incrementAbilityActivation(context.meta(), ability.id());
    }

    private boolean hasSmeltableDrops(List<ItemStack> drops) {
        for (ItemStack drop : drops) {
            if (resolveSmeltedMaterial(drop.getType()) != null) {
                return true;
            }
        }
        return false;
    }

    private List<ItemStack> autoSmeltDrops(List<ItemStack> drops) {
        List<ItemStack> result = new ArrayList<>(drops.size());
        for (ItemStack drop : drops) {
            Material smelted = resolveSmeltedMaterial(drop.getType());
            if (smelted == null) {
                result.add(drop);
            } else {
                result.add(new ItemStack(smelted, drop.getAmount()));
            }
        }
        return result;
    }

    private Material resolveSmeltedMaterial(Material source) {
        return switch (source) {
            case RAW_IRON -> Material.IRON_INGOT;
            case RAW_GOLD -> Material.GOLD_INGOT;
            case RAW_COPPER -> Material.COPPER_INGOT;
            case ANCIENT_DEBRIS -> Material.NETHERITE_SCRAP;
            case IRON_ORE -> Material.IRON_INGOT;
            case GOLD_ORE -> Material.GOLD_INGOT;
            case COPPER_ORE -> Material.COPPER_INGOT;
            default -> null;
        };
    }
}

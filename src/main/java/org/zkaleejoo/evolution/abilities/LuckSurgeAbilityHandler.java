package org.zkaleejoo.evolution.abilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.zkaleejoo.evolution.SpecialAbilityConfig;
import org.zkaleejoo.evolution.ToolEvolutionManager;

public class LuckSurgeAbilityHandler implements AbilityHandler {

    private static final long ACTIVE_WINDOW_MILLIS = 4_000L;
    private static final Map<UUID, Long> ACTIVE_UNTIL_BY_PLAYER = new HashMap<>();

    @Override
    public boolean canTrigger(ItemMeta meta, SpecialAbilityConfig ability, ToolEvolutionManager evolutionManager) {
        return meta != null && ability != null && ability.enabled();
    }

    @Override
    public void onBlockBreak(BlockBreakAbilityContext context, SpecialAbilityConfig ability) {
        Material brokenType = context.event().getBlock().getType();
        if (ability.hasMaterialWhitelist() && !ability.materialWhitelist().contains(brokenType)) {
            return;
        }

        long now = System.currentTimeMillis();
        UUID playerId = context.player().getUniqueId();
        Long activeUntil = ACTIVE_UNTIL_BY_PLAYER.get(playerId);
        boolean isActive = activeUntil != null && activeUntil > now;

        if (!isActive && context.evolutionManager().canProcAbility(context.meta(), ability)) {
            ACTIVE_UNTIL_BY_PLAYER.put(playerId, now + ACTIVE_WINDOW_MILLIS);
            context.evolutionManager().applyCooldown(context.meta(), ability);
            context.evolutionManager().incrementAbilityActivation(context.meta(), ability.id());
            isActive = true;
        }

        if (!isActive) {
            ACTIVE_UNTIL_BY_PLAYER.remove(playerId);
            return;
        }

        List<ItemStack> drops = context.getDrops();
        if (drops.isEmpty()) {
            return;
        }

        double configuredMultiplier = Math.max(1.0D, ability.amount());
        double multiplier = Math.min(configuredMultiplier, Math.max(1.0D, ability.maxMultiplier()));
        if (multiplier <= 1.0D) {
            return;
        }

        context.setDrops(multiplyDrops(drops, multiplier));
    }

    private List<ItemStack> multiplyDrops(List<ItemStack> drops, double multiplier) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack drop : drops) {
            int boostedAmount = Math.max(1, (int) Math.ceil(drop.getAmount() * multiplier));
            int maxStack = Math.max(1, drop.getMaxStackSize());
            while (boostedAmount > 0) {
                int stackAmount = Math.min(maxStack, boostedAmount);
                ItemStack boostedDrop = drop.clone();
                boostedDrop.setAmount(stackAmount);
                result.add(boostedDrop);
                boostedAmount -= stackAmount;
            }
        }
        return result;
    }
}

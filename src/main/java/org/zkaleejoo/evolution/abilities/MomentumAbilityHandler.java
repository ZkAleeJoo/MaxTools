package org.zkaleejoo.evolution.abilities;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.zkaleejoo.evolution.SpecialAbilityConfig;
import org.zkaleejoo.evolution.ToolEvolutionManager;

public class MomentumAbilityHandler implements AbilityHandler {

    private static final int EFFECT_DURATION_TICKS = 60;
    private final Map<UUID, MomentumState> playerMomentum = new ConcurrentHashMap<>();

    @Override
    public boolean canTrigger(ItemMeta meta, SpecialAbilityConfig ability, ToolEvolutionManager evolutionManager) {
        return meta != null && ability != null && ability.enabled();
    }

    @Override
    public void onBlockBreak(BlockBreakAbilityContext context, SpecialAbilityConfig ability) {
        if (!context.evolutionManager().isOffCooldown(context.meta(), ability.id())) {
            return;
        }

        long now = System.currentTimeMillis();
        UUID playerId = context.player().getUniqueId();
        MomentumState current = playerMomentum.get(playerId);
        int stacks = (current != null && now <= current.lastBreakAt() + ability.stackWindowMs())
                ? Math.min(ability.maxStacks(), current.stacks() + 1)
                : 1;
        playerMomentum.put(playerId, new MomentumState(stacks, now));
    }

    @Override
    public void onTick(TickAbilityContext context, SpecialAbilityConfig ability) {
        UUID playerId = context.player().getUniqueId();
        MomentumState state = playerMomentum.get(playerId);
        if (state == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now > state.lastBreakAt() + ability.stackWindowMs()) {
            playerMomentum.remove(playerId);
            context.evolutionManager().applyCooldown(context.meta(), ability);
            return;
        }

        int stackAmplifier = Math.max(1, ability.perStackAmplifier());
        int effectLevel = Math.max(1, state.stacks() * stackAmplifier);
        int amplifier = effectLevel - 1;
        PotionEffect effect = new PotionEffect(PotionEffectType.HASTE, EFFECT_DURATION_TICKS, amplifier, true, false, true);
        context.player().addPotionEffect(effect);
    }

    @Override
    public void onPlayerDisconnect(UUID playerId) {
        playerMomentum.remove(playerId);
    }

    @Override
    public void onPluginDisable() {
        playerMomentum.clear();
    }

    private record MomentumState(int stacks, long lastBreakAt) {
    }
}

package org.zkaleejoo.evolution.abilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.zkaleejoo.MaxTools;
import org.zkaleejoo.evolution.SpecialAbilityConfig;
import org.zkaleejoo.evolution.ToolEvolutionManager;

public class BlockBreakAbilityContext {

    private final MaxTools plugin;
    private final ToolEvolutionManager evolutionManager;
    private final BlockBreakEvent event;
    private final Player player;
    private final ItemStack tool;
    private final ItemMeta meta;
    private final Map<String, Boolean> procCache = new HashMap<>();
    private List<ItemStack> drops;
    private boolean dropsDispatched;

    public BlockBreakAbilityContext(
            MaxTools plugin,
            ToolEvolutionManager evolutionManager,
            BlockBreakEvent event,
            Player player,
            ItemStack tool,
            ItemMeta meta) {
        this.plugin = plugin;
        this.evolutionManager = evolutionManager;
        this.event = event;
        this.player = player;
        this.tool = tool;
        this.meta = meta;
    }

    public boolean rollProc(SpecialAbilityConfig ability) {
        return procCache.computeIfAbsent(ability.id(), key -> evolutionManager.canProcAbility(meta, ability));
    }

    public void ensureDropsLoaded() {
        if (drops != null) {
            return;
        }
        Block block = event.getBlock();
        drops = block.getDrops(tool, player).stream().map(ItemStack::clone).toList();
    }

    public List<ItemStack> getDrops() {
        ensureDropsLoaded();
        return drops;
    }

    public void setDrops(List<ItemStack> updatedDrops) {
        drops = new ArrayList<>(updatedDrops);
    }

    public boolean hasCustomDrops() {
        return drops != null && !drops.isEmpty();
    }

    public void markDropsDispatched() {
        dropsDispatched = true;
    }

    public boolean isDropsDispatched() {
        return dropsDispatched;
    }

    public MaxTools plugin() {
        return plugin;
    }

    public ToolEvolutionManager evolutionManager() {
        return evolutionManager;
    }

    public BlockBreakEvent event() {
        return event;
    }

    public Player player() {
        return player;
    }

    public ItemStack tool() {
        return tool;
    }

    public ItemMeta meta() {
        return meta;
    }
}

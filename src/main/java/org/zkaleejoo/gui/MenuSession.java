package org.zkaleejoo.gui;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;

public class MenuSession {

    public enum ViewType {
        TREE,
        DETAIL,
        ABILITIES,
        STATS
    }

    private final UUID playerId;
    private final Material toolType;
    private final int usage;
    private int page;
    private Integer selectedMilestoneBlocks;
    private ViewType viewType;
    private Map<Integer, Integer> slotMilestoneIds;

    public MenuSession(UUID playerId, Material toolType, int usage) {
        this.playerId = playerId;
        this.toolType = toolType;
        this.usage = usage;
        this.page = 0;
        this.viewType = ViewType.TREE;
        this.slotMilestoneIds = new HashMap<>();
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public Material getToolType() {
        return toolType;
    }

    public int getUsage() {
        return usage;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = Math.max(0, page);
    }

    public Integer getSelectedMilestoneBlocks() {
        return selectedMilestoneBlocks;
    }

    public void setSelectedMilestoneBlocks(Integer selectedMilestoneBlocks) {
        this.selectedMilestoneBlocks = selectedMilestoneBlocks;
    }

    public ViewType getViewType() {
        return viewType;
    }

    public void setViewType(ViewType viewType) {
        this.viewType = viewType;
    }

    public void setSlotMilestoneIds(Map<Integer, Integer> slotMilestoneIds) {
        this.slotMilestoneIds = new HashMap<>(slotMilestoneIds);
    }

    public Map<Integer, Integer> getSlotMilestoneIds() {
        return Collections.unmodifiableMap(slotMilestoneIds);
    }
}

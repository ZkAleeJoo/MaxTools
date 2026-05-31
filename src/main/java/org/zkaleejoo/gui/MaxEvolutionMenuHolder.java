package org.zkaleejoo.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class MaxEvolutionMenuHolder implements InventoryHolder {

    public enum MenuKind {
        TREE,
        DETAIL,
        ABILITIES,
        STATS,
        ADMIN_PREVIEW
    }

    private final MenuKind menuKind;
    private Inventory inventory;

    public MaxEvolutionMenuHolder(MenuKind menuKind) {
        this.menuKind = menuKind;
    }

    public MenuKind getMenuKind() {
        return menuKind;
    }

    public void bindInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}

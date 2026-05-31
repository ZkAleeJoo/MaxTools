package org.zkaleejoo.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class LanguageMenuHolder implements InventoryHolder {

    private Inventory inventory;

    public void bindInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}

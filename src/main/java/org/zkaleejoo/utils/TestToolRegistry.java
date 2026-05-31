package org.zkaleejoo.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.zkaleejoo.MaxTools;

public class TestToolRegistry {

    private static final String FILE_NAME = "test-tools.yml";
    private static final String TOOLS_PATH = "tools";
    private static final String NEXT_ID_PATH = "next-id";

    private final MaxTools plugin;
    private final File file;
    private FileConfiguration config;

    public TestToolRegistry(MaxTools plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), FILE_NAME);
        reload();
    }

    public synchronized void reload() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException exception) {
                plugin.getLogger().warning("Could not create " + FILE_NAME + ": " + exception.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(file);
        if (!config.contains(NEXT_ID_PATH)) {
            config.set(NEXT_ID_PATH, 1);
            save();
        }
    }

    public synchronized String registerTool(Material material, int usage, Set<String> abilities, UUID ownerId,
            String ownerName) {
        int nextId = Math.max(1, config.getInt(NEXT_ID_PATH, 1));
        String id = String.valueOf(nextId);
        while (config.contains(TOOLS_PATH + "." + id)) {
            nextId++;
            id = String.valueOf(nextId);
        }

        String path = TOOLS_PATH + "." + id;
        config.set(path + ".material", material.name());
        config.set(path + ".usage", Math.max(0, usage));
        config.set(path + ".abilities", new ArrayList<>(abilities));
        config.set(path + ".owner.uuid", ownerId == null ? "" : ownerId.toString());
        config.set(path + ".owner.name", ownerName == null ? "" : ownerName);
        config.set(path + ".created-at", System.currentTimeMillis());
        config.set(NEXT_ID_PATH, nextId + 1);
        save();
        return id;
    }

    public synchronized boolean removeTool(String id) {
        String normalizedId = normalizeId(id);
        if (normalizedId.isBlank() || !config.contains(TOOLS_PATH + "." + normalizedId)) {
            return false;
        }

        config.set(TOOLS_PATH + "." + normalizedId, null);
        save();
        return true;
    }

    public synchronized boolean containsTool(String id) {
        String normalizedId = normalizeId(id);
        return !normalizedId.isBlank() && config.contains(TOOLS_PATH + "." + normalizedId);
    }

    public synchronized List<String> getToolIds() {
        ConfigurationSection section = config.getConfigurationSection(TOOLS_PATH);
        if (section == null) {
            return Collections.emptyList();
        }

        return section.getKeys(false).stream()
                .sorted(this::compareIds)
                .toList();
    }

    public String normalizeId(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }

    private int compareIds(String first, String second) {
        try {
            return Integer.compare(Integer.parseInt(first), Integer.parseInt(second));
        } catch (NumberFormatException exception) {
            return first.compareToIgnoreCase(second);
        }
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save " + FILE_NAME + ": " + exception.getMessage());
        }
    }
}

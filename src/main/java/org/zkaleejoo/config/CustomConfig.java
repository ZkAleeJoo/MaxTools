package org.zkaleejoo.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.zkaleejoo.MaxTools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@SuppressWarnings("null")
public class CustomConfig {
    private MaxTools plugin;
    private String fileName;
    private FileConfiguration fileConfiguration = null;
    private File file = null;
    private String folderName;
    private boolean newFile;

    public CustomConfig(String fileName, String folderName, MaxTools plugin, boolean newFile) {
        this.fileName = fileName;
        this.folderName = folderName;
        this.plugin = plugin;
        this.newFile = newFile;
    }

    public String getPath() {
        return this.fileName;
    }

    public void registerConfig() {
        if (folderName != null) {
            File folder = new File(plugin.getDataFolder(), folderName);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            file = new File(folder, fileName);
        } else {
            file = new File(plugin.getDataFolder(), fileName);
        }

        if (!file.exists()) {
            if (newFile) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                if (folderName != null) {
                    plugin.saveResource(folderName + File.separator + fileName, false);
                } else {
                    plugin.saveResource(fileName, false);
                }
            }
        }

        fileConfiguration = new YamlConfiguration();
        try {
            fileConfiguration.load(file);
            if (!newFile) {
                updateConfig();
            }
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public void updateConfig() {
        try {
            String resourcePath = (folderName != null) ? folderName + "/" + fileName : fileName;
            InputStream resourceStream = plugin.getResource(resourcePath);

            if (resourceStream == null)
                return;

            YamlConfiguration jarConfig = YamlConfiguration
                    .loadConfiguration(new InputStreamReader(resourceStream, StandardCharsets.UTF_8));
            Set<String> mapLikeSections = Set.of("milestones", "special-abilities");

            boolean changed = false;
            for (String key : jarConfig.getKeys(true)) {
                String rootKey = key.contains(".") ? key.substring(0, key.indexOf('.')) : key;
                if (mapLikeSections.contains(rootKey)
                        && key.contains(".")
                        && fileConfiguration.isConfigurationSection(rootKey)) {
                    continue;
                }
                if (!fileConfiguration.contains(key)) {
                    fileConfiguration.set(key, jarConfig.get(key));
                    changed = true;
                }
            }

            if (changed) {
                saveConfig();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveConfig() {
        try {
            fileConfiguration.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getConfig() {
        if (fileConfiguration == null) {
            reloadConfig();
        }
        return fileConfiguration;
    }

    public boolean reloadConfig() {
        if (folderName != null) {
            file = new File(plugin.getDataFolder() + File.separator + folderName, fileName);
        } else {
            file = new File(plugin.getDataFolder(), fileName);
        }

        fileConfiguration = YamlConfiguration.loadConfiguration(file);

        String resourcePath = (folderName != null) ? folderName + "/" + fileName : fileName;
        InputStream resourceStream = plugin.getResource(resourcePath);

        if (resourceStream != null) {
            YamlConfiguration defConfig = YamlConfiguration
                    .loadConfiguration(new InputStreamReader(resourceStream, StandardCharsets.UTF_8));
            fileConfiguration.setDefaults(defConfig);
        }

        return true;
    }
}
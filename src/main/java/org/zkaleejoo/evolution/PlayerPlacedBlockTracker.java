package org.zkaleejoo.evolution;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.zkaleejoo.MaxTools;

public class PlayerPlacedBlockTracker {

    private static final String FILE_NAME = "player-placed-blocks.yml";
    private static final String BLOCKS_PATH = "blocks";

    private final MaxTools plugin;
    private final File file;
    private final Set<String> placedBlocks = new HashSet<>();
    private boolean dirty;

    public PlayerPlacedBlockTracker(MaxTools plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), FILE_NAME);
        load();
    }

    public void markPlaced(Block block) {
        if (!isTrackable(block)) {
            return;
        }

        if (placedBlocks.add(locationKey(block))) {
            dirty = true;
        }
    }

    public boolean isPlayerPlacedBlock(Block block) {
        return isTrackable(block) && placedBlocks.contains(locationKey(block));
    }

    public void forget(Block block) {
        if (!isTrackable(block)) {
            return;
        }

        if (placedBlocks.remove(locationKey(block))) {
            dirty = true;
        }
    }

    public void saveIfDirty() {
        if (!dirty) {
            return;
        }

        save();
    }

    public void save() {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            YamlConfiguration config = new YamlConfiguration();
            config.set(BLOCKS_PATH, placedBlocks.stream().sorted().toList());
            config.save(file);
            dirty = false;
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save player placed block registry: " + exception.getMessage());
        }
    }

    private void load() {
        placedBlocks.clear();
        if (!file.exists()) {
            dirty = false;
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        placedBlocks.addAll(config.getStringList(BLOCKS_PATH));
        dirty = false;
    }

    private boolean isTrackable(Block block) {
        return block != null && block.getWorld() != null;
    }

    private String locationKey(Block block) {
        return block.getWorld().getUID()
                + ":" + block.getX()
                + ":" + block.getY()
                + ":" + block.getZ();
    }
}

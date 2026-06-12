package org.zkaleejoo.storage;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.bukkit.Material;
import org.zkaleejoo.MaxTools;

public class CustomToolDatabase implements AutoCloseable {

    private final MaxTools plugin;
    private Connection connection;
    private long lastPurgeAt;

    public CustomToolDatabase(MaxTools plugin) {
        this.plugin = plugin;
    }

    public synchronized void initialize() {
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create plugin data folder for custom tool database.");
            }

            File databaseFile = new File(plugin.getDataFolder(), "custom_tools.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS custom_tools (
                            tool_id TEXT PRIMARY KEY,
                            material TEXT NOT NULL,
                            owner_uuid TEXT,
                            owner_name TEXT,
                            test_tool INTEGER NOT NULL DEFAULT 0,
                            created_at INTEGER NOT NULL,
                            last_seen_at INTEGER NOT NULL
                        )
                        """);
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS custom_tool_metadata (
                            metadata_key TEXT PRIMARY KEY,
                            metadata_value TEXT NOT NULL
                        )
                        """);
            }
            lastPurgeAt = loadLastPurgeAt();
        } catch (SQLException exception) {
            plugin.getLogger().severe("Could not initialize SQLite custom tool database: " + exception.getMessage());
            closeQuietly();
        }
    }

    public synchronized boolean isAvailable() {
        return connection != null;
    }

    public synchronized void registerTool(String toolId, Material material, UUID ownerId, String ownerName,
            boolean testTool) {
        if (!isAvailable() || toolId == null || toolId.isBlank() || material == null) {
            return;
        }

        long now = System.currentTimeMillis();
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO custom_tools (tool_id, material, owner_uuid, owner_name, test_tool, created_at, last_seen_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(tool_id) DO UPDATE SET
                    material = excluded.material,
                    owner_uuid = COALESCE(excluded.owner_uuid, custom_tools.owner_uuid),
                    owner_name = COALESCE(excluded.owner_name, custom_tools.owner_name),
                    test_tool = excluded.test_tool,
                    last_seen_at = excluded.last_seen_at
                """)) {
            statement.setString(1, toolId);
            statement.setString(2, material.name());
            statement.setString(3, ownerId == null ? null : ownerId.toString());
            statement.setString(4, ownerName == null || ownerName.isBlank() ? null : ownerName);
            statement.setInt(5, testTool ? 1 : 0);
            statement.setLong(6, now);
            statement.setLong(7, now);
            statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not register custom tool " + toolId + ": " + exception.getMessage());
        }
    }

    public synchronized boolean containsTool(String toolId) {
        if (!isAvailable() || toolId == null || toolId.isBlank()) {
            return false;
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM custom_tools WHERE tool_id = ? LIMIT 1")) {
            statement.setString(1, toolId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not read custom tool " + toolId + ": " + exception.getMessage());
            return false;
        }
    }

    public synchronized void markSeen(String toolId, Material material) {
        if (!isAvailable() || toolId == null || toolId.isBlank()) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE custom_tools SET material = COALESCE(?, material), last_seen_at = ? WHERE tool_id = ?")) {
            statement.setString(1, material == null ? null : material.name());
            statement.setLong(2, System.currentTimeMillis());
            statement.setString(3, toolId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not update custom tool " + toolId + ": " + exception.getMessage());
        }
    }

    public synchronized int countTools() {
        if (!isAvailable()) {
            return 0;
        }

        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM custom_tools")) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not count custom tools: " + exception.getMessage());
            return 0;
        }
    }

    public synchronized int deleteAllTools() {
        if (!isAvailable()) {
            return 0;
        }

        try (Statement statement = connection.createStatement()) {
            int deleted = statement.executeUpdate("DELETE FROM custom_tools");
            lastPurgeAt = System.currentTimeMillis();
            saveMetadata("last_purge_at", String.valueOf(lastPurgeAt));
            return deleted;
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not clear custom tool database: " + exception.getMessage());
            return 0;
        }
    }

    public synchronized long getLastPurgeAt() {
        return lastPurgeAt;
    }

    @Override
    public synchronized void close() {
        closeQuietly();
    }

    private void closeQuietly() {
        if (connection == null) {
            return;
        }

        try {
            connection.close();
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not close custom tool database: " + exception.getMessage());
        } finally {
            connection = null;
        }
    }

    private long loadLastPurgeAt() {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT metadata_value FROM custom_tool_metadata WHERE metadata_key = ?")) {
            statement.setString(1, "last_purge_at");
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return 0L;
                }
                return Long.parseLong(resultSet.getString(1));
            }
        } catch (SQLException | NumberFormatException exception) {
            plugin.getLogger().warning("Could not read custom tool purge metadata: " + exception.getMessage());
            return 0L;
        }
    }

    private void saveMetadata(String key, String value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO custom_tool_metadata (metadata_key, metadata_value)
                VALUES (?, ?)
                ON CONFLICT(metadata_key) DO UPDATE SET metadata_value = excluded.metadata_value
                """)) {
            statement.setString(1, key);
            statement.setString(2, value);
            statement.executeUpdate();
        }
    }
}

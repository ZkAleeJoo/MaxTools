package org.zkaleejoo.utils;

import org.bukkit.Bukkit;
import org.zkaleejoo.MaxTools;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class UpdateChecker {

    private static final String GITHUB_VERSION_URL = "https://gist.githubusercontent.com/ZkAleeJoo/29cb10190b642f4dded842d731a044e0/raw/MaxTools-Version.txt";
    private final MaxTools plugin;

    public UpdateChecker(MaxTools plugin) {
        this.plugin = plugin;
    }

    public void getVersion(final Consumer<String> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            HttpURLConnection connection = null;
            try {
                URL url = URI.create(GITHUB_VERSION_URL).toURL();
                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "MaxTools-UpdateChecker");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int statusCode = connection.getResponseCode();
                if (statusCode < 200 || statusCode >= 300) {
                    plugin.getLogger().warning("Could not check updates from BBB. HTTP " + statusCode);
                    return;
                }

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String latestVersion = reader.readLine();
                    if (latestVersion != null && !latestVersion.isBlank()) {
                        String trimmedVersion = latestVersion.trim();
                        if (plugin.isEnabled()) {
                            Bukkit.getScheduler().runTask(plugin, () -> consumer.accept(trimmedVersion));
                        }
                    } else {
                        plugin.getLogger().info("The version in BBB is empty.");
                    }
                }
            } catch (Exception exception) {
                plugin.getLogger().info("Could not connect to BBB to check for updates: " + exception.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }
}
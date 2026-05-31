package org.zkaleejoo.utils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.zkaleejoo.MaxTools;
import org.zkaleejoo.config.MainConfigManager;

public class DiscordWebhookNotifier {

    private static final int MAX_ATTEMPTS = 3;
    private static final long[] BACKOFF_MILLIS = { 1000L, 2000L, 4000L };
    private static final String EVENT_MILESTONE_UNLOCKED = "milestone-unlocked";
    private static final String EVENT_ABILITY_UNLOCKED = "ability-unlocked";
    private static final String EVENT_TEST_MESSAGE = "test-message";
    private static final String DEFAULT_TEMPLATE = "**{player}** - {tool} - {blocks} blocks - {ability} - {timestamp}";

    private final MaxTools plugin;
    private final HttpClient httpClient;
    private final ExecutorService executor;
    private final AtomicInteger droppedTasks = new AtomicInteger(0);
    private volatile boolean disabledByInvalidWebhook = false;

    public DiscordWebhookNotifier(MaxTools plugin, int maxPendingTasks) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newHttpClient();
        this.executor = new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(Math.max(16, maxPendingTasks)),
                r -> {
                    Thread thread = new Thread(r, "met-discord-webhook");
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    public void notifyMilestoneUnlocked(Player player, Material toolType, int blocks, String reward) {
        MainConfigManager config = plugin.getConfigManager();
        if (!config.isDiscordEnabled() || !config.isDiscordMilestoneEnabled()) {
            return;
        }
        sendAsync(buildPayload(EVENT_MILESTONE_UNLOCKED, config.getDiscordMilestoneTemplate(),
                createContext(player, toolType, blocks, reward)));
    }

    public void notifyAbilityUnlocked(Player player, Material toolType, int blocks, String ability) {
        MainConfigManager config = plugin.getConfigManager();
        if (!config.isDiscordEnabled() || !config.isDiscordAbilityEnabled()) {
            return;
        }
        sendAsync(buildPayload(EVENT_ABILITY_UNLOCKED, config.getDiscordAbilityTemplate(),
                createContext(player, toolType, blocks, ability)));
    }

    public boolean sendTestMessage(String playerName, String toolName, int blocks) {
        MainConfigManager config = plugin.getConfigManager();
        if (!config.isDiscordEnabled()) {
            return false;
        }

        DiscordMessageContext context = new DiscordMessageContext(
                playerName,
                "",
                toolName,
                "",
                Math.max(0, blocks),
                "test",
                Instant.now().toString(),
                config.getDiscordServerName(),
                Bukkit.getServer().getName());
        return sendAsync(buildPayload(EVENT_TEST_MESSAGE, config.getDiscordTestTemplate(), context));
    }

    private DiscordMessageContext createContext(Player player, Material toolType, int blocks, String abilityValue) {
        MainConfigManager config = plugin.getConfigManager();
        return new DiscordMessageContext(
                player.getName(),
                player.getUniqueId().toString(),
                config.getToolName(toolType),
                toolType == null ? "" : toolType.name().toLowerCase(Locale.ROOT),
                Math.max(0, blocks),
                abilityValue == null ? "-" : abilityValue,
                Instant.now().toString(),
                config.getDiscordServerName(),
                Bukkit.getServer().getName());
    }

    private String buildPayload(String eventName, String legacyTemplate, DiscordMessageContext context) {
        FileConfiguration config = plugin.getConfigManager().getMainConfig();
        boolean embedsEnabled = config.getBoolean("discord.embeds.enabled", false);
        String eventPath = "discord.embeds." + eventName;
        boolean eventEmbedEnabled = embedsEnabled && config.getBoolean(eventPath + ".enabled", true);

        String content = eventEmbedEnabled
                ? applyPlaceholders(config.getString(eventPath + ".content", ""), context)
                : buildContent(legacyTemplate, context);

        StringBuilder payload = new StringBuilder("{");
        appendStringField(payload, "username", applyPlaceholders(config.getString("discord.username", ""), context));
        appendStringField(payload, "avatar_url",
                applyPlaceholders(config.getString("discord.avatar-url", ""), context));
        appendAllowedMentions(payload, config);

        if (!content.isBlank()) {
            appendStringField(payload, "content", content);
        }

        if (eventEmbedEnabled) {
            String embedJson = buildEmbed(config, eventPath, context);
            if (embedJson != null) {
                appendRawField(payload, "embeds", "[" + embedJson + "]");
            }
        }

        String payloadSoFar = payload.toString();
        if (!payloadSoFar.contains("\"content\"") && !payloadSoFar.contains("\"embeds\"")) {
            appendStringField(payload, "content", buildContent(legacyTemplate, context));
        }

        payload.append('}');
        return payload.toString();
    }

    private String buildContent(String template, DiscordMessageContext context) {
        String configuredTemplate = (template == null || template.isBlank())
                ? DEFAULT_TEMPLATE
                : template;
        return applyPlaceholders(configuredTemplate, context);
    }

    private String buildEmbed(FileConfiguration config, String eventPath, DiscordMessageContext context) {
        StringBuilder embed = new StringBuilder("{");

        appendStringField(embed, "title", applyPlaceholders(config.getString(eventPath + ".title", ""), context));
        appendStringField(embed, "description",
                applyPlaceholders(config.getString(eventPath + ".description", ""), context));
        appendStringField(embed, "url", applyPlaceholders(config.getString(eventPath + ".url", ""), context));

        int color = parseColor(config.getString(eventPath + ".color", ""));
        if (color >= 0) {
            appendRawField(embed, "color", String.valueOf(color));
        }

        if (config.getBoolean(eventPath + ".timestamp", true)) {
            appendStringField(embed, "timestamp", context.timestamp());
        }

        appendAuthor(embed, config, eventPath, context);
        appendImageObject(embed, "thumbnail", applyPlaceholders(config.getString(eventPath + ".thumbnail-url", ""),
                context));
        appendImageObject(embed, "image", applyPlaceholders(config.getString(eventPath + ".image-url", ""), context));
        appendFooter(embed, config, eventPath, context);
        appendFields(embed, config, eventPath, context);

        if (embed.length() == 1) {
            return null;
        }

        embed.append('}');
        return embed.toString();
    }

    private void appendAuthor(StringBuilder embed, FileConfiguration config, String eventPath,
            DiscordMessageContext context) {
        String name = applyPlaceholders(config.getString(eventPath + ".author.name", ""), context);
        if (name.isBlank()) {
            return;
        }

        StringBuilder author = new StringBuilder("{");
        appendStringField(author, "name", name);
        appendStringField(author, "url", applyPlaceholders(config.getString(eventPath + ".author.url", ""), context));
        appendStringField(author, "icon_url",
                applyPlaceholders(config.getString(eventPath + ".author.icon-url", ""), context));
        author.append('}');
        appendRawField(embed, "author", author.toString());
    }

    private void appendFooter(StringBuilder embed, FileConfiguration config, String eventPath,
            DiscordMessageContext context) {
        String text = applyPlaceholders(config.getString(eventPath + ".footer.text", ""), context);
        if (text.isBlank()) {
            return;
        }

        StringBuilder footer = new StringBuilder("{");
        appendStringField(footer, "text", text);
        appendStringField(footer, "icon_url",
                applyPlaceholders(config.getString(eventPath + ".footer.icon-url", ""), context));
        footer.append('}');
        appendRawField(embed, "footer", footer.toString());
    }

    private void appendFields(StringBuilder embed, FileConfiguration config, String eventPath,
            DiscordMessageContext context) {
        List<Map<?, ?>> configuredFields = config.getMapList(eventPath + ".fields");
        if (configuredFields.isEmpty()) {
            return;
        }

        List<String> fieldJson = new ArrayList<>();
        for (Map<?, ?> configuredField : configuredFields) {
            String name = applyPlaceholders(Objects.toString(configuredField.get("name"), ""), context);
            String value = applyPlaceholders(Objects.toString(configuredField.get("value"), ""), context);
            if (name.isBlank() || value.isBlank()) {
                continue;
            }

            StringBuilder field = new StringBuilder("{");
            appendStringField(field, "name", name);
            appendStringField(field, "value", value);
            appendRawField(field, "inline", String.valueOf(Boolean.parseBoolean(
                    Objects.toString(configuredField.get("inline"), "false"))));
            field.append('}');
            fieldJson.add(field.toString());
        }

        if (!fieldJson.isEmpty()) {
            appendRawField(embed, "fields", "[" + String.join(",", fieldJson) + "]");
        }
    }

    private void appendImageObject(StringBuilder json, String key, String url) {
        if (url == null || url.isBlank()) {
            return;
        }

        StringBuilder image = new StringBuilder("{");
        appendStringField(image, "url", url);
        image.append('}');
        appendRawField(json, key, image.toString());
    }

    private void appendAllowedMentions(StringBuilder payload, FileConfiguration config) {
        List<String> parseValues = config.getStringList("discord.allowed-mentions.parse").stream()
                .map(value -> value.toLowerCase(Locale.ROOT).trim())
                .filter(value -> value.equals("roles") || value.equals("users") || value.equals("everyone"))
                .distinct()
                .toList();

        StringBuilder parseArray = new StringBuilder("[");
        for (String value : parseValues) {
            appendRawArrayValue(parseArray, "\"" + escapeJson(value) + "\"");
        }
        parseArray.append(']');

        StringBuilder allowedMentions = new StringBuilder("{");
        appendRawField(allowedMentions, "parse", parseArray.toString());
        appendRawField(allowedMentions, "replied_user",
                String.valueOf(config.getBoolean("discord.allowed-mentions.replied-user", false)));
        allowedMentions.append('}');
        appendRawField(payload, "allowed_mentions", allowedMentions.toString());
    }

    private String applyPlaceholders(String template, DiscordMessageContext context) {
        if (template == null) {
            return "";
        }

        return template
                .replace("{player}", context.playerName())
                .replace("{player_uuid}", context.playerUuid())
                .replace("{tool}", context.toolName())
                .replace("{tool_type}", context.toolType())
                .replace("{blocks}", String.valueOf(context.blocks()))
                .replace("{ability}", context.ability())
                .replace("{reward}", context.ability())
                .replace("{timestamp}", context.timestamp())
                .replace("{server}", context.serverName())
                .replace("{server_software}", context.serverSoftware())
                .replace("{plugin}", plugin.getName());
    }

    private int parseColor(String configuredColor) {
        if (configuredColor == null || configuredColor.isBlank()) {
            return -1;
        }

        String normalized = configuredColor.trim();
        boolean hexadecimal = false;
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
            hexadecimal = true;
        } else if (normalized.toLowerCase(Locale.ROOT).startsWith("0x")) {
            normalized = normalized.substring(2);
            hexadecimal = true;
        }

        try {
            int color = hexadecimal
                    ? Integer.parseInt(normalized, 16)
                    : Integer.parseInt(normalized);
            return Math.max(0, Math.min(0xFFFFFF, color));
        } catch (NumberFormatException ignored) {
            try {
                int color = Integer.parseInt(normalized, 16);
                return Math.max(0, Math.min(0xFFFFFF, color));
            } catch (NumberFormatException ignoredAgain) {
                return -1;
            }
        }
    }

    private boolean sendAsync(String payload) {
        if (disabledByInvalidWebhook) {
            return false;
        }

        MainConfigManager config = plugin.getConfigManager();
        String webhook = config.getDiscordWebhookUrl();
        if (webhook == null || webhook.isBlank()) {
            return false;
        }

        URI webhookUri;
        try {
            webhookUri = new URI(webhook.trim());
        } catch (URISyntaxException ex) {
            plugin.getLogger().warning("Discord webhook URL is invalid. Discord notifications were disabled.");
            disabledByInvalidWebhook = true;
            return false;
        }

        if (!isHttpUri(webhookUri)) {
            plugin.getLogger().warning("Discord webhook URL must use http or https. Discord notifications were disabled.");
            disabledByInvalidWebhook = true;
            return false;
        }

        try {
            executor.execute(() -> sendWithRetry(webhookUri, payload));
            return true;
        } catch (RejectedExecutionException ex) {
            int dropped = droppedTasks.incrementAndGet();
            if (dropped % 100 == 0) {
                plugin.getLogger().warning("Discord queue is full. Dropped tasks: " + dropped);
            }
            return false;
        }
    }

    private void sendWithRetry(URI webhookUri, String payload) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            if (disabledByInvalidWebhook || !plugin.isEnabled()) {
                return;
            }

            HttpRequest request = HttpRequest.newBuilder(webhookUri)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            try {
                HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
                int status = response.statusCode();
                if (status >= 200 && status < 300) {
                    return;
                }

                if (isInvalidWebhookStatus(status)) {
                    plugin.getLogger().warning("Discord rejected the webhook URL with status " + status
                            + ". Discord notifications were disabled.");
                    disabledByInvalidWebhook = true;
                    return;
                }

                if (!isRetryableStatus(status) || attempt == MAX_ATTEMPTS) {
                    if (!isRetryableStatus(status)) {
                        plugin.getLogger().warning("Discord webhook request failed with status " + status + ".");
                    }
                    return;
                }
            } catch (IOException ex) {
                if (attempt == MAX_ATTEMPTS) {
                    plugin.getLogger().warning("Discord webhook request failed: " + ex.getMessage());
                    return;
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }

            sleepBeforeRetry(attempt);
        }
    }

    private void sleepBeforeRetry(int attempt) {
        int index = Math.max(0, Math.min(BACKOFF_MILLIS.length - 1, attempt - 1));
        try {
            Thread.sleep(BACKOFF_MILLIS[index]);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean isRetryableStatus(int statusCode) {
        return statusCode == 429 || statusCode >= 500;
    }

    private boolean isInvalidWebhookStatus(int statusCode) {
        return statusCode == 401 || statusCode == 403 || statusCode == 404 || statusCode == 410;
    }

    private boolean isHttpUri(URI uri) {
        String scheme = uri.getScheme();
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }

    private void appendStringField(StringBuilder json, String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        appendRawField(json, key, "\"" + escapeJson(value) + "\"");
    }

    private void appendRawField(StringBuilder json, String key, String rawValue) {
        appendSeparator(json);
        json.append('"').append(escapeJson(key)).append("\":").append(rawValue);
    }

    private void appendRawArrayValue(StringBuilder json, String rawValue) {
        appendSeparator(json);
        json.append(rawValue);
    }

    private void appendSeparator(StringBuilder json) {
        char last = json.charAt(json.length() - 1);
        if (last != '{' && last != '[') {
            json.append(',');
        }
    }

    private String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder();
        for (char c : value.toCharArray()) {
            switch (c) {
                case '\"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20) {
                        escaped.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.toString();
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void resetInvalidWebhookState() {
        disabledByInvalidWebhook = false;
    }

    private record DiscordMessageContext(
            String playerName,
            String playerUuid,
            String toolName,
            String toolType,
            int blocks,
            String ability,
            String timestamp,
            String serverName,
            String serverSoftware) {
    }
}

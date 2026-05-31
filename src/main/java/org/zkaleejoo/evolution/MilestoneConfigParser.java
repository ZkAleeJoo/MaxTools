package org.zkaleejoo.evolution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

final class MilestoneConfigParser {

    private MilestoneConfigParser() {
    }

    static List<EvolutionMilestone> parse(FileConfiguration config, String path, Consumer<String> warningSink) {
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            return Collections.emptyList();
        }

        List<EvolutionMilestone> parsed = new ArrayList<>();

        for (String key : section.getKeys(false)) {
            String milestonePath = path + "." + key;
            if (!isExplicitlySet(config, milestonePath + ".blocks")) {
                continue;
            }

            int blocks = config.getInt(milestonePath + ".blocks", -1);
            if (blocks <= 0) {
                warningSink.accept("Invalid milestone blocks value in key " + milestonePath);
                continue;
            }

            String enchantment = getExplicitString(config, milestonePath + ".enchantment", "");
            int level = Math.max(1, getExplicitInt(config, milestonePath + ".level", 1));
            List<String> unlockAbilities = getExplicitStringList(config, milestonePath + ".unlock-abilities");

            if (unlockAbilities.isEmpty() && getExplicitBoolean(config, milestonePath + ".unlock-special", false)) {
                unlockAbilities = List.of("self-repair");
            }

            parsed.add(new EvolutionMilestone(blocks, enchantment, level, normalizeAbilityIds(unlockAbilities)));
        }

        parsed.sort(Comparator.comparingInt(EvolutionMilestone::blocksRequired));
        return parsed;
    }

    private static boolean isExplicitlySet(FileConfiguration config, String path) {
        return config.contains(path, true);
    }

    private static String getExplicitString(FileConfiguration config, String path, String fallback) {
        return isExplicitlySet(config, path) ? config.getString(path, fallback) : fallback;
    }

    private static int getExplicitInt(FileConfiguration config, String path, int fallback) {
        return isExplicitlySet(config, path) ? config.getInt(path, fallback) : fallback;
    }

    private static boolean getExplicitBoolean(FileConfiguration config, String path, boolean fallback) {
        return isExplicitlySet(config, path) ? config.getBoolean(path, fallback) : fallback;
    }

    private static List<String> getExplicitStringList(FileConfiguration config, String path) {
        return isExplicitlySet(config, path) ? config.getStringList(path) : Collections.emptyList();
    }

    private static List<String> normalizeAbilityIds(List<String> ids) {
        return ids.stream()
                .filter(Objects::nonNull)
                .map(id -> id.trim().toLowerCase(Locale.ROOT))
                .filter(id -> !id.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }
}

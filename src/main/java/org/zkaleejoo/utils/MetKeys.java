package org.zkaleejoo.utils;

import java.util.Locale;
import org.bukkit.NamespacedKey;
import org.zkaleejoo.MaxTools;

public final class MetKeys {

    public static final String MENU_TARGET_PAGE = "menu_target_page";
    public static final String MENU_OPEN_ABILITIES = "menu_open_abilities";
    public static final String MENU_OPEN_STATS = "menu_open_stats";
    public static final String MENU_NON_MOVABLE = "menu_non_movable";
    public static final String BLOCKS_MINED = "blocks_mined";
    public static final String SPECIAL_UNLOCKED = "special_unlocked";
    public static final String UNLOCKED_ABILITIES = "unlocked_abilities";
    public static final String MANAGED_LORE_LINES = "managed_lore_lines";
    public static final String BASE_DISPLAY_NAME = "base_display_name";
    public static final String LAST_PROGRESS_DISPLAY_NAME = "last_progress_display_name";
    public static final String LAST_APPLIED_MILESTONE = "last_applied_milestone";
    public static final String LAST_LORE_PROGRESS_USAGE = "last_lore_progress_usage";
    public static final String LAST_LORE_PROGRESS_PERCENT = "last_lore_progress_percent";
    public static final String LAST_LORE_PROGRESS_TARGET = "last_lore_progress_target";
    public static final String ABILITY_ACTIVATIONS_TOTAL = "ability_activations_total";
    public static final String CUSTOM_TOOL_ID = "custom_tool_id";
    public static final String TEST_MODE = "test_mode";
    public static final String TEST_TOOL_ID = "test_tool_id";

    private MetKeys() {
    }

    public static NamespacedKey key(MaxTools plugin, String key) {
        return new NamespacedKey(plugin, key);
    }

    public static NamespacedKey abilityActivationKey(MaxTools plugin, String abilityId) {
        return key(plugin, "ability_activations_" + normalizeAbilityId(abilityId));
    }

    public static NamespacedKey abilityCooldownKey(MaxTools plugin, String abilityId) {
        return key(plugin, "ability_cooldown_" + normalizeAbilityId(abilityId));
    }

    public static NamespacedKey abilityDistanceKey(MaxTools plugin, String abilityId) {
        return key(plugin, "ability_distance_" + normalizeAbilityId(abilityId));
    }

    private static String normalizeAbilityId(String abilityId) {
        return abilityId == null ? "unknown" : abilityId.toLowerCase(Locale.ROOT).replace('-', '_');
    }
}

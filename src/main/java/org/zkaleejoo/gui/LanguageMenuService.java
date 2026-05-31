package org.zkaleejoo.gui;

import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.zkaleejoo.MaxTools;

public class LanguageMenuService implements Listener {

    private static final int ENGLISH_SLOT = 11;
    private static final int SPANISH_SLOT = 15;
    private static final String ENGLISH_TEXTURE_URL = "http://textures.minecraft.net/texture/b656f0b0343e143bd8b3bc2cce3ea7de44a9f5b6b71cb674b9c2d4bdcb495468";
    private static final String SPANISH_TEXTURE_URL = "http://textures.minecraft.net/texture/a20f3e49dd01bde56b39b458a4d4582424a5f43a78ad8b5f6c6b2f7c4b64e91";

    private static String toTextureValue(String url) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes());
    }

    private final MaxTools plugin;

    public LanguageMenuService(MaxTools plugin) {
        this.plugin = plugin;
    }

    public void openMenu(Player player) {
        LanguageMenuHolder holder = new LanguageMenuHolder();
        String title = plugin.getConfigManager().getGuiString("menu-layouts.language-menu.title",
                plugin.getConfigManager().isSpanishLanguage() ? "&8Selecciona idioma" : "&8Select language");
        int size = plugin.getConfigManager().getGuiInventorySize("menu-layouts.language-menu.size", 27);
        Component titleComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(title);
        Inventory inventory = Bukkit.createInventory(holder, size, titleComponent);
        holder.bindInventory(inventory);

        inventory.setItem(getLanguageSlot("english", ENGLISH_SLOT),
                buildLanguageHead("en",
                        plugin.getConfigManager().getGuiString("menu-layouts.language-menu.options.english.title",
                                "&bEnglish"),
                        plugin.getConfigManager().getGuiString("menu-layouts.language-menu.options.english.texture",
                                ENGLISH_TEXTURE_URL),
                        plugin.getConfigManager().getGuiStringList("menu-layouts.language-menu.options.english.lore",
                                List.of("&7Switch plugin language to English.", "&8Hardcoded option"))));
        inventory.setItem(getLanguageSlot("spanish", SPANISH_SLOT),
                buildLanguageHead("es",
                        plugin.getConfigManager().getGuiString("menu-layouts.language-menu.options.spanish.title",
                                "&bEspañol"),
                        plugin.getConfigManager().getGuiString("menu-layouts.language-menu.options.spanish.texture",
                                SPANISH_TEXTURE_URL),
                        plugin.getConfigManager().getGuiStringList("menu-layouts.language-menu.options.spanish.lore",
                                List.of("&7Cambiar el idioma del plugin a Español.", "&8Opción hardcodeada"))));

        player.openInventory(inventory);
        Location location = player.getLocation();
        if (location != null) {
            player.playSound(location, Objects.requireNonNull(Sound.UI_BUTTON_CLICK), 0.7f, 1.2f);
        }
    }

    @SuppressWarnings("null")
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getView().getTopInventory().getHolder() instanceof LanguageMenuHolder)) {
            return;
        }

        event.setCancelled(true);
        if (event.getClickedInventory() == null
                || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }

        String language = null;
        if (event.getSlot() == getLanguageSlot("english", ENGLISH_SLOT)) {
            language = "en";
        } else if (event.getSlot() == getLanguageSlot("spanish", SPANISH_SLOT)) {
            language = "es";
        }

        if (language == null) {
            return;
        }

        boolean changed = plugin.getConfigManager().setLanguage(language);
        if (changed) {
            if (plugin.getEvolutionMenuService() != null) {
                plugin.getEvolutionMenuService().resetActiveMenuSessions();
            }
            plugin.recreateDiscordWebhookNotifier();
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgLanguageChanged()));
        } else {
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgLanguageAlreadySelected()));
        }
        player.closeInventory();
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.3f);
    }

    private int getLanguageSlot(String key, int fallback) {
        return Math.max(0,
                plugin.getConfigManager().getGuiInt("menu-layouts.language-menu.options." + key + ".slot", fallback));
    }

    @SuppressWarnings("null")
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof LanguageMenuHolder
                && event.getPlayer() instanceof Player player) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.9f);
        }
    }

    @SuppressWarnings("null")
    private ItemStack buildLanguageHead(String languageCode, String displayName, String texture, List<String> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta rawMeta = item.getItemMeta();
        if (!(rawMeta instanceof SkullMeta meta)) {
            return item;
        }

        try {
            PlayerProfile profile = Bukkit
                    .createProfile(UUID.nameUUIDFromBytes(("met-lang-" + languageCode).getBytes()));
            profile.setProperty(new ProfileProperty("textures", toTextureValue(texture)));
            meta.setPlayerProfile(profile);
        } catch (Exception ignored) {
            return item;
        }

        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(displayName));
        meta.lore(lore.stream()
                .map(LegacyComponentSerializer.legacyAmpersand()::deserialize)
                .toList());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }
}

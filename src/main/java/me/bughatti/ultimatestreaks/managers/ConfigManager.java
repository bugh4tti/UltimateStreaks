package me.bughatti.ultimatestreaks.managers;

import me.bughatti.ultimatestreaks.UltimateStreaks;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigManager {

    private final UltimateStreaks plugin;
    private FileConfiguration config;

    private static final Pattern HEX_PATTERN_AMP = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern HEX_PATTERN_X = Pattern.compile("&x(&[A-Fa-f0-9]){6}");

    public ConfigManager(UltimateStreaks plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public int getMaxStreakDays() {
        return config.getInt("settings.max-streak-days", 50);
    }

    public int getStreakResetHours() {
        return config.getInt("settings.streak-reset-hours", 24);
    }

    public String getMessage(String path) {
        String raw = config.getString("messages." + path, "");
        String prefix = config.getString("messages.prefix", "");
        return colorize(prefix + raw);
    }

    public String getMessageNoPrefix(String path) {
        String raw = config.getString("messages." + path, "");
        return colorize(raw);
    }

    public ConfigurationSection getStreakSection(String dayKey) {
        return config.getConfigurationSection("streaks." + dayKey);
    }

    public String getGuiTitle(int page) {
        String raw = config.getString("gui.title", "&8Rachas - Página {page}");
        return colorize(raw.replace("{page}", String.valueOf(page)));
    }

    public int getGuiSize() {
        return config.getInt("gui.size", 54);
    }

    /**
     * Traduce colores clásicos (&) y hex (&#RRGGBB y &x&R&R&G&G&B&B)
     */
    public String colorize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Soporte &x&R&R&G&G&B&B
        Matcher matcherX = HEX_PATTERN_X.matcher(text);
        while (matcherX.find()) {
            String hexCode = matcherX.group().replace("&x", "").replace("&", "");
            text = text.replace(matcherX.group(), net.md_5.bungee.api.ChatColor.of("#" + hexCode).toString());
            matcherX = HEX_PATTERN_X.matcher(text);
        }

        // Soporte &#RRGGBB
        Matcher matcherAmp = HEX_PATTERN_AMP.matcher(text);
        while (matcherAmp.find()) {
            String hexCode = matcherAmp.group(1);
            text = text.replace(matcherAmp.group(), net.md_5.bungee.api.ChatColor.of("#" + hexCode).toString());
            matcherAmp = HEX_PATTERN_AMP.matcher(text);
        }

        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public List<String> colorizeList(List<String> list) {
        List<String> result = new ArrayList<>();
        if (list == null) return result;
        for (String line : list) {
            result.add(colorize(line));
        }
        return result;
    }
    }

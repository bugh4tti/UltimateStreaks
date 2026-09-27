package me.bughatti.ultimatestreaks.managers;

import me.bughatti.ultimatestreaks.UltimateStreaks;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class DataManager {

    private final UltimateStreaks plugin;
    private File dataFile;
    private FileConfiguration dataConfig;

    public DataManager(UltimateStreaks plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("No se pudo crear playerdata.yml: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void saveAll() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("No se pudo guardar playerdata.yml: " + e.getMessage());
        }
    }

    private String path(UUID uuid) {
        return "players." + uuid.toString();
    }

    public int getCurrentDay(UUID uuid) {
        return dataConfig.getInt(path(uuid) + ".current-day", 1);
    }

    public void setCurrentDay(UUID uuid, int day) {
        dataConfig.set(path(uuid) + ".current-day", day);
        saveAll();
    }

    public long getLastClaim(UUID uuid) {
        return dataConfig.getLong(path(uuid) + ".last-claim", 0L);
    }

    public void setLastClaim(UUID uuid, long timestamp) {
        dataConfig.set(path(uuid) + ".last-claim", timestamp);
        saveAll();
    }

    /**
     * Devuelve true si el jugador puede reclamar su racha ahora
     */
    public boolean canClaim(UUID uuid) {
        long last = getLastClaim(uuid);
        if (last == 0L) return true;

        int resetHours = plugin.getConfigManager().getStreakResetHours();
        long elapsed = System.currentTimeMillis() - last;
        long required = TimeUnit.HOURS.toMillis(resetHours);

        return elapsed >= required;
    }

    /**
     * Tiempo restante en formato legible (ej: "3h 25m")
     */
    public String getTimeUntilNextClaim(UUID uuid) {
        long last = getLastClaim(uuid);
        int resetHours = plugin.getConfigManager().getStreakResetHours();
        long required = TimeUnit.HOURS.toMillis(resetHours);
        long elapsed = System.currentTimeMillis() - last;
        long remaining = required - elapsed;

        if (remaining <= 0) return "0m";

        long hours = TimeUnit.MILLISECONDS.toHours(remaining);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60;

        return hours + "h " + minutes + "m";
    }

    /**
     * Procesa el reclamo de racha: avanza el día, guarda el timestamp.
     * Si el jugador se pasó del reset-hours * 2 (no reclamó a tiempo), reinicia a día 1.
     */
    public int processClaim(Player player) {
        UUID uuid = player.getUniqueId();
        int maxDays = plugin.getConfigManager().getMaxStreakDays();
        int currentDay = getCurrentDay(uuid);
        long last = getLastClaim(uuid);
        int resetHours = plugin.getConfigManager().getStreakResetHours();

        if (last != 0L) {
            long elapsed = System.currentTimeMillis() - last;
            long breakLimit = TimeUnit.HOURS.toMillis((long) resetHours * 2);
            if (elapsed > breakLimit) {
                currentDay = 1; // se rompió la racha
            }
        }

        int dayToGive = currentDay;

        int nextDay = currentDay + 1;
        if (nextDay > maxDays) {
            nextDay = 1; // reinicia el ciclo al llegar al máximo
        }

        setCurrentDay(uuid, nextDay);
        setLastClaim(uuid, System.currentTimeMillis());

        return dayToGive;
    }

    public void give(Player player, int day) {
        UUID uuid = player.getUniqueId();
        setCurrentDay(uuid, day);
    }
  }

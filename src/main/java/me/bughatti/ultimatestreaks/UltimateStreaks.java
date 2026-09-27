package me.bughatti.ultimatestreaks;

import me.bughatti.ultimatestreaks.commands.StreaksCommand;
import me.bughatti.ultimatestreaks.commands.StreaksTabCompleter;
import me.bughatti.ultimatestreaks.managers.ConfigManager;
import me.bughatti.ultimatestreaks.managers.DataManager;
import me.bughatti.ultimatestreaks.managers.GUIManager;
import org.bukkit.plugin.java.JavaPlugin;

public class UltimateStreaks extends JavaPlugin {

    private static UltimateStreaks instance;

    private ConfigManager configManager;
    private DataManager dataManager;
    private GUIManager guiManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.dataManager = new DataManager(this);
        this.guiManager = new GUIManager(this);

        StreaksCommand command = new StreaksCommand(this);
        getCommand("ultimatestreaks").setExecutor(command);
        getCommand("ultimatestreaks").setTabCompleter(new StreaksTabCompleter(this));

        getServer().getPluginManager().registerEvents(guiManager, this);

        getLogger().info("UltimateStreaks habilitado correctamente.");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveAll();
        }
        getLogger().info("UltimateStreaks deshabilitado.");
    }

    public static UltimateStreaks getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public GUIManager getGuiManager() {
        return guiManager;
    }
          }

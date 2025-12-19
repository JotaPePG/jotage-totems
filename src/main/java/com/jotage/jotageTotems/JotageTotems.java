package com.jotage.jotageTotems;

import com.jotage.jotageTotems.listeners.TotemListener;
import com.jotage.jotageTotems.managers.*;
import com.jotage.jotageTotems.ui.TotemMenu;
import org.bukkit.plugin.java.JavaPlugin;

public final class JotageTotems extends JavaPlugin {

    private ConfigManager configManager;
    private MessageManager messageManager;
    private RecipeManager recipeManager;
    private PlayerDataManager playerDataManager;
    private TotemManager totemManager;
    private TeleportHandler teleportHandler;
    private TotemMenu totemMenu;

    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";

    private static JotageTotems instance;

    public static JotageTotems getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info(YELLOW + "Iniciando Jotage Totems v" + getDescription().getVersion() + "..." + RESET);
        getLogger().info("Carregando configurações...");

        try {
            // Loads config.yml
            configManager = new ConfigManager(this);
            getLogger().info(GREEN + "✓ ConfigManager carregado" + RESET);
            configManager.loadConfig();

            // Loads messages.yml
            messageManager = new MessageManager(this);
            getLogger().info(GREEN + "✓ MessageManager carregado" + RESET);

            // Loads totemManager
            totemManager = new TotemManager(this);
            getLogger().info(GREEN + "✓ TotemManager carregado" + RESET);

            // Loads playerDataManager
            playerDataManager = new PlayerDataManager(this);
            getLogger().info(GREEN + "✓ PlayerDataManager carregado" + RESET);

            // Loads totemHandler
            teleportHandler = new TeleportHandler(this);
            getLogger().info(GREEN + "✓ TeleportHandler carregado" + RESET);

            // Loads TotemListener
            getServer().getPluginManager().registerEvents(new TotemListener(this), this);
            getLogger().info(GREEN + "✓ TotemListener registrado" + RESET);

            // Loads totemRecipe
            recipeManager = new RecipeManager(this);
            getLogger().info(GREEN + "✓ RecipeManager carregado" + RESET);
            getLogger().info(GREEN + "✓ Receita do totem registrada" + RESET);

            // Loads totemMenu
            totemMenu = new TotemMenu(this);
            getServer().getPluginManager().registerEvents(new TotemMenu(this), this);
            getLogger().info(GREEN + "✓ TotemMenu registrado" + RESET);

            totemManager.loadFromFile();
            playerDataManager.loadFromFile();
            recipeManager.registerTotemRecipe();

        }
        catch (Exception e) {
            getLogger().severe("ERRO ao carregar managers: " + e.getMessage());
            e.printStackTrace();
            getLogger().severe(RED + "Plugin será desabilitado!" + RESET);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getLogger().info(GREEN + "Plugin JOTAGE-TOTEMS habilitado com sucesso!" + RESET);
        getLogger().info("Material do totem: " + configManager.getTotemBlockMaterial().name());
        getLogger().info("Cooldown: " + configManager.getCooldownSeconds() + " segundos");


    }

    @Override
    public void onDisable() {
        getLogger().info("Desabilitando Jotage Totems...");
        getLogger().info("Salvando dados...");

        if (recipeManager != null) {
            recipeManager.unregisterRecipe();
        }

        if (playerDataManager != null) {
            playerDataManager.saveToFile();
        }

        if (totemManager != null) {
            totemManager.saveToFile();
        }

        if (teleportHandler != null) {
            teleportHandler.cancelAllTeleports();
        }

        configManager = null;
        messageManager = null;
        recipeManager = null;
        playerDataManager = null;
        totemManager = null;
        teleportHandler = null;
        totemMenu = null;

        instance = null;

        getLogger().info("Plugin desabilitado com sucesso!");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public TotemManager getTotemManager() {
        return totemManager;
    }

    public TeleportHandler getTeleportHandler() {
        return teleportHandler;
    }

    public TotemMenu getTotemMenu() {
        return totemMenu;
    }
}

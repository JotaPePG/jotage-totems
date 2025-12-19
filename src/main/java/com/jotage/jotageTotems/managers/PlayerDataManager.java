package com.jotage.jotageTotems.managers;

import com.jotage.jotageTotems.JotageTotems;
import com.jotage.jotageTotems.models.PlayerTotemData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PlayerDataManager {

    private final JotageTotems plugin;
    private final Map<UUID, PlayerTotemData> playersData;
    private File dataFile;

    public PlayerDataManager(JotageTotems plugin) {
        this.plugin = plugin;
        this.playersData = new HashMap<>();

        setupFile();
    }

    private void setupFile() {
        dataFile = new File(plugin.getDataFolder(), "playerdata.yml");

        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
                plugin.getLogger().info("Arquivo playerdata.yml criado");
            } catch (IOException e) {
                plugin.getLogger().severe("Erro ao criar playerdata.yml: " + e.getMessage());
            }
        }
    }

    public PlayerTotemData getPlayerData(UUID playerId) {
        // Se já existe na memória, retorna
        if (playersData.containsKey(playerId)) {
            return playersData.get(playerId);
        }

        // Se não existe, cria novo
        PlayerTotemData data = new PlayerTotemData(playerId);
        playersData.put(playerId, data);

        return data;
    }

    public PlayerTotemData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }

    public boolean hasPlayerData(UUID playerId) {
        return playersData.containsKey(playerId);
    }

    public boolean registerTotem(UUID playerId, UUID totemId) {
        PlayerTotemData data = getPlayerData(playerId);
        return data.registerTotem(totemId);
    }

    public boolean unregisterTotem(UUID playerId, UUID totemId) {
        PlayerTotemData data = getPlayerData(playerId);
        return data.unregisterTotem(totemId);
    }

    public void unregisterTotemFromAll(UUID totemId) {
        int removed = 0;

        for (PlayerTotemData data : playersData.values()) {
            if (data.unregisterTotem(totemId)) {
                removed++;
            }
        }

        if (removed > 0) {
            plugin.getLogger().info("Totem desregistrado de " + removed + " jogadores");
        }
    }

    public void setCustomName(UUID playerId, UUID totemId, String customName) {
        PlayerTotemData data = getPlayerData(playerId);
        data.setCustomName(totemId, customName);
    }

    public String getCustomName(UUID playerId, UUID totemId) {
        PlayerTotemData data = getPlayerData(playerId);
        return data.getCustomName(totemId);
    }

    public void setPendingBreak(UUID playerId, UUID totemId) {
        PlayerTotemData data = getPlayerData(playerId);
        data.setPendingBreak(totemId);
    }

    public void clearPendingBreak(UUID playerId) {
        PlayerTotemData data = getPlayerData(playerId);
        data.clearPendingBreak();
    }

    public boolean isPendingBreakFor(UUID playerId, UUID totemId) {
        PlayerTotemData data = getPlayerData(playerId);
        return data.isPendingBreakFor(totemId);
    }

    public boolean isBreakConfirmationExpired(UUID playerId) {
        PlayerTotemData data = getPlayerData(playerId);
        long confirmationTime = plugin.getConfigManager().getBreakConfirmationMillis();
        return data.isBreakConfirmationExpired(confirmationTime);
    }

    public boolean canTeleport(UUID playerId) {
        PlayerTotemData data = getPlayerData(playerId);

        long lastTeleport = data.getLastTeleportTime();

        // Se nunca teleportou, pode teleportar
        if (lastTeleport == 0) {
            return true;
        }

        long cooldown = plugin.getConfigManager().getCooldownMillis();
        long elapsed = System.currentTimeMillis() - lastTeleport;

        return elapsed >= cooldown;
    }

    public long getRemainingCooldown(UUID playerId) {
        PlayerTotemData data = getPlayerData(playerId);

        long lastTeleport = data.getLastTeleportTime();

        if (lastTeleport == 0) {
            return 0;
        }

        long cooldown = plugin.getConfigManager().getCooldownMillis();
        long elapsed = System.currentTimeMillis() - lastTeleport;
        long remaining = cooldown - elapsed;

        // Retorna em segundos
        return Math.max(0, remaining / 1000);
    }

    public void updateLastTeleport(UUID playerId) {
        PlayerTotemData data = getPlayerData(playerId);
        data.updateLastTeleport();
    }

    public void saveToFile() {
        FileConfiguration config = new YamlConfiguration();

        for (PlayerTotemData data : playersData.values()) {
            String path = "players." + data.getPlayerId().toString();

            // Salva totems registrados
            List<String> registeredList = new ArrayList<>();
            for (UUID totemId : data.getRegisteredTotems()) {
                registeredList.add(totemId.toString());
            }
            config.set(path + ".registered-totems", registeredList);

            // Salva nomes customizados
            if (!data.getCustomTotemNames().isEmpty()) {
                for (Map.Entry<UUID, String> entry : data.getCustomTotemNames().entrySet()) {
                    String customPath = path + ".custom-names." + entry.getKey().toString();
                    config.set(customPath, entry.getValue());
                }
            }

            // Salva último teleporte
            config.set(path + ".last-teleport", data.getLastTeleportTime());
        }

        try {
            config.save(dataFile);
            plugin.getLogger().info("Salvos dados de " + playersData.size() + " jogadores");
        } catch (IOException e) {
            plugin.getLogger().severe("Erro ao salvar playerdata.yml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loadFromFile() {
        if (!dataFile.exists()) {
            plugin.getLogger().info("Nenhum dado de jogador encontrado");
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection playersSection = config.getConfigurationSection("players");

        if (playersSection == null) {
            plugin.getLogger().info("Nenhum dado de jogador encontrado");
            return;
        }

        int loaded = 0;
        int errors = 0;

        for (String playerIdString : playersSection.getKeys(false)) {
            try {
                String path = "players." + playerIdString;

                UUID playerId = UUID.fromString(playerIdString);
                PlayerTotemData data = new PlayerTotemData(playerId);

                // Carrega totems registrados
                List<String> registeredList = config.getStringList(path + ".registered-totems");
                for (String totemIdString : registeredList) {
                    try {
                        UUID totemId = UUID.fromString(totemIdString);
                        data.registerTotem(totemId);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("UUID de totem inválido: " + totemIdString);
                    }
                }

                // Carrega nomes customizados
                ConfigurationSection customNamesSection = config.getConfigurationSection(path + ".custom-names");
                if (customNamesSection != null) {
                    for (String totemIdString : customNamesSection.getKeys(false)) {
                        try {
                            UUID totemId = UUID.fromString(totemIdString);
                            String customName = config.getString(path + ".custom-names." + totemIdString);
                            data.setCustomName(totemId, customName);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("UUID de totem inválido em custom names: " + totemIdString);
                        }
                    }
                }

                // Carrega último teleporte
                long lastTeleport = config.getLong(path + ".last-teleport", 0);
                data.setLastTeleportTime(lastTeleport);

                // Adiciona ao mapa
                playersData.put(playerId, data);
                loaded++;

            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao carregar dados do jogador " + playerIdString + ": " + e.getMessage());
                errors++;
            }
        }

        plugin.getLogger().info("Carregados dados de " + loaded + " jogadores");
        if (errors > 0) {
            plugin.getLogger().warning(errors + " jogadores falharam ao carregar");
        }
    }

    public void savePlayerData(UUID playerId) {
        if (!playersData.containsKey(playerId)) {
            return;
        }

        // Para simplificar, apenas salva tudo
        // Em produção, você poderia otimizar salvando apenas um jogador
        saveToFile();
    }

    public void unloadPlayerData(UUID playerId) {
        playersData.remove(playerId);
    }

    public int getLoadedPlayersCount() {
        return playersData.size();
    }

    public Collection<PlayerTotemData> getAllPlayerData() {
        return playersData.values();
    }
}


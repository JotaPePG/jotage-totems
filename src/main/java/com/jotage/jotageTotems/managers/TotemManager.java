package com.jotage.jotageTotems.managers;

import com.jotage.jotageTotems.JotageTotems;
import com.jotage.jotageTotems.models.Totem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TotemManager {

    private final JotageTotems plugin;
    private final Map<UUID, Totem> totems;
    private final Map<Location, UUID> locationIndex;
    private File totemsFile;

    public TotemManager(JotageTotems plugin) {
        this.plugin = plugin;
        this.totems = new HashMap<>();
        this.locationIndex = new HashMap<>();

        setupFile();
    }

    private void setupFile() {
        totemsFile = new File(plugin.getDataFolder(), "totems.yml");

        if (!totemsFile.exists()) {
            try {
                totemsFile.createNewFile();
                plugin.getLogger().info("Arquivo totems.yml criado");
            } catch (IOException e) {
                plugin.getLogger().severe("Erro ao criar totems.yml: " + e.getMessage());
            }
        }
    }

    public Totem createTotem(Location location, Player player, String name) {
        UUID totemId = UUID.randomUUID();
        Material blockMaterial = plugin.getConfigManager().getTotemBlockMaterial();
        Totem totem = new Totem(totemId, player.getUniqueId(), name, location, blockMaterial);

        totem.placeBlock();

        totems.put(totemId, totem);

        locationIndex.put(location, totemId);

        plugin.getLogger().info("Totem criado: " + name + " por " + player.getName() +
                " em " + formatLocation(location));

        return totem;
    }

    private String formatLocation(Location location) {
        return String.format("%d, %d, %d em %s",
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                location.getWorld().getName()
        );
    }

    public boolean removeTotem(UUID totemId) {
        Totem totem = totems.get(totemId);

        if (totem == null) return false;

        totem.removeBlock();

        locationIndex.remove(totem.getLocation());

        totems.remove(totemId);

        plugin.getLogger().info("Totem removido: " + totem.getName());

        return true;
    }

    public boolean removeTotemAt(Location location) {
        UUID totemId = locationIndex.get(location);

        if (totemId == null) {
            return false;
        }

        return removeTotem(totemId);
    }

    public Totem getTotemById(UUID totemId) {
        return totems.get(totemId);
    }

    public Totem getTotemByLocation(Location location) {
        UUID totemId = locationIndex.get(location);

        if (totemId == null) {
            return null;
        }

        return totems.get(totemId);
    }

    public List<Totem> getTotemsByOwner(UUID ownerId) {
        return totems.values().stream()
                .filter(totem -> totem.getOwnerId().equals(ownerId))
                .collect(Collectors.toList());
    }

    public Collection<Totem> getAllTotems() {
        return totems.values();
    }

    public int getTotalTotems() {
        return totems.size();
    }

    public boolean canPlayerCreateMore(Player player) {
        int maxTotems = plugin.getConfigManager().getMaxTotemsPerPlayer();
        int currentTotems = getTotemsByOwner(player.getUniqueId()).size();

        return currentTotems < maxTotems;
    }

    public int getRemainingTotems(Player player) {
        int maxTotems = plugin.getConfigManager().getMaxTotemsPerPlayer();
        int currentTotems = getTotemsByOwner(player.getUniqueId()).size();

        return Math.max(0, maxTotems - currentTotems);
    }

    public boolean hasTotemAt(Location location) {
        return locationIndex.containsKey(location);
    }

    public void saveToFile() {
        FileConfiguration config = new YamlConfiguration();

        // Para cada totem, salva suas informações
        for (Totem totem : totems.values()) {
            String path = "totems." + totem.getId().toString();

            config.set(path + ".name", totem.getName());
            config.set(path + ".owner", totem.getOwnerId().toString());
            config.set(path + ".world", totem.getWorld().getName());
            config.set(path + ".x", totem.getLocation().getX());
            config.set(path + ".y", totem.getLocation().getY());
            config.set(path + ".z", totem.getLocation().getZ());
            config.set(path + ".material", totem.getBlockMaterial().name());
            config.set(path + ".created", totem.getCreatedAt());
        }

        try {
            config.save(totemsFile);
            plugin.getLogger().info("Salvos " + totems.size() + " totems em totems.yml");
        } catch (IOException e) {
            plugin.getLogger().severe("Erro ao salvar totems.yml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loadFromFile() {
        if (!totemsFile.exists()) {
            plugin.getLogger().info("Nenhum totem salvo encontrado");
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(totemsFile);
        ConfigurationSection totemsSection = config.getConfigurationSection("totems");

        if (totemsSection == null) {
            plugin.getLogger().info("Nenhum totem salvo encontrado");
            return;
        }

        int loaded = 0;
        int errors = 0;

        // Para cada totem salvo
        for (String idString : totemsSection.getKeys(false)) {
            try {
                String path = "totems." + idString;

                // Lê os dados
                UUID id = UUID.fromString(idString);
                String name = config.getString(path + ".name");
                UUID ownerId = UUID.fromString(config.getString(path + ".owner"));
                String worldName = config.getString(path + ".world");
                double x = config.getDouble(path + ".x");
                double y = config.getDouble(path + ".y");
                double z = config.getDouble(path + ".z");
                String materialName = config.getString(path + ".material");

                // Valida o mundo
                org.bukkit.World world = plugin.getServer().getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("Mundo não encontrado para totem: " + worldName);
                    errors++;
                    continue;
                }

                // Valida o material
                Material material;
                try {
                    material = Material.valueOf(materialName);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Material inválido para totem: " + materialName);
                    material = plugin.getConfigManager().getTotemBlockMaterial();
                }

                // Cria a location
                Location location = new Location(world, x, y, z);

                // Recria o totem
                Totem totem = new Totem(id, ownerId, name, location, material);

                // Verifica se o bloco ainda existe no mundo
                if (!totem.isValid()) {
                    // Se o bloco foi destruído, coloca de volta
                    totem.placeBlock();
                    plugin.getLogger().info("Bloco do totem restaurado: " + name);
                }

                // Adiciona aos mapas
                totems.put(id, totem);
                locationIndex.put(location, id);

                loaded++;

            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao carregar totem " + idString + ": " + e.getMessage());
                errors++;
            }
        }

        plugin.getLogger().info("Carregados " + loaded + " totems do arquivo");
        if (errors > 0) {
            plugin.getLogger().warning(errors + " totems falharam ao carregar");
        }
    }

    public int cleanupInvalidTotems() {
        List<UUID> toRemove = new ArrayList<>();

        for (Totem totem : totems.values()) {
            if (!totem.isValid()) {
                toRemove.add(totem.getId());
            }
        }

        for (UUID id : toRemove) {
            removeTotem(id);
        }

        return toRemove.size();
    }
}

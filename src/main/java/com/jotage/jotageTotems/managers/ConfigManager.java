package com.jotage.jotageTotems.managers;

import com.jotage.jotageTotems.JotageTotems;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Gerencia as configurações do plugin.
 * Carrega valores do config.yml e fornece acesso a eles.
 */
public class ConfigManager {

    private final JotageTotems plugin;
    private FileConfiguration config;

    // Valores cacheados
    private long cooldownSeconds;
    private int teleportCountdown;
    private int xpCost;
    private int maxTotemsPerPlayer;
    private int breakConfirmationTime;

    private Material totemBlockMaterial;
    private boolean totemBlockIndestructible;
    private boolean totemParticlesAmbient;
    private Particle totemParticleType;

    private boolean effectsParticles;
    private Particle effectsParticleType;
    private boolean effectsSounds;
    private Sound effectsSoundType;
    private Sound countdownSound;

    public ConfigManager(JotageTotems plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        loadValues();
    }

    @SuppressWarnings("deprecation")
    private void loadValues() {
        // Configurações básicas
        cooldownSeconds = config.getLong("cooldown", 300);
        teleportCountdown = config.getInt("teleport-countdown", 3);
        xpCost = config.getInt("xp-cost", 1);
        maxTotemsPerPlayer = config.getInt("max-totems-per-player", 20);
        breakConfirmationTime = config.getInt("break-confirmation-time", 5);

        // Bloco do totem
        String materialName = config.getString("totem-block.material", "BEDROCK");
        try {
            totemBlockMaterial = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Material inválido: " + materialName + ". Usando BEDROCK.");
            totemBlockMaterial = Material.BEDROCK;
        }

        totemBlockIndestructible = config.getBoolean("totem-block.indestructible", true);
        totemParticlesAmbient = config.getBoolean("totem-block.particles-ambient", true);

        String totemParticleName = config.getString("totem-block.particle-type", "PORTAL");
        try {
            totemParticleType = Particle.valueOf(totemParticleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Partícula inválida: " + totemParticleName + ". Usando PORTAL.");
            totemParticleType = Particle.PORTAL;
        }

        // Efeitos do teleporte
        effectsParticles = config.getBoolean("effects.particles", true);

        String effectsParticleName = config.getString("effects.particle-type", "ENCHANT");
        try {
            effectsParticleType = Particle.valueOf(effectsParticleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Partícula de efeito inválida: " + effectsParticleName + ". Usando ENCHANT.");
            effectsParticleType = Particle.ENCHANT;
        }

        effectsSounds = config.getBoolean("effects.sounds", true);

        String soundName = config.getString("effects.sound-type", "ENTITY_ENDERMAN_TELEPORT");
        effectsSoundType = getModernSound(
                soundName,
                "entity.enderman.teleport"
        );


        String countdownSoundName = config.getString("effects.countdown-sound", "BLOCK_NOTE_BLOCK_PLING");
        countdownSound = getModernSound(
                countdownSoundName,
                "block.note_block.pling"
        );

    }

    public void reload() {
        loadConfig();
    }

    // ========== GETTERS ==========

    public long getCooldownSeconds() {
        return cooldownSeconds;
    }

    public long getCooldownMillis() {
        return cooldownSeconds * 1000;
    }

    public int getTeleportCountdown() {
        return teleportCountdown;
    }

    public int getXpCost() {
        return xpCost;
    }

    public int getMaxTotemsPerPlayer() {
        return maxTotemsPerPlayer;
    }

    public int getBreakConfirmationTime() {
        return breakConfirmationTime;
    }

    public long getBreakConfirmationMillis() {
        return breakConfirmationTime * 1000L;
    }

    public Material getTotemBlockMaterial() {
        return totemBlockMaterial;
    }

    public boolean isTotemBlockIndestructible() {
        return totemBlockIndestructible;
    }

    public boolean isTotemParticlesAmbient() {
        return totemParticlesAmbient;
    }

    public Particle getTotemParticleType() {
        return totemParticleType;
    }

    public boolean isEffectsParticles() {
        return effectsParticles;
    }

    public Particle getEffectsParticleType() {
        return effectsParticleType;
    }

    public boolean isEffectsSounds() {
        return effectsSounds;
    }

    public Sound getEffectsSoundType() {
        return effectsSoundType;
    }

    public Sound getCountdownSound() {
        return countdownSound;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    private Sound getModernSound(String name, String fallback) {
        // Tenta namespaced key moderna
        NamespacedKey key = NamespacedKey.fromString(name);
        if (key != null) {
            Sound sound = Registry.SOUND_EVENT.get(key);
            if (sound != null) {
                return sound;
            }
        }

        // Tenta enum antigo
        try {
            return Sound.valueOf(name.toUpperCase());
        } catch (Exception ignored) {}

        // Fallback final
        plugin.getLogger().warning("Som inválido: " + name + ". Usando " + fallback + ".");
        return Registry.SOUND_EVENT.get(NamespacedKey.minecraft(fallback));
    }

}
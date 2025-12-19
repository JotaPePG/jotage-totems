package com.jotage.jotageTotems.managers;

import com.jotage.jotageTotems.JotageTotems;
import com.jotage.jotageTotems.models.TeleportTask;
import com.jotage.jotageTotems.models.Totem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportHandler {

    private final JotageTotems plugin;
    private final Map<UUID, TeleportTask> activeTeleports;

    public TeleportHandler(JotageTotems plugin) {
        this.plugin = plugin;
        this.activeTeleports = new HashMap<>();
    }

    public boolean startTeleport(Player player, Totem destination) {
        UUID playerId = player.getUniqueId();

        // ===== VALIDAÇÃO 1: Teleporte já ativo? =====
        if (activeTeleports.containsKey(playerId)) {
            plugin.getMessageManager().sendMessage(player, "teleport-already-active");
            return false;
        }

        // ===== VALIDAÇÃO 2: Totem ainda existe? =====
        if (!destination.isValid()) {
            plugin.getMessageManager().sendMessage(player, "error-totem-invalid");
            return false;
        }

        // ===== VALIDAÇÃO 3: Cooldown =====
        if (!plugin.getPlayerDataManager().canTeleport(playerId)) {
            long remaining = plugin.getPlayerDataManager().getRemainingCooldown(playerId);

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("time", String.valueOf(remaining));

            plugin.getMessageManager().sendMessage(player, "error-cooldown", placeholders);
            return false;
        }

        // ===== VALIDAÇÃO 4: XP =====
        int xpCost = plugin.getConfigManager().getXpCost();
        if (player.getLevel() < xpCost) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("xp", String.valueOf(xpCost));

            plugin.getMessageManager().sendMessage(player, "error-no-xp", placeholders);
            return false;
        }

        // ===== TUDO OK: INICIA TELEPORTE =====

        int countdown = plugin.getConfigManager().getTeleportCountdown();

        // Cria a TeleportTask
        TeleportTask teleportTask = new TeleportTask(player, destination, countdown);

        // Adiciona ao mapa de teleportes ativos
        activeTeleports.put(playerId, teleportTask);

        // Mensagem inicial
        plugin.getMessageManager().sendMessage(player, "teleport-started");

        // Agenda a task que roda a cada 1 segundo (20 ticks)
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            // ===== VERIFICA SE JOGADOR AINDA ESTÁ ONLINE =====
            if (!teleportTask.isPlayerOnline()) {
                cancelTeleport(player);
                return;
            }

            // ===== VERIFICA SE JOGADOR SE MOVEU =====
            if (teleportTask.hasPlayerMoved()) {
                plugin.getMessageManager().sendMessage(player, "teleport-cancelled-moved");
                cancelTeleport(player);
                return;
            }

            // ===== DECREMENTA CONTADOR =====
            teleportTask.decrementCountdown();
            int currentCount = teleportTask.getCountdown();

            // ===== SE AINDA ESTÁ CONTANDO: Mostra mensagem e som =====
            if (currentCount > 0) {
                // Mensagem de contagem
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("seconds", String.valueOf(currentCount));
                plugin.getMessageManager().sendMessage(player, "teleport-countdown", placeholders);

                // Som de contagem
                if (plugin.getConfigManager().isEffectsSounds()) {
                    Sound countdownSound = plugin.getConfigManager().getCountdownSound();
                    player.playSound(player.getLocation(), countdownSound, 1.0f, 1.0f);
                }

                // Partículas ao redor do jogador
                if (plugin.getConfigManager().isEffectsParticles()) {
                    spawnCountdownParticles(player.getLocation());
                }
            }

            // ===== COUNTDOWN CHEGOU A 0: TELEPORTA! =====
            if (teleportTask.isComplete()) {
                executeTeleport(player, destination);
                teleportTask.cancel();
                activeTeleports.remove(playerId);
            }

        }, 0L, 20L); // Inicia imediatamente (0L), repete a cada 20 ticks (1 segundo)

        // Guarda a BukkitTask no TeleportTask para poder cancelar depois
        teleportTask.setTask(bukkitTask);

        return true;
    }

    public void cancelTeleport(Player player) {
        UUID playerId = player.getUniqueId();

        TeleportTask task = activeTeleports.get(playerId);

        if (task == null) {
            return; // Não há teleporte ativo
        }

        // Cancela a BukkitTask
        task.cancel();

        // Remove do mapa
        activeTeleports.remove(playerId);
    }

    public void cancelTeleport(UUID playerId) {
        TeleportTask task = activeTeleports.get(playerId);

        if (task != null) {
            task.cancel();
            activeTeleports.remove(playerId);
        }
    }

    public void cancelAllTeleports() {
        for (TeleportTask task : activeTeleports.values()) {
            task.cancel();
        }

        activeTeleports.clear();

        plugin.getLogger().info("Cancelados " + activeTeleports.size() + " teleportes ativos");
    }

    public boolean isTeleporting(Player player) {
        return activeTeleports.containsKey(player.getUniqueId());
    }

    public int getActiveTeleportsCount() {
        return activeTeleports.size();
    }

    public TeleportTask getActiveTeleport(Player player) {
        return activeTeleports.get(player.getUniqueId());
    }

    private void spawnCountdownParticles(Location location) {
        Particle particle = plugin.getConfigManager().getEffectsParticleType();

        // Spawna partículas em círculo ao redor do jogador
        for (int i = 0; i < 10; i++) {
            double angle = 2 * Math.PI * i / 10; // Divide círculo em 10 partes
            double x = Math.cos(angle) * 0.5;    // Raio de 0.5 blocos
            double z = Math.sin(angle) * 0.5;

            Location particleLoc = location.clone().add(x, 1, z);
            location.getWorld().spawnParticle(particle, particleLoc, 1, 0, 0, 0, 0);
        }
    }

    private void spawnTeleportParticles(Location location) {
        Particle particle = plugin.getConfigManager().getEffectsParticleType();

        // Efeito de espiral subindo
        for (double y = 0; y <= 2; y += 0.1) {
            double angle = y * Math.PI;
            double radius = 1.0 - (y / 2); // Raio diminui conforme sobe
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;

            Location particleLoc = location.clone().add(x, y, z);
            location.getWorld().spawnParticle(particle, particleLoc, 1, 0, 0, 0, 0);
        }

        // Explosão de partículas no centro
        location.getWorld().spawnParticle(particle, location.clone().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
    }

    private void executeTeleport(Player player, Totem destination) {
        UUID playerId = player.getUniqueId();

        // Pega a location de teleporte (em cima do bloco)
        Location tpLocation = destination.getTeleportLocation();

        // ===== EFEITOS NO LOCAL DE PARTIDA =====
        if (plugin.getConfigManager().isEffectsParticles()) {
            spawnTeleportParticles(player.getLocation());
        }

        if (plugin.getConfigManager().isEffectsSounds()) {
            Sound sound = plugin.getConfigManager().getEffectsSoundType();
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }

        // ===== TELEPORTA O JOGADOR =====
        player.teleport(tpLocation);

        // ===== EFEITOS NO LOCAL DE CHEGADA =====
        if (plugin.getConfigManager().isEffectsParticles()) {
            spawnTeleportParticles(tpLocation);
        }

        if (plugin.getConfigManager().isEffectsSounds()) {
            Sound sound = plugin.getConfigManager().getEffectsSoundType();
            player.playSound(tpLocation, sound, 1.0f, 1.0f);
        }

        // ===== CONSOME XP =====
        int xpCost = plugin.getConfigManager().getXpCost();
        player.setLevel(player.getLevel() - xpCost);

        // ===== ATUALIZA COOLDOWN =====
        plugin.getPlayerDataManager().updateLastTeleport(playerId);

        // ===== MENSAGEM DE SUCESSO =====
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("totem", destination.getName());
        plugin.getMessageManager().sendMessage(player, "teleport-success", placeholders);

        plugin.getLogger().info("Jogador " + player.getName() + " teleportou para " + destination.getName());
    }
}

package com.jotage.jotageTotems.models;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class TeleportTask {

    private final Player player;
    private final Totem destination;
    private final Location startLocation;
    private int countdown;
    private BukkitTask task;

    public TeleportTask(Player player, Totem destination, int countdown) {
        this.player = player;
        this.destination = destination;
        this.startLocation = player.getLocation().clone();
        this.countdown = countdown;
        this.task = null;
    }

    public Player getPlayer() {
        return player;
    }

    public Totem getDestination() {
        return destination;
    }

    public Location getStartLocation() {
        return startLocation;
    }

    public int getCountdown() {
        return countdown;
    }

    public BukkitTask getTask() {
        return task;
    }

    public void setTask(BukkitTask task) {
        this.task = task;
    }

    public void decrementCountdown() {
        this.countdown--;
    }

    public boolean isComplete() {
        return countdown <= 0;
    }

    public void cancel() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    public boolean hasPlayerMoved() {
        Location currentLocation = player.getLocation();
        // Compara as coordenadas X, Y, Z
        // Usamos getBlockX/Y/Z para comparar blocos inteiros
        // (evita cancelar por micro-movimentos causados por lag)
        return currentLocation.getBlockX() != startLocation.getBlockX() ||
                currentLocation.getBlockY() != startLocation.getBlockY() ||
                currentLocation.getBlockZ() != startLocation.getBlockZ();
    }

    public boolean isPlayerOnline() {
        return player != null && player.isOnline();
    }

    public boolean isActive() {
        return task != null && !task.isCancelled();
    }

    @Override
    public String toString() {
        return String.format("TeleportTask{player=%s, destination=%s, countdown=%d}",
                player.getName(),
                destination.getName(),
                countdown
        );
    }
}

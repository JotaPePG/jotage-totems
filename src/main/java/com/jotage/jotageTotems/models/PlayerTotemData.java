package com.jotage.jotageTotems.models;

import java.util.*;

public class PlayerTotemData {

    private final UUID playerId;
    private final Set<UUID> registeredTotems;
    private final Map<UUID, String> customTotemNames;
    private long lastTeleportTime;
    private UUID pendingTotemBreak;
    private long breakConfirmTime;

    public PlayerTotemData(UUID playerId) {
        this.playerId = playerId;
        this.registeredTotems = new HashSet<>();
        this.customTotemNames = new HashMap<>();
        this.lastTeleportTime = 0;
        this.pendingTotemBreak = null;
        this.breakConfirmTime = 0;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public Set<UUID> getRegisteredTotems() {
        return registeredTotems;
    }

    public Map<UUID, String> getCustomTotemNames() {
        return customTotemNames;
    }

    public long getLastTeleportTime() {
        return lastTeleportTime;
    }

    public UUID getPendingTotemBreak() {
        return pendingTotemBreak;
    }

    public long getBreakConfirmTime() {
        return breakConfirmTime;
    }

    public void updateLastTeleport() {
        this.lastTeleportTime = System.currentTimeMillis();
    }

    public void setLastTeleportTime(long lastTeleportTime) {
        this.lastTeleportTime = lastTeleportTime;
    }

    // Adiciona totem na lista
    public boolean registerTotem(UUID totemId) {
        return registeredTotems.add(totemId);
    }

    // Remove totem na lista
    public boolean unregisterTotem(UUID totemId) {

        boolean removed = registeredTotems.remove(totemId);

        customTotemNames.remove(totemId);

        return removed;
    }

    public boolean hasTotemRegistered(UUID totemId) {
        return registeredTotems.contains(totemId);
    }

    public int getRegisteredTotemsCount() {
        return registeredTotems.size();
    }

    public boolean hasNoTotems() {
        return registeredTotems.isEmpty();
    }

    public void setCustomName(UUID totemId, String customName) {
        // Se o nome for vazio/null, remove o custom name
        if (customName == null || customName.trim().isEmpty()) {
            customTotemNames.remove(totemId);
        } else {
            customTotemNames.put(totemId, customName);
        }
    }

    public String getCustomName(UUID totemId) {
        return customTotemNames.get(totemId);
    }

    public boolean hasCustomName(UUID totemId) {
        return customTotemNames.containsKey(totemId);
    }

    public void setPendingBreak(UUID totemId) {
        this.pendingTotemBreak = totemId;
        this.breakConfirmTime = System.currentTimeMillis();
    }

    public void clearPendingBreak() {
        this.pendingTotemBreak = null;
        this.breakConfirmTime = 0;
    }

    public boolean hasPendingBreak() {
        return pendingTotemBreak != null;
    }

    public boolean isBreakConfirmationExpired(long confirmationTime) {
        if (!hasPendingBreak()) {
            return false;
        }

        long elapsedTime = System.currentTimeMillis() - breakConfirmTime;
        return elapsedTime > confirmationTime;
    }

    public boolean isPendingBreakFor(UUID totemId) {
        return pendingTotemBreak != null && pendingTotemBreak.equals(totemId);
    }

    @Override
    public String toString() {
        return String.format("PlayerTotemData{player=%s, totems=%d, lastTP=%d}",
                playerId.toString().substring(0, 8),
                registeredTotems.size(),
                lastTeleportTime
        );
    }
}

package com.jotage.jotageTotems.models;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.UUID;

public class Totem {

    private final UUID id;
    private final UUID ownerId;
    private String name;
    private World world;
    private Location location;
    private final long createdAt;
    private Block block;
    private Material blockMaterial;

    public Totem(UUID id, UUID ownerId, String name, Location location, Material blockMaterial) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.world = location.getWorld();
        this.location = location;
        this.createdAt = System.currentTimeMillis();
        this.block = location.getBlock();
        this.blockMaterial = blockMaterial;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public World getWorld() {
        return world;
    }

    public Location getLocation() {
        return location;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public Block getBlock() {
        return block;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBlock(Block block) {
        this.block = block;
    }

    public boolean isValid() {
        return block != null && block.getType() == blockMaterial;
    }

    public void placeBlock() {
        if (block != null) {
            block.setType(blockMaterial);
        }
    }

    public void removeBlock() {
        if (block != null && isValid()) {
            block.setType(Material.AIR);
        }
    }

    public Location getTeleportLocation() {
        Location teleportLoc = location.clone();

        teleportLoc.add(0.5, 1, 0.5);

        return teleportLoc;
    }

    public Material getBlockMaterial() {
        return blockMaterial;
    }
}

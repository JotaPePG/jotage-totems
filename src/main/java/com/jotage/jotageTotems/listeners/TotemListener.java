package com.jotage.jotageTotems.listeners;

import com.jotage.jotageTotems.JotageTotems;
import com.jotage.jotageTotems.models.Totem;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TotemListener implements Listener {

    private final JotageTotems plugin;

    public TotemListener(JotageTotems plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerIntereact(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        Action action = event.getAction();

        // Verifica se é click direito
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) {
            return;
        }

        // Verifica se tem item na mão
        if (item == null) {
            return;
        }

        // Verifica se é o item de totem
        if (!plugin.getRecipeManager().isTotemItem(item)) {
            return;
        }

        // ===== CENÁRIO 1: SHIFT + CLICK = COLOCAR TOTEM =====
        if (player.isSneaking()) {
            handleTotemPlacement(event, player, item);
            return;
        }

        // ===== CENÁRIO 2: CLICK EM BLOCO = INTERAGIR COM TOTEM =====
        if (action == Action.RIGHT_CLICK_BLOCK) {
            Block clickedBlock = event.getClickedBlock();

            if (clickedBlock != null) {
                handleTotemInteraction(event, player, clickedBlock);
            }
        }
    }

    private void handleTotemPlacement(PlayerInteractEvent event, Player player, ItemStack item) {
        // Cancela o evento para não consumir o totem normal
        event.setCancelled(true);

        // ===== VALIDAÇÃO 1: Permissão =====
        if (!player.hasPermission("totem.create")) {
            plugin.getMessageManager().sendMessage(player, "error-no-permission");
            return;
        }

        // ===== VALIDAÇÃO 2: Limite de totens =====
        if (!plugin.getTotemManager().canPlayerCreateMore(player)) {
            int max = plugin.getConfigManager().getMaxTotemsPerPlayer();

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("max", String.valueOf(max));

            plugin.getMessageManager().sendMessage(player, "error-max-totems", placeholders);
            return;
        }

        // ===== PEGA A LOCATION ONDE VAI COLOCAR =====
        Location location;

        if (event.getClickedBlock() != null) {
            // Click em um bloco: coloca em cima
            location = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation();
        } else {
            // Click no ar: coloca aos pés do jogador
            location = player.getLocation().getBlock().getLocation();
        }

        // ===== VALIDA SE JÁ TEM TOTEM ALI =====
        if (plugin.getTotemManager().hasTotemAt(location)) {
            plugin.getMessageManager().sendMessage(player, "error-totem-exists");
            return;
        }

        // ===== PEDE O NOME DO TOTEM =====
        // TODO: Implementar sistema de chat input
        // Por enquanto, usa nome padrão
        String totemName = "Totem de " + player.getName();

        // ===== CRIA O TOTEM =====
        Totem totem = plugin.getTotemManager().createTotem(location, player, totemName);

        if (totem == null) {
            plugin.getMessageManager().sendMessage(player, "error-generic");
            return;
        }

        // ===== CONSOME O ITEM =====
        item.setAmount(item.getAmount() - 1);

        // ===== REGISTRA AUTOMATICAMENTE PARA O CRIADOR =====
        plugin.getPlayerDataManager().registerTotem(player.getUniqueId(), totem.getId());

        // ===== MENSAGEM DE SUCESSO =====
        plugin.getMessageManager().sendMessage(player, "totem-created");

        // Salva dados
        plugin.getTotemManager().saveToFile();
        plugin.getPlayerDataManager().saveToFile();
    }

    private void handleTotemInteraction(PlayerInteractEvent event, Player player, Block block) {
        Location location = block.getLocation();

        // Verifica se é um totem
        Totem totem = plugin.getTotemManager().getTotemByLocation(location);

        if (totem == null) {
            return; // Não é um totem
        }

        // Cancela o evento
        event.setCancelled(true);

        // ===== VALIDAÇÃO: Permissão =====
        if (!player.hasPermission("totem.use")) {
            plugin.getMessageManager().sendMessage(player, "error-no-permission");
            return;
        }

        UUID playerId = player.getUniqueId();

        // ===== CENÁRIO A: SHIFT + CLICK = REGISTRAR TOTEM =====
        if (player.isSneaking()) {

            // Verifica se já está registrado
            if (plugin.getPlayerDataManager().getPlayerData(playerId).hasTotemRegistered(totem.getId())) {
                plugin.getMessageManager().sendMessage(player, "totem-already-registered");
                return;
            }

            // Registra o totem
            plugin.getPlayerDataManager().registerTotem(playerId, totem.getId());
            plugin.getMessageManager().sendMessage(player, "totem-registered");

            // Salva
            plugin.getPlayerDataManager().saveToFile();
            return;
        }

        // ===== CENÁRIO B: CLICK NORMAL = ABRIR MENU =====
        // TODO: Abrir TotemMenu
        plugin.getTotemMenu().open(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location location = block.getLocation();

        // Verifica se é um totem
        Totem totem = plugin.getTotemManager().getTotemByLocation(location);

        if (totem == null) {
            return; // Não é um totem
        }

        // Cancela o evento (não quebra ainda)
        event.setCancelled(true);

        // ===== VALIDAÇÃO: Permissão =====
        if (!player.hasPermission("totem.create")) {
            plugin.getMessageManager().sendMessage(player, "error-no-permission");
            return;
        }

        // ===== VALIDAÇÃO: É o dono? =====
        if (!totem.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("totem.admin")) {
            plugin.getMessageManager().sendMessage(player, "error-not-owner");
            return;
        }

        // ===== VALIDAÇÃO: Totem indestrutível? =====
        if (plugin.getConfigManager().isTotemBlockIndestructible()) {
            // Se configurado como indestrutível, só admin pode quebrar
            if (!player.hasPermission("totem.admin")) {
                plugin.getMessageManager().sendMessage(player, "error-totem-indestructible");
                return;
            }
        }

        UUID playerId = player.getUniqueId();

        // ===== SISTEMA DE CONFIRMAÇÃO =====

        // Verifica se já tem uma quebra pendente
        if (plugin.getPlayerDataManager().isPendingBreakFor(playerId, totem.getId())) {

            // Verifica se expirou
            if (plugin.getPlayerDataManager().isBreakConfirmationExpired(playerId)) {
                // Expirou! Reseta e pede para quebrar de novo
                plugin.getPlayerDataManager().clearPendingBreak(playerId);
                plugin.getPlayerDataManager().setPendingBreak(playerId, totem.getId());
                plugin.getMessageManager().sendMessage(player, "totem-break-confirm");
                return;
            }

            // CONFIRMADO! Remove o totem
            plugin.getTotemManager().removeTotem(totem.getId());
            plugin.getPlayerDataManager().unregisterTotemFromAll(totem.getId());
            plugin.getPlayerDataManager().clearPendingBreak(playerId);

            plugin.getMessageManager().sendMessage(player, "totem-removed");

            // Salva
            plugin.getTotemManager().saveToFile();
            plugin.getPlayerDataManager().saveToFile();

        } else {
            // Primeira quebra: inicia confirmação
            plugin.getPlayerDataManager().setPendingBreak(playerId, totem.getId());
            plugin.getMessageManager().sendMessage(player, "totem-break-confirm");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Otimização: só verifica se jogador está teleportando
        if (!plugin.getTeleportHandler().isTeleporting(player)) {
            return;
        }

        // Verifica se realmente MOVEU (não apenas girou a cabeça)
        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null) {
            return;
        }

        // Compara apenas X, Y, Z (ignora pitch/yaw)
        if (from.getBlockX() != to.getBlockX() ||
                from.getBlockY() != to.getBlockY() ||
                from.getBlockZ() != to.getBlockZ()) {

            // MOVEU! O TeleportHandler já vai cancelar na próxima verificação
            // (não precisa fazer nada aqui, o handler detecta via hasPlayerMoved())
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamage(EntityDamageEvent event) {
        // Verifica se é um jogador
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        // Verifica se está teleportando
        if (plugin.getTeleportHandler().isTeleporting(player)) {
            // Cancela o teleporte
            plugin.getTeleportHandler().cancelTeleport(player);
            plugin.getMessageManager().sendMessage(player, "teleport-cancelled-damage");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Cancela teleporte se estiver ativo
        if (plugin.getTeleportHandler().isTeleporting(player)) {
            plugin.getTeleportHandler().cancelTeleport(player);
        }

        // Limpa confirmação de quebra pendente
        plugin.getPlayerDataManager().clearPendingBreak(playerId);

        // Salva dados do jogador
        plugin.getPlayerDataManager().savePlayerData(playerId);

        // Opcional: descarregar dados da memória
        // plugin.getPlayerDataManager().unloadPlayerData(playerId);
    }
}

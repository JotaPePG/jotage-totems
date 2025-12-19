package com.jotage.jotageTotems.listeners;

import com.jotage.jotageTotems.JotageTotems;
import com.jotage.jotageTotems.models.Totem;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatListener implements Listener {

    private final JotageTotems plugin;
    private final Map<UUID, UUID> waitingForInput;

    public ChatListener(JotageTotems plugin) {
        this.plugin = plugin;
        this.waitingForInput = new HashMap<>();
    }

    public void waitForRename(Player player, UUID totemId) {
        waitingForInput.put(player.getUniqueId(), totemId);

        // Mensagem instruindo o jogador
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "RENOMEAR TOTEM" + ChatColor.YELLOW);
        player.sendMessage(ChatColor.WHITE + "Digite o novo nome no chat");
        player.sendMessage(ChatColor.GRAY + "ou digite " + ChatColor.RED + "cancelar" + ChatColor.GRAY + " para cancelar  ");
        player.sendMessage("");
    }

    public void cancelWaiting(UUID playerId) {
        waitingForInput.remove(playerId);
    }

    public boolean isWaitingForInput(UUID playerId) {
        return waitingForInput.containsKey(playerId);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Verifica se o jogador está aguardando input
        if (!waitingForInput.containsKey(playerId)) {
            return; // Não está aguardando, deixa a mensagem passar
        }

        // Cancela o evento (mensagem não aparece no chat)
        event.setCancelled(true);

        String message = event.getMessage().trim();
        UUID totemId = waitingForInput.get(playerId);

        // Remove do map
        waitingForInput.remove(playerId);

        // ===== VERIFICAÇÃO 1: Cancelar? =====
        if (message.equalsIgnoreCase("cancelar") ||
                message.equalsIgnoreCase("cancel") ||
                message.equalsIgnoreCase("sair")) {

            player.sendMessage(ChatColor.RED + "✗ Renomeação cancelada.");
            return;
        }

        // ===== VERIFICAÇÃO 2: Nome vazio? =====
        if (message.isEmpty()) {
            player.sendMessage(ChatColor.RED + "✗ Nome não pode ser vazio!");
            return;
        }

        // ===== VERIFICAÇÃO 3: Nome muito longo? =====
        if (message.length() > 32) {
            player.sendMessage(ChatColor.RED + "✗ Nome muito longo! Máximo 32 caracteres.");
            return;
        }

        // ===== VERIFICAÇÃO 4: Totem ainda existe? =====
        Totem totem = plugin.getTotemManager().getTotemById(totemId);

        if (totem == null) {
            player.sendMessage(ChatColor.RED + "✗ Totem não encontrado!");
            return;
        }

        // ===== VERIFICAÇÃO 5: Permissão para renomear? =====
        // Pode renomear se: é o dono OU tem permissão admin
        boolean isOwner = totem.getOwnerId().equals(playerId);
        boolean isAdmin = player.hasPermission("totem.admin");

        if (!isOwner && !isAdmin) {
            player.sendMessage(ChatColor.RED + "✗ Você não tem permissão para renomear este totem!");
            player.sendMessage(ChatColor.GRAY + "(Apenas o dono ou admins podem renomear)");
            return;
        }

        // ===== TUDO OK: RENOMEIA! =====

        String oldName = totem.getName();
        totem.setName(message);

        // Salva
        plugin.getTotemManager().saveToFile();

        // Mensagem de sucesso
        player.sendMessage("");
        player.sendMessage(ChatColor.GREEN + "✓ Totem renomeado com sucesso!");
        player.sendMessage(ChatColor.GRAY + "  Nome antigo: " + ChatColor.WHITE + oldName);
        player.sendMessage(ChatColor.GRAY + "  Nome novo: " + ChatColor.AQUA + message);
        player.sendMessage("");

        // Log
        plugin.getLogger().info("Jogador " + player.getName() + " renomeou totem de '" +
                oldName + "' para '" + message + "'");

        // Reabre o menu se estava aberto
        if (player.getOpenInventory().getTitle().equals(ChatColor.DARK_PURPLE + "Seus Totens")) {
            plugin.getTotemMenu().refresh(player);
        }
    }
}

package com.jotage.jotageTotems.ui;

import com.jotage.jotageTotems.JotageTotems;
import com.jotage.jotageTotems.models.PlayerTotemData;
import com.jotage.jotageTotems.models.Totem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.*;

public class TotemMenu implements Listener {

    private final JotageTotems plugin;
    private static final String MENU_TITLE = ChatColor.DARK_PURPLE + "Seus Totens";
    private static final int MENU_SIZE = 54;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public TotemMenu(JotageTotems plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        UUID playerId = player.getUniqueId();

        // Pega os dados do jogador
        PlayerTotemData data = plugin.getPlayerDataManager().getPlayerData(playerId);

        // Verifica se tem totens registrados
        if (data.hasNoTotems()) {
            plugin.getMessageManager().sendMessage(player, "error-no-totems");
            return;
        }

        // Cria o inventário
        Inventory inventory = buildInventory(player);

        // Abre para o jogador
        player.openInventory(inventory);
    }

    private Inventory buildInventory(Player player) {
        UUID playerId = player.getUniqueId();

        // Cria inventário vazio
        Inventory inventory = Bukkit.createInventory(null, MENU_SIZE, MENU_TITLE);

        // Pega os totens registrados
        PlayerTotemData data = plugin.getPlayerDataManager().getPlayerData(playerId);
        Set<UUID> registeredTotems = data.getRegisteredTotems();

        int slot = 0;

        // Para cada totem registrado
        for (UUID totemId : registeredTotems) {
            // Pega o totem do TotemManager
            Totem totem = plugin.getTotemManager().getTotemById(totemId);

            // Verifica se o totem ainda existe
            if (totem == null || !totem.isValid()) {
                // Totem foi destruído, remove do jogador
                plugin.getPlayerDataManager().unregisterTotem(playerId, totemId);
                continue;
            }

            // Cria o item que representa o totem
            ItemStack item = createTotemItem(player, totem);

            // Adiciona ao inventário
            inventory.setItem(slot, item);
            slot++;

            // Se chegou no limite do inventário, para
            if (slot >= MENU_SIZE) {
                break;
            }
        }

        return inventory;
    }

    private ItemStack createTotemItem(Player player, Totem totem) {
        // Material do item (mesmo do bloco do totem)
        Material material = totem.getBlockMaterial();
        ItemStack item = new ItemStack(material, 1);

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        // ===== NOME DO ITEM =====

        // Verifica se jogador tem nome customizado para este totem
        String displayName;
        String customName = plugin.getPlayerDataManager().getCustomName(player.getUniqueId(), totem.getId());

        if (customName != null) {
            // Usa nome customizado (roxo)
            displayName = ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + customName;
        } else {
            // Usa nome original (aqua)
            displayName = ChatColor.AQUA + "" + ChatColor.BOLD + totem.getName();
        }

        meta.setDisplayName(displayName);

        // ===== LORE (INFORMAÇÕES) =====

        List<String> lore = new ArrayList<>();

        // Nome do dono
        String ownerName = Bukkit.getOfflinePlayer(totem.getOwnerId()).getName();
        if (ownerName == null) {
            ownerName = "Desconhecido";
        }
        lore.add(ChatColor.GRAY + "Dono: " + ChatColor.WHITE + ownerName);

        // Localização
        lore.add(ChatColor.GRAY + "Localização: " + ChatColor.WHITE +
                totem.getLocation().getBlockX() + ", " +
                totem.getLocation().getBlockY() + ", " +
                totem.getLocation().getBlockZ());

        // Mundo
        lore.add(ChatColor.GRAY + "Mundo: " + ChatColor.WHITE + totem.getWorld().getName());

        // Data de criação
        String date = DATE_FORMAT.format(new Date(totem.getCreatedAt()));
        lore.add(ChatColor.GRAY + "Criado: " + ChatColor.WHITE + date);

        // Linha vazia
        lore.add("");

        // Instruções
        lore.add(ChatColor.YELLOW + "▸ " + ChatColor.GOLD + "Click para teleportar");
        lore.add(ChatColor.GRAY + "Shift + Click para renomear");

        meta.setLore(lore);

        // ===== MARCA O ITEM (para identificar no click) =====
        // Usamos PersistentDataContainer para guardar o UUID do totem
        meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "totem_id"),
                org.bukkit.persistence.PersistentDataType.STRING,
                totem.getId().toString()
        );

        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Verifica se é um jogador
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        // Verifica se é nosso menu (pelo título)
        if (!event.getView().getTitle().equals(MENU_TITLE)) {
            return;
        }

        // Cancela o evento (não permite pegar itens)
        event.setCancelled(true);

        // Verifica se clicou em um item
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // Pega o UUID do totem do item
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) {
            return;
        }

        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "totem_id");

        if (!meta.getPersistentDataContainer().has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
            return; // Não é um item de totem
        }

        String totemIdString = meta.getPersistentDataContainer().get(
                key,
                org.bukkit.persistence.PersistentDataType.STRING
        );

        if (totemIdString == null) {
            return;
        }

        UUID totemId;
        try {
            totemId = UUID.fromString(totemIdString);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("UUID de totem inválido no menu: " + totemIdString);
            return;
        }

        // Pega o totem
        Totem totem = plugin.getTotemManager().getTotemById(totemId);

        if (totem == null) {
            plugin.getMessageManager().sendMessage(player, "error-totem-invalid");
            player.closeInventory();
            return;
        }

        // ===== SHIFT + CLICK = RENOMEAR =====
        if (event.isShiftClick()) {
            handleRename(player, totem);
            return;
        }

        // ===== CLICK NORMAL = TELEPORTAR =====
        handleTeleport(player, totem);
    }

    private void handleTeleport(Player player, Totem totem) {
        // Fecha o menu
        player.closeInventory();

        // Inicia o teleporte
        boolean success = plugin.getTeleportHandler().startTeleport(player, totem);

        if (!success) {
            // Mensagem de erro já foi enviada pelo TeleportHandler
            // Pode reabrir o menu se quiser
            // open(player);
        }
    }

    private void handleRename(Player player, Totem totem) {
        // TODO: Implementar sistema de chat input
        // Por enquanto, apenas mensagem
        player.closeInventory();
        player.sendMessage(ChatColor.YELLOW + "Sistema de renomeação em breve!");

        // Exemplo de como seria:
        // 1. Fecha menu
        // 2. Pede ao jogador digitar novo nome no chat
        // 3. Escuta próxima mensagem do jogador
        // 4. Define o nome customizado
        // 5. Reabre o menu

        /*
        player.closeInventory();
        player.sendMessage(ChatColor.YELLOW + "Digite o novo nome do totem:");

        // Aguarda próxima mensagem (precisa de um ChatListener)
        // chatListener.waitForInput(player, (input) -> {
        //     plugin.getPlayerDataManager().setCustomName(
        //         player.getUniqueId(),
        //         totem.getId(),
        //         input
        //     );
        //     player.sendMessage(ChatColor.GREEN + "Totem renomeado!");
        //     open(player);
        // });
        */
    }

    /**
     * Atualiza o menu para um jogador.
     * Útil para recarregar após mudanças.
     *
     * @param player Jogador
     */
    public void refresh(Player player) {
        // Se o jogador está com o menu aberto
        if (player.getOpenInventory().getTitle().equals(MENU_TITLE)) {
            // Reconstrói e atualiza
            Inventory newInventory = buildInventory(player);
            player.getOpenInventory().getTopInventory().setContents(newInventory.getContents());
        }
    }
}

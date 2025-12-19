package com.jotage.jotageTotems.managers;

import com.jotage.jotageTotems.JotageTotems;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

public class RecipeManager {

    private final JotageTotems plugin;
    private NamespacedKey totemKey;
    private static final String TOTEM_NAME = ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Totem de Teleporte";

    private static final String[] TOTEM_LORE = {
            ChatColor.GRAY + "Um totem místico de teleporte",
            ChatColor.GRAY + "Shift + Click direito no chão para posicionar",
            "",
            ChatColor.DARK_PURPLE + "✦ " + ChatColor.LIGHT_PURPLE + "Jotage Totems"
    };

    public RecipeManager(JotageTotems plugin) {
        this.plugin = plugin;

        this.totemKey = new NamespacedKey(plugin, "totem_item");
    }

    public void registerTotemRecipe() {
        // Cria o item resultado
        ItemStack result = createTotemItem();

        // Cria a NamespacedKey para a receita
        NamespacedKey recipeKey = new NamespacedKey(plugin, "totem_recipe");

        // Remove receita antiga se existir (útil para /reload)
        try {
            plugin.getServer().removeRecipe(recipeKey);
        } catch (Exception ignored) {
            // Receita não existe ainda, tudo bem
        }

        // Cria a receita moldada (Shaped Recipe)
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);

        // Define o formato da receita
        // Linha 1: E E E
        // Linha 2: E T E
        // Linha 3: E E E
        recipe.shape(
                "EEE",
                "ETE",
                "EEE"
        );

        // Define os ingredientes
        recipe.setIngredient('E', Material.ENDER_PEARL);     // E = Ender Pearl
        recipe.setIngredient('T', Material.TOTEM_OF_UNDYING); // T = Totem of Undying

        // Registra a receita no servidor
        plugin.getServer().addRecipe(recipe);

        plugin.getLogger().info("Receita do Totem de Teleporte registrada");
    }

    public ItemStack createTotemItem() {
        // Cria o item base (Totem of Undying)
        ItemStack item = new ItemStack(Material.TOTEM_OF_UNDYING, 1);

        // Pega o ItemMeta (metadados do item)
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            plugin.getLogger().severe("ERRO: ItemMeta é null ao criar Totem de Teleporte!");
            return item;
        }

        // Define o nome customizado
        meta.setDisplayName(TOTEM_NAME);

        // Define a lore (descrição)
        meta.setLore(Arrays.asList(TOTEM_LORE));

        // Marca o item no PersistentDataContainer
        // Isso permite identificar o item mesmo se o jogador renomear
        meta.getPersistentDataContainer().set(
                totemKey,                      // Nossa chave única
                PersistentDataType.BYTE,       // Tipo de dado (1 byte é suficiente)
                (byte) 1                       // Valor (1 = é um totem)
        );

        // Aplica o meta no item
        item.setItemMeta(meta);

        return item;
    }

    public ItemStack createTotemItem(int amount) {
        ItemStack item = createTotemItem();
        item.setAmount(amount);
        return item;
    }

    public boolean isTotemItem(ItemStack item) {
        // Verifica se o item existe
        if (item == null || item.getType() != Material.TOTEM_OF_UNDYING) {
            return false;
        }

        // Verifica se tem meta
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        // Verifica se tem o marker no PersistentDataContainer
        return meta.getPersistentDataContainer().has(totemKey, PersistentDataType.BYTE);
    }

    public boolean isTotemItemByName(ItemStack item) {
        if (item == null || item.getType() != Material.TOTEM_OF_UNDYING) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }

        return meta.getDisplayName().equals(TOTEM_NAME);
    }

    public void unregisterRecipe() {
        NamespacedKey recipeKey = new NamespacedKey(plugin, "totem_recipe");
        plugin.getServer().removeRecipe(recipeKey);
    }

    public String getTotemItemName() {
        return TOTEM_NAME;
    }

    public String[] getTotemItemLore() {
        return TOTEM_LORE;
    }
}

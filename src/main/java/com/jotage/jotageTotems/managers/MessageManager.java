package com.jotage.jotageTotems.managers;

import com.jotage.jotageTotems.JotageTotems;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

public class MessageManager {

    private final JotageTotems plugin;
    private File messagesFile;
    private FileConfiguration messages;

    private String prefix;

    public MessageManager(JotageTotems plugin) {
        this.plugin = plugin;
        loadMessages();
        reload();
    }

    public void loadMessages() {
        // Cria a pasta do plugin se não existir
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        // Define o caminho do arquivo
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        // Se o arquivo não existir, copia o padrão da pasta resources/
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        // Carrega o arquivo YAML
        messages = YamlConfiguration.loadConfiguration(messagesFile);

        // Carrega valores padrão (do JAR) para garantir que nenhuma chave fique faltando
        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream)
            );
            messages.setDefaults(defaultConfig);

        }

        //Cacheio o prefixo
        prefix = colorize(messages.getString("prefix", "&8[&5Totem&8]&r"));
    }

    public void reload() {
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        prefix = colorize(messages.getString("prefix", "&8[&5Totem&8]&r"));
    }

    public void save() {
        try {
            messages.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Erro ao salvar messages.yml: " + e.getMessage());
        }
    }

    public String getMessage(String key) {
        String message = messages.getString(key);

        if (message == null) {
            plugin.getLogger().warning("Mensagem não encontrada: " + key);
            return colorize("&cMensagem não encontrada: " + key);
        }

        // Substitui {prefix} pelo prefixo
        message = message.replace("{prefix}", prefix);

        return colorize(message);
    }

    public String getMessage(String key, Map<String, String> placeholders) {
        String message = getMessage(key);

        // Substitui cada placeholder
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            message = message.replace(placeholder, entry.getValue());
        }

        return message;
    }

    public void sendMessage(Player player, String key) {
        player.sendMessage(getMessage(key));
    }

    public void sendMessage(Player player, String key, Map<String, String> placeholders) {
        player.sendMessage(getMessage(key, placeholders));
    }

    private String colorize(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public String getPrefix() {
        return prefix;
    }

    public FileConfiguration getMessages() {
        return messages;
    }
}

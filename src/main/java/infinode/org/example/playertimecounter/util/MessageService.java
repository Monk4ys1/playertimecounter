package infinode.org.example.playertimecounter.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lädt und sendet konfigurierbare Nachrichten (Legacy-&amp;-Farbcodes).
 * <p>
 * Unterstützt Platzhalter wie {@code {remaining}}, {@code {used}}, {@code {limit}}.
 */
public final class MessageService {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private final JavaPlugin plugin;
    private String prefix;
    private final Map<String, List<String>> messageLines = new HashMap<>();

    public MessageService(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    /** Liest Prefix und alle Message-Keys neu aus der Config. */
    public void reload() {
        FileConfiguration config = plugin.getConfig();
        prefix = config.getString("messages.prefix", "&8[&bZeitlimit&8]&r ");
        messageLines.clear();

        if (config.getConfigurationSection("messages") == null) {
            return;
        }
        for (String key : config.getConfigurationSection("messages").getKeys(false)) {
            if ("prefix".equals(key)) {
                continue;
            }
            String path = "messages." + key;
            if (config.isList(path)) {
                messageLines.put(key, new ArrayList<>(config.getStringList(path)));
            } else {
                String raw = config.getString(path, "");
                // Mehrzeilige Kick-/Deny-Texte (YAML |-) als einzelne Zeilen behandeln
                List<String> lines = new ArrayList<>();
                for (String line : raw.split("\\R")) {
                    lines.add(line);
                }
                messageLines.put(key, lines);
            }
        }
    }

    /**
     * Sendet eine Nachricht (mit Prefix) an einen CommandSender.
     *
     * @param recipient Empfänger
     * @param key       Key unter {@code messages.*}
     * @param placeholders Platzhalter ohne geschweifte Klammern → Wert
     */
    public void send(CommandSender recipient, String key, Map<String, String> placeholders) {
        for (String line : resolveLines(key, placeholders)) {
            recipient.sendMessage(colorize(prefix + line));
        }
    }

    /** Variante ohne zusätzliche Platzhalter. */
    public void send(CommandSender recipient, String key) {
        send(recipient, key, Map.of());
    }

    /**
     * Sendet Zeilen ohne Prefix (z. B. Kick-Grund).
     */
    public void sendRaw(CommandSender recipient, String key, Map<String, String> placeholders) {
        for (String line : resolveLines(key, placeholders)) {
            recipient.sendMessage(colorize(line));
        }
    }

    /**
     * Baut einen Adventure-{@link Component} aus einem Message-Key
     * (mehrere Zeilen werden mit Newline verbunden).
     */
    public Component component(String key, Map<String, String> placeholders) {
        List<String> lines = resolveLines(key, placeholders);
        if (lines.isEmpty()) {
            return Component.empty();
        }
        String joined = String.join("\n", lines);
        return colorize(joined);
    }

    /** Actionbar-Nachricht an einen Spieler. */
    public void sendActionBar(Player player, String key, Map<String, String> placeholders) {
        List<String> lines = resolveLines(key, placeholders);
        if (lines.isEmpty()) {
            return;
        }
        player.sendActionBar(colorize(lines.getFirst()));
    }

    private List<String> resolveLines(String key, Map<String, String> placeholders) {
        List<String> template = messageLines.getOrDefault(key, List.of("&cFehlende Nachricht: " + key));
        List<String> resolved = new ArrayList<>(template.size());
        for (String line : template) {
            String result = line;
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                result = result.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            resolved.add(result);
        }
        return resolved;
    }

    private static Component colorize(String input) {
        return LEGACY.deserialize(input == null ? "" : input);
    }
}

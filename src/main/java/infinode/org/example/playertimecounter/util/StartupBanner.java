package infinode.org.example.playertimecounter.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * Farbige, klar strukturierte Konsolen-Banner für Enable/Disable.
 * <p>
 * Nutzt Adventure/Legacy auf dem Konsolen-Audience (Paper),
 * Fallback auf den normalen Logger.
 */
public final class StartupBanner {

    private StartupBanner() {
    }

    public static void printEnable(JavaPlugin plugin, long dailyLimitMinutes, int resetHour, boolean tabEnabled) {
        String version = plugin.getPluginMeta().getVersion();
        String[] lines = {
                "",
                "&b╔══════════════════════════════════════════╗",
                "&b║&f   PlayerTimeCounter &8v" + padRight(version, 8) + "             &b║",
                "&b║&7   Tägliches Spielzeit-Limit              &b║",
                "&b╠══════════════════════════════════════════╣",
                "&b║ &a✓ &7Status      &8→ &faktiviert               &b║",
                "&b║ &a✓ &7Tageslimit  &8→ &f" + padRight(dailyLimitMinutes + " Min.", 22) + "&b║",
                "&b║ &a✓ &7Reset       &8→ &f" + padRight(TimeFormatter.formatResetHour(resetHour) + " Uhr", 22) + "&b║",
                "&b║ &a✓ &7TAB-Anzeige &8→ &f" + padRight(tabEnabled ? "aktiv" : "aus", 22) + "&b║",
                "&b║ &a✓ &7Server      &8→ &f" + padRight(shortServerName(), 22) + "&b║",
                "&b╚══════════════════════════════════════════╝",
                ""
        };
        send(plugin, lines);
    }

    public static void printDisable(JavaPlugin plugin) {
        String[] lines = {
                "",
                "&8╔══════════════════════════════════════════╗",
                "&8║&7   PlayerTimeCounter                      &8║",
                "&8║ &c○ &7deaktiviert – Spielzeiten gespeichert  &8║",
                "&8╚══════════════════════════════════════════╝",
                ""
        };
        send(plugin, lines);
    }

    public static void printReload(JavaPlugin plugin, long dailyLimitMinutes) {
        Logger logger = plugin.getLogger();
        String msg = "Config neu geladen · Tageslimit: " + dailyLimitMinutes + " Min.";
        try {
            Bukkit.getConsoleSender().sendMessage(
                    net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                            .legacyAmpersand()
                            .deserialize("&8[&bZeitlimit&8] &a" + msg)
            );
        } catch (Exception ignored) {
            logger.info(msg);
        }
    }

    private static void send(JavaPlugin plugin, String[] lines) {
        var serializer = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand();
        try {
            var console = Bukkit.getConsoleSender();
            for (String line : lines) {
                if (line.isEmpty()) {
                    console.sendMessage(net.kyori.adventure.text.Component.empty());
                } else {
                    console.sendMessage(serializer.deserialize(line));
                }
            }
        } catch (Exception ex) {
            Logger logger = plugin.getLogger();
            for (String line : lines) {
                logger.info(line.replaceAll("&[0-9a-fk-or]", ""));
            }
        }
    }

    private static String shortServerName() {
        String name = Bukkit.getName() + " " + Bukkit.getBukkitVersion();
        if (name.length() > 20) {
            return name.substring(0, 20);
        }
        return name;
    }

    private static String padRight(String value, int width) {
        if (value.length() >= width) {
            return value.substring(0, width);
        }
        return value + " ".repeat(width - value.length());
    }
}

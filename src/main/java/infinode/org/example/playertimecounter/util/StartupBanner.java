package infinode.org.example.playertimecounter.util;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * Klare Konsolen-Ausgabe für Enable, Disable und Reload.
 * Farbcodes werden über den Paper-Konsolen-Sender gerendert.
 */
public final class StartupBanner {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final String RULE = "&8&m                                                &r";

    private StartupBanner() {
    }

    public static void printEnable(JavaPlugin plugin, long dailyLimitMinutes, int resetHour, boolean tabEnabled) {
        String version = plugin.getPluginMeta().getVersion();
        send(plugin, new String[]{
                "",
                RULE,
                "&b  PlayerTimeCounter &8v" + version,
                "&7  Tägliches Spielzeit-Limit pro Spieler",
                "&8  - &7Status    &faktiviert",
                "&8  - &7Limit     &f" + dailyLimitMinutes + " Minuten",
                "&8  - &7Reset     &f" + TimeFormatter.formatResetHour(resetHour) + " Uhr",
                "&8  - &7TAB       &f" + (tabEnabled ? "Restzeit rechts in der Liste" : "aus"),
                "&8  - &7Befehle   &f/playtime &8| &f/playtimetest &8| &f/playtimereload",
                "&8  - &7Bypass    &fplayertimecounter.bypass &8(standard: niemand)",
                RULE,
                ""
        });
    }

    public static void printDisable(JavaPlugin plugin) {
        send(plugin, new String[]{
                "",
                RULE,
                "&7  PlayerTimeCounter &8deaktiviert",
                "&7  Spielzeiten gespeichert.",
                RULE,
                ""
        });
    }

    public static void printReload(JavaPlugin plugin, long dailyLimitMinutes) {
        send(plugin, new String[]{
                "&8[&bZeitlimit&8] &aConfig neu geladen. &7Tageslimit: &f" + dailyLimitMinutes + " Minuten"
        });
    }

    private static void send(JavaPlugin plugin, String[] lines) {
        try {
            var console = Bukkit.getConsoleSender();
            for (String line : lines) {
                console.sendMessage(line.isEmpty()
                        ? net.kyori.adventure.text.Component.empty()
                        : LEGACY.deserialize(line));
            }
        } catch (Exception ex) {
            Logger logger = plugin.getLogger();
            for (String line : lines) {
                logger.info(line.replaceAll("&[0-9a-fk-or]", ""));
            }
        }
    }
}

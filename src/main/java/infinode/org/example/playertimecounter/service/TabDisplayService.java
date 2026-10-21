package infinode.org.example.playertimecounter.service;

import infinode.org.example.playertimecounter.config.PluginSettings;
import infinode.org.example.playertimecounter.util.MessageService;
import infinode.org.example.playertimecounter.util.TimeFormatter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Zeigt die verbleibende Tages-Spielzeit in der TAB-Liste:
 * <ul>
 *   <li>formatierter Text hinter dem Spielernamen (z. B. {@code 1h 59m})</li>
 *   <li>optional Header/Footer</li>
 * </ul>
 * Die gelbe Scoreboard-Zahl rechts wird bewusst nicht gesetzt.
 * Ein altes Objective {@code ptc_remain} wird entfernt, falls es noch existiert.
 */
public final class TabDisplayService {

    private static final String OBJECTIVE_NAME = "ptc_remain";

    private final TimeLimitService timeLimitService;
    private final MessageService messages;
    private PluginSettings settings;
    /** Letzter gerenderter Listenname, damit die TAB-Zeile nicht flackert. */
    private final Map<UUID, String> lastListName = new HashMap<>();

    public TabDisplayService(
            TimeLimitService timeLimitService,
            MessageService messages,
            PluginSettings settings
    ) {
        this.timeLimitService = timeLimitService;
        this.messages = messages;
        this.settings = settings;
    }

    public void updateSettings(PluginSettings settings) {
        this.settings = settings;
        lastListName.clear();
        if (!settings.isTabEnabled()) {
            clearAll();
            return;
        }
        if (!settings.isTabHeaderFooterEnabled()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
            }
        }
        refreshAll();
    }

    /** Aktualisiert TAB für einen Spieler (Join / manuelle Änderung). */
    public void updatePlayer(Player player) {
        if (!settings.isTabEnabled()) {
            return;
        }
        applyPlayerListName(player);
        applyHeaderFooter(player);
        unregisterObjectives();
    }

    /** Periodisches Update für alle Online-Spieler. */
    public void refreshAll() {
        if (!settings.isTabEnabled()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyPlayerListName(player);
            applyHeaderFooter(player);
        }
        unregisterObjectives();
    }

    private void unregisterObjectives() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Objective objective = viewer.getScoreboard().getObjective(OBJECTIVE_NAME);
            if (objective != null) {
                objective.unregister();
            }
        }
        Objective mainObjective = Bukkit.getScoreboardManager()
                .getMainScoreboard()
                .getObjective(OBJECTIVE_NAME);
        if (mainObjective != null) {
            mainObjective.unregister();
        }
    }

    /** Entfernt TAB-Anpassungen beim Quit. */
    public void clearPlayer(Player player) {
        lastListName.remove(player.getUniqueId());
        player.playerListName(null);
    }

    /** Setzt beim Disable alles zurück. */
    public void clearAll() {
        lastListName.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playerListName(null);
            player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard board = viewer.getScoreboard();
            Objective objective = board.getObjective(OBJECTIVE_NAME);
            if (objective != null) {
                objective.unregister();
            }
        }
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective mainObjective = main.getObjective(OBJECTIVE_NAME);
        if (mainObjective != null) {
            mainObjective.unregister();
        }
    }

    // -------------------------------------------------------------------------

    private void applyPlayerListName(Player player) {
        if (!settings.isTabSuffixEnabled()) {
            if (lastListName.remove(player.getUniqueId()) != null) {
                player.playerListName(null);
            }
            return;
        }

        Map<String, String> placeholders = buildTabPlaceholders(player);
        String rendered = messages.applyPlaceholders(settings.getTabSuffixFormat(), placeholders);
        String previous = lastListName.put(player.getUniqueId(), rendered);
        if (rendered.equals(previous)) {
            return;
        }

        player.playerListName(
                Component.text()
                        .append(Component.text(player.getName()))
                        .append(messages.colorize(rendered))
                        .build()
        );
    }

    private void applyHeaderFooter(Player player) {
        if (!settings.isTabHeaderFooterEnabled()) {
            return;
        }
        Map<String, String> placeholders = buildTabPlaceholders(player);
        Component header = messages.colorizeMultiline(
                messages.applyPlaceholders(settings.getTabHeader(), placeholders)
        );
        Component footer = messages.colorizeMultiline(
                messages.applyPlaceholders(settings.getTabFooter(), placeholders)
        );
        player.sendPlayerListHeaderAndFooter(header, footer);
    }

    private Map<String, String> buildTabPlaceholders(Player player) {
        Map<String, String> map = new HashMap<>(timeLimitService.placeholders(player));
        long remainingMs = timeLimitService.hasBypass(player)
                ? settings.getDailyLimitMs()
                : timeLimitService.getRemainingMs(player.getUniqueId());

        boolean bypass = timeLimitService.hasBypass(player);
        map.put("remaining_compact", bypass
                ? settings.getTabBypassLabel()
                : TimeFormatter.formatCompact(remainingMs));
        map.put("remaining_minutes", String.valueOf(minutesRemaining(remainingMs)));
        map.put("color", bypass ? "&d" : remainingColor(remainingMs));
        map.put("online", String.valueOf(Bukkit.getOnlinePlayers().size()));
        return map;
    }

    private static int minutesRemaining(long remainingMs) {
        return (int) Math.ceil(Math.max(0L, remainingMs) / 60_000.0);
    }

    /**
     * Farbcode je nach Restanteil am Tageslimit (&amp;a / &amp;e / &amp;c).
     */
    private String remainingColor(long remainingMs) {
        double ratio = settings.getDailyLimitMs() <= 0
                ? 0
                : (double) remainingMs / settings.getDailyLimitMs();
        if (ratio > 0.5) {
            return "&a";
        }
        if (ratio > 0.2) {
            return "&e";
        }
        return "&c";
    }
}

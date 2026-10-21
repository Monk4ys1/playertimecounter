package infinode.org.example.playertimecounter.service;

import infinode.org.example.playertimecounter.config.PluginSettings;
import infinode.org.example.playertimecounter.util.MessageService;
import infinode.org.example.playertimecounter.util.TimeFormatter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;

/**
 * Zeigt die verbleibende Tages-Spielzeit in der TAB-Liste:
 * <ul>
 *   <li>gelbe Zahl rechts (Scoreboard {@link DisplaySlot#PLAYER_LIST}, Minuten)</li>
 *   <li>formatierter Suffix hinter dem Spielernamen</li>
 *   <li>optional Header/Footer</li>
 * </ul>
 */
public final class TabDisplayService {

    private static final String OBJECTIVE_NAME = "ptc_remain";

    private final TimeLimitService timeLimitService;
    private final MessageService messages;
    private PluginSettings settings;

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
        if (!settings.isTabEnabled()) {
            clearAll();
        } else {
            refreshAll();
        }
    }

    /** Aktualisiert TAB für einen Spieler (Join / manuelle Änderung). */
    public void updatePlayer(Player player) {
        if (!settings.isTabEnabled()) {
            return;
        }
        applyPlayerListName(player);
        applyHeaderFooter(player);
        syncScoresOnAllBoards();
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
        if (settings.isTabScoreEnabled()) {
            syncScoresOnAllBoards();
        } else {
            unregisterObjectives();
        }
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
        player.playerListName(null);
        player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
        removeScore(player.getName());
    }

    /** Setzt beim Disable alles zurück. */
    public void clearAll() {
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
            player.playerListName(null);
            return;
        }

        Map<String, String> placeholders = buildTabPlaceholders(player);
        String template = settings.getTabSuffixFormat();
        String rendered = messages.applyPlaceholders(template, placeholders);
        Component suffix = messages.colorize(rendered);

        player.playerListName(
                Component.text()
                        .append(Component.text(player.getName()))
                        .append(suffix)
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

    /**
     * Schreibt die Rest-Minuten als Score auf jedes aktive Scoreboard,
     * damit die gelbe Zahl rechts im TAB bei allen sichtbar ist.
     */
    private void syncScoresOnAllBoards() {
        if (!settings.isTabScoreEnabled()) {
            return;
        }

        Map<String, Integer> scores = new HashMap<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            scores.put(online.getName(), remainingMinutesForTab(online));
        }

        // Haupt-Scoreboard + alle Spieler-Scoreboards (andere Plugins)
        ensureAndWrite(Bukkit.getScoreboardManager().getMainScoreboard(), scores);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            ensureAndWrite(viewer.getScoreboard(), scores);
        }
    }

    private void ensureAndWrite(Scoreboard board, Map<String, Integer> scores) {
        Objective objective = board.getObjective(OBJECTIVE_NAME);
        if (objective == null) {
            objective = board.registerNewObjective(
                    OBJECTIVE_NAME,
                    Criteria.DUMMY,
                    messages.colorize(settings.getTabScoreTitle())
            );
        }
        if (objective.getDisplaySlot() != DisplaySlot.PLAYER_LIST) {
            objective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        }

        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            objective.getScore(entry.getKey()).setScore(entry.getValue());
        }
    }

    private void removeScore(String playerName) {
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        resetScore(main, playerName);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            resetScore(viewer.getScoreboard(), playerName);
        }
    }

    private void resetScore(Scoreboard board, String playerName) {
        Objective objective = board.getObjective(OBJECTIVE_NAME);
        if (objective != null) {
            board.resetScores(playerName);
        }
    }

    private int remainingMinutesForTab(Player player) {
        if (timeLimitService.hasBypass(player)) {
            // Bypass: volle Limit-Minuten als Anzeige (kein Countdown)
            return (int) (settings.getDailyLimitMs() / 60_000L);
        }
        long remainingMs = timeLimitService.getRemainingMs(player.getUniqueId());
        return (int) Math.ceil(remainingMs / 60_000.0);
    }

    private Map<String, String> buildTabPlaceholders(Player player) {
        Map<String, String> map = new HashMap<>(timeLimitService.placeholders(player));
        long remainingMs = timeLimitService.hasBypass(player)
                ? settings.getDailyLimitMs()
                : timeLimitService.getRemainingMs(player.getUniqueId());

        map.put("remaining_compact", timeLimitService.hasBypass(player)
                ? settings.getTabBypassLabel()
                : TimeFormatter.formatCompact(remainingMs));
        map.put("remaining_minutes", String.valueOf(remainingMinutesForTab(player)));
        map.put("color", remainingColor(remainingMs));
        map.put("online", String.valueOf(Bukkit.getOnlinePlayers().size()));
        return map;
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

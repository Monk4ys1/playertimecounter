package infinode.org.example.playertimecounter.service;

import infinode.org.example.playertimecounter.config.PluginSettings;
import infinode.org.example.playertimecounter.data.PlayerDayRecord;
import infinode.org.example.playertimecounter.data.PlayerTimeStorage;
import infinode.org.example.playertimecounter.util.MessageService;
import infinode.org.example.playertimecounter.util.TimeFormatter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kernlogik: Spielzeit zählen, Limit prüfen, Kick/Join-Sperre,
 * sporadische Erinnerungen und Warnschwellen.
 */
public final class TimeLimitService {

    private final JavaPlugin plugin;
    private final PlayerTimeStorage storage;
    private final MessageService messages;
    private PluginSettings settings;
    private TabDisplayService tabDisplay;

    /** Session-Start (Epoch-Millis) pro online Spieler. */
    private final Map<UUID, Long> sessionStartMs = new ConcurrentHashMap<>();

    /** Zeitpunkt der letzten periodischen Erinnerung. */
    private final Map<UUID, Long> lastReminderMs = new ConcurrentHashMap<>();

    /** Letzter Auto-Save (Epoch-Millis). */
    private long lastAutoSaveMs = System.currentTimeMillis();

    public TimeLimitService(
            JavaPlugin plugin,
            PlayerTimeStorage storage,
            MessageService messages,
            PluginSettings settings
    ) {
        this.plugin = plugin;
        this.storage = storage;
        this.messages = messages;
        this.settings = settings;
    }

    /**
     * Verbindet die TAB-Anzeige und startet Sessions für bereits online Spieler.
     * Muss nach der Konstruktion aufgerufen werden (Zyklus vermeiden).
     */
    public void attachTabDisplay(TabDisplayService tabDisplay) {
        this.tabDisplay = tabDisplay;
        for (Player player : Bukkit.getOnlinePlayers()) {
            handleJoin(player);
        }
    }

    /** Tauscht die Einstellungen nach einem Config-Reload aus. */
    public void updateSettings(PluginSettings settings) {
        this.settings = settings;
        messages.reload();
        if (tabDisplay != null) {
            tabDisplay.updateSettings(settings);
        }
    }

    public PluginSettings getSettings() {
        return settings;
    }

    /**
     * Wird beim erfolgreichen Join aufgerufen.
     * Startet die Session und zeigt die Restzeit einmalig an.
     */
    public void handleJoin(Player player) {
        if (hasBypass(player)) {
            sessionStartMs.remove(player.getUniqueId());
            if (tabDisplay != null) {
                tabDisplay.updatePlayer(player);
            }
            return;
        }

        ensureCurrentDay(player.getUniqueId());
        sessionStartMs.put(player.getUniqueId(), System.currentTimeMillis());
        lastReminderMs.put(player.getUniqueId(), System.currentTimeMillis());

        if (tabDisplay != null) {
            tabDisplay.updatePlayer(player);
        }

        // Kurze Begrüßung mit Restzeit (nach 1 Tick, damit Client bereit ist)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                sendRemainingInfo(player, true);
            }
        }, 20L);
    }

    /**
     * Prüft vor dem Join, ob das Tageslimit bereits erreicht ist.
     *
     * @return {@code true}, wenn der Join erlaubt ist
     */
    public boolean canJoin(Player player) {
        if (hasBypass(player)) {
            return true;
        }

        PlayerDayRecord record = ensureCurrentDay(player.getUniqueId());
        if (record.getUsedMs() >= settings.getDailyLimitMs()) {
            return false;
        }
        return true;
    }

    /** Kick- bzw. Deny-Komponente für Limit-Überschreitung. */
    public Component denyComponent(Player player) {
        return messages.component("deny-join", placeholders(player));
    }

    public Component kickComponent(Player player) {
        return messages.component("kick", placeholders(player));
    }

    /** Speichert die Session-Zeit beim Quit. */
    public void handleQuit(Player player) {
        flushSession(player.getUniqueId());
        sessionStartMs.remove(player.getUniqueId());
        lastReminderMs.remove(player.getUniqueId());
        if (tabDisplay != null) {
            tabDisplay.clearPlayer(player);
        }
        storage.save();
    }

    /**
     * Periodischer Tick: Session-Zeit aufaddieren, Limit/Warnungen prüfen,
     * TAB aktualisieren. Alle 60 Sekunden zusätzlich auf Disk speichern.
     */
    public void tickOnlinePlayers() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (hasBypass(player)) {
                continue;
            }
            tickPlayer(player, now);
        }

        if (tabDisplay != null) {
            tabDisplay.refreshAll();
        }

        if (now - lastAutoSaveMs >= 60_000L) {
            lastAutoSaveMs = now;
            for (UUID uuid : sessionStartMs.keySet()) {
                flushSession(uuid);
            }
            storage.save();
        }
    }

    private void tickPlayer(Player player, long now) {
        UUID uuid = player.getUniqueId();
        ensureCurrentDay(uuid);
        flushSession(uuid);
        sessionStartMs.put(uuid, now);

        PlayerDayRecord record = storage.getOrCreate(uuid, currentDayKey());
        long remaining = remainingMs(record);

        if (remaining <= 0L) {
            enforceLimit(player);
            return;
        }

        checkWarningThresholds(player, record, remaining);
        maybeSendReminder(player, remaining, now);
    }

    /** Kick und Session beenden, wenn das Limit erreicht ist. */
    private void enforceLimit(Player player) {
        flushSession(player.getUniqueId());
        sessionStartMs.remove(player.getUniqueId());
        storage.save();
        player.kick(kickComponent(player));
    }

    private void checkWarningThresholds(Player player, PlayerDayRecord record, long remaining) {
        for (long threshold : settings.getWarningThresholdsMs()) {
            if (remaining <= threshold && !record.hasWarningFired(threshold)) {
                record.markWarningFired(threshold);
                Map<String, String> placeholders = placeholders(player);
                messages.send(player, "warning", placeholders);
                messages.sendActionBar(player, "actionbar", placeholders);
            }
        }
    }

    private void maybeSendReminder(Player player, long remaining, long now) {
        int intervalSec = settings.getReminderIntervalSeconds();
        if (intervalSec <= 0) {
            return;
        }

        long last = lastReminderMs.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < intervalSec * 1000L) {
            return;
        }

        lastReminderMs.put(player.getUniqueId(), now);
        Map<String, String> placeholders = placeholders(player);
        messages.send(player, "reminder-chat", placeholders);
        messages.sendActionBar(player, "actionbar", placeholders);
    }

    /**
     * Zeigt Restzeit per Chat (und optional Actionbar) an.
     */
    public void sendRemainingInfo(Player player, boolean withActionBar) {
        if (hasBypass(player)) {
            messages.send(player, "bypass");
            return;
        }

        flushSession(player.getUniqueId());
        ensureCurrentDay(player.getUniqueId());

        Map<String, String> placeholders = placeholders(player);
        messages.send(player, "remaining", placeholders);
        if (withActionBar) {
            messages.sendActionBar(player, "actionbar", placeholders);
        }
    }

    // -------------------------------------------------------------------------
    // Test-/Admin-Manipulationen
    // -------------------------------------------------------------------------

    /** Setzt die verbrauchte Zeit absolut (in Minuten). */
    public void setUsedMinutes(Player target, long minutes) {
        ensureCurrentDay(target.getUniqueId());
        PlayerDayRecord record = storage.getOrCreate(target.getUniqueId(), currentDayKey());
        record.setUsedMs(Math.max(0L, minutes) * 60_000L);
        record.clearWarnings();
        rearmSession(target);
        storage.save();
        refreshTab(target);
        maybeKickIfOverLimit(target);
    }

    /** Addiert verbrauchte Minuten. */
    public void addUsedMinutes(Player target, long minutes) {
        ensureCurrentDay(target.getUniqueId());
        PlayerDayRecord record = storage.getOrCreate(target.getUniqueId(), currentDayKey());
        record.addUsedMs(Math.max(0L, minutes) * 60_000L);
        rearmSession(target);
        storage.save();
        refreshTab(target);
        maybeKickIfOverLimit(target);
    }

    /** Setzt die Tageszeit eines Spielers auf 0 zurück. */
    public void resetPlayer(Player target) {
        PlayerDayRecord record = storage.getOrCreate(target.getUniqueId(), currentDayKey());
        record.setDayKey(currentDayKey());
        record.setUsedMs(0L);
        record.clearWarnings();
        rearmSession(target);
        lastReminderMs.put(target.getUniqueId(), System.currentTimeMillis());
        storage.save();
        refreshTab(target);
    }

    private void refreshTab(Player target) {
        if (tabDisplay != null && target.isOnline()) {
            tabDisplay.updatePlayer(target);
        }
    }

    private void maybeKickIfOverLimit(Player target) {
        if (!target.isOnline() || hasBypass(target)) {
            return;
        }
        PlayerDayRecord record = storage.getOrCreate(target.getUniqueId(), currentDayKey());
        if (record.getUsedMs() >= settings.getDailyLimitMs()) {
            enforceLimit(target);
        }
    }

    private void rearmSession(Player target) {
        if (target.isOnline() && !hasBypass(target)) {
            sessionStartMs.put(target.getUniqueId(), System.currentTimeMillis());
        }
    }

    // -------------------------------------------------------------------------
    // Abfragen / Platzhalter
    // -------------------------------------------------------------------------

    public long getUsedMs(UUID uuid) {
        flushSession(uuid);
        PlayerDayRecord record = ensureCurrentDay(uuid);
        return record.getUsedMs();
    }

    public long getRemainingMs(UUID uuid) {
        return Math.max(0L, settings.getDailyLimitMs() - getUsedMs(uuid));
    }

    public Map<String, String> placeholders(Player player) {
        flushSession(player.getUniqueId());
        PlayerDayRecord record = ensureCurrentDay(player.getUniqueId());
        long used = record.getUsedMs();
        long remaining = Math.max(0L, settings.getDailyLimitMs() - used);

        Map<String, String> map = new HashMap<>();
        map.put("player", player.getName());
        map.put("used", TimeFormatter.format(used));
        map.put("remaining", TimeFormatter.format(remaining));
        map.put("limit", TimeFormatter.format(settings.getDailyLimitMs()));
        map.put("reset", TimeFormatter.formatResetHour(settings.getResetHour()));
        return map;
    }

    public boolean hasBypass(Player player) {
        return player.hasPermission(settings.getBypassPermission());
    }

    public void saveAll() {
        for (UUID uuid : sessionStartMs.keySet()) {
            flushSession(uuid);
        }
        storage.save();
    }

    // -------------------------------------------------------------------------
    // Intern: Session & Tageswechsel
    // -------------------------------------------------------------------------

    private void flushSession(UUID uuid) {
        Long start = sessionStartMs.get(uuid);
        if (start == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long delta = now - start;
        if (delta <= 0L) {
            return;
        }
        PlayerDayRecord record = ensureCurrentDay(uuid);
        record.addUsedMs(delta);
        sessionStartMs.put(uuid, now);
    }

    private PlayerDayRecord ensureCurrentDay(UUID uuid) {
        String today = currentDayKey();
        PlayerDayRecord record = storage.getOrCreate(uuid, today);
        if (!today.equals(record.getDayKey())) {
            record.setDayKey(today);
            record.setUsedMs(0L);
            record.clearWarnings();
        }
        return record;
    }

    private long remainingMs(PlayerDayRecord record) {
        return Math.max(0L, settings.getDailyLimitMs() - record.getUsedMs());
    }

    /**
     * Tages-Schlüssel relativ zur konfigurierten Reset-Stunde.
     * Vor der Reset-Stunde zählt noch der vorherige Kalendertag.
     */
    public String currentDayKey() {
        LocalDateTime now = LocalDateTime.now();
        LocalTime reset = LocalTime.of(settings.getResetHour(), 0);
        LocalDate day = now.toLocalTime().isBefore(reset)
                ? now.toLocalDate().minusDays(1)
                : now.toLocalDate();
        return day + "@" + settings.getResetHour();
    }
}

package infinode.org.example.playertimecounter.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Typsichere Abbildung der {@code config.yml}.
 * <p>
 * Wird bei jedem Reload neu erzeugt und in den {@code TimeLimitService} übergeben.
 */
public final class PluginSettings {

    private final long dailyLimitMs;
    private final int trackerIntervalSeconds;
    private final int reminderIntervalSeconds;
    private final List<Long> warningThresholdsMs;
    private final int resetHour;
    private final String bypassPermission;

    /**
     * Liest alle Einstellungen aus der aktuellen Plugin-Config.
     *
     * @param plugin laufende Plugin-Instanz
     */
    public PluginSettings(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();

        int dailyMinutes = Math.max(1, config.getInt("daily-limit-minutes", 120));
        this.dailyLimitMs = dailyMinutes * 60_000L;

        this.trackerIntervalSeconds = Math.max(1, config.getInt("tracker-interval-seconds", 5));
        this.reminderIntervalSeconds = Math.max(0, config.getInt("reminder-interval-seconds", 300));

        List<Long> thresholds = new ArrayList<>();
        for (int minutes : config.getIntegerList("warning-thresholds-minutes")) {
            if (minutes > 0) {
                thresholds.add(minutes * 60_000L);
            }
        }
        // Absteigend sortieren, damit die größte Schwelle zuerst geprüft wird
        thresholds.sort(Collections.reverseOrder());
        this.warningThresholdsMs = List.copyOf(thresholds);

        int hour = config.getInt("reset-hour", 0);
        this.resetHour = Math.floorMod(hour, 24);

        this.bypassPermission = config.getString("bypass-permission", "playertimecounter.bypass");
    }

    /** Tageslimit in Millisekunden. */
    public long getDailyLimitMs() {
        return dailyLimitMs;
    }

    /** Intervall des Tracker-Tasks in Sekunden. */
    public int getTrackerIntervalSeconds() {
        return trackerIntervalSeconds;
    }

    /**
     * Intervall für periodische Chat-/Actionbar-Erinnerungen in Sekunden.
     * {@code 0} deaktiviert periodische Erinnerungen.
     */
    public int getReminderIntervalSeconds() {
        return reminderIntervalSeconds;
    }

    /**
     * Warnschwellen in Millisekunden Restzeit (absteigend sortiert).
     * Jede Schwelle wird pro Tag nur einmal ausgelöst.
     */
    public List<Long> getWarningThresholdsMs() {
        return warningThresholdsMs;
    }

    /** Stunde (0–23) des Tageswechsels in der Server-Zeitzone. */
    public int getResetHour() {
        return resetHour;
    }

    /** Permission, die das Limit komplett umgeht. */
    public String getBypassPermission() {
        return bypassPermission;
    }
}

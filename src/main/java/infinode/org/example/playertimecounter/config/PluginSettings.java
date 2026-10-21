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

    // TAB
    private final boolean tabEnabled;
    private final boolean tabSuffixEnabled;
    private final boolean tabHeaderFooterEnabled;
    private final String tabSuffixFormat;
    private final String tabHeader;
    private final String tabFooter;
    private final String tabBypassLabel;

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
        thresholds.sort(Collections.reverseOrder());
        this.warningThresholdsMs = List.copyOf(thresholds);

        int hour = config.getInt("reset-hour", 0);
        this.resetHour = Math.floorMod(hour, 24);

        this.bypassPermission = config.getString("bypass-permission", "playertimecounter.bypass");

        this.tabEnabled = config.getBoolean("tab.enabled", true);
        this.tabSuffixEnabled = config.getBoolean("tab.suffix-enabled", true);
        this.tabHeaderFooterEnabled = config.getBoolean("tab.header-footer-enabled", true);
        this.tabSuffixFormat = config.getString("tab.suffix-format", " &8· {color}{remaining_compact}");
        this.tabHeader = config.getString("tab.header", "&b&lZeitlimit\n&7Heute noch &f{remaining}");
        this.tabFooter = config.getString("tab.footer", "&8Reset um &7{reset} &8Uhr &8| &7{online} online");
        this.tabBypassLabel = config.getString("tab.bypass-label", "∞");
    }

    public long getDailyLimitMs() {
        return dailyLimitMs;
    }

    public int getTrackerIntervalSeconds() {
        return trackerIntervalSeconds;
    }

    public int getReminderIntervalSeconds() {
        return reminderIntervalSeconds;
    }

    public List<Long> getWarningThresholdsMs() {
        return warningThresholdsMs;
    }

    public int getResetHour() {
        return resetHour;
    }

    public String getBypassPermission() {
        return bypassPermission;
    }

    public boolean isTabEnabled() {
        return tabEnabled;
    }

    public boolean isTabSuffixEnabled() {
        return tabSuffixEnabled;
    }

    public boolean isTabHeaderFooterEnabled() {
        return tabHeaderFooterEnabled;
    }

    public String getTabSuffixFormat() {
        return tabSuffixFormat;
    }

    public String getTabHeader() {
        return tabHeader;
    }

    public String getTabFooter() {
        return tabFooter;
    }

    public String getTabBypassLabel() {
        return tabBypassLabel;
    }
}

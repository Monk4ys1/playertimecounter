package infinode.org.example.playertimecounter;

import infinode.org.example.playertimecounter.command.PlaytimeCommand;
import infinode.org.example.playertimecounter.command.PlaytimeReloadCommand;
import infinode.org.example.playertimecounter.command.PlaytimeTestCommand;
import infinode.org.example.playertimecounter.config.PluginSettings;
import infinode.org.example.playertimecounter.data.PlayerTimeStorage;
import infinode.org.example.playertimecounter.listener.PlayerConnectionListener;
import infinode.org.example.playertimecounter.service.TimeLimitService;
import infinode.org.example.playertimecounter.task.PlaytimeTrackerTask;
import infinode.org.example.playertimecounter.util.MessageService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * PlayerTimeCounter – Hauptklasse.
 * <p>
 * Begrenzt die tägliche Spielzeit pro Spieler. Bei Erreichen des Limits
 * wird der Spieler gekickt und bis zum nächsten Tageswechsel am Login gehindert.
 * <p>
 * Befehle:
 * <ul>
 *   <li>{@code /playtime} – Restzeit abfragen</li>
 *   <li>{@code /playtimetest} – Admin-Tests (set/add/reset/status)</li>
 *   <li>{@code /playtimereload} – Config neu laden</li>
 * </ul>
 */
public final class Playertimecounter extends JavaPlugin {

    private TimeLimitService timeLimitService;
    private MessageService messageService;
    private PlayerTimeStorage storage;
    private PlaytimeTrackerTask trackerTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.messageService = new MessageService(this);
        this.storage = new PlayerTimeStorage(this);
        PluginSettings settings = new PluginSettings(this);
        this.timeLimitService = new TimeLimitService(this, storage, messageService, settings);

        registerCommands();
        getServer().getPluginManager().registerEvents(
                new PlayerConnectionListener(timeLimitService),
                this
        );

        startTracker(settings.getTrackerIntervalSeconds());

        getLogger().info("PlayerTimeCounter aktiv – Tageslimit: "
                + (settings.getDailyLimitMs() / 60_000L) + " Minuten.");
    }

    @Override
    public void onDisable() {
        if (trackerTask != null) {
            trackerTask.cancel();
            trackerTask = null;
        }
        if (timeLimitService != null) {
            timeLimitService.saveAll();
        }
        getLogger().info("PlayerTimeCounter deaktiviert – Spielzeiten gespeichert.");
    }

    /**
     * Lädt die Config neu und startet den Tracker mit dem neuen Intervall.
     */
    public void reloadPluginConfig() {
        reloadConfig();
        PluginSettings settings = new PluginSettings(this);
        timeLimitService.updateSettings(settings);

        if (trackerTask != null) {
            trackerTask.cancel();
        }
        startTracker(settings.getTrackerIntervalSeconds());
    }

    private void startTracker(int intervalSeconds) {
        trackerTask = new PlaytimeTrackerTask(timeLimitService);
        // delay 20 ticks (1s), dann periodisch
        trackerTask.runTaskTimer(this, 20L, intervalSeconds * 20L);
    }

    private void registerCommands() {
        PluginCommand playtime = getCommand("playtime");
        if (playtime != null) {
            playtime.setExecutor(new PlaytimeCommand(timeLimitService, messageService));
        }

        PluginCommand test = getCommand("playtimetest");
        if (test != null) {
            PlaytimeTestCommand testCommand = new PlaytimeTestCommand(timeLimitService, messageService);
            test.setExecutor(testCommand);
            test.setTabCompleter(testCommand);
        }

        PluginCommand reload = getCommand("playtimereload");
        if (reload != null) {
            reload.setExecutor(new PlaytimeReloadCommand(this, messageService));
        }
    }
}

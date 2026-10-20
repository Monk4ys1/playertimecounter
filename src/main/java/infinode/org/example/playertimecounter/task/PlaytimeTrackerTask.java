package infinode.org.example.playertimecounter.task;

import infinode.org.example.playertimecounter.service.TimeLimitService;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Periodischer Task, der die Online-Spielzeit aller Spieler fortschreibt
 * und bei Bedarf Erinnerungen / Kicks auslöst.
 */
public final class PlaytimeTrackerTask extends BukkitRunnable {

    private final TimeLimitService timeLimitService;

    public PlaytimeTrackerTask(TimeLimitService timeLimitService) {
        this.timeLimitService = timeLimitService;
    }

    @Override
    public void run() {
        timeLimitService.tickOnlinePlayers();
    }
}

package infinode.org.example.playertimecounter.listener;

import infinode.org.example.playertimecounter.service.TimeLimitService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Verbindet Join/Quit mit dem {@link TimeLimitService}.
 * <p>
 * Login wird blockiert, wenn das Tageslimit bereits aufgebraucht ist.
 */
public final class PlayerConnectionListener implements Listener {

    private final TimeLimitService timeLimitService;

    public PlayerConnectionListener(TimeLimitService timeLimitService) {
        this.timeLimitService = timeLimitService;
    }

    /**
     * Früher Login-Check: Kick-Grund anzeigen, bevor die Welt geladen wird.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        if (!timeLimitService.canJoin(player)) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, timeLimitService.denyComponent(player));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        timeLimitService.handleJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        timeLimitService.handleQuit(event.getPlayer());
    }
}

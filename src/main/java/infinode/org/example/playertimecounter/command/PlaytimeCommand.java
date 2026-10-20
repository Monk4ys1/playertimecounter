package infinode.org.example.playertimecounter.command;

import infinode.org.example.playertimecounter.service.TimeLimitService;
import infinode.org.example.playertimecounter.util.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /playtime} – zeigt dem Spieler seine verbleibende Tageszeit.
 */
public final class PlaytimeCommand implements CommandExecutor {

    private final TimeLimitService timeLimitService;
    private final MessageService messages;

    public PlaytimeCommand(TimeLimitService timeLimitService, MessageService messages) {
        this.timeLimitService = timeLimitService;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "player-only");
            return true;
        }

        timeLimitService.sendRemainingInfo(player, true);
        return true;
    }
}

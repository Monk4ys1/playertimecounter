package infinode.org.example.playertimecounter.command;

import infinode.org.example.playertimecounter.Playertimecounter;
import infinode.org.example.playertimecounter.util.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /playtimereload} – lädt {@code config.yml} neu.
 */
public final class PlaytimeReloadCommand implements CommandExecutor {

    private final Playertimecounter plugin;
    private final MessageService messages;

    public PlaytimeReloadCommand(Playertimecounter plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!sender.hasPermission("playertimecounter.reload")) {
            messages.send(sender, "no-permission");
            return true;
        }

        plugin.reloadPluginConfig();
        messages.send(sender, "reload-success");
        return true;
    }
}

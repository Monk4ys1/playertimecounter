package infinode.org.example.playertimecounter.command;

import infinode.org.example.playertimecounter.service.TimeLimitService;
import infinode.org.example.playertimecounter.util.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@code /playtimetest} – Admin-Hilfen zum Testen des Zeitlimits.
 * <pre>
 * /playtimetest set &lt;Minuten&gt; [Spieler]   – verbrauchte Zeit setzen
 * /playtimetest add &lt;Minuten&gt; [Spieler]   – Minuten hinzufügen
 * /playtimetest reset [Spieler]           – Tageszeit zurücksetzen
 * /playtimetest status [Spieler]          – Status anzeigen
 * </pre>
 */
public final class PlaytimeTestCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("set", "add", "reset", "status");

    private final TimeLimitService timeLimitService;
    private final MessageService messages;

    public PlaytimeTestCommand(TimeLimitService timeLimitService, MessageService messages) {
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
        if (!sender.hasPermission("playertimecounter.test")) {
            messages.send(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            messages.send(sender, "test-usage");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "set" -> handleSet(sender, args);
            case "add" -> handleAdd(sender, args);
            case "reset" -> handleReset(sender, args);
            case "status" -> handleStatus(sender, args);
            default -> {
                messages.send(sender, "test-usage");
                yield true;
            }
        };
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messages.send(sender, "test-usage");
            return true;
        }

        Long minutes = parseMinutes(args[1]);
        if (minutes == null) {
            messages.send(sender, "test-usage");
            return true;
        }

        Player target = resolveTarget(sender, args, 2);
        if (target == null) {
            return true;
        }

        timeLimitService.setUsedMinutes(target, minutes);
        // Player-Objekt bleibt nach Kick nutzbar (UUID/Name/Platzhalter)
        messages.send(sender, "test-set", timeLimitService.placeholders(target));
        return true;
    }

    private boolean handleAdd(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messages.send(sender, "test-usage");
            return true;
        }

        Long minutes = parseMinutes(args[1]);
        if (minutes == null) {
            messages.send(sender, "test-usage");
            return true;
        }

        Player target = resolveTarget(sender, args, 2);
        if (target == null) {
            return true;
        }

        timeLimitService.addUsedMinutes(target, minutes);
        messages.send(sender, "test-add", timeLimitService.placeholders(target));
        return true;
    }

    private boolean handleReset(CommandSender sender, String[] args) {
        Player target = resolveTarget(sender, args, 1);
        if (target == null) {
            return true;
        }

        timeLimitService.resetPlayer(target);
        messages.send(sender, "test-reset", Map.of("player", target.getName()));
        return true;
    }

    private boolean handleStatus(CommandSender sender, String[] args) {
        Player target = resolveTarget(sender, args, 1);
        if (target == null) {
            return true;
        }

        messages.send(sender, "test-status", timeLimitService.placeholders(target));
        return true;
    }

    /**
     * Löst den Zielspieler auf: optionaler Name an {@code nameIndex},
     * sonst der Sender selbst (muss Spieler sein).
     */
    private Player resolveTarget(CommandSender sender, String[] args, int nameIndex) {
        if (args.length > nameIndex) {
            Player target = Bukkit.getPlayerExact(args[nameIndex]);
            if (target == null) {
                messages.send(sender, "player-not-found", Map.of("player", args[nameIndex]));
                return null;
            }
            return target;
        }

        if (sender instanceof Player player) {
            return player;
        }

        messages.send(sender, "player-only");
        return null;
    }

    private static Long parseMinutes(String raw) {
        try {
            long value = Long.parseLong(raw);
            return value < 0 ? null : value;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (!sender.hasPermission("playertimecounter.test")) {
            return List.of();
        }

        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && (sub.equals("set") || sub.equals("add"))) {
            return filter(List.of("0", "1", "5", "10", "30", "60", "120"), args[1]);
        }

        int playerArgIndex = (sub.equals("set") || sub.equals("add")) ? 3 : 2;
        if (args.length == playerArgIndex || (args.length == 2 && (sub.equals("reset") || sub.equals("status")))) {
            String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        return List.of();
    }

    private static List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower))
                .collect(Collectors.toCollection(ArrayList::new));
    }
}

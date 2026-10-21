package infinode.org.example.playertimecounter.util;

/**
 * Formatiert Millisekunden in lesbare Zeitangaben (z. B. {@code 1h 23m 05s}).
 */
public final class TimeFormatter {

    private TimeFormatter() {
    }

    /**
     * Formatiert eine Dauer als Stunden/Minuten/Sekunden.
     * Negative Werte werden als {@code 0s} dargestellt.
     *
     * @param millis Dauer in Millisekunden
     * @return formatierter String
     */
    public static String format(long millis) {
        long totalSeconds = Math.max(0L, millis) / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        if (hours > 0) {
            return String.format("%dh %02dm %02ds", hours, minutes, seconds);
        }
        if (minutes > 0) {
            return String.format("%dm %02ds", minutes, seconds);
        }
        return String.format("%ds", seconds);
    }

    /**
     * Kompakte Darstellung für die TAB-Liste (ohne Sekunden).
     * Beispiele: {@code 2h 05m}, {@code 45m}, {@code &lt;1m}.
     */
    public static String formatCompact(long millis) {
        long totalSeconds = Math.max(0L, millis) / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;

        if (hours > 0) {
            return String.format("%dh %02dm", hours, minutes);
        }
        if (minutes > 0) {
            return minutes + "m";
        }
        return "<1m";
    }

    /**
     * Formatiert die Reset-Stunde als {@code HH:00}.
     *
     * @param hour Stunde 0–23
     * @return z. B. {@code 04:00}
     */
    public static String formatResetHour(int hour) {
        return String.format("%02d:00", Math.floorMod(hour, 24));
    }
}

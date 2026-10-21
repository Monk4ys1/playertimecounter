package infinode.org.example.playertimecounter.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Berechnet den „Plugin-Tag“.
 * <p>
 * Liegt die aktuelle Uhrzeit vor {@code resetHour}, zählt noch der vorherige
 * Kalendertag. So lässt sich der Reset z. B. auf 04:00 legen.
 */
public final class DayCycle {

    private DayCycle() {
    }

    /**
     * Schlüssel für Speicher und Tageswechsel, z. B. {@code 2026-10-21@4}.
     *
     * @param now       aktuelle Serverzeit
     * @param resetHour Stunde 0–23 (Werte außerhalb werden normalisiert)
     */
    public static String dayKey(LocalDateTime now, int resetHour) {
        int hour = Math.floorMod(resetHour, 24);
        LocalTime reset = LocalTime.of(hour, 0);
        LocalDate day = now.toLocalTime().isBefore(reset)
                ? now.toLocalDate().minusDays(1)
                : now.toLocalDate();
        return day + "@" + hour;
    }
}

package infinode.org.example.playertimecounter.util;

import infinode.org.example.playertimecounter.data.PlayerDayRecord;

import java.util.List;

/**
 * Entscheidet, ob bei der aktuellen Restzeit genau eine Warnung raus soll.
 * <p>
 * Alle bereits erreichten Schwellen werden markiert, damit ein späterer Join
 * nicht mehrere alte Warnungen auf einmal auslöst.
 */
public final class Warnings {

    private Warnings() {
    }

    /**
     * @param record                  Tagesdaten des Spielers
     * @param thresholdsDescendingMs  Schwellen in Millisekunden, absteigend
     * @param remainingMs             aktuelle Restzeit
     * @return {@code true}, wenn genau eine Warnung gesendet werden soll
     */
    public static boolean markAndShouldNotify(
            PlayerDayRecord record,
            List<Long> thresholdsDescendingMs,
            long remainingMs
    ) {
        boolean crossedNewThreshold = false;
        for (long threshold : thresholdsDescendingMs) {
            if (remainingMs <= threshold && !record.hasWarningFired(threshold)) {
                crossedNewThreshold = true;
                break;
            }
        }
        if (!crossedNewThreshold) {
            return false;
        }
        for (long threshold : thresholdsDescendingMs) {
            if (remainingMs <= threshold) {
                record.markWarningFired(threshold);
            }
        }
        return true;
    }
}

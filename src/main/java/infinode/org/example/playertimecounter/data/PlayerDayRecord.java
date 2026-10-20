package infinode.org.example.playertimecounter.data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Speichert die Tagesdaten eines Spielers: verbrauchte Zeit,
 * Tages-Schlüssel und bereits ausgelöste Warnschwellen.
 */
public final class PlayerDayRecord {

    private final UUID uuid;
    private String dayKey;
    private long usedMs;
    private final Set<Long> firedWarningsMs = new HashSet<>();

    public PlayerDayRecord(UUID uuid, String dayKey, long usedMs) {
        this.uuid = uuid;
        this.dayKey = dayKey;
        this.usedMs = Math.max(0L, usedMs);
    }

    public UUID getUuid() {
        return uuid;
    }

    /** Identifiziert den aktuellen „Plugin-Tag“ (Datum + Reset-Stunde). */
    public String getDayKey() {
        return dayKey;
    }

    public void setDayKey(String dayKey) {
        this.dayKey = dayKey;
    }

    /** Bisher verbrauchte Spielzeit in Millisekunden. */
    public long getUsedMs() {
        return usedMs;
    }

    public void setUsedMs(long usedMs) {
        this.usedMs = Math.max(0L, usedMs);
    }

    public void addUsedMs(long deltaMs) {
        if (deltaMs > 0) {
            this.usedMs += deltaMs;
        }
    }

    /** Markiert eine Warnschwelle als bereits ausgelöst. */
    public void markWarningFired(long thresholdMs) {
        firedWarningsMs.add(thresholdMs);
    }

    public boolean hasWarningFired(long thresholdMs) {
        return firedWarningsMs.contains(thresholdMs);
    }

    public void clearWarnings() {
        firedWarningsMs.clear();
    }

    public Set<Long> getFiredWarningsMs() {
        return Set.copyOf(firedWarningsMs);
    }

    public void setFiredWarningsMs(Set<Long> thresholds) {
        firedWarningsMs.clear();
        if (thresholds != null) {
            firedWarningsMs.addAll(thresholds);
        }
    }
}

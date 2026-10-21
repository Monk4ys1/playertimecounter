package infinode.org.example.playertimecounter.util;

import infinode.org.example.playertimecounter.data.PlayerDayRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaytimeRulesTest {

    @Test
    void formatsDurations() {
        assertEquals("0s", TimeFormatter.format(0));
        assertEquals("0s", TimeFormatter.format(-50));
        assertEquals("5s", TimeFormatter.format(5_000));
        assertEquals("1m 05s", TimeFormatter.format(65_000));
        assertEquals("1h 01m 01s", TimeFormatter.format(3_661_000));
    }

    @Test
    void formatsCompactTabLabels() {
        assertEquals("<1m", TimeFormatter.formatCompact(0));
        assertEquals("<1m", TimeFormatter.formatCompact(59_000));
        assertEquals("1m", TimeFormatter.formatCompact(60_000));
        assertEquals("1h 05m", TimeFormatter.formatCompact(3_900_000));
    }

    @Test
    void formatsResetHour() {
        assertEquals("04:00", TimeFormatter.formatResetHour(4));
        assertEquals("00:00", TimeFormatter.formatResetHour(0));
        assertEquals("23:00", TimeFormatter.formatResetHour(-1));
        assertEquals("01:00", TimeFormatter.formatResetHour(25));
    }

    @Test
    void dayKeyRespectsResetHour() {
        LocalDateTime morning = LocalDateTime.of(2026, 10, 21, 3, 15);
        LocalDateTime afterReset = LocalDateTime.of(2026, 10, 21, 4, 0);

        assertEquals("2026-10-21@0", DayCycle.dayKey(morning, 0));
        assertEquals("2026-10-20@4", DayCycle.dayKey(morning, 4));
        assertEquals("2026-10-21@4", DayCycle.dayKey(afterReset, 4));
    }

    @Test
    void warningFiresOnceForTheTightestReachedThreshold() {
        PlayerDayRecord record = new PlayerDayRecord(UUID.randomUUID(), "day", 0);
        List<Long> thresholds = List.of(30L, 10L, 5L, 1L);

        assertTrue(Warnings.markAndShouldNotify(record, thresholds, 4));
        assertTrue(record.hasWarningFired(30));
        assertTrue(record.hasWarningFired(10));
        assertTrue(record.hasWarningFired(5));
        assertFalse(record.hasWarningFired(1));

        assertFalse(Warnings.markAndShouldNotify(record, thresholds, 4));
        assertTrue(Warnings.markAndShouldNotify(record, thresholds, 1));
        assertFalse(Warnings.markAndShouldNotify(record, thresholds, 1));
    }
}

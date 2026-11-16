package de.moinmorni.wolkenkampf.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TimelineTest {

    private final Timeline t = new Timeline(150, 270, 390, 180);

    @Test
    void eventsComeInOrderAndAreAnnouncedBefore() {
        assertEquals(Timeline.Event.REFILL, t.next(0));
        assertEquals(150, t.untilNext(0));
        assertEquals(Timeline.Event.STORM, t.next(150));
        assertEquals(Timeline.Event.FOG, t.next(300));
        assertNull(t.next(390));
        assertEquals(-1, t.untilNext(400));
        assertEquals(Timeline.Event.REFILL, t.startsAt(150));
        assertNull(t.startsAt(151));
        assertEquals(Timeline.Event.STORM, t.warnAt(270 - Timeline.WARN));
        assertNull(t.warnAt(10));
    }

    @Test
    void stormGetsFasterButNeverHectic() {
        assertTrue(Double.isInfinite(t.strikeEvery(100)));
        assertEquals(6.0, t.strikeEvery(270), 1e-9);
        assertTrue(t.strikeEvery(330) < 6.0);
        assertEquals(3.0, t.strikeEvery(2000), 1e-9);
    }

    @Test
    void fogRisesFromBelowToAboveEverything() {
        assertEquals(-30, t.fogLevel(100, -30, 20), 1e-9);
        assertEquals(-30, t.fogLevel(390, -30, 20), 1e-9);
        assertEquals(-5, t.fogLevel(390 + 90, -30, 20), 1e-9);
        assertEquals(20, t.fogLevel(390 + 180, -30, 20), 1e-9);
        assertEquals(20, t.fogLevel(5000, -30, 20), 1e-9);
    }

    @Test
    void fogHurtsMoreTheLongerItLasts() {
        assertEquals(0, t.fogDamage(389), 1e-9);
        assertEquals(1, t.fogDamage(390), 1e-9);
        assertEquals(1, t.fogDamage(569), 1e-9);
        assertEquals(2, t.fogDamage(570), 1e-9);
        assertEquals(3, t.fogDamage(570 + Timeline.THICKER_EVERY), 1e-9);
        assertEquals(Timeline.MAX_FOG_DAMAGE, t.fogDamage(100_000), 1e-9);
    }

    @Test
    void wrongOrderIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Timeline(300, 200, 400, 100));
        assertThrows(IllegalArgumentException.class, () -> new Timeline(100, 200, 150, 100));
        assertThrows(IllegalArgumentException.class, () -> new Timeline(100, 200, 300, 5));
    }
}

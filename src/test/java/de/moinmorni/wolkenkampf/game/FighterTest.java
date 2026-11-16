package de.moinmorni.wolkenkampf.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class FighterTest {

    private static final UUID ANNA = new UUID(0, 1);
    private static final UUID BERT = new UUID(0, 2);
    /** Gondelboden (Block-Unterkante); man steht in der Gondel auf FLOOR + 1, die Insel liegt zehn Blöcke tiefer. */
    private static final double FLOOR = 110;

    @Test
    void landingProtectionLastsUntilGroundBelowTheGondolaFloor() {
        Fighter f = new Fighter(ANNA, "Anna", 0);
        f.startLanding(FLOOR);
        // Gondel gerade offen: der Client meldet noch „am Boden“ (er stand ja auf dem Gondelboden) – keine Landung.
        f.landingTick(true, FLOOR + 1);
        assertTrue(f.landing(), "Schutz bleibt, solange man noch in der Gondel steht");
        // Nach einem Teleport meldet der Client einmal „nicht am Boden“ und gleich wieder „am Boden“ – noch oben.
        f.landingTick(false, FLOOR + 1);
        f.landingTick(true, FLOOR + 1);
        assertTrue(f.landing(), "Wackeln auf Gondelhöhe beendet den Schutz nicht");
        // Wer auf einer Kante oder dem Rand der Gondel stehen bleibt und mit dem Ballon steigt, bleibt geschützt.
        f.landingTick(true, FLOOR + 6);
        assertTrue(f.landing(), "oben auf der steigenden Gondel geschützt");
        f.landingTick(false, FLOOR - 4);
        assertTrue(f.landing(), "im Fall geschützt");
        f.landingTick(true, FLOOR - 9);
        assertFalse(f.landing(), "auf der Insel gelandet – ab jetzt gilt Fallschaden wieder");
        // Wieder hinaufgeklettert: der Schutz kommt nicht zurück.
        f.landingTick(true, FLOOR + 1);
        assertFalse(f.landing());
        // Neue Runde: Schutz wieder an.
        f.startLanding(FLOOR);
        f.landingTick(true, FLOOR + 1);
        assertTrue(f.landing());
    }

    @Test
    void creditForPushingSomeoneDownCountsInGameTicks() {
        Fighter f = new Fighter(ANNA, "Anna", 0);
        assertNull(f.creditTo(0));
        f.hitBy(BERT, 1_000);
        assertEquals(BERT, f.creditTo(1_000 + Fighter.CREDIT_TICKS), "genau 15 s (300 Ticks) später zählt es noch");
        assertNull(f.creditTo(1_001 + Fighter.CREDIT_TICKS), "danach nicht mehr");
    }
}

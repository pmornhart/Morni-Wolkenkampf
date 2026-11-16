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

    @Test
    void landingProtectionLastsUntilTheFirstRealTouchdown() {
        Fighter f = new Fighter(ANNA, "Anna", 0);
        f.landing(true);
        // Gondel gerade offen: der Client meldet noch „am Boden“ (stand auf dem Gondelboden) – das ist keine Landung.
        f.landingTick(true);
        f.landingTick(true);
        assertTrue(f.landing(), "Schutz bleibt, solange man noch gar nicht gefallen ist");
        f.landingTick(false);
        f.landingTick(false);
        assertTrue(f.landing(), "im Fall geschützt");
        f.landingTick(true);
        assertFalse(f.landing(), "auf der Insel gelandet – ab jetzt gilt Fallschaden wieder");
        // Neue Runde: Schutz wieder an, „in der Luft gewesen“ zählt von vorn.
        f.landing(true);
        f.landingTick(true);
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

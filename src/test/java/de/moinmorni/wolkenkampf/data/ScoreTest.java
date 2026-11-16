package de.moinmorni.wolkenkampf.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ScoreTest {

    @Test
    void playerDataCountsAndRemembersTheKit() {
        PlayerData d = new PlayerData(new UUID(0, 1), "Anna");
        assertTrue(d.addGame(true, 3, false, 4), "erste Runde mit Kills = Bestleistung");
        assertFalse(d.addGame(false, 1, true, 2));
        d.kit("baumeister");
        PlayerData back = PlayerData.fromYaml(d.uuid(), d.toYaml());
        assertEquals(2, back.games());
        assertEquals(1, back.wins());
        assertEquals(4, back.kills());
        assertEquals(1, back.deaths());
        assertEquals(6, back.chests());
        assertEquals(3, back.bestKills());
        assertEquals("baumeister", back.kit());
        assertEquals("Anna", back.name());
    }

    @Test
    void leaderboardSortsByWinsThenKills() {
        Leaderboard b = new Leaderboard();
        UUID a = new UUID(0, 1);
        UUID c = new UUID(0, 2);
        UUID e = new UUID(0, 3);
        b.update(a, "Anna", 2, 5, 4);
        b.update(c, "Carl", 2, 9, 6);
        b.update(e, "Emil", 3, 1, 9);
        List<Leaderboard.Entry> top = b.top(10);
        assertEquals(List.of("Emil", "Carl", "Anna"), top.stream().map(Leaderboard.Entry::name).toList());
        assertEquals(1, b.rank(e));
        assertEquals(0, b.rank(new UUID(9, 9)));
        Leaderboard back = Leaderboard.fromYaml(b.toYaml());
        assertEquals(top, back.top(10));
        b.update(a, "Anna", 5, 5, 5);
        assertEquals("Anna", b.top(1).get(0).name(), "neue Werte ersetzen die alten");
    }

    @Test
    void leaderboardKeepsOnlyTheBest() {
        Leaderboard b = new Leaderboard();
        for (int i = 0; i < Leaderboard.KEEP + 10; i++) {
            b.update(new UUID(0, i), "P" + i, i, 0, 1);
        }
        assertEquals(Leaderboard.KEEP, b.top(1000).size());
        assertEquals("P" + (Leaderboard.KEEP + 9), b.top(1).get(0).name());
    }
}

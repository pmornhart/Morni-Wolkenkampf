package de.moinmorni.wolkenkampf.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TeamsTest {

    private static List<UUID> people(int n) {
        List<UUID> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(new UUID(0, i + 1));
        }
        return out;
    }

    private static List<List<UUID>> singles(List<UUID> ps) {
        return ps.stream().map(List::of).toList();
    }

    @Test
    void soloMakesOneTeamPerPerson() {
        List<UUID> ps = people(5);
        List<List<UUID>> teams = Teams.form(Mode.SOLO, List.of(List.of(ps.get(0), ps.get(1)), List.of(ps.get(2)),
                List.of(ps.get(3), ps.get(4))));
        assertEquals(5, teams.size());
        teams.forEach(t -> assertEquals(1, t.size()));
    }

    @Test
    void duoKeepsAPartyOfTwoTogether() {
        List<UUID> ps = people(4);
        // Anna kam zuerst, dann Bert, dann Annas Party-Freund Carl, dann Dora.
        List<List<UUID>> groups = List.of(List.of(ps.get(0), ps.get(2)), List.of(ps.get(1)), List.of(ps.get(3)));
        List<List<UUID>> teams = Teams.form(Mode.DUO, groups);
        assertEquals(2, teams.size());
        assertEquals(List.of(ps.get(0), ps.get(2)), teams.get(0));
        assertEquals(List.of(ps.get(1), ps.get(3)), teams.get(1));
    }

    @Test
    void duoSplitsBigPartiesAndLeavesOneAloneWhenOdd() {
        List<UUID> ps = people(5);
        List<List<UUID>> teams = Teams.form(Mode.DUO, List.of(List.of(ps.get(0), ps.get(1), ps.get(2)),
                List.of(ps.get(3)), List.of(ps.get(4))));
        assertEquals(3, teams.size());
        assertEquals(List.of(ps.get(0), ps.get(1)), teams.get(0));
        assertEquals(List.of(ps.get(2), ps.get(3)), teams.get(1));
        assertEquals(List.of(ps.get(4)), teams.get(2));
        Set<UUID> all = new HashSet<>();
        teams.forEach(all::addAll);
        assertEquals(5, all.size(), "niemand doppelt, niemand fehlt");
    }

    @Test
    void twelvePeopleMakeSixDuos() {
        List<List<UUID>> teams = Teams.form(Mode.DUO, singles(people(12)));
        assertEquals(6, teams.size());
        teams.forEach(t -> assertEquals(2, t.size()));
    }

    @Test
    void duosNeedAtLeastThreePeople() {
        assertEquals(Mode.SOLO, Mode.effective(Mode.DUO, 2));
        assertEquals(Mode.DUO, Mode.effective(Mode.DUO, 3));
        assertEquals(Mode.SOLO, Mode.effective(Mode.SOLO, 12));
        assertEquals(Mode.DUO, Mode.byId("zweier"));
        assertEquals(Mode.DUO, Mode.byId("Duo"));
        assertEquals(Mode.SOLO, Mode.byId("allein"));
        assertNull(Mode.byId("dreier"));
    }

    @Test
    void spawnsAreSpreadEvenlyAndNeverTwice() {
        for (int teams = 1; teams <= 12; teams++) {
            for (int offset = 0; offset < 12; offset++) {
                List<Integer> picked = Spawns.pick(teams, 12, offset);
                assertEquals(teams, new HashSet<>(picked).size(), teams + " Teams, Drehung " + offset);
                picked.forEach(i -> assertTrue(i >= 0 && i < 12));
                List<Integer> sorted = new ArrayList<>(picked);
                sorted.sort(null);
                int minGap = 12;
                for (int i = 0; i < sorted.size(); i++) {
                    int next = i + 1 < sorted.size() ? sorted.get(i + 1) : sorted.get(0) + 12;
                    minGap = Math.min(minGap, next - sorted.get(i));
                }
                assertTrue(minGap >= 12 / teams, "Abstand " + minGap + " bei " + teams + " Teams");
            }
        }
        assertEquals(List.of(0, 6), Spawns.pick(2, 12, 0));
        assertEquals(List.of(3, 7, 11), Spawns.pick(3, 12, 3));
    }

    @Test
    void groupsStayTogetherAndRespectTheLimit() {
        List<UUID> ps = people(14);
        List<List<UUID>> groups = new ArrayList<>();
        groups.add(List.of(ps.get(0), ps.get(1), ps.get(2)));
        for (int i = 3; i < 14; i++) {
            groups.add(List.of(ps.get(i)));
        }
        List<UUID> picked = Groups.pick(groups, 12);
        assertEquals(12, picked.size());
        assertTrue(picked.containsAll(List.of(ps.get(0), ps.get(1), ps.get(2))));
    }

    @Test
    void votesPickTheMostVotedFreeChoice() {
        Votes<Mode> v = new Votes<>(Mode.class);
        UUID a = new UUID(1, 1);
        UUID b = new UUID(1, 2);
        UUID c = new UUID(1, 3);
        v.vote(a, Mode.DUO);
        v.vote(b, Mode.DUO);
        v.vote(c, Mode.SOLO);
        assertEquals(Mode.DUO, v.winner(Set.of(Mode.SOLO, Mode.DUO)));
        assertEquals(Mode.SOLO, v.winner(Set.of(Mode.SOLO)), "nur freie zählen");
        v.retain(List.of(c));
        assertEquals(0, v.count(Mode.DUO));
        assertNull(v.winner(Set.of()));
    }
}

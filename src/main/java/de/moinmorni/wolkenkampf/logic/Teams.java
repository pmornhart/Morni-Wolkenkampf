package de.moinmorni.wolkenkampf.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Teams für eine Runde bilden. Allein: jede Person ist ein Team. Zu zweit: eine Party bleibt zusammen (eine Party aus
 * drei oder mehr wird in Zweier-Paare geteilt), die übrigen werden in Ankunfts-Reihenfolge zu Paaren; bleibt jemand
 * übrig, kämpft er allein.
 */
public final class Teams {

    private Teams() {
    }

    /**
     * @param groups Partys bzw. Einzelne in der Reihenfolge, in der sie im Wolkenhafen ankamen
     * @return Teams (jedes nicht leer)
     */
    public static List<List<UUID>> form(Mode mode, List<List<UUID>> groups) {
        List<List<UUID>> teams = new ArrayList<>();
        if (mode == Mode.SOLO) {
            for (List<UUID> g : groups) {
                for (UUID u : g) {
                    teams.add(List.of(u));
                }
            }
            return teams;
        }
        List<UUID> singles = new ArrayList<>();
        for (List<UUID> g : groups) {
            int i = 0;
            for (; i + 1 < g.size(); i += 2) {
                teams.add(List.of(g.get(i), g.get(i + 1)));
            }
            if (i < g.size()) {
                singles.add(g.get(i));
            }
        }
        for (int i = 0; i < singles.size(); i += 2) {
            teams.add(i + 1 < singles.size() ? List.of(singles.get(i), singles.get(i + 1)) : List.of(singles.get(i)));
        }
        return teams;
    }
}

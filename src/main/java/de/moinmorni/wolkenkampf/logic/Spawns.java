package de.moinmorni.wolkenkampf.logic;

import java.util.ArrayList;
import java.util.List;

/** Welche Startinseln bekommen die Teams? So gleichmäßig wie möglich im Ring verteilt, damit niemand im Vorteil ist. */
public final class Spawns {

    private Spawns() {
    }

    /**
     * @param teams   Zahl der Teams (1 … islands)
     * @param islands Zahl der Startinseln im Ring
     * @param offset  Drehung (z. B. zufällig), damit nicht immer dieselben Inseln drankommen
     * @return Insel-Nummern (0 … islands−1), eine je Team, alle verschieden
     */
    public static List<Integer> pick(int teams, int islands, int offset) {
        if (teams < 1 || teams > islands) {
            throw new IllegalArgumentException("teams=" + teams + " islands=" + islands);
        }
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < teams; i++) {
            int idx = (int) Math.floor(i * (double) islands / teams);
            out.add(Math.floorMod(idx + offset, islands));
        }
        return out;
    }
}

package de.moinmorni.wolkenkampf.logic;

import java.util.Locale;

/** Spielart: jeder für sich oder in Zweier-Teams (eine Insel, eine Gondel pro Team). */
public enum Mode {
    SOLO("allein", 1),
    DUO("zweier", 2);

    private final String id;
    private final int teamSize;

    Mode(String id, int teamSize) {
        this.id = id;
        this.teamSize = teamSize;
    }

    public String id() {
        return id;
    }

    public int teamSize() {
        return teamSize;
    }

    /** Aus einem Befehl: „allein“/„solo“ oder „zweier“/„duo“/„teams“; sonst null. */
    public static Mode byId(String s) {
        if (s == null) {
            return null;
        }
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "allein", "solo", "einzeln", "single" -> SOLO;
            case "zweier", "duo", "duos", "team", "teams", "zu-zweit" -> DUO;
            default -> null;
        };
    }

    /**
     * Die Spielart, die wirklich gespielt wird: Zweier-Teams brauchen mindestens drei Leute – mit zweien gäbe es nur ein
     * einziges Team und niemanden zum Kämpfen.
     */
    public static Mode effective(Mode wanted, int players) {
        return wanted == DUO && players >= 3 ? DUO : SOLO;
    }
}

package de.moinmorni.wolkenkampf.logic;

/**
 * Ablauf einer Runde in Sekunden nach dem Öffnen der Gondeln: erst „Nachschub“ (alle Truhen füllen sich neu), dann
 * „Gewitter“ (Blitze an zufälligen Stellen, jeder mit Vorwarnung), am Ende steigt „Nebel“ von unten auf und schadet allen,
 * die darin stehen – so hat jede Runde ein Ende.
 *
 * @param refill      Nachschub
 * @param storm       Beginn des Gewitters
 * @param fog         Beginn des Nebels
 * @param fogDuration so lange steigt der Nebel von ganz unten bis über die höchste Insel
 */
public record Timeline(int refill, int storm, int fog, int fogDuration) {

    /** So lange vorher wird ein Ereignis angekündigt. */
    public static final int WARN = 15;
    /** Ist der Nebel ganz oben, wird er so oft (Sekunden) dichter: +1 Schaden pro Sekunde. */
    public static final int THICKER_EVERY = 30;
    /** Höchster Schaden pro Sekunde im Nebel. */
    public static final double MAX_FOG_DAMAGE = 6;

    public enum Event {
        REFILL, STORM, FOG
    }

    public Timeline {
        if (refill < 1 || storm <= refill || fog <= storm || fogDuration < 10) {
            throw new IllegalArgumentException("Reihenfolge: Nachschub < Gewitter < Nebel, Nebel-Dauer ≥ 10");
        }
    }

    public int at(Event e) {
        return switch (e) {
            case REFILL -> refill;
            case STORM -> storm;
            case FOG -> fog;
        };
    }

    /** Das nächste Ereignis nach Sekunde {@code t} (null, wenn alle vorbei sind). */
    public Event next(int t) {
        for (Event e : Event.values()) {
            if (t < at(e)) {
                return e;
            }
        }
        return null;
    }

    /** Sekunden bis zum nächsten Ereignis (−1, wenn keins mehr kommt). */
    public int untilNext(int t) {
        Event e = next(t);
        return e == null ? -1 : at(e) - t;
    }

    /** Ereignis, das genau in Sekunde {@code t} beginnt (sonst null). */
    public Event startsAt(int t) {
        for (Event e : Event.values()) {
            if (at(e) == t) {
                return e;
            }
        }
        return null;
    }

    /** Ereignis, das in Sekunde {@code t} angekündigt wird ({@link #WARN} Sekunden vorher; sonst null). */
    public Event warnAt(int t) {
        for (Event e : Event.values()) {
            if (at(e) - WARN == t) {
                return e;
            }
        }
        return null;
    }

    public boolean storming(int t) {
        return t >= storm;
    }

    public boolean foggy(int t) {
        return t >= fog;
    }

    /** Sekunden zwischen zwei Blitzen: anfangs 6, nach und nach bis 3. */
    public double strikeEvery(int t) {
        if (t < storm) {
            return Double.POSITIVE_INFINITY;
        }
        return Math.max(3.0, 6.0 - (t - storm) / 40.0);
    }

    /** Wie weit der Nebel gestiegen ist: 0 = ganz unten, 1 = über allem. */
    public double fogRise(int t) {
        if (t < fog) {
            return 0;
        }
        return Math.min(1.0, (t - fog) / (double) fogDuration);
    }

    /** Höhe der Nebelgrenze zwischen {@code bottom} und {@code top}. */
    public double fogLevel(int t, double bottom, double top) {
        return bottom + (top - bottom) * fogRise(t);
    }

    /** Schaden pro Sekunde im Nebel (halbe Herzen): 1, ganz oben 2, danach alle 30 s eins mehr (höchstens 6). */
    public double fogDamage(int t) {
        if (t < fog) {
            return 0;
        }
        int full = fog + fogDuration;
        if (t < full) {
            return 1;
        }
        return Math.min(MAX_FOG_DAMAGE, 2 + (t - full) / THICKER_EVERY);
    }
}

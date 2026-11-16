package de.moinmorni.wolkenkampf.logic;

import java.util.ArrayList;
import java.util.List;

/**
 * Aufbau einer Karte ohne Minecraft: wo die Startinseln, die Mittelinsel und die kleinen Inseln schweben, wo Truhen und
 * Gondeln stehen und wo Nebel, Rand und Bauhöhe liegen. Alles relativ zum Mittelpunkt der Karte; y = 0 ist die
 * Bodenhöhe der Inseln (man steht auf y = top + 1).
 *
 * @param starts   Startinseln im Ring (je Team eine)
 * @param middle   die große Mittelinsel mit den besseren Truhen
 * @param small    kleine Inseln dazwischen
 * @param voidY    wer tiefer fällt, scheidet aus
 * @param fogBottom Nebel beginnt hier …
 * @param fogTop    … und steigt bis hierher (über die höchste Insel)
 * @param border   waagrechter Abstand zur Mitte, ab dem der Sturm schadet und nicht mehr gebaut werden kann
 * @param buildTop höher kann man nicht bauen
 */
public record MapLayout(List<Island> starts, Island middle, List<Island> small, int voidY, int fogBottom, int fogTop,
        int border, int buildTop) {

    /** Gondelboden über dem Inselboden. */
    public static final int GONDOLA_HEIGHT = 10;
    /** So tief reicht eine Insel höchstens unter ihren Boden. */
    public static final int MAX_DEPTH = 18;

    /** Eine Truhe: Block-Stelle (auf dem Boden, y = top + 1) und Blickrichtung („north“ …). */
    public record Spot(int x, int y, int z, String facing) {
    }

    /**
     * Eine Insel.
     *
     * @param top    oberste Bodenschicht
     * @param radius ungefährer Radius der Bodenfläche
     * @param depth  wie weit sie nach unten reicht
     */
    public record Island(int x, int z, int top, int radius, int depth, List<Spot> chests) {

        /** Abstand der Mittelpunkte (waagrecht). */
        public double distance(Island o) {
            return Math.hypot(x - o.x, z - o.z);
        }

        /** Lücke von Rand zu Rand (waagrecht). */
        public double gap(Island o) {
            return distance(o) - radius - o.radius;
        }

        /** Stehplatz in der Gondel: Boden der Gondel liegt {@link #GONDOLA_HEIGHT} über dem Inselboden. */
        public int gondolaY() {
            return top + GONDOLA_HEIGHT;
        }
    }

    /**
     * Ring-Karte.
     *
     * @param count       Startinseln
     * @param ring        Abstand der Startinseln zur Mitte
     * @param rotation    Drehung des Rings (Grad)
     * @param startRadius Radius der Startinseln
     * @param heights     Bodenhöhen der Startinseln (wiederholt sich)
     * @param middleRadius Radius der Mittelinsel
     * @param middleTop   Bodenhöhe der Mittelinsel
     * @param middleChests Truhen der Mittelinsel (relativ zu ihrer Mitte, y relativ zum Boden + 1)
     * @param smallCount  kleine Inseln (zwischen zwei Startinseln, gleichmäßig verteilt)
     * @param smallRing   Abstand der kleinen Inseln zur Mitte
     * @param smallRadius ihr Radius
     * @param smallChests Truhen je kleiner Insel (1 oder 2)
     */
    public static MapLayout ring(int count, double ring, double rotation, int startRadius, int[] heights,
            int middleRadius, int middleTop, List<Spot> middleChests, int smallCount, double smallRing, int smallRadius,
            int smallChests) {
        List<Island> starts = new ArrayList<>();
        double rot = Math.toRadians(rotation);
        for (int i = 0; i < count; i++) {
            double a = rot + 2 * Math.PI * i / count;
            int x = (int) Math.round(Math.sin(a) * ring);
            int z = (int) Math.round(-Math.cos(a) * ring);
            int top = heights[i % heights.length];
            starts.add(new Island(x, z, top, startRadius, startRadius + 9, startChests(x, z, top)));
        }
        List<Spot> mc = new ArrayList<>();
        for (Spot s : middleChests) {
            mc.add(new Spot(s.x(), middleTop + s.y(), s.z(), s.facing()));
        }
        Island middle = new Island(0, 0, middleTop, middleRadius, MAX_DEPTH, List.copyOf(mc));
        List<Island> small = new ArrayList<>();
        for (int i = 0; i < smallCount; i++) {
            // Zwischen zwei Startinseln: halber Winkelschritt dazwischen.
            double step = 2 * Math.PI / count;
            double a = rot + step * (i * count / (double) smallCount) + step / 2;
            int x = (int) Math.round(Math.sin(a) * smallRing);
            int z = (int) Math.round(-Math.cos(a) * smallRing);
            int top = 1;
            List<Spot> chests = new ArrayList<>();
            if (smallChests > 1) {
                // Quer zur Richtung Mitte, je zwei Blöcke links und rechts.
                int px = (int) Math.round(Math.cos(a) * 2);
                int pz = (int) Math.round(Math.sin(a) * 2);
                chests.add(new Spot(x + px, top + 1, z + pz, facingTo(0, 0, x + px, z + pz)));
                chests.add(new Spot(x - px, top + 1, z - pz, facingTo(0, 0, x - px, z - pz)));
            } else {
                chests.add(new Spot(x, top + 1, z, facingTo(0, 0, x, z)));
            }
            small.add(new Island(x, z, top, smallRadius, smallRadius + 6, List.copyOf(chests)));
        }
        int highest = Math.max(middleTop, starts.stream().mapToInt(Island::top).max().orElse(0));
        return new MapLayout(List.copyOf(starts), middle, List.copyOf(small), -42, -MAX_DEPTH - 8, highest + 14,
                (int) Math.round(ring + startRadius + 16), 42);
    }

    /**
     * Zwei Truhen je Startinsel: hinten (von der Mitte der Karte weg) links und rechts, mit dem Deckel zur Inselmitte.
     * Die Mitte (Landeplatz unter der Gondel) bleibt frei.
     */
    static List<Spot> startChests(int x, int z, int top) {
        double len = Math.hypot(x, z);
        double ox = len == 0 ? 0 : x / len;
        double oz = len == 0 ? 1 : z / len;
        double px = -oz;
        double pz = ox;
        List<Spot> out = new ArrayList<>();
        for (int side : new int[] {-1, 1}) {
            int cx = x + (int) Math.round(ox * 2 + px * side * 3);
            int cz = z + (int) Math.round(oz * 2 + pz * side * 3);
            out.add(new Spot(cx, top + 1, cz, facingTo(x, z, cx, cz)));
        }
        return List.copyOf(out);
    }

    /** Himmelsrichtung, in die eine Truhe bei (fx|fz) schaut, wenn sie zu (tx|tz) zeigen soll. */
    static String facingTo(int tx, int tz, int fx, int fz) {
        int dx = tx - fx;
        int dz = tz - fz;
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx >= 0 ? "east" : "west";
        }
        return dz >= 0 ? "south" : "north";
    }

    /** Alle Inseln (Start, Mitte, klein). */
    public List<Island> all() {
        List<Island> out = new ArrayList<>(starts);
        out.add(middle);
        out.addAll(small);
        return out;
    }

    /** Liegt (x|z) außerhalb des Randes? */
    public boolean outside(double x, double z) {
        return Math.hypot(x, z) > border;
    }
}

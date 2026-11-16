package de.moinmorni.wolkenkampf.world;

import de.moinmorni.wolkenkampf.logic.IslandColour;
import java.util.ArrayList;
import java.util.List;

/**
 * Ballon-Gondel über einer Startinsel: ein Korb (Boden aus Falltüren, Wände aus Zäunen), darüber die Hülle in Bahnen in
 * der Farbe der Insel mit Bauchbinde, dazwischen Seile und ein Brenner. Beim Start klappen die Falltüren auf – man
 * schwebt hinunter auf die Insel –, danach steigt der Ballon davon.
 *
 * <p>Alle Stellen relativ zur Mitte des Korbbodens (y = 0 ist der Boden aus Falltüren, man steht auf y = 1 … also auf
 * der oberen Hälfte der Falltür).
 */
public final class Gondola {

    /** Halbe Breite des Korbs (5 × 5 mit Rand). */
    public static final int HALF = 2;
    /** Hülle beginnt so weit über dem Boden. */
    public static final int ENVELOPE = 5;
    /** Radius der Öffnung unten an der Hülle. */
    private static final double MOUTH = 1.7;
    /** Bahnen rundherum. */
    private static final int GORES = 8;
    /** Größter Radius der Hülle. */
    private static final double RADIUS = 4.2;

    private Gondola() {
    }

    /** Stellen der Bodenfalltüren (3 × 3). */
    public static List<int[]> floor() {
        List<int[]> out = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                out.add(new int[] {dx, 0, dz});
            }
        }
        return out;
    }

    public static String trapdoor(boolean open) {
        return "spruce_trapdoor[facing=north,half=top,open=" + open + "]";
    }

    /** Die ganze Gondel als eigener Plan (Mitte des Korbbodens = 0|0|0). */
    public static Plan plan(IslandColour c) {
        Plan p = new Plan();
        // Korb: Boden aus Falltüren, Rand aus Brettern, Wände aus Zäunen, unten ein Kranz aus Stufen.
        for (int[] f : floor()) {
            p.set(f[0], 0, f[2], trapdoor(false));
        }
        for (int dx = -HALF; dx <= HALF; dx++) {
            for (int dz = -HALF; dz <= HALF; dz++) {
                boolean rim = Math.abs(dx) == HALF || Math.abs(dz) == HALF;
                if (!rim) {
                    continue;
                }
                p.set(dx, 0, dz, "stripped_spruce_wood[axis=y]");
                p.set(dx, 1, dz, "spruce_fence");
                boolean corner = Math.abs(dx) == HALF && Math.abs(dz) == HALF;
                if (!corner) {
                    p.set(dx, -1, dz, "spruce_slab[type=top]");
                }
            }
        }
        // Seile (Zäune) an den Ecken bis zur Hülle, oben ein Querbalken.
        for (int[] k : new int[][] {{-HALF, -HALF}, {HALF, -HALF}, {-HALF, HALF}, {HALF, HALF}}) {
            for (int y = 2; y <= ENVELOPE + 2; y++) {
                p.set(k[0], y, k[1], "spruce_fence");
            }
        }
        // Laternen an zwei Ecken, Brenner unter der Hülle (Lagerfeuer raucht in den Ballon).
        p.set(-HALF, 2, 0, "lantern[hanging=false]");
        p.set(HALF, 2, 0, "lantern[hanging=false]");
        p.set(0, ENVELOPE - 1, 0, "campfire[lit=true,signal_fire=false,facing=north]");
        p.set(0, ENVELOPE, 0, "iron_chain[axis=y]");
        p.set(0, ENVELOPE + 1, 0, "iron_chain[axis=y]");
        // Sandsäcke außen am Korb.
        p.set(0, 1, -HALF - 1, "brown_wool");
        p.set(0, 1, HALF + 1, "brown_wool");
        envelope(p, c);
        p.connect();
        return p;
    }

    /** Hülle: Tropfenform, nur die Haut, Bahnen abwechselnd Inselfarbe und hell, Bauchbinde und Kappe. */
    private static void envelope(Plan p, IslandColour c) {
        int widest = (int) Math.round(RADIUS * 1.15);
        int top = widest + (int) Math.round(RADIUS * 0.95);
        int half = (int) Math.ceil(RADIUS) + 1;
        String a = c.dye() + "_wool";
        String b = c.stripe() + "_wool";
        String band = c.band() + "_wool";
        for (int layer = 0; layer <= top; layer++) {
            for (int dx = -half; dx <= half; dx++) {
                for (int dz = -half; dz <= half; dz++) {
                    if (!inside(layer, dx, dz, widest, top)) {
                        continue;
                    }
                    boolean skin = !inside(layer + 1, dx, dz, widest, top)
                            || (layer > 0 && !inside(layer - 1, dx, dz, widest, top))
                            || !inside(layer, dx + 1, dz, widest, top) || !inside(layer, dx - 1, dz, widest, top)
                            || !inside(layer, dx, dz + 1, widest, top) || !inside(layer, dx, dz - 1, widest, top);
                    if (!skin || (layer == 0 && Math.hypot(dx, dz) < MOUTH - 0.6)) {
                        continue;
                    }
                    String block;
                    if (layer == Math.round(widest * 0.55) || layer >= top - 1) {
                        block = band;
                    } else {
                        int gore = (int) Math.floor(((Math.atan2(dz, dx) + Math.PI) / (Math.PI * 2)) * GORES + 0.5) % GORES;
                        block = gore % 2 == 0 ? a : b;
                    }
                    p.set(dx, ENVELOPE + 1 + layer, dz, block);
                }
            }
        }
    }

    private static double radius(int layer, int widest, int top) {
        if (layer < 0 || layer > top) {
            return 0;
        }
        if (layer <= widest) {
            return MOUTH + (RADIUS - MOUTH) * Math.sin((layer / (double) widest) * (Math.PI / 2));
        }
        return RADIUS * Math.cos(((layer - widest) / (top - widest + 0.6)) * (Math.PI / 2));
    }

    private static boolean inside(int layer, int dx, int dz, int widest, int top) {
        return Math.hypot(dx, dz) <= radius(layer, widest, top) + 0.3;
    }

    /** Höhe der Gondel über dem Boden (für die Aufräum-Box). */
    public static int height() {
        int widest = (int) Math.round(RADIUS * 1.15);
        return ENVELOPE + 2 + widest + (int) Math.round(RADIUS * 0.95);
    }
}

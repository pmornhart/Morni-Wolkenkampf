package de.moinmorni.wolkenkampf.world;

import de.moinmorni.wolkenkampf.logic.IslandColour;
import de.moinmorni.wolkenkampf.logic.MapLayout;
import java.util.Random;

/**
 * Karte „Kristallgipfel“: zwölf verschneite Felsinseln mit Eisnadeln, Amethyst und verschneiten Fichten; sechs kleine
 * Kristallfelsen dazwischen; in der Mitte ein Felsgipfel mit Gipfelkreuz, einer Kristallgrotte (zwei Truhen) und einer
 * roten Biwakschachtel. Unten hängen Amethyst und Eiszapfen. Darunter das Wolkenmeer, ringsum Gletschergipfel und
 * schwebende Kristalle. Eigener Entwurf.
 */
final class KristallgipfelPlan {

    private static final Decor.Palette FROST = new Decor.Palette("snow_block", "snow_block", 1, Decor.CRYSTAL_ROCK,
            Decor.CRYSTAL_HANGING);

    private KristallgipfelPlan() {
    }

    static Plan build(MapLayout layout) {
        Plan p = new Plan();
        Random r = new Random(1848);
        for (int i = 0; i < layout.starts().size(); i++) {
            MapLayout.Island is = layout.starts().get(i);
            Decor.island(p, is.x(), is.z(), is.top(), is.radius(), is.depth(), r, FROST);
            surface(p, is, r);
            startIsland(p, is, IslandColour.of(i), r);
        }
        MapLayout.Island mid = layout.middle();
        Decor.island(p, mid.x(), mid.z(), mid.top(), mid.radius(), mid.depth(), r, FROST);
        surface(p, mid, r);
        middle(p, mid, r);
        for (MapLayout.Island s : layout.small()) {
            Decor.island(p, s.x(), s.z(), s.top(), s.radius(), s.depth(), r, FROST);
            surface(p, s, r);
            crystalRock(p, s, r);
        }
        MapKit.chests(p, layout);
        MapKit.gondolas(p, layout);
        scenery(p, layout, r);
        p.connect();
        return p;
    }

    /** Oberfläche auflockern: etwas Kalzit, Packeis und Schneeschichten (nicht auf Landeplatz und Truhen). */
    private static void surface(Plan p, MapLayout.Island is, Random r) {
        for (int dx = -is.radius() - 1; dx <= is.radius() + 1; dx++) {
            for (int dz = -is.radius() - 1; dz <= is.radius() + 1; dz++) {
                int x = is.x() + dx;
                int z = is.z() + dz;
                var d = p.get(x, is.top(), z);
                if (d == null || d.getMaterial() != org.bukkit.Material.SNOW_BLOCK) {
                    continue;
                }
                int roll = r.nextInt(100);
                if (roll < 10) {
                    p.set(x, is.top(), z, "calcite");
                } else if (roll < 16) {
                    p.set(x, is.top(), z, "packed_ice");
                } else if (roll < 20 && !MapKit.reserved(is, x, z)) {
                    p.set(x, is.top() + 1, z, "snow[layers=1]");
                }
            }
        }
    }

    /** Startinsel: Eisnadel hinten, verschneite Fichte, Amethyst, Seelenlaterne, Fahne in der Inselfarbe vorne. */
    private static void startIsland(Plan p, MapLayout.Island is, IslandColour c, Random r) {
        int y = is.top() + 1;
        int[] spike = MapKit.local(is, 4, -1);
        iceSpike(p, spike[0], y, spike[1], 5 + r.nextInt(2));
        int[] tree = MapKit.local(is, 3, 3);
        if (!MapKit.reserved(is, tree[0], tree[1])) {
            Decor.spruce(p, tree[0], y, tree[1], 4, r, true);
        }
        int[] crystal = MapKit.local(is, 0, -4);
        p.set(crystal[0], y, crystal[1], "amethyst_block");
        p.set(crystal[0], y + 1, crystal[1], "amethyst_cluster[facing=up]");
        int[] bud = MapKit.local(is, 1, -4);
        if (p.get(bud[0], y, bud[1]) == null) {
            p.set(bud[0], y, bud[1], "medium_amethyst_bud[facing=up]");
        }
        int[] post = MapKit.local(is, -3, -3);
        Decor.lanternPost(p, post[0], y, post[1], "birch_fence", "soul_lantern");
        int[] flag = MapKit.local(is, -3, 3);
        Decor.flag(p, flag[0], y, flag[1], c, MapKit.rotationToMiddle(is));
    }

    /** Eisnadel aus Packeis mit blauer Spitze. */
    private static void iceSpike(Plan p, int x, int y, int z, int height) {
        for (int i = 0; i < height; i++) {
            p.set(x, y + i, z, i >= height - 2 ? "blue_ice" : "packed_ice");
        }
        for (int[] o : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            p.setIfEmpty(x + o[0], y, z + o[1], "packed_ice");
            if (height > 5) {
                p.setIfEmpty(x + o[0], y + 1, z + o[1], "packed_ice");
            }
        }
    }

    /** Amethyst-Spitze schräg aus dem Boden (Kristall). */
    private static void crystal(Plan p, int x, int y, int z, int height, int lean) {
        for (int i = 0; i < height; i++) {
            int ox = lean * i / 3;
            p.set(x + ox, y + i, z, i == 0 ? "budding_amethyst" : "amethyst_block");
        }
        p.set(x + lean * height / 3, y + height, z, "amethyst_cluster[facing=up]");
    }

    /** Kleiner Kristallfels: halbe Geode aus Kalzit und Amethyst um die Truhe. */
    private static void crystalRock(Plan p, MapLayout.Island s, Random r) {
        int y = s.top() + 1;
        crystal(p, s.x() - 3, y, s.z() + 1, 4, 1);
        crystal(p, s.x() + 2, y, s.z() - 2, 3, -1);
        p.set(s.x() + 2, y, s.z() + 2, "calcite");
        p.set(s.x() + 2, y + 1, s.z() + 2, "small_amethyst_bud[facing=up]");
        p.set(s.x() - 1, y, s.z() - 3, "soul_lantern[hanging=false]");
    }

    /**
     * Mittelinsel: Felsgipfel mit Schneekappe und Gipfelkreuz, eine Grotte voller Amethyst nach Süden (zwei Truhen),
     * Kristallspitzen, Eisnadeln und eine rote Biwakschachtel.
     */
    private static void middle(Plan p, MapLayout.Island m, Random r) {
        int t = m.top();
        int peakR = 5;
        int height = 16;
        for (int dx = -peakR; dx <= peakR; dx++) {
            for (int dz = -peakR; dz <= peakR; dz++) {
                double d = Math.hypot(dx, dz) / (peakR + 0.4);
                if (d > 1) {
                    continue;
                }
                int top = (int) Math.round(height * Math.pow(1 - d, 1.15) + Math.sin(dx * 1.3 + dz) * 0.8);
                for (int y = 1; y <= top; y++) {
                    String block;
                    if (y >= top - 1 && y > height * 0.45) {
                        block = "snow_block";
                    } else {
                        block = Decor.CRYSTAL_ROCK.at(r, 3);
                    }
                    p.set(dx, t + y, dz, block);
                }
            }
        }
        // Grotte nach Süden: Eingang bei z = 5, Raum x −2 … 2, z 1 … 4, innen Amethyst.
        for (int x = -2; x <= 2; x++) {
            for (int z = 1; z <= 6; z++) {
                for (int y = 1; y <= 3; y++) {
                    p.clear(x, t + y, z);
                }
                p.set(x, t, z, "calcite");
            }
            p.set(x, t + 4, 1, "amethyst_block");
            p.set(x, t + 4, 2, "amethyst_block");
            p.set(x, t + 4, 3, "amethyst_block");
        }
        for (int z = 1; z <= 4; z++) {
            p.set(-3, t + 2, z, "amethyst_block");
            p.set(3, t + 2, z, "amethyst_block");
            p.set(-3, t + 1, z, "calcite");
            p.set(3, t + 1, z, "calcite");
            p.set(-3, t + 3, z, "calcite");
            p.set(3, t + 3, z, "calcite");
        }
        for (int x = -2; x <= 2; x++) {
            p.set(x, t + 2, 0, "amethyst_block");
            p.set(x, t + 1, 0, "calcite");
            p.set(x, t + 3, 0, "calcite");
        }
        p.set(0, t + 3, 3, "amethyst_cluster[facing=down]");
        p.set(-1, t + 3, 2, "large_amethyst_bud[facing=down]");
        p.set(1, t + 3, 1, "amethyst_cluster[facing=down]");
        p.set(0, t + 1, 1, "amethyst_cluster[facing=up]");
        p.set(0, t + 3, 1, "soul_lantern[hanging=true]");
        // Gipfelkreuz aus dunklem Holz.
        int summit = t + height + 1;
        for (int i = 0; i < 5; i++) {
            p.set(0, summit + i, 0, "dark_oak_fence");
        }
        p.set(-1, summit + 3, 0, "dark_oak_fence");
        p.set(1, summit + 3, 0, "dark_oak_fence");
        p.set(0, summit + 5, 0, "lightning_rod[facing=up]");
        p.set(0, summit - 1, 0, "stone");
        // Kristallspitzen und Eisnadeln rund um den Gipfel.
        crystal(p, -7, t + 1, -2, 5, -1);
        crystal(p, 7, t + 1, -2, 5, 1);
        crystal(p, -4, t + 1, -9, 4, 0);
        iceSpike(p, 4, t + 1, -9, 6);
        iceSpike(p, -10, t + 1, 2, 4);
        // Biwakschachtel: rote Blechhütte mit Fenster, Laterne und Bank (Osten).
        int bx = 9;
        int bz = 1;
        for (int x = bx - 1; x <= bx + 1; x++) {
            for (int z = bz - 1; z <= bz + 2; z++) {
                boolean wall = x != bx || z == bz - 1 || z == bz + 2;
                for (int y = t + 1; y <= t + 2; y++) {
                    if (wall) {
                        p.set(x, y, z, "red_terracotta");
                    } else {
                        p.clear(x, y, z);
                    }
                }
                p.set(x, t + 3, z, "red_concrete");
            }
        }
        p.clear(bx, t + 1, bz - 1);
        p.clear(bx, t + 2, bz - 1);
        p.set(bx + 1, t + 2, bz + 1, "glass_pane");
        p.set(bx, t + 1, bz + 1, "white_carpet");
        p.set(bx, t + 2, bz + 1, "lantern[hanging=true]");
        p.set(bx, t + 4, bz, "snow[layers=2]");
        p.set(bx - 1, t + 4, bz + 1, "snow[layers=1]");
        // Laternen am Weg zur Grotte.
        Decor.lanternPost(p, -3, t + 1, 7, "birch_fence", "soul_lantern");
        Decor.lanternPost(p, 3, t + 1, 7, "birch_fence", "soul_lantern");
        Decor.spruce(p, -9, t + 1, -6, 5, r, true);
        Decor.spruce(p, 10, t + 1, -6, 4, r, true);
    }

    /** Wolkenmeer, Gletschergipfel in der Ferne, schwebende Kristalle außerhalb des Randes. */
    private static void scenery(Plan p, MapLayout layout, Random r) {
        Decor.cloudSea(p, -58, 10, 200, r);
        Decor.mountainRing(p, -60, 145, r);
        int border = layout.border();
        for (int i = 0; i < 8; i++) {
            double a = 2 * Math.PI * i / 8 + r.nextDouble() * 0.4;
            double d = border + 26 + r.nextInt(30);
            int x = (int) Math.round(Math.cos(a) * d);
            int z = (int) Math.round(Math.sin(a) * d);
            int y = -10 + r.nextInt(30);
            if (i % 2 == 0) {
                Decor.cloud(p, x, y, z, 4 + r.nextInt(3), r);
            } else {
                floatingCrystal(p, x, y, z, 5 + r.nextInt(4), r);
            }
        }
    }

    /** Schwebender Kristall: Doppelspitze aus Amethyst mit Kalzit-Kern. */
    private static void floatingCrystal(Plan p, int x, int y, int z, int size, Random r) {
        for (int dy = -size; dy <= size; dy++) {
            double rad = (size - Math.abs(dy)) * 0.45;
            int ri = (int) Math.ceil(rad);
            for (int dx = -ri; dx <= ri; dx++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    if (Math.hypot(dx, dz) <= rad + 0.2) {
                        boolean core = Math.hypot(dx, dz) < rad - 1;
                        p.set(x + dx, y + dy, z + dz, core ? "calcite" : r.nextInt(5) == 0 ? "budding_amethyst" : "amethyst_block");
                    }
                }
            }
        }
        p.set(x, y + size + 1, z, "amethyst_cluster[facing=up]");
        p.set(x, y - size - 1, z, "amethyst_cluster[facing=down]");
    }
}

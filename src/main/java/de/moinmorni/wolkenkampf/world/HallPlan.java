package de.moinmorni.wolkenkampf.world;

import de.moinmorni.wolkenkampf.logic.IslandColour;
import java.util.Random;

/**
 * Der Wolkenhafen: eine Alm-Insel, die auf Wolken sitzt. In der Mitte ein Platz mit einem großen, an Ketten
 * festgemachten Ballon, im Norden die Bestenliste, links und rechts die Kartentische, im Westen der Ausrüstungs-Stand,
 * im Osten die Tafel zur Spielart. Rundherum Fichten, Hecken, Laternen und Bänke; zwei kleine Ballons schweben vorbei.
 * Hier wartet man auf die nächste Runde und stimmt ab. Eigener Entwurf.
 */
public final class HallPlan {

    public static final double SPAWN_X = 0.5;
    public static final double SPAWN_Y = 1;
    public static final double SPAWN_Z = 11.5;
    public static final float SPAWN_YAW = 180f;
    /** Alles innerhalb dieses Abstands zur Mitte gehört zum Wolkenhafen (bleibt geladen). */
    public static final int REACH = 32;
    /** Radius der Insel. */
    public static final int RADIUS = 18;
    /** Kartentische (relativ): Wolkenalm links, Kristallgipfel rechts. */
    public static final int[][] TABLES = {{-7, -4}, {7, -4}};
    /** Bestenliste. */
    public static final int[] BOARD = {0, -13};
    /** Ausrüstungs-Stand (Westen) und Tafel zur Spielart (Osten). */
    public static final int[] KITS = {-12, 3};
    public static final int[] MODES = {12, 3};
    /** Wer tiefer fällt, kommt zurück zum Ankunftsplatz. */
    public static final int FALL_Y = -12;

    private HallPlan() {
    }

    public static Plan build() {
        Plan p = new Plan();
        Random r = new Random(2026);
        Decor.island(p, 0, 0, 0, RADIUS, 16, r, new Decor.Palette("grass_block[snowy=false]", "dirt", 2, Decor.ALM_ROCK,
                Decor.ALM_HANGING));
        plaza(p);
        for (int[] t : TABLES) {
            table(p, t[0], t[1]);
        }
        board(p);
        kitStall(p);
        modeBoard(p);
        // Großer Ballon über dem Platz, mit vier Ankerketten am Boden festgemacht.
        p.merge(Gondola.plan(IslandColour.ALPENROSE), 0, 9, 0);
        for (int[] k : new int[][] {{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
            p.set(k[0], 1, k[1], "stone_brick_wall");
            for (int y = 2; y <= 8; y++) {
                p.set(k[0], y, k[1], "iron_chain[axis=y]");
            }
        }
        // Laternen rund um den Platz, Bänke, Hecken und Fichten am Rand.
        for (int i = 0; i < 8; i++) {
            double a = Math.PI / 8 + i * Math.PI / 4;
            int x = (int) Math.round(Math.cos(a) * 9);
            int z = (int) Math.round(Math.sin(a) * 9);
            if (Math.abs(x) <= 1 && z > 0) {
                continue;
            }
            Decor.lanternPost(p, x, 1, z, "spruce_fence", "lantern");
        }
        Decor.bench(p, -5, 1, 8, "south", true);
        Decor.bench(p, 4, 1, 8, "south", true);
        Decor.bench(p, -9, 1, -8, "north", true);
        Decor.bench(p, 8, 1, -8, "north", true);
        rim(p, r);
        for (int x = -RADIUS - 1; x <= RADIUS + 1; x++) {
            for (int z = -RADIUS - 1; z <= RADIUS + 1; z++) {
                var d = p.get(x, 0, z);
                if (d != null && d.getMaterial() == org.bukkit.Material.GRASS_BLOCK && r.nextInt(100) < 35) {
                    Decor.meadow(p, x, 0, z, r);
                }
            }
        }
        // Zwei kleine Ballons ziehen vorbei, Wolken unter und um die Insel, Wolkenmeer und Gipfel in der Ferne.
        p.merge(Gondola.plan(IslandColour.ENZIAN), -27, 7, -12);
        p.merge(Gondola.plan(IslandColour.TROLLBLUME), 26, 13, -18);
        for (int i = 0; i < 14; i++) {
            double a = 2 * Math.PI * i / 14 + r.nextDouble() * 0.3;
            double d = RADIUS - 2 + r.nextInt(8);
            Decor.cloud(p, (int) Math.round(Math.cos(a) * d), -9 - r.nextInt(4), (int) Math.round(Math.sin(a) * d),
                    3 + r.nextInt(3), r);
        }
        Decor.cloudSea(p, -58, 0, 180, r);
        Decor.mountainRing(p, -60, 100, r);
        barrierRing(p);
        p.connect();
        return p;
    }

    /** Runder Platz mit weiß-blauem Rautenpflaster und Weg zum Ankunftsplatz. */
    private static void plaza(Plan p) {
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 13; z++) {
                double d = Math.hypot(x, z);
                boolean square = d <= 7.5;
                boolean path = Math.abs(x) <= 1 && z > 0 && z <= 13;
                if (!square && !path) {
                    continue;
                }
                String block;
                if (square && d > 6.6) {
                    block = "stone_bricks";
                } else if (square) {
                    block = Decor.lozenge(x, z, 2) ? "calcite" : "light_blue_terracotta";
                } else {
                    block = (z % 2 == 0) ? "polished_andesite" : "andesite";
                }
                p.set(x, 0, z, block);
            }
        }
        p.set(0, 0, 0, "chiseled_stone_bricks");
    }

    private static void table(Plan p, int x, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            p.set(x + dx, 1, z, "spruce_fence");
            p.set(x + dx, 2, z, "spruce_pressure_plate");
        }
        p.set(x, 2, z, "cartography_table");
        p.set(x, 0, z, "stone_bricks");
    }

    /** Bestenliste: Tafel aus dunklem Holz mit Dach. */
    private static void board(Plan p) {
        int bx = BOARD[0];
        int bz = BOARD[1] - 1;
        for (int dx = -4; dx <= 4; dx++) {
            for (int y = 1; y <= 4; y++) {
                p.set(bx + dx, y, bz, Math.abs(dx) == 4 ? "stripped_spruce_log[axis=y]" : "dark_oak_planks");
            }
            p.set(bx + dx, 5, bz, "spruce_slab[type=bottom]");
            p.set(bx + dx, 5, bz + 1, "spruce_stairs[facing=north,half=bottom,shape=straight]");
        }
        p.banner(bx - 4, 3, bz + 1, "light_blue_wall_banner[facing=south]", Decor.cloudBanner());
        p.banner(bx + 4, 3, bz + 1, "light_blue_wall_banner[facing=south]", Decor.cloudBanner());
    }

    /** Ausrüstungs-Stand: Marktstand mit gestreiftem Dach, Theke aus Fässern, Rüstungsständer-Ersatz aus Blöcken. */
    private static void kitStall(Plan p) {
        int x = KITS[0];
        int z = KITS[1];
        for (int dz = -2; dz <= 2; dz++) {
            p.set(x - 1, 1, z + dz, "barrel[facing=east,open=false]");
            p.set(x - 1, 2, z + dz, "spruce_slab[type=bottom]");
            for (int y = 1; y <= 3; y++) {
                if (Math.abs(dz) == 2) {
                    p.set(x - 3, y, z + dz, "spruce_fence");
                    p.set(x, y, z + dz, "spruce_fence");
                }
            }
            for (int dx = -3; dx <= 0; dx++) {
                p.set(x + dx, 4, z + dz, (dz + 2) % 2 == 0 ? "white_wool" : "light_blue_wool");
            }
        }
        p.set(x - 1, 3, z - 1, "potted_cornflower");
        p.set(x - 1, 3, z + 1, "lantern[hanging=false]");
        p.set(x - 2, 1, z, "smithing_table");
        p.set(x - 2, 1, z + 1, "fletching_table");
        p.set(x - 2, 1, z - 1, "crafting_table");
    }

    /** Tafel zur Spielart: zwei Fahnen (allein = eine, zu zweit = zwei) an einem Holzgestell. */
    private static void modeBoard(Plan p) {
        int x = MODES[0];
        int z = MODES[1];
        for (int dz = -2; dz <= 2; dz++) {
            p.set(x + 1, 1, z + dz, "spruce_planks");
            p.set(x + 1, 2, z + dz, Math.abs(dz) == 2 ? "stripped_spruce_log[axis=y]" : "spruce_planks");
            p.set(x + 1, 3, z + dz, "spruce_slab[type=bottom]");
        }
        p.banner(x, 2, z - 1, "blue_wall_banner[facing=west]", Decor.flagPattern(IslandColour.ENZIAN));
        p.banner(x, 2, z + 1, "red_wall_banner[facing=west]", Decor.flagPattern(IslandColour.ALPENROSE));
    }

    /** Rand: Fichten und Azaleenhecken in Lücken. */
    private static void rim(Plan p, Random r) {
        for (int i = 0; i < 26; i++) {
            double a = 2 * Math.PI * i / 26 + r.nextDouble() * 0.1;
            int x = (int) Math.round(Math.cos(a) * (RADIUS - 2.5));
            int z = (int) Math.round(Math.sin(a) * (RADIUS - 2.5));
            if (Math.abs(x) <= 3 && z > 0) {
                continue;
            }
            if (i % 3 == 0) {
                Decor.spruce(p, x, 1, z, 5 + r.nextInt(3), r, false);
            } else {
                p.setIfEmpty(x, 1, z, "azalea_leaves[persistent=true]");
                if (r.nextBoolean()) {
                    p.setIfEmpty(x, 2, z, "flowering_azalea_leaves[persistent=true]");
                }
            }
        }
    }

    /** Unsichtbarer Zaun um die Insel: niemand fällt hinunter. */
    private static void barrierRing(Plan p) {
        for (int x = -RADIUS - 3; x <= RADIUS + 3; x++) {
            for (int z = -RADIUS - 3; z <= RADIUS + 3; z++) {
                double d = Math.hypot(x, z);
                if (d > RADIUS - 0.5 && d <= RADIUS + 2.5 && p.get(x, 0, z) == null && near(p, x, z)) {
                    for (int y = 0; y <= 3; y++) {
                        p.setIfEmpty(x, y, z, "barrier");
                    }
                }
            }
        }
    }

    private static boolean near(Plan p, int x, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (p.get(x + dx, 0, z + dz) != null) {
                    return true;
                }
            }
        }
        return false;
    }
}

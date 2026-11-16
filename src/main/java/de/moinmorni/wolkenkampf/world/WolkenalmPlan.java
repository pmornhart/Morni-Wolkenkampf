package de.moinmorni.wolkenkampf.world;

import de.moinmorni.wolkenkampf.logic.IslandColour;
import de.moinmorni.wolkenkampf.logic.MapLayout;
import java.util.Random;

/**
 * Karte „Wolkenalm“: zwölf grüne Alm-Inseln im Ring, jede mit Fichte, Holzstoß, Laterne und einer Fahne in ihrer
 * Farbe; sechs kleine Inseln dazwischen; in der Mitte die große Alm mit weißer Bergkapelle samt Dachreiter und Glocke,
 * Maibaum, Brunnen, Linden und Bänken. Darunter das Wolkenmeer, ringsum ferne Gipfel. Eigener Entwurf.
 */
final class WolkenalmPlan {

    private static final Decor.Palette ALM = new Decor.Palette("grass_block[snowy=false]", "dirt", 2, Decor.ALM_ROCK,
            Decor.ALM_HANGING);

    private WolkenalmPlan() {
    }

    static Plan build(MapLayout layout) {
        Plan p = new Plan();
        Random r = new Random(4711);
        for (int i = 0; i < layout.starts().size(); i++) {
            MapLayout.Island is = layout.starts().get(i);
            Decor.island(p, is.x(), is.z(), is.top(), is.radius(), is.depth(), r, ALM);
            startIsland(p, is, IslandColour.of(i), r);
        }
        MapLayout.Island mid = layout.middle();
        Decor.island(p, mid.x(), mid.z(), mid.top(), mid.radius(), mid.depth(), r, ALM);
        middle(p, mid, r);
        for (MapLayout.Island s : layout.small()) {
            Decor.island(p, s.x(), s.z(), s.top(), s.radius(), s.depth(), r, ALM);
            smallIsland(p, s, r);
        }
        MapKit.chests(p, layout);
        for (MapLayout.Island is : layout.all()) {
            MapKit.meadow(p, is, r, 45);
        }
        MapKit.gondolas(p, layout);
        scenery(p, layout, r);
        p.connect();
        return p;
    }

    /** Startinsel: Fichte hinten in der Mitte, Holzstoß, Heuballen, Laterne, Fahne in der Inselfarbe vorne. */
    private static void startIsland(Plan p, MapLayout.Island is, IslandColour c, Random r) {
        int y = is.top() + 1;
        int[] tree = MapKit.local(is, 4, 0);
        Decor.spruce(p, tree[0], y, tree[1], 5, r, false);
        int[] pile = MapKit.local(is, 0, -4);
        Decor.woodpile(p, pile[0], y, pile[1], Math.abs(is.x()) > Math.abs(is.z()));
        int[] hay = MapKit.local(is, 1, 4);
        p.set(hay[0], y, hay[1], "hay_block[axis=y]");
        int[] hay2 = MapKit.local(is, 0, 4);
        if (p.get(hay2[0], y, hay2[1]) == null) {
            p.set(hay2[0], y, hay2[1], "hay_block[axis=x]");
        }
        int[] post = MapKit.local(is, -3, -3);
        Decor.lanternPost(p, post[0], y, post[1], "spruce_fence", "lantern");
        int[] flag = MapKit.local(is, -3, 3);
        Decor.flag(p, flag[0], y, flag[1], c, MapKit.rotationToMiddle(is));
    }

    /** Kleine Insel: Truhe, Azaleenbusch, Blumen. */
    private static void smallIsland(Plan p, MapLayout.Island s, Random r) {
        int y = s.top() + 1;
        p.set(s.x() + 2, y, s.z(), "flowering_azalea");
        p.set(s.x() - 2, y, s.z() + 1, "azalea");
        p.set(s.x() - 1, y, s.z() - 2, "mossy_cobblestone");
        p.set(s.x() - 1, y + 1, s.z() - 2, "lantern[hanging=false]");
    }

    /** Mittelinsel mit Bergkapelle, Maibaum, Brunnen, Linden, Bänken und Wegen. */
    private static void middle(Plan p, MapLayout.Island m, Random r) {
        int t = m.top();
        int y = t + 1;
        // Wege: vom Kapelleneingang nach Süden und rund um die Kapelle.
        MapKit.path(p, t, 0, 5, 0, 11, r);
        MapKit.path(p, t, -5, 6, 5, 6, r);
        MapKit.path(p, t, -5, -8, -5, 6, r);
        MapKit.path(p, t, 5, -8, 5, 6, r);
        chapel(p, t, r);
        maypole(p, -8, y, 2);
        well(p, 8, y, 2);
        Decor.broadTree(p, -9, y, -3, "oak_log", "oak_leaves", 4, 3, r);
        Decor.broadTree(p, 9, y, -3, "oak_log", "oak_leaves", 4, 3, r);
        Decor.bench(p, -4, y, 9, "south", true);
        Decor.bench(p, 3, y, 9, "south", true);
        Decor.lanternPost(p, -2, y, 5, "spruce_fence", "lantern");
        Decor.lanternPost(p, 2, y, 5, "spruce_fence", "lantern");
        Decor.lanternPost(p, -3, y, -8, "spruce_fence", "lantern");
        Decor.lanternPost(p, 3, y, -8, "spruce_fence", "lantern");
        p.set(-1, y, -8, "hay_block[axis=y]");
        p.set(1, y, -8, "barrel[facing=up,open=false]");
    }

    /**
     * Bergkapelle: weiß verputzt, Sockel aus Steinziegeln, Satteldach aus dunklem Holz, Dachreiter mit Glocke und Spitze,
     * innen Bänke, Altar mit Kerzen, Kreuz an der Wand und zwei Truhen neben dem Altar.
     */
    private static void chapel(Plan p, int t, Random r) {
        int x1 = -3;
        int x2 = 3;
        int z1 = -6;
        int z2 = 4;
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean wall = x == x1 || x == x2 || z == z1 || z == z2;
                p.set(x, t, z, wall ? "stone_bricks" : (x + z) % 2 == 0 ? "polished_andesite" : "smooth_stone");
                for (int y = t + 1; y <= t + 4; y++) {
                    if (wall) {
                        p.set(x, y, z, y == t + 1 ? "stone_bricks" : "white_concrete");
                    } else {
                        p.clear(x, y, z);
                    }
                }
            }
        }
        // Eingang (offener Bogen) im Süden.
        for (int y = t + 1; y <= t + 3; y++) {
            p.clear(0, y, z2);
        }
        p.set(-1, t + 3, z2, "white_concrete");
        p.set(1, t + 3, z2, "white_concrete");
        p.set(0, t + 4, z2, "stone_brick_stairs[facing=north,half=top,shape=straight]");
        // Fenster an den Seiten.
        for (int z : new int[] {-3, 0}) {
            for (int x : new int[] {x1, x2}) {
                p.set(x, t + 2, z, "glass_pane");
                p.set(x, t + 3, z, "glass_pane");
                p.set(x + (x < 0 ? -1 : 1), t + 1, z, "spruce_trapdoor[facing=" + (x < 0 ? "west" : "east")
                        + ",half=top,open=false]");
            }
        }
        // Satteldach (First entlang z), Giebelwände weiß.
        for (int k = 0; k <= 4; k++) {
            int yy = t + 5 + k;
            int w = 4 - k;
            for (int z = z1 - 1; z <= z2 + 1; z++) {
                if (w > 0) {
                    p.set(-w, yy, z, "dark_oak_stairs[facing=east,half=bottom,shape=straight]");
                    p.set(w, yy, z, "dark_oak_stairs[facing=west,half=bottom,shape=straight]");
                } else {
                    p.set(0, yy, z, "dark_oak_slab[type=bottom]");
                }
            }
            for (int x = -w + 1; x <= w - 1; x++) {
                p.set(x, yy, z1, "white_concrete");
                p.set(x, yy, z2, "white_concrete");
            }
        }
        p.set(0, t + 6, z2, "yellow_stained_glass_pane");
        p.set(0, t + 6, z1, "light_blue_stained_glass_pane");
        // Dachreiter mit Glocke über dem Eingang.
        int bz = 2;
        for (int x = -1; x <= 1; x++) {
            for (int z = bz - 1; z <= bz + 1; z++) {
                p.set(x, t + 8, z, "dark_oak_planks");
                p.set(x, t + 11, z, "dark_oak_planks");
                boolean corner = Math.abs(x) == 1 && Math.abs(z - bz) == 1;
                for (int y = t + 9; y <= t + 10; y++) {
                    if (corner) {
                        p.set(x, y, z, "dark_oak_fence");
                    } else {
                        p.clear(x, y, z);
                    }
                }
            }
        }
        p.set(0, t + 10, bz, "bell[attachment=ceiling,facing=north]");
        p.set(0, t + 12, bz - 1, "dark_oak_stairs[facing=south,half=bottom,shape=straight]");
        p.set(0, t + 12, bz + 1, "dark_oak_stairs[facing=north,half=bottom,shape=straight]");
        p.set(-1, t + 12, bz, "dark_oak_stairs[facing=east,half=bottom,shape=straight]");
        p.set(1, t + 12, bz, "dark_oak_stairs[facing=west,half=bottom,shape=straight]");
        p.set(0, t + 12, bz, "dark_oak_planks");
        p.set(0, t + 13, bz, "dark_oak_fence");
        p.set(0, t + 14, bz, "lightning_rod[facing=up]");
        p.set(0, t + 15, bz, "gold_block");
        // Innen: Bänke, Altar, Kerzen, Kreuz, Laterne.
        for (int z : new int[] {-2, 0, 2}) {
            for (int x : new int[] {-2, -1, 1, 2}) {
                p.set(x, t + 1, z, "spruce_stairs[facing=south,half=bottom,shape=straight]");
            }
        }
        for (int x = -1; x <= 1; x++) {
            p.set(x, t + 1, z1 + 1, x == 0 ? "chiseled_quartz_block" : "quartz_stairs[facing=" + (x < 0 ? "east" : "west")
                    + ",half=bottom,shape=straight]");
        }
        p.set(0, t + 2, z1 + 1, "white_candle[candles=3,lit=true]");
        // Kreuz an der Stirnwand: Längsbalken über die Querbalken hinaus.
        for (int y = t + 2; y <= t + 5; y++) {
            p.set(0, y, z1, "stripped_dark_oak_log[axis=y]");
        }
        p.set(-1, t + 4, z1, "stripped_dark_oak_log[axis=x]");
        p.set(1, t + 4, z1, "stripped_dark_oak_log[axis=x]");
        Decor.hangingLantern(p, 0, t + 8, -1, 1, "lantern");
        p.set(-2, t + 1, z1 + 1, "potted_white_tulip");
        p.set(2, t + 1, z1 + 1, "potted_blue_orchid");
    }

    /** Maibaum: weiß-blau gestreifter Stamm, Kranz, oben eine Rautenfahne. */
    private static void maypole(Plan p, int x, int y, int z) {
        p.set(x, y - 1, z, "stone_bricks");
        for (int i = 0; i < 13; i++) {
            p.set(x, y + i, z, i % 3 == 2 ? "light_blue_concrete" : "stripped_birch_log[axis=y]");
        }
        for (int[] o : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}}) {
            p.set(x + o[0], y + 10, z + o[1], "spruce_leaves[persistent=true]");
        }
        p.set(x, y + 13, z, "birch_fence");
        p.banner(x, y + 14, z, "white_banner[rotation=0]", Decor.lozenges());
        for (int[] o : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            p.set(x + o[0], y, z + o[1], "flowering_azalea_leaves[persistent=true]");
        }
    }

    /** Dorfbrunnen: Steintrog mit Wasser, Laterne am Pfosten. */
    private static void well(Plan p, int x, int y, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                p.set(x + dx, y - 1, z + dz, "stone_bricks");
                p.set(x + dx, y, z + dz, dx == 0 && dz == 0 ? "water" : "stone_brick_wall");
            }
        }
        p.set(x, y + 1, z - 1, "stone_brick_wall");
        p.set(x, y + 2, z - 1, "stone_brick_wall");
        p.set(x, y + 3, z - 1, "lantern[hanging=false]");
    }

    /** Wolkenmeer unten, ferne Gipfel, kleine Wolken und schwebende Alm-Inselchen außerhalb des Randes. */
    private static void scenery(Plan p, MapLayout layout, Random r) {
        Decor.cloudSea(p, -58, 10, 200, r);
        Decor.mountainRing(p, -60, 112, r);
        int border = layout.border();
        for (int i = 0; i < 9; i++) {
            double a = 2 * Math.PI * i / 9 + r.nextDouble() * 0.4;
            double d = border + 26 + r.nextInt(30);
            Decor.cloud(p, (int) Math.round(Math.cos(a) * d), 18 + r.nextInt(16), (int) Math.round(Math.sin(a) * d),
                    4 + r.nextInt(3), r);
        }
        for (int i = 0; i < 5; i++) {
            double a = 2 * Math.PI * i / 5 + 0.6;
            double d = border + 30 + r.nextInt(25);
            int x = (int) Math.round(Math.cos(a) * d);
            int z = (int) Math.round(Math.sin(a) * d);
            int top = -6 + r.nextInt(14);
            Decor.island(p, x, z, top, 4 + r.nextInt(2), 9, r, ALM);
            Decor.spruce(p, x, top + 1, z, 4 + r.nextInt(3), r, false);
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    var d0 = p.get(x + dx, top, z + dz);
                    if (d0 != null && d0.getMaterial() == org.bukkit.Material.GRASS_BLOCK) {
                        Decor.meadow(p, x + dx, top, z + dz, r);
                    }
                }
            }
        }
    }
}

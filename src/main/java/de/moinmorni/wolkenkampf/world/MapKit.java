package de.moinmorni.wolkenkampf.world;

import de.moinmorni.wolkenkampf.logic.IslandColour;
import de.moinmorni.wolkenkampf.logic.MapLayout;
import java.util.Random;

/** Gemeinsame Bausteine beider Karten: Truhen, Gondeln, Ausrichtung auf einer Startinsel, Blumenwiesen. */
final class MapKit {

    private MapKit() {
    }

    /**
     * Stelle auf einer Startinsel in eigenen Achsen: {@code out} weg von der Kartenmitte, {@code side} quer dazu.
     *
     * @return {x, z}
     */
    static int[] local(MapLayout.Island is, double out, double side) {
        double len = Math.hypot(is.x(), is.z());
        double ox = len == 0 ? 0 : is.x() / len;
        double oz = len == 0 ? 1 : is.z() / len;
        double px = -oz;
        double pz = ox;
        return new int[] {is.x() + (int) Math.round(ox * out + px * side), is.z() + (int) Math.round(oz * out + pz * side)};
    }

    /** Banner-Drehung (0–15), die von der Insel zur Kartenmitte zeigt. */
    static String rotationToMiddle(MapLayout.Island is) {
        double a = Math.atan2(-is.x(), is.z()); // Minecraft: 0 = Süden, im Uhrzeigersinn
        int rot = (int) Math.round(Math.toDegrees(a) / 22.5);
        return String.valueOf(Math.floorMod(rot + 8, 16));
    }

    /** Muss dieser Block frei bleiben (Landeplatz unter der Gondel oder Truhe)? */
    static boolean reserved(MapLayout.Island is, int x, int z) {
        if (Math.abs(x - is.x()) <= 1 && Math.abs(z - is.z()) <= 1) {
            return true;
        }
        for (MapLayout.Spot s : is.chests()) {
            if (Math.abs(s.x() - x) <= 1 && Math.abs(s.z() - z) <= 1) {
                return true;
            }
        }
        return false;
    }

    /** Alle Truhen der Karte (leer – gefüllt wird zu Beginn jeder Runde). */
    static void chests(Plan p, MapLayout layout) {
        for (MapLayout.Island is : layout.all()) {
            for (MapLayout.Spot s : is.chests()) {
                p.set(s.x(), s.y(), s.z(), "chest[facing=" + s.facing() + ",type=single,waterlogged=false]");
                p.clear(s.x(), s.y() + 1, s.z());
            }
        }
    }

    /** Über jeder Startinsel eine Ballon-Gondel in der Inselfarbe. */
    static void gondolas(Plan p, MapLayout layout) {
        for (int i = 0; i < layout.starts().size(); i++) {
            MapLayout.Island is = layout.starts().get(i);
            p.merge(Gondola.plan(IslandColour.of(i)), is.x(), is.gondolaY(), is.z());
        }
    }

    /** Freie Grasblöcke einer Insel mit Gras und Blumen bestreuen. */
    static void meadow(Plan p, MapLayout.Island is, Random r, int chance) {
        for (int dx = -is.radius(); dx <= is.radius(); dx++) {
            for (int dz = -is.radius(); dz <= is.radius(); dz++) {
                int x = is.x() + dx;
                int z = is.z() + dz;
                if (reserved(is, x, z) || r.nextInt(100) >= chance) {
                    continue;
                }
                var d = p.get(x, is.top(), z);
                if (d != null && d.getMaterial() == org.bukkit.Material.GRASS_BLOCK) {
                    Decor.meadow(p, x, is.top(), z, r);
                }
            }
        }
    }

    /** Ein Wegstück aus Kies und Pfad-Erde (nur auf vorhandenem Boden). */
    static void path(Plan p, int top, int x1, int z1, int x2, int z2, Random r) {
        int steps = (int) Math.ceil(Math.hypot(x2 - x1, z2 - z1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0 : i / (double) steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t);
            int z = (int) Math.round(z1 + (z2 - z1) * t);
            for (int[] o : new int[][] {{0, 0}, {1, 0}, {0, 1}}) {
                var d = p.get(x + o[0], top, z + o[1]);
                if (d != null && d.getMaterial() == org.bukkit.Material.GRASS_BLOCK) {
                    p.set(x + o[0], top, z + o[1], Decor.pick(r, "dirt_path", "dirt_path", "coarse_dirt", "gravel"));
                    p.clear(x + o[0], top + 1, z + o[1]);
                }
            }
        }
    }
}

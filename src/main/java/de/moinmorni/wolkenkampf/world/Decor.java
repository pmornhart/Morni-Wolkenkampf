package de.moinmorni.wolkenkampf.world;

import de.moinmorni.wolkenkampf.logic.IslandColour;
import java.util.List;
import java.util.Random;
import org.bukkit.DyeColor;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;

/**
 * Bausteine für Wolkenhafen und Karten: schwebende Inseln mit Wurzeln und Zapfen darunter, Wolken, Bäume, Berge in der
 * Ferne, Laternen, Banner. Eigene Entwürfe, nur Minecraft-Blöcke.
 */
final class Decor {

    private Decor() {
    }

    /** Woraus eine Insel besteht. */
    interface Rock {
        String at(Random r, int depth);
    }

    /**
     * Aussehen einer Insel.
     *
     * @param surface  oberste Schicht
     * @param soil     darunter
     * @param soilDepth so viele Schichten {@code soil}
     * @param rock     Fels darunter (nach Tiefe)
     * @param hanging  was unten herabhängt
     */
    record Palette(String surface, String soil, int soilDepth, Rock rock, Hanging hanging) {
    }

    /** Was unter einer Insel hängt (Wurzeln, Tropfstein, Kristalle …). */
    interface Hanging {
        void at(Plan p, int x, int y, int z, Random r);
    }

    static String pick(Random r, String... options) {
        return options[r.nextInt(options.length)];
    }

    static final Rock ALM_ROCK = (r, d) -> {
        int roll = r.nextInt(100);
        if (d > 9) {
            return roll < 40 ? "deepslate" : roll < 65 ? "tuff" : roll < 85 ? "stone" : "cobbled_deepslate";
        }
        return roll < 55 ? "stone" : roll < 70 ? "andesite" : roll < 82 ? "tuff" : roll < 92 ? "cobblestone"
                : roll < 97 ? "mossy_cobblestone" : "coal_ore";
    };

    static final Rock CRYSTAL_ROCK = (r, d) -> {
        int roll = r.nextInt(100);
        if (d > 9) {
            return roll < 35 ? "calcite" : roll < 60 ? "stone" : roll < 80 ? "smooth_basalt" : roll < 92 ? "tuff"
                    : "amethyst_block";
        }
        return roll < 40 ? "stone" : roll < 60 ? "calcite" : roll < 72 ? "diorite" : roll < 84 ? "andesite"
                : roll < 92 ? "packed_ice" : "iron_ore";
    };

    /** Unter der Alm: Wurzeln, Moos, kleine Tropfsteine. */
    static final Hanging ALM_HANGING = (p, x, y, z, r) -> {
        int roll = r.nextInt(100);
        if (roll < 14) {
            p.setIfEmpty(x, y, z, "hanging_roots");
        } else if (roll < 20) {
            p.setIfEmpty(x, y, z, "pointed_dripstone[vertical_direction=down,thickness=frustum]");
            p.setIfEmpty(x, y - 1, z, "pointed_dripstone[vertical_direction=down,thickness=tip]");
        } else if (roll < 23) {
            p.setIfEmpty(x, y, z, "cave_vines_plant[berries=true]");
            p.setIfEmpty(x, y - 1, z, "cave_vines[berries=true,age=25]");
        }
    };

    /** Unter dem Kristallgipfel: Amethyst und Eiszapfen. */
    static final Hanging CRYSTAL_HANGING = (p, x, y, z, r) -> {
        int roll = r.nextInt(100);
        if (roll < 9) {
            p.setIfEmpty(x, y, z, "amethyst_cluster[facing=down]");
        } else if (roll < 14) {
            p.setIfEmpty(x, y, z, "large_amethyst_bud[facing=down]");
        } else if (roll < 19) {
            int len = 1 + r.nextInt(3);
            for (int i = 0; i < len; i++) {
                p.setIfEmpty(x, y - i, z, i == len - 1 ? "blue_ice" : "packed_ice");
            }
        }
    };

    /**
     * Schwebende Insel: welliger Rand (kein Kreis), oben flach, nach unten spitz zulaufend, unten hängt etwas herab.
     * Liefert nichts zurück – der Plan ist danach gefüllt.
     *
     * @param top    oberste Schicht (y)
     * @param radius ungefährer Radius der Oberfläche (der Rand weicht höchstens etwa ±1,3 ab)
     * @param depth  so tief reicht sie in der Mitte
     */
    static void island(Plan p, int cx, int cz, int top, int radius, int depth, Random r, Palette pal) {
        double ph1 = r.nextDouble() * Math.PI * 2;
        double ph2 = r.nextDouble() * Math.PI * 2;
        double ph3 = r.nextDouble() * Math.PI * 2;
        int reach = radius + 2;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                double d = Math.hypot(dx, dz);
                double a = Math.atan2(dz, dx);
                double edge = radius + 0.7 * Math.sin(3 * a + ph1) + 0.4 * Math.sin(5 * a + ph2)
                        + 0.25 * Math.sin(7 * a + ph3);
                if (d > edge + 0.35) {
                    continue;
                }
                double f = Math.min(1, d / Math.max(1, edge));
                int col = 1 + (int) Math.round(depth * Math.pow(1 - Math.pow(f, 1.7), 0.9)) + r.nextInt(2);
                int x = cx + dx;
                int z = cz + dz;
                p.set(x, top, z, pal.surface());
                for (int i = 1; i <= col; i++) {
                    p.setIfEmpty(x, top - i, z, i <= pal.soilDepth() ? pal.soil() : pal.rock().at(r, i));
                }
                if (pal.hanging() != null) {
                    pal.hanging().at(p, x, top - col - 1, z, r);
                }
            }
        }
    }

    /** Liegt (x|z) sicher auf der Oberfläche einer Insel (nicht am wackligen Rand)? */
    static boolean inner(int cx, int cz, int radius, int x, int z) {
        return Math.hypot(x - cx, z - cz) <= radius - 1.4;
    }

    /** Wolke aus Wolle: mehrere Kugeln, unten grau schattiert. */
    static void cloud(Plan p, int cx, int cy, int cz, int size, Random r) {
        int puffs = 3 + size / 2 + r.nextInt(3);
        for (int i = 0; i < puffs; i++) {
            int px = cx + (int) Math.round((r.nextDouble() - 0.5) * size * 2.2);
            int pz = cz + (int) Math.round((r.nextDouble() - 0.5) * size * 1.4);
            int py = cy + r.nextInt(2);
            double rad = size * (0.45 + r.nextDouble() * 0.45);
            int ri = (int) Math.ceil(rad);
            for (int dx = -ri; dx <= ri; dx++) {
                for (int dy = -ri / 2; dy <= ri; dy++) {
                    for (int dz = -ri; dz <= ri; dz++) {
                        double d = Math.sqrt(dx * dx + dy * dy * 2.4 + dz * dz);
                        if (d <= rad) {
                            p.set(px + dx, py + dy, pz + dz, dy <= -ri / 2 + 0 && dy < 0 ? "light_gray_wool" : "white_wool");
                        }
                    }
                }
            }
        }
    }

    /** Wolkenmeer: viele flache Wolken in einer Schicht, mit Lücken. */
    static void cloudSea(Plan p, int y, int inner, int outer, Random r) {
        for (int i = 0; i < 70; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double d = inner + r.nextDouble() * (outer - inner);
            cloud(p, (int) Math.round(Math.cos(a) * d), y + r.nextInt(3) - 1, (int) Math.round(Math.sin(a) * d),
                    5 + r.nextInt(5), r);
        }
    }

    /** Fichte: Stamm und kegelförmige Krone. {@code snow} legt Schnee auf die Äste. */
    static void spruce(Plan p, int x, int y, int z, int height, Random r, boolean snow) {
        for (int i = 0; i < height; i++) {
            p.set(x, y + i, z, "spruce_log[axis=y]");
        }
        int top = y + height;
        p.set(x, top, z, "spruce_leaves[persistent=true]");
        p.set(x, top + 1, z, "spruce_leaves[persistent=true]");
        if (snow) {
            p.set(x, top + 2, z, "snow[layers=2]");
        }
        for (int layer = 0; layer < height - 1; layer++) {
            int ly = top - layer;
            int rad = 1 + layer / 2 - (layer % 2 == 0 ? 0 : 1);
            rad = Math.max(1, Math.min(3, rad));
            for (int dx = -rad; dx <= rad; dx++) {
                for (int dz = -rad; dz <= rad; dz++) {
                    if ((dx != 0 || dz != 0) && Math.abs(dx) + Math.abs(dz) <= rad + 1 && r.nextInt(10) > 0) {
                        p.setIfEmpty(x + dx, ly, z + dz, "spruce_leaves[persistent=true]");
                        if (snow && layer % 2 == 0 && r.nextInt(3) > 0) {
                            p.setIfEmpty(x + dx, ly + 1, z + dz, "snow[layers=1]");
                        }
                    }
                }
            }
        }
    }

    /** Laubbaum mit runder Krone (Linde, Ahorn …). */
    static void broadTree(Plan p, int x, int y, int z, String log, String leaves, int trunk, int radius, Random r) {
        for (int i = 0; i < trunk; i++) {
            p.set(x, y + i, z, log + "[axis=y]");
        }
        int cy = y + trunk + radius / 2;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius + 1; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double d = Math.sqrt(dx * dx + dy * dy * 1.6 + dz * dz);
                    if (d <= radius + 0.3 - r.nextDouble() * 0.6) {
                        p.setIfEmpty(x + dx, cy + dy, z + dz, leaves + "[persistent=true]");
                    }
                }
            }
        }
        for (int i = trunk; i < trunk + radius / 2 + 1; i++) {
            p.set(x, y + i, z, log + "[axis=y]");
        }
    }

    /** Laternenpfahl: drei Zaunpfosten, oben eine Laterne. */
    static void lanternPost(Plan p, int x, int y, int z, String fence, String lantern) {
        for (int i = 0; i < 3; i++) {
            p.set(x, y + i, z, fence);
        }
        p.set(x, y + 3, z, lantern + "[hanging=false]");
    }

    /** Hängende Laterne an einer Kette. */
    static void hangingLantern(Plan p, int x, int y, int z, int chain, String lantern) {
        for (int i = 0; i < chain; i++) {
            p.set(x, y - i, z, "iron_chain[axis=y]");
        }
        p.set(x, y - chain, z, lantern + "[hanging=true]");
    }

    /** Ferner Berg mit Schneekappe, der aus dem Wolkenmeer ragt. */
    static void mountain(Plan p, int cx, int cz, int y0, int radius, int height, Random r) {
        double ph = r.nextDouble() * 6;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz) / radius;
                if (d > 1) {
                    continue;
                }
                double ridge = 0.14 * Math.sin(dx * 0.45 + ph) * Math.cos(dz * 0.35 - ph);
                int top = (int) Math.round(height * Math.pow(1 - d, 1.3) + height * ridge * (1 - d));
                if (top < 1) {
                    continue;
                }
                int crust = 4 + r.nextInt(3);
                for (int y = Math.max(0, top - crust); y <= top; y++) {
                    String block;
                    if (y >= top - 1 && top > height * 0.55) {
                        block = r.nextInt(7) == 0 ? "packed_ice" : "snow_block";
                    } else if (y >= top - 1 && top < height * 0.2) {
                        block = r.nextInt(3) == 0 ? "moss_block" : "grass_block[snowy=false]";
                    } else {
                        block = ALM_ROCK.at(r, top - y > 2 ? 10 : 0);
                    }
                    p.setIfEmpty(cx + dx, y0 + y, cz + dz, block);
                }
            }
        }
    }

    /** Ring ferner Gipfel um die Karte. */
    static void mountainRing(Plan p, int y0, int distance, Random r) {
        int count = 7;
        for (int i = 0; i < count; i++) {
            double a = 2 * Math.PI * i / count + r.nextDouble() * 0.5;
            double d = distance + r.nextInt(30);
            mountain(p, (int) Math.round(Math.cos(a) * d), (int) Math.round(Math.sin(a) * d), y0, 22 + r.nextInt(14),
                    52 + r.nextInt(28), r);
        }
    }

    /** Blumen und Gras auf einem Grasblock (nur wenn darüber frei ist). */
    static void meadow(Plan p, int x, int y, int z, Random r) {
        if (p.get(x, y + 1, z) != null) {
            return;
        }
        int roll = r.nextInt(100);
        String plant = roll < 30 ? "short_grass" : roll < 38 ? "fern" : roll < 44 ? "dandelion" : roll < 49 ? "poppy"
                : roll < 54 ? "cornflower" : roll < 59 ? "oxeye_daisy" : roll < 63 ? "azure_bluet" : roll < 66 ? "allium"
                : roll < 69 ? "lily_of_the_valley" : null;
        if (plant != null) {
            p.set(x, y + 1, z, plant);
        }
    }

    /** Flagge in der Inselfarbe an einem Fahnenmast. */
    static void flag(Plan p, int x, int y, int z, IslandColour c, String rotation) {
        p.set(x, y, z, "spruce_fence");
        p.set(x, y + 1, z, "spruce_fence");
        p.banner(x, y + 2, z, c.dye() + "_banner[rotation=" + rotation + "]", flagPattern(c));
    }

    static List<Pattern> flagPattern(IslandColour c) {
        DyeColor accent = c == IslandColour.EDELWEISS ? DyeColor.YELLOW : DyeColor.WHITE;
        return List.of(new Pattern(accent, PatternType.STRIPE_MIDDLE), new Pattern(accent, PatternType.FLOWER),
                new Pattern(DyeColor.valueOf(c.dye().toUpperCase(java.util.Locale.ROOT)), PatternType.BORDER));
    }

    /** Weiß-blaue Rauten. */
    static List<Pattern> lozenges() {
        return List.of(new Pattern(DyeColor.LIGHT_BLUE, PatternType.RHOMBUS),
                new Pattern(DyeColor.LIGHT_BLUE, PatternType.TRIANGLES_TOP),
                new Pattern(DyeColor.LIGHT_BLUE, PatternType.TRIANGLES_BOTTOM));
    }

    /** Wolkenkampf-Banner: himmelblau mit weißer Wolke. */
    static List<Pattern> cloudBanner() {
        return List.of(new Pattern(DyeColor.WHITE, PatternType.FLOWER), new Pattern(DyeColor.WHITE, PatternType.HALF_HORIZONTAL_BOTTOM),
                new Pattern(DyeColor.LIGHT_BLUE, PatternType.CURLY_BORDER));
    }

    /** Rautenmuster wie die weiß-blaue Fahne: schräg gestelltes Schachbrett. */
    static boolean lozenge(int x, int z, int size) {
        return Math.floorMod(Math.floorDiv(x + z, size) + Math.floorDiv(x - z, size), 2) == 0;
    }

    /** Sitzbank aus Treppe mit Falltüren als Armlehnen. */
    static void bench(Plan p, int x, int y, int z, String facing, boolean alongX) {
        String stairs = "spruce_stairs[facing=" + facing + ",half=bottom,shape=straight]";
        if (alongX) {
            p.set(x, y, z, stairs);
            p.set(x + 1, y, z, stairs);
            p.set(x - 1, y, z, "spruce_trapdoor[facing=west,half=bottom,open=true]");
            p.set(x + 2, y, z, "spruce_trapdoor[facing=east,half=bottom,open=true]");
        } else {
            p.set(x, y, z, stairs);
            p.set(x, y, z + 1, stairs);
            p.set(x, y, z - 1, "spruce_trapdoor[facing=north,half=bottom,open=true]");
            p.set(x, y, z + 2, "spruce_trapdoor[facing=south,half=bottom,open=true]");
        }
    }

    /** Holzstoß aus liegenden Stämmen. */
    static void woodpile(Plan p, int x, int y, int z, boolean alongX) {
        String log = "spruce_log[axis=" + (alongX ? "z" : "x") + "]";
        for (int i = 0; i < 2; i++) {
            int ox = alongX ? i : 0;
            int oz = alongX ? 0 : i;
            p.set(x + ox, y, z + oz, log);
        }
        p.set(x, y + 1, z, log);
    }
}

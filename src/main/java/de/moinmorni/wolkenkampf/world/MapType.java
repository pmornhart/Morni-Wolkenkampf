package de.moinmorni.wolkenkampf.world;

import de.moinmorni.wolkenkampf.logic.MapLayout;
import java.util.List;
import java.util.Locale;

/**
 * Die Karten. Jede liegt an einer eigenen Stelle der Welt (Wolkenhafen bei 0|0) und hat ihren eigenen Aufbau
 * ({@link MapLayout}) und Bauplan.
 */
public enum MapType {
    WOLKENALM("wolkenalm", 1000, "spruce_planks", "cobblestone", "grass_block"),
    KRISTALLGIPFEL("kristallgipfel", 2000, "birch_planks", "calcite", "amethyst_cluster");

    /** Bodenhöhe aller Inseln (y der obersten Schicht = FLOOR_Y + top). */
    public static final int FLOOR_Y = 100;
    /** So viel Platz rund um den Rand gehört noch zur Karte (Wiederherstellung, Aufräumen). */
    private static final int MARGIN = 6;

    private final String id;
    private final int originX;
    private final String planks;
    private final String stone;
    private final String icon;
    private MapLayout layout;
    private Plan plan;
    private Plan playPlan;

    MapType(String id, int originX, String planks, String stone, String icon) {
        this.id = id;
        this.originX = originX;
        this.planks = planks;
        this.stone = stone;
        this.icon = icon;
    }

    public String id() {
        return id;
    }

    public int originX() {
        return originX;
    }

    public int originZ() {
        return 0;
    }

    /** Baublock „Bretter“ dieser Karte (Minecraft-Name). */
    public String planks() {
        return planks;
    }

    /** Baublock „Stein“ dieser Karte (Minecraft-Name). */
    public String stone() {
        return stone;
    }

    /** Gegenstand fürs Menü. */
    public String icon() {
        return icon;
    }

    public synchronized MapLayout layout() {
        if (layout == null) {
            layout = switch (this) {
                case WOLKENALM -> MapLayout.ring(12, 42, 0, 6, new int[] {0, 1}, 12, 3, List.of(
                        new MapLayout.Spot(-2, 1, -4, "east"), new MapLayout.Spot(2, 1, -4, "west"),
                        new MapLayout.Spot(-7, 1, -7, "south"), new MapLayout.Spot(7, 1, -7, "south"),
                        new MapLayout.Spot(-7, 1, 7, "north"), new MapLayout.Spot(7, 1, 7, "north")),
                        6, 25, 3, 1);
                case KRISTALLGIPFEL -> MapLayout.ring(12, 40, 15, 6, new int[] {1, 0}, 13, 4, List.of(
                        new MapLayout.Spot(-2, 1, 3, "east"), new MapLayout.Spot(2, 1, 3, "west"),
                        new MapLayout.Spot(-7, 1, -7, "south"), new MapLayout.Spot(7, 1, -7, "south"),
                        new MapLayout.Spot(-8, 1, 6, "east"), new MapLayout.Spot(8, 1, 6, "west")),
                        6, 24, 4, 1);
            };
        }
        return layout;
    }

    /** Ganzer Bauplan mit Kulisse (Wolkenmeer, Berge) – nur beim Bauen gebraucht. */
    public synchronized Plan plan() {
        if (plan == null) {
            plan = switch (this) {
                case WOLKENALM -> WolkenalmPlan.build(layout());
                case KRISTALLGIPFEL -> KristallgipfelPlan.build(layout());
            };
        }
        return plan;
    }

    /** Nur der Teil, in dem gespielt wird (wird nach jeder Runde wiederhergestellt). */
    public synchronized Plan playPlan() {
        if (playPlan == null) {
            int[] b = playBox();
            playPlan = plan().crop(b[0], b[1], b[2], b[3], b[4], b[5]);
        }
        return playPlan;
    }

    /** Ganzen Bauplan vergessen (nach dem Bauen; die Spielfläche bleibt im Speicher). */
    public synchronized void forgetFullPlan() {
        playPlan();
        plan = null;
    }

    /** Box der Spielfläche (relativ): minX, minY, minZ, maxX, maxY, maxZ. */
    public int[] playBox() {
        MapLayout l = layout();
        int r = l.border() + MARGIN;
        return new int[] {-r, l.voidY(), -r, r, l.buildTop() + Gondola.height() + 4, r};
    }

    public static MapType byId(String s) {
        if (s == null) {
            return null;
        }
        String k = s.toLowerCase(Locale.ROOT);
        for (MapType m : values()) {
            if (m.id.equals(k)) {
                return m;
            }
        }
        return switch (k) {
            case "alm", "cloud-alp", "wolken" -> WOLKENALM;
            case "kristall", "crystal", "gipfel", "crystal-peak" -> KRISTALLGIPFEL;
            default -> null;
        };
    }
}

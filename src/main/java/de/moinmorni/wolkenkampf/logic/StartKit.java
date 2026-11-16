package de.moinmorni.wolkenkampf.logic;

import java.util.List;
import java.util.Locale;

/**
 * Start-Ausrüstungen: frei wählbar, alle kostenlos, alle gleich bescheiden – nichts davon gibt es zu kaufen. Man bekommt
 * sie, sobald sich die Gondel öffnet.
 */
public enum StartKit {
    WANDERER("wanderer", "leather_boots", List.of()),
    BAUMEISTER("baumeister", "bricks", List.of(new Loot.Stack("planks", 24), new Loot.Stack("stone_pickaxe", 1))),
    JAEGER("jaeger", "bow", List.of(new Loot.Stack("bow", 1), new Loot.Stack("arrow", 8))),
    BERGFUEHRER("bergfuehrer", "water_bucket", List.of(new Loot.Stack("leather_boots_feather", 1),
            new Loot.Stack("water_bucket", 1))),
    SENNERIN("sennerin", "baked_potato", List.of(new Loot.Stack("baked_potato", 6), new Loot.Stack("potion_healing", 1))),
    HOLZKNECHT("holzknecht", "stone_axe", List.of(new Loot.Stack("stone_axe", 1))),
    SCHMIED("schmied", "chainmail_chestplate", List.of(new Loot.Stack("chainmail_chestplate", 1)));

    private final String id;
    private final String icon;
    private final List<Loot.Stack> items;

    StartKit(String id, String icon, List<Loot.Stack> items) {
        this.id = id;
        this.icon = icon;
        this.items = items;
    }

    public String id() {
        return id;
    }

    /** Minecraft-Gegenstand fürs Menü. */
    public String icon() {
        return icon;
    }

    public List<Loot.Stack> items() {
        return items;
    }

    /** Nach Name; unbekannt → null. */
    public static StartKit byId(String s) {
        if (s == null) {
            return null;
        }
        String k = s.toLowerCase(Locale.ROOT).replace("ü", "ue").replace("ä", "ae");
        for (StartKit kit : values()) {
            if (kit.id.equals(k)) {
                return kit;
            }
        }
        return switch (k) {
            case "builder" -> BAUMEISTER;
            case "hunter" -> JAEGER;
            case "guide", "mountain-guide" -> BERGFUEHRER;
            case "dairymaid", "cheesemaker" -> SENNERIN;
            case "lumberjack" -> HOLZKNECHT;
            case "smith" -> SCHMIED;
            case "hiker", "none", "ohne" -> WANDERER;
            default -> null;
        };
    }
}

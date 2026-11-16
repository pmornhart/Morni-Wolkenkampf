package de.moinmorni.wolkenkampf.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

/**
 * Was in den Truhen liegt – ohne Minecraft, nur Namen und Mengen. Drei Stufen: normale Inseln, Mittelinsel (besser) und
 * Nachschub. Gerecht verteilt: jede Startinsel bekommt über ihre Truhen zusammen mindestens eine Waffe, ein
 * Rüstungsteil, genug Blöcke für eine Brücke und etwas zu essen.
 *
 * <p>Namen der Dinge: „planks“ und „stone“ sind die Baublöcke der Karte, alles andere steht in {@link #ITEMS}.
 */
public final class Loot {

    /** Plätze einer Truhe. */
    public static final int SLOTS = 27;
    /** So viele Blöcke bekommt jede Startinsel mindestens (genug für eine Brücke zur Nachbarinsel). */
    public static final int MIN_BLOCKS = 40;

    public enum Tier {
        /** Startinseln und kleine Inseln. */
        ISLAND,
        /** Die große Mittelinsel: bessere Waffen und Rüstung. */
        MIDDLE,
        /** Nachschub mitten in der Runde. */
        REFILL
    }

    public enum Kind {
        WEAPON, ARMOR, BLOCKS, BOW, ARROWS, FOOD, POTION, TOOL, UTILITY
    }

    /** Ein Eintrag der Tabelle: Ding, Menge von–bis, Gewicht (je höher, desto häufiger). */
    public record Entry(String id, int min, int max, int weight, Kind kind) {
    }

    /** Ein Stapel in einer Truhe. */
    public record Stack(String id, int amount) {
    }

    /** Alle Dinge, die es geben kann, mit ihrem Minecraft-Gegenstand (Verzauberung/Trank steht im Namen dahinter). */
    public static final Map<String, String> ITEMS;

    private static final Map<Tier, List<Entry>> TABLES = new LinkedHashMap<>();

    static {
        Map<String, String> m = new TreeMap<>();
        for (String plain : List.of("stone_sword", "iron_sword", "diamond_sword", "stone_axe", "iron_axe",
                "leather_helmet", "leather_chestplate", "leather_leggings", "leather_boots",
                "chainmail_helmet", "chainmail_chestplate", "chainmail_leggings", "chainmail_boots",
                "golden_helmet", "golden_chestplate",
                "iron_helmet", "iron_chestplate", "iron_leggings", "iron_boots",
                "diamond_helmet", "diamond_chestplate", "diamond_leggings", "diamond_boots",
                "bow", "arrow", "bread", "cooked_beef", "baked_potato", "apple", "golden_carrot", "golden_apple",
                "stone_pickaxe", "iron_pickaxe", "shears", "snowball", "water_bucket", "fishing_rod", "ender_pearl",
                "cobweb", "shield")) {
            m.put(plain, plain);
        }
        m.put("planks", "planks");
        m.put("stone", "stone");
        m.put("iron_sword_sharp", "iron_sword");
        m.put("bow_power", "bow");
        m.put("leather_boots_feather", "leather_boots");
        m.put("iron_chestplate_prot", "iron_chestplate");
        m.put("potion_healing", "potion");
        m.put("potion_speed", "potion");
        m.put("potion_regeneration", "potion");
        m.put("potion_strength", "potion");
        m.put("potion_fire", "potion");
        m.put("splash_healing", "splash_potion");
        ITEMS = Collections.unmodifiableMap(m);

        TABLES.put(Tier.ISLAND, List.of(
                new Entry("stone_sword", 1, 1, 10, Kind.WEAPON),
                new Entry("iron_sword", 1, 1, 3, Kind.WEAPON),
                new Entry("stone_axe", 1, 1, 5, Kind.WEAPON),
                new Entry("leather_helmet", 1, 1, 3, Kind.ARMOR),
                new Entry("leather_chestplate", 1, 1, 3, Kind.ARMOR),
                new Entry("chainmail_helmet", 1, 1, 3, Kind.ARMOR),
                new Entry("chainmail_chestplate", 1, 1, 2, Kind.ARMOR),
                new Entry("chainmail_leggings", 1, 1, 2, Kind.ARMOR),
                new Entry("chainmail_boots", 1, 1, 3, Kind.ARMOR),
                new Entry("golden_helmet", 1, 1, 2, Kind.ARMOR),
                new Entry("iron_helmet", 1, 1, 2, Kind.ARMOR),
                new Entry("iron_boots", 1, 1, 2, Kind.ARMOR),
                new Entry("iron_leggings", 1, 1, 1, Kind.ARMOR),
                new Entry("planks", 12, 24, 12, Kind.BLOCKS),
                new Entry("stone", 12, 20, 9, Kind.BLOCKS),
                new Entry("bow", 1, 1, 4, Kind.BOW),
                new Entry("arrow", 4, 10, 6, Kind.ARROWS),
                new Entry("bread", 2, 4, 7, Kind.FOOD),
                new Entry("cooked_beef", 2, 3, 5, Kind.FOOD),
                new Entry("baked_potato", 3, 5, 5, Kind.FOOD),
                new Entry("apple", 2, 3, 3, Kind.FOOD),
                new Entry("potion_healing", 1, 1, 2, Kind.POTION),
                new Entry("potion_speed", 1, 1, 2, Kind.POTION),
                new Entry("stone_pickaxe", 1, 1, 4, Kind.TOOL),
                new Entry("shears", 1, 1, 1, Kind.TOOL),
                new Entry("snowball", 6, 12, 4, Kind.UTILITY),
                new Entry("water_bucket", 1, 1, 2, Kind.UTILITY),
                new Entry("fishing_rod", 1, 1, 2, Kind.UTILITY),
                new Entry("cobweb", 2, 4, 2, Kind.UTILITY),
                new Entry("ender_pearl", 1, 1, 1, Kind.UTILITY)));

        TABLES.put(Tier.MIDDLE, List.of(
                new Entry("iron_sword", 1, 1, 7, Kind.WEAPON),
                new Entry("iron_sword_sharp", 1, 1, 3, Kind.WEAPON),
                new Entry("diamond_sword", 1, 1, 3, Kind.WEAPON),
                new Entry("iron_axe", 1, 1, 4, Kind.WEAPON),
                new Entry("iron_helmet", 1, 1, 4, Kind.ARMOR),
                new Entry("iron_chestplate", 1, 1, 3, Kind.ARMOR),
                new Entry("iron_chestplate_prot", 1, 1, 1, Kind.ARMOR),
                new Entry("iron_leggings", 1, 1, 3, Kind.ARMOR),
                new Entry("iron_boots", 1, 1, 4, Kind.ARMOR),
                new Entry("diamond_helmet", 1, 1, 2, Kind.ARMOR),
                new Entry("diamond_chestplate", 1, 1, 1, Kind.ARMOR),
                new Entry("diamond_leggings", 1, 1, 1, Kind.ARMOR),
                new Entry("diamond_boots", 1, 1, 2, Kind.ARMOR),
                new Entry("shield", 1, 1, 2, Kind.ARMOR),
                new Entry("planks", 20, 32, 5, Kind.BLOCKS),
                new Entry("stone", 16, 32, 4, Kind.BLOCKS),
                new Entry("bow_power", 1, 1, 4, Kind.BOW),
                new Entry("bow", 1, 1, 2, Kind.BOW),
                new Entry("arrow", 8, 16, 6, Kind.ARROWS),
                new Entry("cooked_beef", 3, 5, 4, Kind.FOOD),
                new Entry("golden_carrot", 2, 4, 3, Kind.FOOD),
                new Entry("potion_healing", 1, 1, 3, Kind.POTION),
                new Entry("splash_healing", 1, 2, 3, Kind.POTION),
                new Entry("potion_regeneration", 1, 1, 2, Kind.POTION),
                new Entry("potion_strength", 1, 1, 1, Kind.POTION),
                new Entry("potion_fire", 1, 1, 1, Kind.POTION),
                new Entry("ender_pearl", 1, 2, 4, Kind.UTILITY),
                new Entry("golden_apple", 1, 2, 4, Kind.UTILITY),
                new Entry("water_bucket", 1, 1, 2, Kind.UTILITY),
                new Entry("iron_pickaxe", 1, 1, 2, Kind.TOOL)));

        TABLES.put(Tier.REFILL, List.of(
                new Entry("iron_sword", 1, 1, 4, Kind.WEAPON),
                new Entry("stone_axe", 1, 1, 3, Kind.WEAPON),
                new Entry("iron_helmet", 1, 1, 3, Kind.ARMOR),
                new Entry("iron_boots", 1, 1, 3, Kind.ARMOR),
                new Entry("chainmail_chestplate", 1, 1, 3, Kind.ARMOR),
                new Entry("chainmail_leggings", 1, 1, 3, Kind.ARMOR),
                new Entry("planks", 12, 24, 8, Kind.BLOCKS),
                new Entry("stone", 12, 24, 6, Kind.BLOCKS),
                new Entry("bow", 1, 1, 3, Kind.BOW),
                new Entry("arrow", 6, 12, 6, Kind.ARROWS),
                new Entry("cooked_beef", 2, 4, 6, Kind.FOOD),
                new Entry("golden_carrot", 2, 3, 3, Kind.FOOD),
                new Entry("potion_healing", 1, 1, 3, Kind.POTION),
                new Entry("potion_speed", 1, 1, 2, Kind.POTION),
                new Entry("snowball", 8, 16, 4, Kind.UTILITY),
                new Entry("ender_pearl", 1, 1, 2, Kind.UTILITY),
                new Entry("golden_apple", 1, 1, 2, Kind.UTILITY),
                new Entry("water_bucket", 1, 1, 2, Kind.UTILITY)));
    }

    private Loot() {
    }

    public static List<Entry> table(Tier tier) {
        return TABLES.get(tier);
    }

    public static Kind kind(String id) {
        for (List<Entry> t : TABLES.values()) {
            for (Entry e : t) {
                if (e.id().equals(id)) {
                    return e.kind();
                }
            }
        }
        return null;
    }

    /** Zufällige Ziehungen pro Truhe (zusätzlich zu dem, was jede Insel sicher bekommt). */
    static int rolls(Random r, Tier tier) {
        return switch (tier) {
            case ISLAND -> 3 + r.nextInt(3);
            case MIDDLE -> 5 + r.nextInt(3);
            case REFILL -> 3 + r.nextInt(3);
        };
    }

    private static Stack roll(Random r, Entry e) {
        return new Stack(e.id(), e.min() + r.nextInt(e.max() - e.min() + 1));
    }

    private static Entry pick(Random r, List<Entry> table, Set<Kind> only) {
        int total = 0;
        for (Entry e : table) {
            if (only == null || only.contains(e.kind())) {
                total += e.weight();
            }
        }
        int n = r.nextInt(total);
        for (Entry e : table) {
            if (only != null && !only.contains(e.kind())) {
                continue;
            }
            n -= e.weight();
            if (n < 0) {
                return e;
            }
        }
        throw new IllegalStateException("leere Tabelle");
    }

    /**
     * Truhen einer Insel füllen.
     *
     * @param chests  Zahl der Truhen der Insel (mindestens 1)
     * @param fair    true für Startinseln: über alle Truhen zusammen mindestens Waffe, Rüstung, {@link #MIN_BLOCKS}
     *                Blöcke und Essen
     * @return je Truhe: Platz (0 … 26) → Stapel
     */
    public static List<Map<Integer, Stack>> fill(Random r, int chests, Tier tier, boolean fair) {
        List<List<Stack>> content = new ArrayList<>();
        for (int i = 0; i < chests; i++) {
            content.add(new ArrayList<>());
        }
        List<Entry> table = table(tier);
        if (fair) {
            List<Stack> sure = new ArrayList<>();
            sure.add(roll(r, pick(r, table, Set.of(Kind.WEAPON))));
            sure.add(roll(r, pick(r, table, Set.of(Kind.ARMOR))));
            sure.add(roll(r, pick(r, table, Set.of(Kind.FOOD))));
            int blocks = 0;
            while (blocks < MIN_BLOCKS) {
                Stack s = roll(r, pick(r, table, Set.of(Kind.BLOCKS)));
                blocks += s.amount();
                sure.add(s);
            }
            for (int i = 0; i < sure.size(); i++) {
                // Reihum verteilen, Beginn zufällig: mal liegt das Schwert in der einen, mal in der anderen Truhe.
                content.get((i + r.nextInt(chests)) % chests).add(sure.get(i));
            }
        }
        for (List<Stack> c : content) {
            int n = rolls(r, tier);
            for (int i = 0; i < n; i++) {
                c.add(roll(r, pick(r, table, null)));
            }
        }
        List<Map<Integer, Stack>> out = new ArrayList<>();
        for (List<Stack> c : content) {
            out.add(place(r, c));
        }
        return out;
    }

    /** Stapel auf zufällige, verschiedene Plätze legen. */
    static Map<Integer, Stack> place(Random r, List<Stack> stacks) {
        List<Integer> free = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            free.add(i);
        }
        Collections.shuffle(free, r);
        Map<Integer, Stack> out = new TreeMap<>();
        for (int i = 0; i < stacks.size() && i < SLOTS; i++) {
            out.put(free.get(i), stacks.get(i));
        }
        return out;
    }
}

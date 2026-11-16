package de.moinmorni.wolkenkampf.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

class LootTest {

    private static final int SEEDS = 2000;

    private static boolean has(List<Map<Integer, Loot.Stack>> chests, Loot.Kind kind) {
        return chests.stream().flatMap(c -> c.values().stream()).anyMatch(s -> Loot.kind(s.id()) == kind);
    }

    private static int blocks(List<Map<Integer, Loot.Stack>> chests) {
        return chests.stream().flatMap(c -> c.values().stream()).filter(s -> Loot.kind(s.id()) == Loot.Kind.BLOCKS)
                .mapToInt(Loot.Stack::amount).sum();
    }

    @Test
    void everyStartIslandGetsAWeaponArmourFoodAndEnoughBlocks() {
        for (int seed = 0; seed < SEEDS; seed++) {
            List<Map<Integer, Loot.Stack>> island = Loot.fill(new Random(seed), 2, Loot.Tier.ISLAND, true);
            assertEquals(2, island.size());
            assertTrue(has(island, Loot.Kind.WEAPON), "Waffe, Startwert " + seed);
            assertTrue(has(island, Loot.Kind.ARMOR), "Rüstung, Startwert " + seed);
            assertTrue(has(island, Loot.Kind.FOOD), "Essen, Startwert " + seed);
            assertTrue(blocks(island) >= Loot.MIN_BLOCKS, "Blöcke " + blocks(island) + ", Startwert " + seed);
        }
    }

    @Test
    void slotsAreDistinctAndInsideTheChestAndAmountsInRange() {
        for (Loot.Tier tier : Loot.Tier.values()) {
            for (int seed = 0; seed < SEEDS; seed++) {
                for (Map<Integer, Loot.Stack> chest : Loot.fill(new Random(seed), 3, tier, tier == Loot.Tier.ISLAND)) {
                    assertFalse(chest.isEmpty(), "keine leere Truhe");
                    for (var e : chest.entrySet()) {
                        assertTrue(e.getKey() >= 0 && e.getKey() < Loot.SLOTS, "Platz " + e.getKey());
                        Loot.Stack s = e.getValue();
                        assertTrue(s.amount() >= 1 && s.amount() <= 64, s.toString());
                        assertNotNull(Loot.ITEMS.get(s.id()), "unbekanntes Ding " + s.id());
                    }
                }
            }
        }
    }

    @Test
    void theMiddleIsBetterThanTheIslands() {
        int islandDiamonds = 0;
        int middleDiamonds = 0;
        int middlePearls = 0;
        for (int seed = 0; seed < SEEDS; seed++) {
            for (var c : Loot.fill(new Random(seed), 2, Loot.Tier.ISLAND, true)) {
                islandDiamonds += (int) c.values().stream().filter(s -> s.id().startsWith("diamond")).count();
            }
            for (var c : Loot.fill(new Random(seed), 6, Loot.Tier.MIDDLE, false)) {
                middleDiamonds += (int) c.values().stream().filter(s -> s.id().startsWith("diamond")).count();
                middlePearls += (int) c.values().stream().filter(s -> s.id().equals("ender_pearl")).count();
            }
        }
        assertEquals(0, islandDiamonds, "Diamant gibt es nur in der Mitte");
        assertTrue(middleDiamonds > SEEDS, "Mitte: " + middleDiamonds + " Diamant-Teile");
        assertTrue(middlePearls > SEEDS / 2, "Mitte: " + middlePearls + " Perlen");
    }

    @Test
    void sameSeedSameChests() {
        assertEquals(Loot.fill(new Random(42), 2, Loot.Tier.ISLAND, true), Loot.fill(new Random(42), 2, Loot.Tier.ISLAND, true));
    }

    @Test
    void everyTableEntryIsSensible() {
        for (Loot.Tier tier : Loot.Tier.values()) {
            List<Loot.Entry> table = Loot.table(tier);
            assertFalse(table.isEmpty());
            for (Loot.Entry e : table) {
                assertTrue(e.min() >= 1 && e.max() >= e.min() && e.max() <= 64, e.toString());
                assertTrue(e.weight() > 0, e.toString());
                assertNotNull(Loot.ITEMS.get(e.id()), e.id());
            }
            assertTrue(table.stream().anyMatch(e -> e.kind() == Loot.Kind.WEAPON), tier + " ohne Waffe");
            assertTrue(table.stream().anyMatch(e -> e.kind() == Loot.Kind.BLOCKS), tier + " ohne Blöcke");
        }
    }

    @Test
    void startKitsAreFreeAndModest() {
        for (StartKit k : StartKit.values()) {
            assertEquals(k, StartKit.byId(k.id()));
            assertTrue(k.items().size() <= 2, k.id());
            for (Loot.Stack s : k.items()) {
                assertNotNull(Loot.ITEMS.get(s.id()), s.id());
                assertFalse(s.id().contains("diamond") || s.id().equals("ender_pearl") || s.id().equals("golden_apple"),
                        k.id() + ": " + s.id() + " wäre zu stark");
            }
        }
        assertEquals(StartKit.BERGFUEHRER, StartKit.byId("Bergführer"));
        assertEquals(StartKit.JAEGER, StartKit.byId("jäger"));
        assertEquals(StartKit.WANDERER, StartKit.byId("ohne"));
        assertEquals(null, StartKit.byId("drache"));
    }
}

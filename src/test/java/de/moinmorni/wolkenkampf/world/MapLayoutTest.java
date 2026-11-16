package de.moinmorni.wolkenkampf.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.moinmorni.wolkenkampf.logic.MapLayout;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

/** Aufbau beider Karten ohne Server: Abstände, Truhen, Gondeln, Nebel, Rand – und gerecht für alle Startinseln. */
class MapLayoutTest {

    @Test
    void twelveStartIslandsThatNeedABridge() {
        for (MapType m : MapType.values()) {
            MapLayout l = m.layout();
            assertEquals(12, l.starts().size(), m.id());
            List<MapLayout.Island> s = l.starts();
            for (int i = 0; i < s.size(); i++) {
                MapLayout.Island a = s.get(i);
                MapLayout.Island b = s.get((i + 1) % s.size());
                double gap = a.gap(b);
                assertTrue(gap >= 6, m.id() + ": Nachbarn " + i + " zu nah (" + gap + ")");
                assertTrue(gap <= 14, m.id() + ": Nachbarn " + i + " zu weit (" + gap + ")");
            }
        }
    }

    @Test
    void everyStartIslandIsEquallyFarFromTheMiddle() {
        for (MapType m : MapType.values()) {
            MapLayout l = m.layout();
            double min = Double.MAX_VALUE;
            double max = 0;
            for (MapLayout.Island is : l.starts()) {
                double g = is.gap(l.middle());
                min = Math.min(min, g);
                max = Math.max(max, g);
                assertTrue(g <= 30, m.id() + ": Mitte zu weit (" + g + " Blöcke Brücke)");
            }
            assertTrue(max - min <= 1.5, m.id() + ": Abstand zur Mitte " + min + " … " + max);
            int lowest = l.starts().stream().mapToInt(MapLayout.Island::top).min().orElse(0);
            int highest = l.starts().stream().mapToInt(MapLayout.Island::top).max().orElse(0);
            assertTrue(highest - lowest <= 1, m.id() + ": Höhen unterschiedlich");
        }
    }

    @Test
    void islandsNeverOverlap() {
        for (MapType m : MapType.values()) {
            List<MapLayout.Island> all = m.layout().all();
            for (int i = 0; i < all.size(); i++) {
                for (int j = i + 1; j < all.size(); j++) {
                    assertTrue(all.get(i).gap(all.get(j)) >= 4, m.id() + ": Inseln " + i + "/" + j + " überlappen");
                }
            }
        }
    }

    @Test
    void chestsAreOnTheIslandAndTheLandingSpotStaysFree() {
        for (MapType m : MapType.values()) {
            MapLayout l = m.layout();
            Set<String> seen = new HashSet<>();
            for (MapLayout.Island is : l.all()) {
                assertFalse(is.chests().isEmpty(), m.id() + ": Insel ohne Truhe");
                for (MapLayout.Spot c : is.chests()) {
                    assertTrue(seen.add(c.x() + "," + c.y() + "," + c.z()), m.id() + ": Truhe doppelt " + c);
                    double d = Math.hypot(c.x() - is.x(), c.z() - is.z());
                    assertTrue(d <= is.radius() - 1.3, m.id() + ": Truhe am Rand " + c + " (" + d + ")");
                    assertEquals(is.top() + 1, c.y(), m.id() + ": Truhe nicht auf dem Boden " + c);
                    assertTrue(List.of("north", "south", "east", "west").contains(c.facing()));
                }
            }
            for (MapLayout.Island is : l.starts()) {
                assertEquals(2, is.chests().size());
                for (MapLayout.Spot c : is.chests()) {
                    assertTrue(Math.abs(c.x() - is.x()) > 1 || Math.abs(c.z() - is.z()) > 1,
                            m.id() + ": Truhe auf dem Landeplatz " + c);
                }
            }
            assertTrue(l.middle().chests().size() >= 4, m.id() + ": Mitte braucht mehr Truhen");
        }
    }

    @Test
    void fogVoidBorderAndGondolasFitTogether() {
        for (MapType m : MapType.values()) {
            MapLayout l = m.layout();
            int deepest = l.all().stream().mapToInt(i -> i.top() - i.depth()).min().orElse(0);
            int highest = l.all().stream().mapToInt(MapLayout.Island::top).max().orElse(0);
            assertTrue(l.voidY() < deepest - 10, m.id() + ": Absturzgrenze zu nah an den Inseln");
            assertTrue(l.fogBottom() < deepest, m.id() + ": Nebel beginnt nicht unter den Inseln");
            assertTrue(l.fogTop() > highest + 3, m.id() + ": Nebel steigt nicht über die Inseln");
            for (MapLayout.Island is : l.starts()) {
                assertTrue(!l.outside(is.x() + is.radius(), is.z()) && !l.outside(is.x(), is.z() + is.radius()),
                        m.id() + ": Startinsel außerhalb des Randes");
                assertTrue(is.gondolaY() + Gondola.height() < l.buildTop() + 20, m.id() + ": Gondel zu hoch");
            }
            int[] box = m.playBox();
            assertTrue(box[1] <= l.voidY() && box[4] >= l.buildTop(), m.id() + ": Wiederherstellungs-Box zu klein");
        }
    }

    @Test
    void namesOfMaterialsExist() {
        for (MapType m : MapType.values()) {
            assertNotNull(Material.matchMaterial(m.planks()), m.planks());
            assertNotNull(Material.matchMaterial(m.stone()), m.stone());
            assertNotNull(Material.matchMaterial(m.icon()), m.icon());
        }
        for (String item : de.moinmorni.wolkenkampf.logic.Loot.ITEMS.values()) {
            if (!item.equals("planks") && !item.equals("stone")) {
                assertNotNull(Material.matchMaterial(item), item);
            }
        }
        for (var k : de.moinmorni.wolkenkampf.logic.StartKit.values()) {
            assertNotNull(Material.matchMaterial(k.icon()), k.icon());
        }
        Set<String> dyes = new HashSet<>();
        for (var c : de.moinmorni.wolkenkampf.logic.IslandColour.values()) {
            assertTrue(dyes.add(c.dye()), "Farbe doppelt: " + c.dye());
            assertNotNull(Material.matchMaterial(c.dye() + "_wool"), c.dye());
            assertNotNull(Material.matchMaterial(c.stripe() + "_wool"), c.stripe());
            assertNotNull(Material.matchMaterial(c.band() + "_wool"), c.band());
            assertNotNull(Material.matchMaterial(c.dye() + "_banner"), c.dye());
        }
    }
}

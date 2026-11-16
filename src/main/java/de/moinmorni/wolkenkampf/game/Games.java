package de.moinmorni.wolkenkampf.game;

import de.moinmorni.wolkenkampf.WolkenkampfPlugin;
import de.moinmorni.wolkenkampf.logic.Mode;
import de.moinmorni.wolkenkampf.world.MapType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.entity.Player;

/** Alle laufenden Runden (höchstens eine je Karte). */
public final class Games {

    private final WolkenkampfPlugin plugin;
    private final Map<MapType, Game> running = new EnumMap<>(MapType.class);

    public Games(WolkenkampfPlugin plugin) {
        this.plugin = plugin;
    }

    public Game start(MapType map, Mode mode, List<List<Player>> teams) {
        Game g = new Game(plugin, map, mode, teams);
        running.put(map, g);
        plugin.stateChanged();
        return g;
    }

    public Collection<Game> all() {
        return running.values();
    }

    public Game on(MapType map) {
        return running.get(map);
    }

    /** Karten ohne laufende Runde, die gerade nicht wiederhergestellt werden. */
    public Set<MapType> free() {
        Set<MapType> out = EnumSet.allOf(MapType.class);
        out.removeAll(running.keySet());
        out.removeIf(m -> plugin.construction().restoring(m));
        return out;
    }

    /** Runde, in der jemand mitspielt (auch ausgeschieden oder gerade nicht verbunden) oder zuschaut. */
    public Game of(UUID uuid) {
        for (Game g : running.values()) {
            if (g.fighter(uuid) != null || g.spectating(uuid)) {
                return g;
            }
        }
        return null;
    }

    /** Runde, in der jemand noch kämpft. */
    public Game fighting(UUID uuid) {
        for (Game g : running.values()) {
            Fighter f = g.fighter(uuid);
            if (f != null && f.alive()) {
                return g;
            }
        }
        return null;
    }

    public void tick() {
        for (Game g : new ArrayList<>(running.values())) {
            boolean alive;
            try {
                alive = g.tick();
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.SEVERE, "Fehler in der Runde " + g.map().id() + " – sie wird beendet", e);
                g.close();
                alive = false;
            }
            if (!alive) {
                running.remove(g.map());
                plugin.stateChanged();
                plugin.boards().refreshHall();
            }
        }
    }

    public void abortAll() {
        for (Game g : new ArrayList<>(running.values())) {
            g.close();
        }
        running.clear();
    }

    /** Leute in Runden (kämpfend). */
    public int playing() {
        return running.values().stream().mapToInt(Game::aliveCount).sum();
    }
}

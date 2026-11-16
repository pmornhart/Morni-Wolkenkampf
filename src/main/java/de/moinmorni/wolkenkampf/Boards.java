package de.moinmorni.wolkenkampf;

import de.moinmorni.wolkenkampf.data.Leaderboard;
import de.moinmorni.wolkenkampf.game.Fighter;
import de.moinmorni.wolkenkampf.game.Game;
import de.moinmorni.wolkenkampf.logic.Mode;
import de.moinmorni.wolkenkampf.logic.StartKit;
import de.moinmorni.wolkenkampf.world.HallPlan;
import de.moinmorni.wolkenkampf.world.MapType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Leiste rechts (Runde und Wolkenhafen) und die schwebenden Tafeln im Wolkenhafen. */
public final class Boards {

    private static final int TOP_ROWS = 8;

    private final WolkenkampfPlugin plugin;
    private final Sidebar sidebar = new Sidebar();
    private final List<Holo> hallHolos = new ArrayList<>();

    Boards(WolkenkampfPlugin plugin) {
        this.plugin = plugin;
    }

    public Sidebar sidebar() {
        return sidebar;
    }

    /** Leiste für alle, die eine Runde sehen. */
    public void game(Game g) {
        for (Player p : g.audience()) {
            String lang = plugin.lang(p);
            List<Component> lines = new ArrayList<>();
            lines.add(Component.empty());
            lines.add(Texts.text(lang, "side.map", Placeholder.unparsed("map", Texts.plain(lang, "map." + g.map().id()))));
            lines.add(Texts.text(lang, "side.mode", Placeholder.unparsed("mode", Texts.plain(lang, "mode." + g.mode().id()))));
            if (g.phase() == Game.Phase.GONDOLA) {
                lines.add(Texts.text(lang, "side.gondola"));
            } else {
                lines.add(Texts.text(lang, "side.time", Placeholder.unparsed("time", Ui.clock(g.seconds()))));
                String next = g.nextEventKey();
                if (next != null) {
                    lines.add(Texts.text(lang, next, Placeholder.unparsed("time", Ui.clock(g.untilNext()))));
                } else if (g.phase() == Game.Phase.FIGHT) {
                    lines.add(Texts.text(lang, "side.fog"));
                }
            }
            lines.add(Component.text(" "));
            lines.add(Texts.text(lang, g.mode() == Mode.DUO ? "side.alive.duo" : "side.alive",
                    Placeholder.unparsed("n", String.valueOf(g.aliveCount())),
                    Placeholder.unparsed("teams", String.valueOf(g.aliveTeams()))));
            Fighter f = g.fighter(p.getUniqueId());
            if (f != null) {
                lines.add(Texts.text(lang, "side.kills", Placeholder.unparsed("n", String.valueOf(f.kills()))));
                var c = g.team(f).colour();
                lines.add(Texts.text(lang, "side.island", Placeholder.component("colour",
                        Component.text(Texts.plain(lang, "colour." + c.id()), TextColor.color(c.rgb())))));
                if (!f.alive()) {
                    lines.add(Texts.text(lang, "side.out"));
                }
            } else {
                lines.add(Texts.text(lang, "side.spectating"));
            }
            lines.add(Component.text("  "));
            lines.add(Texts.text(lang, "side.site"));
            sidebar.show(p, Texts.text(lang, "side.title"), lines);
        }
    }

    /** Leiste im Wolkenhafen. */
    public void hall(Player p) {
        String lang = plugin.lang(p);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());
        lines.add(Texts.text(lang, "side.hall.players", Placeholder.unparsed("n", String.valueOf(plugin.hall().waiting().size()))));
        int cd = plugin.hall().countdown();
        lines.add(cd >= 0 ? Texts.text(lang, "side.hall.start", Placeholder.unparsed("secs", String.valueOf(cd)))
                : Texts.text(lang, "side.hall.wait"));
        lines.add(Component.text(" "));
        MapType next = plugin.hall().nextMap();
        lines.add(Texts.text(lang, "side.map", Placeholder.unparsed("map",
                next == null ? "–" : Texts.plain(lang, "map." + next.id()))));
        lines.add(Texts.text(lang, "side.mode", Placeholder.unparsed("mode",
                Texts.plain(lang, "mode." + plugin.hall().nextMode().id()))));
        StartKit kit = plugin.hall().kit(p);
        lines.add(Texts.text(lang, "side.kit", Placeholder.unparsed("kit", Texts.plain(lang, "kit." + kit.id()))));
        lines.add(Component.text("  "));
        int wins = plugin.store().loaded(p.getUniqueId()).map(d -> d.wins()).orElse(0);
        lines.add(Texts.text(lang, "side.hall.wins", Placeholder.unparsed("n", String.valueOf(wins))));
        lines.add(Component.text("   "));
        lines.add(Texts.text(lang, "side.site"));
        sidebar.show(p, Texts.text(lang, "side.title"), lines);
    }

    public void hallAll() {
        for (UUID u : plugin.hall().waiting()) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                hall(p);
            }
        }
    }

    // ---------- Tafeln im Wolkenhafen ----------

    public void spawnHall() {
        removeHall();
        MapType[] maps = MapType.values();
        for (int i = 0; i < maps.length && i < HallPlan.TABLES.length; i++) {
            MapType m = maps[i];
            int[] t = HallPlan.TABLES[i];
            hallHolos.add(Holo.spawn(plugin, at(t[0] + 0.5, 3.6, t[1] + 0.5), lang -> table(lang, m), 1f));
        }
        hallHolos.add(Holo.spawn(plugin, at(HallPlan.BOARD[0] + 0.5, 2.8, HallPlan.BOARD[1] + 0.25), this::top, 0.9f));
        hallHolos.add(Holo.spawn(plugin, at(HallPlan.KITS[0] - 1 + 0.5, 5.6, HallPlan.KITS[1] + 0.5),
                lang -> Texts.text(lang, "board.kits"), 1f));
        hallHolos.add(Holo.spawn(plugin, at(HallPlan.MODES[0] + 0.5, 4.6, HallPlan.MODES[1] + 0.5), this::modes, 1f));
        hallHolos.add(Holo.spawn(plugin, at(0.5, 4.2, 6.5), lang -> Texts.text(lang, "board.welcome"), 1.1f));
    }

    private Location at(double x, double y, double z) {
        return new Location(plugin.world(), x, MapType.FLOOR_Y + y, z);
    }

    private Component table(String lang, MapType m) {
        Game g = plugin.games().on(m);
        String state = g != null ? Texts.raw(lang, "board.table.running")
                : Texts.raw(lang, "board.table.votes").replace("<n>", String.valueOf(plugin.hall().mapVotes().count(m)));
        return Texts.text(lang, "board.table", Placeholder.unparsed("map", Texts.plain(lang, "map." + m.id())),
                Placeholder.unparsed("desc", Texts.plain(lang, "map." + m.id() + ".short")),
                Placeholder.parsed("state", state));
    }

    private Component modes(String lang) {
        return Texts.text(lang, "board.modes",
                Placeholder.unparsed("solo", String.valueOf(plugin.hall().modeVotes().count(Mode.SOLO))),
                Placeholder.unparsed("duo", String.valueOf(plugin.hall().modeVotes().count(Mode.DUO))));
    }

    private Component top(String lang) {
        Component c = Texts.text(lang, "board.title");
        List<Leaderboard.Entry> top = plugin.board().top(TOP_ROWS);
        if (top.isEmpty()) {
            c = c.append(Component.newline()).append(Texts.text(lang, "top.empty"));
        }
        for (int i = 0; i < top.size(); i++) {
            Leaderboard.Entry e = top.get(i);
            c = c.append(Component.newline()).append(Texts.text(lang, "top.row", Placeholder.unparsed("pos", String.valueOf(i + 1)),
                    Placeholder.unparsed("name", e.name()), Placeholder.unparsed("wins", String.valueOf(e.wins())),
                    Placeholder.unparsed("kills", String.valueOf(e.kills()))));
        }
        return c;
    }

    public void refreshHall() {
        if (hallHolos.isEmpty()) {
            return;
        }
        if (!hallHolos.stream().allMatch(Holo::valid)) {
            spawnHall();
            return;
        }
        MapType[] maps = MapType.values();
        for (int i = 0; i < maps.length; i++) {
            MapType m = maps[i];
            hallHolos.get(i).update(lang -> table(lang, m));
        }
        hallHolos.get(maps.length).update(this::top);
        hallHolos.get(maps.length + 2).update(this::modes);
    }

    public void removeHall() {
        hallHolos.forEach(Holo::remove);
        hallHolos.clear();
    }
}

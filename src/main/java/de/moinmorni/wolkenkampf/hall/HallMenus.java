package de.moinmorni.wolkenkampf.hall;

import de.moinmorni.wolkenkampf.Items;
import de.moinmorni.wolkenkampf.Texts;
import de.moinmorni.wolkenkampf.WolkenkampfPlugin;
import de.moinmorni.wolkenkampf.data.Leaderboard;
import de.moinmorni.wolkenkampf.data.PlayerData;
import de.moinmorni.wolkenkampf.game.Game;
import de.moinmorni.wolkenkampf.logic.Mode;
import de.moinmorni.wolkenkampf.logic.StartKit;
import de.moinmorni.wolkenkampf.menu.Menu;
import de.moinmorni.wolkenkampf.world.MapType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Truhen-Menüs des Wolkenhafens: Abstimmung (Karte, Spielart, zuschauen), Start-Ausrüstung, Statistik. */
public final class HallMenus {

    public static final int[] MAP_SLOTS = {10, 12};
    public static final int[] MODE_SLOTS = {14, 16};
    public static final int CLOSE = 22;
    /** Plätze der Ausrüstungen (sieben in einer Reihe). */
    public static final int FIRST_KIT = 10;

    private HallMenus() {
    }

    private static Material material(String name) {
        Material m = Material.matchMaterial(name.toUpperCase(Locale.ROOT));
        return m == null ? Material.PAPER : m;
    }

    public static void vote(WolkenkampfPlugin plugin, Player p) {
        String lang = plugin.lang(p);
        Menu menu = new Menu(3, Texts.text(lang, "menu.vote.title"));
        MapType[] maps = MapType.values();
        for (int i = 0; i < maps.length && i < MAP_SLOTS.length; i++) {
            MapType m = maps[i];
            Game running = plugin.games().on(m);
            List<Component> lore = new ArrayList<>(Texts.lines(lang, "map." + m.id() + ".desc"));
            lore.add(Component.empty());
            lore.add(Texts.text(lang, "menu.vote.votes", Placeholder.unparsed("n", String.valueOf(plugin.hall().mapVotes().count(m)))));
            boolean mine = plugin.hall().mapVotes().voteOf(p.getUniqueId()) == m;
            if (running != null) {
                lore.add(Texts.text(lang, "menu.vote.busy", Placeholder.unparsed("n", String.valueOf(running.aliveCount()))));
            } else if (plugin.construction().restoring(m)) {
                lore.add(Texts.text(lang, "menu.vote.cleaning"));
            } else {
                lore.add(Texts.text(lang, mine ? "menu.vote.mine" : "menu.vote.click"));
            }
            menu.set(MAP_SLOTS[i], Items.glow(Items.named(material(m.icon()), 1, Texts.text(lang, "map." + m.id())
                    .color(TextColor.color(Texts.SKY)), lore), mine), q -> {
                        q.closeInventory();
                        Game g = plugin.games().on(m);
                        if (g != null) {
                            plugin.spectate(q, g);
                        } else {
                            plugin.hall().vote(q, m);
                        }
                    });
        }
        Mode[] modes = Mode.values();
        for (int i = 0; i < modes.length && i < MODE_SLOTS.length; i++) {
            Mode m = modes[i];
            boolean mine = plugin.hall().modeVotes().voteOf(p.getUniqueId()) == m;
            List<Component> lore = new ArrayList<>(Texts.lines(lang, "mode." + m.id() + ".desc"));
            lore.add(Component.empty());
            lore.add(Texts.text(lang, "menu.vote.votes", Placeholder.unparsed("n", String.valueOf(plugin.hall().modeVotes().count(m)))));
            lore.add(Texts.text(lang, mine ? "menu.vote.mine" : "menu.vote.click"));
            menu.set(MODE_SLOTS[i], Items.glow(Items.named(m == Mode.SOLO ? Material.IRON_SWORD : Material.SHIELD, m.teamSize(),
                    Texts.text(lang, "mode." + m.id()).color(TextColor.color(Texts.GOLD)), lore), mine), q -> {
                        q.closeInventory();
                        plugin.hall().vote(q, m);
                    });
        }
        menu.set(CLOSE, Items.named(Material.BARRIER, 1, Texts.text(lang, "menu.close"), null), Player::closeInventory);
        menu.open(p);
    }

    public static void kits(WolkenkampfPlugin plugin, Player p) {
        String lang = plugin.lang(p);
        Menu menu = new Menu(3, Texts.text(lang, "menu.kit.title"));
        StartKit current = plugin.hall().kit(p);
        StartKit[] kits = StartKit.values();
        for (int i = 0; i < kits.length; i++) {
            StartKit k = kits[i];
            List<Component> lore = new ArrayList<>(Texts.lines(lang, "kit." + k.id() + ".desc"));
            lore.add(Component.empty());
            lore.add(Texts.text(lang, k == current ? "menu.kit.mine" : "menu.kit.click"));
            menu.set(FIRST_KIT + i, Items.glow(Items.named(material(k.icon()), 1, Texts.text(lang, "kit." + k.id())
                    .color(TextColor.color(Texts.SKY)), lore), k == current), q -> {
                        q.closeInventory();
                        plugin.hall().chooseKit(q, k);
                    });
        }
        menu.set(CLOSE, Items.named(Material.BARRIER, 1, Texts.text(lang, "menu.close"), null), Player::closeInventory);
        menu.open(p);
    }

    public static void stats(WolkenkampfPlugin plugin, Player p) {
        String lang = plugin.lang(p);
        Menu menu = new Menu(3, Texts.text(lang, "menu.stats.title"));
        PlayerData d = plugin.data(p);
        menu.set(11, Items.named(Material.PLAYER_HEAD, 1, Texts.text(lang, "menu.stats.me"), statLines(lang, d)), null);
        List<Component> lore = new ArrayList<>();
        List<Leaderboard.Entry> top = plugin.board().top(10);
        if (top.isEmpty()) {
            lore.add(Texts.text(lang, "top.empty"));
        }
        for (int i = 0; i < top.size(); i++) {
            Leaderboard.Entry e = top.get(i);
            lore.add(Texts.text(lang, "top.row", Placeholder.unparsed("pos", String.valueOf(i + 1)),
                    Placeholder.unparsed("name", e.name()), Placeholder.unparsed("wins", String.valueOf(e.wins())),
                    Placeholder.unparsed("kills", String.valueOf(e.kills()))));
        }
        menu.set(15, Items.named(Material.GOLDEN_HELMET, 1, Texts.text(lang, "menu.stats.top"), lore), null);
        menu.set(CLOSE, Items.named(Material.BARRIER, 1, Texts.text(lang, "menu.close"), null), Player::closeInventory);
        menu.open(p);
    }

    public static List<Component> statLines(String lang, PlayerData d) {
        return Texts.lines(lang, "stats.lines", Placeholder.unparsed("games", String.valueOf(d.games())),
                Placeholder.unparsed("wins", String.valueOf(d.wins())), Placeholder.unparsed("kills", String.valueOf(d.kills())),
                Placeholder.unparsed("deaths", String.valueOf(d.deaths())), Placeholder.unparsed("chests", String.valueOf(d.chests())),
                Placeholder.unparsed("best", String.valueOf(d.bestKills())));
    }
}

package de.moinmorni.wolkenkampf.hall;

import de.moinmorni.wolkenkampf.Holo;
import de.moinmorni.wolkenkampf.Items;
import de.moinmorni.wolkenkampf.Texts;
import de.moinmorni.wolkenkampf.Ui;
import de.moinmorni.wolkenkampf.WolkenkampfPlugin;
import de.moinmorni.wolkenkampf.game.Game;
import de.moinmorni.wolkenkampf.logic.Groups;
import de.moinmorni.wolkenkampf.logic.Mode;
import de.moinmorni.wolkenkampf.logic.StartKit;
import de.moinmorni.wolkenkampf.logic.Teams;
import de.moinmorni.wolkenkampf.logic.Votes;
import de.moinmorni.wolkenkampf.world.HallPlan;
import de.moinmorni.wolkenkampf.world.MapType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Der Wolkenhafen: hier wartet man, stimmt über Karte und Spielart ab, wählt seine Start-Ausrüstung und sagt
 * „bereit“. Ein Countdown startet die nächste Runde mit höchstens zwölf Leuten – Partys bleiben zusammen.
 */
public final class Hall {

    public static final int SLOT_VOTE = 0;
    public static final int SLOT_READY = 1;
    public static final int SLOT_KIT = 2;
    public static final int SLOT_STATS = 4;
    public static final int SLOT_LOBBY = 8;
    private static final int ALL_READY_SECONDS = 5;
    private static final int MIN_PLAYERS = 2;

    private final WolkenkampfPlugin plugin;
    private final Set<UUID> waiting = new LinkedHashSet<>();
    private final Set<UUID> ready = new HashSet<>();
    private final Map<UUID, UUID> leaders = new HashMap<>();
    private final Votes<MapType> mapVotes = new Votes<>(MapType.class);
    private final Votes<Mode> modeVotes = new Votes<>(Mode.class);
    private int countdown = -1;

    public Hall(WolkenkampfPlugin plugin) {
        this.plugin = plugin;
    }

    public Location spawn() {
        return new Location(plugin.world(), HallPlan.SPAWN_X, MapType.FLOOR_Y + HallPlan.SPAWN_Y, HallPlan.SPAWN_Z,
                HallPlan.SPAWN_YAW, 0f);
    }

    public boolean inHall(UUID uuid) {
        return waiting.contains(uuid);
    }

    public Set<UUID> waiting() {
        return waiting;
    }

    public Votes<MapType> mapVotes() {
        return mapVotes;
    }

    public Votes<Mode> modeVotes() {
        return modeVotes;
    }

    public int countdown() {
        return countdown;
    }

    public boolean ready(UUID uuid) {
        return ready.contains(uuid);
    }

    /** Karte, die als Nächstes drankäme (null, wenn keine frei ist). */
    public MapType nextMap() {
        return mapVotes.winner(plugin.games().free());
    }

    /** Spielart nach Stimmen: zu zweit nur, wenn mehr dafür als dagegen sind. */
    public Mode nextMode() {
        return modeVotes.count(Mode.DUO) > modeVotes.count(Mode.SOLO) ? Mode.DUO : Mode.SOLO;
    }

    /** Gewählte Start-Ausrüstung (gemerkt in den Spielerdaten). */
    public StartKit kit(Player p) {
        StartKit k = StartKit.byId(plugin.data(p).kit());
        return k == null ? StartKit.WANDERER : k;
    }

    /** In den Wolkenhafen (Betreten, nach der Runde, /verlassen). */
    public void enter(Player p) {
        String lang = plugin.lang(p);
        p.teleport(spawn());
        p.setGameMode(GameMode.ADVENTURE);
        p.setFlying(false);
        p.setAllowFlight(false);
        p.setInvulnerable(false);
        p.clearActivePotionEffects();
        var max = p.getAttribute(Attribute.MAX_HEALTH);
        p.setHealth(max == null ? 20 : max.getValue());
        p.setFoodLevel(20);
        p.setFireTicks(0);
        p.setFallDistance(0);
        p.setExp(0);
        p.setLevel(0);
        p.resetPlayerWeather();
        p.resetPlayerTime();
        items(p, lang);
        waiting.add(p.getUniqueId());
        ready.remove(p.getUniqueId());
        Holo.refresh(plugin, p, lang);
        plugin.boards().sidebar().teams(p, Map.of());
        plugin.boards().hall(p);
        refreshParty(p.getUniqueId());
    }

    public void items(Player p, String lang) {
        PlayerInventory inv = p.getInventory();
        inv.clear();
        inv.setItem(SLOT_VOTE, Items.hallTag(Items.named(Material.COMPASS, 1, Texts.text(lang, "item.vote"),
                Texts.lines(lang, "item.vote.lore")), "vote"));
        inv.setItem(SLOT_READY, readyItem(p, lang));
        inv.setItem(SLOT_KIT, kitItem(p, lang));
        inv.setItem(SLOT_STATS, Items.hallTag(Items.named(Material.GOLD_INGOT, 1, Texts.text(lang, "item.stats"),
                Texts.lines(lang, "item.stats.lore")), "stats"));
        inv.setItem(SLOT_LOBBY, Items.hallTag(Items.named(Material.RED_BED, 1, Texts.text(lang, "item.lobby"),
                Texts.lines(lang, "item.lobby.lore")), "lobby"));
    }

    private ItemStack readyItem(Player p, String lang) {
        boolean r = ready.contains(p.getUniqueId());
        return Items.glow(Items.hallTag(Items.named(r ? Material.LIME_DYE : Material.GRAY_DYE, 1,
                Texts.text(lang, "item.ready"), Texts.lines(lang, "item.ready.lore")), "ready"), r);
    }

    private ItemStack kitItem(Player p, String lang) {
        StartKit k = kit(p);
        return Items.hallTag(Items.named(Material.CHEST, 1, Texts.text(lang, "item.kit"),
                Texts.lines(lang, "item.kit.lore", Placeholder.unparsed("kit", Texts.plain(lang, "kit." + k.id())))), "kit");
    }

    public void leave(UUID uuid) {
        waiting.remove(uuid);
        ready.remove(uuid);
        mapVotes.forget(uuid);
        modeVotes.forget(uuid);
        leaders.remove(uuid);
    }

    public void toggleReady(Player p) {
        if (!waiting.contains(p.getUniqueId())) {
            return;
        }
        String lang = plugin.lang(p);
        if (ready.remove(p.getUniqueId())) {
            p.sendMessage(Texts.msg(lang, "hall.not-ready"));
        } else {
            ready.add(p.getUniqueId());
            p.sendMessage(Texts.msg(lang, waiting.size() < MIN_PLAYERS ? "hall.ready.alone" : "hall.ready"));
            Ui.sound(p, "minecraft:block.note_block.pling", 1.4f);
        }
        p.getInventory().setItem(SLOT_READY, readyItem(p, lang));
    }

    public void vote(Player p, MapType map) {
        mapVotes.vote(p.getUniqueId(), map);
        String lang = plugin.lang(p);
        p.sendMessage(Texts.msg(lang, "hall.voted", Placeholder.unparsed("choice", Texts.plain(lang, "map." + map.id()))));
        Ui.sound(p, "minecraft:item.book.page_turn", 1f);
        plugin.boards().refreshHall();
    }

    public void vote(Player p, Mode mode) {
        modeVotes.vote(p.getUniqueId(), mode);
        String lang = plugin.lang(p);
        p.sendMessage(Texts.msg(lang, "hall.voted", Placeholder.unparsed("choice", Texts.plain(lang, "mode." + mode.id()))));
        Ui.sound(p, "minecraft:item.book.page_turn", 1.2f);
        plugin.boards().refreshHall();
    }

    public void chooseKit(Player p, StartKit kit) {
        plugin.data(p).kit(kit.id());
        plugin.store().save(plugin.data(p));
        String lang = plugin.lang(p);
        p.sendMessage(Texts.msg(lang, "hall.kit", Placeholder.unparsed("kit", Texts.plain(lang, "kit." + kit.id()))));
        Ui.sound(p, "minecraft:item.armor.equip_leather", 1f);
        if (inHall(p.getUniqueId())) {
            p.getInventory().setItem(SLOT_KIT, kitItem(p, lang));
        }
    }

    /** Party-Leitung nachschlagen (kommt auf einem anderen Thread zurück). */
    public void refreshParty(UUID uuid) {
        plugin.net().party(uuid).thenAccept(party -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (party.isPresent()) {
                leaders.put(uuid, party.get().leader());
            } else {
                leaders.remove(uuid);
            }
        }));
    }

    /** Jede Sekunde: Countdown, Start. */
    public void tick() {
        waiting.removeIf(u -> Bukkit.getPlayer(u) == null);
        ready.retainAll(waiting);
        mapVotes.retain(waiting);
        modeVotes.retain(waiting);
        if (!plugin.ready()) {
            return;
        }
        MapType next = nextMap();
        boolean anyReady = !ready.isEmpty();
        boolean allReady = !waiting.isEmpty() && ready.containsAll(waiting);
        if (waiting.size() < MIN_PLAYERS || next == null
                || (!anyReady && waiting.size() < plugin.settings().hallMinPlayers())) {
            countdown = -1;
            for (UUID u : waiting) {
                Player p = Bukkit.getPlayer(u);
                if (p != null) {
                    String lang = plugin.lang(p);
                    p.sendActionBar(next == null ? Texts.text(lang, "hall.no-map")
                            : Texts.text(lang, "hall.waiting", Placeholder.unparsed("n", String.valueOf(waiting.size()))));
                }
            }
            return;
        }
        if (countdown < 0) {
            countdown = plugin.settings().hallCountdown();
        }
        if (allReady && countdown > ALL_READY_SECONDS) {
            countdown = ALL_READY_SECONDS;
        }
        Mode mode = Mode.effective(nextMode(), waiting.size());
        for (UUID u : waiting) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                String lang = plugin.lang(p);
                p.sendActionBar(Texts.text(lang, "hall.countdown", Placeholder.unparsed("secs", String.valueOf(countdown)),
                        Placeholder.unparsed("map", Texts.plain(lang, "map." + next.id())),
                        Placeholder.unparsed("mode", Texts.plain(lang, "mode." + mode.id()))));
                if (countdown <= 3 && countdown > 0) {
                    Ui.sound(p, "minecraft:block.note_block.hat", 1.2f);
                }
            }
        }
        if (countdown-- <= 0) {
            start(next, nextMode());
            countdown = -1;
        }
    }

    /** Runde starten (Countdown abgelaufen oder /wolkenkampf start). null, wenn es nicht geht. */
    public Game start(MapType map, Mode wanted) {
        Map<UUID, List<UUID>> groups = new LinkedHashMap<>();
        for (UUID u : waiting) {
            UUID leader = leaders.getOrDefault(u, u);
            if (!waiting.contains(leader)) {
                leader = u;
            }
            groups.computeIfAbsent(leader, k -> new ArrayList<>()).add(u);
        }
        List<UUID> picked = Groups.pick(new ArrayList<>(groups.values()), plugin.settings().maxPlayers());
        List<List<UUID>> pickedGroups = new ArrayList<>();
        for (List<UUID> g : groups.values()) {
            List<UUID> in = g.stream().filter(picked::contains).filter(u -> Bukkit.getPlayer(u) != null).toList();
            if (!in.isEmpty()) {
                pickedGroups.add(in);
            }
        }
        int count = pickedGroups.stream().mapToInt(List::size).sum();
        if (count < MIN_PLAYERS) {
            return null;
        }
        Mode mode = Mode.effective(wanted, count);
        List<List<UUID>> teams = Teams.form(mode, pickedGroups);
        if (teams.size() < 2) {
            return null;
        }
        List<List<Player>> teamPlayers = new ArrayList<>();
        for (List<UUID> t : teams) {
            List<Player> ps = t.stream().map(Bukkit::getPlayer).toList();
            teamPlayers.add(ps);
            ps.forEach(p -> leave(p.getUniqueId()));
        }
        mapVotes.played(map, System.currentTimeMillis());
        for (List<Player> t : teamPlayers) {
            for (Player p : t) {
                String lang = plugin.lang(p);
                p.sendMessage(Texts.msg(lang, "hall.starting", Placeholder.unparsed("map", Texts.plain(lang, "map." + map.id())),
                        Placeholder.unparsed("mode", Texts.plain(lang, "mode." + mode.id())),
                        Placeholder.unparsed("n", String.valueOf(count))));
            }
        }
        for (UUID u : waiting) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                p.sendMessage(Texts.msg(plugin.lang(p), "hall.full"));
            }
        }
        return plugin.games().start(map, mode, teamPlayers);
    }
}

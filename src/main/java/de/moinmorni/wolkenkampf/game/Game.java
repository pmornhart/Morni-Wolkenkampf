package de.moinmorni.wolkenkampf.game;

import de.moinmorni.wolkenkampf.Settings;
import de.moinmorni.wolkenkampf.Texts;
import de.moinmorni.wolkenkampf.Ui;
import de.moinmorni.wolkenkampf.WolkenkampfPlugin;
import de.moinmorni.wolkenkampf.data.PlayerData;
import de.moinmorni.wolkenkampf.logic.IslandColour;
import de.moinmorni.wolkenkampf.logic.Loot;
import de.moinmorni.wolkenkampf.logic.MapLayout;
import de.moinmorni.wolkenkampf.logic.Mode;
import de.moinmorni.wolkenkampf.logic.Spawns;
import de.moinmorni.wolkenkampf.logic.StartKit;
import de.moinmorni.wolkenkampf.logic.Timeline;
import de.moinmorni.wolkenkampf.world.Gondola;
import de.moinmorni.wolkenkampf.world.MapType;
import de.moinmorni.wolkenkampf.world.Plan;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.WeatherType;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Eine Runde Wolkenkampf auf einer Karte: erst warten alle in ihrer Ballon-Gondel, dann klappt der Boden auf, man
 * schwebt hinunter auf die eigene Insel – Truhen plündern, Brücken bauen, kämpfen. Nach und nach kommen Nachschub,
 * Gewitter und zum Schluss steigt der Nebel. Das letzte Team, das noch steht, gewinnt.
 */
public final class Game {

    public enum Phase {
        GONDOLA, FIGHT, OVER
    }

    /** Warum jemand ausscheidet. */
    public enum Cause {
        KILLED, VOID, FOG, STORM, BORDER, LEFT, OTHER
    }

    public static final String FIREWORK_TAG = "wolkenkampf_feuerwerk";
    private static final int SECOND = 20;
    private static final int RISE_DELAY = 40;
    private static final int RISE_EVERY = 3;
    private static final int RISE_STEPS = 16;
    private static final int STRIKE_WARN = 30;
    private static final double STRIKE_RADIUS = 2.6;
    private static final double STRIKE_DAMAGE = 5;
    private static final double BORDER_DAMAGE = 2;
    private static final int SLOW_FALL_TICKS = 70;
    private static final double SPECTATOR_RANGE = 40;
    /** Höhen der Nebelflächen unter der Nebelgrenze. */
    private static final double[] FOG_LAYERS = {0.0, -1.6};
    private static final Map<IslandColour, List<Object[]>> GONDOLA_BLOCKS = new EnumMap<>(IslandColour.class);

    /** Ein Team: seine Insel und seine Leute. */
    public record Team(int number, int island, IslandColour colour, List<UUID> members) {
    }

    /** Eine Truhe der Karte. */
    private record ChestSpot(Location at, Loot.Tier tier, String key) {
    }

    private static final class Strike {
        final Location at;
        int left;

        Strike(Location at, int left) {
            this.at = at;
            this.left = left;
        }
    }

    private final WolkenkampfPlugin plugin;
    private final MapType map;
    private final Mode mode;
    private final World world;
    private final MapLayout layout;
    private final Settings settings;
    private final Timeline timeline;
    private final Random random = new Random();
    private final Map<UUID, Fighter> fighters = new LinkedHashMap<>();
    private final List<Team> teams = new ArrayList<>();
    private final Set<UUID> spectators = new LinkedHashSet<>();
    private final List<ChestSpot> chests = new ArrayList<>();
    private final List<Strike> strikes = new ArrayList<>();
    private final List<org.bukkit.entity.BlockDisplay> fog = new ArrayList<>();
    private final LangBar bar = new LangBar(BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
    private Phase phase = Phase.GONDOLA;
    private int phaseTicks;
    private int ticks;
    private int seconds;
    private int generation;
    private int refills;
    private int strikeCount;
    private double strikeTimer;
    private double fogLevel;
    private int riseStep = -1;
    private int riseTicks;
    private Team winner;

    public Game(WolkenkampfPlugin plugin, MapType map, Mode mode, List<List<Player>> teamPlayers) {
        this.plugin = plugin;
        this.map = map;
        this.mode = mode;
        this.world = plugin.world();
        this.layout = map.layout();
        this.settings = plugin.settings();
        this.timeline = settings.timeline();
        this.fogLevel = MapType.FLOOR_Y + layout.fogBottom();
        List<Integer> islands = Spawns.pick(teamPlayers.size(), layout.starts().size(),
                random.nextInt(layout.starts().size()));
        for (int i = 0; i < teamPlayers.size(); i++) {
            int island = islands.get(i);
            List<UUID> members = teamPlayers.get(i).stream().map(Player::getUniqueId).toList();
            teams.add(new Team(i, island, IslandColour.of(island), members));
            for (Player p : teamPlayers.get(i)) {
                fighters.put(p.getUniqueId(), new Fighter(p.getUniqueId(), p.getName(), i));
                plugin.store().pin(p.getUniqueId());
            }
        }
        fillChests(false);
        phaseTicks = settings.gondolaSeconds() * SECOND;
        Map<String, Integer> colours = nameColours();
        for (Team t : teams) {
            for (int m = 0; m < t.members().size(); m++) {
                Player p = Bukkit.getPlayer(t.members().get(m));
                if (p == null) {
                    continue;
                }
                board(p);
                plugin.boards().sidebar().teams(p, colours);
                p.teleport(gondolaSpot(t, m));
                prepare(p, GameMode.ADVENTURE);
                String lang = plugin.lang(p);
                p.sendMessage(Texts.msg(lang, "game.welcome", Placeholder.unparsed("map", Texts.plain(lang, "map." + map.id())),
                        Placeholder.unparsed("secs", String.valueOf(settings.gondolaSeconds()))));
                p.sendMessage(Texts.msg(lang, "game.island", colourName(lang, t.colour())));
                if (mode == Mode.DUO && t.members().size() > 1) {
                    p.sendMessage(Texts.msg(lang, "game.mate", Placeholder.unparsed("name", mateNames(t, p.getUniqueId()))));
                }
                Ui.title(p, Texts.text(lang, "game.title"), Texts.text(lang, "game.gondola.sub"), 2500);
            }
        }
        updateBar();
        plugin.boards().game(this);
    }

    // ---------- Zugriff ----------

    public MapType map() {
        return map;
    }

    public Mode mode() {
        return mode;
    }

    public Phase phase() {
        return phase;
    }

    public int seconds() {
        return seconds;
    }

    public Timeline timeline() {
        return timeline;
    }

    public Fighter fighter(UUID uuid) {
        return fighters.get(uuid);
    }

    public Map<UUID, Fighter> fighters() {
        return fighters;
    }

    public List<Team> teams() {
        return teams;
    }

    public boolean spectating(UUID uuid) {
        return spectators.contains(uuid);
    }

    /** Lebende, verbundene Spieler. */
    public int aliveCount() {
        return (int) fighters.values().stream().filter(Fighter::alive).count();
    }

    public int aliveTeams() {
        return (int) teams.stream().filter(this::teamAlive).count();
    }

    private boolean teamAlive(Team t) {
        return t.members().stream().map(fighters::get).anyMatch(f -> f != null && f.alive());
    }

    public Team team(Fighter f) {
        return teams.get(f.team());
    }

    public boolean sameTeam(UUID a, UUID b) {
        Fighter fa = fighters.get(a);
        Fighter fb = fighters.get(b);
        return fa != null && fb != null && fa.team() == fb.team();
    }

    public double fogLevel() {
        return fogLevel;
    }

    /** Nächstes Ereignis in Sekunden (−1: keins mehr). */
    public int untilNext() {
        return phase == Phase.FIGHT ? timeline.untilNext(seconds) : -1;
    }

    /** Ist die Stelle (Weltkoordinaten) in der Spielfläche dieser Karte, in der gebaut werden darf? */
    public boolean buildable(Location l) {
        double x = l.getX() - map.originX();
        double z = l.getZ() - map.originZ();
        double y = l.getY() - MapType.FLOOR_Y;
        return l.getWorld() == world && !layout.outside(x, z) && y <= layout.buildTop() && y > layout.voidY();
    }

    /** Gehört die Stelle (grob) zu dieser Karte? */
    public boolean onMap(Location l) {
        return l.getWorld() == world && Math.abs(l.getX() - map.originX()) < 300 && Math.abs(l.getZ() - map.originZ()) < 300;
    }

    Location loc(double x, double y, double z) {
        return new Location(world, map.originX() + x, MapType.FLOOR_Y + y, map.originZ() + z);
    }

    private Location gondolaSpot(Team t, int member) {
        MapLayout.Island is = layout.starts().get(t.island());
        double offset = t.members().size() > 1 ? (member == 0 ? -0.7 : 0.7) : 0;
        double len = Math.hypot(is.x(), is.z());
        double px = len == 0 ? 1 : -is.z() / len;
        double pz = len == 0 ? 0 : is.x() / len;
        Location l = loc(is.x() + 0.5 + px * offset, is.gondolaY() + 1, is.z() + 0.5 + pz * offset);
        l.setYaw((float) Math.toDegrees(Math.atan2(is.x(), -is.z())));
        l.setPitch(20f);
        return l;
    }

    /** Wo Zuschauer und Ausgeschiedene anfangen: hoch über der Mittelinsel. */
    public Location spectatorSpot() {
        Location l = loc(0.5, layout.middle().top() + 16, 18.5);
        l.setYaw(180f);
        l.setPitch(35f);
        return l;
    }

    /** Für /wolkenkampf status (Tests, Verwaltung). */
    public String status() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, "karte=%s art=%s phase=%s zeit=%d lebend=%d teams_lebend=%d nebel=%.1f blitze=%d "
                + "nachschub=%d gondel=%d", map.id(), mode.id(), phase, seconds, aliveCount(), aliveTeams(), fogLevel,
                strikeCount, refills, riseStep));
        for (Fighter f : fighters.values()) {
            Team t = team(f);
            sb.append(String.format(Locale.ROOT, " %s=%s:t%d:i%d:k%d:c%d:p%d", f.name(), f.state(), t.number(), t.island(),
                    f.kills(), f.chests(), f.place()));
        }
        for (UUID u : spectators) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                sb.append(' ').append(p.getName()).append("=SPECTATOR");
            }
        }
        for (Team t : teams) {
            MapLayout.Island is = layout.starts().get(t.island());
            sb.append(String.format(Locale.ROOT, " team%d=%d:%d:%d:%d:%d", t.number(), t.island(), map.originX() + is.x(),
                    map.originZ() + is.z(), MapType.FLOOR_Y + is.top(), MapType.FLOOR_Y + is.gondolaY()));
        }
        return sb.toString();
    }

    /** Alle, die die Runde sehen: verbundene Kämpfer und Zuschauer. */
    public List<Player> audience() {
        List<Player> out = new ArrayList<>();
        for (Fighter f : fighters.values()) {
            Player p = Bukkit.getPlayer(f.uuid());
            if (p != null && f.online()) {
                out.add(p);
            }
        }
        for (UUID u : spectators) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                out.add(p);
            }
        }
        return out;
    }

    void broadcast(Function<String, Component> msg) {
        for (Player p : audience()) {
            p.sendMessage(msg.apply(plugin.lang(p)));
        }
    }

    private void soundAll(String key, float pitch) {
        for (Player p : audience()) {
            Ui.sound(p, key, pitch);
        }
    }

    private void titleAll(String main, String sub, long stay, TagResolver... r) {
        for (Player p : audience()) {
            String lang = plugin.lang(p);
            Ui.title(p, Texts.text(lang, main, r), Texts.text(lang, sub, r), stay);
        }
    }

    TagResolver colourName(String lang, IslandColour c) {
        return Placeholder.component("colour", Component.text(Texts.plain(lang, "colour." + c.id()), TextColor.color(c.rgb())));
    }

    private String mateNames(Team t, UUID except) {
        return t.members().stream().filter(u -> !u.equals(except)).map(u -> fighters.get(u).name())
                .collect(Collectors.joining(", "));
    }

    private String teamNames(Team t) {
        return t.members().stream().map(u -> fighters.get(u).name()).collect(Collectors.joining(" & "));
    }

    /** Namensfarben aller Kämpfer (Inselfarbe). */
    private Map<String, Integer> nameColours() {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Fighter f : fighters.values()) {
            out.put(f.name(), team(f).colour().rgb());
        }
        return out;
    }

    private void board(Player p) {
        String lang = plugin.lang(p);
        bar.show(p, lang);
        if (timeline.storming(seconds) && phase == Phase.FIGHT) {
            p.setPlayerWeather(WeatherType.DOWNFALL);
        }
    }

    // ---------- Spieler ----------

    private void prepare(Player p, GameMode mode) {
        p.setGameMode(mode);
        p.setFlying(false);
        p.setAllowFlight(false);
        p.setInvulnerable(false);
        p.getInventory().clear();
        p.clearActivePotionEffects();
        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        p.setHealth(max == null ? 20 : max.getValue());
        p.setFoodLevel(20);
        p.setSaturation(8);
        p.setFireTicks(0);
        p.setFallDistance(0);
        p.setExp(0);
        p.setLevel(0);
    }

    /** Wiederkommen nach Verbindungsabbruch: wer ausgeschieden ist (oder die Runde läuft schon), schaut zu. */
    public void rejoin(Player p) {
        Fighter f = fighters.get(p.getUniqueId());
        if (f != null) {
            f.online(true);
        }
        spectate(p);
    }

    /** Verbindung weg: wer noch dabei war, scheidet aus (seine Sachen bleiben liegen). */
    public void quit(Player p) {
        Fighter f = fighters.get(p.getUniqueId());
        spectators.remove(p.getUniqueId());
        bar.hide(p);
        if (f == null) {
            return;
        }
        if (f.alive() && phase != Phase.OVER) {
            eliminate(p, f, Cause.LEFT, null, true);
        }
        f.online(false);
    }

    /** Freiwillig gehen (/verlassen). Wer noch kämpft, scheidet aus; die Runde zählt als gespielt. */
    public void leave(Player p) {
        Fighter f = fighters.get(p.getUniqueId());
        spectators.remove(p.getUniqueId());
        bar.hide(p);
        if (f != null) {
            if (f.alive() && phase != Phase.OVER) {
                eliminate(p, f, Cause.LEFT, null, true);
            }
            f.online(false);
        }
    }

    /** Zuschauen (Ausgeschiedene, Leute aus dem Wolkenhafen, Wiederkommende). */
    public void spectate(Player p) {
        if (fighters.get(p.getUniqueId()) == null) {
            spectators.add(p.getUniqueId());
        }
        prepare(p, GameMode.SPECTATOR);
        p.teleport(spectatorSpot());
        board(p);
        plugin.boards().sidebar().teams(p, nameColours());
        plugin.boards().game(this);
        String lang = plugin.lang(p);
        p.sendMessage(Texts.msg(lang, "spectate.start", Placeholder.unparsed("map", Texts.plain(lang, "map." + map.id()))));
    }

    // ---------- Ablauf ----------

    /** Jeder Tick. false = Runde ist vorbei und kann weg. */
    public boolean tick() {
        ticks++;
        switch (phase) {
            case GONDOLA -> tickGondola();
            case FIGHT -> tickFight();
            case OVER -> {
                if (--phaseTicks <= 0) {
                    close();
                    return false;
                }
            }
        }
        return true;
    }

    private void tickGondola() {
        phaseTicks--;
        if (ticks % 5 == 0) {
            for (Team t : teams) {
                MapLayout.Island is = layout.starts().get(t.island());
                for (int m = 0; m < t.members().size(); m++) {
                    Player p = Bukkit.getPlayer(t.members().get(m));
                    if (p == null || !fighters.get(p.getUniqueId()).alive()) {
                        continue;
                    }
                    Location l = p.getLocation();
                    double dx = l.getX() - (map.originX() + is.x() + 0.5);
                    double dz = l.getZ() - (map.originZ() + is.z() + 0.5);
                    if (Math.hypot(dx, dz) > 1.7 || l.getY() < MapType.FLOOR_Y + is.gondolaY() + 0.5) {
                        Location back = gondolaSpot(t, m);
                        back.setYaw(l.getYaw());
                        back.setPitch(l.getPitch());
                        p.teleport(back);
                    }
                }
            }
        }
        if (phaseTicks % SECOND == 0) {
            int secs = phaseTicks / SECOND;
            if (secs > 0 && secs <= 5) {
                for (Player p : audience()) {
                    String lang = plugin.lang(p);
                    Ui.title(p, Texts.text(lang, "game.count", Placeholder.unparsed("n", String.valueOf(secs))),
                            Texts.text(lang, "game.gondola.sub"), 900);
                    Ui.sound(p, "minecraft:block.note_block.hat", 1.0f + (5 - secs) * 0.1f);
                }
            }
            updateBar();
            plugin.boards().game(this);
        }
        if (phaseTicks <= 0) {
            open();
        }
    }

    /** Die Gondeln öffnen sich: Boden klappt auf, alle schweben auf ihre Insel. */
    private void open() {
        phase = Phase.FIGHT;
        seconds = 0;
        for (MapLayout.Island is : layout.starts()) {
            for (int[] f : Gondola.floor()) {
                Block b = world.getBlockAt(map.originX() + is.x() + f[0], MapType.FLOOR_Y + is.gondolaY(), map.originZ() + is.z() + f[2]);
                if (b.getBlockData() instanceof TrapDoor td) {
                    td.setOpen(true);
                    b.setBlockData(td, false);
                }
            }
            world.playSound(loc(is.x() + 0.5, is.gondolaY() + 1, is.z() + 0.5), "minecraft:block.wooden_trapdoor.open", 1.5f, 0.8f);
        }
        for (Fighter f : fighters.values()) {
            Player p = Bukkit.getPlayer(f.uuid());
            if (p == null || !f.alive()) {
                continue;
            }
            String lang = plugin.lang(p);
            p.setGameMode(GameMode.SURVIVAL);
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, SLOW_FALL_TICKS, 0, false, false, true));
            f.landing(true);
            StartKit kit = kitOf(p);
            for (ItemStack item : LootItems.kit(kit, map)) {
                p.getInventory().addItem(item);
            }
            Ui.title(p, Texts.text(lang, "game.go"), Texts.text(lang, "game.go.sub"), 1800);
            Ui.sound(p, "minecraft:entity.firework_rocket.launch", 1.2f);
            p.sendMessage(Texts.msg(lang, "game.started", Placeholder.unparsed("kit", Texts.plain(lang, "kit." + kit.id()))));
        }
        riseTicks = RISE_DELAY;
        updateBar();
        plugin.stateChanged();
    }

    private StartKit kitOf(Player p) {
        StartKit k = StartKit.byId(plugin.data(p).kit());
        return k == null ? StartKit.WANDERER : k;
    }

    private void tickFight() {
        tickRise();
        for (Fighter f : new ArrayList<>(fighters.values())) {
            if (!f.alive()) {
                continue;
            }
            Player p = Bukkit.getPlayer(f.uuid());
            if (p == null) {
                continue;
            }
            if (p.getLocation().getY() < MapType.FLOOR_Y + layout.voidY()) {
                eliminate(p, f, Cause.VOID, null, false);
                continue;
            }
            if (f.landing() && p.isOnGround() && ticks % 2 == 0) {
                f.landing(false);
            }
        }
        if (phase != Phase.FIGHT) {
            return;
        }
        for (Strike s : new ArrayList<>(strikes)) {
            if (--s.left <= 0) {
                strikes.remove(s);
                strike(s.at);
            } else if (s.left % 5 == 0) {
                world.spawnParticle(Particle.ELECTRIC_SPARK, s.at.clone().add(0, 0.3, 0), 25, 0.8, 0.4, 0.8, 0.05);
                world.spawnParticle(Particle.CLOUD, s.at.clone().add(0, 2.5, 0), 4, 0.5, 0.2, 0.5, 0.01);
            }
        }
        if (ticks % 5 == 0 && timeline.foggy(seconds)) {
            fogParticles();
        }
        if (ticks % SECOND == 0) {
            second();
        }
    }

    /** Einmal pro Sekunde während des Kampfs: Ereignisse, Nebel, Rand, Anzeigen. */
    private void second() {
        seconds++;
        Timeline.Event warn = timeline.warnAt(seconds);
        if (warn != null) {
            broadcast(lang -> Texts.msg(lang, "event." + key(warn) + ".warn",
                    Placeholder.unparsed("secs", String.valueOf(Timeline.WARN))));
            soundAll("minecraft:block.note_block.bell", 0.8f);
        }
        Timeline.Event now = timeline.startsAt(seconds);
        if (now == Timeline.Event.REFILL) {
            refill();
        } else if (now == Timeline.Event.STORM) {
            stormBegins();
        } else if (now == Timeline.Event.FOG) {
            titleAll("event.fog.title", "event.fog.sub", 2500);
            fogPlanes();
            broadcast(lang -> Texts.msg(lang, "event.fog.now"));
            soundAll("minecraft:ambient.basalt_deltas.mood", 0.7f);
        }
        if (timeline.storming(seconds)) {
            strikeTimer += 1;
            if (strikeTimer >= timeline.strikeEvery(seconds)) {
                strikeTimer = 0;
                planStrike();
            }
        }
        fogLevel = timeline.fogLevel(seconds, MapType.FLOOR_Y + layout.fogBottom(), MapType.FLOOR_Y + layout.fogTop());
        moveFog();
        double fogDamage = timeline.fogDamage(seconds);
        for (Fighter f : new ArrayList<>(fighters.values())) {
            Player p = Bukkit.getPlayer(f.uuid());
            if (p == null || !f.alive()) {
                continue;
            }
            Location l = p.getLocation();
            String lang = plugin.lang(p);
            if (fogDamage > 0 && l.getY() < fogLevel + 0.2) {
                p.sendActionBar(Texts.text(lang, "fog.inside"));
                hurt(p, f, fogDamage, Cause.FOG);
                continue;
            }
            if (layout.outside(l.getX() - map.originX(), l.getZ() - map.originZ())) {
                p.sendActionBar(Texts.text(lang, "border.outside"));
                hurt(p, f, BORDER_DAMAGE, Cause.BORDER);
            }
        }
        for (UUID u : spectators) {
            keepNear(Bukkit.getPlayer(u));
        }
        for (Fighter f : fighters.values()) {
            if (!f.alive()) {
                keepNear(Bukkit.getPlayer(f.uuid()));
            }
        }
        updateBar();
        plugin.boards().game(this);
        checkWin();
    }

    private static String key(Timeline.Event e) {
        return switch (e) {
            case REFILL -> "refill";
            case STORM -> "storm";
            case FOG -> "fog";
        };
    }

    /** Zuschauer bleiben in der Nähe der Karte. */
    private void keepNear(Player p) {
        if (p == null || p.getGameMode() != GameMode.SPECTATOR) {
            return;
        }
        Location l = p.getLocation();
        double d = Math.hypot(l.getX() - map.originX(), l.getZ() - map.originZ());
        if (l.getWorld() != world || d > layout.border() + SPECTATOR_RANGE || l.getY() < MapType.FLOOR_Y + layout.voidY() - 20) {
            p.teleport(spectatorSpot());
        }
    }

    /** Schaden durch Nebel, Blitz oder Rand – reicht er, scheidet man aus. */
    private void hurt(Player p, Fighter f, double amount, Cause cause) {
        if (amount >= p.getHealth() + p.getAbsorptionAmount()) {
            eliminate(p, f, cause, null, true);
        } else {
            p.damage(amount);
        }
    }

    // ---------- Ereignisse ----------

    /** Truhen füllen: zu Beginn (gerecht verteilt) und beim Nachschub (alle neu). */
    private void fillChests(boolean refill) {
        chests.clear();
        generation++;
        for (int i = 0; i < layout.starts().size(); i++) {
            fillIsland(layout.starts().get(i), refill ? Loot.Tier.REFILL : Loot.Tier.ISLAND, !refill, "s" + i);
        }
        fillIsland(layout.middle(), Loot.Tier.MIDDLE, false, "m");
        for (int i = 0; i < layout.small().size(); i++) {
            fillIsland(layout.small().get(i), refill ? Loot.Tier.REFILL : Loot.Tier.ISLAND, false, "k" + i);
        }
    }

    private void fillIsland(MapLayout.Island is, Loot.Tier tier, boolean fair, String id) {
        List<Map<Integer, Loot.Stack>> content = Loot.fill(random, is.chests().size(), tier, fair);
        for (int c = 0; c < is.chests().size(); c++) {
            MapLayout.Spot s = is.chests().get(c);
            Location at = loc(s.x(), s.y(), s.z());
            Block b = at.getBlock();
            if (b.getType() != Material.CHEST) {
                b.setBlockData(Plan.data("chest[facing=" + s.facing() + ",type=single,waterlogged=false]"), false);
            }
            if (!(b.getState(false) instanceof org.bukkit.block.Chest chest)) {
                continue;
            }
            Inventory inv = chest.getBlockInventory();
            inv.clear();
            content.get(c).forEach((slot, stack) -> inv.setItem(slot, LootItems.of(stack, map)));
            chests.add(new ChestSpot(at, tier, id + "-" + c));
        }
    }

    /** Wird diese Truhe (Block) von der Runde verwaltet? Liefert ihren Schlüssel für die Statistik, sonst null. */
    public String chestKey(Block b) {
        for (ChestSpot c : chests) {
            if (c.at().getBlockX() == b.getX() && c.at().getBlockY() == b.getY() && c.at().getBlockZ() == b.getZ()) {
                return c.key() + "#" + generation;
            }
        }
        return null;
    }

    /** Jemand öffnet eine Truhe der Karte. */
    public void openedChest(Player p, Block b) {
        Fighter f = fighters.get(p.getUniqueId());
        String key = chestKey(b);
        if (f != null && f.alive() && key != null) {
            f.openChest(key);
        }
    }

    private void refill() {
        refills++;
        fillChests(true);
        for (ChestSpot c : chests) {
            world.spawnParticle(Particle.HAPPY_VILLAGER, c.at().clone().add(0.5, 1.1, 0.5), 12, 0.4, 0.3, 0.4, 0);
        }
        titleAll("event.refill.title", "event.refill.sub", 2200);
        broadcast(lang -> Texts.msg(lang, "event.refill.now"));
        soundAll("minecraft:block.chest.open", 0.8f);
        soundAll("minecraft:entity.player.levelup", 1.4f);
    }

    private void stormBegins() {
        for (Player p : audience()) {
            p.setPlayerWeather(WeatherType.DOWNFALL);
        }
        titleAll("event.storm.title", "event.storm.sub", 2500);
        broadcast(lang -> Texts.msg(lang, "event.storm.now"));
        soundAll("minecraft:entity.lightning_bolt.thunder", 0.9f);
        strikeTimer = 0;
        planStrike();
    }

    /** Nächster Blitz: oft in der Nähe von jemandem, sonst irgendwo auf einer Insel. Vorher knistert es. */
    public void planStrike() {
        List<Player> alive = new ArrayList<>();
        for (Fighter f : fighters.values()) {
            Player p = Bukkit.getPlayer(f.uuid());
            if (p != null && f.alive()) {
                alive.add(p);
            }
        }
        Location at;
        if (!alive.isEmpty() && random.nextInt(100) < 65) {
            Location base = alive.get(random.nextInt(alive.size())).getLocation();
            at = ground(base.getX() + random.nextInt(7) - 3, base.getZ() + random.nextInt(7) - 3, base.getY() + 3);
        } else {
            List<MapLayout.Island> all = layout.all();
            MapLayout.Island is = all.get(random.nextInt(all.size()));
            double a = random.nextDouble() * Math.PI * 2;
            double d = random.nextDouble() * (is.radius() - 1);
            Location l = loc(is.x() + 0.5 + Math.cos(a) * d, is.top() + 12, is.z() + 0.5 + Math.sin(a) * d);
            at = ground(l.getX(), l.getZ(), l.getY());
        }
        if (at != null) {
            strikes.add(new Strike(at, STRIKE_WARN));
            world.playSound(at, "minecraft:block.beacon.power_select", 0.8f, 1.8f);
        }
    }

    /** Erster feste Block unter (x, from, z) – Stelle darüber; null, wenn darunter nichts ist. */
    private Location ground(double x, double z, double from) {
        int top = (int) Math.floor(from);
        int bottom = MapType.FLOOR_Y + layout.voidY();
        for (int y = top; y > bottom; y--) {
            Block b = world.getBlockAt((int) Math.floor(x), y, (int) Math.floor(z));
            if (b.getType().isSolid()) {
                return new Location(world, Math.floor(x) + 0.5, y + 1, Math.floor(z) + 0.5);
            }
        }
        return null;
    }

    private void strike(Location at) {
        strikeCount++;
        world.strikeLightningEffect(at);
        for (Fighter f : new ArrayList<>(fighters.values())) {
            Player p = Bukkit.getPlayer(f.uuid());
            if (p == null || !f.alive() || p.getWorld() != world) {
                continue;
            }
            if (p.getLocation().distance(at) <= STRIKE_RADIUS) {
                hurt(p, f, STRIKE_DAMAGE, Cause.STORM);
                if (f.alive()) {
                    p.setVelocity(p.getLocation().toVector().subtract(at.toVector()).setY(0).normalize().multiply(0.6).setY(0.4));
                    p.sendMessage(Texts.msg(plugin.lang(p), "event.storm.hit"));
                }
            }
        }
    }

    /**
     * Nebel sichtbar machen: zwei riesige, durchscheinende weiße Flächen (Block-Anzeigen aus Glas) über der ganzen Karte,
     * die gleitend mit der Nebelgrenze steigen. Bedrock zeigt Block-Anzeigen nicht – dort bleiben die Schwaden.
     */
    private void fogPlanes() {
        float size = layout.border() * 2f + 60f;
        String[] glass = {"white_stained_glass", "light_gray_stained_glass"};
        for (int i = 0; i < FOG_LAYERS.length; i++) {
            String block = glass[i % glass.length];
            Location at = loc(0, 0, 0);
            at.setY(fogLevel + FOG_LAYERS[i]);
            fog.add(world.spawn(at, org.bukkit.entity.BlockDisplay.class, d -> {
                d.setBlock(Plan.data(block));
                d.setPersistent(false);
                d.setViewRange(8f);
                d.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
                d.setTeleportDuration(SECOND);
                d.setTransformation(new org.bukkit.util.Transformation(new org.joml.Vector3f(-size / 2, 0, -size / 2),
                        new org.joml.AxisAngle4f(), new org.joml.Vector3f(size, 0.08f, size), new org.joml.AxisAngle4f()));
            }));
        }
    }

    private void moveFog() {
        if (fog.isEmpty() && timeline.foggy(seconds)) {
            fogPlanes();
        }
        for (int i = 0; i < fog.size(); i++) {
            org.bukkit.entity.BlockDisplay d = fog.get(i);
            if (d.isValid()) {
                Location at = d.getLocation();
                at.setY(fogLevel + FOG_LAYERS[i]);
                d.teleport(at);
            }
        }
    }

    /** Nebel sichtbar machen: eine Schicht grauer Schwaden rund um jeden, der zuschaut oder kämpft. */
    private void fogParticles() {
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(215, 222, 228), 3.2f);
        for (Player p : audience()) {
            Location c = p.getLocation();
            for (int i = 0; i < 70; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                double d = 2 + random.nextDouble() * 16;
                p.spawnParticle(Particle.DUST, c.getX() + Math.cos(a) * d, fogLevel + random.nextDouble() * 0.6 - 0.3,
                        c.getZ() + Math.sin(a) * d, 1, 0, 0, 0, 0, dust);
            }
            if (c.getY() < fogLevel + 1) {
                p.spawnParticle(Particle.CLOUD, c.clone().add(0, 1, 0), 8, 1.5, 0.8, 1.5, 0.01);
            }
        }
    }

    /** Vorspulen (Verwaltung, Tests): Zeit setzen, das Ereignis kommt in der nächsten Sekunde. */
    public void skipTo(int second) {
        if (phase == Phase.GONDOLA) {
            phaseTicks = 1;
            return;
        }
        seconds = Math.max(0, second);
    }

    // ---------- Ballons steigen auf ----------

    private void tickRise() {
        if (riseStep >= RISE_STEPS) {
            return;
        }
        if (riseTicks > 0) {
            riseTicks--;
            return;
        }
        riseTicks = RISE_EVERY;
        for (int i = 0; i < layout.starts().size(); i++) {
            MapLayout.Island is = layout.starts().get(i);
            List<Object[]> blocks = gondolaBlocks(IslandColour.of(i));
            int bx = map.originX() + is.x();
            int by = MapType.FLOOR_Y + is.gondolaY();
            int bz = map.originZ() + is.z();
            // Alte Stelle räumen (nur, was noch zur Gondel gehört), neue Stelle eine Höhe darüber.
            for (Object[] b : blocks) {
                int[] o = (int[]) b[0];
                Block at = world.getBlockAt(bx + o[0], by + o[1] + Math.max(0, riseStep), bz + o[2]);
                if (at.getType() == ((BlockData) b[1]).getMaterial()) {
                    at.setType(Material.AIR, false);
                }
            }
            if (riseStep + 1 < RISE_STEPS) {
                for (Object[] b : blocks) {
                    int[] o = (int[]) b[0];
                    Block at = world.getBlockAt(bx + o[0], by + o[1] + riseStep + 1, bz + o[2]);
                    if (at.getType().isAir()) {
                        at.setBlockData((BlockData) b[1], false);
                    }
                }
            } else {
                world.spawnParticle(Particle.CLOUD, bx + 0.5, by + riseStep + 10, bz + 0.5, 40, 3, 4, 3, 0.02);
            }
        }
        if (riseStep == -1) {
            soundAll("minecraft:entity.breeze.wind_burst", 0.7f);
        }
        riseStep++;
    }

    private static synchronized List<Object[]> gondolaBlocks(IslandColour c) {
        return GONDOLA_BLOCKS.computeIfAbsent(c, k -> {
            List<Object[]> out = new ArrayList<>();
            Gondola.plan(k).forEachAt((x, y, z, d) -> out.add(new Object[] {new int[] {x, y, z}, d}));
            return out;
        });
    }

    // ---------- Ausscheiden und Sieg ----------

    /** Treffer von einem Spieler merken (für Kills durch Hinunterstoßen). */
    public void hitBy(Player victim, Player attacker) {
        Fighter f = fighters.get(victim.getUniqueId());
        if (f != null && attacker != null && !attacker.getUniqueId().equals(victim.getUniqueId())) {
            f.hitBy(attacker.getUniqueId(), System.currentTimeMillis());
        }
    }

    /**
     * Jemand scheidet aus.
     *
     * @param killer wer es war (null: der, der zuletzt getroffen hat, falls es noch nicht lange her ist)
     * @param drop   Sachen an Ort und Stelle fallen lassen (nicht, wenn man in die Tiefe gefallen ist)
     */
    public void eliminate(Player p, Fighter f, Cause cause, Player killer, boolean drop) {
        if (!f.alive() || phase == Phase.OVER) {
            return;
        }
        f.state(Fighter.State.OUT);
        UUID killerId = killer != null ? killer.getUniqueId() : f.creditTo(System.currentTimeMillis());
        Fighter k = killerId == null || sameTeam(killerId, f.uuid()) ? null : fighters.get(killerId);
        if (k != null) {
            k.kill();
            Player kp = Bukkit.getPlayer(k.uuid());
            if (kp != null) {
                String lang = plugin.lang(kp);
                kp.sendActionBar(Texts.text(lang, "kill.bar", Placeholder.unparsed("name", f.name()),
                        Placeholder.unparsed("n", String.valueOf(k.kills()))));
                Ui.sound(kp, "minecraft:entity.experience_orb.pickup", 0.8f);
            }
        }
        if (drop && p != null && p.getLocation().getY() > MapType.FLOOR_Y + layout.voidY()) {
            Location at = p.getLocation();
            for (ItemStack item : p.getInventory().getContents()) {
                if (item != null && !item.getType().isAir()) {
                    world.dropItemNaturally(at, item);
                }
            }
        }
        Team t = team(f);
        if (!teamAlive(t)) {
            int place = aliveTeams() + 1;
            for (UUID u : t.members()) {
                fighters.get(u).place(place);
            }
        }
        String msgKey = switch (cause) {
            case KILLED -> k != null ? "elim.killed" : "elim.died";
            case VOID -> k != null ? "elim.void-by" : "elim.void";
            case FOG -> k != null ? "elim.fog-by" : "elim.fog";
            case STORM -> "elim.storm";
            case BORDER -> "elim.border";
            case LEFT -> "elim.left";
            case OTHER -> "elim.died";
        };
        String killerName = k == null ? "" : k.name();
        int left = aliveCount();
        broadcast(lang -> Texts.msg(lang, msgKey, Placeholder.unparsed("name", f.name()),
                Placeholder.unparsed("killer", killerName), Placeholder.unparsed("n", String.valueOf(left))));
        if (p != null && cause != Cause.LEFT && !p.isDead()) {
            String lang = plugin.lang(p);
            prepare(p, GameMode.SPECTATOR);
            p.teleport(cause == Cause.VOID ? spectatorSpot() : p.getLocation().add(0, 2, 0));
            boolean mateAlive = teamAlive(t);
            Ui.title(p, Texts.text(lang, "out.title"), mateAlive ? Texts.text(lang, "out.sub.mate")
                    : Texts.text(lang, "out.sub", Placeholder.unparsed("n", String.valueOf(f.place()))), 2500);
            p.sendMessage(Texts.msg(lang, "out.how"));
            Ui.sound(p, "minecraft:entity.player.hurt", 0.6f);
        } else if (p != null && !p.isDead()) {
            p.getInventory().clear();
        }
        plugin.boards().game(this);
        checkWin();
    }

    private void checkWin() {
        if (phase == Phase.OVER) {
            return;
        }
        if (aliveTeams() <= 1) {
            end(teams.stream().filter(this::teamAlive).findFirst().orElse(null));
        }
    }

    /** Runde vorbei: Sieger feiern, Statistik melden, nach dem Nachspiel zurück in den Wolkenhafen. */
    private void end(Team won) {
        phase = Phase.OVER;
        winner = won;
        phaseTicks = settings.afterSeconds() * SECOND;
        if (won != null) {
            for (UUID u : won.members()) {
                fighters.get(u).place(1);
            }
        }
        String names = won == null ? "" : teamNames(won);
        for (Player p : audience()) {
            String lang = plugin.lang(p);
            Fighter f = fighters.get(p.getUniqueId());
            boolean mine = won != null && f != null && f.team() == won.number();
            if (won == null) {
                Ui.title(p, Texts.text(lang, "end.draw.title"), Texts.text(lang, "end.draw.sub"), 4000);
            } else if (mine) {
                Ui.title(p, Texts.text(lang, "end.win.title"), Texts.text(lang, "end.win.sub"), 4000);
                Ui.sound(p, "minecraft:ui.toast.challenge_complete", 1f);
            } else {
                Ui.title(p, Texts.text(lang, "end.lose.title", Placeholder.unparsed("names", names)),
                        Texts.text(lang, "end.lose.sub"), 4000);
            }
            p.sendMessage(Texts.msg(lang, won == null ? "end.draw.chat" : "end.chat", Placeholder.unparsed("names", names),
                    colourName(lang, won == null ? IslandColour.EDELWEISS : won.colour())));
            if (f != null) {
                p.sendMessage(Texts.msg(lang, "end.mine", Placeholder.unparsed("kills", String.valueOf(f.kills())),
                        Placeholder.unparsed("chests", String.valueOf(f.chests())),
                        Placeholder.unparsed("place", String.valueOf(Math.max(1, f.place())))));
            }
        }
        if (won != null) {
            for (UUID u : won.members()) {
                Player p = Bukkit.getPlayer(u);
                if (p != null) {
                    p.setInvulnerable(true);
                    fireworks(p.getLocation(), won.colour());
                }
            }
        }
        report();
        updateBar();
        plugin.boards().game(this);
        plugin.stateChanged();
    }

    private void fireworks(Location at, IslandColour c) {
        for (int i = 0; i < 3; i++) {
            Location l = at.clone().add(random.nextInt(5) - 2, 1, random.nextInt(5) - 2);
            int delay = i * 12;
            Bukkit.getScheduler().runTaskLater(plugin, () -> world.spawn(l, Firework.class, fw -> {
                fw.addScoreboardTag(FIREWORK_TAG);
                var meta = fw.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BALL_LARGE).withColor(Color.fromRGB(c.rgb()))
                        .withFade(Color.WHITE).trail(true).build());
                meta.setPower(1);
                fw.setFireworkMeta(meta);
            }), delay);
        }
    }

    /** Statistik: lokal und ans Netzwerk. */
    private void report() {
        for (Fighter f : fighters.values()) {
            boolean won = winner != null && f.team() == winner.number();
            PlayerData d = plugin.store().get(f.uuid(), f.name());
            boolean best = d.addGame(won, f.kills(), !won, f.chests());
            plugin.store().save(d);
            plugin.board().update(f.uuid(), d.name(), d.wins(), d.kills(), d.games());
            Map<String, Long> add = new LinkedHashMap<>();
            add.put("games", 1L);
            add.put("wins", won ? 1L : 0L);
            add.put("kills", (long) f.kills());
            add.put("deaths", won ? 0L : 1L);
            add.put("chests", (long) f.chests());
            plugin.net().stats(f.uuid(), add, Map.of("best_kills", (long) f.kills()));
            Player p = Bukkit.getPlayer(f.uuid());
            if (p != null && best && f.kills() > 0) {
                p.sendMessage(Texts.msg(plugin.lang(p), "end.best", Placeholder.unparsed("n", String.valueOf(f.kills()))));
            }
        }
        plugin.saveBoard();
    }

    /** Runde abbrechen oder fertig: alle zurück in den Wolkenhafen, Karte wiederherstellen. */
    public void close() {
        phase = Phase.OVER;
        for (Player p : audience()) {
            bar.hide(p);
            p.setInvulnerable(false);
            plugin.hall().enter(p);
        }
        for (Fighter f : fighters.values()) {
            plugin.store().unpin(f.uuid(), Bukkit.getPlayer(f.uuid()) != null);
        }
        spectators.clear();
        strikes.clear();
        fog.forEach(org.bukkit.entity.Entity::remove);
        fog.clear();
        if (plugin.isEnabled()) {
            plugin.construction().restore(map, () -> plugin.boards().refreshHall());
        }
    }

    // ---------- Anzeigen ----------

    private void updateBar() {
        switch (phase) {
            case GONDOLA -> {
                int secs = Math.max(0, (phaseTicks + SECOND - 1) / SECOND);
                bar.update(lang -> Texts.text(lang, "bar.gondola", Placeholder.unparsed("secs", String.valueOf(secs))),
                        1f - secs / (float) settings.gondolaSeconds(), BossBar.Color.YELLOW);
            }
            case FIGHT -> {
                Timeline.Event next = timeline.next(seconds);
                if (next != null) {
                    int until = timeline.untilNext(seconds);
                    int prev = switch (next) {
                        case REFILL -> 0;
                        case STORM -> timeline.refill();
                        case FOG -> timeline.storm();
                    };
                    float progress = 1f - until / (float) Math.max(1, timeline.at(next) - prev);
                    bar.update(lang -> Texts.text(lang, "bar." + key(next), Placeholder.unparsed("time", Ui.clock(until))),
                            progress, next == Timeline.Event.REFILL ? BossBar.Color.GREEN
                                    : next == Timeline.Event.STORM ? BossBar.Color.BLUE : BossBar.Color.WHITE);
                } else {
                    double rise = timeline.fogRise(seconds);
                    bar.update(lang -> Texts.text(lang, rise >= 1 ? "bar.fog.full" : "bar.fog.rising"), (float) rise,
                            BossBar.Color.PURPLE);
                }
            }
            case OVER -> bar.update(lang -> Texts.text(lang, "bar.over"), 1f, BossBar.Color.PINK);
        }
    }

    /** Für die Leiste: Name des nächsten Ereignisses (Text-Schlüssel) oder null. */
    public String nextEventKey() {
        Timeline.Event e = phase == Phase.FIGHT ? timeline.next(seconds) : null;
        return e == null ? null : "side.next." + key(e);
    }
}

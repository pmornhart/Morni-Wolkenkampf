package de.moinmorni.wolkenkampf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/**
 * Alle Texte für Spieler, auf Deutsch und Englisch, als MiniMessage. Eigene Farben: {@code <sky>} Himmelblau,
 * {@code <au>} Gold, {@code <em>} Smaragd, {@code <bl>} Weiß-Blau. Namen von Spielern kommen immer als
 * {@code Placeholder.unparsed} hinein.
 */
public final class Texts {

    public static final int SKY = 0x7EC8F2;
    public static final int GOLD = 0xF2B632;
    public static final int EMERALD = 0x2BB673;
    public static final int BLUE = 0x5DA9E9;

    public static final MiniMessage MM = MiniMessage.builder()
            .editTags(t -> t.resolvers(
                    TagResolver.resolver("sky", Tag.styling(TextColor.color(SKY))),
                    TagResolver.resolver("au", Tag.styling(TextColor.color(GOLD))),
                    TagResolver.resolver("em", Tag.styling(TextColor.color(EMERALD))),
                    TagResolver.resolver("bl", Tag.styling(TextColor.color(BLUE)))))
            .build();

    private static final Map<String, String[]> TEXTS = new LinkedHashMap<>();

    private Texts() {
    }

    private static void t(String key, String de, String en) {
        if (TEXTS.put(key, new String[] {de, en}) != null) {
            throw new IllegalStateException("Text doppelt: " + key);
        }
    }

    static {
        t("prefix", "<sky>☁</sky> <au>Wolkenkampf</au> <dark_gray>»</dark_gray> ",
                "<sky>☁</sky> <au>Cloud Clash</au> <dark_gray>»</dark_gray> ");
        t("mode", "Wolkenkampf", "Cloud Clash");

        // ---------- Allgemein ----------
        t("only-players", "Das geht nur im Spiel.", "Only possible in-game.");
        t("unknown-player", "<gray>Niemanden namens <white><name></white> gefunden.</gray>",
                "<gray>Nobody called <white><name></white> found.</gray>");
        t("lobby.kick", "<au>MoinMorni</au>\n<gray>Zurück in die Lobby: Bitte verbinde dich neu mit dem Netzwerk.</gray>",
                "<au>MoinMorni</au>\n<gray>Back to the lobby: please reconnect to the network.</gray>");
        t("lobby.going", "<gray>Ab in die Lobby – bis bald!</gray>", "<gray>Off to the lobby – see you soon!</gray>");

        // ---------- Wolkenhafen ----------
        t("hall.title", "<sky>☁ Wolkenkampf</sky>", "<sky>☁ Cloud Clash</sky>");
        t("hall.subtitle", "<gray>Inseln in den Wolken – wer steht zuletzt?</gray>",
                "<gray>Islands in the clouds – who stands last?</gray>");
        t("hall.welcome", "<gray>Willkommen im <sky>Wolkenhafen</sky>! Mit dem <white>Kompass</white> stimmst du über Karte und Spielart ab, in der <white>Kiste</white> wählst du deine Start-Ausrüstung, mit dem <white>grünen Farbstoff</white> sagst du „bereit“.</gray>",
                "<gray>Welcome to the <sky>Cloud Harbour</sky>! Use the <white>compass</white> to vote for map and mode, the <white>chest</white> to pick your starting kit and the <white>green dye</white> to say you're ready.</gray>");
        t("hall.how", "<gray>So geht's: Jeder startet in einer <au>Ballon-Gondel</au> über seiner eigenen Insel. Öffnet sich die Gondel, schwebst du hinunter. Plündere die <white>Truhen</white>, baue <white>Brücken</white> und kämpfe – wer zuletzt noch steht, gewinnt. Fällst du in die Tiefe, bist du raus!</gray>",
                "<gray>How it works: everyone starts in a <au>balloon gondola</au> above their own island. When it opens, you float down. Loot the <white>chests</white>, build <white>bridges</white> and fight – the last one standing wins. Fall into the depths and you're out!</gray>");
        t("hall.waiting", "<gray>Warte auf Mitspieler … (<white><n></white> da, mindestens 2)</gray>",
                "<gray>Waiting for players … (<white><n></white> here, at least 2)</gray>");
        t("hall.countdown", "<gray>Nächste Runde in <au><secs> s</au> · <sky><map></sky> · <white><mode></white></gray>",
                "<gray>Next round in <au><secs> s</au> · <sky><map></sky> · <white><mode></white></gray>");
        t("hall.no-map", "<gray>Gerade sind alle Karten belegt – gleich wird eine frei.</gray>",
                "<gray>All maps are busy – one will be free soon.</gray>");
        t("hall.ready", "<em>Du bist bereit.</em> <gray>Sind alle bereit, geht's gleich los.</gray>",
                "<em>You're ready.</em> <gray>Once everyone is ready, we start.</gray>");
        t("hall.ready.alone", "<em>Du bist bereit.</em> <gray>Sobald noch jemand da ist, geht's los.</gray>",
                "<em>You're ready.</em> <gray>As soon as someone else joins, we start.</gray>");
        t("hall.not-ready", "<gray>Du bist nicht mehr bereit.</gray>", "<gray>You're no longer ready.</gray>");
        t("hall.voted", "<gray>Deine Stimme: <sky><choice></sky>.</gray>", "<gray>Your vote: <sky><choice></sky>.</gray>");
        t("hall.kit", "<gray>Deine Start-Ausrüstung: <sky><kit></sky> – du bekommst sie, sobald sich die Gondel öffnet.</gray>",
                "<gray>Your starting kit: <sky><kit></sky> – you get it as soon as the gondola opens.</gray>");
        t("hall.full", "<gray>Die Runde ist voll (höchstens 12) – du bist in der nächsten dabei.</gray>",
                "<gray>The round is full (12 at most) – you're in the next one.</gray>");
        t("hall.starting", "<em>Auf geht's!</em> <gray>Karte: <sky><map></sky> · <white><mode></white> · <white><n></white> Leute.</gray>",
                "<em>Here we go!</em> <gray>Map: <sky><map></sky> · <white><mode></white> · <white><n></white> players.</gray>");
        t("hall.party-playing", "<gray>Deine Party kämpft gerade – mit <white>/zuschauen</white> kannst du zusehen.</gray>",
                "<gray>Your party is fighting right now – use <white>/spectate</white> to watch.</gray>");
        t("hall.only", "<gray>Das geht nur im Wolkenhafen.</gray>", "<gray>That only works in the Cloud Harbour.</gray>");

        t("item.vote", "<sky>✎ Abstimmen</sky>", "<sky>✎ Vote</sky>");
        t("item.vote.lore", "<gray>Rechtsklick: Karte und Spielart wählen</gray>\n<gray>oder einer Runde zuschauen.</gray>",
                "<gray>Right-click: choose map and mode</gray>\n<gray>or watch a round.</gray>");
        t("item.ready", "<em>✔ Bereit</em>", "<em>✔ Ready</em>");
        t("item.ready.lore", "<gray>Rechtsklick: bereit oder nicht bereit.</gray>", "<gray>Right-click: ready or not ready.</gray>");
        t("item.kit", "<au>✦ Start-Ausrüstung</au>", "<au>✦ Starting kit</au>");
        t("item.kit.lore", "<gray>Gewählt: <white><kit></white></gray>\n<gray>Rechtsklick: kostenlos eine andere wählen.</gray>",
                "<gray>Chosen: <white><kit></white></gray>\n<gray>Right-click: pick another one, free of charge.</gray>");
        t("item.stats", "<au>★ Statistik</au>", "<au>★ Stats</au>");
        t("item.stats.lore", "<gray>Rechtsklick: deine Werte und die Besten.</gray>", "<gray>Right-click: your stats and the best.</gray>");
        t("item.lobby", "<red>⌂ Zur Lobby</red>", "<red>⌂ To the lobby</red>");
        t("item.lobby.lore", "<gray>Rechtsklick: zurück in die Lobby.</gray>", "<gray>Right-click: back to the lobby.</gray>");

        t("menu.vote.title", "Abstimmen", "Vote");
        t("menu.vote.votes", "<gray>Stimmen: <white><n></white></gray>", "<gray>Votes: <white><n></white></gray>");
        t("menu.vote.busy", "<au>Läuft gerade (<n> übrig) – Klick: zuschauen</au>", "<au>Running (<n> left) – click: watch</au>");
        t("menu.vote.cleaning", "<gray>Wird gerade aufgeräumt – gleich wieder frei.</gray>",
                "<gray>Being tidied up – free again in a moment.</gray>");
        t("menu.vote.mine", "<em>✔ Deine Stimme</em>", "<em>✔ Your vote</em>");
        t("menu.vote.click", "<yellow>Klick: dafür stimmen</yellow>", "<yellow>Click: vote for it</yellow>");
        t("menu.close", "<gray>Schließen</gray>", "<gray>Close</gray>");
        t("menu.kit.title", "Start-Ausrüstung (kostenlos)", "Starting kit (free)");
        t("menu.kit.mine", "<em>✔ Gewählt</em>", "<em>✔ Chosen</em>");
        t("menu.kit.click", "<yellow>Klick: wählen</yellow>", "<yellow>Click: choose</yellow>");
        t("menu.stats.title", "Statistik", "Stats");
        t("menu.stats.me", "<au>Deine Werte</au>", "<au>Your stats</au>");
        t("menu.stats.top", "<au>Die besten Wolkenkämpfer</au>", "<au>The best cloud fighters</au>");

        // ---------- Karten, Spielarten, Ausrüstungen, Inselfarben ----------
        t("map.wolkenalm", "Wolkenalm", "Cloud Alp");
        t("map.wolkenalm.short", "Grüne Almen, Bergkapelle", "Green pastures, mountain chapel");
        t("map.wolkenalm.desc", "<gray>Zwölf grüne Alm-Inseln über dem Wolkenmeer.</gray>\n<gray>In der Mitte die Bergkapelle mit Maibaum.</gray>",
                "<gray>Twelve green pasture islands above a sea of clouds.</gray>\n<gray>In the middle: the mountain chapel and maypole.</gray>");
        t("map.kristallgipfel", "Kristallgipfel", "Crystal Peak");
        t("map.kristallgipfel.short", "Fels, Eis und Amethyst", "Rock, ice and amethyst");
        t("map.kristallgipfel.desc", "<gray>Verschneite Felsinseln mit Eisnadeln und Kristallen.</gray>\n<gray>In der Mitte der Gipfel mit Kreuz und Kristallgrotte.</gray>",
                "<gray>Snowy rock islands with ice spikes and crystals.</gray>\n<gray>In the middle: the summit with its cross and crystal grotto.</gray>");
        t("mode.allein", "Allein", "Solo");
        t("mode.allein.desc", "<gray>Jeder für sich: eine Insel, eine Gondel pro Person.</gray>",
                "<gray>Every one for themselves: one island, one gondola each.</gray>");
        t("mode.zweier", "Zu zweit", "Duos");
        t("mode.zweier.desc", "<gray>Zweier-Teams teilen sich Insel und Gondel.</gray>\n<gray>Partys kommen ins selbe Team. Ab 3 Leuten.</gray>",
                "<gray>Teams of two share an island and gondola.</gray>\n<gray>Parties end up in the same team. From 3 players.</gray>");
        t("kit.wanderer", "Wanderer", "Hiker");
        t("kit.wanderer.desc", "<gray>Ohne alles – nur Mut und gute Schuhe.</gray>", "<gray>Nothing at all – just courage and good boots.</gray>");
        t("kit.baumeister", "Baumeister", "Builder");
        t("kit.baumeister.desc", "<gray>24 Bretter und eine Steinspitzhacke:</gray>\n<gray>schneller die erste Brücke bauen.</gray>",
                "<gray>24 planks and a stone pickaxe:</gray>\n<gray>build your first bridge faster.</gray>");
        t("kit.jaeger", "Jäger", "Hunter");
        t("kit.jaeger.desc", "<gray>Bogen und 8 Pfeile.</gray>", "<gray>Bow and 8 arrows.</gray>");
        t("kit.bergfuehrer", "Bergführer", "Mountain guide");
        t("kit.bergfuehrer.desc", "<gray>Lederstiefel mit Federfall II und ein Wassereimer:</gray>\n<gray>sanfter fallen, sicherer landen.</gray>",
                "<gray>Leather boots with Feather Falling II and a water bucket:</gray>\n<gray>fall softer, land safer.</gray>");
        t("kit.sennerin", "Sennerin", "Dairymaid");
        t("kit.sennerin.desc", "<gray>6 Ofenkartoffeln und ein Heiltrank.</gray>", "<gray>6 baked potatoes and a healing potion.</gray>");
        t("kit.holzknecht", "Holzknecht", "Lumberjack");
        t("kit.holzknecht.desc", "<gray>Eine Steinaxt – zum Hacken und Kämpfen.</gray>", "<gray>A stone axe – for chopping and fighting.</gray>");
        t("kit.schmied", "Schmied", "Smith");
        t("kit.schmied.desc", "<gray>Ein Kettenhemd.</gray>", "<gray>A chainmail chestplate.</gray>");
        t("colour.enzian", "Enzian", "Gentian");
        t("colour.alpenrose", "Alpenrose", "Alpine Rose");
        t("colour.edelweiss", "Edelweiß", "Edelweiss");
        t("colour.trollblume", "Trollblume", "Globeflower");
        t("colour.alpenlein", "Alpenlein", "Alpine Flax");
        t("colour.arnika", "Arnika", "Arnica");
        t("colour.krokus", "Krokus", "Crocus");
        t("colour.frauenmantel", "Frauenmantel", "Lady's Mantle");
        t("colour.vergissmeinnicht", "Vergissmeinnicht", "Forget-me-not");
        t("colour.seidelbast", "Seidelbast", "Daphne");
        t("colour.latsche", "Latsche", "Mountain Pine");
        t("colour.mehlprimel", "Mehlprimel", "Bird's-eye Primrose");

        // ---------- Runde ----------
        t("game.title", "<sky>☁ Wolkenkampf</sky>", "<sky>☁ Cloud Clash</sky>");
        t("game.welcome", "<gray>Du schwebst über der <sky><map></sky>. In <au><secs> s</au> öffnet sich deine Gondel.</gray>",
                "<gray>You're floating above <sky><map></sky>. Your gondola opens in <au><secs> s</au>.</gray>");
        t("game.island", "<gray>Deine Insel: <colour>. Das ist auch deine Farbe.</gray>",
                "<gray>Your island: <colour>. That's your colour, too.</gray>");
        t("game.mate", "<gray>Du kämpfst zusammen mit <white><name></white>.</gray>", "<gray>You're fighting together with <white><name></white>.</gray>");
        t("game.gondola.sub", "<gray>Gleich öffnet sich der Boden der Gondel …</gray>", "<gray>The gondola floor opens in a moment …</gray>");
        t("game.count", "<au><n></au>", "<au><n></au>");
        t("game.go", "<sky>Los geht's!</sky>", "<sky>Let's go!</sky>");
        t("game.go.sub", "<gray>Plündern, Brücken bauen, kämpfen!</gray>", "<gray>Loot, build bridges, fight!</gray>");
        t("game.started", "<gray>Die Gondel ist offen! Deine Ausrüstung: <au><kit></au>. Die <white>Truhen</white> auf deiner Insel warten.</gray>",
                "<gray>The gondola is open! Your kit: <au><kit></au>. The <white>chests</white> on your island are waiting.</gray>");
        t("kill.bar", "<au>⚔ <name> erledigt</au> <gray>· deine Kills: <white><n></white></gray>",
                "<au>⚔ <name> defeated</au> <gray>· your kills: <white><n></white></gray>");

        t("event.refill.warn", "<gray>In <white><secs> s</white> kommt <em>Nachschub</em>: Alle Truhen füllen sich neu!</gray>",
                "<gray>In <white><secs> s</white> <em>supplies</em> arrive: every chest gets refilled!</gray>");
        t("event.refill.title", "<em>Nachschub!</em>", "<em>Supplies!</em>");
        t("event.refill.sub", "<gray>Alle Truhen sind wieder voll.</gray>", "<gray>Every chest is full again.</gray>");
        t("event.refill.now", "<em>Nachschub!</em> <gray>Alle Truhen haben sich neu gefüllt.</gray>",
                "<em>Supplies!</em> <gray>Every chest has been refilled.</gray>");
        t("event.storm.warn", "<gray>In <white><secs> s</white> zieht ein <bl>Gewitter</bl> auf – achte auf knisternde Stellen!</gray>",
                "<gray>A <bl>thunderstorm</bl> rolls in in <white><secs> s</white> – watch out for crackling spots!</gray>");
        t("event.storm.title", "<bl>⚡ Gewitter!</bl>", "<bl>⚡ Thunderstorm!</bl>");
        t("event.storm.sub", "<gray>Wo es knistert, schlägt gleich der Blitz ein.</gray>", "<gray>Where it crackles, lightning strikes next.</gray>");
        t("event.storm.now", "<bl>⚡ Gewitter!</bl> <gray>Blitze schlagen ein – wo es knistert, schnell weg!</gray>",
                "<bl>⚡ Thunderstorm!</bl> <gray>Lightning strikes – where it crackles, get away fast!</gray>");
        t("event.storm.hit", "<bl>⚡ Der Blitz hat dich gestreift!</bl>", "<bl>⚡ The lightning grazed you!</bl>");
        t("event.fog.warn", "<gray>In <white><secs> s</white> steigt der <white>Nebel</white> aus der Tiefe – bleib oben!</gray>",
                "<gray>In <white><secs> s</white> the <white>fog</white> rises from the depths – stay up high!</gray>");
        t("event.fog.title", "<white>Der Nebel steigt!</white>", "<white>The fog is rising!</white>");
        t("event.fog.sub", "<gray>Wer im Nebel steht, nimmt Schaden.</gray>", "<gray>Standing in the fog hurts.</gray>");
        t("event.fog.now", "<white>Der Nebel steigt!</white> <gray>Langsam verschluckt er die Inseln von unten – wer darin steht, nimmt Schaden. Geh nach oben!</gray>",
                "<white>The fog is rising!</white> <gray>It slowly swallows the islands from below – standing in it hurts. Go up!</gray>");
        t("fog.inside", "<red>Du stehst im Nebel – nach oben!</red>", "<red>You're in the fog – go up!</red>");
        t("border.outside", "<red>Am Rand tobt der Sturm – zurück zu den Inseln!</red>",
                "<red>A gale rages at the edge – back to the islands!</red>");
        t("border.build", "<gray>Hier kannst du nicht bauen (zu weit draußen oder zu hoch).</gray>",
                "<gray>You can't build here (too far out or too high).</gray>");
        t("border.pearl", "<gray>So weit trägt dich die Perle nicht – dort draußen tobt der Sturm.</gray>",
                "<gray>The pearl can't take you that far – a gale rages out there.</gray>");

        t("elim.killed", "<white><name></white> <gray>wurde von</gray> <au><killer></au> <gray>erledigt. Noch <white><n></white> dabei.</gray>",
                "<white><name></white> <gray>was defeated by</gray> <au><killer></au><gray>. <white><n></white> left.</gray>");
        t("elim.died", "<white><name></white> <gray>ist ausgeschieden. Noch <white><n></white> dabei.</gray>",
                "<white><name></white> <gray>is out. <white><n></white> left.</gray>");
        t("elim.void", "<white><name></white> <gray>ist in die Tiefe gestürzt. Noch <white><n></white> dabei.</gray>",
                "<white><name></white> <gray>fell into the depths. <white><n></white> left.</gray>");
        t("elim.void-by", "<white><name></white> <gray>wurde von</gray> <au><killer></au> <gray>in die Tiefe gestoßen. Noch <white><n></white> dabei.</gray>",
                "<white><name></white> <gray>was knocked into the depths by</gray> <au><killer></au><gray>. <white><n></white> left.</gray>");
        t("elim.fog", "<white><name></white> <gray>hat sich im Nebel verirrt. Noch <white><n></white> dabei.</gray>",
                "<white><name></white> <gray>got lost in the fog. <white><n></white> left.</gray>");
        t("elim.fog-by", "<white><name></white> <gray>ist im Nebel untergegangen – auf der Flucht vor</gray> <au><killer></au><gray>. Noch <white><n></white> dabei.</gray>",
                "<white><name></white> <gray>was lost in the fog while fleeing from</gray> <au><killer></au><gray>. <white><n></white> left.</gray>");
        t("elim.storm", "<white><name></white> <gray>wurde vom Blitz getroffen. Noch <white><n></white> dabei.</gray>",
                "<white><name></white> <gray>was struck by lightning. <white><n></white> left.</gray>");
        t("elim.border", "<white><name></white> <gray>wurde vom Sturm am Rand erwischt. Noch <white><n></white> dabei.</gray>",
                "<white><name></white> <gray>was caught by the gale at the edge. <white><n></white> left.</gray>");
        t("elim.left", "<white><name></white> <gray>hat die Runde verlassen. Noch <white><n></white> dabei.</gray>",
                "<white><name></white> <gray>left the round. <white><n></white> left.</gray>");
        t("out.title", "<red>Ausgeschieden</red>", "<red>You're out</red>");
        t("out.sub", "<gray>Platz <white><n></white> – du schaust jetzt zu.</gray>", "<gray>Place <white><n></white> – you're spectating now.</gray>");
        t("out.sub.mate", "<gray>Dein Team kämpft weiter – feuere es an!</gray>", "<gray>Your team fights on – cheer them on!</gray>");
        t("out.how", "<gray>Du schaust zu. Mit <white>/verlassen</white> geht's zurück in den Wolkenhafen.</gray>",
                "<gray>You're spectating. <white>/leave</white> takes you back to the Cloud Harbour.</gray>");

        t("end.win.title", "<au>★ Gewonnen! ★</au>", "<au>★ Victory! ★</au>");
        t("end.win.sub", "<gray>Du stehst zuletzt auf den Wolken.</gray>", "<gray>You're the last one standing on the clouds.</gray>");
        t("end.lose.title", "<au><names></au> <gray>gewinnt</gray>", "<au><names></au> <gray>wins</gray>");
        t("end.lose.sub", "<gray>Gut gekämpft – nächste Runde gleich!</gray>", "<gray>Well fought – next round soon!</gray>");
        t("end.draw.title", "<gray>Unentschieden</gray>", "<gray>Draw</gray>");
        t("end.draw.sub", "<gray>Niemand ist übrig geblieben.</gray>", "<gray>Nobody is left.</gray>");
        t("end.chat", "<au>★</au> <gray>Sieg für</gray> <au><names></au> <gray>(Insel <colour>)!</gray>",
                "<au>★</au> <gray>Victory for</gray> <au><names></au> <gray>(island <colour>)!</gray>");
        t("end.draw.chat", "<gray>Unentschieden – diesmal hat der Nebel gewonnen.</gray>",
                "<gray>A draw – this time the fog won.</gray>");
        t("end.mine", "<gray>Deine Runde: Platz <white><place></white> · <white><kills></white> Kills · <white><chests></white> Truhen geöffnet.</gray>",
                "<gray>Your round: place <white><place></white> · <white><kills></white> kills · <white><chests></white> chests opened.</gray>");
        t("end.best", "<au>Neue Bestleistung:</au> <gray><white><n></white> Kills in einer Runde!</gray>",
                "<au>New personal best:</au> <gray><white><n></white> kills in one round!</gray>");

        t("spectate.start", "<gray>Du schaust <sky><map></sky> zu. <white>/verlassen</white> bringt dich zurück.</gray>",
                "<gray>You're watching <sky><map></sky>. <white>/leave</white> takes you back.</gray>");
        t("spectate.none", "<gray>Gerade läuft keine Runde.</gray>", "<gray>No round is running right now.</gray>");
        t("spectate.fighting", "<gray>Du kämpfst doch gerade selbst!</gray>", "<gray>You're fighting yourself right now!</gray>");
        t("leave.none", "<gray>Du bist schon im Wolkenhafen.</gray>", "<gray>You're already in the Cloud Harbour.</gray>");
        t("leave.done", "<gray>Zurück im Wolkenhafen.</gray>", "<gray>Back in the Cloud Harbour.</gray>");

        // ---------- Leiste oben ----------
        t("bar.gondola", "<au>Die Gondel öffnet sich in <secs> s</au>", "<au>The gondola opens in <secs> s</au>");
        t("bar.refill", "<em>Nachschub in <time></em>", "<em>Supplies in <time></em>");
        t("bar.storm", "<bl>Gewitter in <time></bl>", "<bl>Thunderstorm in <time></bl>");
        t("bar.fog", "<white>Nebel in <time></white>", "<white>Fog in <time></white>");
        t("bar.fog.rising", "<white>Der Nebel steigt …</white>", "<white>The fog is rising …</white>");
        t("bar.fog.full", "<red>Der Nebel steht über allem!</red>", "<red>The fog covers everything!</red>");
        t("bar.over", "<au>Runde vorbei</au>", "<au>Round over</au>");

        // ---------- Leiste rechts ----------
        t("side.title", "<sky><b>☁ Wolkenkampf</b></sky>", "<sky><b>☁ Cloud Clash</b></sky>");
        t("side.map", "<gray>Karte: <white><map></white></gray>", "<gray>Map: <white><map></white></gray>");
        t("side.mode", "<gray>Spielart: <white><mode></white></gray>", "<gray>Mode: <white><mode></white></gray>");
        t("side.gondola", "<au>In der Gondel …</au>", "<au>In the gondola …</au>");
        t("side.time", "<gray>Zeit: <white><time></white></gray>", "<gray>Time: <white><time></white></gray>");
        t("side.next.refill", "<em>Nachschub in <time></em>", "<em>Supplies in <time></em>");
        t("side.next.storm", "<bl>Gewitter in <time></bl>", "<bl>Storm in <time></bl>");
        t("side.next.fog", "<white>Nebel in <time></white>", "<white>Fog in <time></white>");
        t("side.fog", "<white>Der Nebel steigt</white>", "<white>The fog is rising</white>");
        t("side.alive", "<gray>Übrig: <white><n></white></gray>", "<gray>Left: <white><n></white></gray>");
        t("side.alive.duo", "<gray>Übrig: <white><n></white> (<white><teams></white> Teams)</gray>",
                "<gray>Left: <white><n></white> (<white><teams></white> teams)</gray>");
        t("side.kills", "<gray>Deine Kills: <au><n></au></gray>", "<gray>Your kills: <au><n></au></gray>");
        t("side.island", "<gray>Insel: <colour></gray>", "<gray>Island: <colour></gray>");
        t("side.out", "<red>Ausgeschieden</red>", "<red>Out</red>");
        t("side.spectating", "<gray>Du schaust zu</gray>", "<gray>Spectating</gray>");
        t("side.site", "<au>moinmorni.net</au>", "<au>moinmorni.net</au>");
        t("side.hall.players", "<gray>Im Wolkenhafen: <white><n></white></gray>", "<gray>In the harbour: <white><n></white></gray>");
        t("side.hall.start", "<gray>Start in <au><secs> s</au></gray>", "<gray>Start in <au><secs> s</au></gray>");
        t("side.hall.wait", "<gray>Warte auf Mitspieler …</gray>", "<gray>Waiting for players …</gray>");
        t("side.kit", "<gray>Ausrüstung: <white><kit></white></gray>", "<gray>Kit: <white><kit></white></gray>");
        t("side.hall.wins", "<gray>Deine Siege: <au><n></au></gray>", "<gray>Your wins: <au><n></au></gray>");

        // ---------- Statistik ----------
        t("stats.head", "<au>Wolkenkampf – <name></au>", "<au>Cloud Clash – <name></au>");
        t("stats.none", "<gray>Von <white><name></white> gibt es noch keine Werte.</gray>", "<gray>No stats for <white><name></white> yet.</gray>");
        t("stats.lines", "<gray>Runden: <white><games></white> · Siege: <au><wins></au></gray>\n<gray>Kills: <white><kills></white> · Ausgeschieden: <white><deaths></white></gray>\n<gray>Truhen geöffnet: <white><chests></white></gray>\n<gray>Meiste Kills in einer Runde: <white><best></white></gray>",
                "<gray>Rounds: <white><games></white> · Wins: <au><wins></au></gray>\n<gray>Kills: <white><kills></white> · Eliminated: <white><deaths></white></gray>\n<gray>Chests opened: <white><chests></white></gray>\n<gray>Most kills in one round: <white><best></white></gray>");
        t("top.head", "<au>Die besten Wolkenkämpfer</au>", "<au>The best cloud fighters</au>");
        t("top.empty", "<gray>Noch niemand – sei der Erste!</gray>", "<gray>Nobody yet – be the first!</gray>");
        t("top.row", "<au><pos>.</au> <white><name></white> <gray>– <au><wins></au> Siege, <white><kills></white> Kills</gray>",
                "<au><pos>.</au> <white><name></white> <gray>– <au><wins></au> wins, <white><kills></white> kills</gray>");

        // ---------- Tafeln im Wolkenhafen ----------
        t("board.title", "<au><b>Die besten Wolkenkämpfer</b></au>", "<au><b>The best cloud fighters</b></au>");
        t("board.table", "<sky><b><map></b></sky>\n<gray><desc></gray>\n<state>", "<sky><b><map></b></sky>\n<gray><desc></gray>\n<state>");
        t("board.table.running", "<au>Runde läuft – Kompass: zuschauen</au>", "<au>Round running – compass: watch</au>");
        t("board.table.votes", "<gray>Stimmen: <white><n></white></gray>", "<gray>Votes: <white><n></white></gray>");
        t("board.modes", "<au><b>Spielart</b></au>\n<gray>Allein: <white><solo></white> Stimmen</gray>\n<gray>Zu zweit: <white><duo></white> Stimmen</gray>\n<dark_gray>Abstimmen mit dem Kompass</dark_gray>",
                "<au><b>Mode</b></au>\n<gray>Solo: <white><solo></white> votes</gray>\n<gray>Duos: <white><duo></white> votes</gray>\n<dark_gray>Vote with the compass</dark_gray>");
        t("board.kits", "<au><b>Start-Ausrüstung</b></au>\n<gray>Alle kostenlos – wähle mit der</gray>\n<gray>Kiste in deiner Schnellleiste.</gray>",
                "<au><b>Starting kits</b></au>\n<gray>All free – pick one with the</gray>\n<gray>chest in your hotbar.</gray>");
        t("board.welcome", "<sky><b>☁ Wolkenkampf ☁</b></sky>\n<gray>Insel gegen Insel – wer steht zuletzt?</gray>",
                "<sky><b>☁ Cloud Clash ☁</b></sky>\n<gray>Island against island – who stands last?</gray>");
    }

    /** Alle Schlüssel (für Tests). */
    public static Set<String> keys() {
        return Collections.unmodifiableSet(TEXTS.keySet());
    }

    /** Roher MiniMessage-Text. Unbekannte Sprache → Deutsch. */
    public static String raw(String lang, String key) {
        String[] t = TEXTS.get(key);
        if (t == null) {
            return key;
        }
        return "en".equals(lang) ? t[1] : t[0];
    }

    /** Text ohne Vorsilbe; für Gegenstände ohne kursiv. */
    public static Component text(String lang, String key, TagResolver... resolvers) {
        return MM.deserialize(raw(lang, key), resolvers).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /** Nachricht für den Chat, mit „☁ Wolkenkampf »“ davor. */
    public static Component msg(String lang, String key, TagResolver... resolvers) {
        return MM.deserialize(raw(lang, "prefix")).append(MM.deserialize(raw(lang, key), resolvers));
    }

    /** Mehrzeiliger Text als einzelne Zeilen (für Beschreibungen von Gegenständen). */
    public static List<Component> lines(String lang, String key, TagResolver... resolvers) {
        List<Component> out = new ArrayList<>();
        for (String line : raw(lang, key).split("\n")) {
            out.add(MM.deserialize(line, resolvers).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        }
        return out;
    }

    /** Reiner Text (ohne Formatierung), z. B. für Namen von Karten. */
    public static String plain(String lang, String key) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(MM.deserialize(raw(lang, key)));
    }
}

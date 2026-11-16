# Morni Wolkenkampf

Der Insel-Kampf-Modus des Minecraft-Netzwerks **MoinMorni** (moinmorni.net): **Jeder startet in einer Ballon-Gondel
über seiner eigenen kleinen Insel in den Wolken – Truhen plündern, Brücken bauen, kämpfen. Wer zuletzt noch steht,
gewinnt.** 2 bis 12 Leute, allein oder in Zweier-Teams. Ein Paper-Plugin für Paper 26.x (Java 25), im Netzwerk und
auch ganz allein.

Englischer Name: **Cloud Clash**.

## So wird gespielt

1. Im **Wolkenhafen** (Alm-Insel auf Wolken mit großem, festgemachtem Ballon) stimmt man mit dem **Kompass** über
   Karte und Spielart ab, wählt mit der **Kiste** seine kostenlose Start-Ausrüstung und sagt mit dem **grünen
   Farbstoff** „bereit“. Ab 2 Leuten läuft ein Countdown; sind alle bereit, geht es nach 5 Sekunden los.
2. Jedes Team steht in einer **Ballon-Gondel** in der Farbe seiner Insel (Inselfarben nach Alpenblumen: Enzian,
   Alpenrose, Edelweiß …). Nach dem Countdown klappt der Boden auf und fällt weg, man schwebt sanft hinunter (bis zur
   Landung auf der Insel ohne Fallschaden), die Ballons steigen davon.
3. Auf jeder Startinsel stehen **zwei Truhen** – zusammen immer mit Waffe, Rüstungsteil, Essen und mindestens 40
   Blöcken für die erste Brücke. Die große **Mittelinsel** hat bessere Truhen (Eisen, Diamant, Perlen, Goldäpfel).
4. Brücken bauen, kämpfen, Gegner in die Tiefe stoßen. Wer stirbt oder in die Tiefe fällt, scheidet aus und schaut zu;
   wer zuletzt getroffen hat, bekommt den Kill.
5. Damit jede Runde ein Ende hat, kommen Ereignisse (jeweils 15 s vorher angekündigt, Leiste oben zählt herunter):
   - **Nachschub** (2:30): alle Truhen füllen sich neu.
   - **Gewitter** (4:30): es regnet, Blitze schlagen ein – vorher knistert es an der Stelle.
   - **Nebel** (6:30): steigt in 3 Minuten von unten über alle Inseln; wer darin steht, nimmt Schaden, der immer
     stärker wird.
6. Das letzte Team, das noch steht, gewinnt. Danach geht es zurück in den Wolkenhafen, und die Karte wird Block für
   Block wiederhergestellt.

**Zurück ins Spiel:** Verliert jemand mitten in der Runde die Verbindung (zum Beispiel, weil das Spiel abstürzt), bleibt
sein Platz eine Weile frei (`rueckkehr-sekunden`, Vorgabe 90). Kommt er rechtzeitig wieder – direkt oder über „Zurück
ins Spiel“ in der Lobby –, steht er genau dort, wo er war: gleiche Insel, gleiche Sachen, gleiches Team („Willkommen
zurück!“). Hat sich die Gondel inzwischen geöffnet, schwebt er auf seine Insel hinunter und bekommt seine
Start-Ausrüstung. Kommt er zu spät, scheidet er aus wie bisher und seine Sachen fallen dort, wo er war. Wer gerade von
einem Gegner getroffen wurde, scheidet beim Verlassen sofort aus – sonst rettete ein Verbindungsabbruch aus jedem Kampf.

**Spielarten:** Allein (jeder eine Insel) oder **Zu zweit** (ab 3 Leuten; eine Party von zwei kommt ins selbe Team, kein
Schaden unter Teamkameraden).

**Start-Ausrüstungen** (alle kostenlos, nichts zu kaufen): Wanderer (nichts), Baumeister (24 Bretter + Steinspitzhacke),
Jäger (Bogen + 8 Pfeile), Bergführer (Federfall-Stiefel + Wassereimer), Sennerin (Ofenkartoffeln + Heiltrank),
Holzknecht (Steinaxt), Schmied (Kettenhemd).

## Karten (eigene Entwürfe, aus dem Code gebaut)

| Karte | Inseln | Mitte |
|---|---|---|
| **Wolkenalm** – grüne Almen über dem Wolkenmeer, ferne Gipfel | 12 Startinseln mit Fichte, Holzstoß, Laterne und Fahne in der Inselfarbe; 6 kleine Inseln dazwischen | weiße Bergkapelle mit Dachreiter und Glocke (2 Truhen am Altar), Maibaum, Brunnen, Linden |
| **Kristallgipfel** – verschneite Felsinseln mit Eisnadeln und Amethyst | 12 Startinseln, 6 Kristallfelsen | Felsgipfel mit Gipfelkreuz, Kristallgrotte (2 Truhen), rote Biwakschachtel |

Wolkenhafen und Karten entstehen beim ersten Start in einer eigenen leeren Welt (`wolkenkampf`); auf jeder Karte läuft
höchstens eine Runde gleichzeitig. Am Rand (gut 60 Blöcke von der Mitte) tobt ein Sturm: dort schadet es, dort kann
man nicht bauen, Perlen tragen nicht hinaus.

## Befehle

| Befehl | Was er tut |
|---|---|
| `/karte [wolkenalm\|kristallgipfel]` | über die Karte abstimmen (ohne Angabe: Menü) – auch `/map`, `/vote` |
| `/spielart [allein\|zweier]` | über die Spielart abstimmen – auch `/mode` |
| `/ausruestung [name]` | Start-Ausrüstung wählen – auch `/kit` |
| `/bereit` | bereit melden – auch `/ready` |
| `/zuschauen [spieler\|karte]` | einer Runde zuschauen – auch `/spectate` |
| `/verlassen` | Runde oder Zuschauen verlassen – auch `/leave` |
| `/stats [spieler]` · `/top` | Statistik und die besten Wolkenkämpfer |
| `/wolkenkampf info\|status\|start [karte] [allein\|zweier]\|vorspulen <countdown\|oeffnen\|nachschub\|gewitter\|nebel\|ende\|sekunden>\|blitz\|stopp\|wiederherstellen\|neubauen\|neuladen` | Verwaltung und Tests (`moinmorni.wolkenkampf.admin`); `vorspulen countdown` startet in der Gondel gleich die letzten fünf Sekunden |

Einstellungen stehen in der `config.yml` (Zurück-ins-Spiel-Zeit, Countdown, Mindestzahl, höchstens Spieler,
Gondel-Zeit, Zeiten der Ereignisse, Nebel-Dauer). Alle Menüs sind Truhen-Fenster und gehen auch mit Bedrock (Geyser); der Nebel ist auf Java
zusätzlich als steigende Fläche zu sehen, auf Bedrock als Schwaden.

## Im Netzwerk

Das Netzwerk-Plugin („MoinMorni“) stellt die Schnittstelle `de.moinmorni.api.MoinMorni` bereit; Wolkenkampf nutzt sie,
wenn sie da ist (`softdepend`): Sprache, Party (zusammen in eine Runde, zu zweit ins selbe Team), Statistik, Zustand,
zurück zur Lobby. Ohne Netzwerk kommt die Sprache aus dem Client.

Ab Version 2 der Schnittstelle meldet Wolkenkampf außerdem, wer gerade **spielt** (`playing`: ab dem Rundenstart bis
zum Ausscheiden oder Rundenende – so lange fragt „/lobby“ erst nach), schickt das Bett im Wolkenhafen über
`askToLobby` in die Lobby und hält nach einem Verbindungsabbruch den Platz frei (`holdSlot(uuid, sekunden)`, nach der
Rückkehr, bei Rundenende oder zu spät `holdSlot(uuid, 0)`). Ein älteres Netzwerk-Plugin kennt diese Methoden nicht –
dann geht es ohne Nachfrage und ohne Angebot in der Lobby weiter wie bisher.

Statistik nach jeder Runde (`stats("wolkenkampf", …)`, höher ist besser):

- hochgezählt: `games`, `wins`, `kills`, `deaths` (ausgeschieden), `chests` (Truhen geöffnet, jede einmal je Füllung)
- Bestwert: `best_kills` (meiste Kills in einer Runde)

Zustand: `waiting` oder `running`, dazu die Spielerzahl. Lokal: `spieler/<uuid>.yml` und `bestenliste.yml`.

Bei jedem Push auf `main` baut GitHub ein Release mit `Morni-Wolkenkampf.jar` (Version aus `pom.xml`).

## Bauen und testen

```bash
mvn package                      # Tests + target/Morni-Wolkenkampf.jar (Java 25)
mvn -Ppaper-26.1 compile         # passt der Code auch zu Paper 26.1?
test/run.sh                      # echter Test: Paper-Server + Test-Spieler (mineflayer)
FRESH=1 test/run.sh              # dasselbe, Welt vorher neu bauen
LANGSAM=8 test/run.sh            # Test-Spieler prüfen die Gondel 8 s später (wie auf einem ausgelasteten Rechner)
```

`test/run.sh` baut das Plugin und ein Test-Netzwerk (`test/fake-network`: Bert spricht Englisch, Anna und Carl sind
eine Party, alle Meldungen landen in `calls.log`), richtet einen Paper-26.1.2-Server ein (offline, Port 25810, RCON
25910, kurze Zeiten, 20 s Zurück ins Spiel) und spielt zwei Runden durch (`test/wolkenkampf-test.cjs`): allein auf
der Wolkenalm (Gondel, Landung, Truhen, Verbindung weg und rechtzeitig zurück, Brücke, Kampf, Hinunterstoßen,
Nachschub, Gewitter, Nebel, Sieg, Statistik, Karte wieder frisch) und zu zweit auf dem Kristallgipfel (Party im selben
Team, beide fallen sofort durch den Boden, kein Schaden unter Teamkameraden, Zuschauer, mitten im Kampf gehen, zu spät
zurück, Sieg zu zweit). Die Gondel öffnet sich im Test erst, wenn der Test sie geprüft hat
(`vorspulen countdown|oeffnen`) – auf einem ausgelasteten Rechner kommen RCON-Befehle Sekunden später an.
Mit `EXTRA_PLUGINS="…/ViaVersion.jar …/ViaBackwards.jar"` kommen ViaVersion und ViaBackwards dazu.

---

Verfasst mit Claude 🤖

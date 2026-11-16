# Stand – Morni Wolkenkampf

**Was es ist:** Der Insel-Kampf-Modus des MoinMorni-Netzwerks (Paper-Plugin `MorniWolkenkampf`, Java 25, Paper 26.x):
2–12 Leute starten in Ballon-Gondeln über eigenen Inseln in den Wolken, plündern Truhen, bauen Brücken und kämpfen –
das letzte Team gewinnt. Allein oder zu zweit. Repo `pmornhart/morni-wolkenkampf`, Jar `Morni-Wolkenkampf.jar`,
Netzwerk-Kennung `wolkenkampf`, englisch „Cloud Clash“.

## Aufbau

- `src/main/java/de/moinmorni/wolkenkampf/`: `WolkenkampfPlugin` (Start, Welt, Takt), `Settings` (config.yml), `Texts`
  (de/en), `Ui`, `Items`, `Holo` (Schrift je Sprache), `Sidebar` (Leiste + Namensfarben), `Boards` (Leisten + Tafeln),
  `Commands`.
- `logic/` (ohne Server, mit Tests): `Mode`, `Teams` (Partys zusammen), `Spawns` (Startinseln verteilen), `Loot`
  (Truhen-Stufen, gerechte Startinseln), `Timeline` (Nachschub → Gewitter → Nebel, Nebel-Höhe und -Schaden),
  `StartKit`, `IslandColour`, `MapLayout` (Inseln, Truhen, Gondeln, Nebel, Rand), `Votes`, `Groups`.
- `data/`: `PlayerData`, `Store`, `Leaderboard`.
- `world/`: `Plan`, `Decor` (Inseln, Wolken, Bäume, Berge), `Gondola` (Ballon-Gondel), `MapKit`, `MapType`,
  `WolkenalmPlan`, `KristallgipfelPlan`, `HallPlan` (Wolkenhafen), `Construction` (Bau bei neuer `VERSION`,
  Wiederherstellung der Spielfläche nach jeder Runde per Chunk-Momentaufnahme), `VoidGenerator`.
- `game/`: `Game` (Gondel, Öffnen, Kampf, Ereignisse, Nebelflächen, Ausscheiden, Sieg, Statistik), `Games`, `Fighter`,
  `LootItems`, `LangBar`.
- `hall/`: `Hall`, `HallMenus`. `menu/`: `Menu`, `MenuListener`. `listen/`: `PlayerListener`, `GameListener`,
  `GuardListener`. `net/`: `Network`, `Connected`, `Standalone` (Sprache aus dem Client).
- `test/`: `run.sh`, `wolkenkampf-test.cjs`, `rcon.cjs`, `fake-network/`.

## Stand (16.11.2026)

- Version 0.1.0 (erster Stand, Branch `claude/erster-stand`, noch nicht veröffentlicht): alles aus dem README.
- 37 Unit-Tests grün (Teams/Partys, Startinseln, Abstimmung, Truhen über 2000 Startwerte gerecht, Mitte besser,
  Ablauf/Nebel, Ausrüstungen, Kartenaufbau beider Karten inkl. Abstände und Truhen, Blocknamen, Bestenliste, Texte inkl.
  Prüfung aller Schlüssel im Quelltext, Landeschutz, Kill-Gutschrift). Übersetzt gegen Paper-API 26.3 und 26.1.
- Echter Test (Paper 26.1.2 + ViaVersion/ViaBackwards, vier Test-Spieler + Zuschauer): **98/98**, dreimal hintereinander
  ohne `FRESH`, Log ohne Fehler – Wolkenhafen, Abstimmung per Menü und Befehl, Ausrüstung, Countdown, Gondeln (halten
  fest, Boden aus neun geschlossenen Klappen, Countdown, Öffnen, Boden fällt weg, alle sind unten, bevor der Ballon
  aufsteigt – auch zu zweit), Landung ohne Fallschaden, Ballons steigen auf, gerechte Truhen, Brücke bauen und abbauen,
  Kampf mit Kill, Sachen bleiben liegen, Hinunterstoßen mit Kill-Gutschrift, Nachschub, Gewitter mit Blitzen und Regen
  nur für die Runde, Nebel steigt und schadet, Sieg, Netzwerk-Statistik, Karte wiederhergestellt, Bestenliste, /top,
  /stats, zweite Runde zu zweit mit Party im selben Team, kein Schaden unter Teamkameraden, Zuschauer, Lobby-Knopf.
- Gondel-Fehler behoben (16.11.): Eine offene Falltür ist eine dünne Wand am Blockrand, oben bündig mit dem Boden – wer
  darüber ragte (zu zweit ±0,7 neben der Mitte fast immer einer, allein wer am Korbrand lehnte), blieb oben stehen,
  stieg mit dem Ballon und fiel später ohne Langsames Fallen (Fallschaden). Jetzt: Klappen gehen auf (Ton, Bild), drei
  Ticks später fällt der Boden weg (Splitter, Krachen); wer halb auf dem Holzrahmen steht, wird über die Öffnung
  gestellt; der Ballon steigt ohne Boden auf (fängt niemanden mehr auf); der Landeschutz endet erst mit Boden unter den
  Füßen unterhalb des Gondelbodens (`Fighter.startLanding/landingTick` – vorher beendete ihn schon ein Teleport-Wackler
  in der Gondel). Neuer Test-Teil prüft: alle sind unten, bevor der Ballon aufsteigt (gegen den alten Stand rot: Tiefe 0
  bei den Hängengebliebenen, zu zweit alle vier, dazu Fallschaden).
- „Gondelboden fehlt in Folge-Läufen“ war kein Fehler der Welt: Boden auf der Platte (Regionsdateien gelesen) und bei
  der Wiederherstellung beim Start immer vollständig, bei jedem Rundenstart geschlossen (Diagnose-Läufe). Ursache war
  der Test: Die Gondel öffnete sich nach festen 6 s, auf dem ausgelasteten Rechner kamen RCON-Antworten aber 4–6 s
  später (der Server erledigt RCON erst, wenn ihm Zeit bleibt) – die Prüfungen liefen nach dem Öffnen. Jetzt wartet die
  Gondel im Test (60 s), bis der Test sie mit `/wolkenkampf vorspulen countdown|oeffnen` öffnet. `LANGSAM=7` stellt
  den ausgelasteten Rechner nach: alter Ablauf rot (Boden ✗, Gondel hält fest ✗ – wie beim Koordinator), neuer grün.
  Dabei behoben: zwei Sieger-Titel-Prüfungen warteten nicht auf die Pakete der anderen Test-Spieler, „Gondel hält
  fest“ maß einen Kreis statt des Korbs (in den Ecken 2,2 von der Mitte), `FRESH` löschte die Welt nicht (Paper 26
  legt sie unter `world/dimensions/minecraft/wolkenkampf` ab).
- Funde im echten Test behoben: Regen blieb nach dem Gewitter im Wolkenhafen (`resetPlayerWeather` fährt die
  Regenstärke nicht zurück → jetzt klares Wetter setzen), Blitz genau auf einem Spieler brach die Runde ab (Null-Vektor).
- Bilder mit echtem Client 26.3 nur lokal unter `/home/user/mm-cache/bilder-wolkenkampf/` (v1–v4): Wolkenhafen,
  beide Karten, Gondel-Sicht allein und zu zweit, Ballons steigen, Landung, Brücke, Gewitter, Nebel, Kapelle innen.
- Nicht echt geprüft: Bedrock-Spieler (Geyser; Nebelfläche dort nur als Schwaden), echte Spieler mit 12 Leuten,
  Netzwerk mit echtem Hub, Paper 26.3 als Server (nur gebaut).

## Offen

- Philip: Bilder ansehen; Go für `main` (dann erstes Release v0.1.0).
- Netzwerk-Repo: `MODE_REPOS` (Repo `morni-wolkenkampf`), Lobby-Figur, Webseite (Modus-Seite, Bestenliste nach
  `wins`), Einstellungs-Katalog für die `config.yml`.
